package dybr.kanedias.com.fair

import android.os.Bundle
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
import convalida.library.Convalida
import dybr.kanedias.com.fair.database.DbProvider
import dybr.kanedias.com.fair.entities.Auth
import dybr.kanedias.com.fair.entities.Account
import dybr.kanedias.com.fair.entities.RegisterRequest
import dybr.kanedias.com.fair.misc.Android
import dybr.kanedias.com.fair.ui.LoginInputs
import dybr.kanedias.com.fair.ui.RegisterInputs
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.TimeUnit
import dybr.kanedias.com.fair.ui.Sidebar

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

    @BindView(R.id.acc_termsofservice_checkbox)
    lateinit var termsOfServiceSwitch: CheckBox

    @BindView(R.id.confirm_button)
    lateinit var confirmButton: Button

    @BindView(R.id.register_checkbox)
    lateinit var registerSwitch: CheckBox

    @BindViews(
            R.id.acc_email, R.id.acc_username,
            R.id.acc_password, R.id.acc_password_confirm,
            R.id.acc_termsofservice_checkbox
    )
    lateinit var regInputs: List<@JvmSuppressWildcards View>

    @BindViews(R.id.acc_email, R.id.acc_password)
    lateinit var loginInputs: List<@JvmSuppressWildcards View>

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
            confirmButton.setText(R.string.enter)
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
            doRegistration()
        } else {
            // send login query
            doLogin()
        }
        progressDialog.dismiss()
    }

    /**
     * Creates session for the user, saves auth and closes fragment on success.
     */
    private fun doLogin() {
        val acc = Account().apply {
            email = emailInput.text.toString()
            password = passwordInput.text.toString()
            current = true
        }


        Network.makeAsyncRequest(activity, { Network.login(acc) }, mapOf(422 to R.string.invalid_credentials))
        acc.accessToken?.let { // success, we obtained access token
            saveAuth(acc)

            Toast.makeText(activity, R.string.login_successful, Toast.LENGTH_SHORT).show()
            fragmentManager!!.popBackStack()
            activity.refreshTabs()
        }
    }

    /**
     * Obtains registration info from input fields, sends registration request, handles answer.
     * Also saves account, makes it current and closes the fragment if everything was successful.
     */
    private fun doRegistration() {
        val req = RegisterRequest().apply {
            email = emailInput.text.toString()
            password = passwordInput.text.toString()
            confirmPassword = confirmPasswordInput.text.toString()
            termsOfService = termsOfServiceSwitch.isChecked
        }

        val response = Network.makeAsyncRequest(activity, { Network.register(req) })
        response?.let { // on success
            val acc = Account().apply {
                email = it.email
                password = req.password // get from request, can't be obtained from user info
                createdAt = it.createdAt
                updatedAt = it.updatedAt
                isOver18 = it.isOver18 // default is false
                current = true // we just registered, certainly we want to use it now
            }
            saveAuth(acc)

            Toast.makeText(activity, R.string.congrats_diary_registered, Toast.LENGTH_SHORT).show()
            fragmentManager!!.popBackStack()
            activity.refreshTabs()
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
}