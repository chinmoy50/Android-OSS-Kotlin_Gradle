package com.kickstarter.viewmodels

import android.net.Uri
import android.util.Pair
import com.kickstarter.libs.ActivityViewModel
import com.kickstarter.libs.Environment
import com.kickstarter.libs.RefTag
import com.kickstarter.libs.rx.transformers.Transformers
import com.kickstarter.libs.utils.NumberUtils
import com.kickstarter.libs.utils.ObjectUtils
import com.kickstarter.libs.utils.Secrets
import com.kickstarter.libs.utils.UrlUtils.appendRefTag
import com.kickstarter.libs.utils.UrlUtils.refTag
import com.kickstarter.libs.utils.extensions.isProjectPreviewUri
import com.kickstarter.libs.utils.extensions.isProjectUri
import com.kickstarter.models.Update
import com.kickstarter.services.ApiClientType
import com.kickstarter.ui.IntentKey
import com.kickstarter.ui.activities.UpdateActivity
import com.kickstarter.ui.intentmappers.ProjectIntentMapper
import okhttp3.Request
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject

interface UpdateViewModel {
    interface Inputs {
        /** Call when an external link has been activated.  */
        fun externalLinkActivated()

        /** Call when a project update comments uri request has been made.  */
        fun goToCommentsRequest(request: Request)

        /** Call when a project uri request has been made.  */
        fun goToProjectRequest(request: Request)

        /** Call when a project update uri request has been made.  */
        fun goToUpdateRequest(request: Request)

        /** Call when the share button is clicked.  */
        fun shareIconButtonClicked()
    }

    interface Outputs {
        /** Emits a project url to open externally.  */
        fun openProjectExternally(): Observable<String>

        /** Emits when we should start the share intent to show the share sheet.  */
        fun startShareIntent(): Observable<Pair<Update, String>>

        /** Emits an update to start the comments activity with.  */
        fun startRootCommentsActivity(): Observable<Update>

        /** Emits a Uri and a ref tag to start the project activity with.  */
        fun startProjectActivity(): Observable<Pair<Uri, RefTag>>

        /** Emits a string to display in the toolbar title.  */
        fun updateSequence(): Observable<String>

        /** Emits a url to load in the web view.  */
        fun webViewUrl(): Observable<String>
    }

    class ViewModel(environment: Environment) : ActivityViewModel<UpdateActivity?>(environment), Inputs, Outputs {

        private val client: ApiClientType = environment.apiClient()
        private val externalLinkActivated = PublishSubject.create<Request?>()
        private val goToCommentsRequest = PublishSubject.create<Request>()
        private val goToProjectRequest = PublishSubject.create<Request>()
        private val goToUpdateRequest = PublishSubject.create<Request>()
        private val shareButtonClicked = PublishSubject.create<Void?>()
        private val openProjectExternally = PublishSubject.create<String>()
        private val startShareIntent = PublishSubject.create<Pair<Update, String>>()
        private val startRootCommentsActivity = PublishSubject.create<Update>()
        private val startProjectActivity = PublishSubject.create<Pair<Uri, RefTag>>()
        private val updateSequence = BehaviorSubject.create<String>()
        private val webViewUrl = BehaviorSubject.create<String>()
        private val deedLinkToRootComment = BehaviorSubject.create<Boolean>()

        @JvmField
        val inputs: Inputs = this

        @JvmField
        val outputs: Outputs = this

