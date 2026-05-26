package com.example.canescan_crud;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.view.View;
import java.util.Locale;

public class AccessibilityHelper {
    private static TextToSpeech tts;
    private static boolean isTTSEnabled = false;
    private static boolean isSoundsEnabled = false;

    public static void init(Context context) {
        if (tts == null) {
            tts = new TextToSpeech(context.getApplicationContext(), status -> {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            });
        }
        updateSettings(context);
    }

    public static void updateSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);
        isTTSEnabled = prefs.getBoolean("enable_tts", false);
        isSoundsEnabled = prefs.getBoolean("enable_sounds", false);
    }

    public static void speak(String text) {
        if (isTTSEnabled && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public static void handleViewClick(Context context, View view) {
        if (isSoundsEnabled) {
            // Optional: play a click sound if needed
            // view.playSoundEffect(android.view.SoundEffectConstants.CLICK);
        }
        
        if (isTTSEnabled) {
            String textToSpeak = "";
            if (view.getContentDescription() != null) {
                textToSpeak = view.getContentDescription().toString();
            } else if (view instanceof android.widget.TextView) {
                textToSpeak = ((android.widget.TextView) view).getText().toString();
            }
            
            if (!textToSpeak.isEmpty()) {
                speak(textToSpeak);
            }
        }
    }
}