package com.alexai.assistant.ui.media;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.format.Formatter;
import android.view.View;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.alexai.assistant.R;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Alex's own image viewer (zoom), video player and single-file music player.
 * Files are chosen with Android's file picker, so no storage permission is needed.
 * Playback pauses when the screen is left; there is no background playback.
 */
public class MediaViewerActivity extends AppCompatActivity {
    public static final String EXTRA_KIND = "kind";
    private static final String STATE_URI = "uri";

    private String kind = "image";
    private Uri current;
    private ZoomImageView zoom;
    private VideoView video;
    private View audioPanel;
    private TextView info, audioTitle, audioTime;
    private SeekBar audioSeek;
    private MaterialButton audioToggle;
    private MediaPlayer player;
    private boolean prepared;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<String[]> picker;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (player != null && prepared) {
                int pos = player.getCurrentPosition();
                audioSeek.setProgress(pos);
                audioTime.setText(format(pos) + " / " + format(player.getDuration()));
                if (player.isPlaying()) handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String k = getIntent().getStringExtra(EXTRA_KIND);
        if ("video".equals(k) || "audio".equals(k)) kind = k;
        setContentView(R.layout.activity_media);

        zoom = findViewById(R.id.zoom);
        video = findViewById(R.id.video);
        audioPanel = findViewById(R.id.audio_panel);
        info = findViewById(R.id.info);
        audioTitle = findViewById(R.id.audio_title);
        audioTime = findViewById(R.id.audio_time);
        audioSeek = findViewById(R.id.audio_seek);
        audioToggle = findViewById(R.id.audio_toggle);

        boolean image = kind.equals("image");
        zoom.setVisibility(image ? View.VISIBLE : View.GONE);
        video.setVisibility(kind.equals("video") ? View.VISIBLE : View.GONE);
        audioPanel.setVisibility(kind.equals("audio") ? View.VISIBLE : View.GONE);
        int zoomControls = image ? View.VISIBLE : View.GONE;
        findViewById(R.id.zoom_in).setVisibility(zoomControls);
        findViewById(R.id.zoom_out).setVisibility(zoomControls);
        findViewById(R.id.zoom_fit).setVisibility(zoomControls);
        info.setText("Choose a " + kind + " file.");

        picker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), new androidx.activity.result.ActivityResultCallback<Uri>() {
            @Override public void onActivityResult(Uri uri) {
                if (uri == null) {
                    if (current == null) finish();
                    return;
                }
                open(uri);
            }
        });

        findViewById(R.id.pick).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pick(); }
        });
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        findViewById(R.id.zoom_in).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { zoom.zoomIn(); }
        });
        findViewById(R.id.zoom_out).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { zoom.zoomOut(); }
        });
        findViewById(R.id.zoom_fit).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { zoom.resetZoom(); }
        });
        audioToggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleAudio(); }
        });
        findViewById(R.id.audio_back10).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { skipAudio(-10000); }
        });
        findViewById(R.id.audio_fwd10).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { skipAudio(10000); }
        });
        audioSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && player != null && prepared) player.seekTo(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        String saved = savedInstanceState == null ? null : savedInstanceState.getString(STATE_URI);
        if (saved != null) open(Uri.parse(saved));
        else pick();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (current != null) outState.putString(STATE_URI, current.toString());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null && prepared && player.isPlaying()) {
            player.pause();
            audioToggle.setText("Play");
        }
        if (video.isPlaying()) video.pause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        video.stopPlayback();
        super.onDestroy();
    }

    private void pick() { picker.launch(new String[]{kind + "/*"}); }

    private void open(Uri uri) {
        current = uri;
        releasePlayer();
        String name = displayName(uri);
        switch (kind) {
            case "video": playVideo(uri, name); break;
            case "audio": playAudio(uri, name); break;
            default: loadImage(uri, name); break;
        }
    }

    // ---- image -------------------------------------------------------------------------------

    private void loadImage(final Uri uri, final String name) {
        info.setText("Loading " + name + "...");
        new Thread(new Runnable() {
            @Override public void run() {
                Bitmap bmp = null;
                int w = 0, h = 0;
                try {
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    try (InputStream in = getContentResolver().openInputStream(uri)) {
                        BitmapFactory.decodeStream(in, null, bounds);
                    }
                    w = bounds.outWidth;
                    h = bounds.outHeight;
                    int sample = 1;
                    while (w / sample > 2048 || h / sample > 2048) sample *= 2;
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = sample;
                    try (InputStream in = getContentResolver().openInputStream(uri)) {
                        bmp = BitmapFactory.decodeStream(in, null, opts);
                    }
                } catch (IOException | RuntimeException | OutOfMemoryError e) {
                    bmp = null;
                }
                final Bitmap result = bmp;
                final int fw = w, fh = h;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (isDestroyed()) return;
                        if (result == null) {
                            info.setText("I couldn't read this image.");
                        } else {
                            zoom.setImageBitmap(result);
                            info.setText(name + "  -  " + fw + " x " + fh + "  -  " + size(uri));
                        }
                    }
                });
            }
        }).start();
    }

    // ---- video -------------------------------------------------------------------------------

    private void playVideo(Uri uri, String name) {
        MediaController controller = new MediaController(this);
        controller.setAnchorView(video);
        video.setMediaController(controller);
        video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                info.setText("I couldn't play this video.");
                return true;
            }
        });
        video.setVideoURI(uri);
        video.start();
        info.setText(name + "  -  " + size(uri));
    }

    // ---- audio -------------------------------------------------------------------------------

    private void playAudio(Uri uri, String name) {
        audioTitle.setText(name);
        info.setText(size(uri));
        audioToggle.setText("Play");
        audioSeek.setProgress(0);
        player = new MediaPlayer();
        prepared = false;
        try {
            player.setDataSource(this, uri);
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    prepared = true;
                    audioSeek.setMax(mp.getDuration());
                    mp.start();
                    audioToggle.setText("Pause");
                    handler.post(ticker);
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) { audioToggle.setText("Play"); }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    info.setText("I couldn't play this audio file.");
                    prepared = false;
                    return true;
                }
            });
            player.prepareAsync();
        } catch (IOException | RuntimeException e) {
            info.setText("I couldn't play this audio file.");
            releasePlayer();
        }
    }

    private void toggleAudio() {
        if (player == null || !prepared) return;
        if (player.isPlaying()) {
            player.pause();
            audioToggle.setText("Play");
        } else {
            player.start();
            audioToggle.setText("Pause");
            handler.post(ticker);
        }
    }

    private void skipAudio(int deltaMs) {
        if (player == null || !prepared) return;
        int target = Math.max(0, Math.min(player.getDuration(), player.getCurrentPosition() + deltaMs));
        player.seekTo(target);
        handler.post(ticker);
    }

    private void releasePlayer() {
        handler.removeCallbacks(ticker);
        prepared = false;
        if (player != null) {
            try { player.release(); } catch (RuntimeException ignored) { }
            player = null;
        }
    }

    // ---- helpers -----------------------------------------------------------------------------

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String n = c.getString(0);
                if (n != null) return n;
            }
        } catch (RuntimeException ignored) { }
        return "Selected file";
    }

    private String size(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return Formatter.formatShortFileSize(this, c.getLong(0));
        } catch (RuntimeException ignored) { }
        return "size unknown";
    }

    private static String format(int ms) {
        int s = ms / 1000;
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }
}
