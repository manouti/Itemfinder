/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Manages sound and vibrations for {@link CaptureActivity}.
 */
public final class SoundManager implements MediaPlayer.OnErrorListener, Closeable {

  private static final String TAG = SoundManager.class.getSimpleName();

  private static final float SOUND_VOLUME = 0.60f;
  private static final long VIBRATE_DURATION = 200L;

  private static final int MSG_MP_RELEASE = 0x0000bbcc;

  private final Activity activity;
  private MediaPlayer mediaPlayer;
  private boolean playSound;
  private boolean vibrate;

  private final WeakReference<SoundManager> weakReferenceToThisTask = new WeakReference<>(this);
  private final MediaPlayerReleaseHandler mediaPlayerReleaseHandler = new MediaPlayerReleaseHandler(weakReferenceToThisTask);

  public SoundManager(Activity activity) {
    this.activity = activity;
    this.mediaPlayer = null;
    updatePrefs();
  }

  public SoundManager(Activity activity, boolean playSound, boolean vibrate) {
    this.activity = activity;
    this.mediaPlayer = null;
    this.playSound = playSound;
    this.vibrate = vibrate;
    if (playSound) {
      // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
      // so we now play on the music stream.
      activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
      mediaPlayer = buildMediaPlayer(activity);
    }
  }

  public synchronized void updatePrefs() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    playSound = shouldPlaySound(prefs, activity);
    vibrate = prefs.getBoolean(PreferencesActivity.KEY_VIBRATE, false);
    if (playSound && mediaPlayer == null) {
      // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
      // so we now play on the music stream.
      activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
      mediaPlayer = buildMediaPlayer(activity);
    }
  }

  public synchronized void playSoundAndVibrate() {
    if (playSound && mediaPlayer != null) {
      mediaPlayer.start();
    }
    if (vibrate) {
      Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
      vibrator.vibrate(VIBRATE_DURATION);
    }
  }

  private static boolean shouldPlaySound(SharedPreferences prefs, Context activity) {
    boolean shouldPlaySound = prefs.getBoolean(PreferencesActivity.KEY_PLAY_SOUND, true);
    if (shouldPlaySound) {
      // See if sound settings overrides this
      AudioManager audioService = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
      if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
        shouldPlaySound = false;
      }
    }
    return shouldPlaySound;
  }

  private MediaPlayer buildMediaPlayer(Context activity) {
    MediaPlayer mediaPlayer = new MediaPlayer();
    try {
      AssetFileDescriptor file = activity.getResources().openRawResourceFd(R.raw.itemfinder);
      try {
        mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
      } finally {
        file.close();
      }
      mediaPlayer.setOnErrorListener(this);
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
      mediaPlayer.setLooping(false);
      mediaPlayer.setVolume(SOUND_VOLUME, SOUND_VOLUME);
      mediaPlayer.prepare(); // TODO do this asynchronously for better performance, see the dev guide
      return mediaPlayer;
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      mediaPlayer.release();
      return null;
    }
  }

  @Override
  public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
    if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
      // we are finished, so put up an appropriate error toast if required and finish
      activity.finish();
    } else {
      // possibly media player error, so release and recreate
      close();
      updatePrefs();
    }
    return true;
  }

  @Override
  public synchronized void close() {
    if (mediaPlayer != null) {
      mediaPlayerReleaseHandler.sendEmptyMessageDelayed(MSG_MP_RELEASE, 200);
    }
  }

  private void releaseMediaPlayer() {
    mediaPlayer.release();
    mediaPlayer = null;
  }

  // See https://groups.google.com/forum/#!topic/android-developers/Ciiu1C-_EmE
  private static class MediaPlayerReleaseHandler extends Handler {
    private WeakReference<SoundManager> soundManagerRef;

    public MediaPlayerReleaseHandler(WeakReference<SoundManager> soundManagerRef) {
      this.soundManagerRef = soundManagerRef;
    }

    @Override
    public void handleMessage(Message msg) {
      if (msg.what == MSG_MP_RELEASE) {
        soundManagerRef.get().releaseMediaPlayer();
      }
    }
  }
}
