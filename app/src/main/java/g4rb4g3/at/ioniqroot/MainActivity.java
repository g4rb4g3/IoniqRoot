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
    findViewById(R.id.btn_extract_gapps).setOnClickListener(this);
    findViewById(R.id.btn_reboot).setOnClickListener(this);
    findViewById(R.id.btn_update_gapps).setOnClickListener(this);
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
      case R.id.btn_extract_gapps:
        extractGapps();
        break;
      case R.id.btn_reboot:
        ProcessExecutor.executeRootCommand(this, "reboot");
        break;
      case R.id.btn_update_gapps:
        updateGapps();
        break;
    }
  }

  private void mountSystemRw() {
    ProcessExecutor.executeRootCommand(this, "mount -o remount,rw /system");
  }

  private void mountSystemRo() {
    ProcessExecutor.executeRootCommand(this, "sync -f /system");
    ProcessExecutor.executeRootCommand(this, "mount -o remount,ro /system");
  }

  private void extractGapps() {
    if (!isRoboEnabled()) {
      return;
    }

    makeSystemWriteable();

    String filepath = extractAsset("gapps.tar");
    if (filepath == null) {
      return;
    }

    StringBuilder cmd = new StringBuilder("busybox tar xf ")
        .append(filepath)
        .append(" -C /system/");
    ProcessExecutor.executeRootCommand(this, cmd.toString());

    mountSystemRo();
  }

  private void updateGapps() {
    if (!isRoboEnabled()) {
      return;
    }

    if (!isPackageInstalled("com.google.android.gms")) {
      return;
    }

    String filepath = extractAsset("com.google.android.gms.apk");
    if (filepath == null) {
      return;
    }

    StringBuilder cmd = new StringBuilder("chmod 644 ").append(filepath);
    ProcessExecutor.executeRootCommand(this, cmd.toString());

    cmd = new StringBuilder("pm install -r ").append(filepath);
    ProcessExecutor.executeRootCommand(this, cmd.toString());

    filepath = extractAsset("com.android.vending.apk");
    if (filepath == null) {
      return;
    }

    cmd = new StringBuilder("chmod 644 ").append(filepath);
    ProcessExecutor.executeRootCommand(this, cmd.toString());

    cmd = new StringBuilder("pm install -r ").append(filepath);
    ProcessExecutor.executeRootCommand(this, cmd.toString());
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

  private String extractAsset(String filename) {
    try {
      File f = new File(getCacheDir() + "/" + filename);
      InputStream is = getAssets().open(filename);
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
      Log.e(TAG, "error extracting asset " + filename, e);
      return null;
    }
  }
}
