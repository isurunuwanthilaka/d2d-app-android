package com.fyp.d2d_android;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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


    //rssiend


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


}
