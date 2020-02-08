package com.example.environmentcontrol;
/*
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private LineChart line_chart;
    private HorizontalBarChart bar_chart;
    private Boolean flag = true;
    private Button start, stop, test;
    private int count = 1, times = 0;
    //    private String save_score, disp_score;
    private List<Entry> entries2 = new ArrayList<>();
    private List<Float> save_score = new ArrayList<>();
    private Float score, avg_score = 0.0f, total_score = 0.0f;
    private Integer count_num;
    private Handler handler;
    private Boolean check_channel = true;
    private List<String> countdown_str;

    private SQLiteDatabase db;
    private MyDBHelper dbHelper;

    private TextView mConnectionState, countdown, score_text;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGatt mBluetoothGatt;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        dbHelper = new MyDBHelper(this);
        db = dbHelper.getWritableDatabase(); // 開啟資料庫

        final Intent intent = getIntent();
//        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
//        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceName = getSharedPreferences("name", MODE_PRIVATE)
                .getString("device_name", "");
        mDeviceAddress = getSharedPreferences("name", MODE_PRIVATE)
                .getString("device_address", "");

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        start = (Button) findViewById(R.id.start_chart);
        line_chart = (LineChart) findViewById(R.id.line_chart);
        bar_chart = (HorizontalBarChart) findViewById(R.id.bar_chart);

//        getActionBar().setTitle(mDeviceName);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = true;


                Log.d("channel_status",check_channel+"");
                if (mGattCharacteristics != null) {
                    //write 進入快閃
                    final BluetoothGattCharacteristic writecharacteristic =
                            mGattCharacteristics.get(2).get(0);
                    mBluetoothLeService.writeCharacteristic(writecharacteristic);


                    Log.d("testdata", "sleep");
                    try {
                        Thread.sleep(500); //1000為1秒
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d("testdata", "sleep end");

                    //notify 收資料
                    final BluetoothGattCharacteristic characteristic =
                            mGattCharacteristics.get(2).get(1);
                    final int charaProp = characteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = characteristic;
                        mBluetoothLeService.setCharacteristicNotification(
                                characteristic, true);
                    }
                }
                start.setVisibility(Button.INVISIBLE);

            }

        });


        // bar_chart configuration
        bar_chart.setDrawBarShadow(true);
        bar_chart.getAxisRight().setAxisMinValue(0.0f); // set x-axis min-value and max-value
        bar_chart.getAxisRight().setAxisMaximum(100.0f);
        bar_chart.getAxisLeft().setAxisMinValue(0.0f);
        bar_chart.getAxisLeft().setAxisMaximum(100.0f);
        bar_chart.getAxisLeft().setEnabled(false);
        bar_chart.getAxisRight().setEnabled(false);
        bar_chart.getDescription().setEnabled(false); // disable description right-down
        bar_chart.getLegend().setEnabled(false); // disable description left-down
        bar_chart.getXAxis().setEnabled(false);
        bar_chart.setTouchEnabled(false);
        // bar_chart configuration end

        // line_chart configuration
        line_chart.getAxisRight().setAxisMinValue(0.0f); // set x-axis min-value and max-value
        line_chart.getAxisRight().setAxisMaximum(100.0f);
        line_chart.getAxisRight().setEnabled(false);
        line_chart.getAxisLeft().setAxisMinValue(0.0f);
        line_chart.getAxisLeft().setAxisMaximum(100.0f);
        line_chart.getDescription().setEnabled(false);// disable description right-down
        line_chart.getLegend().setEnabled(false); // disable description left-down
        XAxis line_xAxis = line_chart.getXAxis();
        line_xAxis.setAxisMinimum(0.0f);
        line_xAxis.setAxisMaximum(360.0f);
        line_xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        line_chart.setTouchEnabled(false);

//        line_chart.getAxisLeft().dr
        // line_chart configuration end


//        mBluetoothLeService.connect(mDeviceAddress);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
            if(flag) {
                if (!data.isEmpty() && times < 370) {
                    score = Float.parseFloat(data);
                    get_data();
                    if (times > 9) {
                        save_score.add(score);
                        Log.d("score_display"," a  " + total_score + "   "+score);
                        total_score = (total_score + score);
                        Log.d("score_display","   " + total_score);
                    }
                    times = times + 1;
                } else if (times == 370) {

                    avg_score = (total_score / 360);

                    Log.d("score_display",avg_score+"   " + total_score);
                    times++;
                    AlertDialog.Builder builder = new AlertDialog.Builder(DeviceControlActivity.this);
                    builder.setMessage("The training score is : " + avg_score);
                    builder.setTitle("End of training");
                    builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            Date date = new Date();
                            String id = getSharedPreferences("name", MODE_PRIVATE)
                                    .getString("id", "");

                            //存入資料庫
                            ContentValues values = new ContentValues();
                            values.put("id", id);
                            values.put("date", dateFormat.format(date));
                            values.put("avg_score", avg_score);
                            db.insert("data", null, values);
                            mBluetoothLeService.disconnect();

                            //寫入手機
                            SimpleDateFormat dateFormat_1 = new SimpleDateFormat("yyyyMMddHHmmss");
                            Date day = new Date();
                            String filename = id+"_"+dateFormat_1.format(day)+".txt";
                            File dir = getApplicationContext().getFilesDir();

                            File outFile = new File(dir, filename);
                            writeToFile(outFile, "Hello! 大家好");

                            //startActivity(new Intent(DeviceControlActivity.this, NFTdiaplayActivity.class));
                            finish();
                        }
                    });
                    builder.create().show();
                }
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void get_data() {
        countdown = findViewById(R.id.countdown);
        line_chart.clear();
        bar_chart.clear();
        countdown_str = Arrays.asList("ten", "nine", "eight","seven","six","five","four","three","two","one");

        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                count_num = times;
                if (count_num < 10)  //倒數
                {
                    countdown.setVisibility(TextView.VISIBLE);
                    List<BarEntry> entries = new ArrayList<>();
//                    countdown.setText((10 - count_num) + "Start after five seconds");
                    countdown.setText("Start after "+countdown_str.get(count_num)+" seconds");
                    entries.add(new BarEntry(0f, 100 - (count_num * 10)));
                    Log.d("score", count_num + "   " + (10 - count_num) + "    " + (100 - (count_num * 10)) + "");
                    BarDataSet set = new BarDataSet(entries, "BarDataSet");
                    set.setColor(Color.rgb(253, 201, 203));
                    set.setBarShadowColor(Color.rgb(187, 213, 236));
                    BarData data = new BarData(set);
                    data.setBarWidth(1.0f);
                    bar_chart.setData(data);
                    bar_chart.invalidate();
                } else {
                    countdown.setVisibility(TextView.INVISIBLE);
                    line_chart.setVisibility(Chart.VISIBLE);
                    //get data and set data here --- barchart
                    List<BarEntry> entries = new ArrayList<>();
                    entries.add(new BarEntry(0f, score));
                    BarDataSet set = new BarDataSet(entries, "BarDataSet");
                    set.setColor(Color.rgb(253, 201, 203));
                    set.setBarShadowColor(Color.rgb(187, 213, 236));
                    BarData data = new BarData(set);
                    data.setBarWidth(1.0f);
                    bar_chart.setData(data);
                    bar_chart.invalidate();
                    //end

                    // update linechart
                    entries2.add(new Entry(times - 10, score));
                    LineDataSet dataSet = new LineDataSet(entries2, "");
                    LineData lineData = new LineData(dataSet);
                    dataSet.setDrawCircles(false); // disable point(circle)
                    dataSet.setDrawValues(false); // disable value display
                    line_chart.setData(lineData);
                    line_chart.invalidate(); // refresh

//                    if (count_num == 10)
//                        save_score = score + "/";
//                    else if (count_num == 369)
//                        save_score = save_score + score;
//                    else
//                        save_score = save_score + score + "/";

                    Log.d("score_display", count_num + "    " + save_score);

                }

            }
        }));
    }

    private void writeToFile(File fout, String data) {
        FileOutputStream osw = null;
        try {
            osw = new FileOutputStream(fout);
            osw.write(data.getBytes());
            osw.flush();
        } catch (Exception e) {
            ;
        } finally {
            try {
                osw.close();
            } catch (Exception e) {
                ;
            }
        }
    }
}

*/