        init {
            val initialUpdate = intent()
                .map { it.getParcelableExtra(IntentKey.UPDATE) as? Update? }

            val project = intent()
                .flatMap {
                    ProjectIntentMapper
                        .project(it, client)
                        .compose(Transformers.neverError())
                }
                .share()

            val initialUpdateUrl = initialUpdate
                ?.map {
                    it?.urls()?.web()?.update()
                }

            val deepLinkUpdate = intent()
                .map { it.getStringExtra(IntentKey.UPDATE_POST_ID) }
                .filter { ObjectUtils.isNotNull(it) }
                .compose(Transformers.combineLatestPair(project))
                .map {
                    Pair(requireNotNull(it.second.slug()), requireNotNull(it.first))
                }
                .switchMap {
                    client.fetchUpdate(it.first, it.second).compose(Transformers.neverError())
                }
                .share()

            val anotherUpdateUrl = goToUpdateRequest
                .map { it.url.toString() }

            val deepLinkUrl = deepLinkUpdate
                .map { it.urls().web().update() }

            Observable.merge(initialUpdateUrl, anotherUpdateUrl, deepLinkUrl)
                .distinctUntilChanged()
                .filter { ObjectUtils.isNotNull(it) }
                .compose(bindToLifecycle())
                .subscribe {
                    it?.let { url ->
                        webViewUrl.onNext(url)
                    }
                }

            val anotherUpdate = goToUpdateRequest
                .map { projectUpdateParams(it) }
                .switchMap {
                    client.fetchUpdate(it.first, it.second).compose(Transformers.neverError())
                }
                .share()

            val currentUpdate = Observable.merge(initialUpdate, anotherUpdate, deepLinkUpdate)
                .filter {
                    ObjectUtils.isNotNull(it)
                }

            currentUpdate
                .compose(Transformers.takeWhen(shareButtonClicked))
                .map {
                    it?.let { update ->
                        Pair.create(
                            update,
                            appendRefTag(update.urls().web().update(), RefTag.updateShare().tag())
                        )
                    }
                }
                .compose(bindToLifecycle())
                .subscribe { startShareIntent.onNext(it) }

            intent()
                .map {
                    it.getBooleanExtra(IntentKey.IS_UPDATE_COMMENT, false)
                }
                .subscribe {
                    deedLinkToRootComment.onNext(it)
                }

            currentUpdate
                .withLatestFrom(deedLinkToRootComment) { update, isDeeplinkForComment -> Pair(update, isDeeplinkForComment) }
                .filter { it.second }
                .distinctUntilChanged()
                .compose(bindToLifecycle())
                .subscribe {
                    startRootCommentsActivity.onNext(it.first)
                    deedLinkToRootComment.onNext(false)
                }

            currentUpdate
                .compose(Transformers.takeWhen(goToCommentsRequest))
                .compose(bindToLifecycle())
                .subscribe { startRootCommentsActivity.onNext(it) }

            currentUpdate
                .map { it?.sequence()?.let { it1 -> NumberUtils.format(it1) } }
                .compose(bindToLifecycle())
                .subscribe { updateSequence.onNext(it) }

            goToProjectRequest
                .map { Uri.parse(it.url.toUri().toString()) }
                .filter { it.isProjectUri(Secrets.WebEndpoint.PRODUCTION) }
                .filter { !it.isProjectPreviewUri(Secrets.WebEndpoint.PRODUCTION) }
                .compose(bindToLifecycle())
                .subscribe {
                    startProjectActivity.onNext(
                        Pair.create(
                            it,
                            RefTag.update()
                        )
                    )
                }

            goToProjectRequest
                .map { Uri.parse(it.url.toUri().toString()) }
                .filter { it.isProjectUri(Secrets.WebEndpoint.PRODUCTION) }
                .filter { it.isProjectPreviewUri(Secrets.WebEndpoint.PRODUCTION) }
                .map { it.toString() }
                .map {
                    if (refTag(it) == null)
                        appendRefTag(it, RefTag.update().tag())
                    else
                        it
                }
                .compose(bindToLifecycle())
                .subscribe { openProjectExternally.onNext(it) }
        }

        /**
         * Parses a request for project and update params.
         *
         * @param request   Comments or update request.
         * @return Pair of project param string and update param string.
         */
        private fun projectUpdateParams(request: Request): Pair<String, String> {
            // todo: build a Navigation helper for better param extraction
            val projectParam = request.url.encodedPathSegments[2]
            val updateParam = request.url.encodedPathSegments[4]
            return Pair.create(projectParam, updateParam)
        }

        override fun externalLinkActivated() = externalLinkActivated.onNext(null)

        override fun goToCommentsRequest(request: Request) = goToCommentsRequest.onNext(request)

        override fun goToProjectRequest(request: Request) = goToProjectRequest.onNext(request)

        override fun goToUpdateRequest(request: Request) = goToUpdateRequest.onNext(request)

        override fun shareIconButtonClicked() = shareButtonClicked.onNext(null)

        override fun openProjectExternally(): Observable<String> = openProjectExternally

        override fun startShareIntent(): Observable<Pair<Update, String>> = startShareIntent

        override fun startRootCommentsActivity(): Observable<Update> = startRootCommentsActivity

        override fun startProjectActivity(): Observable<Pair<Uri, RefTag>> = startProjectActivity

        override fun updateSequence(): Observable<String> = updateSequence

        override fun webViewUrl(): Observable<String> = webViewUrl
    }
}
