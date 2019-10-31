package sharpeye.sharpeye.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.widget.TextView;

import java.util.Locale;

public class Font {

    public enum FontList
    {
        CHARACTERE
    }

    public static void setForTextView(Context appContext, FontList font, TextView tx)
    {
        String fontName;
        switch(font)
                {
                    case CHARACTERE: fontName = "CaracteresL1.ttf";
                    break ;
                    default: return;
                }
        AssetManager am = appContext.getApplicationContext().getAssets();

        Typeface typeface = Typeface.createFromAsset(am,
                String.format(Locale.US, "fonts/%s", fontName));

        tx.setTypeface(typeface);
    }
}
