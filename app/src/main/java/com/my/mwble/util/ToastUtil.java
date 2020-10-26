package com.my.mwble.util;

import android.content.Context;
import android.widget.Toast;

/**
 * 吐司类
 */
public class ToastUtil {
    public static void show(Context context, String msg) {
        if (null == msg) {
            Toast.makeText(context, "null", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }
}
