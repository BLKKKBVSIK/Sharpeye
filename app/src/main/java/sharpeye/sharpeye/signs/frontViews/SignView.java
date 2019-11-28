package sharpeye.sharpeye.signs.frontViews;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import sharpeye.sharpeye.R;
import sharpeye.sharpeye.utils.Font;


/**
 * FrontView dealing with the sign for speed limits
 */
public class SignView implements IFrontViews {
    private TextView tvLimit;
    private ImageView emptySign;
    private Context context;

    public SignView(Context _context)
    {
        context = _context;
        tvLimit = ((Activity) context).findViewById(R.id.speed_limit);
        emptySign = ((Activity) context).findViewById(R.id.emptySign);
    }

    @Override
    public final void setVisible()
    {
        tvLimit.setVisibility(View.VISIBLE);
        emptySign.setVisibility(View.VISIBLE);
    }

    @Override
    public final void setInvisible()
    {
        tvLimit.setVisibility(View.INVISIBLE);
        emptySign.setVisibility(View.INVISIBLE);
    }

    @Override
    public final void setTextColor(int color) {
        tvLimit.setTextColor(color);
    }

    @Override
    public final void setText(String text) {
        tvLimit.setText(text);
    }

    @Override
    public final void setFont(Font.FontList font) {
        Font.setForTextView(context.getApplicationContext(), Font.FontList.CHARACTERE, tvLimit);
    }

    @Override
    public void setFontSize(int unit, float fontSize) {
        tvLimit.setTextSize(unit, fontSize);
    }

}
