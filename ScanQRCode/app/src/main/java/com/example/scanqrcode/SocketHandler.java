package com.example.scanqrcode;

import android.util.Log;
import android.widget.Toast;

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

    public static SocketHandler getInstance() {
        return ourInstance;
    }

    private SocketHandler() {

    }

    public void connect(MainActivity mainActivity, String ip, int port) {
        this.ip = ip;
        this.port = port;
        new Thread(new ThreadSocketConnect(mainActivity, ip, port)).start();
    }

    public void send(String message, SnapActivity snapActivity) {
        if (!connected) {
            Log.d("ALEXEI", "Socket not connected.");
            return;
        }
        this.message = message;
        this.snapActivity = snapActivity;
        snapActivity.setInitalProgressRange(message.length() / token_size);
        send_token(0);
    }

    public void send_token(int token) {
        int total_size = message.length();

        int start_index = token * token_size;
        int end_index = (token + 1) * token_size;
        if (start_index >= total_size) {
            new Thread(new ThreadSocketSender("-1#")).start();
            return;
        }
        if (end_index >= total_size) {
            end_index = total_size - 1;
        }
        String content = String.valueOf(token) + "#" + message.substring(start_index, end_index);
        new Thread(new ThreadSocketSender(content)).start();
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
            Socket socket;
            try {
                socket = new Socket(ip, port);
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.d("ALEXEI", "Connected");
                connected = true;
                new Thread(new ThreadSocketReceiver()).start();
                this.mainActivity.startSnapActivity();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ThreadSocketReceiver implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    final String message = input.readLine();
                    if (message != null) {
                        Log.d("ALEXEI", "server: " + message);
//                        int split = message.indexOf("#");
//                        int token = Integer.parseInt(message.substring(0, split-1));
                        int token = Integer.parseInt(message);
                        snapActivity.setProgressValue(token);
                        if(token >= 0) {
                            send_token(token + 1);
                        }
                        else {
                            Log.d("ALEXEI", "Message sent successfully");
                            snapActivity.dispatchTakePictureIntent();
                        }
                    } else {
//                        Thread1 = new Thread(new ThreadSocketConnect());
//                        Thread1.start();
//                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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