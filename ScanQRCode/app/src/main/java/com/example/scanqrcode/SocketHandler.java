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

    public static SocketHandler getInstance(){
        return ourInstance;
    }

    private SocketHandler(){

    }

    public void connect(MainActivity mainActivity, String ip, int port){
        this.ip = ip;
        this.port = port;
        new Thread(new ThreadSocketConnect(mainActivity, ip, port)).start();
    }

    public void send(String message){
        if(! connected) {
            Log.d("ALEXEI", "Socket not connected.");
            return;
        }
        new Thread(new ThreadSocketSender(message)).start();
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
            output.write("*Start*");
            output.write(message);
            Log.d("ALEXEI", "Length: " + Integer.toString(message.length()));
            output.write("*End*");
            output.flush();
            Log.d("ALEXEI", "client: " + message);
        }
    }
}
