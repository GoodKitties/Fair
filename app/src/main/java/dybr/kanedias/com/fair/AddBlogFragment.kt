package dybr.kanedias.com.fair

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife

/**
 * @author Kanedias
 *
 * Created on 14.01.18
 */
class AddBlogFragment: Fragment() {

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_create_account, container, false)
        ButterKnife.bind(this, root)
        activity = context as MainActivity

        return root
    }
}