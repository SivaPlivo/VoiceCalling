package com.plivo.voicecalling.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.plivo.endpoint.Endpoint;
import com.plivo.endpoint.Incoming;
import com.plivo.endpoint.Outgoing;
import com.plivo.voicecalling.Helpers.EndPointListner;
import com.plivo.voicecalling.Helpers.Phone;
import com.plivo.voicecalling.R;

//Siva Cherukuri

public class LoginActivity extends AppCompatActivity implements EndPointListner{

    public Endpoint endpoint;

    SharedPreferences sharedPreferences;

    EditText username,password;
    Button loginBtn;
    TextView terminalLogTextView;

    String usernameStr, passwordStr;

    ProgressDialog progress;

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    public String fireBaseToken;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("key");
            Log.d("MainActivity", "Refreshed token: " + message);
            fireBaseToken = message;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.requestAppPermissions();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("intentKey"));

        try {
            final String refreshToken = FirebaseInstanceId.getInstance().getToken();
            Log.d("MainActivity", "Refreshed token: " + refreshToken);
            if (refreshToken != null) {
                fireBaseToken = refreshToken;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        progress = new ProgressDialog(LoginActivity.this);

        username = (EditText) findViewById(R.id.editText1);
        password = (EditText) findViewById(R.id.editText2);
        loginBtn = (Button) findViewById(R.id.loginBtn);

        terminalLogTextView = (TextView)findViewById(R.id.terminalOutput);
        terminalLogTextView.setMovementMethod(new ScrollingMovementMethod());
        terminalLogTextView.setBackgroundColor(1);
        terminalLogTextView.append("\n Init");


        endpoint = Phone.getInstance(this).endpoint;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        usernameStr = sharedPreferences.getString("username","");

        if (usernameStr.length() > 0) {

            passwordStr = sharedPreferences.getString("password","");

            this.showProgressView();

            Phone.getInstance(this).login(usernameStr, passwordStr);

        }

    }

    public void showProgressView()
    {
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
    }

    public void sendRegistrationToServer(final String token) {

        Log.d("Device Token ","is:"+token);
        Log.v("Device Token ","is:"+token);
        Log.e("Device Token ","is:"+token);

        Toast.makeText(this, "Token: "+token, Toast.LENGTH_SHORT).show();

        try {

            Phone.getInstance(this).registerToken(token);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void login(View view) {

        if (username.getText().toString().equals("") || password.getText().toString().equals("")) {

            Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show();

        } else {

            Log.d("PlivoInbound", "Trying to log in");

            this.showProgressView();
            usernameStr = username.getText().toString();
            passwordStr = password.getText().toString();
            Phone.getInstance(this).login(usernameStr, passwordStr);
        }
    }

    //Endpoint Listeners

    public void onLogin() {

        Log.d("PlivoInbound", "Logging in");

        LoginActivity.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                terminalLogTextView.append("\n Login Success");

                sendRegistrationToServer(fireBaseToken);

                // To dismiss the dialog
                progress.dismiss();

                sharedPreferences.edit().putString("username", usernameStr).apply();
                sharedPreferences.edit().putString("password", passwordStr).apply();

            }
        });

        Log.d("Listner","Phone");

        Intent intent = new Intent(this, VoiceActivity.class);
        startActivity(intent);

    }

    public void onLogout() {

        Log.d("PlivoInbound", "Logged out");

    }

    public void onLoginFailed() {

        Log.d("PlivoInbound", "Login failed");

    }

    public void onIncomingCall(Incoming incoming) {

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

    public  void  requestAppPermissions()
    {
        if (Build.VERSION.SDK_INT < 23) {
            // your code

        } else {

            requestPermissions(new String[]{
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.RECORD_AUDIO,Manifest.permission.MODIFY_AUDIO_SETTINGS,
                            Manifest.permission.PROCESS_OUTGOING_CALLS,Manifest.permission.WRITE_SETTINGS,
                            Manifest.permission.READ_PHONE_STATE,Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.WAKE_LOCK,
                            Manifest.permission.VIBRATE,Manifest.permission.READ_LOGS,Manifest.permission.USE_SIP,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }

    }
}
