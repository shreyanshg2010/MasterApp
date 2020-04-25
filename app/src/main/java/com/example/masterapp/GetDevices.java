package com.example.masterapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetDevices extends AppCompatActivity {
    private static final String TAG = "GetDevices";
    ListView listView;
    int portList[] = new int[20];
    int hostPort;
    TextView textView;
    public int SocketServerPort = 9000;
    private static final String REQUEST_CONNECT_CLIENT = "request-connect-client";
    ArrayList<String> hostList = new ArrayList<>();
    ArrayList<Socket> socketList = new ArrayList<>();
    String[] devices = new String[20];
    ServerSocket serverSocket;
    Set<String> rememberDevice = new HashSet<>();
    ArrayList<Integer> localport = new ArrayList<>();
    int count=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_devices);



        listView = findViewById(R.id.list1);
        textView = findViewById(R.id.text1);
        textView.setMovementMethod(new ScrollingMovementMethod());
        int s;
        Intent intent =getIntent();
        devices = intent.getExtras().getStringArray("Devices");
        s=intent.getIntExtra("Size",0);
        hostList = intent.getStringArrayListExtra("HostList");
        portList = intent.getIntArrayExtra("portList");
        s = intent.getExtras().getInt("Size");
        List<String> list = new ArrayList<>();
        for(int i=0;i<s;i++){
            if(devices[i]!=null){
                list.add(devices[i]);
                rememberDevice.add(devices[i]);
                UserSharedPref.initializeDeviceSetSharedPref(getApplicationContext()).edit().putStringSet(UserSharedPref.rememberDeviceSet,rememberDevice).apply();
            }
            else{
                break;
            }
        }
        ListAdapter adapter= new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list );
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                String item = ((TextView)view).getText().toString();

                try {
                    connectToHost(hostList.get(position),portList[position],position);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            }
        });
        setResult(RESULT_OK);


    }

    private void connectToHost(String ipAddress, int hostPort,int position) throws UnknownHostException {

        JSONObject jsonData = new JSONObject();
        Log.d(TAG, "connectToHost: ");
        try {
            jsonData.put("request", REQUEST_CONNECT_CLIENT);
            jsonData.put("ipAddress", ipAddress);
            jsonData.put("localip", getLocalIpAddress());
            try {
                ServerSocket serverSocket = new ServerSocket(0);
                 localport.add(serverSocket.getLocalPort());
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            jsonData.put("Port",localport.get(count-1));
            jsonData.put("position",position);
            Log.d(TAG, "connectToHost: 123");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "can't put request");
            return;
        }

        new SocketServerTask().execute(jsonData);
    }

    private String getLocalIpAddress() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
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
                socket = new Socket(InetAddress.getByName(jsonData.get("ipAddress").toString()),9000);

                socketList.add(socket);
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
               /*try {
                    serverSocket = new ServerSocket(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                ReceiveMessageTask receiveMessageTask = new ReceiveMessageTask(localport.get(count-1),count);
                count++;
                /*try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                receiveMessageTask.start();
                Log.d("hello","handler started");
                Toast.makeText(GetDevices.this, "Connection Done", Toast.LENGTH_SHORT).show();
                try {
                    listView.getChildAt(Integer.parseInt(jsonData.get("position").toString())).setBackgroundColor(getResources().getColor(R.color.green));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                Toast.makeText(GetDevices.this, "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        }

    }


    @Override
    public void onBackPressed() {

        super.onBackPressed();

    }

    private class ReceiveMessageTask extends Thread{
        int localPort;
        int count;
        public ReceiveMessageTask(int localport,int count){
            this.localPort = localport;
            this.count =count;
        }

        @Override
        public void run() {
            DataInputStream dataInputStream = null;
            try {
/*
                serverSocket = new ServerSocket(localPort);
*/


                while(true){
                    try {
                        serverSocket=new ServerSocket(localPort);
                        Log.d("hello","thread"+count);
                        Socket socket = serverSocket.accept();
                        Log.d("baap",""+socket.getLocalPort());
                        Log.d("hello","thread accept"+count);
                        Log.d("hello",""+socket.getLocalPort());
                        File file = new File(Environment.getExternalStorageDirectory().getPath(),"savedBytes"+count+".txt");
                        if(file.exists())
                            file.delete();
                        final FileOutputStream fileOutputStream = new FileOutputStream(file,true);
                        byte[] buffer = new byte[1024];
                        dataInputStream = new DataInputStream(socket.getInputStream());
                        int bytesRead;
                        while((bytesRead = dataInputStream.read(buffer))!=-1){
                            fileOutputStream.write(buffer,0,bytesRead);
                        }
                        FileInputStream fileInputStream = new FileInputStream(file);
                        byte[] messageByteArray = new byte[(int)file.length()];
                        fileInputStream.read(messageByteArray);
                        int index=0;
                        byte[] sizeAsByteArray = new byte[4];
                        System.arraycopy(messageByteArray,index,sizeAsByteArray,0,4);
                        index+=4;
                        int imageCount = ByteBuffer.wrap(sizeAsByteArray).getInt();
                        ArrayList<byte[]> delimiters = new ArrayList<>();
                        for(int i=0;i<=imageCount;i++){
                            byte[] byteArray = new byte[4];
                            System.arraycopy(messageByteArray,index,byteArray,0,4);
                            index+=4;
                            delimiters.add(byteArray);
                        }
                        int jsonSize = ByteBuffer.wrap(delimiters.get(0)).getInt();
                        byte[] jsonByteArray = new byte[jsonSize];
                        System.arraycopy(messageByteArray,index,jsonByteArray,0,jsonSize);
                        index+=jsonSize;
                        JSONObject jsonObject = new JSONObject(new String(jsonByteArray));
                        File slaveImages = new File(Environment.getExternalStorageDirectory().getPath(),"SlaveImages");
                        if(!slaveImages.exists()){
                            boolean success = slaveImages.mkdir();
                            if(success){
                                Log.d("hello","Directory created");
                            }
                            else {
                                Log.d("hello","directory creation failed");
                            }
                        }
                        String deviceName = jsonObject.getString("deviceName");
                        Log.d("hello",deviceName);

                        String receivedText = jsonObject.get("message").toString();

                        for(int i=0 ;i<imageCount;i++){
                            int imageSize = ByteBuffer.wrap(delimiters.get(i+1)).getInt();
                            byte[] imageByteArray = new byte[imageSize];
                            System.arraycopy(messageByteArray,index,imageByteArray,0,imageSize);
                            index+=imageSize;
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageByteArray,0,imageSize);
                            File imageFile = new File(slaveImages.getPath(),deviceName+"slaveImage"+(i+1)+".jpg");
                            if(imageFile.exists()){
                                imageFile.delete();
                            }
                            try{
                                OutputStream outputStream = new FileOutputStream(imageFile);
                                if(bitmap!=null){
                                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                                }
                                Log.d("hello","file created"+" "+deviceName);
                                outputStream.flush();
                                outputStream.close();
                            }catch (IOException e){
                                Log.d("hello","exception");
                                e.printStackTrace();
                            }
                        }
                        if(imageCount!=0){
                            String text = textView.getText().toString() + "\n" + receivedText+" ["+imageCount+" images received]";
                            textView.setText(text);
                        }
                        else {
                            String text = textView.getText().toString() + "\n" + receivedText;
                            textView.setText(text);
                        }
                        dataInputStream.close();
                        file.delete();
                        serverSocket.close();
                    }
                    catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
