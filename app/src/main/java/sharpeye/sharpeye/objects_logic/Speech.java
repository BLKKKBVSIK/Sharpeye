package sharpeye.sharpeye.objects_logic;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class Speech {

    private TextToSpeech textToSpeech;

    public Speech(Context context) {
        init(context);
    }

    private void setLanguage() {
        Set<Locale> availableLanguages = textToSpeech.getAvailableLanguages();
        if (availableLanguages != null) {
            for (Locale language: availableLanguages) {
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

    public void init(Context context) {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                setLanguage();
            }
        });
    }

    public void clean() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null,
                UUID.randomUUID().toString());
    }
}
