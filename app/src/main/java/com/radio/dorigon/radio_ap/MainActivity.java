package com.radio.dorigon.radio_ap;

import android.app.Notification;
import android.content.Context;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener{

    private static final int HELLO_ID = 1;

    private String TAG = getClass().getSimpleName();
    private boolean isPlay, isVisible;

    private MediaPlayer mp = new MediaPlayer();
    private AudioManager audioManager;
    private SeekBar seekbar;
    private Button btnPlayStop;
    private ProgressBar progressBar;
    private TextView txtVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startVariables();
        if (!isPlay)
            play();
        if (isVisible)
            progressBar.setVisibility(View.VISIBLE);
        else
            progressBar.setVisibility(View.GONE);

        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar arg) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg) {
            }

            @Override
            public void onProgressChanged(SeekBar arg, int progress, boolean arg1) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                calculateVolume(progress);
            }
        });

        btnPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPlay) {
                    stop();
                } else {
                    play();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(HELLO_ID);
        super.onDestroy();
    }

    @Override
    public void finish() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(HELLO_ID);
        super.finish();
    }

    public boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "Stream is prepared");
        mp.start();
        isVisible = false;
        progressBar.setVisibility(View.GONE);
    }

    public void onCompletion(MediaPlayer mp) {
        stop();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
            seekbar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            seekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            calculateVolume(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, 0);
            seekbar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            seekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            calculateVolume(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onBackPressed() {
        this.moveTaskToBack(true);
    }

    protected void startVariables() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        this.setContentView(R.layout.activity_main);
        seekbar = (SeekBar) this.findViewById(R.id.seekBar);
        btnPlayStop = (Button) this.findViewById(R.id.btn_play_stop);
        progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_IN);
        txtVolume = (TextView) this.findViewById(R.id.txtVolume);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        seekbar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        seekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        calculateVolume(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    protected void calculateVolume(int progress) {
        int maxVolume = seekbar.getMax(), aux,  volume;
        aux = 100 * progress;
        volume = aux / maxVolume;
        txtVolume.setText(getString(R.string.volume) + " " + volume + "%");
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("Media Player Error: ");
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                sb.append("Not Valid for Progressive Playback");
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                sb.append("Server Died");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                sb.append("Unknown");
                stop();
                Toast.makeText(this, R.string.connectServerError, Toast.LENGTH_LONG).show();
                break;
            default:
                sb.append(" Non standard (");
                sb.append(what);
                sb.append(")");
        }
        sb.append(" (" + what + ") ");
        sb.append(extra);
        Log.e(TAG, sb.toString());
        return true;
    }

    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.d(TAG, "PlayerService onBufferingUpdate : " + percent + "%");
    }

    private void play() {
        notification(getString(R.string.notificationMessage));
        try {
            if (mp != null) {
                mp.stop();
                mp.reset();
            }
            if (isOnline()) {
                progressBar.setVisibility(View.VISIBLE);
                mp.setDataSource(this, Uri.parse("http://dlsolucoesweb.ddns.net:1880/"));
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp.setOnPreparedListener(this);
                mp.setOnBufferingUpdateListener(this);
                mp.setOnErrorListener(this);
                mp.prepareAsync();
                isPlay = true;
                isVisible = true;
                btnPlayStop.setBackgroundResource(R.drawable.img_stop);
            } else {
                stop();
                Toast.makeText(this, R.string.connectionError, Toast.LENGTH_LONG).show();
            }
            Log.d(TAG, "LoadClip Done");
        } catch (Throwable t) {
            Log.d(TAG, t.toString());
        }
    }

    private void stop() {
        notification(getString(R.string.notificationMessagePause));
        mp.reset();
        mp.stop();
        progressBar.setVisibility(View.GONE);
        isPlay = false;
        isVisible = false;
        btnPlayStop.setBackgroundResource(R.drawable.img_play);
    }

    private void notification(String message) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setOngoing(true);

        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager.notify(HELLO_ID, mBuilder.build());

    }
}