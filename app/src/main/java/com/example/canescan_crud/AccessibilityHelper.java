package com.example.canescan_crud;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;
import java.util.Locale;

public class AccessibilityHelper {
    private static TextToSpeech tts;
    private static boolean isSoundsEnabled;
    private static boolean isTTSEnabled;
    private static boolean isInitialized = false;

    public static void init(Context context) {
        if (isInitialized) {
            updateSettings(context);
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);
        isSoundsEnabled = prefs.getBoolean("enable_sounds", false);
        isTTSEnabled = prefs.getBoolean("enable_tts", false);

        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
            }
        });
        isInitialized = true;
    }

    public static void updateSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);
        isSoundsEnabled = prefs.getBoolean("enable_sounds", false);
        isTTSEnabled = prefs.getBoolean("enable_tts", false);
    }

    public static void speak(String text) {
        if (isTTSEnabled && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AccessibilityTTS");
        }
    }

    public static void handleViewClick(Context context, View view) {
        if (!isInitialized) {
            init(context);
        }
        updateSettings(context);

        if (isSoundsEnabled) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.7f);
            }
        }

        if (isTTSEnabled && tts != null) {
            String text = "";
            int id = view.getId();
            
            if (id == R.id.switch_dark_mode) {
                text = "You clicked Dark Mode";
            } else if (id == R.id.switch_enable_sounds) {
                text = "You click Enable Sounds by Click";
            } else if (id == R.id.switch_enable_tts) {
                text = "You click Read Aloud Text to Speech";
            } else if (id == R.id.nav_home) {
                text = "Navigating to Dashboard";
            } else if (id == R.id.nav_map) {
                text = "Navigating to Map";
            } else if (id == R.id.nav_history) {
                text = "Navigating to History";
            } else if (id == R.id.nav_settings) {
                text = "Navigating to Settings";
            } else if (view instanceof SwitchCompat) {
                text = "You clicked " + ((SwitchCompat) view).getText().toString();
            } else if (view instanceof Button) {
                text = "You clicked " + ((Button) view).getText().toString();
            } else if (view instanceof TextView) {
                text = "You clicked " + ((TextView) view).getText().toString();
            } else if (view.getContentDescription() != null) {
                text = "You clicked " + view.getContentDescription().toString();
            }

            if (!text.isEmpty()) {
                speak(text);
            }
        }
    }

    public static void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        isInitialized = false;
    }
}
