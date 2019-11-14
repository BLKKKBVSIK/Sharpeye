package sharpeye.sharpeye.signs.frontViews;

import android.util.TypedValue;

import sharpeye.sharpeye.utils.Font;

/**
 * Interface to create frontViews
 */
public interface IFrontViews {

    /**
     * Sets the element(s) visible
     */
    void setVisible();

    /**
     * Sets the element(s) invisible
     */
    void setInvisible();

    /**
     * Sets the Color (if possible)
     * @param color
     */
    void setTextColor(int color);

    /**
     * Sets the text (if there is some)
     * @param text
     */
    void setText(String text);

    /**
     * Sets the font (if there is some)
     * @param font
     */
    void setFont(Font.FontList font);

    /**
     * Sets the font size (if possible)
     * @param unit
     * @param fontSize
     * @see TypedValue
     */
    void setFontSize(int unit, float fontSize);
}
