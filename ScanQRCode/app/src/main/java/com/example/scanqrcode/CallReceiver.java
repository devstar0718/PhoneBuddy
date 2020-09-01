package com.example.scanqrcode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;

abstract class PhonecallReceiver extends BroadcastReceiver {

    //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;  //because the passed incoming is only valid in ringing


    @Override
    public void onReceive(Context context, Intent intent) {

        //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
        }
        else{
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if(stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)){
                state = TelephonyManager.CALL_STATE_IDLE;
            }
            else if(stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            }
            else if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)){
                state = TelephonyManager.CALL_STATE_RINGING;
            }


            onCallStateChanged(context, state, number);
        }
    }

    //Derived classes should override these to respond to specific events of interest
    protected abstract void onIncomingCallReceived(Context ctx, String number, Date start);
    protected abstract void onIncomingCallAnswered(Context ctx, String number, Date start);
    protected abstract void onIncomingCallEnded(Context ctx, String number, Date start, Date end);

    protected abstract void onOutgoingCallStarted(Context ctx, String number, Date start);
    protected abstract void onOutgoingCallEnded(Context ctx, String number, Date start, Date end);

    protected abstract void onMissedCall(Context ctx, String number, Date start);

    //Deals with actual events

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(Context context, int state, String number) {
        if(lastState == state || number == null){
            //No change, debounce extras
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;
                onIncomingCallReceived(context, number, callStartTime);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if(lastState != TelephonyManager.CALL_STATE_RINGING){
                    isIncoming = false;
                    callStartTime = new Date();
                    onOutgoingCallStarted(context, savedNumber, callStartTime);
                }
                else
                {
                    isIncoming = true;
                    callStartTime = new Date();
                    onIncomingCallAnswered(context, savedNumber, callStartTime);
                }

                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if(lastState == TelephonyManager.CALL_STATE_RINGING){
                    //Ring but no pickup-  a miss
                    onMissedCall(context, savedNumber, callStartTime);
                }
                else if(isIncoming){
                    onIncomingCallEnded(context, savedNumber, callStartTime, new Date());
                }
                else{
                    onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());
                }
                break;
        }
        lastState = state;
    }
}

public class CallReceiver extends PhonecallReceiver {
    public SocketHandler socketHandler;
    public CallReceiver(){
        socketHandler = SocketHandler.getInstance();
    }

    public void SendCallID(String command, String number, String start){
        SendCallID(command, number, start, "");
    }

    public void SendCallID(String command, String number, String start, String end){
        JSONObject obj = new JSONObject();
        try {
            obj.put("command", command);
            obj.put("number", number);
            obj.put("start", start);
            if(end.length() > 0)
                obj.put("end", end);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socketHandler.sendCallID(obj.toString());
    }

    private String DateToString(Date date){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = dateFormat.format(date);
        return strDate;
    }
    @Override
    protected void onIncomingCallReceived(Context ctx, String number, Date start)
    {
        System.out.println("Incoming Call Received: " + number + ", " + start.toString());
        SendCallID("CallReceived", number, DateToString(start));
    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number, Date start)
    {
        System.out.println("Incoming Call Answered: " + number + ", " + start.toString());
        SendCallID("CallStarted", number, DateToString(start));
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end)
    {
        System.out.println("Incoming Call Ended: " + number + ", " + start.toString() + ", " + end.toString());
        SendCallID("CallEnded", number, DateToString(start), DateToString(end));
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start)
    {
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end)
    {
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start)
    {
        System.out.println("Missed Call: " + number + ", " + start.toString());
        SendCallID("CallMissed", number, DateToString(start));
    }

}