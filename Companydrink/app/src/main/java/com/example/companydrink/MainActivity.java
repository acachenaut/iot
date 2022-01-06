package com.example.companydrink;

import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    MqttAndroidClient client;
    TextView subText;
    EditText employeeIdEdit;
    int drinkId = 0;
    int radioBtnIndex = 0;
    int employeeId = 0;
    List<String> list = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subText = (TextView)findViewById(R.id.subText);
        employeeIdEdit = (EditText) findViewById(R.id.editText1);


        if (readFromFile(MainActivity.this) == "" || readFromFile(MainActivity.this).compareTo("") == 0){
            employeeId = 0;
            employeeIdEdit.setText("0");
        }
        else {
            employeeId = Integer.parseInt(readFromFile(MainActivity.this));
            employeeIdEdit.setText(String.valueOf(employeeId));
        }

        list.add("Café");
        list.add("Café au lait");
        list.add("Coca Cola");
        list.add("Chocolat Chaud");
        list.add("IceTea");

        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O){

            NotificationChannel channel= new NotificationChannel("My Notification","My Notification",NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager =getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }


        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://test.mosquitto.org:1883",clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this,"connected!!",Toast.LENGTH_LONG).show();
                    setSubscription();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this,"connection failed!!",Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        employeeIdEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString() == "" || s.toString().compareTo("") == 0){
                    employeeId = 0;
                    writeToFile("0", MainActivity.this);
                } else {
                    writeToFile(s.toString(), MainActivity.this);
                    employeeId = Integer.parseInt(s.toString());
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                if (employeeId == Integer.parseInt(topic.substring(topic.lastIndexOf("/")+1))) {
                    drinkId = Integer.parseInt(message.toString());
                    subText.setText(list.get(drinkId-1));

                    String textTitle = new String("Envie d'une boisson ?");
                    String textContent = new String("Venez voir la boisson que l'on vous propose");
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this,"My Notification");
                    builder.setContentTitle(textTitle);
                    builder.setContentText(textContent);
                    builder.setSmallIcon(R.drawable.ic_launcher_background);
                    builder.setAutoCancel(true);
                    Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("message","ok");
                    PendingIntent pendingIntent=PendingIntent.getActivity(MainActivity.this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(pendingIntent);
                    NotificationManagerCompat managerCompat=NotificationManagerCompat.from(MainActivity.this);
                    managerCompat.notify(1,builder.build());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

    }

    public void published(View v){

        String topic = "COMPANY/OpenSpace1/Order";
        String message = employeeId + ";" + drinkId;
        try {
            client.publish(topic, message.getBytes(),0,false);
            Toast.makeText(this,"Commande envoyée !",Toast.LENGTH_SHORT).show();
        } catch ( MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSubscription(){

        try{

            client.subscribe("COMPANY/NotifyEmployee/#",0);


        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    public void rien(View v){

        Toast.makeText(MainActivity.this,"Commande annulée !",Toast.LENGTH_LONG).show();

    }

    public void cafe(View view) {
        radioBtnIndex = 1;
    }

    public void cafeLait(View view) {
        radioBtnIndex = 2;
    }

    public void coca(View view) {
        radioBtnIndex = 3;
    }

    public void chocolat(View view) {
        radioBtnIndex = 4;
    }

    public void iceTea(View view) {
        radioBtnIndex = 5;
    }

    public void choicePublish(View view) {
        String topic = "COMPANY/OpenSpace1/Order";
        String message = employeeId + ";" + radioBtnIndex;
        try {
            client.publish(topic, message.getBytes(),0,false);
            Toast.makeText(this,"Commande envoyée !",Toast.LENGTH_SHORT).show();
        } catch ( MqttException e) {
            e.printStackTrace();
        }
    }

    private void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readFromFile(Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret.replaceAll("[^\\d.]", "");
    }
}