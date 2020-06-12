package com.jesiontrop.justchatting.base.utils.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFMService";
    private static final String CHAT_ENGAGE_TOPIC = "chat_engage";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "FCM MessageId: " + remoteMessage.getMessageId());
        Log.d(TAG, "FCM Notification Message: " + remoteMessage.getNotification());
        Log.d(TAG, "FCM Data Message: " + remoteMessage.getData());
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.d(TAG, "FCM Token: " + s);

        FirebaseMessaging.getInstance().subscribeToTopic(CHAT_ENGAGE_TOPIC);
    }
}
