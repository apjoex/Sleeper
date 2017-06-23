package com.sleeper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by apjoe on 6/23/2017.
 */

public class ConnectThread extends Thread {

//    private BluetoothSocket bTSocket;
//
//    public boolean connect(BluetoothDevice bTDevice, UUID mUUID) {
//        BluetoothSocket temp = null;
//        try {
//            temp = bTDevice.createRfcommSocketToServiceRecord(mUUID);
//        } catch (IOException e) {
//            Log.d("CONNECT_THREAD","Could not create RFCOMM socket:" + e.toString());
//            return false;
//        }
//        try {
//            bTSocket.connect();
//        } catch(IOException e) {
//            Log.d("CONNECT_THREAD","Could not connect: " + e.toString());
//            try {
//                bTSocket.close();
//            } catch(IOException close) {
//                Log.d("CONNECT_THREAD", "Could not close connection:" + e.toString());
//                return false;
//            }
//        }
//        return true;
//    }
//
//    public boolean cancel() {
//        try {
//            bTSocket.close();
//        } catch(IOException e) {
//            Log.d("CONNECT_THREAD","Could not close connection:" + e.toString());
//            return false;
//        }
//        return true;
//    }

    private final BluetoothDevice bTDevice;
    private final BluetoothSocket bTSocket;

    public ConnectThread(BluetoothDevice bTDevice, UUID UUID) {
        BluetoothSocket tmp = null;
        this.bTDevice = bTDevice;

        try {
            tmp = this.bTDevice.createRfcommSocketToServiceRecord(UUID);
        }
        catch (IOException e) {
            Log.d("CONNECTTHREAD", "Could not start listening for RFCOMM");
        }
        bTSocket = tmp;
    }

    public boolean connect() {

        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        try {
            bTSocket.connect();
        } catch(IOException e) {
            Log.d("CONNECTTHREAD","Could not connect: " + e.toString());

            try {
                bTSocket.close();
            } catch(IOException close) {
                Log.d("CONNECTTHREAD", "Could not close connection:" + e.toString());
                return false;
            }
        }
        return true;
    }

    public boolean cancel() {
        try {
            bTSocket.close();
        } catch(IOException e) {
            return false;
        }
        return true;
    }

    public BluetoothSocket getbTSocket() {
        return bTSocket;
    }
}
