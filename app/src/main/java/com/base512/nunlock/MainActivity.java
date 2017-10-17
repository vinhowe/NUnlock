package com.base512.nunlock;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    TextView statusLabel;

    ImageView lockButton;
    ImageView unlockButton;

    EditText ipField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusLabel = findViewById(R.id.status_label);

        lockButton = findViewById(R.id.lockButton);
        unlockButton = findViewById(R.id.unlockButton);

        ipField = findViewById(R.id.ipField);

        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLockedState(true);
            }
        });

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLockedState(false);
            }
        });
    }

    public void setLockedState(final boolean state) {
        statusLabel.setText("Attempting connection");

        final String ipAddress = ipField.getText().toString();
        new AsyncTask<Integer, Void, Void>(){
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    Log.d(MainActivity.class.getName(), executeRemoteCommand(state, "root", "gtvh4ckr", ipAddress, 22, MainActivity.this));
                } catch (Exception e) {
                    statusLabel.setText("CONNECTION FAILED: " + e.getMessage());
                    Log.e(SshLogger.class.getName(), e.getMessage());
                }
                return null;
            }
        }.execute(1);
    }

    public static String executeRemoteCommand(boolean lock, String username, String password, String hostname, int port, final MainActivity mainActivity)
            throws Exception {
        JSch.setLogger(new SshLogger());
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, hostname, port);
        session.setPassword(password);

        // Avoid asking for key confirmation
        Properties prop = new Properties();
        prop.put("StrictHostKeyChecking", "no");
        session.setConfig(prop);

        session.connect();

        if(session.isConnected()) {
            Log.d(SshLogger.class.getName(), "CONNECTED");
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.statusLabel.setText("CONNECTED");
                }
            });
        } else {
            Log.d(SshLogger.class.getName(), "NOT CONNECTED");
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.statusLabel.setText("NOT CONNECTED");
                }
            });
        }

        // SSH Channel
        ChannelExec channelSsh = (ChannelExec) session.openChannel("exec");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //channelSsh.setOutputStream(baos);

        int currentValue = lock? 0: 1;
        int newValue = lock? 1: 0;

        // Execute command
       // channelSsh.setCommand("/etc/init.d/nestlabs stop; sleep 1; /etc/init.d/nestlabs start;");
        channelSsh.setCommand("/etc/init.d/nestlabs stop; sed -i 's/temperature_lock\" value=\""+currentValue+"\"/temperature_lock\" value=\""+newValue+"\"/' /nestlabs/etc/user/settings.config; /etc/init.d/nestlabs start;");
        channelSsh.connect();
        channelSsh.disconnect();
        session.disconnect();

        return baos.toString();
    }

    public static class SshLogger implements com.jcraft.jsch.Logger {

        @Override
        public boolean isEnabled(int i) {
            return true;
        }

        @Override
        public void log(int i, String s) {
            Log.d(SshLogger.class.getName(), s);
        }
    }
}
