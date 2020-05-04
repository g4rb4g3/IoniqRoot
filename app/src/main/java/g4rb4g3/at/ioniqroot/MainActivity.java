package g4rb4g3.at.ioniqroot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements View.OnClickListener {
  public static final String TAG = "IoniqRoot";

  public static final String PREFERENCES_NAME = "preferences";
  public static final String PREFERENCE_DISCLAIMER_APPROVED = "disclaimer_approved";

  private SharedPreferences mSharedPreferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mSharedPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

    if (!mSharedPreferences.getBoolean(PREFERENCE_DISCLAIMER_APPROVED, false)) {
      showWarning();
    }

    setContentView(R.layout.activity_main);
    findViewById(R.id.btn_telnet_start).setOnClickListener(this);
    findViewById(R.id.btn_telnet_stop).setOnClickListener(this);
    findViewById(R.id.btn_uninstall_microg).setOnClickListener(this);
    findViewById(R.id.btn_mount_system_rw).setOnClickListener(this);
    findViewById(R.id.btn_mount_system_ro).setOnClickListener(this);
    findViewById(R.id.btn_reboot).setOnClickListener(this);
    findViewById(R.id.btn_install_microg).setOnClickListener(this);
    findViewById(R.id.btn_install_stock_apks).setOnClickListener(this);
    findViewById(R.id.btn_eng_upgrade_activity).setOnClickListener(this);
    findViewById(R.id.btn_eng_menu).setOnClickListener(this);
    findViewById(R.id.btn_refresh_ip).setOnClickListener(this);

    setIp();
  }

  private void showWarning() {
    new AlertDialog.Builder(this)
        .setCancelable(false)
        .setTitle(R.string.disclaimer_title)
        .setMessage(R.string.disclaimer)
        .setPositiveButton(R.string.ok, (dialog, which) -> {
          SharedPreferences.Editor editor = mSharedPreferences.edit();
          editor.putBoolean(PREFERENCE_DISCLAIMER_APPROVED, true);
          editor.commit();

          dialog.dismiss();
        })
        .setNegativeButton(R.string.exit, (dialog, which) -> ((Activity) getApplicationContext()).finish())
        .show();
  }

  private void setIp() {
    try {
      List<String> ips = Telnet.getIPAddresses();
      if (ips.size() == 0) {
        ((TextView) findViewById(R.id.tv_ip)).setText(R.string.error_getting_ip);
      } else {
        StringBuilder sb = new StringBuilder();
        for (String ip : ips) {
          if (sb.length() > 0) {
            sb.append(" ").append(getString(R.string.or)).append(" ");
          }
          sb.append(ip);
        }
        ((TextView) findViewById(R.id.tv_ip)).setText(sb.toString());
      }
    } catch (SocketException e) {
      handleException(e);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_telnet_start:
        try {
          Telnet.getInstance().start();
        } catch (RemoteException e) {
          handleException(e);
        }
        break;
      case R.id.btn_telnet_stop:
        try {
          Telnet.getInstance().stop();
        } catch (RemoteException e) {
          handleException(e);
        }
        break;
      case R.id.btn_mount_system_rw:
        try {
          mountSystemRw();
        } catch (RemoteException e) {
          handleException(e);
        }
        break;
      case R.id.btn_mount_system_ro:
        try {
          mountSystemRo();
        } catch (RemoteException e) {
          handleException(e);
        }
        break;
      case R.id.btn_reboot:
        reboot();
        break;
      case R.id.btn_install_microg:
        installMicroG();
        break;
      case R.id.btn_uninstall_microg:
        uninstallMicroG();
        break;
      case R.id.btn_install_stock_apks:
        installStockApks();
        break;
      case R.id.btn_eng_upgrade_activity:
        try {
          ProcessExecutor.executeRootCommand("am start -n com.lge.ivi.engineermode/com.lge.ivi.engineermode.UpgradeActivity");
        } catch (RemoteException e) {
          handleException(e);
        }
        break;
      case R.id.btn_eng_menu:
        try {
          ProcessExecutor.executeRootCommand("am start -n com.lge.ivi.engineermode/com.lge.ivi.engineermode.Engineering");
        } catch (RemoteException e) {
          handleException(e);
        }
        break;
      case R.id.btn_refresh_ip:
        setIp();
        break;
    }
  }

  private void reboot() {
    try {
      ProcessExecutor.executeRootCommand("reboot");
    } catch (RemoteException e) {
      handleException(e);
    }
  }

  private void installStockApks() {
    if (!"XX.EUR.SOP.00.191209".equals(getFwVersion())) {
      Toast.makeText(this, getString(R.string.wrong_fw_version), Toast.LENGTH_LONG).show();
      return;
    }

    final String[] files;
    try {
      files = getAssets().list("stock/apk");
    } catch (IOException e) {
      handleException(e);
      return;
    }

    final ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setTitle(getString(R.string.restoring_stock_apk));
    progressDialog.setMessage("");
    progressDialog.setCancelable(false);
    progressDialog.setMax(files.length);
    progressDialog.show();

    new Thread(() -> {
      boolean success = true;
      try {
        mountSystemRw();

        for (final String file : files) {
          runOnUiThread(() -> {
            progressDialog.setMessage(file);
            progressDialog.incrementProgressBy(1);
          });

          String filepath = extractAsset("stock/apk/" + file, file);
          if (filepath == null) {
            throw new FileNotFoundException("error extracting asset " + file);
          }

          ProcessExecutor.executeRootCommand(getCpString(filepath, "/system/app/" + file));
          ProcessExecutor.executeRootCommand("chmod 644 /system/app/" + file);

          new File(filepath).delete();
        }
      } catch (RemoteException | FileNotFoundException e) {
        handleException(e);
        success = false;
      } finally {
        try {
          mountSystemRo();
        } catch (RemoteException e) {
          handleException(e);
        }
        progressDialog.dismiss();
        if (success) {
          askReboot();
        } else {
          showFailed();
        }
      }
    }).start();
  }

  private void handleException(Exception e) {
    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    Log.e(TAG, e.getMessage(), e);
  }

  private void installMicroG() {
    if (new File("/system/bin/app_process.orig").exists()) {
      Toast.makeText(this, R.string.already_installed, Toast.LENGTH_LONG).show();
      return;
    }

    final ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.setTitle(getString(R.string.installing_microg));
    progressDialog.setMessage("");
    progressDialog.setCancelable(false);
    progressDialog.show();

    new Thread(() -> {
      boolean success = true;
      try {
        mountSystemRw();

        //root device and install supersu
        setDialogMessage(progressDialog, getString(R.string.installing) + " SuperSU");
        String filepath = extractAsset("su/bin/su", "su");
        if (filepath == null) {
          success = false;
          return;
        }
        success = installApk("su/apk/Superuser.apk", "superuser.apk");
        if (!success) {
          return;
        }
        ProcessExecutor.executeRootCommand(getCpString(filepath, "/system/xbin/su"));
        ProcessExecutor.executeRootCommand("chmod 6755 /system/xbin/su");

        //install xposed framework
        setDialogMessage(progressDialog, getString(R.string.installing) + " Xposed framework");
        String filepathAppProcess = extractAsset("xposed/bin/app_process", "app_process");
        if (filepathAppProcess == null) {
          success = false;
          return;
        }
        ProcessExecutor.executeRootCommand("mv /system/bin/app_process /system/bin/app_process.orig");
        ProcessExecutor.executeRootCommand(getCpString(filepathAppProcess, "/system/bin/app_process"));
        ProcessExecutor.executeRootCommand("chmod 755 /system/bin/app_process");
        ProcessExecutor.executeRootCommand("chown root:shell /system/bin/app_process");

        success = installApk("xposed/apk/de.robv.android.xposed.installer_v32_de4f0d.apk", "xposed.apk");
        if (!success) {
          return;
        }

        String owner = getOwner("/data/data", "de.robv.android.xposed.installer");
        if (owner == null) {
          success = false;
          return;
        }

        filepath = extractAsset("xposed/data/bin/XposedBridge.jar", "XposedBridge.jar");
        ProcessExecutor.executeRootCommand("mkdir /data/data/de.robv.android.xposed.installer/bin");
        ProcessExecutor.executeRootCommand(getCpString(filepath, "/data/data/de.robv.android.xposed.installer/bin/XposedBridge.jar.newversion"));
        ProcessExecutor.executeRootCommand(getCpString(filepathAppProcess, "/data/data/de.robv.android.xposed.installer/bin/app_process"));
        ProcessExecutor.executeRootCommand("chmod 775 /data/data/de.robv.android.xposed.installer/bin/");
        ProcessExecutor.executeRootCommand("chmod 644 /data/data/de.robv.android.xposed.installer/bin/XposedBridge.jar");
        ProcessExecutor.executeRootCommand("chmod 700 /data/data/de.robv.android.xposed.installer/bin/app_process");

        //install signature fake xposed module
        setDialogMessage(progressDialog, getString(R.string.installing) + " FakeGapps");
        success = installApk("xposed/apk/com.thermatk.android.xf.fakegapps_v3_bfc686.apk", "fakegapps.apk");
        if (!success) {
          return;
        }

        ProcessExecutor.executeRootCommand("mkdir /data/data/de.robv.android.xposed.installer/conf");
        filepath = extractAsset("xposed/data/conf/modules.list", "modules.list");
        ProcessExecutor.executeRootCommand(getCpString(filepath, "/data/data/de.robv.android.xposed.installer/conf/modules.list"));
        ProcessExecutor.executeRootCommand("chmod 775 /data/data/de.robv.android.xposed.installer/conf/");
        ProcessExecutor.executeRootCommand("chmod 664 /data/data/de.robv.android.xposed.installer/conf/*");

        ProcessExecutor.executeRootCommand("mkdir /data/data/de.robv.android.xposed.installer/shared_prefs");
        filepath = extractAsset("xposed/data/shared_prefs/enabled_modules.xml", "enabled_modules.xml");
        ProcessExecutor.executeRootCommand(getCpString(filepath, "/data/data/de.robv.android.xposed.installer/shared_prefs/enabled_modules.xml"));
        ProcessExecutor.executeRootCommand("chmod 775 /data/data/de.robv.android.xposed.installer/shared_prefs/");
        ProcessExecutor.executeRootCommand("chmod 664 /data/data/de.robv.android.xposed.installer/shared_prefs/*");

        for (String dir : new String[]{"bin", "conf", "shared_prefs"}) {
          StringBuilder cmd = new StringBuilder("busybox find /data/data/de.robv.android.xposed.installer/").append(dir).append(" | busybox xargs chown ").append(owner).append(":").append(owner);
          ProcessExecutor.executeRootCommand(cmd.toString());
        }

        setDialogMessage(progressDialog, getString(R.string.installing) + " microG");
        success = installApk("microg/apk/GmsCore-v0.2.10.19420.apk", "GmsCore.apk");
        if (!success) {
          return;
        }
        success = installApk("microg/apk/BlankStore.apk", "BlankStore.apk");
        if (!success) {
          return;
        }
      } catch (RemoteException e) {
        handleException(e);
        success = false;
      } finally {
        try {
          mountSystemRo();
        } catch (RemoteException e) {
          handleException(e);
        }
        clearCache();
        progressDialog.dismiss();
        if (success) {
          askReboot();
        } else {
          showFailed();
        }
      }
    }).start();
  }

  private void uninstallMicroG() {
    if (!new File("/system/bin/app_process.orig").exists()) {
      Toast.makeText(this, R.string.not_installed, Toast.LENGTH_LONG).show();
      return;
    }

    final ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.setTitle(getString(R.string.uninstalling_microg));
    progressDialog.setMessage("");
    progressDialog.setCancelable(false);
    progressDialog.show();

    new Thread(() -> {
      boolean success = true;
      try {
        final String[] packages = new String[]{"eu.chainfire.supersu", "de.robv.android.xposed.installer", "com.thermatk.android.xf.fakegapps", "com.google.android.gms", "com.android.vending"};
        for (String pkg : packages) {
          setDialogMessage(progressDialog, pkg);
          ProcessExecutor.executeRootCommand("pm uninstall " + pkg);
        }
        mountSystemRw();
        ProcessExecutor.executeRootCommand("rm /system/xbin/su");
        ProcessExecutor.executeRootCommand("mv /system/bin/app_process.orig /system/bin/app_process");

      } catch (RemoteException e) {
        handleException(e);
        success = false;
      } finally {
        try {
          mountSystemRo();
        } catch (RemoteException e) {
          handleException(e);
        }
        progressDialog.dismiss();
        if (success) {
          askReboot();
        } else {
          showFailed();
        }
      }
    }).start();
  }

  private String getCpString(String filepath, String target) {
    StringBuilder cmd = new StringBuilder("cp ")
        .append(filepath)
        .append(" ")
        .append(target);
    return cmd.toString();
  }

  private void mountSystemRw() throws RemoteException {
    ProcessExecutor.executeRootCommand("/system/bin/dd if=/dev/zero of=/dev/block/wrs_ss0p0 bs=80 count=1");
    ProcessExecutor.executeRootCommand("mount -o remount,rw /system");
  }

  private void mountSystemRo() throws RemoteException {
    ProcessExecutor.executeRootCommand("sync -f /system");
    ProcessExecutor.executeRootCommand("mount -o remount,ro /system");
  }

  private boolean installApk(String assetPath, String filename) throws RemoteException {
    String filepath = extractAsset(assetPath, filename);
    if (filepath == null) {
      return false;
    }

    StringBuilder cmd = new StringBuilder("chmod 644 ").append(filepath);
    ProcessExecutor.executeRootCommand(cmd.toString());

    cmd = new StringBuilder("pm install -r ").append(filepath);
    ProcessExecutor.executeRootCommand(cmd.toString());

    return true;
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
      handleException(e);
      return null;
    }
  }

  private String getOwner(String path, String directory) throws RemoteException {
    StringBuilder cmd = new StringBuilder("ls -l ")
        .append(path)
        .append("| grep ")
        .append(directory)
        .append(" > /data/data/g4rb4g3.at.ioniqroot/cache/owner.txt");
    ProcessExecutor.executeRootCommand(cmd.toString());
    ProcessExecutor.executeRootCommand("chmod 666 /data/data/g4rb4g3.at.ioniqroot/cache/owner.txt");
    String details = ProcessExecutor.execute("cat /data/data/g4rb4g3.at.ioniqroot/cache/owner.txt");
    Pattern pattern = Pattern.compile("u0_a[0-9][0-9]");
    Matcher matcher = pattern.matcher(details);
    if (matcher.find()) {
      return matcher.group();
    }
    return null;
  }

  private String getFwVersion() {
    try {
      Class aClass = Class.forName("android.os.SystemProperties");
      Method method = aClass.getDeclaredMethod("get", String.class);
      return (String) method.invoke(null, "ro.lge.fw_version");
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      handleException(e);
    }
    return null;
  }

  private void setDialogMessage(final ProgressDialog progressDialog, final String message) {
    runOnUiThread(() -> progressDialog.setMessage(message));
  }
  
  private void showFailed() {
    runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(R.string.process_failed), Toast.LENGTH_LONG).show());
  }

  private void askReboot() {
    runOnUiThread(() -> {
      new AlertDialog.Builder(this)
          .setMessage(R.string.successfully_completed)
          .setPositiveButton(R.string.reboot, (dialog, which) -> {
            reboot();
          })
          .setNegativeButton(R.string.cancel, (dialog, which) -> {
            dialog.dismiss();
          })
          .show();
    });
  }

  private void clearCache() {
    for (File file : getCacheDir().listFiles()) {
      file.delete();
    }
  }
}
