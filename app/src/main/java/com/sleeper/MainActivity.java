package com.sleeper;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.kittinunf.fuel.Fuel;
import com.github.kittinunf.fuel.core.FuelError;
import com.github.kittinunf.fuel.core.Handler;
import com.github.kittinunf.fuel.core.Request;
import com.github.kittinunf.fuel.core.Response;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_BLUETOOTH = 630;
    private static final int PERMISSION_REQUEST = 630;
    Set<BluetoothDevice> pairedDevices;
    ArrayList<DeviceItem> deviceItems = new ArrayList<>();
    ArrayList<DeviceItem> newDeviceItems = new ArrayList<>();
    ImageView bt_btn;
    Context context;
    CharSequence[] names, newNames;
    List<String> listItems = new ArrayList<String>();
    List<String> newListItems = new ArrayList<>();
    ProgressDialog progressDialog;
    CountDownTimer timer;
    LinearLayout function_on, function_off;
    TextView heart_rate_text;
    int last_heart_rate = 0;
    Request request;
    ConnectThread connectThread;
    BluetoothSocket socket;
    Button turn_on_btn, turn_off_btn;
    ManageConnectThread manageConnectThread;
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        bt_btn = (ImageView)findViewById(R.id.BT_btn);
        function_on = (LinearLayout)findViewById(R.id.function_on);
        function_off = (LinearLayout)findViewById(R.id.function_off);
        heart_rate_text = (TextView)findViewById(R.id.heart_rate_text);
        turn_on_btn = (Button)findViewById(R.id.turn_on_btn);
        turn_off_btn = (Button)findViewById(R.id.turn_off_btn);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Check if device supports BT
        if(bluetoothAdapter != null){
            if(!bluetoothAdapter.isEnabled()){
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, REQUEST_BLUETOOTH);
            }
        }else{
            Toast.makeText(this, "Your device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            System.exit(0);
        }

        clickEvents();
    }

    private void clickEvents() {

        bt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pairedDevices = bluetoothAdapter.getBondedDevices();
                if(pairedDevices.size() > 1){
                    for (BluetoothDevice device : pairedDevices ){
                        deviceItems.add(new DeviceItem(device.getName(), device.getAddress()));
                        listItems.add(device.getName());
                    }

                    names = listItems.toArray(new CharSequence[listItems.size()]);

                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Paired Devices")
                            .setItems(names, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                   // Toast.makeText(context, "The name is "+deviceItems.get(i).getName()+" with address "+deviceItems.get(i).getAdddress(), Toast.LENGTH_SHORT).show();
                                    connectThread = new ConnectThread(bluetoothAdapter.getRemoteDevice(deviceItems.get(i).getAdddress()), MY_UUID);
                                    connectThread.connect();
                                    socket = connectThread.getbTSocket();

//                                    BluetoothConnector connector = new BluetoothConnector(bluetoothAdapter.getRemoteDevice(deviceItems.get(i).getAdddress()),true,bluetoothAdapter,null);
//                                    try {
//                                        connector.connect();
//                                    } catch (IOException e) {
//                                        Log.e("ERROR","E no work"+e.toString());
//                                    }
                                    listItems.clear();
                                }
                            })
                            .setPositiveButton("SCAN FOR NEW DEVICE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    listItems.clear();
                                    startScan();
                                }
                            }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    listItems.clear();
                                }
                            }).create();
                    dialog.show();
                }

            }
        });


        function_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer = new CountDownTimer(5000, 1000) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        getFitbitData();
                        start();
                    }
                }.start();
            }
        });

        function_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(timer != null){
                    timer.cancel();
                }

                if(request != null)
                request.cancel();

            }
        });

        turn_on_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manageConnectThread = new ManageConnectThread();
                if(socket != null){
                    try {
                        manageConnectThread.sendData(socket, 49);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(context, "No connected device", Toast.LENGTH_SHORT).show();
                }

            }
        });

        turn_off_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manageConnectThread = new ManageConnectThread();
                if(socket != null){
                    try {
                        manageConnectThread.sendData(socket, 48);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(context, "No connected device", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void startScan() {

       if(ContextCompat.checkSelfPermission(context,
               Manifest.permission.ACCESS_COARSE_LOCATION )
               != PackageManager.PERMISSION_GRANTED){
           ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_REQUEST);
        }else{

           progressDialog = ProgressDialog.show(context,null,"Scanning for new devices...",false,false);


           IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
           filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
           context.registerReceiver(bReciever, filter);
           bluetoothAdapter.startDiscovery();


           new CountDownTimer(5000,1000){

               @Override
               public void onTick(long l) {

               }

               @Override
               public void onFinish() {
                   progressDialog.dismiss();

                   newNames = newListItems.toArray(new CharSequence[newListItems.size()]);

                   AlertDialog dialog = new AlertDialog.Builder(context)
                           .setTitle("Found Devices")
                           .setCancelable(false)
                           .setItems(newNames, new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialogInterface, int i) {
                                  Toast.makeText(context, "The name is "+newDeviceItems.get(i).getName()+" with address "+newDeviceItems.get(i).getAdddress(), Toast.LENGTH_SHORT).show();
                                   connectThread = new ConnectThread(bluetoothAdapter.getRemoteDevice(newDeviceItems.get(i).getAdddress()), UUID.randomUUID());
                                   connectThread.connect();
                                   newListItems.clear();
                               }
                           })
                           .setPositiveButton("RESCAN", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialogInterface, int i) {
                                   dialogInterface.dismiss();
                                   newListItems.clear();
                                   startScan();
                               }
                           }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialogInterface, int i) {
                                   dialogInterface.dismiss();
                                   newListItems.clear();
                                   context.unregisterReceiver(bReciever);
                                   bluetoothAdapter.cancelDiscovery();
                               }
                           }).create();
                   dialog.show();

               }
           }.start();
       }

    }

    private final BroadcastReceiver bReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() ==  BluetoothDevice.BOND_BONDED){
                    device.createBond();
                }
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Create a new device item
                DeviceItem newDevice = new DeviceItem(device.getName(), device.getAddress());
                newDeviceItems.add(newDevice);
                newListItems.add(newDevice.getName());
                // Add it to our adapter
                //mAdapter.add(newDevice);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_REQUEST:
                if(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(context, "Yay! Permission granted!", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(context, "Please grant permissions to scan for new Bluetooth devices", Toast.LENGTH_SHORT).show();
                }
        }
    }


    private void getFitbitData() {

        String url = "https://api.fitbit.com/1/user/-/activities/heart/date/2017-06-09/1d.json";

        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization","Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1UkZCQkgiLCJhdWQiOiIyMjhLVEMiLCJpc3MiOiJGaXRiaXQiLCJ0eXAiOiJhY2Nlc3NfdG9rZW4iLCJzY29wZXMiOiJ3aHIgd251dCB3cHJvIHdzbGUgd3dlaSB3c29jIHdzZXQgd2FjdCB3bG9jIiwiZXhwIjoxNDk4NjQzMjUzLCJpYXQiOjE0OTgwMzg0NTN9.mdLpgBbRqW0QIxOussxnmqtbYnUarDUkRc4iu1vCy28");

        request =  Fuel.get(url).header(headers).responseString(new Handler<String>() {
            @Override
            public void success(@NotNull Request request, @NotNull Response response, String s) {
                try {
                    JSONObject baseObject = new JSONObject(s);
                    JSONArray dataSet = baseObject.getJSONObject("activities-heart-intraday").getJSONArray("dataset");
                    int  lastIndex = dataSet.length() - 1;
                    JSONObject lastData = (JSONObject) dataSet.get(lastIndex);
                    Toast.makeText(context, lastData.getString("time"), Toast.LENGTH_SHORT).show();
                    last_heart_rate = Integer.parseInt(lastData.getString("value"));
                    heart_rate_text.setText("HEART RATE: "+last_heart_rate);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(@NotNull Request request, @NotNull Response response, @NotNull FuelError fuelError) {

            }
        });
    }
}
