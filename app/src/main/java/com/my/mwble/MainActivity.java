package com.my.mwble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.my.mwble.adapter.SeacherDeviceAdapter;
import com.my.mwble.util.BleUtil;
import com.my.mwble.util.DigitalTrans;
import com.my.mwble.util.GpsUtil;
import com.my.mwble.util.LogUtil;
import com.my.mwble.util.NumUtil;
import com.my.mwble.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Android Studio.
 * User: mwb
 * Date: 2020/10/24 0024
 * Time: 上午 11:32
 * Describe:BLE蓝牙基础
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BluetoothAdapter bluetoothAdapter;
    private static int REQUEST_ENABLE_BT = 1; // 打开蓝牙页面请求代码
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1; // 位置权限
    private static final int SET_GPS_OPEN_STATE = 2; // 设置GPS是否打开了
    private static final int REQUEST_STORY_CODE = 3; // 文件读取权限

    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 10000; // 蓝牙扫描停止时间 10S

    private RecyclerView recyclerView;
    private SeacherDeviceAdapter adapter;

    private List<BluetoothDevice> deviceData = new ArrayList<>();

    private EditText et_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initDevice();
    }

    private void initView() {
        findViewById(R.id.btn_seach).setOnClickListener(this);
        findViewById(R.id.btn_send).setOnClickListener(this);

        recyclerView = findViewById(R.id.rv_main);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SeacherDeviceAdapter(this, new SeacherDeviceAdapter.CallBack() {
            @Override
            public void onItemClick(int position, BluetoothDevice device) {
                bindBlueTooth(device); // 绑定选中的蓝牙设备
            }
        });

        recyclerView.setAdapter(adapter);

        et_send = findViewById(R.id.et_send);
    }

    private void initDevice() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_seach: // 搜索蓝牙设备
                seach();
                break;
            case R.id.btn_send: // 发送数据
                if (et_send.getText().toString().isEmpty()) {
                    ToastUtil.show(MainActivity.this, "请输入要发送的16进制数据");
                    return;
                }

                sendMsg(et_send.getText().toString());
                break;
        }
    }

    /**
     * 搜索设备
     * 1.当前设备是否支持BLE蓝牙功能
     * 2.设备的蓝牙功能是否处于开启状态
     * 2.1 没有开启则去开启
     * 3.判断设备的api是否需要开启定位权限
     * （PS：至于为什么要开启定位权限，这你得问Google了）
     * 3.1GPS是否打开了
     * 3.2是否拥有GPS权限，需要使用GPS才能使用蓝牙设备
     */
    private void seach() {
        // 当前的系统版本 < Android 4.3 API=18，目前市面大部分系统都在6.0了...这个判断几乎可以不用写了。可省略
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ToastUtil.show(this, "当前设备系统版本不支持BLE蓝牙功能！请升级系统版本到4.3以上");
            return;
        }

        //1. 当前设备是否支持BLE蓝牙设备
        if (BleUtil.checkDeviceSupportBleBlueTooth(this)) {
            // 2.判断蓝牙设备是否打开了
            if (checkBlueIsOpen()) {
                // 3.断设备的api是否需要开启定位权限
                checkGPS();
            } else { // 没有打开,跳转到系统蓝牙页面
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            ToastUtil.show(this, "当前设备不支持BLE蓝牙功能！");
        }
    }

    /**
     * GPS是否开启了
     */
    private void checkGPS() {
        // 3.1GPS是否打开了
        if (GpsUtil.isOPen(this)) { // GPS已经开启了
            checkGpsPermission();
        } else {
            // 3.2是否拥有GPS权限，需要使用GPS才能使用蓝牙设备
            tipGPSSetting();
        }
    }

    /**
     * 蓝牙是否打开了
     *
     * @return true 打开了，false 没有打开
     */
    private boolean checkBlueIsOpen() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 搜索蓝牙设备
     * 创建搜索callback 返回扫描到的信息
     * 创建定时任务，在指定时间内结束蓝牙扫描，蓝牙扫描是一个很耗电的操作！
     */
    private void seachBlueTooth() {
        ToastUtil.show(this, "开始搜索蓝牙设备");
        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(null, createScanSetting(), scanCallback);

        bluetoothAdapter.startDiscovery();

        handler.postDelayed(new Runnable() { // 指定时间内停止蓝牙搜索
            @Override
            public void run() {
                closeSeach();
            }
        }, SCAN_PERIOD);

        deviceData.clear();
    }

    /**
     * 回调
     */
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            LogUtil.i("name:" + result.getDevice().getName() + ";强度：" + result.getRssi());

            if (device != null) {

                if (deviceData.size() > 0) {

                    if (!deviceData.contains(device)) { // 扫描到会有很多重复的数据,剔除，只添加第一次扫描到的设备
                        deviceData.add(device);
                    }
                } else {
                    deviceData.add(device);
                }
                adapter.setData(deviceData);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    /**
     * 停止扫描
     */
    private void closeSeach() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(scanCallback);
            ToastUtil.show(this, (SCAN_PERIOD / 1000) + "秒搜索时间已到，停止搜索");
        }
    }

    /**
     * 扫描广播数据设置
     *
     * @return
     */
    public ScanSettings createScanSetting() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
