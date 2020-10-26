package com.my.mwble.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

public class GpsUtil {
    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     * User: mwb
     * @param context
     * @return true 表示开启
     */
    public static final boolean isOPen(final Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            LogUtil.i("GPS is open");
            return true;
        }
        LogUtil.i("GPS is close");
        return false;
    }

    /**
     * 打开GPS
     *
     * @param activity
     * @param requestCode
     */
    public static final void openGPS(Activity activity, int requestCode) {
//        Intent GPSIntent = new Intent();
//        GPSIntent.setClassName("com.android.settings",
//                "com.android.settings.widget.SettingsAppWidgetProvider");
//        GPSIntent.addCategory("android.intent.category.ALTERNATIVE");
//        GPSIntent.setData(Uri.parse("custom:3"));
//        try {
//            PendingIntent.getBroadcast(context, 0, GPSIntent, 0).send();
//        } catch (PendingIntent.CanceledException e) {
//            e.printStackTrace();
//   s     }

        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivityForResult(intent, requestCode);
    }
}
