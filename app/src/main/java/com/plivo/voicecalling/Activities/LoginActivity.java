package com.plivo.voicecalling.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.AuthCredInfoVector;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.StringVector;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;

public class LoginActivity extends AppCompatActivity implements EndPointListner, Handler.Callback, MyAppObserver{

    public Endpoint endpoint;

    SharedPreferences sharedPreferences;

    EditText username,password;
    Button loginBtn;
    TextView terminalLogTextView;

    String usernameStr, passwordStr;

    ProgressDialog progress;

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    public String fireBaseToken;

    public static MyApp app = null;
    public static MyCall currentCall = null;
    public static MyAccount account = null;
    public static AccountConfig accCfg = null;

    private final Handler handler = new Handler(this);

    public class MSG_TYPE
    {
        public final static int INCOMING_CALL = 1;
        public final static int CALL_STATE = 2;
        public final static int REG_STATE = 3;
        public final static int BUDDY_STATE = 4;
        public final static int CALL_MEDIA_STATE = 5;
        public final static int CHANGE_NETWORK = 6;
    }

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

        if (app == null) {
            app = new MyApp();
            // Wait for GDB to init, for native debugging only
            if (false &&
                    (getApplicationInfo().flags &
                            ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
            }

            app.init(this, getFilesDir().getAbsolutePath());
        }

        if (app.accList.size() == 0) {
            accCfg = new AccountConfig();
            accCfg.setIdUri("sip:localhost");
            accCfg.getNatConfig().setIceEnabled(true);
            accCfg.getVideoConfig().setAutoTransmitOutgoing(true);
            accCfg.getVideoConfig().setAutoShowIncoming(true);
            account = app.addAcc(accCfg);
        } else {
            account = app.accList.get(0);
            accCfg = account.cfg;
        }

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

            dlgAccountSetting();

        }

    }

    @Override
    public boolean handleMessage(Message m)
    {
        if (m.what == 0) {

            app.deinit();
            finish();
            Runtime.getRuntime().gc();
            android.os.Process.killProcess(android.os.Process.myPid());

        } else if (m.what == MSG_TYPE.CALL_STATE) {



        } else if (m.what == MSG_TYPE.BUDDY_STATE) {



        } else if (m.what == MSG_TYPE.REG_STATE) {

            String msg_str = (String) m.obj;

            Log.d("Message string: ",msg_str);

        } else if (m.what == MSG_TYPE.INCOMING_CALL) {

	    /* Incoming call */
            final MyCall call = (MyCall) m.obj;
            CallOpParam prm = new CallOpParam();

	    /* Only one call at anytime */
            if (currentCall != null) {
		/*
		prm.setStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);
		try {
		call.hangup(prm);
		} catch (Exception e) {}
		*/
                // TODO: set status code
                call.delete();
                return true;
            }

	    /* Answer with ringing */
            prm.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
            try {
                call.answer(prm);
            } catch (Exception e) {}

            currentCall = call;

        } else if (m.what == MSG_TYPE.CHANGE_NETWORK) {
            app.handleNetworkChange();
        } else {

	    /* Message not handled */
            return false;

        }

        return true;
    }

    private void dlgAccountSetting()
    {
        String acc_id 	 = "sip:snsone170720062259@phone.test.plivo.com";
        String registrar = "sip:phone.test.plivo.com";
        String proxy 	 = "";
        String username  = "snsone170720062259";
        String password  = "12345";

        accCfg.setIdUri(acc_id);
        accCfg.getRegConfig().setRegistrarUri(registrar);
        AuthCredInfoVector creds = accCfg.getSipConfig().
                getAuthCreds();
        creds.clear();
        if (username.length() != 0) {
            creds.add(new AuthCredInfo("Digest", "*", username, 0,
                    password));
        }
        StringVector proxies = accCfg.getSipConfig().getProxies();
        proxies.clear();
        if (proxy.length() != 0) {
            proxies.add(proxy);
        }

		    /* Enable ICE */
        accCfg.getNatConfig().setIceEnabled(true);

		    /* Finally */
        try {
            account.modify(accCfg);
        } catch (Exception e) {}
    }


    /*
    * === MyAppObserver ===
    *
    * As we cannot do UI from worker thread, the callbacks mostly just send
    * a message to UI/main thread.
    */

    public void notifyIncomingCall(MyCall call)
    {
        Message m = Message.obtain(handler, MSG_TYPE.INCOMING_CALL, call);
        m.sendToTarget();
    }

    public void notifyRegState(pjsip_status_code code, String reason,
                               int expiration)
    {
        String msg_str = "";
        if (expiration == 0)
            msg_str += "Unregistration";
        else
            msg_str += "Registration";

        if (code.swigValue()/100 == 2)
            msg_str += " successful";
        else
            msg_str += " failed: " + reason;

        Message m = Message.obtain(handler, MSG_TYPE.REG_STATE, msg_str);
        m.sendToTarget();
    }

    public void notifyCallState(MyCall call)
    {
        if (currentCall == null || call.getId() != currentCall.getId())
            return;

        CallInfo ci;
        try {
            ci = call.getInfo();
        } catch (Exception e) {
            ci = null;
        }
        Message m = Message.obtain(handler, MSG_TYPE.CALL_STATE, ci);
        m.sendToTarget();

        if (ci != null &&
                ci.getState() == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)
        {
            currentCall = null;
        }
    }

    public void notifyCallMediaState(MyCall call)
    {
        Message m = Message.obtain(handler, MSG_TYPE.CALL_MEDIA_STATE, null);
        m.sendToTarget();
    }

    public void notifyBuddyState(MyBuddy buddy)
    {
        Message m = Message.obtain(handler, MSG_TYPE.BUDDY_STATE, buddy);
        m.sendToTarget();
    }

    public void notifyChangeNetwork()
    {
        Message m = Message.obtain(handler, MSG_TYPE.CHANGE_NETWORK, null);
        m.sendToTarget();
    }

    /* === end of MyAppObserver ==== */

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
