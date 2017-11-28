package dybr.kanedias.com.fair

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife

/**
 * View holder for showing regular posts in diary view.
 *
 * @see PostListFragment.postRibbon
 * @author Kanedias
 */
class RegularPostViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {
    @BindView(R.id.post_title)
    lateinit var titleView: TextView

    @BindView(R.id.post_message)
    lateinit var bodyView: TextView

    init {
        ButterKnife.bind(this, iv)
        iv.setOnClickListener {} // TODO: show comments fragment
    }
}