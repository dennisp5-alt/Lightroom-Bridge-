package com.dennis.performancetrial;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

/**
 * Narrow, user-controlled Shizuku A/B trial. This does NOT accelerate another app's CPU/GPU
 * processing. Only changes an app's device-idle whitelist membership when the user requests it.
 * Camera/Lightroom are the ONLY permitted target packages. No arbitrary shell input.
 */
public final class MainActivity extends Activity {
    private static final int BG = 0xff0b101a, PANEL = 0xff171f2e, PANEL2 = 0xff212c3e,
        TEXT = 0xfff4f6fb, MUTED = 0xffa9b8cc, CYAN = 0xff65d7e8, GREEN = 0xff9ae3bd,
        WARN = 0xffffc77a;
    private static final int REQ=201;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final String[] packages={"com.sec.android.app.camera","com.adobe.lrmobile"};
    private final String[] names={"Samsung Camera","Lightroom Mobile"};
    private final TextView[] report = new TextView[2];
    private TextView access;
    private LinearLayout content;
    private final Shizuku.OnRequestPermissionResultListener permissionResult =
        (request,grant)->runOnUiThread(()->{ showAccess();
            if(request==REQ && grant==PackageManager.PERMISSION_GRANTED) notice("Shizuku permission granted. Inspect an app to continue.");
        });
    private final Shizuku.OnBinderReceivedListener binderReceived=()->runOnUiThread(this::showAccess);
    private final Shizuku.OnBinderDeadListener binderDead=()->runOnUiThread(this::showAccess);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        build();
        try {
            Shizuku.addRequestPermissionResultListener(permissionResult);
            Shizuku.addBinderReceivedListenerSticky(binderReceived);
            Shizuku.addBinderDeadListener(binderDead);
        } catch(RuntimeException e){notice("Shizuku library not ready: "+message(e));}
        showAccess();
    }
    @Override protected void onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(permissionResult);
            Shizuku.removeBinderReceivedListener(binderReceived);
            Shizuku.removeBinderDeadListener(binderDead);
        }catch(RuntimeException ignored){}
        worker.shutdownNow();
        super.onDestroy();
    }
    private LinearLayout column() {
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private TextView text(String val,int sp,int color,boolean bold){
        TextView t=new TextView(this);t.setText(val);t.setTextSize(sp);t.setTextColor(color);
        t.setLineSpacing(dp(2),1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private void pad(LinearLayout l,int all){l.setPadding(dp(all),dp(all),dp(all),dp(all));}
    private void gap(LinearLayout l,int height) {View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
    private Button button(String label, int color,Runnable onClick){
        Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextColor(BG);
        b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(shape(color,12));b.setPadding(dp(11),0,dp(11),0);
        b.setOnClickListener(v->onClick.run());return b;
    }
    private void btn(LinearLayout l,String label,int color,Runnable click){
        Button b=button(label,color,click);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(46));p.topMargin=dp(8);l.addView(b,p);
    }
    private LinearLayout card(LinearLayout parent){
        LinearLayout l=column();pad(l,16);l.setBackground(shape(PANEL,18));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(16);
        parent.addView(l,p);return l;
    }
    private void add(LinearLayout l,String s,int sp,int color,boolean bold){l.addView(text(s,sp,color,bold));}
    private void build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(BG);
        content=column();pad(content,18);sc.addView(content);setContentView(sc);
        add(content,"DENNIS  /  ENGINEERING TRIAL",11,CYAN,true);gap(content,8);
        add(content,"Performance Manager",27,TEXT,true);gap(content,5);
        add(content,"SHIZUKU • CAMERA + LIGHTROOM • v0.1.0",11,MUTED,false);gap(content,16);
        LinearLayout top=card(content);
        add(top,"Shizuku connection",18,TEXT,true);gap(top,6);
        access=text("Checking…",13,MUTED,false);top.addView(access);
        btn(top,"Connect / grant Shizuku permission",CYAN,this::requestShizuku);
        add(top,"If Shizuku is stopped, open Shizuku and start it through Wireless debugging first.",12,MUTED,false);
        for(int i=0;i<2;i++)buildTarget(i);
        LinearLayout intro=card(content);
        add(intro,"What this test changes",17,TEXT,true);gap(intro,8);
        add(intro,"Only the chosen app's device-idle background exemption, if Android allows it. It may help interrupted background work; it is not a camera image-quality boost, GPU override, or CPU overclock.",13,MUTED,false);
        gap(intro,10);
        add(intro,"A/B procedure",15,TEXT,true);
        add(intro,"1. Inspect the app. Record a baseline task time with trial OFF.\n2. Enable trial and repeat the same task under similar conditions.\n3. Record the trial time. Repeat 3 times and compare consistency.\n4. Restore the original state when finished.",13,MUTED,false);
        gap(intro,10);
        add(intro,"No telemetry, no network permission, no root and no changes to the app's files, image processing or GPU drivers.",12,GREEN,false);
        gap(content,8);
    }
    private void buildTarget(int i) {
        final String pkg=packages[i];
        LinearLayout l=card(content);
        add(l,names[i],20,TEXT,true);
        add(l,pkg,12,MUTED,false);
        gap(l,8);
        String info=i==0?"Samsung Camera is typically foreground-optimised already. Background exemption may have no effect.":
            "Lightroom trial may affect some background behaviour, but not export speed or image quality while in the foreground.";
        add(l,info,13,MUTED,false);gap(l,10);
        report[i]=text("Inspect first; no changes have been made.",12,CYAN,false);l.addView(report[i]);
        btn(l,"Inspect current settings",0xffa8c0e8,()->inspect(i));
        btn(l,"Enable reversible background trial",GREEN,()->enable(i));
        btn(l,"Restore original setting",WARN,()->restore(i));
        btn(l,"Open "+names[i],0xffc9d4e6,()->launch(i));
        gap(l,12);
        add(l,"A/B TASK TIME  •  SECONDS",11,MUTED,true);gap(l,7);
        EditText base=input(i,"baseline","Baseline seconds (OFF)");
        EditText trial=input(i,"trial","Trial seconds (ON)");
        l.addView(base);gap(l,6);l.addView(trial);gap(l,7);
        TextView diff=text("",12,GREEN,false);l.addView(diff);
        Runnable recalc=()->{
            try{
                double a=Double.parseDouble(base.getText().toString()),b=Double.parseDouble(trial.getText().toString());
                if(a>0&&b>0)diff.setText(String.format(Locale.US,"Elapsed-time change: %+.1f%%  (negative = faster; repeat to confirm)",(b-a)/a*100));
                else diff.setText("");
            }catch(NumberFormatException e){diff.setText("Enter both times to compare.");}
        };
        TextWatcher watch=new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){recalc.run();}
            public void afterTextChanged(Editable e){}
        };
        base.addTextChangedListener(watch);trial.addTextChangedListener(watch);recalc.run();
    }
    private EditText input(int i,String field,String hint){
        String key=packages[i]+"."+field;
        EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(14);
        e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setHint(hint);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setBackground(shape(PANEL2,9));e.setPadding(dp(12),dp(10),dp(12),dp(10));
        e.setText(getPreferences(0).getString(key,""));
        e.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                getPreferences(0).edit().putString(key,s.toString()).apply();
            }
            public void afterTextChanged(Editable s){}
        });
        return e;
    }
    private void showAccess(){
        if(access==null)return;
        boolean running=false;boolean granted=false;
        try{
            running=Shizuku.pingBinder();
            granted=running&&Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;
        }catch(RuntimeException ignored){}
        access.setText(!running?"Not running. Start Shizuku on the phone first.":
            granted?"Connected. Permission granted. Ready for inspection.":"Running. Tap the permission button.");
        access.setTextColor(granted?GREEN:WARN);
    }
    private void requestShizuku(){
        try {
            if(!Shizuku.pingBinder()){notice("Start Shizuku via Wireless debugging, then reopen this screen.");return;}
            if(Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED){notice("Already authorised.");showAccess();return;}
            if(Shizuku.shouldShowRequestPermissionRationale()){
                notice("Permission was denied. Open Shizuku → Authorised applications to grant access.");
                return;
            }
            Shizuku.requestPermission(REQ);
        }catch(RuntimeException e){notice("Shizuku: "+message(e));}
    }
    private boolean ready(){
        try{return Shizuku.pingBinder()&&Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;}
        catch(RuntimeException e){return false;}
    }
    private void notice(String s){
        android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_LONG).show();
    }
    private boolean installed(String pkg){
        try { getPackageManager().getPackageInfo(pkg,0);return true; }
        catch(PackageManager.NameNotFoundException e){return false;}
    }
    private interface Work{String run() throws Exception;}
    private void run(int i,Work w){
        if(!ready()){notice("Connect and authorise Shizuku first.");return;}
        final String pkg=packages[i];
        if(!installed(pkg)){report[i].setText("Package not installed or not visible: "+pkg);return;}
        report[i].setText("Running diagnostic…");
        worker.submit(()->{
            String s;try{s=w.run();}catch(Exception e){s="Error: "+message(e);}
            String result=s;runOnUiThread(()->{if(!isFinishing())report[i].setText(result);});
        });
    }
    private static String message(Throwable e){
        String s=e.getMessage();return s==null?e.getClass().getSimpleName():s;
    }
    private record Cmd(int exit,String output){}
    private Cmd command(String cmd)throws Exception {
        // Targets are fixed constants, not user input. Shizuku.newProcess is deprecated in favour
        // of UserService; acceptable for this small trial, replace if it proves worthwhile.
        ShizukuRemoteProcess p=Shizuku.newProcess(new String[]{"sh","-c",cmd+" 2>&1"},null,null);
        try{
            ByteArrayOutputStream buf=new ByteArrayOutputStream();
            try(InputStream in=p.getInputStream()){
                byte[] b=new byte[2048];int n;
                while((n=in.read(b))>=0){buf.write(b,0,n);if(buf.size()>64000)break;}
            }
            int code=p.waitFor();
            return new Cmd(code,buf.toString(StandardCharsets.UTF_8));
        }finally{p.destroy();}
    }
    private boolean exempt(String pkg)throws Exception{
        Cmd c=command("cmd deviceidle whitelist");
        if(c.exit!=0)throw new IllegalStateException("Cannot read device-idle whitelist: "+c.output.trim());
        // Typical format is 'user,package.name,uid' or 'system,package.name,uid'.
        String regex="(?m)^\\s*(?:user|system)\\s*,\\s*"+Pattern.quote(pkg)+"\\s*,";
        if(!c.output.contains(",")&&c.output.trim().length()>0)
            throw new IllegalStateException("Unrecognised whitelist output; refusing to modify settings.");
        return Pattern.compile(regex).matcher(c.output).find();
    }
    private void inspect(int i){
        run(i,()->{
            String pkg=packages[i];
            boolean x=exempt(pkg);
            Cmd standby=command("am get-standby-bucket "+pkg);
            boolean managed=getPreferences(0).getBoolean(pkg+".managed",false);
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            String thermal=pm==null?"unavailable":String.valueOf(pm.getCurrentThermalStatus());
            return "Device-idle exempt: "+(x?"YES":"NO")+
               "\nStandby bucket: "+(standby.exit==0?standby.output.trim():"unavailable")+
               "\nThis app added exemption: "+(managed?"YES (restore available)":"NO")+
               "\nPhone thermal status: "+thermal+" (from this app, not the target)"+
               "\nNo GPU/CPU acceleration is being forced.";
        });
    }
    private void enable(int i){
        run(i,()->{
            String pkg=packages[i];
            boolean before=exempt(pkg);
            if(before)return "Already exempt. No change made. A/B performance trial cannot be attributed to this setting.";
            // Save intent first so an interruption leaves a Restore option, even if the write succeeds.
            getPreferences(0).edit().putBoolean(pkg+".managed",true).commit();
            Cmd c=command("cmd deviceidle whitelist +"+pkg);
            boolean after=exempt(pkg);
            if(!after)return "Unable to enable on this Samsung/Android version. Command: "+c.output.trim()+
                "\nUse Restore to clear the trial marker.";
            return "TRIAL ON: background device-idle exemption added.\nThis does NOT change camera processing, GPU scheduling, or Lightroom foreground export speed.\nReturn here and restore when testing ends.";
        });
    }
    private void restore(int i){
        run(i,()->{
            String pkg=packages[i];
            if(!getPreferences(0).getBoolean(pkg+".managed",false))
                return "No changes by this app to restore. Existing system exemptions are left untouched.";
            Cmd c=command("cmd deviceidle whitelist -"+pkg);
            if(exempt(pkg))return "Restore failed or system-enforced exemption remains. Command: "+c.output.trim()+
              "\nOriginal status was not exempt. Try again or inspect Android settings.";
            getPreferences(0).edit().putBoolean(pkg+".managed",false).commit();
            return "RESTORED: exemption removed. App returned to its original non-exempt status.";
        });
    }
    private void launch(int i){
        String pkg=packages[i];
        Intent intent=getPackageManager().getLaunchIntentForPackage(pkg);
        if(intent==null){notice("Could not locate launch action for "+pkg);return;}
        try{startActivity(intent);}catch(RuntimeException e){notice("Unable to launch: "+message(e));}
    }
}
