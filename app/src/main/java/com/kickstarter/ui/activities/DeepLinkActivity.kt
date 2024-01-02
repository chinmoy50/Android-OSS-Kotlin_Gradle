package com.kickstarter.ui.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kickstarter.R
import com.kickstarter.libs.RefTag
import com.kickstarter.libs.utils.ApplicationUtils
import com.kickstarter.libs.utils.ThirdPartyEventValues
import com.kickstarter.libs.utils.UrlUtils.commentId
import com.kickstarter.libs.utils.UrlUtils.refTag
import com.kickstarter.libs.utils.UrlUtils.saveFlag
import com.kickstarter.libs.utils.extensions.addToDisposable
import com.kickstarter.libs.utils.extensions.getEnvironment
import com.kickstarter.libs.utils.extensions.getProjectIntent
import com.kickstarter.libs.utils.extensions.path
import com.kickstarter.ui.IntentKey
import com.kickstarter.ui.extensions.startPreLaunchProjectActivity
import com.kickstarter.viewmodels.DeepLinkViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.android.schedulers.AndroidSchedulers

class DeepLinkActivity : AppCompatActivity() {

    private lateinit var viewModelFactory: DeepLinkViewModel.Factory
    private val viewModel: DeepLinkViewModel.DeepLinkViewModel by viewModels { viewModelFactory }

    private var disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setSplashScreenTheme(R.style.SplashTheme)
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                val slideUp = ObjectAnimator.ofFloat(
                    splashScreenView,
                    View.TRANSLATION_Y,
                    0f,
                    -splashScreenView.height.toFloat()
                )
                slideUp.interpolator = AnticipateInterpolator()
                slideUp.duration = 100L
            }
        }

        this.getEnvironment()?.let {
            viewModelFactory = DeepLinkViewModel.Factory(it, intent = intent)
        }

        viewModel.outputs.startBrowser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { url: String -> startBrowser(url) }
                .addToDisposable(disposables)

        viewModel.outputs.startDiscoveryActivity()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { startDiscoveryActivity() }
                .addToDisposable(disposables)

        viewModel.outputs.startProjectActivity()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { uri: Uri -> startProjectActivity(uri) }
                .addToDisposable(disposables)

        viewModel.outputs.startProjectActivityToSave()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { startProjectActivityForSave(it) }
                .addToDisposable(disposables)

        viewModel.outputs.startProjectActivityForComment()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { startProjectActivityForComment(it) }
                .addToDisposable(disposables)

        viewModel.outputs.startProjectActivityForUpdate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { startProjectActivityForUpdate(it) }
                .addToDisposable(disposables)

        viewModel.outputs.startProjectActivityForCommentToUpdate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { startProjectActivityForCommentToUpdate(it) }
                .addToDisposable(disposables)

        viewModel.outputs.startProjectActivityForCheckout()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { uri: Uri ->
                startProjectActivityForCheckout(
                    uri
                )
            }.addToDisposable(disposables)

        viewModel.outputs.finishDeeplinkActivity()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { finish() }
                .addToDisposable(disposables)

        viewModel.outputs.startPreLaunchProjectActivity()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                startPreLaunchProjectActivity(it, "DEEPLINK")
            }.addToDisposable(disposables)
    }

    private fun projectIntent(uri: Uri): Intent {
        val projectIntent = Intent().getProjectIntent(this)
            .setData(uri)
            .putExtra(IntentKey.PREVIOUS_SCREEN, ThirdPartyEventValues.ScreenName.DEEPLINK.value)
        val ref = refTag(uri.toString())
        if (ref != null) {
            projectIntent.putExtra(IntentKey.REF_TAG, RefTag.from(ref))
        }
        return projectIntent
    }

    private fun startDiscoveryActivity() {
        ApplicationUtils.startNewDiscoveryActivity(this)
        finish()
    }

    private fun startProjectActivity(uri: Uri) {
        startActivity(projectIntent(uri))
        finish()
    }

    private fun startProjectActivityForSave(uri: Uri) {
        val projectIntent = Intent().getProjectIntent(this)
            .setData(uri)
            .putExtra(IntentKey.DEEP_LINK_SCREEN_PROJECT_SAVE, true)
            .putExtra(IntentKey.PREVIOUS_SCREEN, ThirdPartyEventValues.ScreenName.DEEPLINK.value)

        saveFlag(uri.toString())?.let {
            projectIntent.putExtra(IntentKey.SAVE_FLAG_VALUE, it)
        }

        startActivity(projectIntent)
        finish()
    }

    private fun startProjectActivityForComment(uri: Uri) {
        val projectIntent = projectIntent(uri)
            .putExtra(IntentKey.DEEP_LINK_SCREEN_PROJECT_COMMENT, true)

        commentId(uri.toString())?.let {
            projectIntent.putExtra(IntentKey.COMMENT, it)
        }

        startActivity(projectIntent)
        finish()
    }

    private fun startProjectActivityForCommentToUpdate(uri: Uri) {
        val path = uri.path().split("/")

        val projectIntent = projectIntent(uri)
            .putExtra(IntentKey.DEEP_LINK_SCREEN_PROJECT_UPDATE, path[path.lastIndex - 1])
            .putExtra(IntentKey.DEEP_LINK_SCREEN_PROJECT_UPDATE_COMMENT, true)

        commentId(uri.toString())?.let {
            projectIntent.putExtra(IntentKey.COMMENT, it)
        }

        startActivity(projectIntent)
        finish()
    }

    private fun startProjectActivityForUpdate(uri: Uri) {
        val projectIntent = projectIntent(uri)
            .putExtra(IntentKey.DEEP_LINK_SCREEN_PROJECT_UPDATE, uri.lastPathSegment)

        startActivity(projectIntent)
        finish()
    }

    private fun startProjectActivityForCheckout(uri: Uri) {
        val projectIntent = projectIntent(uri)
            .putExtra(IntentKey.EXPAND_PLEDGE_SHEET, true)
        startActivity(projectIntent)
        finish()
    }

    private fun startBrowser(url: String) {
        ApplicationUtils.openUrlExternally(this, url)
        finish()
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }
}
