package com.kickstarter.services

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.kickstarter.libs.utils.ObjectUtils
import com.kickstarter.models.Location
import com.kickstarter.models.Project
import com.kickstarter.models.Reward
import com.kickstarter.models.StoredCard
import com.kickstarter.models.UserPrivacy
import com.kickstarter.services.apiresponses.ShippingRulesEnvelope
import com.kickstarter.services.mutations.SavePaymentMethodData
import com.kickstarter.services.transformers.complexRewardItemsTransformer
import com.kickstarter.services.transformers.encodeRelayId
import com.kickstarter.services.transformers.projectTransformer
import com.kickstarter.services.transformers.rewardTransformer
import com.kickstarter.services.transformers.shippingRulesListTransformer
import com.kickstarter.services.transformers.userTransformer
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import type.CurrencyCode
import type.FlaggingKind

interface ApolloClientTypeV2 {
    fun getProject(project: Project): Observable<Project>
    fun getProject(slug: String): Observable<Project>
    fun createSetupIntent(project: Project? = null): Observable<String>
    fun savePaymentMethod(savePaymentMethodData: SavePaymentMethodData): Observable<StoredCard>
    fun getStoredCards(): Observable<List<StoredCard>>
    fun deletePaymentSource(paymentSourceId: String): Observable<DeletePaymentSourceMutation.Data>
    fun createFlagging(project: Project? = null, details: String, flaggingKind: String): Observable<String>
    fun userPrivacy(): Observable<UserPrivacy>
    fun watchProject(project: Project): Observable<Project>
    fun unWatchProject(project: Project): Observable<Project>
    fun updateUserPassword(currentPassword: String = "", newPassword: String, confirmPassword: String): Observable<UpdateUserPasswordMutation.Data>
    fun updateUserEmail(email: String, currentPassword: String): Observable<UpdateUserEmailMutation.Data>
    fun sendVerificationEmail(): Observable<SendEmailVerificationMutation.Data>
    fun updateUserCurrencyPreference(currency: CurrencyCode): Observable<UpdateUserCurrencyMutation.Data>
    fun getShippingRules(reward: Reward): Observable<ShippingRulesEnvelope>
    fun getProjectAddOns(slug: String, locationId: Location): Observable<List<Reward>>
}

class KSApolloClientV2(val service: ApolloClient) : ApolloClientTypeV2 {
    override fun getProject(project: Project): Observable<Project> {
        return getProject(project.slug() ?: "")
    }
    override fun getProject(slug: String): Observable<Project> {
        return Observable.defer {
            val ps = PublishSubject.create<Project>()
            this.service.query(
                FetchProjectQuery.builder()
                    .slug(slug)
                    .build()
            ).enqueue(object : ApolloCall.Callback<FetchProjectQuery.Data>() {
                override fun onFailure(e: ApolloException) {
                    ps.onError(e)
                }

                override fun onResponse(response: Response<FetchProjectQuery.Data>) {
                    if (response.hasErrors()) ps.onError(java.lang.Exception(response.errors?.first()?.message))
                    else {
                        response.data?.let { responseData ->
                            ps.onNext(
                                projectTransformer(
                                    responseData.project()?.fragments()?.fullProject()
                                )
                            )
                        }
                        ps.onComplete()
                    }
                }
            })
            return@defer ps
        }.subscribeOn(Schedulers.io())
    }

