package sharpeye.sharpeye.signs.frontViews;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;

import sharpeye.sharpeye.R;
import sharpeye.sharpeye.utils.Font;

public interface IFrontViews {

    void setVisible();

    void setInvisible();

    void setTextColor(int color);

    void setText(String text);

    void setFont(Font.FontList font);
}
