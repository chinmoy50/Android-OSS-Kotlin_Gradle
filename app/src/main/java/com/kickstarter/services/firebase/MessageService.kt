package com.kickstarter.services.firebase

import android.text.TextUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.kickstarter.KSApplication
import com.kickstarter.R
import com.kickstarter.libs.PushNotifications
import com.kickstarter.libs.braze.RemotePushClientType
import com.kickstarter.models.pushdata.Activity
import com.kickstarter.models.pushdata.GCM
import com.kickstarter.services.ApiClientTypeV2
import com.kickstarter.services.apiresponses.PushNotificationEnvelope
import com.kickstarter.services.apiresponses.PushNotificationEnvelope.Companion.builder
import com.kickstarter.services.apiresponses.PushNotificationEnvelope.ErroredPledge
import com.kickstarter.services.apiresponses.PushNotificationEnvelope.Survey
import timber.log.Timber
import javax.inject.Inject

class MessageService : FirebaseMessagingService() {
    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var pushNotifications: PushNotifications

    @Inject
    lateinit var remotePushClientType: RemotePushClientType

    @Inject
    lateinit var apiClient: ApiClientTypeV2

    override fun onNewToken(s: String) {
        apiClient.registerPushToken(s)
        super.onNewToken(s)
    }

    override fun onCreate() {
        super.onCreate()
        (applicationContext as KSApplication).component().inject(this)
    }

    /**
     * Called when a message is received from Firebase.
     * - If the message comes from braze it will be handle on:
     * - @see RemotePushClientType#handleRemoteMessages(android.content.Context, com.google.firebase.messaging.RemoteMessage)
     * - If the message is from Kickstarter it will be process here
     *
     * @param remoteMessage Object containing message information.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val isBrazeMessage =
            remotePushClientType.handleRemoteMessages(this, remoteMessage)

        if (!isBrazeMessage) {
            val senderId = getString(R.string.gcm_defaultSenderId)
            val from = remoteMessage.from
            if (!TextUtils.equals(from, senderId)) {
                Timber.e("Received a message from %s, expecting %s", from, senderId)
                return
            }

            val data = remoteMessage.data

            val envelope = builder()
                .activity(
                    gson.fromJson(
                        data["activity"], Activity::class.java
                    )
                )
                .erroredPledge(gson.fromJson(data["errored_pledge"], ErroredPledge::class.java))
                .gcm(gson.fromJson(data["gcm"], GCM::class.java))
                .message(
                    gson.fromJson(
                        data["message"], PushNotificationEnvelope.Message::class.java
                    )
                )
                .project(
                    gson.fromJson(
                        data["project"], PushNotificationEnvelope.Project::class.java
                    )
                )
                .survey(gson.fromJson(data["survey"], Survey::class.java))
                .build()

            Timber.d("Received message: %s", envelope.toString())
            pushNotifications.add(envelope)
        }
    }
}
