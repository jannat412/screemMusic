package com.jannat.screemmusic;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
    Intent serviceIntent;
    private Button buttonPlayStop;
    private TextView currentposition;
    private TextView totalposition;
    String strAudioLink = "https://firebasestorage.googleapis.com/v0/b/shironamhin-14c71.appspot.com/o/Icche_ghuri_Song_image%2FPakhi-.mp3?alt=media&token=4bfe3e79-6d97-4812-8500-e0fd15785fc8";
    private boolean boolMusicPlaying = false;
    private boolean isOnline;
    boolean mBufferBroadcastIsRegistered;
    private ProgressDialog pdBuff = null;
    private SeekBar seekBar;
    private int seekMax;
    private static int songEnded = 0;
    boolean mBroadcastIsRegistered;
    private myPlayService mBoundService = new myPlayService();

    public static final String BROADCAST_SEEKBAR = "com.jannat.screemmusic.sendseekbar";
    Intent intent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try {
            serviceIntent = new Intent(this, myPlayService.class);
            intent = new Intent(BROADCAST_SEEKBAR);

            initViews();
            setListeners();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    e.getClass().getName() + " " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
        buttonPlayStop.setBackgroundResource(R.drawable.pausebuttonsm);
        playAudio();
        boolMusicPlaying = true;
        seekBar.setOnSeekBarChangeListener(this);

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myPlayService.LocalBinder binder = (myPlayService.LocalBinder) service;
            mBoundService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            updateUI(serviceIntent);
        }
    };

    private void updateUI(Intent serviceIntent) {
        String counter = serviceIntent.getStringExtra("counter");
        String mediamax = serviceIntent.getStringExtra("mediamax");
        String strSongEnded = serviceIntent.getStringExtra("song_ended");
        int seekProgress = Integer.parseInt(counter);
        seekMax = Integer.parseInt(mediamax);
        songEnded = Integer.parseInt(strSongEnded);
        seekBar.setMax(seekMax);

        long second = (seekMax / 1000) % 60;
        long minute = (seekMax / (1000 * 60)) % 60;
        String totalDuration = String.format("%02d:%02d", minute, second);

        long second1 = (seekProgress / 1000) % 60;
        long minute1 = (seekProgress / (1000 * 60)) % 60;
        String currentDuration = String.format("%02d:%02d", minute1, second1);

        totalposition.setText(totalDuration);
        currentposition.setText(currentDuration);

        seekBar.setProgress(seekProgress);
        if (songEnded == 1) {
            buttonPlayStop.setBackgroundResource(R.drawable.playbuttonsm);
        }
    }

    private void initViews() {
        buttonPlayStop = (Button) findViewById(R.id.ButtonPlayStop);
        buttonPlayStop.setBackgroundResource(R.drawable.playbuttonsm);
        seekBar = (SeekBar) findViewById(R.id.SeekBar01);
        totalposition = (TextView) findViewById(R.id.totalposition);
        currentposition = (TextView) findViewById(R.id.currentposition);


    }
    private void setListeners() {
        buttonPlayStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonPlayStopClick();
            }
        });
        seekBar.setOnSeekBarChangeListener(this);
    }
    private void buttonPlayStopClick() {
        if (!boolMusicPlaying) {
            buttonPlayStop.setBackgroundResource(R.drawable.pausebuttonsm);
            mBoundService.playMedia();
            boolMusicPlaying = true;
        } else {
            if (boolMusicPlaying) {
                buttonPlayStop.setBackgroundResource(R.drawable.playbuttonsm);
                mBoundService.pauseMedia();
                boolMusicPlaying = false;
            }
        }
    }
    private void stopMyPlayService() {
        if (mBroadcastIsRegistered) {
            try {
                unregisterReceiver(broadcastReceiver);
                mBroadcastIsRegistered = false;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(
                        getApplicationContext(),
                        e.getClass().getName() + " " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
        try {
            stopService(serviceIntent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    e.getClass().getName() + " " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
        boolMusicPlaying = false;
    }
    private void playAudio() {
        checkConnectivity();
        if (isOnline) {
            stopMyPlayService();
            serviceIntent.putExtra("sentAudioLink", strAudioLink);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            try {
                startService(serviceIntent);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(
                        getApplicationContext(),
                        e.getClass().getName() + " " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
            registerReceiver(broadcastReceiver, new IntentFilter(
                    myPlayService.BROADCAST_ACTION));
            mBroadcastIsRegistered = true;
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Network Not Connected...");
            alertDialog.setMessage("Please connect to a network and try again");
            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // here you can add functions
                }
            });
            alertDialog.setIcon(R.drawable.mcircle);
            buttonPlayStop.setBackgroundResource(R.drawable.playbuttonsm);
            alertDialog.show();
        }

    }
    private void showPD(Intent bufferIntent) {
        String bufferValue = bufferIntent.getStringExtra("buffering");
        int bufferIntValue = Integer.parseInt(bufferValue);
        switch (bufferIntValue) {
            case 0:
                // txtBuffer.setText("");
                if (pdBuff != null) {
                    pdBuff.dismiss();
                }
                break;

            case 1:
                BufferDialogue();
                break;
            case 2:
                buttonPlayStop.setBackgroundResource(R.drawable.playbuttonsm);
                break;

        }
    }
    private void BufferDialogue() {

        pdBuff = ProgressDialog.show(PlayerActivity.this, "Buffering...",
                "Acquiring song...", true);
    }

    private BroadcastReceiver broadcastBufferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent bufferIntent) {
            showPD(bufferIntent);
        }
    };

    @Override
    protected void onPause() {
        // Unregister broadcast receiver
        if (mBufferBroadcastIsRegistered) {
            try{
                unregisterReceiver(broadcastBufferReceiver);
                mBufferBroadcastIsRegistered = false;
            }catch (Exception e){
                e.printStackTrace();
                Toast.makeText(
                        getApplicationContext(),
                        e.getClass().getName() + " " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
        super.onPause();
    }
    @Override
    protected void onResume() {
        // Register broadcast receiver
        if (!mBroadcastIsRegistered) {
            registerReceiver(broadcastBufferReceiver, new IntentFilter(
                    myPlayService.BROADCAST_BUFFER));
            mBufferBroadcastIsRegistered = true;
        }
        super.onResume();
    }

    private void checkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting()
                || cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting())
            isOnline = true;
        else
            isOnline = false;
    }


    @Override
    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
        // TODO Auto-generated method stub
        if (fromUser) {
            int seekPos = sb.getProgress();
            intent.putExtra("seekpos", seekPos);
            sendBroadcast(intent);
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
