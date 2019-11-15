package sharpeye.sharpeye.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import sharpeye.sharpeye.R;

/**
 * Example d'utilisation:
 * new PopUpFactory(this).setTitle("title").setMessage("message").setPositiveButton("ok", () -> {  }).show();
 */
public class PopUpFactory {

    private AlertDialog.Builder dialogBuilder;
    private View checkboxView;

    public interface OnClickListener {
        public void onClick();
    }

    public interface OnCheckedChangeListener {
        public void onCheckedChanged(boolean isChecked);
    }

    public PopUpFactory(Context context) {
        dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setCancelable(false);
        checkboxView = View.inflate(context, R.layout.popup_checkbox, null);
    }

    public PopUpFactory setTitle(String title) {
        dialogBuilder.setTitle(title);
        return this;
    }

    public PopUpFactory setMessage(String message) {
        dialogBuilder.setMessage(message);
        return this;
    }

    public PopUpFactory setCheckbox(String message, OnCheckedChangeListener listener) {
        CheckBox checkbox = checkboxView.findViewById(R.id.popup_checkbox);
        checkbox.setText(message);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                listener.onCheckedChanged(isChecked);
            }
        });
        dialogBuilder.setView(checkboxView);
        return this;
    }

    public PopUpFactory setPositiveButton(String text, OnClickListener listener) {
        dialogBuilder.setPositiveButton(text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    listener.onClick();
                }
            }
        });
        return this;
    }

    public PopUpFactory setNegativeButton(String text, OnClickListener listener) {
        dialogBuilder.setNegativeButton(text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == AlertDialog.BUTTON_NEGATIVE) {
                    listener.onClick();
                }
            }
        });
        return this;
    }

    public void show() {
        dialogBuilder.show();
    }
}
