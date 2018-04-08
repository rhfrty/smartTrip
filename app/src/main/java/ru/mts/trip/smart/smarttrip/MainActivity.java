package ru.mts.trip.smart.smarttrip;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.util.Log;
//For GPS
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.view.View.OnClickListener;
import android.os.AsyncTask;
//for Json
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
//for WS
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
//for bluetooth
import android.bluetooth.*;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.search.Category;
import com.here.android.mpa.search.CategoryFilter;

import junit.framework.Test;

public class MainActivity extends AppCompatActivity {

    TextView main_text, gps_text, err;
    Button btn_och, btn_find;
    public Spinner spinnerPetrol;
    public static String LOG_TAG = "my_log";
    public boolean flagBluethooth, mapFlag = false;
    ArrayList <String> petrol_station = new ArrayList<>();
    ArrayList <Integer> petrol_destantion = new ArrayList<>();
    ArrayList <Double> petrol_x = new ArrayList<>();
    ArrayList <Double> petrol_y = new ArrayList<>();
    public String nextJson = null;
    public Integer count_petrol, rad, errCode;
    public String gpsLatitude, gpsLongitude;
    public double gpsX, gpsY;
    public String resUrl = "";
    private MapMarker m_map_marker;
    ArrayList <MapMarker> p_map_marker = new ArrayList<>();
    BluetoothSocket clientSocket;
    public Map m_map;
    public Image p_marker_img = new Image();
    public ArrayAdapter<String> adapter;
    AlertDialog.Builder ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        main_text = (TextView) findViewById(R.id.main_text);
        gps_text = (TextView) findViewById(R.id.gps_text);
        btn_och = (Button) findViewById(R.id.btn_och);
        err = (TextView) findViewById(R.id.err);
        spinnerPetrol = (Spinner)findViewById(R.id.spinnerPetrol);
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
        ad = new AlertDialog.Builder(this);
        ad.setTitle("Внимание");
        ad.setPositiveButton("Позвонить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                err.setText("Звонок");
                String toDial="tel:89885301214";
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(toDial)));
                dialog.dismiss();
            }
        });
        ad.setNegativeButton("Заявка", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                err.setText("Заявка №Y2384");
                dialog.dismiss();
            }
        });
        ad.setCancelable(true);


        /* Here Карта */
        mapFragment.init(new OnEngineInitListener() {
            @Override  public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    m_map = mapFragment.getMap();
                    m_map.setCenter(new GeoCoordinate(47.2638, 39.6428), Map.Animation.NONE);
                } else {
                    System.out.println("ERROR: Cannot initialize MapFragment");
                }
            }
        });

        OnClickListener ocl_btn_och = new OnClickListener() {
            @Override
            public void onClick(View v) {
                String enableBT = BluetoothAdapter.ACTION_REQUEST_ENABLE;
                startActivityForResult(new Intent(enableBT), 0);
                if (m_map != null && p_map_marker != null) {
                    for (int j = 0; j < p_map_marker.size(); j++) {
                        m_map.removeMapObject(p_map_marker.get(j));
                    }
                    p_map_marker.clear();
                }

                BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
                byte[] buffer = new byte[16];
                try{
                    BluetoothDevice device = bluetooth.getRemoteDevice("98:D3:32:20:85:31");

                    Method m = device.getClass().getMethod(
                            "createRfcommSocket", new Class[] {int.class});

                    clientSocket = (BluetoothSocket) m.invoke(device, 1);
                    clientSocket.connect();
                    OutputStream outStream = clientSocket.getOutputStream();
                    outStream.write('1');
                    InputStream inputStream = clientSocket.getInputStream();
                    DataInputStream dinput = new DataInputStream(inputStream);
                    dinput.readFully(buffer, 0, buffer.length);
                    String readMessage = new String(buffer, 0, 16);

                    clientSocket.close();

                    calculation readings = new calculation();
                    rad = readings.devided(readMessage);
                    errCode = readings.errCode;
                    if (errCode == 1367) {
                        err.setText("ABS");
                        ad.setMessage("Обнаружена неисправность ABS, позвонить официальному диллеру или оставить онлайн заявку по поиску севиса.");
                        ad.show();
                    }

                    //err.setText("Send: " + String.valueOf(r));
                } catch (IOException e) {
                    Log.d("BLUETOOTH", e.getMessage());
                } catch (SecurityException e) {
                    Log.d("BLUETOOTH", e.getMessage());
                } catch (NoSuchMethodException e) {
                    Log.d("BLUETOOTH", e.getMessage());
                } catch (IllegalArgumentException e) {
                    Log.d("BLUETOOTH", e.getMessage());
                } catch (IllegalAccessException e) {
                    Log.d("BLUETOOTH", e.getMessage());
                } catch (InvocationTargetException e) {
                    Log.d("BLUETOOTH", e.getMessage());
                }

                nextJson = null;
                jsonAct();
                m_map.setCenter(new GeoCoordinate(gpsX, gpsY), Map.Animation.NONE);
                if (m_map != null && m_map_marker != null) {
                    m_map.removeMapObject(m_map_marker);
                    m_map_marker = null;
                }
                createMapMarker(m_map);
            }


        };



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            gps_text.setText("Нет прав");
            if (manager != null) {
                Location location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location == null) {
                    location =  manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    gpsLongitude = String.valueOf(location.getLongitude());
                    gpsLatitude = String.valueOf(location.getLatitude());
                    gps_text.setText(gpsLatitude + " : " + gpsLongitude);
                }
                if (location != null) {
                    gpsLongitude = String.valueOf(location.getLongitude());
                    gpsLatitude = String.valueOf(location.getLatitude());
                    gps_text.setText(gpsLatitude + " : " + gpsLongitude);
                }
            }
            //Location location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return;
        } else {
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, MainActivity);
        }

        btn_och.setOnClickListener(ocl_btn_och);


    }


    private class ParseTask extends AsyncTask<Void, Void, String>   {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultJson = "";


        @Override
        protected String doInBackground(Void... params)  {
            // получаем данные с внешнего ресурса
            try {
                if (nextJson != null) {
                    resUrl = nextJson;
                }
                URL url = new URL(resUrl);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Accept-Language","ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                resultJson = buffer.toString();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return resultJson;
        }

        @Override
        protected void onPostExecute(String strJson) {
            super.onPostExecute(strJson);
            JSONObject dataJsonObj = null;

            try {
                dataJsonObj = new JSONObject(strJson);
                JSONArray res_itm = null;
                JSONObject res = null;
                if (dataJsonObj.has("results")){
                    res = dataJsonObj.getJSONObject("results");
                    res_itm = res.getJSONArray("items");
                    if (res.has("next")) {
                        nextJson = res.getString("next");
                    }
                    else {
                        nextJson = null;
                    }
                }
                else {
                    res_itm = dataJsonObj.getJSONArray("items");
                    if (dataJsonObj.has("next")) {
                        nextJson = dataJsonObj.getString("next");
                    }
                    else {
                        nextJson = null;
                    }
                }
                for (int i = 0; i < res_itm.length(); i++) {
                    JSONObject items = res_itm.getJSONObject(i);
                    petrol_station.add(items.getString("title"));
                    petrol_destantion.add(items.getInt("distance"));
                    JSONArray res_pos = items.getJSONArray("position");
                    petrol_x.add(res_pos.optDouble(0));
                    petrol_y.add(res_pos.optDouble(1));
                    count_petrol = ++count_petrol;
                }

            } catch (JSONException e) {
                e.printStackTrace();
                main_text.setText("JSON error : ");
            }
            if (nextJson != null) {
                new ParseTask().execute();
            }
            main_text.setText(" => " + count_petrol);
            if (count_petrol > 0 && count_petrol <= 2) {
                soudPlay("petrol");
                err.setText(rad.toString());
                try {
                    p_marker_img.setImageResource(R.drawable.point_petrol);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                for (int k = 0; k < count_petrol; k++) {
                    p_map_marker.add(new MapMarker(new GeoCoordinate(petrol_x.get(k), petrol_y.get(k)), p_marker_img));
                    m_map.addMapObject(p_map_marker.get(k));
                }
            }
        }

    }

    public void jsonAct() {
        petrol_station.clear();
        count_petrol = 0;
        resUrl = "https://places.cit.api.here.com/places/v1/discover/explore?in="
                + gpsLatitude + ","
                + gpsLongitude + ";r="
                + rad
                + "&cat=petrol-station"
                + "&app_id=BUV5QxYfUArlNnhFgHD7"
                + "&app_code=met4vkOXKaXnEAnR4XY3RQ";
        new ParseTask().execute();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, petrol_station);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPetrol.setAdapter(adapter);
        OnItemSelectedListener itemSelectedListener = new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = (String)parent.getItemAtPosition(position);
                main_text.setText(item);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerPetrol.setOnItemSelectedListener(itemSelectedListener);
    }


    private LocationListener MainActivity = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location!=null) {
                gps_text.setText(String.valueOf(location.getLatitude()) + " : " + String.valueOf(location.getLongitude()));
                gpsLatitude = String.valueOf(location.getLatitude());
                gpsLongitude = String.valueOf(location.getLongitude());
                gpsX = location.getLatitude();
                gpsY = location.getLongitude();
            }
            else{
                gps_text.setText("Sorry, location unavailable");
            }
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
        @Override
        public void onProviderEnabled(String provider) {
        }
        @Override
        public void onProviderDisabled(String provider) {
        }
    };



    //bluethooth
    public void BluethoothTest()
    {
        BluetoothAdapter bluetoothAd = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAd.isEnabled()) {
        }
        else
        {
            if (flagBluethooth == false) {
                // Bluetooth выключен. Предложим пользователю включить его.
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
                flagBluethooth = true;
            }
        }
    }


    private void createMapMarker(Map m_map) {
        Image marker_img = new Image();
        try {
            marker_img.setImageResource(R.drawable.point_center);
        } catch (IOException e) {
            e.printStackTrace();
        }
        m_map_marker = new MapMarker(m_map.getCenter(), marker_img);
        m_map.addMapObject(m_map_marker);

    }


    private void soudPlay(String send) {
        MediaPlayer mp;
        if (send == "petrol"){
            mp = MediaPlayer.create(this, R.raw.disappointed);
        }else {
            mp = MediaPlayer.create(this, R.raw.ok);
        }
        mp.start();
    }


}

