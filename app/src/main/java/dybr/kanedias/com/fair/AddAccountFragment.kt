package dybr.kanedias.com.fair

import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import butterknife.*
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import convalida.library.Convalida
import dybr.kanedias.com.fair.database.DbProvider
import dybr.kanedias.com.fair.entities.Auth
import dybr.kanedias.com.fair.entities.Account
import dybr.kanedias.com.fair.entities.RegisterRequest
import dybr.kanedias.com.fair.misc.Android
import dybr.kanedias.com.fair.misc.HttpException
import dybr.kanedias.com.fair.ui.LoginInputs
import dybr.kanedias.com.fair.ui.RegisterInputs
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import dybr.kanedias.com.fair.ui.Sidebar
import java.net.HttpURLConnection

/**
 * Fragment responsible for adding account. Appears when you click "add account" in the sidebar.
 * This may be either registration or logging in.
 *
 * @see Sidebar.addAccount
 * @author Kanedias
 *
 * Created on 11/11/2017.
 */
class AddAccountFragment : Fragment() {

    @BindView(R.id.acc_email_input)
    lateinit var emailInput: EditText

    @BindView(R.id.acc_username_input)
    lateinit var usernameInput: EditText

    @BindView(R.id.acc_password_input)
    lateinit var passwordInput: EditText

    @BindView(R.id.acc_password_confirm_input)
    lateinit var confirmPasswordInput: EditText

    @BindView(R.id.acc_diary_address_input)
    lateinit var namespaceInput: EditText

    @BindView(R.id.acc_diary_title_input)
    lateinit var titleInput: EditText

    @BindView(R.id.confirm_button)
    lateinit var confirmButton: Button

    @BindView(R.id.register_checkbox)
    lateinit var registerSwitch: CheckBox

    @BindViews(
            R.id.acc_email, R.id.acc_username,
            R.id.acc_password, R.id.acc_password_confirm,
            R.id.acc_diary_address, R.id.acc_diary_title
    )
    lateinit var regInputs: List<@JvmSuppressWildcards TextInputLayout>

