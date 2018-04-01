package com.kanedias.dybr.fair.ui

import convalida.annotations.*
import com.kanedias.dybr.fair.AddAccountFragment
import com.kanedias.dybr.fair.R

/**
 * Validation helper class for registering.
 * This has to be upper-level class because of way how Convalida annotation processing works in inner classes.
 * @see AddAccountFragment
 */
class RegisterInputs(private val parent: AddAccountFragment) {

    @JvmField
    @EmailValidation(R.string.invalid_email)
    val email = parent.emailInput

    @JvmField
    @PasswordValidation(errorMessage = R.string.invalid_password)
    val password = parent.passwordInput

    @JvmField
    @ConfirmPasswordValidation(R.string.passwords_not_match)
    val confirmPassword = parent.confirmPasswordInput

    // required for validator to work
    fun getString(id: Int): String = parent.getString(id)
}