package com.ollie.s2cheat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class CheatService extends Service {

    private WindowManager wm;
    private View btnView;
    private View menuView;
    private boolean menuOpen = false;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotif();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildBtn();
    }

    private void startForegroundNotif() {
        String CH = "ollie_s2_ch";
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(
                CH, "olliE S2 Cheat", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
        Notification n = new Notification.Builder(this, CH)
            .setContentTitle("olliE S2 Active")
            .setContentText("@n1crolord cheat running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build();
        startForeground(1337, n);
    }

    private WindowManager.LayoutParams makeParams(int w, int h) {
        int type = SamsungCompat.getOverlayType(this);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            w, h, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.LEFT;
        p.x = 0; p.y = 100;
        return p;
    }

    private void buildBtn() {
        Button btn = new Button(this);
        btn.setText("@n1crolord");
        btn.setTextSize(10);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(0xFF6A0DAD);
        btnView = btn;
        WindowManager.LayoutParams p = makeParams(180, 70);
        wm.addView(btnView, p);
        makeDraggable(btnView, p);
        btn.setOnClickListener(v -> toggleMenu());
    }

    private void toggleMenu() {
        if (menuOpen) {
            if (menuView != null) { wm.removeView(menuView); menuView = null; }
            menuOpen = false;
        } else {
            buildMenu();
            menuOpen = true;
        }
    }

    private void buildMenu() {
        ScrollView sv = new ScrollView(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setBackgroundColor(0xEE1A0030);
        ll.setPadding(20, 20, 20, 20);

        TextView title = new TextView(this);
        title.setText("olliE S2 Cheat");
        title.setTextColor(0xFFCC00FF);
        title.setTextSize(16);
        ll.addView(title);

        String[] features = {"God Mode","No Recoil","Infinite Ammo","Speed Hack","Aimbot","ESP","Skin Changer","Bunny Hop"};
        for (String f : features) {
            Switch sw = new Switch(this);
            sw.setText(f);
            sw.setTextColor(Color.WHITE);
            ll.addView(sw);
        }

        sv.addView(ll);
        menuView = sv;
        WindowManager.LayoutParams p = makeParams(500, 700);
        p.x = 10; p.y = 180;
        wm.addView(menuView, p);
    }

    private void makeDraggable(View v, WindowManager.LayoutParams p) {
        v.setOnTouchListener(new View.OnTouchListener() {
            float ix, iy; int ox, oy;
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ix = e.getRawX(); iy = e.getRawY();
                        ox = p.x; oy = p.y; return false;
                    case MotionEvent.ACTION_MOVE:
                        p.x = (int)(ox + e.getRawX() - ix);
                        p.y = (int)(oy + e.getRawY() - iy);
                        wm.updateViewLayout(v, p); return true;
                }
                return false;
            }
        });
    }

    @Override
    public int onStartCommand(Intent i, int f, int s) { return START_STICKY; }

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        if (btnView != null) wm.removeView(btnView);
        if (menuView != null) wm.removeView(menuView);
        super.onDestroy();
    }
}