    @BindViews(R.id.acc_email, R.id.acc_password)
    lateinit var loginInputs: List<@JvmSuppressWildcards TextInputLayout>

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_create_account, container, false)
        ButterKnife.bind(this, root)
        activity = context as MainActivity
        return root
    }

    /**
     * Show/hide additional register fields, change main button text
     */
    @OnCheckedChanged(R.id.register_checkbox)
    fun switchToRegister(checked: Boolean) {
        if (checked) {
            confirmButton.setText(R.string.register)
            loginInputs.forEach { it -> it.visibility = View.GONE }
            regInputs.forEach { it -> it.visibility = View.VISIBLE }
        } else {
            regInputs.forEach { it -> it.visibility = View.GONE }
            loginInputs.forEach { it -> it.visibility = View.VISIBLE }
        }
    }

    /**
     * Performs register/login call after all fields are validated
     */
    @OnClick(R.id.confirm_button)
    fun confirm() {
        val validator = when (registerSwitch.isChecked) {
            true -> Convalida.init(RegisterInputs(this))
            false -> Convalida.init(LoginInputs(this))
        }

        if (!validator.validateFields()) {
            // don't allow network request if there are errors in form
            // and hide errors after 3 seconds
            async(Android) { delay(3, TimeUnit.SECONDS); validator.clearValidations() }
            return
        }

        val progressDialog = MaterialDialog.Builder(activity)
                .title(R.string.please_wait)
                .content(R.string.checking_in_progress)
                .progress(true, 0)
                .build()

        progressDialog.show()
        if (registerSwitch.isChecked) {
            // send register query
            RegisterCallback(progressDialog).execute(Step.CHECK_USERNAME)
        } else {
            // send login query
            LoginCallback(progressDialog).execute()
        }
    }

    /**
     * Authentication task.
     * If auth is successful, saves account and dismisses both dialog and fragment.
     *
     * Be sure to launch and pass progress dialog prior to launching this.
     */
    inner class LoginCallback(dialog: MaterialDialog): AsyncTask<Unit, Unit, Boolean>() {

        private val pd = dialog

        /**
         * Just delegate auth to [Network.login] and handle the result
         */
        override fun doInBackground(vararg nothing: Unit?): Boolean {
            try {
                val acc = Account()
                acc.apply {
                    email = emailInput.text.toString()
                    password = passwordInput.text.toString()
                    current = true
                }

                Network.login(acc)
                saveAuth(acc)
                return true
            } catch (httpex: HttpException) { // non-200 code on login
                // TODO: check actual code on login
                if (httpex.code == HttpURLConnection.HTTP_UNAUTHORIZED) { // unauthorized
                    makeToast(getString(R.string.invalid_credentials))
                } else {
                    val errorText = getString(R.string.unexpected_website_error)
                    makeToast("$errorText: ${httpex.message}")
                }
            }  catch (ioex: IOException) {
                val errorText = getString(R.string.error_connecting)
                makeToast("$errorText: ${ioex.localizedMessage}")
            } finally {
                pd.dismiss()
            }

            return false
        }

        override fun onPostExecute(result: Boolean?) {
            // post success message and dismiss fragment if all went well
            if (result!!) {
                makeToast(getString(R.string.login_successful))
                fragmentManager!!.popBackStack()
                activity.refreshTabs()
            }
        }
    }

    /**
     * Registration steps.
     * These represent server-side sequence of operation.
     *
     * @see RegisterCallback
     */
    enum class Step {
        CHECK_USERNAME,
        CHECK_EMAIL,
        CHECK_NAMESPACE,
    }

    /**
     * Pre-registration async checks and registration.
     *
     * Performs checks for username, email, address not to be taken.
     * If something goes wrong, dismisses the dialog and shows toast about what has gone wrong.
     * If all is successful, posts registration information, saves account and dismisses both dialog and fragment.
     *
     * Be sure to launch and pass progress dialog prior to launching this.
     */
    inner class RegisterCallback(dialog: MaterialDialog): AsyncTask<Step, Step, Boolean>() {

        private val pd = dialog

        /**
         * Recursive, does auth checks one by one. If all checks pass,
         * calls [sendRegisterInfo]
         */
        override fun doInBackground(vararg initial: Step?) : Boolean {
            val step = initial[0]!!
            publishProgress(step)

            // initialize check urls/error strings
            val url = when (step) {
                Step.CHECK_USERNAME -> "${Network.IDENTITY_ENDPOINT}/exists?name=${usernameInput.text}"
                Step.CHECK_EMAIL -> "${Network.USER_ENDPOINT}/exists?email=${emailInput.text}"
                Step.CHECK_NAMESPACE -> "${Network.IDENTITY_ENDPOINT}/exists?uri=${namespaceInput.text}"
            }
            val possibleError = getString(when (step) {
                Step.CHECK_USERNAME -> R.string.nickname_already_taken
                Step.CHECK_EMAIL -> R.string.email_already_registered
                Step.CHECK_NAMESPACE -> R.string.namespace_already_taken
            })

            val req = Request.Builder().url(url).build()
            try {
                // call it synchronously, we don't want callback hell here
                val resp = Network.httpClient.newCall(req).execute()
                if (!resp.isSuccessful) {
                    // TODO: this normally shouldn't happen but it'd be good to test this!
                    throw IOException(getString(R.string.unexpected_website_error))
                }

                // body looks like { "result": true }
                // no need in heavy-lifting through Gson
                val json = JSONObject(resp.body()?.string())
                val result = json.get("result") as Boolean
                if (result) {
                    makeToast(possibleError)
                    return false
                }


                if (step.ordinal != Step.values().size - 1) {
                    // proceed to next step
                    return doInBackground(Step.values()[step.ordinal + 1])
                }

                // this was the last step, verifications complete!
                // send actual register POST
                return sendRegisterInfo()
            }  catch (httpex: HttpException) { // non-200 code
                // TODO: check actual code on login
                if (httpex.code == HttpURLConnection.HTTP_NOT_FOUND) { // unauthorized
                    makeToast(getString(R.string.profile_not_found))
                } else {
                    val errorText = getString(R.string.unexpected_website_error)
                    makeToast("$errorText: ${httpex.message}")
                }
            } catch (ioex: IOException) {
                val errorText =  getString(R.string.error_connecting)
                makeToast("$errorText: ${ioex.localizedMessage}")
            } finally {
                pd.dismiss()
            }

            return false
        }

        /**
         * Send registration request and validate the result.
         *
         * This also sets session cookies in http client that are needed
         * to call authenticated-only API functions.
         */
        private fun sendRegisterInfo(): Boolean {
            val regRequest = RegisterRequest(
                    name = usernameInput.text.toString(),
                    email = emailInput.text.toString(),
                    uri = namespaceInput.text.toString(),
                    password = passwordInput.text.toString(),
                    confirmPassword = passwordInput.text.toString(),
                    title = titleInput.text.toString()
            )
            val body = RequestBody.create(Network.MIME_JSON, Gson().toJson(regRequest))
            val req = Request.Builder().post(body).url(Network.REGISTER_ENDPOINT).build()
            val resp = Network.httpClient.newCall(req).execute() // if it throws, we still catch it in [doInBackground]
            if (!resp.isSuccessful) {
                throw IOException(getString(R.string.unexpected_website_error))
            }

            // body looks like { "status": "OK" }
            val json = JSONObject(resp.body()?.string())
            val result = json.get("status") as String
            if (result != "OK") {
                makeToast(getString(R.string.registration_failed))
                return false
            }

            // if we're here then we survived through registration checks
            // and registration itself was successful, let's save what we have
            val acc = Account().apply {
                name = regRequest.name
                email = regRequest.email
                password = regRequest.password
                current = true
            }

            // without identity we can't know current profile name
            Network.populateIdentity(acc)
            saveAuth(acc)
            return true
        }

        override fun onProgressUpdate(vararg value: Step?) {
            // update text of progress dialog to current step description
            when (value[0]!!) {
                Step.CHECK_USERNAME -> pd.setContent(R.string.checking_nickname)
                Step.CHECK_EMAIL -> pd.setContent(R.string.checking_email)
                Step.CHECK_NAMESPACE -> pd.setContent(R.string.checking_namespace)
            }
        }

        override fun onPostExecute(result: Boolean?) {
            // post success message and dismiss fragment if all went well
            if (result!!) {
                makeToast(getString(R.string.congrats_diary_registered))
                fragmentManager!!.popBackStack()
                activity.refreshTabs()
            }
        }
    }

    /**
     * Get identity, persist registration info in DB, set in [Auth].
     * *This should be done in background thread*
     */
    private fun saveAuth(acc: Account) {
        DbProvider.helper.accDao.create(acc)
        Auth.user = acc
    }

    /**
     * Be sure to call this function only if fragment is already attached to activity!
     */
    fun makeToast(text: String) {
        activity.runOnUiThread { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show() }
    }

}