package com.kanedias.dybr.fair

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import butterknife.*
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.database.entities.Account
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.dybr.fair.dto.RegisterRequest
import com.kanedias.dybr.fair.themes.showThemed
import com.kanedias.dybr.fair.ui.Sidebar
import kotlinx.coroutines.*

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_create_account, container, false)
        ButterKnife.bind(this, view)
        activity = context as MainActivity

        return view
    }

    /**
     * Show/hide additional register fields, change main button text
     */
    @OnCheckedChanged(R.id.register_checkbox)
    fun switchToRegister(checked: Boolean) {
        if (checked) {
            confirmButton.setText(R.string.register)
            loginInputs.forEach { it.visibility = View.GONE }
            regInputs.forEach { it.visibility = View.VISIBLE }
        } else {
            confirmButton.setText(R.string.enter)
            regInputs.forEach { it.visibility = View.GONE }
            loginInputs.forEach { it.visibility = View.VISIBLE }
        }
        emailInput.requestFocus()
    }

    /**
     * Performs register/login call after all fields are validated
     */
    @OnClick(R.id.confirm_button)
    fun confirm() {
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
        if (DbProvider.helper.accDao.queryForEq("email", emailInput.text.toString()).isNotEmpty()) {
            // we already have this account as active, skip!
            Toast.makeText(activity, R.string.email_already_added, Toast.LENGTH_SHORT).show()
            return
        }

        val acc = Account().apply {
            email = emailInput.text.toString()
            password = passwordInput.text.toString()
            current = true
        }

        val progressDialog = MaterialDialog(requireContext())
                .title(R.string.please_wait)
                .message(R.string.checking_in_progress)

        lifecycleScope.launch {
            progressDialog.showThemed(activity.styleLevel)

            try {
                withContext(Dispatchers.IO) { Network.login(acc) }

                Toast.makeText(requireContext(), R.string.login_successful, Toast.LENGTH_SHORT).show()
                if (Auth.user.lastProfileId.isNullOrBlank() || Auth.user.lastProfileId == "0") {
                    // user doesn't have active profile, select one
                    activity.startProfileSelector() // shows profile selection dialog
                }

                //we logged in successfully, return to main activity
                DbProvider.helper.accDao.create(acc)
                handleSuccess()
            } catch (ex: Exception) {
                val errorMap = mapOf(
                        "email_not_confirmed" to getString(R.string.email_not_activated_yet),
                        "email_not_found" to getString(R.string.email_not_found),
                        "email_invalid" to getString(R.string.email_is_invalid),
                        "password_invalid" to getString(R.string.incorrect_password)
                )

                Network.reportErrors(ctx = context, ex = ex, detailMapping = errorMap)
            }

            progressDialog.dismiss()
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

        val progressDialog = MaterialDialog(requireContext())
                .title(R.string.please_wait)
                .message(R.string.checking_in_progress)

        lifecycleScope.launch {
            progressDialog.showThemed(activity.styleLevel)

            try {
                val response = withContext(Dispatchers.IO) { Network.createAccount(req) }
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
                Toast.makeText(requireContext(), R.string.congrats_diary_registered, Toast.LENGTH_SHORT).show()

                // let's make sure user understood what's needed of him
                MaterialDialog(requireContext())
                        .title(R.string.next_steps)
                        .message(R.string.registered_please_confirm_mail)
                        .positiveButton(android.R.string.ok)
                        .show()

                // return to main activity
                Auth.updateCurrentUser(acc)
                handleSuccess()
            } catch (ex: Exception) {
                val errorMap = mapOf(
                        "email_registered" to getString(R.string.email_already_registered),
                        "email_not_confirmed" to getString(R.string.email_not_activated_yet),
                        "email_not_found" to getString(R.string.email_not_found),
                        "email_invalid" to getString(R.string.email_is_invalid),
                        "password_invalid" to getString(R.string.incorrect_password)
                )

                Network.reportErrors(ctx = requireContext(), ex = ex, detailMapping = errorMap)
            }

            progressDialog.dismiss()
        }
    }

    /**
     * Handle successful account addition. Navigate back to [MainActivity] and update sidebar account list.
     */
    private fun handleSuccess() {
        requireFragmentManager().popBackStack()
        activity.refresh()
    }
}