package com.kanedias.dybr.fair

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
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.entities.Account
import com.kanedias.dybr.fair.entities.Auth
import com.kanedias.dybr.fair.entities.RegisterRequest
import com.kanedias.dybr.fair.ui.LoginInputs
import com.kanedias.dybr.fair.ui.RegisterInputs
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.TimeUnit
import com.kanedias.dybr.fair.ui.Sidebar
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

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

    @BindView(R.id.acc_password_input)
    lateinit var passwordInput: EditText

    @BindView(R.id.acc_password_confirm_input)
    lateinit var confirmPasswordInput: EditText

    @BindView(R.id.acc_termsofservice_checkbox)
    lateinit var termsOfServiceSwitch: CheckBox

    @BindView(R.id.acc_is_over_18_checkbox)
    lateinit var isAdultSwitch: CheckBox

    @BindView(R.id.confirm_button)
    lateinit var confirmButton: Button

    @BindView(R.id.register_checkbox)
    lateinit var registerSwitch: CheckBox

    @BindViews(
            R.id.acc_email,
            R.id.acc_password, R.id.acc_password_confirm,
            R.id.acc_termsofservice_checkbox, R.id.acc_is_over_18_checkbox
    )
    lateinit var regInputs: List<@JvmSuppressWildcards View>

    @BindViews(R.id.acc_email, R.id.acc_password)
    lateinit var loginInputs: List<@JvmSuppressWildcards View>

    private lateinit var activity: MainActivity

    private lateinit var progressDialog: MaterialDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_create_account, container, false)
        ButterKnife.bind(this, root)
        activity = context as MainActivity

        progressDialog = MaterialDialog.Builder(activity)
                .title(R.string.please_wait)
                .content(R.string.checking_in_progress)
                .progress(true, 0)
                .build()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog.dismiss()
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
        emailInput.requestFocus()
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
            async(UI) { delay(3, TimeUnit.SECONDS); validator.clearValidations() }
            return
        }

        if (registerSwitch.isChecked) {
            // send register query
            doRegistration()
        } else {
            // send login query
            doLogin()
        }

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

        launch(UI) {
            progressDialog.show()

            try {
                async { Network.login(acc) }.await()

                Toast.makeText(activity, R.string.login_successful, Toast.LENGTH_SHORT).show()
                activity.selectProfile() // shows profile selection dialog

                //we logged in successfully, return to main activity
                DbProvider.helper.accDao.create(acc)
                handleSuccess()
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex, mapOf(422 to R.string.invalid_credentials))
            }

            progressDialog.hide()
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
            isAdult = isAdultSwitch.isChecked
        }

        launch(UI) {
            progressDialog.show()

            try {
                val response = async { Network.createAccount(req) }.await()
                val acc = Account().apply {
                    serverId = response.id
                    email = response.email
                    password = req.password // get from request, can't be obtained from user info
                    createdAt = response.createdAt
                    updatedAt = response.updatedAt
                    isAdult = response.isAdult // default is false
                    current = true // we just registered, certainly we want to use it now
                }

                DbProvider.helper.accDao.create(acc)
                Toast.makeText(activity, R.string.congrats_diary_registered, Toast.LENGTH_SHORT).show()

                // let's make sure user understood what's needed of him
                MaterialDialog.Builder(activity)
                        .title(R.string.next_steps)
                        .content(R.string.registered_please_confirm_mail)
                        .positiveText(android.R.string.ok)
                        .show()

                // return to main activity
                Auth.updateCurrentUser(acc)
                handleSuccess()
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex, mapOf(422 to R.string.invalid_credentials))
            }

            progressDialog.hide()
        }
    }

    /**
     * Handle successful account addition. Navigate back to [MainActivity] and update sidebar account list.
     */
    private fun handleSuccess() {
        fragmentManager!!.popBackStack()
        activity.refresh()
    }
}