package com.kanedias.dybr.fair

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.dybr.fair.dto.ProfileCreateRequest
import kotlinx.coroutines.*

/**
 * Fragment for creating profile for currently logged in account.
 *
 * @author Kanedias
 *
 * Created on 24.12.17
 */
class AddProfileFragment: Fragment() {

    @BindView(R.id.prof_nickname_input)
    lateinit var nicknameInput: EditText

    @BindView(R.id.prof_birthday_input)
    lateinit var birthdayInput: EditText

    @BindView(R.id.prof_description_input)
    lateinit var descInput: EditText

    private lateinit var activity: MainActivity

    private lateinit var progressDialog: MaterialDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_create_profile, container, false)
        ButterKnife.bind(this, root)
        activity = context as MainActivity

        progressDialog = MaterialDialog.Builder(activity)
                .title(R.string.please_wait)
                .content(R.string.checking_in_progress)
                .progress(true, 0)
                .build()

        return root
    }

    @OnClick(R.id.prof_create_button)
    fun confirm() {
        val profReq = ProfileCreateRequest().apply {
            nickname = nicknameInput.text.toString()
            birthday = birthdayInput.text.toString()
            description = descInput.text.toString()
        }

        GlobalScope.launch(Dispatchers.Main) {
            progressDialog.show()

            try {
                val profile = async(Dispatchers.IO) { Network.createProfile(profReq) }.await()
                Auth.updateCurrentProfile(profile)

                //we created profile successfully, return to main activity
                Toast.makeText(activity, R.string.profile_created, Toast.LENGTH_SHORT).show()
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