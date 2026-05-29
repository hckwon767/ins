package com.instation.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;

/**
 * Native Foreground Media Service.
 * Android 에게 "이 앱은 미디어 재생 중" 임을 알려서
 * Doze / App Standby / 제조사 절전 정책에서 kill 당하지 않도록 함.
 *
 * 오디오 재생 자체는 WebView HTML5 audio 가 담당.
 * 이 서비스는 "상주 알림 + Android 미디어 우선권 확보" 역할만 함.
 */
public class RadioForegroundService extends Service {

    public static final String CHANNEL_ID   = "instation_radio";
    public static final int    NOTIF_ID     = 9001;

    public static final String ACTION_START  = "com.instation.player.START";
    public static final String ACTION_STOP   = "com.instation.player.STOP";
    public static final String ACTION_UPDATE = "com.instation.player.UPDATE";

    public static final String EXTRA_TITLE   = "title";
    public static final String EXTRA_ARTIST  = "artist";
    public static final String EXTRA_CHANNEL = "channel";

    private MediaSessionCompat mediaSession;

    // ── Binder (Activity 에서 직접 참조하려면 사용) ──────────
    public class LocalBinder extends Binder {
        RadioForegroundService getService() { return RadioForegroundService.this; }
    }
    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initMediaSession();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            startForeground(NOTIF_ID, buildNotification("INSTATION", "", ""));
            return START_STICKY;
        }

        String action = intent.getAction();
        if (action == null) action = ACTION_START;

        String title   = intent.getStringExtra(EXTRA_TITLE);
        String artist  = intent.getStringExtra(EXTRA_ARTIST);
        String channel = intent.getStringExtra(EXTRA_CHANNEL);
        if (title   == null) title   = "INSTATION";
        if (artist  == null) artist  = "";
        if (channel == null) channel = "";

        switch (action) {
            case ACTION_STOP:
                stopForeground(true);
                stopSelf();
                break;

            case ACTION_START:
            case ACTION_UPDATE:
            default:
                updateMediaSession(title, artist);
                startForeground(NOTIF_ID, buildNotification(title, artist, channel));
                break;
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        if (mediaSession != null) mediaSession.release();
        super.onDestroy();
    }

    // ── 알림 채널 생성 (Android 8.0+) ────────────────────────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID,
                "INSTATION 라디오",
                NotificationManager.IMPORTANCE_LOW
            );
            ch.setDescription("백그라운드 라디오 스트리밍");
            ch.setShowBadge(false);
            ch.setSound(null, null);
            ch.enableLights(false);
            ch.enableVibration(false);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    // ── MediaSession 초기화 ───────────────────────────────────
    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "INSTATIONRadio");
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_STOP
            )
            .build();
        mediaSession.setPlaybackState(state);
        mediaSession.setActive(true);
    }

    private void updateMediaSession(String title, String artist) {
        if (mediaSession == null) return;
        android.support.v4.media.MediaMetadataCompat meta =
            new android.support.v4.media.MediaMetadataCompat.Builder()
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE,  title)
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .build();
        mediaSession.setMetadata(meta);
    }

    // ── 알림 빌드 ─────────────────────────────────────────────
    private Notification buildNotification(String title, String artist, String channel) {
        Intent tapIntent = new Intent(this, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tapPending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title.isEmpty() ? "INSTATION" : title)
            .setContentText(artist.isEmpty() ? channel : artist + (channel.isEmpty() ? "" : " • " + channel))
            .setContentIntent(tapPending)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW);

        if (mediaSession != null) {
            builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView()
            );
        }

        return builder.build();
    }
}