//        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER); // 耗电最少，扫描时间间隔最短
//        builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED); // 平衡模式，耗电适中，扫描时间间隔一般，我使用这种模式来更新设备状态
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);//最耗电，扫描延迟时间短，打开扫描需要立马返回结果可以使用
        //builder.setReportDelay(100);//设置延迟返回时间
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        }
        return builder.build();
    }

    /**
     * 提示需要开启蓝牙
     */
    private void tipGPSSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("提示");
        builder.setMessage("安卓6.0以后使用蓝牙需要开启定位功能，但本应用不会使用到您的位置信息，开始定位只是为了扫描到蓝牙设备。是否确定打开");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                GpsUtil.openGPS(MainActivity.this, SET_GPS_OPEN_STATE);
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ToastUtil.show(MainActivity.this, "您无法使用此功能");
            }
        });
        builder.show();
    }

    /**
     * 蓝牙需要的定位权限
     */
    private void checkGpsPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // 如果当前版本是9.0（包含）以下的版本
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] strings =
                        {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                ActivityCompat.requestPermissions(this, strings, REQUEST_CODE_ACCESS_COARSE_LOCATION);
            } else {
                seachBlueTooth();
            }
        } else {
            // 10.0系统
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this,
                    "android.permission.ACCESS_BACKGROUND_LOCATION") != PackageManager.PERMISSION_GRANTED) {
                String[] strings = {android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        "android.permission.ACCESS_BACKGROUND_LOCATION"};
                ActivityCompat.requestPermissions(this, strings, REQUEST_CODE_ACCESS_COARSE_LOCATION);
            } else {
                seachBlueTooth();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) { // 从蓝牙页面返回了，在检查一次是否打开了
            if (checkBlueIsOpen()) {
                // 蓝牙打开了
                seach();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("提示");
                builder.setMessage("蓝牙没有打开将无法使用此功能，是否确定打开");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        seach(); // 再次执行搜索
                    }
                });

                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        ToastUtil.show(MainActivity.this, "您无法使用此功能");
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        } else if (requestCode == SET_GPS_OPEN_STATE) { // GPS是否打开了
            if (GpsUtil.isOPen(this)) { // GPS打开了
                checkGpsPermission();
            } else {
                tipGPSSetting();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // 得到了权限
                    seachBlueTooth();
                } else {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("提示");
                    builder.setMessage("安卓6.0以后使用蓝牙需要开启定位功能，但本应用不会使用到您的位置信息，开启定位只是为了扫描到蓝牙设备。是否确定打开");
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            launchAppDetailsSettings(MainActivity.this);
                        }
                    });

                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            ToastUtil.show(MainActivity.this, "您无法使用此功能");
                        }
                    });
                    builder.setCancelable(false);
                    builder.show();

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    /**
     * 跳转权限Activity
     */
    public void launchAppDetailsSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));

        if (!isIntentAvailable(this, intent)) {
            ToastUtil.show(this, "请手动跳转到权限页面，给予权限!");
            return;
        }
        activity.startActivity(intent);
    }

    /**
     * 意图是否可用
     *
     * @param intent The intent.
     * @return {@code true}: yes<br>{@code false}: no
     */
    public boolean isIntentAvailable(Activity activity, Intent intent) {
        return activity
                .getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .size() > 0;
    }

    // ===================================================================================================================
    private BluetoothGatt mBluetoothGatt; // 自定义Gatt实现类
    private BluetoothGattCallback mGattCallback = new mWBluetoothGattCallback();
    //定义重连次数
    private int reConnectionNum = 0;
    //最多重连次数
    private int maxConnectionNum = 3;
    private BluetoothDevice mBluetoothDevice;

    private String UUDI_1 = "0000ffe0-0000-1000-8000-00805f9b34fb"; // Service1
    private String CHARACTERISTIC_UUID_1 = "0000ffe1-0000-1000-8000-00805f9b34fb"; // 特征1

    private BluetoothGattCharacteristic mGattCharacteristic;

    /**
     * 绑定蓝牙
     *
     * @param device
     */
    private void bindBlueTooth(BluetoothDevice device) {
        //连接设备
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(this,
                    false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }
    }

    //定义蓝牙Gatt回调类
    public class mWBluetoothGattCallback extends BluetoothGattCallback {
        //连接状态回调
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, final int status, final int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            // status 用于返回操作是否成功,会返回异常码。
            // newState 返回连接状态，如BluetoothProfile#STATE_DISCONNECTED、BluetoothProfile#STATE_CONNECTED

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //操作成功的情况下
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //判断是否连接码
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtil.show(MainActivity.this, "蓝牙已连接");
                                    LogUtil.i("设备已连接上，开始扫描服务");

                                    // 发现服务
                                    mBluetoothGatt.discoverServices();
                                }
                            });
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            //判断是否断开连接码
                            ToastUtil.show(MainActivity.this, "连接已断开");
                        }
                    } else {
                        //异常码
                        // 重连次数不大于最大重连次数
                        if (reConnectionNum < maxConnectionNum) {
                            // 重连次数自增
                            reConnectionNum++;
                            LogUtil.i("重新连接：" + reConnectionNum);
                            // 连接设备
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                mBluetoothGatt = mBluetoothDevice.connectGatt(MainActivity.this,
                                        false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
                            } else {
                                mBluetoothGatt = mBluetoothDevice.connectGatt(MainActivity.this, false, mGattCallback);
                            }


                        } else {
                            // 断开连接，失败回调
                            ToastUtil.show(MainActivity.this, "蓝牙连接失败，建议重启APP，或者重启蓝牙，或重启设备");
                            closeBLE();
                        }

                    }
                }
            });
        }

        //服务发现回调
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

