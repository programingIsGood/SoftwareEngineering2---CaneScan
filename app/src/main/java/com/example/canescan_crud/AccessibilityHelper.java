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
    private static boolean isTtsReady = false;
    private static String pendingSpeech = null;

    public static void init(Context context) {
        if (isInitialized) {
            updateSettings(context);
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);
        isSoundsEnabled = prefs.getBoolean("enable_sounds", false);
        isTTSEnabled = prefs.getBoolean("enable_tts", false);

        TextToSpeech.OnInitListener listener = status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setPitch(1.0f);
                tts.setSpeechRate(1.0f);

                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = tts.setLanguage(Locale.getDefault());
                }

                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = true;
                    if (pendingSpeech != null) {
                        speak(pendingSpeech);
                        pendingSpeech = null;
                    }
                }
            }
        };

        // Try to initialize with Google TTS engine first (most reliable on HyperOS)
        tts = new TextToSpeech(context.getApplicationContext(), listener, "com.google.android.tts");
        
        // If the engine isn't available, the listener might not be called or might fail.
        // The above constructor doesn't throw if package is missing, it just falls back or fails.
        // Actually, specify engine is safer but let's ensure it doesn't break if missing.

        isInitialized = true;
    }

    public static void updateSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);
        isSoundsEnabled = prefs.getBoolean("enable_sounds", false);
        isTTSEnabled = prefs.getBoolean("enable_tts", false);
    }

    public static void speak(String text) {
        if (isTTSEnabled && tts != null) {
            if (isTtsReady) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AccessibilityTTS");
            } else {
                pendingSpeech = text;
            }
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
            // Check if media volume is muted (common issue on HyperOS)
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null && am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                android.widget.Toast.makeText(context, "Please turn up Media Volume for Voice Assistant", android.widget.Toast.LENGTH_SHORT).show();
            }

            String text = "";
            int id = view.getId();

            if (id == R.id.switch_dark_mode) {
                text = "Dark Mode toggled";
            } else if (id == R.id.sw_sounds) {
                text = "Click sounds toggled";
            } else if (id == R.id.sw_tts) {
                text = "Voice assistant toggled";
            } else if (id == R.id.nav_home) {
                text = "Navigating to Dashboard";
            } else if (id == R.id.nav_map) {
                text = "Navigating to Map";
            } else if (id == R.id.nav_history) {
                text = "Navigating to History";
            } else if (id == R.id.nav_settings) {
                text = "Navigating to Settings";
            } else if (id == R.id.btn_scan_now) {
                text = "Opening Camera for Sugarcane Scan";
            } else if (id == R.id.iv_profile || id == R.id.iv_profile_circle) {
                text = "Viewing Profile Settings";
            } else if (id == R.id.iv_back) {
                text = "Going back";
            } else if (view instanceof SwitchCompat) {
                text = "Clicked " + ((SwitchCompat) view).getText().toString();
            } else if (view instanceof Button) {
                text = "Clicked " + ((Button) view).getText().toString();
            } else if (view instanceof TextView) {
                text = "Clicked " + ((TextView) view).getText().toString();
            } else if (view.getContentDescription() != null) {
                text = "Clicked " + view.getContentDescription().toString();
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
        isTtsReady = false;
    }
}
