package com.ollie.s2cheat;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.view.WindowManager;
public class SamsungCompat {
    public static boolean isSamsung(){
        return Build.MANUFACTURER.equalsIgnoreCase("samsung");
    }
    public static int getOverlayType(android.content.Context ctx){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    }
    public static boolean hasOverlayPermission(Context ctx){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            return Settings.canDrawOverlays(ctx);
        }
        return true;
    }
    public static boolean isGameBoosterBlocking(Context ctx){
        try{
            ctx.getPackageManager().getPackageInfo("com.samsung.android.game.gametools",0);
            return true;
        }catch(PackageManager.NameNotFoundException e){ return false; }
    }
}
