package com.fyp.d2d_android;

import android.app.Notification;
import android.app.NotificationManager;
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

import static com.fyp.d2d_android.MyFirebaseMessagingService.msgFlag;

public class WiFiDirect extends AppCompatActivity {

    private static ReceiveTask receiveTask;
    private static SendTask sendTask;

    Button btnOnOff, btnDiscover;
    ListView listView;
    TextView connectionStatus;
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

    //for notification handling
    private Context mContext;
    private int NOTIFICATION_ID = 1;
    private Notification mNotification;
    private NotificationManager mNotificationManager;

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
            inputStream = cr.openInputStream(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/D2D/" + fileName)));
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

            if(wifiP2pInfo.groupFormed && CloudFileScreen.hasRequested)
            {
                connectionStatus.setText("Client");
                WiFiDirect.receiveTask=new ReceiveTask(getApplicationContext());
                if (wifiP2pInfo.isGroupOwner){
                    WiFiDirect.receiveTask.execute(groupOwnerAddress,getApplicationContext(),1);
                }else {
                    WiFiDirect.receiveTask.execute(groupOwnerAddress,getApplicationContext(),2);
                }

            }else if(wifiP2pInfo.groupFormed)
            {
                connectionStatus.setText("Host");
                WiFiDirect.sendTask=new SendTask(getApplicationContext());
                if (wifiP2pInfo.isGroupOwner){
                    WiFiDirect.sendTask.execute(groupOwnerAddress,getApplicationContext(),1);
                }else{
                    WiFiDirect.sendTask.execute(groupOwnerAddress,getApplicationContext(),2);
                }
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

    class ReceiveTask extends AsyncTask<Object, Void,Void> {
        Socket socket;
        ServerSocket serverSocket;
        String hostAdd;
        Context context;
        int ownership;

        public ReceiveTask(Context context){
            mContext = context;
            //Get the notification manager
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            createNotification(fileName,"File download initiated",android.R.drawable.stat_sys_download);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            createNotification(fileName,"File download successful",android.R.drawable.stat_sys_download_done);
            msgFlag = 0;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            createNotification(fileName,"File download in progress",android.R.drawable.stat_sys_download);
        }

        @Override
        protected Void doInBackground(Object... objects) {
            socket= new Socket();
            hostAdd=((InetAddress)objects[0]).getHostAddress();
            context = (Context) objects[1];
            ownership = (int) objects[2];
            switch (ownership){
                case 1:
                    try {
                        serverSocket=new ServerSocket(8888);
                        socket=serverSocket.accept();
                        receiveFile(socket,fileName);
                        serverSocket.close();
                        CloudFileScreen.hasRequested=false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    try {
                        socket.connect(new InetSocketAddress(hostAdd,8888),500);
                        receiveFile(socket,fileName);
                        CloudFileScreen.hasRequested=false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }

            return null;
        }
    }

    class SendTask extends AsyncTask<Object, Void,Void> {
        Socket socket;
        ServerSocket serverSocket;
        String hostAdd;
        Context context;
        int ownership;

        public SendTask(Context context){
            mContext = context;
            //Get the notification manager
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            createNotification(fileName,"File upload initiated",android.R.drawable.stat_sys_upload);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            createNotification(fileName,"File upload successful",android.R.drawable.stat_sys_upload_done);
            msgFlag = 0;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            createNotification(fileName,"File upload in progress",android.R.drawable.stat_sys_upload);
        }

        @Override
        protected Void doInBackground(Object... objects) {
            socket= new Socket();
            hostAdd=((InetAddress)objects[0]).getHostAddress();
            context = (Context) objects[1];
            ownership = (int) objects[2];
            switch (ownership){
                case 1:
                    try {
                        serverSocket=new ServerSocket(8888);
                        socket=serverSocket.accept();
                        sendFile(socket,context,fileName);
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    try {
                        socket.connect(new InetSocketAddress(hostAdd,8888),500);
                        sendFile(socket,context,fileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
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

    public static boolean receiveFile(Socket socket,String fileName){
        String[] name_arr=fileName.split("\\.",2);
        try {
            //save file with timestamp
//            final File f = new File(Environment.getExternalStorageDirectory() + "/D2D"
//                    + "/" + System.currentTimeMillis()
//                    +"."+ name_arr[name_arr.length-1]);
            //save file with original name
            final File f = new File(Environment.getExternalStorageDirectory() + "/D2D"
                    + "/" + fileName);

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

    private void createNotification(String contentTitle, String contentText, int icon) {

        //Build the notification using Notification.Builder
        Notification.Builder builder = new Notification.Builder(mContext)
                .setSmallIcon(icon)
                .setAutoCancel(true)
                .setContentTitle(contentTitle)
                .setContentText(contentText);

        //Get current notification
        mNotification = builder.getNotification();

        //Show the notification
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }
    }
