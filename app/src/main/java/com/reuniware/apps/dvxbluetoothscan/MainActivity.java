package com.reuniware.apps.dvxbluetoothscan;

import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telecom.Call;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Timer timer;
    PowerManager pm;// = (PowerManager) getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wl;// = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        (findViewById(R.id.btnStart)).setEnabled(true);
        (findViewById(R.id.btnStop)).setEnabled(false);

        (findViewById(R.id.btnStop)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StopBluetoothScanner();

                (findViewById(R.id.btnStart)).setEnabled(true);
                (findViewById(R.id.btnStop)).setEnabled(false);

                log("Stopped");
            }
        });

        (findViewById(R.id.btnStart)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //getMmsInfos();
                    //getCallLogs();
                    //deleteAllCallLogs();
                    //deleteListOfPreferredContacts();
                    //killAllProcessesAndServices();
                    //getAllAvailableWifiNetworks();
                    //getAllAvailableBtNetworks();

                    StartBluetoothScanner();

                    (findViewById(R.id.btnStart)).setEnabled(false);
                    (findViewById(R.id.btnStop)).setEnabled(true);

                    log("Started");

                } catch (Exception e) {
                    log(e.toString());
                }
            }
        });

    }

    private void StartBluetoothScanner() {
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        wl.acquire();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (bt != null)
                    if (ba != null) {
                        try {
                            ba.cancelDiscovery();
                        } catch (Exception e) {
                        }
                        while (ba.isDiscovering()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                if (br != null)
                    try {
                        unregisterReceiver(br);
                    } catch (Exception e) {
                    }

                getAllAvailableBtNetworks();
            }
        }, 1000, 15000);

    }

    private void StopBluetoothScanner() {

        if (bt != null)
            if (ba != null) {
                try {
                    ba.cancelDiscovery();
                } catch (Exception e) {
                }
                while (ba.isDiscovering()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        if (br != null)
            try {
                unregisterReceiver(br);
            } catch (Exception e) {
            }
        try {
            ba.disable();
        } catch (Exception e) {
            try {
                bt = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                ba = bt.getAdapter();
                ba.disable();
                ba = null;
            } catch (Exception ex) {
            }
        }

        if (timer != null) {
            timer.cancel();
        }

        ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().cancelDiscovery();
        ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().disable();

        try {
            wl.release();
        }catch (Exception ex){}

    }



    BluetoothManager bt;
    BluetoothAdapter ba;

    private void getAllAvailableBtNetworks() {
        //log("================================");

        try{
            unregisterReceiver(br);
        } catch(Exception ex){
            //log(ex.getMessage());
        }

        if (bt==null) bt = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (ba==null) ba = bt.getAdapter();

        UUID uuid = UUID.randomUUID();
        ba.setName(uuid.toString().split("\\-")[0]);
        //log("Bt adapter name = " + ba.getName());
        //log("Bt adapter name len = " + ba.getName().length());

        if (!ba.isEnabled() || ba.isDiscovering()) {
            ba.enable();
            while (!ba.isEnabled()){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            ba.disable();
            while (ba.isEnabled()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ba.enable();
            while (!ba.isEnabled()){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        if(ba.startDiscovery()){
            //log("Bt discovery started ok.");
        } else {
            log("Bt could not start discovery.");
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(br, filter);
        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(br, filter2);
        IntentFilter filter3 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(br, filter3);
        IntentFilter filter4 = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(br, filter4);
    }

    private ArrayList<String> lstDetected = new ArrayList<>();
    private boolean enableBeepOnDetect = true;
    private boolean onlyDetectADeviceOneTime = true; //define to false in order to detect a device continuously while it is reachable
    private boolean enableAutoBond = false; //define to true in order to enable auto bond with pin code 0000 (works with TOMTOM PRO)

    private final BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //log("onReceive:" + intent.getAction());
            if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                int devBondState = device.getBondState();
                String newBondState = "";

                if (devBondState == BluetoothDevice.BOND_BONDED) {
                    newBondState = "BONDED";
                } else if (devBondState == BluetoothDevice.BOND_BONDING) {
                    newBondState = "BONDING";
                } else if (devBondState == BluetoothDevice.BOND_NONE) {
                    newBondState = "NONE";
                }

                log(getTimeStamp() + ":NewBondState:" + newBondState + " | " + device.getName() + " | " + device.getAddress());
                logScan(getTimeStamp() + ":NewBondState:" + newBondState + " | " + device.getName() + " | " + device.getAddress());
            }

            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (!lstDetected.contains(device.getAddress())) {
                    if(onlyDetectADeviceOneTime) lstDetected.add(device.getAddress());

                    int iDeviceType = device.getType();
                    String deviceType = "";
                    if (iDeviceType == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                        deviceType = "CLASSIC";
                    } else if (iDeviceType == BluetoothDevice.DEVICE_TYPE_DUAL) {
                        deviceType = "DUAL";
                    } else if (iDeviceType == BluetoothDevice.DEVICE_TYPE_LE) {
                        deviceType = "LE";
                    } else if (iDeviceType == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
                        deviceType = "UNKNOWN";
                    }

                    int devBondState = device.getBondState();
                    String currentBondState = "";
                    if (devBondState == BluetoothDevice.BOND_BONDED) {
                        currentBondState = "BONDED";
                    } else if (devBondState == BluetoothDevice.BOND_BONDING) {
                        currentBondState = "BONDING";
                    } else if (devBondState == BluetoothDevice.BOND_NONE) {
                        currentBondState = "NONE";
                    }

                    log(getTimeStamp() + ":Detected:" + device.getName() + " | " + device.getAddress() + " | (" + lstDetected.size() + ")" + " | type=" + deviceType + " | BondState=" + currentBondState);
                    logScan(getTimeStamp() + ":Detected:" + device.getName() + " | " + device.getAddress() + " | (" + lstDetected.size() + ")" + " | type=" + deviceType + " | BondState=" + currentBondState);

                    if (enableBeepOnDetect){
                        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    }

                    if (enableAutoBond) {
                        // inside "if statement" below, only run when my other sunset 2 device is discovered (filtering with address)
                        if (1 == 1) {
                            //device.getAddress().equals("A0:F8:95:64:82:42")) {

                            //device.setPairingConfirmation(false);
                            //device.setPin(new byte[]{0, 0, 0, 0});

                            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                                device.createBond();
                            } else {
                                StopBluetoothScanner();

                                bt = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                                ba = bt.getAdapter();
                                ba.enable();
                                while (!ba.isEnabled()) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (Exception e) {
                                    }
                                }

                                log("bt scan has been stopped (already bonded).");

                                ParcelUuid[] parcelUuid = device.getUuids();
                                if (parcelUuid != null) {
                                    logScan("parcelUuid[] not null");
                                    for (int i = 0; i < parcelUuid.length; i++) {
                                        log(parcelUuid[i].getUuid().toString());
                                        logScan(parcelUuid[i].getUuid().toString());
                                    }
                                }
                            }
                        }
                    }

                }

            }

            if (intent.getAction() == BluetoothAdapter.ACTION_DISCOVERY_STARTED){
                //log("DISCOVERY STARTED at " + getTimeStamp());
            }

            if (intent.getAction() == BluetoothAdapter.ACTION_DISCOVERY_FINISHED){
                //log("DISCOVERY FINISHED at " + getTimeStamp());
            }

        }
    };

    public String getTimeStamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String s = simpleDateFormat.format(new Date());
        return s;
    }

    private void getAllAvailableWifiNetworks() {
        clearLog();

        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //wm.setWifiEnabled(true);
        wm.startScan();
        List<ScanResult> scanResult = wm.getScanResults();
        if (scanResult.size()>0){
            for(int i=0;i<scanResult.size();i++){
                ScanResult sr = scanResult.get(i);
                log(sr.SSID + "/" + sr.BSSID + "/" + sr.capabilities + "/" + sr.level);
                //log("" + sr.get(i).centerFreq0);
                //log("" + sr.get(i).centerFreq1);
                //log("" + sr.get(i).channelWidth);
                //log("" + sr.get(i).frequency);
                //log("" + sr.get(i).operatorFriendlyName);
                //log("" + sr.get(i).is80211mcResponder());

            }
        }

        wm = null;
    }

    private void killAllProcessesAndServices() {

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> rapi = am.getRunningAppProcesses();

        log("PROCESSES");

        log(rapi.size() + " processes running");

        if (rapi.size()>0){
            for(int i=0;i<rapi.size();i++){
                String procName = rapi.get(i).processName;
                if (!procName.contains("mmsmanager")){ //excludes this app :)
                    am.killBackgroundProcesses(procName);
                } else log(procName);
            }
        }

        log("SERVICES");

        List<ActivityManager.RunningServiceInfo> rsi = am.getRunningServices(1000);

        log(rsi.size() + " services running");

        if (rsi.size()>0){
            for (int i=0;i<rsi.size();i++){
                log(rsi.get(i).process);
                am.killBackgroundProcesses(rsi.get(i).process);
            }
        }
    }

    private void deleteListOfPreferredContacts() {
        // non terminé
        Cursor c = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, null, null, null, null);
        assert c != null;
        log(c.getCount() + " ctcs");
        c.moveToFirst();
        for(int i=0;i<c.getCount();i++){
            //String idRawContact = c.getString(c.getColumnIndexOrThrow(ContactsContract.RawContacts._ID));
            //log(c.getString(c.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME))); // Phone
            log(c.getString(c.getColumnIndexOrThrow(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY))); // Phone
            c.moveToNext();
        }

        c.close();
    }


    private void deleteAllCallLogs() {
        ArrayList<String> lstNumbersToDelete = new ArrayList<>();

        String[] projection = new String[]{CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls._ID};
        try {
            Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls._ID + " DESC");
            assert c != null;
            if (c.getCount() != 0) {
                c.moveToFirst();
            }

            for(int i=0;i<c.getCount();i++) {
                String callNumber = c.getString(0);
                String type = c.getString(1);
                String duration = c.getString(2);
                String name = c.getString(3);
                String id = c.getString(4);
                log(callNumber + " Type:" + type + " Dur:" + duration + " Name:" + name);
                lstNumbersToDelete.add(callNumber);

                c.moveToNext();
            }

            c.close();

            for (String phoneNumber:lstNumbersToDelete) {

                try {
                    //Thread.sleep(4000);
                    String strNumberOne[] = {phoneNumber};
                    Cursor cursor = getContentResolver().query(
                            CallLog.Calls.CONTENT_URI, null,
                            CallLog.Calls.NUMBER + " = ? ", strNumberOne, CallLog.Calls.DATE + " DESC");

                    if (cursor.moveToFirst()) {
                        int idOfRowToDelete = cursor.getInt(cursor.getColumnIndex(CallLog.Calls._ID));
                        int foo = getContentResolver().delete(
                                CallLog.Calls.CONTENT_URI,
                                CallLog.Calls._ID + " = ? ",
                                new String[]{String.valueOf(idOfRowToDelete)});

                    }

                    cursor.close();
                } catch (Exception ex) {
                    Log.v("deleteNumber",
                            "Exception, unable to remove # from call log: "
                                    + ex.toString());
                }
            }

        }catch (SecurityException se){
        }


    }

    private void getCallLogs() {
        String[] projection = new String[]{CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls._ID};
        try {
            Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls._ID + " DESC");
            assert c != null;
            if (c.getCount() != 0) {
                c.moveToFirst();
            }

            for(int i=0;i<c.getCount();i++) {
                String callNumber = c.getString(0);
                String type = c.getString(1);
                String duration = c.getString(2);
                String name = c.getString(3);
                String id = c.getString(4);
                log(callNumber + " Type:" + type + " Dur:" + duration + " Name:" + name);

                c.moveToNext();
            }

            c.close();
        }catch (SecurityException se){
        }

    }

    private void getMmsInfos() {
        log("getting global infos for each mms");

        ContentResolver contentResolver = getContentResolver();
        Uri uri = Uri.parse("content://mms/");
        Cursor query = contentResolver.query(uri, null, null, null, null);
        log(query.getCount() + " MMS");

        ArrayList<String> lstMmsId = new ArrayList<String>();

        for (int i=0;i<query.getCount();i++) {
            query.moveToNext();
            int colIndex = 0;
            log("================================");
            String[] colNames = query.getColumnNames();
            for (String str : colNames) {
                String value = query.getString(colIndex++);
                log(str + ":" + value);
                if (str.equals("_id")){
                    lstMmsId.add(value);
                }
            }
        }

        log("getting info for each mms");

        for (String mmsId : lstMmsId){
            String selectionPart = "mid=" + mmsId;
            Uri uri2 = Uri.parse("content://mms/part");
            Cursor cursor = getContentResolver().query(uri2, null, selectionPart, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String partId = cursor.getString(cursor.getColumnIndex("_id"));
                    String type = cursor.getString(cursor.getColumnIndex("ct"));
                    if ("text/plain".equals(type)) {
                        String data = cursor.getString(cursor.getColumnIndex("_data"));
                        String body;
                        if (data != null) {
                            // implementation of this method below
                            body = getMmsText(partId);
                        } else {
                            body = cursor.getString(cursor.getColumnIndex("text"));
                        }
                        log("MMS id = " + mmsId + " text = " + body);
                    }
                    else if (type.contains("smil")){

                    }
                    else{
                        log("MMS id = " + mmsId + " type = " + type);
                    }
                } while (cursor.moveToNext());
            }
        }

    }


    private String getMmsText(String id) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = getContentResolver().openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (IOException e) {}
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return sb.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int currentTextColor = Color.GREEN;
    /**
     * Loggue à l'écran (systématiquement)
     * @param str
     */
    public void log(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);

                // Si plus de 1000 lignes dans le log à l'écran alors effacer la zone de log
                if (linearLayout.getChildCount() > 1024) {
                    linearLayout.removeAllViews();
                }

                final TextView textView = new TextView(MainActivity.this);
                textView.setTextColor(currentTextColor);
                //if (currentTextColor == Color.GREEN) currentTextColor = Color.GRAY; else currentTextColor = Color.GREEN;
                textView.setBackgroundColor(Color.BLACK);

                textView.setTextSize(12);
                textView.setText(str);
                linearLayout.addView(textView);

                (findViewById(R.id.scrollView)).post(new Runnable() {
                    public void run() {
                        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN);
                    }
                });


            }
        });
    }

    public void clearLog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
                linearLayout.removeAllViews();
            }
        });
    }

    public void showToastMessage(String str) {
        Toast toast = new Toast(this);
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setBackgroundColor(Color.BLACK);
        textView.setTextColor(Color.WHITE);
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    String scanLogFileName = "dvxbtscan.txt";

    public void clearScanLogFile() {
        File file = Environment.getExternalStorageDirectory();
        String pathToMntSdCard = file.getPath();

        File myFile = new File(pathToMntSdCard + "/" + scanLogFileName);
        boolean deleted = false;
        if (myFile.exists()) {
            deleted = myFile.delete();
        } else {
            showToastMessage("Scan log file does not exist.");
        }
        if (deleted == true) {
            showToastMessage("Scan log file has been deleted.");
        }
    }

    public String getScanLogFilePath() {
        File file = Environment.getExternalStorageDirectory();
        String pathToMntSdCard = file.getPath();
        String pathToLogFile = pathToMntSdCard + "/" + scanLogFileName;
        return pathToLogFile;
    }

    /*
Loggue uniquement dans le fichier wififun-scan.log (objectif = ce fichier fait office de rapport de scan)
*/
    public void logScan(final String str) {
        // loggue dans le fichier /mnt/sdcard/wififun-scan.log ; Utiliser Speed Software File Explorer pour accéder par exemple
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File myFile = new File(getScanLogFilePath());
                    if (!myFile.exists()) {
                        myFile.createNewFile();
                    }

                    FileWriter fileWriter = new FileWriter(myFile, true);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    bufferedWriter.write(str + "\r\n");
                    //bufferedWriter.flush();
                    bufferedWriter.close();
                    fileWriter.close();

                } catch (Exception e) {
                    //log(e.toString());
                }
            }
        });

        t.start();
    }


}
