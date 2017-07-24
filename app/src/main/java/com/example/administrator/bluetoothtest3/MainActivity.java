package com.example.administrator.bluetoothtest3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int ENABLE_BLUETOOTH = 2;
    OutputStream outputStream = null;
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//蓝牙串口服务相关UUID
    private String blueAddress = "20:15:12:29:21:48";//蓝牙模块的MAC地址
    private TextView textX;
    private TextView textY;
    private TextView textZ;
    private TextView Rssi;
    float[]  ORIENTATION;
    float[] magneticValues;
    float[] accelerometerValues;
    private  int b_direction=0;//;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket bluetoothSocket;
    int period=0;
    Timer time;

    TimerTask  timerTask=new TimerTask() {
        @Override
        public void run() {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
                Log.d("rssi","TIME pass");
            }
            //开启搜索
            mBluetoothAdapter.startDiscovery();
        }
    };
    //定时向蓝牙发送手机的方向
    TimerTask  cirtimeTask=new TimerTask() {
        @Override
        public void run() {
            if (bluetoothSocket != null) {
                try {
                    outputStream = bluetoothSocket.getOutputStream();
                    byte[] buffer = String.valueOf(b_direction).getBytes();
                    outputStream.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textX = (TextView) findViewById(R.id.textX);
        textY = (TextView) findViewById(R.id.textY);
        textZ = (TextView) findViewById(R.id.textZ);
        Rssi = (TextView) findViewById(R.id.ress);
        accelerometerValues = new float[3];//加速度参数
        magneticValues = new float[3];//磁场参数
        ORIENTATION=new float[3];//方向参数
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);//传感器初始化
        mBluetoothAdapter =BluetoothAdapter.getDefaultAdapter();
        //判断蓝牙是否开启
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "不支持蓝牙", Toast.LENGTH_LONG).show();
            finish();
        } else if (!mBluetoothAdapter.isEnabled()) {
            Log.d("true", "开始连接");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH);
        }
        //指定蓝牙设备，建立远程蓝牙设备实例
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(blueAddress);
        // 设置广播信息过滤
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);//每搜索到一个设备就会发送一个该广播
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//当全部搜索完后发送该广播
        filter.setPriority(Integer.MAX_VALUE);//设置优先级
        // 注册蓝牙搜索广播接收者，接收并处理搜索结果
        this.registerReceiver(receiver, filter);

        //设定定时器，定时重新扫描获取rssi
        time=new Timer();

        time.schedule(timerTask,100,6000);


        mBluetoothAdapter.startDiscovery();


    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("rssi","Receive1 pass");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("rssi","Receive2 pass");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //需要搜索的蓝牙mac
                String s=device.getAddress();
                if(s.equals("E4:02:9B:DF:E8:40")){

                    Log.d("rssi","信号强度："+ String.valueOf
                            (intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE))+"次数为："+period);
                    Rssi.setText("信号强度："+ String.valueOf
                            (intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE))+"次数为："+period);
                    period++;
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                        Log.d("rssi","TIME pass");
                    }
                    //开启搜索
                    mBluetoothAdapter.startDiscovery();

                }

            }
        }
    };
    public  void onAccuracyChanged(Sensor sensor,int i){

    }
    @Override//根据手机偏转的角度向蓝牙发送信息
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == null) {
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            ORIENTATION = event.values.clone();
            int x = (int) ORIENTATION[0] ;
            int y = (int) ORIENTATION[1] ;
            int z = (int) ORIENTATION[2] ;
            textX.setText(String.valueOf(x));
            textY.setText(String.valueOf(y));
            textZ.setText(String.valueOf(z));
            b_direction=x/10;
/*软件刚启动获取第一个方向
            if (b_direction == -1) {
                b_direction = x;
                String s = "A";//要传输的字符串
                byte[] buffer = s.getBytes();
                bluesend(buffer);
            } else if (x > b_direction) {
                String s = "D";//要传输的字符串
                byte[] buffer = s.getBytes();
                bluesend(buffer);
                b_direction = x;
            } else if (x < b_direction) {
                String s = "C";//要传输的字符串
                byte[] buffer = s.getBytes();
                bluesend(buffer);
                b_direction = x;
            } else if (x == b_direction) {
                String s = "A";//要传输的字符串
                byte[] buffer = s.getBytes();
                bluesend(buffer);
                b_direction = x;
            }*/


        }
    }
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(receiver);
        if(bluetoothSocket!=null){
            try{
                bluetoothSocket.close();

            }catch (IOException e){
                e.printStackTrace();
            }
        }



    }

    @Override
    protected void onResume(){
        super.onResume();
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);// ORIENTATION
        Sensor accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.
                TYPE_ACCELEROMETER);
        Sensor magneticSensor = mSensorManager.getDefaultSensor(Sensor.
                TYPE_MAGNETIC_FIELD);
        // 参数三，检测的精准度
        mSensorManager.registerListener(this, accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL);// //加速度传感器精度
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);// //方向传感器精度
        mSensorManager.registerListener(this, magneticSensor,
                SensorManager.SENSOR_DELAY_NORMAL);//磁场传感器精度
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(blueAddress);
        try{
            bluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            Log.d("true","开始连接");
            bluetoothSocket.connect();
            Log.d("true","完成连接");
        }catch (IOException e){
            e.printStackTrace();
        }
        time.schedule(cirtimeTask,1000,1500);
    }

    public void bluesend(byte[] message){
        if(bluetoothSocket!=null){
            try{
                outputStream = bluetoothSocket.getOutputStream();
                Log.d("send", Arrays.toString(message));
                outputStream.write(message);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

    }
}

