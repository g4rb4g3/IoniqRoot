package g4rb4g3.at.ioniqroot;

import android.content.Context;
import android.widget.Toast;

import com.lge.ivi.media.ExtMediaManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Telnet {
  private static Context sContext;
  private static Telnet sInstance;

  public static Telnet getInstance(Context context) {
    if (sInstance == null) {
      sInstance = new Telnet();
      sContext = context;
    }
    return sInstance;
  }

  public void start() {
    ExtMediaManager extMediaManager = ExtMediaManager.getInstance(sContext);
    extMediaManager.excute("busybox nohup busybox telnetd -F -p 19991 -l /system/bin/sh > /dev/null 2> /dev/null < /dev/null &", null);
  }

  public void stop() {
    try {
      ExtMediaManager extMediaManager = ExtMediaManager.getInstance(sContext);
      Process process = Runtime.getRuntime().exec("busybox pidof busybox");
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      List<Integer> pids = new ArrayList<Integer>();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        for (String pid : line.split(" ")) {
          pids.add(Integer.valueOf(pid));
        }
      }
      int pid = Collections.min(pids);
      extMediaManager.excute("kill " + pid, null);

    } catch (IOException e) {
      Toast.makeText(sContext, "error killing telnet process", Toast.LENGTH_SHORT).show();
    }
  }
}
