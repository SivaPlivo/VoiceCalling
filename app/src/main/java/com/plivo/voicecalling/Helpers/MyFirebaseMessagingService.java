package com.plivo.voicecalling.Helpers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.plivo.endpoint.Incoming;
import com.plivo.endpoint.Outgoing;
import com.plivo.voicecalling.Activities.VoiceActivity;
import com.plivo.voicecalling.R;

import java.util.Map;

/**
 * Created by Siva on 19/06/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService implements EndPointListner {

    private static final String TAG = "MyFirebaseMsgService";

    public Map<String, String> notification;
    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        // Check if message contains a notification payload.
        if (remoteMessage.getData() != null)
        {
            Log.d(TAG, "Endpoint is: (0) " + Phone.getInstance(this).endpoint);

            Log.d(TAG, "Message Data (1) is: " + remoteMessage.getData());
            Log.d(TAG, "Endpoint is: (1) " + Phone.getInstance(this).endpoint);

            notification = remoteMessage.getData();

            if(Phone.getInstance(this).endpoint.getRegistered())
            {
                Phone.getInstance(this).relayVOIPNotification(remoteMessage.getData());
            }
            else
            {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

                String usernameStr = sharedPreferences.getString("username","");

                String passwordStr = sharedPreferences.getString("password","");

                Phone.getInstance(this).login(usernameStr, passwordStr);

                Log.d(TAG, "Endpoint is: (2) " + Phone.getInstance(this).endpoint);

            }

            Log.d(TAG, "Endpoint is: (3) " + Phone.getInstance(this).endpoint);

        }else{

            Log.d(TAG, "Message Data (2) is: " + remoteMessage.getData());

        }

    }


    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String title,String messageBody) {

            Intent intent = new Intent(this, VoiceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Plivo")
                    .setContentText("Incoming Call")
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());


    }

    public void onLogin() {

        Log.d("PlivoInbound", "Logging in");

        Phone.getInstance(this).relayVOIPNotification(notification);

        sendNotification("Plivo Notification","");


    }

    public void onLogout() {

        Log.d("PlivoInbound", "Logged out");

    }

    public void onLoginFailed() {

        Log.d("PlivoInbound", "Login failed");

    }

    public void onIncomingCall(Incoming incoming) {

        PowerManager.WakeLock screenOn = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "example");
        screenOn.acquire();

        Log.d("FCM", "Incoming call received");

        Phone.getInstance(this).incoming = incoming;

        Intent intent = new Intent(VoiceActivity.ACTION_INCOMING_CALL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);


//            Intent intent = new Intent(this, IncomingActivity.class);
//            this.startActivity(intent);

    }

    public void onIncomingCallHangup(Incoming incoming) {

    }

    public void onIncomingCallRejected(Incoming incoming) {

    }

    public void onOutgoingCall(Outgoing outgoing) {

    }

    public void onOutgoingCallAnswered(Outgoing outgoing) {

    }

    public void onOutgoingCallRejected(Outgoing outgoing) {

    }

    public void onOutgoingCallHangup(Outgoing outgoing) {

    }

    public void onOutgoingCallInvalid(Outgoing outgoing){

    }

    public void onIncomingDigitNotification(String digits) {

    }
}
