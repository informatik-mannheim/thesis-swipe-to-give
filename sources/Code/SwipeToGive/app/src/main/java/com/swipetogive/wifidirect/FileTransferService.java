// Copyright 2011 Google Inc. All Rights Reserved.

package com.swipetogive.wifidirect;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.swipetogive.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        Log.d("action", intent.getAction());
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            ArrayList<String> fileUri = intent.getStringArrayListExtra(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            Uri uri;
            ArrayList<File> files = new ArrayList<>();

            for(int i = 0; i < fileUri.size(); i++) {
                uri = Uri.parse(fileUri.get(i));
                files.add(new File(uri.getPath()));
            }

            try {
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                sendFileNamesAndFileSizes(files, dataOutputStream);
                byte [] buffer = new byte[1024];
                sendFiles(files, dataOutputStream, buffer);
            } catch (IOException e) {
                Toast.makeText(context,e.getMessage(),Toast.LENGTH_SHORT).show();
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void sendFiles(ArrayList<File> files, DataOutputStream dataOutputStream, byte[] buffer) throws IOException {
        int n = 0;
        // outer loop, executes one for each file
        for(int i =0; i < files.size(); i++){

            System.out.println(files.get(i).getName());
            // create new fileinputstream for each file
            FileInputStream fileInputStream = new FileInputStream(files.get(i));

            //write file to dos
            while((n = fileInputStream.read(buffer)) != -1){
                dataOutputStream.write(buffer,0,n);
                dataOutputStream.flush();
            }
        }
    }

    private void sendFileNamesAndFileSizes(ArrayList<File> files, DataOutputStream dataOutputStream) throws IOException {
        //write the number of files to the server
        dataOutputStream.writeInt(files.size());
        dataOutputStream.flush();

        // write file names
        for(int i = 0; i < files.size(); i++){
            dataOutputStream.writeUTF(files.get(i).getName());
            dataOutputStream.writeLong(files.get(i).length());
            dataOutputStream.flush();
        }
    }
}