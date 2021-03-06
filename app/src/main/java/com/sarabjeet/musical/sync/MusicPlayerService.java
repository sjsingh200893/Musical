package com.sarabjeet.musical.sync;


import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.sarabjeet.musical.R;
import com.sarabjeet.musical.data.SongModel;

import java.io.IOException;
import java.util.ArrayList;

import static com.sarabjeet.musical.utils.Constants.ACTION.ACTION_NEXT;
import static com.sarabjeet.musical.utils.Constants.ACTION.ACTION_PAUSE;
import static com.sarabjeet.musical.utils.Constants.ACTION.ACTION_PLAY;
import static com.sarabjeet.musical.utils.Constants.ACTION.ACTION_RESUME;
import static com.sarabjeet.musical.utils.Constants.PLAYER.PLAY;

/**
 * Created by sarabjeet on 8/5/17.
 */

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener {

    public MediaPlayer mediaPlayer;
    byte[] data;
    LocalBroadcastManager broadcastManager;
    String path = null;
    String title = null;
    String artist = null;
    FirebaseAnalytics mFirebaseAnalytics;
    IBinder mBinder = new MusicBinder();
    ArrayList<SongModel> playList;
    int trackIndex;
    SongModel model;

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        broadcastManager = LocalBroadcastManager.getInstance(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        if (intent.getAction().equals(getString(R.string.default_service_start))) {
        } else if (intent.getAction().equals(ACTION_PLAY)) {
            Bundle playListBundle = intent.getExtras();
            if (playListBundle != null) {
                playList = (ArrayList<SongModel>) playListBundle.getSerializable(getString(R.string.playlist));
            }
            model = playList.get(0);
            trackIndex = 0;
            path = model.getAudioPath();
            title = model.getAudioTitle();
            artist = model.getAudioArtist();
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, intent.getStringExtra("title"));
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(path);
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (intent.getAction().equals(ACTION_PAUSE)) {
            mediaPlayer.pause();
            updatePlayer();
        } else if (intent.getAction().equals(ACTION_RESUME)) {
            mediaPlayer.start();
            updatePlayer();
        } else if (intent.getAction().equals(ACTION_NEXT)) {
            if (++trackIndex < playList.size()) {
                model = playList.get(trackIndex);
                path = model.getAudioPath();
                title = model.getAudioTitle();
                artist = model.getAudioArtist();
                try {
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(path);
                    mediaPlayer.setOnPreparedListener(this);
                    mediaPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;

    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        updatePlayer();
        mp.start();
    }

    private void updatePlayer() {

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        data = mmr.getEmbeddedPicture();
        Intent intent = new Intent(PLAY);
        intent.putExtra(getString(R.string.media_path), path);
        intent.putExtra(getString(R.string.media_artist), artist);
        intent.putExtra(getString(R.string.media_title), title);
        if (mediaPlayer.isPlaying()) {
            intent.putExtra(getString(R.string.mp_state), true);
        } else {
            intent.putExtra(getString(R.string.mp_state), false);
        }
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null)
            mediaPlayer.release();
    }

    public byte[] getData() {
        return data;
    }

    public class MusicBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }
}