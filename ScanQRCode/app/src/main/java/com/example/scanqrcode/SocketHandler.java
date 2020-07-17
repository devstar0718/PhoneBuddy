package com.example.scanqrcode;

import android.app.Activity;
import android.app.Application;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketHandler {
    private static final SocketHandler ourInstance = new SocketHandler();

    private String ip;
    private int port;

    private PrintWriter output;
    private BufferedReader input;

    private boolean connected = false;

    private int token = 0;

    private String message = "";

    final int token_size = 1024 * 1;

    private SnapActivity snapActivity = null;

    private OrderActivity orderActivity = null;

    private Socket socket = null;

    public SocketActivity currentActivity = null;

    public static SocketHandler getInstance() {
        return ourInstance;
    }

    private SocketHandler() {
        currentActivity = new SocketActivity();
    }

    public boolean isConnected() {
        return connected;
    }

    public void connect(MainActivity mainActivity, String ip, int port) {
        this.ip = ip;
        this.port = port;
        new Thread(new ThreadSocketConnect(mainActivity, ip, port)).start();
    }

    public void sendPicture(String message, SnapActivity snapActivity) {
        if (!connected) {
            Toast.makeText(snapActivity, "Socket not connected.", Toast.LENGTH_LONG).show();
            return;
        }
        this.message = message;
        this.snapActivity = snapActivity;
        snapActivity.setInitialProgressRange(message.length() / token_size);
        send_token(0);
    }

    public void sendOrder(String message, OrderActivity orderActivity) {
        if (!connected) {
            Toast.makeText(orderActivity, "Socket not connected.", Toast.LENGTH_LONG).show();
            return;
        }
        this.orderActivity = orderActivity;
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "order");
            obj.put("content", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new Thread(new ThreadSocketSender(obj.toString())).start();
    }

    private void send_token(int token) {
        int total_size = message.length();

        int start_index = token * token_size;
        int end_index = (token + 1) * token_size;

        String content = "";
        if (start_index >= total_size) {
            content = "-1#";
        } else {
            if (end_index >= total_size) {
                end_index = total_size - 1;
            }
            content = String.valueOf(token) + "#" + message.substring(start_index, end_index);
        }

        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "picture");
            obj.put("content", content);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new Thread(new ThreadSocketSender(obj.toString())).start();
    }

    class ThreadSocketConnect implements Runnable {
        private String ip;
        private int port;
        MainActivity mainActivity;

        ThreadSocketConnect(MainActivity mainActivity, String ip, int port) {
            this.mainActivity = mainActivity;
            this.ip = ip;
            this.port = port;
        }

        public void run() {
//            Socket socket;
            try {
                socket = new Socket(ip, port);
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.d("ALEXEI", "Connected");
                connected = true;
                new Thread(new ThreadSocketReceiver()).start();
                this.mainActivity.startTagActivity();
            } catch (IOException e) {
                e.printStackTrace();
                connected = false;
            }
        }
    }

    class ThreadSocketReceiver implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (!connected) break;
                try {
                    final String message = input.readLine();
                    if (message != null) {
                        Log.d("ALEXEI", "server: " + message);
                        if(message.length() >=5 && message.substring(0, 5).equals("MSG: ")){
                            currentActivity.ShowText(message.substring(5));
                        }
                        else if(message.length() >=5 && message.substring(0, 5).equals("BEP: ")){
                            currentActivity.Beep();
                        }
                        else if (message.equals("OK")) {
//                            orderActivity.actionCompleted();
                            currentActivity.gotoTagActivity();
                        } else {
                            int token = Integer.parseInt(message);
                            snapActivity.setProgressValue(token);
                            if (token >= 0) {
                                send_token(token + 1);
                            } else {
//                                Log.d("ALEXEI", "Message sent successfully");
//                                snapActivity.dispatchTakePictureIntent();
                                currentActivity.gotoTagActivity();
                            }
                        }
                    } else {
                        currentActivity.ShowText("Socket Connection has been lost.");
                        currentActivity.gotoMainActivity();
                        connected = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    currentActivity.ShowText("Socket Connection has been lost.");
                    currentActivity.gotoMainActivity();
                    connected = false;
                }
            }
        }
    }

    class ThreadSocketSender implements Runnable {
        private String message;

        ThreadSocketSender(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            output.write(message);
            Log.d("ALEXEI", Integer.toString(message.length()));
            output.flush();
            Log.d("ALEXEI", "client: " + message);
        }
    }


}
