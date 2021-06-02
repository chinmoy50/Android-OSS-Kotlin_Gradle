package com.kickstarter.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.kickstarter.R
import com.kickstarter.databinding.ItemCommentCardBinding
import com.kickstarter.models.Comment
import com.kickstarter.ui.viewholders.CommentCardViewHolder
import com.kickstarter.ui.viewholders.EmptyCommentsViewHolder
import com.kickstarter.ui.viewholders.KSViewHolder
import com.kickstarter.ui.views.CommentCardStatus

class CommentsAdapter(private val delegate: Delegate) : KSListAdapter() {
    interface Delegate : EmptyCommentsViewHolder.Delegate, CommentCardViewHolder.Delegate

    @LayoutRes
    override fun layout(sectionRow: SectionRow): Int {
        return R.layout.item_comment_card
    }

    fun takeData(comments: List<Comment>) {
        clearSections()
        addSection(comments)
        submitList(items())
    }

    fun insertData(comment: Comment, position: Int) {
        insertSection(position, listOf(comment))
        notifyItemChanged(position)
    }

    fun updateItem(comment: Comment, position: Int, commentCardStatus: CommentCardStatus) {
        setSection(position, listOf(Pair(comment, commentCardStatus)))
        notifyItemChanged(position)
    }

    override fun viewHolder(@LayoutRes layout: Int, viewGroup: ViewGroup): KSViewHolder {
        return CommentCardViewHolder(ItemCommentCardBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false), delegate)
    }
}
