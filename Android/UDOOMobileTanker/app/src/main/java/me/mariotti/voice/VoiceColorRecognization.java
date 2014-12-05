package me.mariotti.voice;


import me.mariotti.opencv.TargetSearch;

public class VoiceColorRecognization extends VoiceActivity {
    private static final int VOICE_COLOR = 1;
    private TargetSearch targetSearch;


    public VoiceColorRecognization(TargetSearch targetSearch) {
        this.targetSearch = targetSearch;
        listen(VOICE_COLOR);
    }

    @Override
    public void recognitionResults(int requestCode, String bestMatch) {
        if (requestCode == VOICE_COLOR) {
            if (bestMatch.contains("blu")) {
                targetSearch.setTargetColorToBlue();
            } else if (bestMatch.contains("rosso")) {
                targetSearch.setTargetColorToRed();
            } else if (bestMatch.contains("giallo")) {
                targetSearch.setTargetColorToYellow();
            } else if (bestMatch.contains("verde")) {
                targetSearch.setTargetColorToGreen();
            }
            finish();
        }
    }
}
