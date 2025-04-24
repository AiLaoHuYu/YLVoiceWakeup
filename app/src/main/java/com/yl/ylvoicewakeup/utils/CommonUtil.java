package com.yl.ylvoicewakeup.utils;

import android.app.ActivityManager;
import android.content.Context;

public class CommonUtil {

    public static String getForegroundActivity(Context context) {
        ActivityManager mActivityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (mActivityManager.getRunningTasks(1) == null) {
            return null;
        }
        ActivityManager.RunningTaskInfo mRunningTask =
                mActivityManager.getRunningTasks(1).get(0);
        if (mRunningTask == null) {
            return null;
        }

        String pkgName = mRunningTask.topActivity.getPackageName();
        //String activityName =  mRunningTask.topActivity.getClassName();
        return pkgName;
    }

}
