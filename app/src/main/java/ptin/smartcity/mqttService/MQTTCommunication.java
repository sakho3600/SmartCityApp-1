package ptin.smartcity.mqttService;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import ptin.smartcity.services.LocationService;
import ptin.smartcity.smartcityapp.MainActivity;
import ptin.smartcity.storage.DataStorage;

/**
 * Created by PauMarfany on 30/5/17.
 */

public class MQTTCommunication {

    // JSON Message
    private JSONObject jsonObject;

    // MQTT Comunication
    private MqttAndroidClient mqttAndroidClient;
    private Boolean IS_MESSAGE_SENDED = false;

    // Define variables
    private final String MQTT_TOPIC = "emergency/alerts";
    private final String ID = "SmartCityApp";
    private final String SERVER_URI;
    private final int KEEP_ALIVE = 60;

    public MQTTCommunication(Context act) throws IOException {
        // Obtenim la IP i el port del servidor
        if ( !DataStorage.has("MQTTipAddress") || !DataStorage.has("MQTTport") ) {
            throw new IOException("There is no socket data available!");
        }

        // Generem la URI del servidor
        this.SERVER_URI = "tcp://" + DataStorage.getData("MQTTipAddress") + ":" + DataStorage.getData("MQTTport");

        // Create mqttClient
        mqttAndroidClient = new MqttAndroidClient(act, this.SERVER_URI, MqttClient.generateClientId());

        // Set KEEPALIVE time
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setKeepAliveInterval(this.KEEP_ALIVE);

        // Connect to MQTT Broker
        Log.i("MQTT", "Connecting to broker...");
        try {
            IMqttToken mqttToken = mqttAndroidClient.connect(mqttConnectOptions);
            mqttToken.waitForCompletion(10000);
        } catch ( MqttException e ) { throw new IOException("Cannot connect to MQTT broker!"); }

    }

    // Subscribe to MQTT topic
    public void subscribeToTopic() throws IOException {
        // Ens connectem al tòpic
        try {
            mqttAndroidClient.subscribe(this.MQTT_TOPIC, 0);
            Log.i("MQTT", "Subscribed to topic: " + this.MQTT_TOPIC);
        } catch ( MqttException e ) { throw new IOException("Cannot subscribe to MQTT topic!"); }
    }

    // Parse data from fields
    private void getData() {
        // Retrive data from filesystem
        String name = DataStorage.getData("Name");
        String surname = DataStorage.getData("Surname");
        String gender = DataStorage.getData("Gender");
        String birthday = DataStorage.getData("Birthday");
        String phoneNumber = DataStorage.getData("PhoneNumber");
        String comments = DataStorage.getData("Comments");

        // Obtenim la localització
        String latitude = String.valueOf( LocationService.getLatitude() );
        String longitude = String.valueOf( LocationService.getLongitude() );

        // We add all the values
        try {
            jsonObject.put("id", this.ID);
            jsonObject.put("EmergencyType", MainActivity.EMERGENCY_OPTION);
            jsonObject.put("Name", name);
            jsonObject.put("Surname", surname);
            jsonObject.put("Gender", gender);
            jsonObject.put("Birthday", birthday);
            jsonObject.put("PhoneNumber", phoneNumber);
            jsonObject.put("Comments", comments);
            jsonObject.put("Latitude", latitude);
            jsonObject.put("Longitude", longitude);
        } catch ( JSONException e ) { e.printStackTrace(); }
    }

    // Send message over MQTT Topic
    public void sendMessage() throws IOException {
        // We create a JSONObject
        this.jsonObject = new JSONObject();
        this.getData();

        // We send the message
        try {
            MqttMessage message = new MqttMessage(jsonObject.toString().getBytes());

            // Publish message
            IMqttDeliveryToken token = mqttAndroidClient.publish(this.MQTT_TOPIC, message);
            Log.i("MQTT", "Send message to MQTT broker");

        } catch (MqttException e) { throw new IOException("Cannot send message to MQTT broker!"); }
    }

    // Disconnect from MQTT Broker
    public void disconnect() throws IOException {
        // We disconnect from the broker
        try {
            this.mqttAndroidClient.disconnect();
            Log.i("MQTT", "Disconnected from broker");
        } catch (MqttException e) { throw new IOException("Cannot disconnect from MQTT topic!"); }
    }

}