    override fun createSetupIntent(project: Project?): Observable<String> {
        return Observable.defer {
            val createSetupIntentMut = CreateSetupIntentMutation.builder()
                .apply {
                    if (project != null) this.projectId(encodeRelayId(project))
                }
                .build()

            val ps = PublishSubject.create<String>()
            this.service.mutate(createSetupIntentMut)
                .enqueue(object : ApolloCall.Callback<CreateSetupIntentMutation.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<CreateSetupIntentMutation.Data>) {
                        if (response.hasErrors()) ps.onError(java.lang.Exception(response.errors?.first()?.message))
                        else {
                            ps.onNext(response.data?.createSetupIntent()?.clientSecret() ?: "")
                        }
                        ps.onComplete()
                    }
                })
            return@defer ps
        }
    }

    override fun savePaymentMethod(savePaymentMethodData: SavePaymentMethodData): Observable<StoredCard> {
        return Observable.defer {
            val ps = PublishSubject.create<StoredCard>()
            service.mutate(
                SavePaymentMethodMutation.builder()
                    .paymentType(savePaymentMethodData.paymentType)
                    .stripeToken(savePaymentMethodData.stripeToken)
                    .stripeCardId(savePaymentMethodData.stripeCardId)
                    .reusable(savePaymentMethodData.reusable)
                    .intentClientSecret(savePaymentMethodData.intentClientSecret)
                    .build()
            )
                .enqueue(object : ApolloCall.Callback<SavePaymentMethodMutation.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<SavePaymentMethodMutation.Data>) {
                        if (response.hasErrors()) {
                            ps.onError(Exception(response.errors?.first()?.message))
                        }

                        val paymentSource = response.data?.createPaymentSource()?.paymentSource()
                        paymentSource?.let {
                            val storedCard = StoredCard.builder()
                                .expiration(it.expirationDate())
                                .id(it.id())
                                .lastFourDigits(it.lastFour())
                                .type(it.type())
                                .build()
                            ps.onNext(storedCard)
                        }
                        ps.onComplete()
                    }
                })
            return@defer ps
        }
    }

    override fun getStoredCards(): Observable<List<StoredCard>> {
        return Observable.defer {
            val ps = PublishSubject.create<List<StoredCard>>()
            this.service.query(UserPaymentsQuery.builder().build())
                .enqueue(object : ApolloCall.Callback<UserPaymentsQuery.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<UserPaymentsQuery.Data>) {
                        if (response.hasErrors()) {
                            ps.onError(Exception(response.errors?.first()?.message))
                        } else {
                            val cardsList = mutableListOf<StoredCard>()
                            response.data?.me()?.storedCards()?.nodes()?.map {
                                it?.let { cardData ->
                                    val card = StoredCard.builder()
                                        .expiration(cardData.expirationDate())
                                        .id(cardData.id())
                                        .lastFourDigits(cardData.lastFour())
                                        .type(it.type())
                                        .build()
                                    cardsList.add(card)
                                }
                            }
                            ps.onNext(cardsList)
                            ps.onComplete()
                        }
                    }
                })
            return@defer ps
        }
    }

    override fun deletePaymentSource(paymentSourceId: String): Observable<DeletePaymentSourceMutation.Data> {
        return Observable.defer {
            val ps = PublishSubject.create<DeletePaymentSourceMutation.Data>()
            service.mutate(
                DeletePaymentSourceMutation.builder()
                    .paymentSourceId(paymentSourceId)
                    .build()
            )
                .enqueue(object : ApolloCall.Callback<DeletePaymentSourceMutation.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<DeletePaymentSourceMutation.Data>) {
                        if (response.hasErrors()) {
                            ps.onError(Exception(response.errors?.first()?.message))
                        }

                        response.data?.let { ps.onNext(it) }
                        ps.onComplete()
                    }
                })
            return@defer ps
        }
    }

    override fun createFlagging(project: Project?, details: String, flaggingKind: String): Observable<String> {
        return Observable.defer {
            project?.let {
                val ps = PublishSubject.create<String>()
                val flagging = FlaggingKind.safeValueOf(flaggingKind)
                val mutation = CreateFlaggingMutation.builder()
                    .contentId(encodeRelayId(it))
                    .details(details)
                    .kind(flagging)
                    .build()

                service.mutate(
                    mutation
                ).enqueue(object : ApolloCall.Callback<CreateFlaggingMutation.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<CreateFlaggingMutation.Data>) {
                        if (response.hasErrors()) {
                            ps.onError(Exception(response.errors?.first()?.message))
                        }
                        response.data?.let { data ->
                            data.createFlagging()?.flagging()?.kind()?.name?.let { kindString ->
                                ps.onNext(kindString)
                            }
                        }
                        ps.onComplete()
                    }
                })
                return@defer ps
            }
        }
    }

    override fun userPrivacy(): Observable<UserPrivacy> {
        return Observable.defer {
            val ps = PublishSubject.create<UserPrivacy>()
            service.query(UserPrivacyQuery.builder().build())
                .enqueue(object : ApolloCall.Callback<UserPrivacyQuery.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<UserPrivacyQuery.Data>) {
                        response.data?.me()?.let {
                            val defaultCurrency = "USD"
                            val userPrivacy = UserPrivacy(
                                name = it.name(),
                                email = it.email() ?: "",
                                hasPassword = it.hasPassword() ?: false,
                                isCreator = it.isCreator ?: false,
                                isDeliverable = it.isDeliverable ?: false,
                                isEmailVerified = it.isEmailVerified ?: false,
                                chosenCurrency = it.chosenCurrency() ?: defaultCurrency
                            )
                             ps.onNext(userPrivacy)

                        }
                        ps.onComplete()
                    }
                })
            return@defer ps
        }
    }

    override fun watchProject(project: Project): Observable<Project> {
        return Observable.defer {
            val ps = PublishSubject.create<Project>()
            this.service.mutate(
                WatchProjectMutation.builder().id(encodeRelayId(project)).build()
            )
                .enqueue(object : ApolloCall.Callback<WatchProjectMutation.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<WatchProjectMutation.Data>) {
                        if (response.hasErrors()) {
                            ps.onError(java.lang.Exception(response.errors?.first()?.message))
                        }
                        /* make a copy of what you posted. just in case
                         * we want to update the list without doing
                         * a full refresh.
                         */
                        ps.onNext(
                            projectTransformer(
                                response.data?.watchProject()?.project()?.fragments()?.fullProject()
                            )
                        )
                        ps.onComplete()
                    }
                })
            return@defer ps
        }
    }

    override fun unWatchProject(project: Project): Observable<Project> {
        return Observable.defer {
            val ps = PublishSubject.create<Project>()
            this.service.mutate(
                UnwatchProjectMutation.builder().id(encodeRelayId(project)).build()
            )
                .enqueue(object : ApolloCall.Callback<UnwatchProjectMutation.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<UnwatchProjectMutation.Data>) {
                        if (response.hasErrors()) {
                            ps.onError(java.lang.Exception(response.errors?.first()?.message))
                        }
                        /* make a copy of what you posted. just in case
                         * we want to update the list without doing
                         * a full refresh.
                         */
                        ps.onNext(
                            projectTransformer(
                                response.data?.watchProject()?.project()?.fragments()?.fullProject()
                            )
                        )
                        ps.onComplete()
                    }
                })
            return@defer ps
        }
    }

    override fun updateUserPassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Observable<UpdateUserPasswordMutation.Data> {
        return Observable.defer {
            val ps = PublishSubject.create<UpdateUserPasswordMutation.Data>()
            service.mutate(
                UpdateUserPasswordMutation.builder()
                    .currentPassword(currentPassword)
                    .password(newPassword)
                    .passwordConfirmation(confirmPassword)
                    .build()
            )
                .enqueue(object : ApolloCall.Callback<UpdateUserPasswordMutation.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<UpdateUserPasswordMutation.Data>) {
                        if (response.hasErrors()) {
                            ps.onError(Exception(response.errors?.first()?.message))
                        }
                        response.data?.let {
                            ps.onNext(it)
                        }

                        ps.onComplete()
                    }
                })
            return@defer ps
        }
    }

    override fun updateUserEmail(
        email: String,
        currentPassword: String
    ): Observable<UpdateUserEmailMutation.Data> {
        return Observable.defer {
            val ps = PublishSubject.create<UpdateUserEmailMutation.Data>()
            service.mutate(
                UpdateUserEmailMutation.builder()
                    .email(email)
                    .currentPassword(currentPassword)
                    .build()
            )
                .enqueue(object : ApolloCall.Callback<UpdateUserEmailMutation.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<UpdateUserEmailMutation.Data>) {
                        if (response.hasErrors()) {
                            ps.onError(Exception(response.errors?.first()?.message))
                        }
                        response.data?.let { data ->
                            ps.onNext(data)
                        }
                        ps.onComplete()
                    }
                })
            return@defer ps
        }
    }

    override fun sendVerificationEmail(): Observable<SendEmailVerificationMutation.Data> {
        return Observable.defer {
            val ps = PublishSubject.create<SendEmailVerificationMutation.Data>()
            service.mutate(
                SendEmailVerificationMutation.builder()
                    .build()
            )
                .enqueue(object : ApolloCall.Callback<SendEmailVerificationMutation.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<SendEmailVerificationMutation.Data>) {
                        if (response.hasErrors()) {
                            ps.onError(Exception(response.errors?.first()?.message))
                        }
                        response.data?.let { data ->
                            ps.onNext(data)
                        }
                        ps.onComplete()
                    }
                })
            return@defer ps
        }
    }

    override fun updateUserCurrencyPreference(currency: CurrencyCode): Observable<UpdateUserCurrencyMutation.Data> {
        return Observable.defer {
            val ps = PublishSubject.create<UpdateUserCurrencyMutation.Data>()
            service.mutate(
                UpdateUserCurrencyMutation.builder()
                    .chosenCurrency(currency)
                    .build()
            )
                .enqueue(object : ApolloCall.Callback<UpdateUserCurrencyMutation.Data>() {
                    override fun onFailure(exception: ApolloException) {
                        ps.onError(exception)
                    }

                    override fun onResponse(response: Response<UpdateUserCurrencyMutation.Data>) {
                        if (response.hasErrors()) {
                            ps.onError(Exception(response.errors?.first()?.message))
                        }
                        response.data?.let {
                            ps.onNext(it)
                        }
                        ps.onComplete()
                    }
                })
            return@defer ps
        }
    }

    override fun getShippingRules(reward: Reward): Observable<ShippingRulesEnvelope> {
        return Observable.defer {
            val ps = PublishSubject.create<ShippingRulesEnvelope>()

            this.service.query(
                GetShippingRulesForRewardIdQuery.builder()
                    .rewardId(encodeRelayId(reward))
                    .build()
            )
                .enqueue(object : ApolloCall.Callback<GetShippingRulesForRewardIdQuery.Data>() {
                    override fun onFailure(e: ApolloException) {
                        ps.onError(e)
                    }

                    override fun onResponse(response: Response<GetShippingRulesForRewardIdQuery.Data>) {
                        response.data?.let { data ->
                            Observable.just(data?.node() as? GetShippingRulesForRewardIdQuery.AsReward)
                                .filter { !it?.shippingRulesExpanded()?.nodes().isNullOrEmpty() }
                                .map {
                                    it?.shippingRulesExpanded()?.nodes()?.mapNotNull { node ->
                                        node.fragments().shippingRule()
                                    }
                                }
                                .filter { ObjectUtils.isNotNull(it) }
                                .subscribe { shippingList ->
                                    val shippingEnvelope = shippingRulesListTransformer(shippingList ?: emptyList())
                                    ps.onNext(shippingEnvelope)
                                    ps.onComplete()
                                }
                        }
                    }
                })
            return@defer ps
        }.subscribeOn(Schedulers.io())
    }

    private fun getAddOnsFromProject(addOnsGr: GetProjectAddOnsQuery.AddOns): List<Reward> {
        return addOnsGr.nodes()?.map { node ->
            val shippingRulesGr =
                node.shippingRulesExpanded()?.nodes()?.map { it.fragments().shippingRule() }
                    ?: emptyList()
            rewardTransformer(
                node.fragments().reward(),
                shippingRulesGr,
                addOnItems = complexRewardItemsTransformer(node.items()?.fragments()?.rewardItems())
            )
        }?.toList() ?: emptyList()
    }
    override fun getProjectAddOns(slug: String, locationId: Location): Observable<List<Reward>> {
        return Observable.defer {
            val ps = PublishSubject.create<List<Reward>>()

            this.service.query(
                GetProjectAddOnsQuery.builder()
                    .slug(slug)
                    .locationId(encodeRelayId(locationId))
                    .build()
            )
                .enqueue(object : ApolloCall.Callback<GetProjectAddOnsQuery.Data>() {
                    override fun onFailure(e: ApolloException) {
                        ps.onError(e)
                    }

                    override fun onResponse(response: Response<GetProjectAddOnsQuery.Data>) {
                        response.data?.let { data ->
                            rx.Observable.just(data.project()?.addOns())
                                .filter { it?.nodes() != null }
                                .map<List<Reward>> { addOnsList ->
                                    addOnsList?.let {
                                        getAddOnsFromProject(
                                            it
                                        )
                                    } ?: emptyList()
                                }
                                .subscribe {
                                    ps.onNext(it)
                                    ps.onComplete()
                                }
                        }
                    }
                })
            return@defer ps
        }
    }
}
