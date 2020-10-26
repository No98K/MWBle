package com.my.mwble.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by Android Studio.
 * User: mwb
 * Date: 2020/05/29 0024
 * Time: 上午 11:32
 * Describe:BLE蓝牙基础
 */
public class BleUtil {
    /**
     * 检查当前设备是否支持蓝牙
     *
     * @param context
     * @return
     */
    public static boolean checkDeviceSupportBleBlueTooth(Context context) {

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检查蓝牙是否打开了
     *
     * @param context
     * @return
     */
    public static boolean checkBlueIsOpen(Context context) {
        BluetoothAdapter bluetoothAdapter;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return false;
        } else {
            return true;
        }
    }

}
