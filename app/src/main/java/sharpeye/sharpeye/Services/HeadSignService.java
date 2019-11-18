package sharpeye.sharpeye.Services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import sharpeye.sharpeye.DetectorActivity;
import sharpeye.sharpeye.R;
import sharpeye.sharpeye.SharpeyeApplication;
import sharpeye.sharpeye.utils.CurrentState;
import sharpeye.sharpeye.utils.Font;

public class HeadSignService extends Service {

    private final IBinder binder = new HeadSignService.HeadSignBinder();
    private GPSService mService;
    private boolean mBound = false;
    private WindowManager mWindowManager;
    private View mChatHeadView;
    public Handler handler = null;
    public static Runnable runnable = null;
    private CurrentState currentState;

    public HeadSignService() {
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            GPSService.GPSBinder binder = (GPSService.GPSBinder) service;
            mService = binder.getService();
            mBound = true;
            currentState = mService.getCurrentState();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class HeadSignBinder extends Binder {
        public HeadSignService getService() {
            return HeadSignService.this;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();
        //Inflate the chat head layout we created
        mChatHeadView = LayoutInflater.from(this).inflate(R.layout.layout_chat_head, null);

        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY ,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the chat head position
        params.gravity = Gravity.BOTTOM  | Gravity.LEFT ;
        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mChatHeadView, params);

        //Set the close button.
        final ImageView closeButton = mChatHeadView.findViewById(R.id.close_btn);
        final RelativeLayout rl = mChatHeadView.findViewById(R.id.speed_limit_sign);
        final TextView chatHeadText = mChatHeadView.findViewById(R.id.chat_head_text);
        Font.setForTextView(getApplicationContext(), Font.FontList.CHARACTERE, chatHeadText);
        //final RelativeLayout parentRl= (RelativeLayout) mChatHeadView.findViewById(R.id.chat_head_root);
        final GestureDetector gestureDetector = new GestureDetector(this, new SingleTapConfirm());

        final int rlWidth = 180;
        final int rlHeight = 180;

        Intent intent = new Intent(getApplicationContext(), GPSService.class);
        getApplicationContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //close the service and remove the chat head from the window
                stopSelf();
            }
        });

        rl.setOnTouchListener(new View.OnTouchListener() {

            private int lastAction;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (gestureDetector.onTouchEvent(event)) {
                    /*Intent intent = new Intent(ChatHeadService.this, SharpeyeApplication.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    value += 1;
                    chatHeadText.setText(String.valueOf(value));
                    startActivity(intent);*/
                    return true;
                } else {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            //remember the initial position.
                            initialX = params.x;
                            initialY = params.y;

                            //get the touch location
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();


                            lastAction = event.getAction();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if ((initialX + (int) (event.getRawX() - initialTouchX))
                                    >= mWindowManager.getDefaultDisplay().getWidth() - rlWidth)
                            {
                                params.x = mWindowManager.getDefaultDisplay().getWidth() - rlWidth;
                            } else {
                                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            }
                            if ((initialY + (int) (event.getRawY() - initialTouchY))
                                    >= mWindowManager.getDefaultDisplay().getHeight() - rlHeight) {
                                params.y = mWindowManager.getDefaultDisplay().getHeight() - rlHeight;
                            } else {
                                params.y = initialY - (int) (event.getRawY() - initialTouchY);
                            }
                            mWindowManager.updateViewLayout(mChatHeadView, params);
                            lastAction = event.getAction();
                            return true;
                    }
                }

                return false;
            }
        });
        handler = new Handler();
        runnable = () -> {
            if (mBound)
                currentState = mService.getCurrentState();
            if (currentState != null && currentState.isSpeedLimit())
                chatHeadText.setText(String.valueOf(currentState.getSpeedLimit()));
            handler.postDelayed(runnable, 1000);
        };

        handler.postDelayed(runnable, 1000);
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

    private void unbindService()
    {
        Log.d("gpsstopService", "start");
        if (mService != null && mBound) {
            getApplicationContext().unbindService(connection);
            mBound = false;
        }
        Log.d("gpsstopService", "stop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatHeadView != null) mWindowManager.removeView(mChatHeadView);
        unbindService();
    }
}
