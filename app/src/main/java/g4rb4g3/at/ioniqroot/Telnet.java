package g4rb4g3.at.ioniqroot;

import android.content.Context;
import android.os.RemoteException;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
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
    ProcessExecutor.executeRootCommand("busybox nohup busybox telnetd -F -p 25 -l /system/bin/sh > /dev/null 2> /dev/null < /dev/null &");
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

  public static List<String> getIPAddresses() throws SocketException {
    List<String> ips = new ArrayList<String>();
    List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
    for (NetworkInterface intf : interfaces) {
      List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
      for (InetAddress addr : addrs) {
        if (!addr.isLoopbackAddress()) {
          if (InetAddressUtils.isIPv4Address(addr.getHostAddress())) {
            ips.add(addr.getHostAddress());
          } else {
            ips.add(addr.getHostAddress().substring(0, addr.getHostAddress().indexOf("%")));
          }
        }
      }
    }
    return Collections.unmodifiableList(ips);
  }
}
