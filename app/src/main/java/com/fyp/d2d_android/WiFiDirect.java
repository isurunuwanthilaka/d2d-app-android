package com.fyp.d2d_android;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WiFiDirect extends AppCompatActivity {

    private static ClientTask clientTask;
    private static ServerTask serverTask;

    Button btnOnOff, btnDiscover;
    ListView listView;
    TextView read_msg_box, connectionStatus;
    private Toolbar toolbar;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers=new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    String paringSSID;
    String mySSID;
    String fileName;
    public static boolean transactionDone=false;

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
//            Log.d("inside copyFile", e.toString());
            return false;
        }
        return true;
    }


    private void exqListener() {
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wifiManager.isWifiEnabled())
                {
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("ON");
                }else {
                    wifiManager.setWifiEnabled(true);
                    btnOnOff.setText("OFF");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery Starting Failed");
                    }
                });
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                connectTo(deviceArray[i]);
            }
        });

        //automate WiFi enabling and discovery
        if(!wifiManager.isWifiEnabled()){
            btnOnOff.callOnClick();
        }
        btnDiscover.callOnClick();
    }

    public static boolean sendFile(Socket socket, Context context, String fileName) {
        int len;
        byte[] buf = new byte[1024];

        try {
            OutputStream outputStream = socket.getOutputStream();
            ContentResolver cr = context.getContentResolver();
            InputStream inputStream = null;
            inputStream = cr.openInputStream(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/D2D/" + fileName + ".jpg")));
            if (inputStream == null) {
                throw new FileNotFoundException("can't open input stream: " + "Environment.getExternalStorageDirectory()+\"/D2D/\"" + fileName);
            }
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    WifiP2pManager.PeerListListener peerListListener=new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if(!peerList.getDeviceList().equals(peers))
            {
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNameArray=new String[peerList.getDeviceList().size()];
                deviceArray=new WifiP2pDevice[peerList.getDeviceList().size()];
                int index=0;

                for(WifiP2pDevice device : peerList.getDeviceList())
                {
                    deviceNameArray[index]=device.deviceName;
                    //System.out.println("\""+tem+"\"");
                    deviceArray[index]=device;
                    index++;
                }

                ArrayAdapter<String> adapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);
            }

            if(peers.size()==0)
            {
                Toast.makeText(getApplicationContext(),"No Device Found",Toast.LENGTH_SHORT).show();
                return;
            }else {
                if (!transactionDone && CloudFileScreen.hasRequested){
                    try {
                        WifiP2pDevice device=fetchSecondDevice(deviceArray,paringSSID);
                        transactionDone=true;
                        connectTo(device);
                    }catch (IOException e){
                        Toast.makeText(getApplicationContext(),"2nd Device Not Found",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener=new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress=wifiP2pInfo.groupOwnerAddress;

//            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner)
            if(wifiP2pInfo.groupFormed && CloudFileScreen.hasRequested)
            {
                connectionStatus.setText("Client");
                WiFiDirect.clientTask=new ClientTask();
                WiFiDirect.clientTask.execute(groupOwnerAddress,getApplicationContext());

            }else if(wifiP2pInfo.groupFormed)
            {
                connectionStatus.setText("Host");
                WiFiDirect.serverTask=new ServerTask();
                WiFiDirect.serverTask.execute(getApplicationContext());
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    class ClientTask extends AsyncTask<Object, Void,Void> {
        Socket socket;
        String hostAdd;
        Context context;

        @Override
        protected Void doInBackground(Object... objects) {
            socket= new Socket();
            hostAdd=((InetAddress)objects[0]).getHostAddress();
            context = (Context) objects[1];
            try {
                socket.connect(new InetSocketAddress(hostAdd,8888),500);
                receiveFile(socket);
                CloudFileScreen.hasRequested=false;
//                sendReceive=new SendReceive(socket);
//                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    class ServerTask extends AsyncTask<Object, String,String> {
        Socket socket;
        ServerSocket serverSocket;
        Context context;
        @Override
        protected String doInBackground(Object... objects) {
            context = (Context) objects[0];
            try {
                serverSocket=new ServerSocket(8888);
                socket=serverSocket.accept();
                sendFile(socket,context,fileName);
                serverSocket.close();
//                sendReceive=new SendReceive(socket);
//                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get intent data from previous activity
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            paringSSID = extras.getString("pairingSSID");
            mySSID = extras.getString("mySSID");
            fileName = extras.getString("fileName");
        }

        //adding toolbar
        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        setContentView(R.layout.activity_wifidirect);
        initialWork();
        exqListener();
    }

    private void initialWork() {
        btnOnOff = findViewById(R.id.onOff);
        btnDiscover = findViewById(R.id.discover);
        listView = findViewById(R.id.peerListView);
        connectionStatus = findViewById(R.id.connectionStatus);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public static boolean receiveFile(Socket socket){
        try {
            final File f = new File(Environment.getExternalStorageDirectory() + "/D2D"
                    + "/" + System.currentTimeMillis()
                    + ".jpg");

            File dirs = new File(f.getParent());
            if (!dirs.exists())
                dirs.mkdirs();
            dirs.mkdirs();
            f.createNewFile();
            InputStream inputstream = socket.getInputStream();
            copyFile(inputstream, new FileOutputStream(f));
        }catch (IOException e){
            return false;
        }
        return true;

    }

    public void connectTo(WifiP2pDevice d){
        final WifiP2pDevice device=d;
        WifiP2pConfig config=new WifiP2pConfig();
        config.deviceAddress=device.deviceAddress;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"Connected to "+device.deviceName,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static WifiP2pDevice fetchSecondDevice(WifiP2pDevice[] arr,String name) throws IOException{

        for (WifiP2pDevice d:arr){
            if(d.deviceName.equalsIgnoreCase(name)){
                return d;
            }
        }
        throw new IOException("Device not found");
    }

    }
