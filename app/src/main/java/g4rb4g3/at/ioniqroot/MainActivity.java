package g4rb4g3.at.ioniqroot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity implements View.OnClickListener {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    findViewById(R.id.btn_telnet_start).setOnClickListener(this);
    findViewById(R.id.btn_telnet_stop).setOnClickListener(this);
    findViewById(R.id.btn_goto_cts).setOnClickListener(this);
    findViewById(R.id.btn_mount_system_rw).setOnClickListener(this);
    findViewById(R.id.btn_mount_system_ro).setOnClickListener(this);
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
        ProcessExecutor.executeRootCommand(this, "mount -o remount,rw /system");
        break;
      case R.id.btn_mount_system_ro:
        ProcessExecutor.executeRootCommand(this, "sync -f /system");
        ProcessExecutor.executeRootCommand(this, "mount -o remount,ro /system");
        break;
    }
  }


}
