package g4rb4g3.at.ioniqroot;

import android.content.Context;
import android.os.RemoteException;

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

  public void start() throws RemoteException {
    ProcessExecutor.executeRootCommand("busybox nohup busybox telnetd -F -p 19991 -l /system/bin/sh > /dev/null 2> /dev/null < /dev/null &");
  }

  public void stop() throws RemoteException {
    List<Integer> pids = new ArrayList<Integer>();
    String p = ProcessExecutor.execute("busybox pidof busybox");
    for (String pid : p.split(" ")) {
      pids.add(Integer.valueOf(pid));
    }
    int pid = Collections.min(pids);
    ProcessExecutor.executeRootCommand("kill " + pid);
  }
}
