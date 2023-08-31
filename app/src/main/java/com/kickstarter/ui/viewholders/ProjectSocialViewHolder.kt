package com.kickstarter.ui.viewholders

import com.kickstarter.databinding.ProjectSocialViewBinding
import com.kickstarter.libs.transformations.CircleTransformation

import com.kickstarter.models.User
import com.squareup.picasso.Picasso

class ProjectSocialViewHolder(private val binding: ProjectSocialViewBinding) : KSViewHolder(binding.root) {
    private var user: User? = null
    @Throws(Exception::class)
    override fun bindData(data: Any?) {
        user = requireNotNull(data as User?) { User::class.java.toString() + " required to be non-null." }
    }

    override fun onBind() {
        user?.avatar()?.small()?.let {
            Picasso.get()
                .load(it)
                .transform(CircleTransformation())
                .into(binding.friendImage)
        }
        binding.friendName.text = user?.name()
    }
}
