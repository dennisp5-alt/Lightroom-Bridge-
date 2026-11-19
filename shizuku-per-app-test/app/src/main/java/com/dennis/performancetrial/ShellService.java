package com.dennis.performancetrial;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Shizuku UserService executes exclusively allowlisted device-idle and standby inspection commands.
 * Runs under Shizuku's shell identity, NOT Android app UID. Never accepts arbitrary commands.
 */
public final class ShellService extends ITrialShell.Stub {
    private static final String CAMERA="com.sec.android.app.camera";
    private static final String LIGHTROOM="com.adobe.lrmobile";
    public ShellService() {}
    private boolean allowed(String cmd) {
        if("cmd deviceidle whitelist".equals(cmd)) return true;
        for(String pkg:new String[]{CAMERA,LIGHTROOM}) {
            if(("am get-standby-bucket "+pkg).equals(cmd))return true;
            if(("cmd deviceidle whitelist +"+pkg).equals(cmd))return true;
            if(("cmd deviceidle whitelist -"+pkg).equals(cmd))return true;
        }
        return false;
    }
    @Override public String exec(String command) {
        if(!allowed(command))return "126\nCommand refused by allowlist";
        Process p=null;
        try{
            p=new ProcessBuilder("sh","-c",command).redirectErrorStream(true).start();
            ByteArrayOutputStream buf=new ByteArrayOutputStream();
            try(InputStream in=p.getInputStream()) {
                byte[] bytes=new byte[2048];int n;
                while((n=in.read(bytes))!=-1) {
                    if(buf.size()+n>64000){buf.write(bytes,0,Math.max(0,64000-buf.size()));break;}
                    buf.write(bytes,0,n);
                }
            }
            int result=p.waitFor();
            return result+"\n"+buf.toString(StandardCharsets.UTF_8);
        }catch(Exception e){
            return "125\n"+e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage());
        }finally{if(p!=null)p.destroy();}
    }
    @Override public void destroy(){System.exit(0);}
}
