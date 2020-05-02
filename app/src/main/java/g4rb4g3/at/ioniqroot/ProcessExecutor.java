package g4rb4g3.at.ioniqroot;

import android.os.RemoteException;
import android.util.Log;

import com.lge.ivi.server.ExtMediaService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessExecutor {

  static final String TAG = "ProcessExecutor";

  public static String execute(String command) {
    BufferedReader bufferedReader = null;
    try {
      Process process = Runtime.getRuntime().exec(command);
      bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        if(sb.length() > 0) {
          sb.append("\n");
        }
        sb.append(line);
      }
      return sb.toString();
    } catch (IOException e) {
      Log.e(TAG, "error executing command " + command, e);
    } finally {
      if(bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (IOException e) {
          Log.e(TAG, "error closing reader.", e);
        }
      }
    }
    return null;
  }

  public static void executeRootCommand(String command) throws RemoteException {
    ExtMediaService.getInstance().excute(command, null);
  }
}
