package sharpeye.sharpeye.signs.frontViews;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import sharpeye.sharpeye.R;
import sharpeye.sharpeye.utils.Font;

/**
 * FrontView dealing with the text for Speed counter
 */
public class SpeedView implements IFrontViews {

    public TextView tvSpeed;
    private Context context;
    private float _fontSize;

    public SpeedView(Context _context)
    {
        context = _context;
        tvSpeed = ((Activity) context).findViewById(R.id.speed);
        _fontSize = tvSpeed.getTextSize();
    }

    @Override
    public final void setVisible() {
        tvSpeed.setVisibility(View.VISIBLE);
    }

    @Override
    public final void setInvisible() {
        tvSpeed.setVisibility(View.INVISIBLE);
    }

    @Override
    public final void setTextColor(int color) {
        tvSpeed.setTextColor(color);
    }

    @Override
    public final void setText(String text) {
        tvSpeed.setText(text);
    }

    @Override
    public final void setFont(Font.FontList font) {
        Font.setForTextView(context.getApplicationContext(), Font.FontList.CHARACTERE, tvSpeed);
        tvSpeed.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    }

    @Override
    public void setFontSize(int unit, float fontSize) {
        if (_fontSize != fontSize) { tvSpeed.setTextSize(unit, fontSize);}
    }

}