package com.parrot.bebopdronepilotingnewapi;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

public class MainActivity extends WearableActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SensorEventListener,
        MessageApi.MessageListener
{

    double futureTime = 0;
    double waitTime = 250;
    int recieveRequest = 0;

    private PutDataMapRequest putDataMapRequest;

    int CoordinateFiveRunnableRecorderA = 0;
    int CoordinateFiveRunnableRecorderB = 0;
    int ifStatementSwitch = 1;
    int request = 0;

    Double[] initAccel = {0.0, 0.0, 0.0};
    Double[] endAccel = {0.0, 0.0, 0.0};
    float[] CoordinateChange = {0.0f, 0.0f, 0.0f};
    Double[] accelRaw = {0.0, 0.0, 0.0};
    float[][] CoordinateChangeFiveLayer = {
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
    };

    Vibrator vibrator;
    long[] vibrationPattern = {0, 500, 50, 300};
    final int indexInPatternToRepeat = -1;

    Thread getFiveCoordinateThread;

    Sensor gyroSensor;
    Sensor accelSensor;

    private GoogleApiClient mGoogleApiClient;
    private int counter = 1;

    private static final String TAG = "TAGW";

    /*

    adb forward tcp:4444 localabstract:/adb-hub
    adb connect 127.0.0.1:4444

     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setAmbientEnabled();

        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                Log.w(TAG, "accelValue was " +
                        "run");
            }
        });

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();



    }


    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregister();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {

            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void getFiveCoordinatesButtonWear(View view){
        getFiveCoordinates();
    }

    public void getFiveCoordinates(){
        Log.w(TAG, "ACTIVE");

        final Runnable recorder = new Runnable() {
            @Override

            public void run() {

                Log.w(TAG, "Thread Wear start");


                int waitTimeInterval = 40;
                int waitTime = 200 - waitTimeInterval;



                putDataMapRequest = PutDataMapRequest.create("/sendWearFive");
                request++;
                putDataMapRequest.getDataMap().putInt("request", request);

                float[][] arrayOne = {
                        {0.0f, 0.0f, 0.0f},
                        {0.0f, 0.0f, 0.0f},
                        {0.0f, 0.0f, 0.0f},
                        {0.0f, 0.0f, 0.0f},
                        {0.0f, 0.0f, 0.0f},
                };

                float[][] arrayTwo = {
                        {0.0f, 0.0f, 0.0f},
                        {0.0f, 0.0f, 0.0f},
                        {0.0f, 0.0f, 0.0f},
                        {0.0f, 0.0f, 0.0f},
                        {0.0f, 0.0f, 0.0f},
                };

                synchronized (this) {
                    try {
                        wait((long) (1500));
                        Log.w(TAG, String.format("WAITED"));


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.w(TAG, "Log vibrate Start");
                vibrator.vibrate(100);

                synchronized (this) {
                    try {
                        wait((long) (100));


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.w(TAG, "Log vibrate Ended");

                futureTime = System.currentTimeMillis() + ((waitTime+waitTimeInterval)*5);
                while(System.currentTimeMillis()<futureTime) {

                    synchronized (this) {
                        try {
                            wait((long) (waitTime));


                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    //keep this for loop - it syncs the cordinates properly
                    for(CoordinateFiveRunnableRecorderB = 0; CoordinateFiveRunnableRecorderB<3; CoordinateFiveRunnableRecorderB++) {
                        arrayOne[CoordinateFiveRunnableRecorderA][CoordinateFiveRunnableRecorderB] = CoordinateChange[CoordinateFiveRunnableRecorderB];
                    }

                    synchronized (this) {
                        try {
                            wait((long) (waitTimeInterval));


                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    for(CoordinateFiveRunnableRecorderB = 0; CoordinateFiveRunnableRecorderB<3; CoordinateFiveRunnableRecorderB++) {
                        arrayTwo[CoordinateFiveRunnableRecorderA][CoordinateFiveRunnableRecorderB] = CoordinateChange[CoordinateFiveRunnableRecorderB];
                        CoordinateChangeFiveLayer[CoordinateFiveRunnableRecorderA][CoordinateFiveRunnableRecorderB] = (arrayOne[CoordinateFiveRunnableRecorderA][CoordinateFiveRunnableRecorderB] +
                                arrayTwo[CoordinateFiveRunnableRecorderA][CoordinateFiveRunnableRecorderB])/2;

                    }



                    putDataMapRequest.getDataMap().putFloatArray("fiveCoordinateChangeArray" + Integer.toString(CoordinateFiveRunnableRecorderA), CoordinateChangeFiveLayer[CoordinateFiveRunnableRecorderA]);
                    Log.w(TAG, String.format("%s ---- %d", "fiveCoordinateChangeArray" + Integer.toString(CoordinateFiveRunnableRecorderA), CoordinateFiveRunnableRecorderA));
                    Log.w(TAG, "arrayOne = " + Arrays.toString(arrayOne[CoordinateFiveRunnableRecorderA]));
                    Log.w(TAG, "arrayTwo = " + Arrays.toString(arrayTwo[CoordinateFiveRunnableRecorderA]));
                    Log.w(TAG, Arrays.toString(CoordinateChangeFiveLayer[CoordinateFiveRunnableRecorderA]));
                    Log.w(TAG, Arrays.toString(putDataMapRequest.getDataMap().getFloatArray("fiveCoordinateChangeArray" + Integer.toString(CoordinateFiveRunnableRecorderA))));

                    CoordinateFiveRunnableRecorderA++;

                }

                CoordinateFiveRunnableRecorderA = 0;
                CoordinateFiveRunnableRecorderB = 0;

                PutDataRequest rq = putDataMapRequest.asPutDataRequest();
                Wearable.DataApi.putDataItem(mGoogleApiClient, rq)
                        .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                if (!dataItemResult.getStatus().isSuccess()) {
                                    Log.w(TAG, "request failed wear");
                                } else {
                                    Log.w(TAG, "request success wear");
                                }
                            }
                        });

                //EndDataMapStuff

                Log.w(TAG, Arrays.deepToString(CoordinateChangeFiveLayer));
            };
        };

        getFiveCoordinateThread = new Thread(recorder);
        getFiveCoordinateThread.start();


    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            for (int a = 0; a < accelRaw.length; a++) {
                accelRaw[a] = (double) event.values[a];
            }

            if (ifStatementSwitch == 1) {
                System.arraycopy(accelRaw, 0, initAccel, 0, initAccel.length);
            } else {
                System.arraycopy(accelRaw, 0, endAccel, 0, endAccel.length);
            }

            ifStatementSwitch = ifStatementSwitch * -1;

            for(int a = 0; a<2; a++){
                CoordinateChange[a] = (float)((float)(endAccel[a] - initAccel[a]))*0.6f;
            }
            CoordinateChange[2] = (float)(endAccel[2] - initAccel[2]) * 0.8f;

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

        Log.d(TAG, "onAccuracyChanged - accuracy: " + accuracy);
        Log.d(TAG, "onAccuracyChanged - accuracy: " + accuracy);
    }

    private void registerSensors(){
        SensorManager mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        mSensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    private void unregister(){
        SensorManager mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));

        mSensorManager.unregisterListener(this, gyroSensor);
        mSensorManager.unregisterListener(this, accelSensor);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(messageEvent.getPath().equals("/path/message")){
            getFiveCoordinates();
        } else {
            Log.w(TAG, messageEvent.getPath());
        }
    }
}