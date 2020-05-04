package g4rb4g3.at.ioniqroot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import g4rb4g3.at.ioniqroot.MainActivity;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class BroadcastsReceiver extends BroadcastReceiver {
  private static int mCalls = 0;
  private static long mLastCall = 0L;
  private static String mLastAction = "";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (!mLastAction.equals(intent.getAction())) {
      mCalls = 0;
      mLastCall = 0L;
    }
    mLastAction = intent.getAction();
    if (System.currentTimeMillis() - mLastCall > 2000L) {
      mCalls = 0;
    }
    mLastCall = System.currentTimeMillis();
    mCalls++;
    if (mCalls == 2) {
      mCalls = 0;
      switch (intent.getAction()) {
        case "com.lge.ivi.action.KEY_SETTINGS":
          context.startActivity(new Intent(context, MainActivity.class).setFlags(FLAG_ACTIVITY_NEW_TASK));
          break;
      }
    }
  }
}
