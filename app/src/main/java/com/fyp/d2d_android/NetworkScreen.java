package com.fyp.d2d_android;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import static android.content.Context.BATTERY_SERVICE;

public class NetworkScreen extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_network_screen, container, false);
        batteryTxt = (TextView) rootView.findViewById(R.id.batteryLevel);
        getActivity().registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        int rssi = ((MainActivity) getActivity()).getResult();
        SSIDTxt = (TextView) rootView.findViewById(R.id.RSSILevel);
        SSIDTxt.setText("RSSI Level : " + String.valueOf(rssi));

        Button button = (Button) rootView.findViewById(R.id.refresh);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int rssi = ((MainActivity) getActivity()).getResult();
                System.out.println(rssi);
            }
        });


        return rootView;
    }

    private TextView batteryTxt;
    private TextView SSIDTxt;
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batteryTxt.setText("Battery Level : " + String.valueOf(level) + "%");
        }
    };

    @Override
    public void onResume() {
        Log.e("DEBUG", "onResume of LoginFragment");
        super.onResume();
    }

    public NetworkScreen() {


    }

}
