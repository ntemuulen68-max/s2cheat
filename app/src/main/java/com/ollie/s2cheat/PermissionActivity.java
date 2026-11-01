package com.ollie.s2cheat;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
public class PermissionActivity extends Activity {
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)){
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, 1001);
            return;
        }
        launch();
    }
    @Override protected void onActivityResult(int req,int res,Intent d){
        super.onActivityResult(req,res,d); launch();
    }
    private void launch(){
        Intent i=new Intent(this,CheatService.class);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
            startForegroundService(i);
        else startService(i);
        finish();
    }
}
