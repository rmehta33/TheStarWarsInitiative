package com.example.rishuv.myapplication;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ScheduledExecutorService;

public class MainActivity extends Activity implements
        SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{

    private static final String TAG = "tag";

    private float[] gyroRawValues = {0.0f,0.0f,0.0f};
    private float[] accelRawValues = {0.0f,0.0f,0.0f};


    private TextView accelValue;
    private TextView gyroValue;

    Sensor gyroSensor;
    Sensor accelSensor;

    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                accelValue = (TextView) stub.findViewById(R.id.accelValueid);
                gyroValue = (TextView) stub.findViewById(R.id.gyroValueid);
                registerSensors();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /*public void sendStepCount(int steps, long timstamp){
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/step-counter");

        putDataMapRequest.getDataMap().putInt("step-count", steps);
        putDataMapRequest.getDataMap().putLong("timestamp", timstamp);

        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient,request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if(!dataItemResult.getStatus().isSuccess()){
                            Log.w(TAG, "you fucked up");
                        } else {
                            Log.w(TAG, "you good");
                        }
                    }
                });
    }
    */

    public void sendStepCount(float[] accelRaw, float[] gyroRaw){
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/sensor-data");

        putDataMapRequest.getDataMap().putFloatArray("accelRaw", accelRaw);
        putDataMapRequest.getDataMap().putFloatArray("gyroRaw", gyroRaw);
        putDataMapRequest.getDataMap().putLong("time",System.currentTimeMillis());

        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient,request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if(!dataItemResult.getStatus().isSuccess()){
                            Log.w(TAG, "you fucked up");
                        } else {
                            Log.w(TAG, "you good");
                        }
                    }
                });
    }



    private void registerSensors(){
        SensorManager mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregister(){
        SensorManager mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));

        mSensorManager.unregisterListener(this, gyroSensor);
        mSensorManager.unregisterListener(this, accelSensor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregister();

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged - accuracy: " + accuracy);
    }

    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){

            System.arraycopy(event.values, 0, gyroRawValues, 0, gyroRawValues.length);
            gyroValue.setText(String.format("%.3f %.3f %.3f", event.values[0], event.values[1], event.values[2]));
        }

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){

            System.arraycopy(event.values, 0, accelRawValues, 0, accelRawValues.length);
            accelValue.setText(String.format("%.3f %.3f %.3f", event.values[0], event.values[1], event.values[2]));
        }

        sendStepCount(gyroRawValues, accelRawValues);

    }


}