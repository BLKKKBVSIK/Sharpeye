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
        void onClick();
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean isChecked);
    }

    /**
     * Create a PopUpFactory to generate a popup
     * @param context context of your activity
     */
    public PopUpFactory(Context context) {
        dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setCancelable(false);
        checkboxView = View.inflate(context, R.layout.popup_checkbox, null);
    }

    /**
     * Sets the Title of the popup
     * @param title title of the popup
     * @return The popupFactory
     */
    public PopUpFactory setTitle(String title) {
        dialogBuilder.setTitle(title);
        return this;
    }

    /**
     * Sets the message inside the popup
     * @param message message in the popup
     * @return The popup factory
     */
    public PopUpFactory setMessage(String message) {
        dialogBuilder.setMessage(message);
        return this;
    }

    /**
     * Set a checkbox with a message and an action
     * @param message message for the checkbox
     * @param listener the function to attach to checkbox
     * @return The popup factory
     */
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

    /**
     * Sets the positive button of the popup
     * @param text button's text
     * @param listener the function to attach to the button
     * @return The popup factory
     */
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

    /**
     * Sets the negative button of the popup
     * @param text button's text
     * @param listener the function to attach to the button
     * @return The popup factory
     */
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

    /**
     * Inflates the popup
     */
    public void show() {
        dialogBuilder.show();
    }
}