//                        LogUtil.i("mmmm:" + mBluetoothGatt.getServices().size());
//                        for (int i = 0; i < mBluetoothGatt.getServices().size(); i++) {
//                            LogUtil.i("mmmm service:" + mBluetoothGatt.getServices().get(i).getUuid());
//
//
//                            for (int k = 0; k < mBluetoothGatt.getServices().get(i).getCharacteristics().size(); k++) {
//                                LogUtil.i("mmmm      Characteristic:" + mBluetoothGatt.getServices().get(i).getCharacteristics().get(k).getUuid());
//                            }
//
//                        }

                        //获取指定uuid的service
                        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(UUDI_1));
//                        bluetoothGattServiceList.add(gattService);
                        //获取到特定的服务不为空
                        if (gattService != null) {
                            LogUtil.i("获取服务成功!");

                            BluetoothGattCharacteristic gattCharacteristic =
                                    gattService.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID_1));

                            mGattCharacteristic = gattCharacteristic;

                            if (gattCharacteristic != null) {
                                LogUtil.i("获取特征成功!");


                                boolean isEnableNotification = mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);

                                if (isEnableNotification) {
                                    LogUtil.i("开启通知成功!");
                                    //通过GATt实体类将，特征值写入到外设中。
                                    mBluetoothGatt.writeCharacteristic(gattCharacteristic);
                                    //如果只是需要读取外设的特征值：
                                    //通过Gatt对象读取特定特征（Characteristic）的特征值
                                    mBluetoothGatt.readCharacteristic(gattCharacteristic);
                                } else {
                                    LogUtil.i("开启通知失败!");
                                }

                            } else {
                                LogUtil.i("获取特征失败!");
                            }

                        } else {
                            //获取特定服务失败
                            LogUtil.i("获取服务失败!");
                        }
                    }
                });
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogUtil.i("获取读取到的特征值成功!");
                //获取读取到的特征值
//                characteristic.getValue();
                LogUtil.i("获取读取特征Value：" + characteristic.getValue().toString());
            } else {
                LogUtil.i("获取读取到的特征值失败!");
            }
        }

        //特征写入回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        LogUtil.i("特征写入回调成功！");
                        ToastUtil.show(MainActivity.this, "写入特征成功");
                        //获取写入到外设的特征值
                        characteristic.getValue();
                    } else {
                        LogUtil.i("特征写入回调失败！");
                    }
                }
            });
        }

        //外设特征值改变回调
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            final byte[] value = characteristic.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // value为设备发送的数据，根据数据协议进行解析
                    LogUtil.i("原始数据：" + new String(characteristic.getValue()));
                    LogUtil.i("设备发送数据：" + DigitalTrans.byte2hex(value));
                }
            });
        }

        //描述写入回调
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            LogUtil.i("开启监听成功");
        }
    }

    /**
     * 关闭BLE蓝牙连接
     */
    public void closeBLE() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        ToastUtil.show(this, "蓝牙已断开");
    }

    /**
     * 发送数据
     * 将输入的16进制转化为byte发送
     */
    private void sendMsg(String msg) {
        if (null == mGattCharacteristic || null == mBluetoothGatt) {
            ToastUtil.show(MainActivity.this, "请先连接蓝牙设备");
            return;
        }

        mGattCharacteristic.setValue(NumUtil.hexString2Bytes(msg));
        mBluetoothGatt.writeCharacteristic(mGattCharacteristic);
    }
}