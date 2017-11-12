package dybr.kanedias.com.fair

import android.content.Context
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.Fragment
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import convalida.annotations.ConfirmPasswordValidation
import convalida.annotations.EmailValidation
import convalida.annotations.NotEmptyValidation
import convalida.annotations.PasswordValidation
import convalida.library.Convalida
import convalida.library.ConvalidaValidator

/**
 * @author Kanedias
 *
 * Created on 11/11/2017.
 */
class AddAccountFragment : Fragment() {

    @EmailValidation(R.string.invalid_email)
    @BindView(R.id.account_email)
    lateinit var emailInput: TextInputLayout

    @PasswordValidation(errorMessage = R.string.invalid_password)
    @BindView(R.id.account_password)
    lateinit var passwordInput: TextInputLayout

    // These fields are optional
    @ConfirmPasswordValidation(R.string.passwords_not_match)
    @BindView(R.id.account_password_confirm)
    lateinit var confirmPasswordInput: TextInputLayout

    @NotEmptyValidation(R.string.should_not_be_empty)
    @BindView(R.id.account_namespace)
    lateinit var namespaceInput: TextInputLayout

    @BindView(R.id.confirm_button)
    lateinit var confirmButton: Button

    lateinit var validator: ConvalidaValidator

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_create_account, container, false)
        ButterKnife.bind(this, root)
        validator = Convalida.init(this)
        validator.validateFields()
        return root
    }

    @OnCheckedChanged(R.id.register_checkbox)
    fun switchToRegister(checked: Boolean) {
        confirmButton.setText(if (checked) R.string.register else R.string.enter)
    }

    @OnClick(R.id.confirm_button)
    fun confirm() {

    }

}