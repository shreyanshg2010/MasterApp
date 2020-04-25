package com.example.masterapp;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.InetAddresses;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public String SERVICE_NAME = "shrey Device";
    public String SERVICE_TYPE = "_gemineye._tcp.";
    private NsdManager mNsdManager;
    Intent intent;
    Intent intent1;

    public int SocketServerPort = 6000;
    private static final String REQUEST_CONNECT_CLIENT = "request-connect-client";

    public InetAddress hostAddress;
    Button btngetDevices;
    Button opendiscoverableDevices;
    public int hostPort;
    String[] StringDevices = new String[20];
    ArrayList<String> hostList = new ArrayList<>();
    int portList[]=new int[20];
    ListView listdevices;
    public int k = 0,l=0, m=0;
    public final int s1 = 1;
    public ArrayList<String> listDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        btngetDevices = findViewById(R.id.btnGetDevices);
        opendiscoverableDevices = findViewById(R.id.open);
        btngetDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                intent=new Intent(MainActivity.this, GetDevices.class);

                if (StringDevices != null) {
                    intent.putExtra("Devices", StringDevices);
                    intent.putExtra("portList",portList);
                    System.out.println(portList[0]);
                    System.out.println("Main Activity Size :"+ hostList.size());
                    intent.putStringArrayListExtra("HostList",hostList);
                    System.out.println(k);
                    intent.putExtra("Size", k);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "No devices Found", Toast.LENGTH_LONG).show();
                }
            }
        });
        opendiscoverableDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(UserSharedPref.initializeDeviceSetSharedPref(getApplicationContext()).getStringSet(UserSharedPref.rememberDeviceSet,null)==null)
                {
                    Toast.makeText(MainActivity.this,"No remembered devices",Toast.LENGTH_LONG).show();
                }
                else {
                    intent1=new Intent(MainActivity.this,Remember.class);
                    startActivity(intent1);
                }
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == s1) {
            if (resultCode == RESULT_OK) {
                System.out.println("Back to Main Activity");
            }
        }
    }

    @Override
    protected void onPause() {
        if (mNsdManager != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }

        StringDevices=new String[20];
        k=0;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdManager != null) {
            Log.d(TAG, "onResume: ");
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        }

    }

    @Override
    protected void onDestroy() {
        if (mNsdManager != null) {
            Log.d(TAG, "onDestroy: ");
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
        super.onDestroy();
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
                Toast.makeText(MainActivity.this, "Device Registered " + service.getServiceName(), Toast.LENGTH_SHORT).show();
                StringDevices[k++] = service.getServiceName();
                mNsdManager.resolveService(service, new MyResolveListener());
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            Log.e(TAG, "service lost: " + service);
            for (int i = 0; i < k; i++) {
                if (StringDevices[i] == service.getServiceName()) {
                    for (int j = i + 1; j < k; ++j) {
                        StringDevices[j - 1] = StringDevices[j];
                    }

                }
            }
            k--;

            System.out.println(k);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
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

            if (serviceInfo.getServiceName().equals(SERVICE_NAME)) {
                Log.d(TAG, "Same IP.");
                return;
            }
            hostPort = serviceInfo.getPort();
            hostAddress = serviceInfo.getHost();
            portList[l++]=hostPort;
            hostList.add(hostAddress.getHostAddress());
            System.out.println(l);
        }
    }
}

