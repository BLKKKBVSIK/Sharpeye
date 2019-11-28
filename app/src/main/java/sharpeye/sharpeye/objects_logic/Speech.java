package sharpeye.sharpeye.objects_logic;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.util.Log;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class Speech {

    private TextToSpeech textToSpeech;
    private boolean TTSAvailable = true;

    public Speech(Context context) {
        init(context);
    }

    private void setLanguage() {
        if (textToSpeech != null) {
            Set<Locale> availableLanguages = textToSpeech.getAvailableLanguages();
            if (availableLanguages != null) {
                for (Locale language : availableLanguages) {
                    if (language.toString().equals("fr_FR")) {
                        int result = textToSpeech.setLanguage(language);
                        if (result != TextToSpeech.LANG_MISSING_DATA &&
                                result != TextToSpeech.LANG_NOT_SUPPORTED) {
                            break;
                        }
                        Log.e("TextToSpeech", "fr_FR is not supported or data are missing");
                    }
                }
            }
        }
    }

    public boolean isAvailable() {
        return (TTSAvailable);
    }

    public void init(Context context) {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    TTSAvailable = true;
                    setLanguage();
                } else {
                    TTSAvailable = false;
                    textToSpeech = null;
                }
            }
        });
    }

    public void clean() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    public void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null,
                    UUID.randomUUID().toString());
        }
    }
}
