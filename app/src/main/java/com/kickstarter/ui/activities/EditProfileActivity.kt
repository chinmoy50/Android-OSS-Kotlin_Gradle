package com.kickstarter.ui.activities

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.isGone
import com.kickstarter.databinding.ActivityEditProfileBinding
import com.kickstarter.libs.BaseActivity
import com.kickstarter.libs.featureflag.FlagKey
import com.kickstarter.libs.qualifiers.RequiresActivityViewModel
import com.kickstarter.libs.rx.transformers.Transformers
import com.kickstarter.libs.transformations.CircleTransformation
import com.kickstarter.libs.utils.SwitchCompatUtils
import com.kickstarter.libs.utils.extensions.addToDisposable
import com.kickstarter.libs.utils.extensions.getEnvironment
import com.kickstarter.libs.utils.extensions.isFalse
import com.kickstarter.models.User
import com.kickstarter.ui.extensions.showSnackbar
import com.kickstarter.viewmodels.EditProfileViewModel
import com.kickstarter.viewmodels.SearchViewModel
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class EditProfileActivity : ComponentActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var viewModelFactory: EditProfileViewModel.Factory
    private val viewModel: EditProfileViewModel.EditProfileViewModel by viewModels { viewModelFactory }
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       this.getEnvironment()?.let { env ->
            viewModelFactory = EditProfileViewModel.Factory(env)
        }

        binding = ActivityEditProfileBinding.inflate(layoutInflater)

        setContentView(binding.root)

        this.viewModel.outputs.userAvatarUrl()
                .observeOn(AndroidSchedulers.mainThread())
            .subscribe { url ->
                Picasso.get().load(url).transform(CircleTransformation()).into(binding.avatarImageView)
            }.addToDisposable(disposables)

        this.viewModel.outputs.user()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { this.displayPreferences(it) }
                .addToDisposable(disposables)

        this.viewModel.outputs.userName()
                .observeOn(AndroidSchedulers.mainThread())
            .subscribe { binding.nameEditText.setText(it, TextView.BufferType.EDITABLE) }
                .addToDisposable(disposables)

        this.viewModel.outputs.hidePrivateProfileRow()
                .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.privateProfileRow.isGone = it
                binding.privateProfileTextView.isGone = it
                binding.publicProfileTextView.isGone = it
            }.addToDisposable(disposables)

        this.viewModel.unableToSavePreferenceError()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { showSnackbar(binding.newSettingsLayout, it) }
                .addToDisposable(disposables)

        binding.privateProfileSwitch.setOnClickListener {
            this.viewModel.inputs.showPublicProfile(binding.privateProfileSwitch.isChecked)
        }
    }

    private fun displayPreferences(user: User) {
        SwitchCompatUtils.setCheckedWithoutAnimation(binding.privateProfileSwitch, user.showPublicProfile().isFalse())
    }
}
