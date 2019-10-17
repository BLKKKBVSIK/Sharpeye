package sharpeye.sharpeye.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import sharpeye.sharpeye.R;

public class PopUpFactory {

    private AlertDialog.Builder dialogBuilder;
    private CheckBox checkbox;

    public interface OnClickListener {
        public void onClick();
    }

    public interface OnCheckedChangeListener {
        public void onCheckedChanged(boolean isChecked);
    }

    public PopUpFactory(Context context) {
        dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setCancelable(false);
        View view = View.inflate(context, R.layout.popup_checkbox, null);
        checkbox = view.findViewById(R.id.popup_checkbox);
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
        checkbox.setText(message);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                listener.onCheckedChanged(isChecked);
            }
        });
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