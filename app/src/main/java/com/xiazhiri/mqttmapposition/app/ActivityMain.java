package com.xiazhiri.mqttmapposition.app;

import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

import java.util.Random;


public class ActivityMain extends ActionBarActivity implements MqttCallback, IMqttActionListener, LocationListener {

    @InjectView(R.id.mapView)
    MapView mapView;
    private MqttAndroidClient client;
    private boolean isConnected;
    private GraphicsLayer layer;
    String id = "Android" + String.valueOf(new Random().nextInt(100));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mapView.addLayer(new ArcGISTiledMapServiceLayer("http://cache1.arcgisonline.cn/ArcGIS/rest/services/ChinaOnlineCommunity/MapServer"));

        layer = new GraphicsLayer();
        mapView.addLayer(layer);

        mapView.getLocationDisplayManager().start();
        mapView.getLocationDisplayManager().setLocationListener(this);

        try {
            client = new MqttAndroidClient(this, "tcp://broker.mqttdashboard.com:1883", "Likaci/MqttMap/" + id);
            client.setCallback(this);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setKeepAliveInterval(10);
            options.setConnectionTimeout(1000);
            options.setCleanSession(false);
            client.connect(options, null, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        ButterKnife.reset(this);
        super.onDestroy();
    }

    //region LocationListener
    @Override
    public void onLocationChanged(Location location) {
        String msg = String.format("{\"id\":\"%s\",\"x\":%.9f,\"y\":%.9f}", id, location.getLongitude(), location.getLatitude());
        try {
            client.publish("Likaci/MqttMap", msg.getBytes(), 0, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
    //endregion

    //region MqttActionCallBack -- connection success or fail
    @Override
    public void onSuccess(IMqttToken iMqttToken) {
        isConnected = true;
        ShowToast("连接成功");
        try {
            client.subscribe("Likaci/MqttMap", 0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
        isConnected = false;
        ShowToast("连接失败");
    }
    //endregion

    //region MqttCallBack message or offline
    @Override
    public void connectionLost(Throwable throwable) {
        isConnected = false;
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        if (topic.equals("Likaci/MqttMap")) {
            String msg = new String(mqttMessage.getPayload());
            JSONObject json = new JSONObject(msg);
            if (!json.getString("id").equals(id)) {
                Point p = (Point) GeometryEngine.project(new Point(json.getDouble("x"), json.getDouble("y")), SpatialReference.create(4326), SpatialReference.create(3857));

                layer.removeAll();

                SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol(Color.parseColor("#763382"), 15, SimpleMarkerSymbol.STYLE.DIAMOND);
                layer.addGraphic(new Graphic(p, markerSymbol));

                TextSymbol textSymbol = new TextSymbol(15, json.getString("id"), Color.parseColor("#763382"), TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);
                textSymbol.setOffsetY(-15);
                layer.addGraphic(new Graphic(p, textSymbol));

            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
    //endregion

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    Toast toast;

    public void ShowToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(this, null, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
            toast.show();
        }
    }

}
