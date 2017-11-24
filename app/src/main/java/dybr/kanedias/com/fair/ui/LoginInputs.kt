package dybr.kanedias.com.fair.ui

import convalida.annotations.EmailValidation
import convalida.annotations.PasswordValidation
import dybr.kanedias.com.fair.AddAccountFragment
import dybr.kanedias.com.fair.R

/**
 * Validation helper class for logging in.
 * This has to be upper-level class because of way how Convalida annotation processing works in inner classes.
 * @see AddAccountFragment
 */
class LoginInputs(private val parent: AddAccountFragment) {

    @JvmField
    @EmailValidation(R.string.invalid_email)
    val email = parent.emailInput

    @JvmField
    @PasswordValidation(errorMessage = R.string.should_not_be_empty)
    val password = parent.passwordInput

    // required for validator to work
    fun getString(id: Int): String = parent.getString(id)
}