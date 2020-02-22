package com.kanedias.dybr.fair

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.dybr.fair.dto.ProfileCreateRequest
import com.kanedias.dybr.fair.themes.showThemed
import kotlinx.coroutines.*

/**
 * Fragment for creating blog for currently logged in profile.
 *
 * @author Kanedias
 *
 * Created on 14.01.18
 */
class AddBlogFragment: Fragment() {

    @BindView(R.id.blog_slug_input)
    lateinit var slugInput: EditText

    @BindView(R.id.blog_title_input)
    lateinit var titleInput: EditText

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_create_blog, container, false)
        ButterKnife.bind(this, view)
        activity = context as MainActivity

        return view
    }

    @OnClick(R.id.blog_create_button)
    fun confirm() {
        val profReq = ProfileCreateRequest().apply {
            id = Auth.profile?.id
            blogSlug = slugInput.text.toString()
            blogTitle = titleInput.text.toString()
        }

        val progressDialog = MaterialDialog(requireContext())
                .title(R.string.please_wait)
                .message(R.string.checking_in_progress)

        lifecycleScope.launch {
            progressDialog.showThemed(activity.styleLevel)

            try {
                val updated = withContext(Dispatchers.IO) { Network.updateProfile(profReq) }
                Auth.updateCurrentProfile(updated)

                //we created blog successfully, return to main activity
                Toast.makeText(requireContext(), R.string.blog_created, Toast.LENGTH_SHORT).show()
                handleSuccess()
            } catch (ex: Exception) {
                Network.reportErrors(context, ex)
            }

            progressDialog.dismiss()
        }
    }

    /**
     * Handle successful blog addition. Navigate back to [MainActivity] and update sidebar account list.
     */
    private fun handleSuccess() {
        requireFragmentManager().popBackStack()
        activity.refresh()
    }
}