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
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import convalida.library.Convalida
import convalida.library.ConvalidaValidator
import dybr.kanedias.com.fair.ui.LoginInputs
import dybr.kanedias.com.fair.ui.RegisterInputs
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/**
 * @author Kanedias
 *
 * Created on 11/11/2017.
 */
class AddAccountFragment : Fragment() {

    @BindView(R.id.account_email)
    lateinit var emailInput: TextInputLayout

    @BindView(R.id.account_username)
    lateinit var usernameInput: TextInputLayout

    @BindView(R.id.account_password)
    lateinit var passwordInput: TextInputLayout

    // These fields are optional
    @BindView(R.id.account_password_confirm)
    lateinit var confirmPasswordInput: TextInputLayout

    @BindView(R.id.account_namespace)
    lateinit var namespaceInput: TextInputLayout

    @BindView(R.id.confirm_button)
    lateinit var confirmButton: Button

    @BindView(R.id.register_checkbox)
    lateinit var registerSwitch: CheckBox

    lateinit var validator: ConvalidaValidator

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_create_account, container, false)
        ButterKnife.bind(this, root)
        validator = Convalida.init(LoginInputs(this))
        return root
    }

    @OnCheckedChanged(R.id.register_checkbox)
    fun switchToRegister(checked: Boolean) {
        if (checked) {
            confirmButton.setText(R.string.register)
            confirmPasswordInput.visibility = View.VISIBLE
            usernameInput.visibility = View.VISIBLE
            namespaceInput.visibility = View.VISIBLE
            validator = Convalida.init(RegisterInputs(this))
        } else {
            confirmButton.setText(R.string.enter)
            confirmPasswordInput.visibility = View.GONE
            usernameInput.visibility = View.GONE
            namespaceInput.visibility = View.GONE
            validator = Convalida.init(LoginInputs(this))
        }
    }

    @OnClick(R.id.confirm_button)
    fun confirm() {
        if (!validator.validateFields()) {
            return
        }

        var progressDialog = MaterialDialog.Builder(activity!!)
                .title(R.string.checking_in_progress)
                .content(R.string.checking_in_progress)
                .progress(true, 0)
                .build()

        progressDialog.show()
        if (registerSwitch.isChecked) {
            // send register query
            progressDialog.setContent(R.string.checking_nickname)
            RegisterCallback(progressDialog).execute(Step.CHECK_USERNAME)
        } else {
            // send login query
        }
    }

    enum class Step {
        CHECK_USERNAME,
        CHECK_EMAIL,
        CHECK_NAMESPACE
    }

    inner class RegisterCallback(dialog: MaterialDialog): AsyncTask<Step, Step, Boolean>() {

        private val pd = dialog

        override fun doInBackground(vararg initial: Step?) : Boolean {
            val step = initial[0]!!
            publishProgress(step)

            val url = when (step) {
                Step.CHECK_USERNAME -> "${Network.IDENTITY_ENDPOINT}/exists?name=${usernameInput.editText!!.text}"
                Step.CHECK_EMAIL -> "${Network.USER_ENDPOINT}/exists?email=${emailInput.editText!!.text}"
                Step.CHECK_NAMESPACE -> "${Network.IDENTITY_ENDPOINT}/exists?uri=${namespaceInput.editText!!.text}"
            }
            val possibleError = getString(when (step) {
                Step.CHECK_USERNAME -> R.string.nickname_already_taken
                Step.CHECK_EMAIL -> R.string.email_already_registered
                Step.CHECK_NAMESPACE -> R.string.namespace_already_taken
            })
            val req = Request.Builder().url(url).build()
            try {
                val resp = Network.httpClient.newCall(req).execute()
                if (!resp.isSuccessful) {
                    makeToast(getString(R.string.unexpected_website_error))
                    return false
                }

                val json = JSONObject(resp.body()?.string())
                val result = json.get("result") as Boolean
                if (result) {
                    makeToast(possibleError)
                    return false
                }


                if (step.ordinal == Step.values().size - 1) {
                    // this was the last step, verifications complete!
                    // send actual register POST
                    sendRegisterInfo()
                    return true
                }

                doInBackground(Step.values()[step.ordinal + 1])
            } catch (ioex: IOException) {
                val errorText =  getString(R.string.error_connecting)
                makeToast("$errorText: ${ioex.localizedMessage}")
            } finally {
                pd.dismiss()
            }

            return false
        }

        fun sendRegisterInfo() {

        }

        override fun onProgressUpdate(vararg value: Step?) {
            when (value[0]!!) {
                Step.CHECK_USERNAME -> pd.setContent(R.string.checking_nickname)
                Step.CHECK_EMAIL -> pd.setContent(R.string.checking_email)
                Step.CHECK_NAMESPACE -> pd.setContent(R.string.checking_namespace)
            }
        }

        override fun onPostExecute(result: Boolean?) {
            if (result!!) {
                makeToast(getString(R.string.congrats_diary_registered))
                fragmentManager!!.popBackStack()
            }
        }
    }

    /**
     * Be sure to call this function only if fragment is already attached to activity!
     */
    fun makeToast(text: String) {
        activity!!.runOnUiThread { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show() }
    }

}