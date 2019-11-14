package sharpeye.sharpeye.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.widget.TextView;

import java.util.Locale;

/**
 * Adds functions to deal with fonts
 */
public class Font {

    /**
     * Enum of all the available fonts to change
     */
    public enum FontList
    {
        CHARACTERE
    }

    /**
     * Changes the font of a given textView
     * @param appContext context of the app
     * @param font the desired font
     * @param tx The textView you want to edit
     */
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
