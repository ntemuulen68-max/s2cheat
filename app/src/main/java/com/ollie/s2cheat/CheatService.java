package com.ollie.s2cheat;
import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.IBinder;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import java.util.*;
public class CheatService extends Service {
    static { System.loadLibrary("ollie_s2_full"); }
    public native void nativeTick();
    public native void setFeature(String key,boolean val,float fval);
    public native float[] getEspData();
    private WindowManager wm;
    private LinearLayout menuView;
    private ScrollView menuScroll;
    private TextView toggleBtn;
    private EspView espView;
    private int overlayType;
    private final Map<String,Boolean> toggles=new LinkedHashMap<String,Boolean>(){{
        put("godMode",false);put("infiniteAmmo",false);put("aimbot",false);
        put("espBox",false);put("espHealth",false);put("espDist",false);
        put("noRecoil",false);put("noSpread",false);put("bunnyHop",false);
        put("speedHack",false);put("skinChanger",false);put("halloweenScore",false);
    }};
    class EspView extends android.view.View {
        Paint box=new Paint(),txt=new Paint(),hp=new Paint();
        float[] data=new float[]{0};
        EspView(android.content.Context c){super(c);
            box.setColor(0xFFB44FFF);box.setStyle(Paint.Style.STROKE);box.setStrokeWidth(2f);
            txt.setColor(0xFFFFFFFF);txt.setTextSize(24f);
            hp.setColor(0xFF44FF88);hp.setStyle(Paint.Style.FILL);}
        @Override protected void onDraw(Canvas c){
            float[]d=data;if(d==null||d.length<1)return;
            int n=(int)d[0],stride=6;
            for(int i=0;i<n;i++){int b=1+i*stride;if(b+stride>d.length)break;
                float hx=d[b],hy=d[b+1],fx=d[b+2],fy=d[b+3],dist=d[b+4],health=d[b+5];
                if(hx<0)continue;float h=fy-hy,w=h*0.4f;
                if(Boolean.TRUE.equals(toggles.get("espBox")))c.drawRect(hx-w,hy,hx+w,fy,box);
                if(Boolean.TRUE.equals(toggles.get("espHealth"))){float bh=Math.max(2f,h*(health/100f));c.drawRect(hx-w-8,fy-bh,hx-w-4,fy,hp);}
                if(Boolean.TRUE.equals(toggles.get("espDist")))c.drawText(String.format("%.0fm",dist),hx-w,hy-4,txt);}}
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onCreate(){
        super.onCreate();
        startForegroundNotif();
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        overlayType=SamsungCompat.getOverlayType();
        buildEsp();buildBtn();buildMenu();startTick();}
    private void buildEsp(){
        espView=new EspView(this);
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        wm.addView(espView,p);}
    private void buildBtn(){
        toggleBtn=new TextView(this);
        toggleBtn.setText("@n1crolord");
        toggleBtn.setTextColor(0xFFFFFFFF);
        toggleBtn.setTextSize(13f);
        toggleBtn.setTypeface(Typeface.DEFAULT_BOLD);
        toggleBtn.setPadding(18,10,18,10);
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xFF6A0DAD,0xFFB44FFF});
        g.setCornerRadius(40f);toggleBtn.setBackground(g);
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.START;p.x=16;p.y=48;
        makeDrag(toggleBtn,p);wm.addView(toggleBtn,p);}
    private void buildMenu(){
        menuScroll=new ScrollView(this);
        menuView=new LinearLayout(this);
        menuView.setOrientation(LinearLayout.VERTICAL);
        menuView.setPadding(16,16,16,20);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xF01A0030,0xF0280050});
        bg.setCornerRadius(20f);menuView.setBackground(bg);
        TextView t=new TextView(this);t.setText("olliE S2 v1.1");
        t.setTextColor(0xFFCF9FFF);t.setTextSize(16f);t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);t.setPadding(0,0,0,12);menuView.addView(t);
        for(Map.Entry<String,Boolean>e:toggles.entrySet())menuView.addView(makeToggle(e.getKey()));
        menuScroll.addView(menuView);menuScroll.setVisibility(View.GONE);
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(360,900,
            overlayType,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.START;p.x=16;p.y=90;
        wm.addView(menuScroll,p);
        toggleBtn.setOnClickListener(v->{
            if(menuScroll.getVisibility()==View.VISIBLE){
                Animation o=new AlphaAnimation(1f,0f);o.setDuration(180);
                o.setAnimationListener(new Animation.AnimationListener(){
                    public void onAnimationStart(Animation a){}
                    public void onAnimationRepeat(Animation a){}
                    public void onAnimationEnd(Animation a){menuScroll.setVisibility(View.GONE);}});
                menuScroll.startAnimation(o);
            }else{menuScroll.setVisibility(View.VISIBLE);
                Animation i=new AlphaAnimation(0f,1f);i.setDuration(200);menuScroll.startAnimation(i);}});}
    private Switch makeToggle(String key){
        Switch sw=new Switch(this);sw.setText(key);sw.setTextColor(0xFFEEEEEE);sw.setChecked(false);
        sw.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFB44FFF));
        sw.setTrackTintList(android.content.res.ColorStateList.valueOf(0xFF6A0DAD));
        sw.setOnCheckedChangeListener((v,c)->{toggles.put(key,c);setFeature(key,c,1f);});return sw;}
    private void makeDrag(View v,WindowManager.LayoutParams p){
        v.setOnTouchListener(new View.OnTouchListener(){int ix,iy;
            public boolean onTouch(View vw,MotionEvent e){
                switch(e.getAction()){
                    case MotionEvent.ACTION_DOWN:ix=(int)e.getRawX()-p.x;iy=(int)e.getRawY()-p.y;return true;
                    case MotionEvent.ACTION_MOVE:p.x=(int)e.getRawX()-ix;p.y=(int)e.getRawY()-iy;
                        wm.updateViewLayout(v,p);return true;}return false;}});}
    private void startTick(){
        Thread t=new Thread(()->{while(!Thread.interrupted()){try{
            nativeTick();
            boolean esp=Boolean.TRUE.equals(toggles.get("espBox"))||Boolean.TRUE.equals(toggles.get("espHealth"))||Boolean.TRUE.equals(toggles.get("espDist"));
            if(esp){float[]d=getEspData();espView.post(()->{espView.data=d;espView.invalidate();});}
            Thread.sleep(16);}catch(InterruptedException e){break;}}});
        t.setDaemon(true);t.start();}
    @Override public void onDestroy(){super.onDestroy();
        try{if(menuScroll!=null)wm.removeView(menuScroll);}catch(Exception ignored){}
        try{if(espView!=null)wm.removeView(espView);}catch(Exception ignored){}
        try{if(toggleBtn!=null)wm.removeView(toggleBtn);}catch(Exception ignored){}}
}
