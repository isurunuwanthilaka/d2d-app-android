package com.fyp.d2d_android;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class NetworkScreen extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_network_screen, container, false);

        //battery level indication
        batteryTxt = (TextView) rootView.findViewById(R.id.batteryLevel);
        getActivity().registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        //rssi indication
        textConnected = (TextView) rootView.findViewById(R.id.connected);
        textIp = (TextView) rootView.findViewById(R.id.ip);

        textSsid = (TextView) rootView.findViewById(R.id.ssid);
        textBssid = (TextView) rootView.findViewById(R.id.bssid);
        textMac = (TextView) rootView.findViewById(R.id.mac);
        textSpeed = (TextView) rootView.findViewById(R.id.speed);
        textRssi = (TextView) rootView.findViewById(R.id.rssi);

        DisplayWifiState();

        getActivity().registerReceiver(this.myWifiReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        getActivity().registerReceiver(this.myRssiChangeReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));

        final Handler handler = new Handler();

        final Runnable r = new Runnable() {
            public void run() {
                String[] myTaskParams = { "1528", String.valueOf(textSpeed.getText()), String.valueOf(textRssi.getText()),String.valueOf(batteryTxt.getText()) };
                new SendPostRequest().execute(myTaskParams);
                handler.postDelayed(this, 60000);
            }
        };
        handler.postDelayed(r, 60000);

        return rootView;
    }


    private TextView SSIDTxt;

    //rssi
    private TextView textConnected;
    private TextView textIp;
    private TextView textSsid;
    private TextView textBssid;
    private TextView textMac;
    private TextView textSpeed;
    private TextView textRssi;

    //battery broadcast receiver
    private TextView batteryTxt;
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batteryTxt.setText("Battery Level : " + String.valueOf(level) + "%");
        }
    };

    //rssi receivers
    private BroadcastReceiver myRssiChangeReceiver
            = new BroadcastReceiver(){

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            WifiManager wifiMan=(WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
            wifiMan.startScan();
            int newRssi = wifiMan.getConnectionInfo().getRssi();
            textRssi.setText("RSSI Level : "+String.valueOf(newRssi));
        }};

    @Override
    public void onResume() {
        super.onResume();
        //Note: Not using RSSI_CHANGED_ACTION because it never calls me back.
        IntentFilter rssiFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getActivity().registerReceiver(myRssiChangeReceiver, rssiFilter);

        WifiManager wifiMan=(WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
        wifiMan.startScan();
    }


    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(myRssiChangeReceiver);

    }

    private BroadcastReceiver myWifiReceiver
            = new BroadcastReceiver(){

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            NetworkInfo networkInfo = (NetworkInfo) arg1.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                DisplayWifiState();
            }
        }};

    private void DisplayWifiState(){

        ConnectivityManager myConnManager = (ConnectivityManager) getActivity().getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiManager myWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        myWifiManager.startScan();
        WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();

        textMac.setText("MAC:"+myWifiInfo.getMacAddress());

        if (myNetworkInfo.isConnected()){
            int myIp = myWifiInfo.getIpAddress();

            textConnected.setText("Connecton status : Connected");

            int intMyIp3 = myIp/0x1000000;
            int intMyIp3mod = myIp%0x1000000;

            int intMyIp2 = intMyIp3mod/0x10000;
            int intMyIp2mod = intMyIp3mod%0x10000;

            int intMyIp1 = intMyIp2mod/0x100;
            int intMyIp0 = intMyIp2mod%0x100;

            textIp.setText("IP:"+String.valueOf(intMyIp0)
                    + "." + String.valueOf(intMyIp1)
                    + "." + String.valueOf(intMyIp2)
                    + "." + String.valueOf(intMyIp3)
            );

            textSsid.setText("SSID :"+myWifiInfo.getSSID());
            textBssid.setText("BSSID :"+myWifiInfo.getBSSID());

            textSpeed.setText(String.valueOf("Link speed : "+myWifiInfo.getLinkSpeed()) + " " + WifiInfo.LINK_SPEED_UNITS);
            textRssi.setText("RSSI Level:"+String.valueOf(myWifiInfo.getRssi()));
        }
        else{
            textConnected.setText("Connecton status : Disconnected");
            textIp.setText("---");
            textSsid.setText("---");
            textBssid.setText("---");
            textSpeed.setText("---");
            textRssi.setText("---");
        }

    }

    public String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while (itr.hasNext()) {

            String key = itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }

    public class SendPostRequest extends AsyncTask<String, Void, String> {

        protected void onPreExecute() {
        }

        protected String doInBackground(String... arg) {

            try {

                URL url = new URL("https://us-central1-fyp-cloud-83c3b.cloudfunctions.net/connData"); // here is your URL path

                JSONObject postDataParams = new JSONObject();
                postDataParams.put("deviceID", arg[0]);
                postDataParams.put("linkSpeed", arg[1]);
                postDataParams.put("connRSSI", arg[2]);
                postDataParams.put("batteryLevel", arg[3]);
                Log.e("params", postDataParams.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(5000 /* milliseconds */);
                conn.setConnectTimeout(5000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, StandardCharsets.UTF_8));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in = new BufferedReader(new
                            InputStreamReader(
                            conn.getInputStream()));

                    StringBuffer sb = new StringBuffer();
                    String line = "";

                    while ((line = in.readLine()) != null) {

                        sb.append(line);
                        break;
                    }

                    in.close();
                    return sb.toString();

                } else {
                    return "false : " + responseCode;
                }
            } catch (Exception e) {
                return "Exception: " + e.getMessage();
            }

        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getActivity().getApplicationContext(), result,
                    Toast.LENGTH_LONG).show();
        }
    }

}
