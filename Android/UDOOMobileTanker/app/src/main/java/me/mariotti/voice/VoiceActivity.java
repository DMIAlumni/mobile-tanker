package me.mariotti.voice;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;

import java.util.ArrayList;

public class VoiceActivity extends Activity{
    public void listen(int requestCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,500);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Choose a color between Red, Green, Yellow and Blue");
        startActivityForResult(intent, requestCode);
    }
    public void recognitionResults(int requestCode, String bestMatch) {}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            recognitionResults(requestCode, matches.get(0));
        }
    }
}
