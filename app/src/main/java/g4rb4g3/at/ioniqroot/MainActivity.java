package g4rb4g3.at.ioniqroot;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements View.OnClickListener {
  private static final String TAG = "MainActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    findViewById(R.id.btn_telnet_start).setOnClickListener(this);
    findViewById(R.id.btn_telnet_stop).setOnClickListener(this);
    findViewById(R.id.btn_goto_cts).setOnClickListener(this);
    findViewById(R.id.btn_mount_system_rw).setOnClickListener(this);
    findViewById(R.id.btn_mount_system_ro).setOnClickListener(this);
    findViewById(R.id.btn_reboot).setOnClickListener(this);
    findViewById(R.id.btn_install_microg).setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_telnet_start:
        Telnet.getInstance(this).start();
        break;
      case R.id.btn_telnet_stop:
        Telnet.getInstance(this).stop();
        break;
      case R.id.btn_goto_cts:
        ProcessExecutor.executeRootCommand(this, "am start -n com.lge.ivi.ctstest/com.lge.ivi.ctstest.CTSTestEnableScreen");
        break;
      case R.id.btn_mount_system_rw:
        mountSystemRw();
        break;
      case R.id.btn_mount_system_ro:
        mountSystemRo();
        break;
      case R.id.btn_reboot:
        ProcessExecutor.executeRootCommand(this, "reboot");
        break;
      case R.id.btn_install_microg:
        installMicroG();
        break;
    }
  }

  private void installMicroG() {
    if (!isRoboEnabled()) {
      return;
    }

    makeSystemWriteable();

    try {
      //root device and install supersu
      String filepath = extractAsset("su/bin/su", "su");
      if (filepath == null) {
        return;
      }
      boolean success = installApk("su/apk/Superuser.apk", "superuser.apk");
      if (!success) {
        return;
      }
      ProcessExecutor.executeRootCommand(this, getCpString(filepath, "/system/xbin/su"));
      ProcessExecutor.executeRootCommand(this, "chmod 6755 /system/xbin/su");

      //install xposed framework
      String filepathAppProcess = extractAsset("xposed/bin/app_process", "app_process");
      if (filepathAppProcess == null) {
        return;
      }
      ProcessExecutor.executeRootCommand(this, "mv /system/bin/app_process /system/bin/app_process.orig");
      ProcessExecutor.executeRootCommand(this, getCpString(filepathAppProcess, "/system/bin/app_process"));
      ProcessExecutor.executeRootCommand(this, "chmod 755 /system/bin/app_process");
      ProcessExecutor.executeRootCommand(this, "chown root:shell /system/bin/app_process");

      success = installApk("xposed/apk/de.robv.android.xposed.installer_v32_de4f0d.apk", "xposed.apk");
      if (!success) {
        return;
      }

      String owner = getOwner("/data/data", "de.robv.android.xposed.installer");
      if(owner == null) {
        return;
      }

      filepath = extractAsset("xposed/data/bin/XposedBridge.jar", "XposedBridge.jar");
      ProcessExecutor.executeRootCommand(this, "mkdir /data/data/de.robv.android.xposed.installer/bin");
      ProcessExecutor.executeRootCommand(this, getCpString(filepath, "/data/data/de.robv.android.xposed.installer/bin/XposedBridge.jar.newversion"));
      ProcessExecutor.executeRootCommand(this, getCpString(filepathAppProcess, "/data/data/de.robv.android.xposed.installer/bin/app_process"));
      ProcessExecutor.executeRootCommand(this, "chmod 775 /data/data/de.robv.android.xposed.installer/bin/");
      ProcessExecutor.executeRootCommand(this, "chmod 644 /data/data/de.robv.android.xposed.installer/bin/XposedBridge.jar");
      ProcessExecutor.executeRootCommand(this, "chmod 700 /data/data/de.robv.android.xposed.installer/bin/app_process");

      //install signature fake xposed module
      success = installApk("xposed/apk/com.thermatk.android.xf.fakegapps_v3_bfc686.apk", "fakegapps.apk");
      if (!success) {
        return;
      }

      ProcessExecutor.executeRootCommand(this, "mkdir /data/data/de.robv.android.xposed.installer/conf");
      filepath = extractAsset("xposed/data/conf/modules.list", "modules.list");
      ProcessExecutor.executeRootCommand(this, getCpString(filepath, "/data/data/de.robv.android.xposed.installer/conf/modules.list"));
      ProcessExecutor.executeRootCommand(this, "chmod 775 /data/data/de.robv.android.xposed.installer/conf/");
      ProcessExecutor.executeRootCommand(this, "chmod 664 /data/data/de.robv.android.xposed.installer/conf/*");

      ProcessExecutor.executeRootCommand(this, "mkdir /data/data/de.robv.android.xposed.installer/shared_prefs");
      filepath = extractAsset("xposed/data/shared_prefs/enabled_modules.xml", "enabled_modules.xml");
      ProcessExecutor.executeRootCommand(this, getCpString(filepath, "/data/data/de.robv.android.xposed.installer/shared_prefs/enabled_modules.xml"));
      ProcessExecutor.executeRootCommand(this, "chmod 775 /data/data/de.robv.android.xposed.installer/shared_prefs/");
      ProcessExecutor.executeRootCommand(this, "chmod 664 /data/data/de.robv.android.xposed.installer/shared_prefs/*");

      for(String dir : new String[] {"bin", "conf", "shared_prefs"}) {
        StringBuilder cmd = new StringBuilder("busybox find /data/data/de.robv.android.xposed.installer/").append(dir).append(" | busybox xargs chown ").append(owner).append(":").append(owner);
        ProcessExecutor.executeRootCommand(this, cmd.toString());
      }

      success = installApk("microg/apk/GmsCore-v0.2.10.19420.apk", "GmsCore.apk");
      if(!success) {
        return;
      }
      success = installApk("microg/apk/BlankStore.apk", "BlankStore.apk");
      if(!success) {
        return;
      }
    } finally {
      mountSystemRo();
    }
  }

  private String getCpString(String filepath, String target) {
    StringBuilder cmd = new StringBuilder("cp ")
        .append(filepath)
        .append(" ")
        .append(target);
    return cmd.toString();
  }

  private void mountSystemRw() {
    ProcessExecutor.executeRootCommand(this, "mount -o remount,rw /system");
  }

  private void mountSystemRo() {
    ProcessExecutor.executeRootCommand(this, "sync -f /system");
    ProcessExecutor.executeRootCommand(this, "mount -o remount,ro /system");
  }

  private boolean installApk(String assetPath, String filename) {
    if (!isRoboEnabled()) {
      return false;
    }

    String filepath = extractAsset(assetPath, filename);
    if (filepath == null) {
      return false;
    }

    StringBuilder cmd = new StringBuilder("chmod 644 ").append(filepath);
    ProcessExecutor.executeRootCommand(this, cmd.toString());

    cmd = new StringBuilder("pm install -r ").append(filepath);
    ProcessExecutor.executeRootCommand(this, cmd.toString());

    return true;
  }

  private boolean isRoboEnabled() {
    String robo = ProcessExecutor.execute("getprop persist.sys.robo.enable");
    if (!"true".equals(robo)) {
      Toast.makeText(this, "enable cts test first!", Toast.LENGTH_LONG).show();
      return false;
    }
    return true;
  }

  private void makeSystemWriteable() {
    String ro = ProcessExecutor.execute("mount");
    for (String s : ro.split("\n")) {
      if (s.startsWith("/dev/block/platform/bdm/by-num/p2") && s.contains(" ro,")) {
        mountSystemRw();
        break;
      }
    }
  }

  private boolean isPackageInstalled(String packageName) {
    try {
      PackageManager pm = this.getPackageManager();
      pm.getPackageInfo(packageName, 0);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      Toast.makeText(this, "packages " + packageName + " not installed, extract gapps first!", Toast.LENGTH_LONG).show();
      return false;
    }
  }

  private String extractAsset(String assetPath, String filename) {
    try {
      File f = new File(getCacheDir() + "/" + filename);
      InputStream is = getAssets().open(assetPath);
      FileOutputStream fos = new FileOutputStream(f);
      byte[] buffer = new byte[1024];
      int bufferLength;

      while ((bufferLength = is.read(buffer)) > 0) {
        fos.write(buffer, 0, bufferLength);
      }
      is.read(buffer);
      is.close();
      fos.close();

      return f.getAbsolutePath();
    } catch (Exception e) {
      Log.e(TAG, "error extracting asset " + assetPath, e);
      return null;
    }
  }

  private String getOwner(String path, String directory) {
    StringBuilder cmd = new StringBuilder("ls -l ")
        .append(path)
        .append("| grep ")
        .append(directory)
        .append(" > /data/data/g4rb4g3.at.ioniqroot/cache/owner.txt");
    ProcessExecutor.executeRootCommand(this, cmd.toString());
    ProcessExecutor.executeRootCommand(this, "chmod 666 /data/data/g4rb4g3.at.ioniqroot/cache/owner.txt");
    String details = ProcessExecutor.execute("cat /data/data/g4rb4g3.at.ioniqroot/cache/owner.txt");
    Pattern pattern = Pattern.compile("u0_a[0-9][0-9]");
    Matcher matcher = pattern.matcher(details);
    if(matcher.find()) {
      return matcher.group();
    }
    return null;
  }
}
