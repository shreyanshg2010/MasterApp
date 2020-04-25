package com.example.masterapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Remember extends AppCompatActivity {

    private static final String TAG = "Remember";
    List<String> arrayList = new ArrayList<String>();
    ListView listView;
    public String SERVICE_TYPE = "_gemineye._tcp.";
    public String SERVICE_NAME = "shrey Device";
    NsdManager nsdManager;
    public String searchDevice;
    InetAddress ipAddress;
    private static final String REQUEST_CONNECT_CLIENT = "request-connect-client";
    public  int HOST_PORT = 9000;
    public int list_position = -1;
    int discoveryStarted = 0;
    int discovered = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remember);
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        listView = findViewById(R.id.list2);
        arrayList.addAll(UserSharedPref.initializeDeviceSetSharedPref(getApplicationContext()).getStringSet(UserSharedPref.rememberDeviceSet,null));

        ListAdapter adapter= new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                searchDevice = parent.getItemAtPosition(position).toString().trim();
                Log.d(TAG,""+searchDevice);
                list_position = position;

                if(discoveryStarted == 0){
                    if (nsdManager != null) {
                        Log.d(TAG, "onResume: ");
                        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
                        discoveryStarted = 1;
                    }
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (nsdManager != null) {
//            Log.d(TAG, "onResume: ");
//            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(discoveryStarted ==1){
            if (nsdManager != null) {
                nsdManager.stopServiceDiscovery(mDiscoveryListener);
                discoveryStarted = 0;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void connectToHost(String ipAddress, int hostPort,int position) throws UnknownHostException {

        JSONObject jsonData = new JSONObject();
        Log.d(TAG, "connectToHost: ");
        try {
            jsonData.put("request", REQUEST_CONNECT_CLIENT);
            jsonData.put("ipAddress", ipAddress);
            jsonData.put("localip", getLocalIpAddress());
            jsonData.put("Port",hostPort);
            jsonData.put("position",position);
            Log.d(TAG, "connectToHost: 123");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "can't put request");
            return;
        }

        new Remember.SocketServerTask().execute(jsonData);
    }

    public class SocketServerTask extends AsyncTask<JSONObject,Void, Void> {
        private JSONObject jsonData;
        private boolean success;


        @Override
        protected Void doInBackground(JSONObject... params) {

            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            jsonData = params[0];

            try {
                Log.d(TAG, "doInBackground: ");
                socket = new Socket(InetAddress.getByName(jsonData.get("ipAddress").toString()),Integer.parseInt(jsonData.get("Port").toString()));

                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream.writeUTF(jsonData.toString());
                Log.i(TAG, "waiting for response from host");
                String response = dataInputStream.readUTF();
                if (response != null && response.equals("Connection Accepted")) {
                    success = true;
                } else {
                    success = false;
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                success = false;
            } finally {

                if (socket != null) {
                    try {
                        Log.i(TAG, "closing the socket");
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (success) {
//                GetDevices.ReceiveMessageTask receiveMessageTask = new GetDevices.ReceiveMessageTask();
//                receiveMessageTask.start();
                Toast.makeText(Remember.this, "Connection Done", Toast.LENGTH_SHORT).show();
                try {
                    listView.getChildAt(Integer.parseInt(jsonData.get("position").toString())).setBackgroundColor(getResources().getColor(R.color.green));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                Toast.makeText(Remember.this, "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        }


    }

    private String getLocalIpAddress() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }

    NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(TAG, "Service discovery started");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            Log.d(TAG, "Service discovery success : " + service);
            Log.d(TAG, "Host = " + service.getServiceName());
            Log.d(TAG, "port = " + String.valueOf(service.getPort()));

            if (!service.getServiceType().equals(SERVICE_TYPE)) {


                Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
            }
            else if (service.getServiceName().equals(SERVICE_NAME)) {


                Log.d(TAG, "Same machine: " + SERVICE_NAME);
            }
            else {
                Log.d(TAG, "Diff Machine : " + service.getServiceName());
                Toast.makeText(Remember.this, "Device Registered " + service.getServiceName(), Toast.LENGTH_SHORT).show();
                if(service.getServiceName().equals(searchDevice)){
                    discovered = 1;
                    nsdManager.resolveService(service,new MyResolveListener());
                }
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            Log.e(TAG, "service lost: " + service);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            nsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            nsdManager.stopServiceDiscovery(this);
        }
    };

    public class MyResolveListener implements NsdManager.ResolveListener {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed " + errorCode);
            Log.e(TAG, "service = " + serviceInfo);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "Resolve Succeeded. " + serviceInfo);
            if(serviceInfo.getServiceName().equals(searchDevice));
            {
                if(discovered == 1){
                    discovered = 0;
                    ipAddress = serviceInfo.getHost();
                    if (nsdManager != null) {
                        nsdManager.stopServiceDiscovery(mDiscoveryListener);
                        discoveryStarted = 0;
                    }
                    try {
                        connectToHost(ipAddress.getHostAddress(),HOST_PORT,list_position);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
