package com.parrot.bebopdronepilotingnewapi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.bebopdronepiloting.R;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PilotingActivity extends AppCompatActivity implements ARDeviceControllerListener,
        SensorEventListener,
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    private static String TAG = "parrotTAG";
    public static String EXTRA_DEVICE_SERVICE = "pilotingActivity.extra.device.service";


    public ARDeviceController deviceController;
    public ARDiscoveryDeviceService service;
    public ARDiscoveryDevice device;

    private Button emergencyBt;
    private Button takeoffBt;
    private Button landingBt;

    private Button gazUpBt;
    private Button gazDownBt;
    private Button yawLeftBt;
    private Button yawRightBt;

    private Button forwardBt;
    private Button backBt;
    private Button rollLeftBt;
    private Button rollRightBt;

    private TextView batteryLabel;

    private AlertDialog alertDialog;

    private RelativeLayout view;

    // video vars
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final int VIDEO_DEQUEUE_TIMEOUT = 33000;
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 368;
    private SurfaceView sfView;
    private MediaCodec mediaCodec;
    private Lock readyLock;
    private boolean isCodecConfigured = false;
    private ByteBuffer csdBuffer;
    private boolean waitForIFrame = true;
    private ByteBuffer [] buffers;

    //My Shit

    SensorManager smAccel;
    SensorManager smOrientation;

    Sensor orientationSensor;
    Sensor accelSensor;

    float[][] fiveLayerChangeService = {
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
    };

    Double[] differences = {0.0,0.0,0.0};
    Double[] Accel = {0.0, 0.0, 0.0};
    Double[] distance = {0.0, 0.0, 0.0};
    Double[] initialCordinate = {0.0, 0.0, 0.0};
    Double[] endCordinate = {0.0, 0.0, 0.0};
    Double[] initAccel = {0.0, 0.0, 0.0};
    Double[] endAccel = {0.0, 0.0, 0.0};
    Double[] cordinateChange = {0.0, 0.0, 0.0};
    Double[] currentCordinateChangeForRunnable = {0.0, 0.0, 0.0};
    Double[] accelRaw = {0.0, 0.0, 0.0};

    Double[][][] test = {
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}}
    };

    float[][] cordinateChangeFiveLayer = {
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
    };


    Double[][][] trackerOne = {
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}}
    };

    Double[][][] tracerComplete = {
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}}
    };

    float[][][] tracerMedianValues = {

            {{-0.043096f, 0.037110f, -0.000957f},{0.094571f, 0.010774f, -0.170467f},{0.001197f, -0.001197f, -0.106303f},{-0.002394f, -0.056264f, 0.023942f}, {0.037110f, 0.022745f, -0.076615f}},
            {{0.732626f, 0.056264f, -0.946188f},{1.182736f, -0.058658f, -1.720952f},{0.162806f, -0.004788f, -0.755610f},{0.696713f, -0.252588f, -0.727837f}, {0.114922f, 0.162806f, -1.016099f}},
            {{0.573411f, -0.656011f, 1.901954f},{5.238513f, -2.551022f, 7.161536f},{0.167594f, -0.238223f, 0.181002f},{0.726640f, -0.805649f, 2.145205f}, {1.386243f, -1.254562f, 1.730529f}},
            {{-0.152032f, 0.278924f, -0.677080f},{0.392649f, -0.389058f, -1.994849f},{-0.141258f, 0.002394f, 0.312204f},{-0.256180f, 0.026336f, 0.075657f}, {0.245405f, 0.175974f, -1.811932f}},
            {{0.185551f, 0.258574f, -0.290177f},{0.015562f, 0.401029f, -0.454898f},{-0.111330f, 0.123301f, -0.109175f},{-0.068235f, 0.050278f, 0.297839f}, {0.093374f, -0.065840f, -0.304542f}},
    };



    Double[][][] actionSteadyArray = {
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}},
            {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}}
    };
    Double[][][] actionSteadyChangedValue = new Double[actionSteadyArray.length][actionSteadyArray[0].length][actionSteadyArray[0][0].length];

    Double[][][] actionSidetoSideArray = {
            {{-0.033530, 0.091011, 0.083826},{-0.033530, 0.091011, 0.083826}},
            {{0.009580, 0.050295, 0.019160},{0.009580, 0.050295, 0.019160}},
            {{-0.411943, 0.079036, -0.040715},{-0.411943, 0.079036, -0.040715}},
            {{0.000000, -0.021555, -0.028740},{0.000000, -0.021555, -0.028740}},
            {{0.004790, 0.000000, 0.093406},{0.004790, 0.000000, 0.093406}},
    };
    Double[][][] actionSidetoSideArrayChangedValue = new Double[actionSteadyArray.length][actionSteadyArray[0].length][actionSteadyArray[0][0].length];

    Double[][][] actionUpDownArray = {
            {{0.059875, -0.021555, -0.053648},{0.059875, -0.021555, -0.053648}},
            {{-0.002395, -0.011975, 0.268243},{-0.002395, -0.011975, 0.268243}},
            {{0.172441, 0.256267, -0.295066},{0.172441, 0.256267, -0.295066}},
            {{-0.055085, 0.126936, 0.074725},{-0.055085, 0.126936, 0.074725}},
            {{-0.522146, 0.055085, -0.019160},{-0.122146, 0.055085, -0.019160}},
    };
    Double[][][] actionUpDownArrayChangedValue = new Double[actionSteadyArray.length][actionSteadyArray[0].length][actionSteadyArray[0][0].length];

    Double[][][] actionTwistArray = {
            {{0.007901, -0.027294, 0.055545},{0.007901, -0.027294, 0.055545}},
            {{-0.022266, -0.015802, 0.006704},{-0.022266, -0.015802, 0.006704}},
            {{-0.049400, -0.761356, -0.223140},{-0.049400, -0.761356, -0.223140}},
            {{0.096247, 0.520739, 0.005746},{0.096247, 0.520739, 0.005746}},
            {{0.101993, 0.517148, 0.122583},{0.101993, 0.517148, 0.122583}},
    };

    Double[][][] actionTwistArrayChangedValue = new Double[actionSteadyArray.length][actionSteadyArray[0].length][actionSteadyArray[0][0].length];

    TextView seekBarForExtremeText;

    int switcherForRegisterButton = 1;
    int ifStatementSwitch = 1;
    int microSecondSensorDelay = 5000;
    int RunnableRecorderA = 0;
    int RunnableRecorderB = 0;
    int CordinateFiveRunnableRecorderA = 0;
    int CordinateFiveRunnableRecorderB = 0;
    int requestOnDataChanged = 1;

    int switcherRoo = 0;

    boolean cont;
    boolean counterForActions = false;

    String currentDate;
    final String[] actionNamesArray = {"steady", "sideToSide"};

    double futureTime = 0;
    double waitTime = 250;
    double differenceFromSeekBarForRecorder = 0.0;

    public int publicProgressValueOfSeekBarForExtreme;
    public int publicProgressValueOfSeekBarForDifference;

    SeekBar seekBarForExtreme;
    SeekBar seekBarForStationaryDifference;

    OutputStreamWriter outputStreamWriter;

    Button registryButton;

    Timer timer;

    Thread recorderThread;
    Thread allActionRunnableThread;
    Thread getFiveCordinateThread;
    Thread actionThread;

    GoogleApiClient mGoogleApiClient;

    //End My Shit

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piloting);

        initIHM();
        //initVideoVars();

        Intent intent = getIntent();
        service = intent.getParcelableExtra(EXTRA_DEVICE_SERVICE);

        //create the device
        try
        {
            device = new ARDiscoveryDevice();

            ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();

            device.initWifi(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE, netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());
        }
        catch (ARDiscoveryException e)
        {
            e.printStackTrace();
            Log.e(TAG, "Error: " + e.getError());
        }


        if (device != null)
        {
            try
            {
                //create the deviceController
                deviceController = new ARDeviceController (device);
                deviceController.addListener(this);
                //deviceController.addStreamListener(this);
            }
            catch (ARControllerException e)
            {
                e.printStackTrace();
            }
        }

        //My Shit

        smAccel = (SensorManager) getSystemService(SENSOR_SERVICE);
        smOrientation = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelSensor = smAccel.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        registryButton = (Button) findViewById(R.id.registerPilotingid);
        registryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (switcherForRegisterButton == 1) {
                    register();
                    Log.w(TAG, "active");
                } else {
                    unregister();
                    Log.w(TAG, "not active");
                }
                switcherForRegisterButton = switcherForRegisterButton * -1;
            }
        });



        timer = new Timer();

        seekBarForExtreme();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //End My Shit
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //start the deviceController
        if (deviceController != null)
        {
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PilotingActivity.this);

            // set title
            alertDialogBuilder.setTitle("Connecting ...");


            // create alert dialog
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();

            ARCONTROLLER_ERROR_ENUM error = deviceController.start();

            if (error != ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK)
            {
                finish();
            }
        }

        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume ...");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause ...");
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onStop()
    {
        if (deviceController != null)
        {
            deviceController.stop();
        }

        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "ConnectionSuspeneded!!!!");
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(TAG, "ConnectionFailed!!!!");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if (path.equals("/sendWearFive")) {

                    for(int a = 0; a<fiveLayerChangeService.length; a++){
                        fiveLayerChangeService[a] = dataMap.getFloatArray("fiveCoordinateChangeArray" + Integer.toString(a));
                        Log.w(TAG, Arrays.toString(fiveLayerChangeService[a]));
                    }

                    getFiveCordinates();

                    requestOnDataChanged = dataMap.getInt("request");

                    Log.w(TAG, String.format("RAN THAT MOFO"));
                } else {
                    Log.w(TAG, String.format("failFive"));
                    Log.w(TAG, dataEvent.toString());
                }
            } else {
                Log.w(TAG, "DataEvent is the same");
            }
            Log.w(TAG, "Was run");
        }
    }

    public void actionRun(String actionName, Double[][][] actionNameArray, Double[][][] actionNameArrayChangedValues, double xDif, double yDif, double zDif){

        double[] localDif = {xDif, yDif, zDif};

        for(int row = 0; row<actionNameArray.length; row++){
            for(int column = 0; column<actionNameArray[0].length; column++){
                if(column == 0){
                    for(int elements = 0; elements<actionNameArray[0][0].length; elements++){
                        actionNameArrayChangedValues[row][0][elements] = actionNameArray[row][0][elements] - localDif[elements];
                    }
                } else if( column == 1){
                    for(int elements = 0; elements<actionNameArray[0][0].length; elements++){
                        actionNameArrayChangedValues[row][1][elements] = actionNameArray[row][1][elements] + localDif[elements];
                    }
                }

            }

            Log.w(TAG, Arrays.toString(localDif));

        }

        logSmallDoubleFormatArray(actionNameArrayChangedValues, actionName);

        actionDetectFinal(actionNameArrayChangedValues, actionName, localDif);
    }

    public void getFiveCordinates(){

        Runnable recorder = new Runnable() {
            @Override

            public void run() {

                cordinateChangeFiveLayer = fiveLayerChangeService;

                for (float[] aFiveLayerChangeService : cordinateChangeFiveLayer) {
                    Log.w(TAG, Arrays.toString(aFiveLayerChangeService));
                }

                counterForActions = false;

                actionRun("steady", actionSteadyArray, actionSteadyChangedValue, .2, .2, .2);
                actionRun("sideToSide", actionSidetoSideArray, actionSidetoSideArrayChangedValue, 0.60, 1, 4);
                actionRun("twist", actionTwistArray, actionTwistArrayChangedValue, 200, 200, 200);



            }
        };

        getFiveCordinateThread = new Thread(recorder);
        getFiveCordinateThread.start();
    }

    public void actions(View view){

        getFiveCordinates();

    }

    public void actionDetectFinal(final Double[][][] actionArray, final String nameOfArray, double[] differences){

        final double[] differenceF = differences;

        Runnable allActionRunnable = new Runnable() {
            @Override
            public void run() {
                boolean actionCont = true;
                for (int a = 0; a < actionArray.length; a++) {
                    if (determineOne(actionArray[a], cordinateChangeFiveLayer[a], nameOfArray, differenceF)) {
                        actionCont = true;
                        Log.w(TAG, String.format("%s - %f %f %f -TT", nameOfArray, cordinateChangeFiveLayer[a][0], cordinateChangeFiveLayer[a][1], cordinateChangeFiveLayer[a][2]));
                    } else {
                        actionCont = false;
                        Log.w(TAG, String.format("%s - %f %f %f -FF", nameOfArray, cordinateChangeFiveLayer[a][0], cordinateChangeFiveLayer[a][1], cordinateChangeFiveLayer[a][2]));
                        break;
                    }
                }

                if(actionCont && !counterForActions){

                    counterForActions = true;

                    if(nameOfArray.equals("steady")){
                        Log.w(TAG, String.format("%s RAN", nameOfArray));

                        if ((deviceController != null) && (deviceController.getFeatureARDrone3() != null))
                        {
                            //send takeOff
                            ARCONTROLLER_ERROR_ENUM error = deviceController.getFeatureARDrone3().sendPilotingTakeOff();
                        }

                    } else if(nameOfArray.equals("sideToSide")) {
                        Log.w(TAG, String.format("%s RAN", nameOfArray));

                        if ((deviceController != null) && (deviceController.getFeatureARDrone3() != null))
                        {
                            //send landing
                            ARCONTROLLER_ERROR_ENUM error = deviceController.getFeatureARDrone3().sendPilotingLanding();
                        }


                    }else if(nameOfArray.equals("twist")){
                        Log.w(TAG, String.format("%s RAN", nameOfArray));

                        if ((deviceController != null) && (deviceController.getFeatureARDrone3() != null))
                        {

                            ARCONTROLLER_ERROR_ENUM error = deviceController.getFeatureARDrone3().sendPilotingEmergency();
                    }

                    } else {
                        Log.w(TAG, "check name entered");
                    }

                }else {
                    Log.w(TAG, String.format("ACTION FOR %s FAILED", nameOfArray));
                }
            }
        };

        allActionRunnableThread = new Thread(allActionRunnable);
        allActionRunnableThread.start();
    }


    public boolean determineOne(Double[][] action, float[] cordinateChangeA, String name,double[] difference){

        boolean check = true;

        for(int b = 0; b<cordinateChangeA.length; b++){
            if(action[0][b] < cordinateChangeA[b] && action[1][b] > cordinateChangeA[b]){
                Log.w(TAG, String.format("%s ---- %f < %f < %f ------- %f - SENDTRUE", name, action[0][b], cordinateChangeA[b], action[1][b], difference[b]));

            } else if(cordinateChangeA[b] == 0.0f){
                Log.w(TAG, String.format("%s ---- %f < %f < %f ------- %f - SENDTRUE", name, action[0][b], cordinateChangeA[b], action[1][b], difference[b]));

            } else {
                check = false;
                Log.w(TAG, String.format("%s ---- %f < %f < %f ------- %f - SENDFalse", name, action[0][b], cordinateChangeA[b], action[1][b], difference[b]));
                break;
            }
        }

        return check;
    }

    protected Double[] cordinateDetect(Double[] initialCordinate, Double[] AccelV, double timeInSeconds) {
        Double[] finalCordinate = {0.0, 0.0, 0.0};

        for(int a = 0; a<AccelV.length; a++){
            distance[a] = AccelV[a] * timeInSeconds*timeInSeconds;
            finalCordinate[a] = initialCordinate[a] + distance[a];
        }

        return finalCordinate;
    }

    protected Double[] accelCorrection(Double[] rawAccel){
        return rawAccel;
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {

        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            initialCordinate = endCordinate;

            for (int a = 0; a < accelRaw.length; a++) {
                accelRaw[a] = (double) event.values[a];
            }

            Accel = accelCorrection(accelRaw);


            if (ifStatementSwitch == 1) {
                System.arraycopy(accelRaw, 0, initAccel, 0, initAccel.length);
            } else {
                System.arraycopy(accelRaw, 0, endAccel, 0, endAccel.length);
            }

            ifStatementSwitch = ifStatementSwitch * -1;

            for (int a = 0; a < cordinateChange.length; a++) {
                cordinateChange[a] = endAccel[a] - initAccel[a];
            }

            cordinateChange[2] = cordinateChange[2] * 0.8;

            endCordinate = cordinateDetect(initialCordinate, accelRaw, (microSecondSensorDelay / (Math.pow(10, 6))));

            //timer.scheduleAtFixedRate(new Delay(), 0, 1000);

            //writeFile(String.format("%f,%f,%f%s", cordinateChange[0], cordinateChange[1], cordinateChange[2], System.getProperty("line.separator")));

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void register() {
        smAccel.registerListener(this, accelSensor, microSecondSensorDelay);
        smOrientation.registerListener(this, orientationSensor, microSecondSensorDelay);
    }

    protected void unregister() {
        smAccel.unregisterListener(this);
        smOrientation.unregisterListener(this);
    }

    Runnable recorder = new Runnable() {
        @Override

        public void run() {

            futureTime = System.currentTimeMillis() + (waitTime*5);
            while(System.currentTimeMillis()<futureTime && RunnableRecorderA<trackerOne.length) {

                synchronized (this) {
                    try {
                        wait((long) (waitTime));


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                for(RunnableRecorderB = 0; RunnableRecorderB<3; RunnableRecorderB++) {
                    trackerOne[RunnableRecorderA][0][RunnableRecorderB] = cordinateChange[RunnableRecorderB];
                    trackerOne[RunnableRecorderA][1][RunnableRecorderB] = cordinateChange[RunnableRecorderB];

                    Log.w(TAG, String.format("%d - %f - %d", RunnableRecorderA, trackerOne[RunnableRecorderA][0][RunnableRecorderB], RunnableRecorderB));
                }

                RunnableRecorderA++;

            }
            RunnableRecorderA = 0;
            RunnableRecorderB = 0;

        }
    };

    public void seekBarForExtreme() {
        seekBarForExtreme = (SeekBar) findViewById(R.id.seekBarPilotingid);
        seekBarForExtremeText = (TextView) findViewById(R.id.seekBarTextPilotingid);

        seekBarForExtremeText.setText(String.format("Covered: %d / %d", seekBarForExtreme.getProgress(), seekBarForExtreme.getMax()));


        seekBarForExtreme.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        publicProgressValueOfSeekBarForExtreme = progress;
                        differenceFromSeekBarForRecorder = progress;
                        differenceFromSeekBarForRecorder = differenceFromSeekBarForRecorder / 50;
                        seekBarForExtremeText.setText(String.format("Covered: %.2f / %d", differenceFromSeekBarForRecorder, seekBar.getMax()));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBarForExtremeText.setText(String.format("Covered: %.2f / %d", differenceFromSeekBarForRecorder, seekBar.getMax()));
                    }
                }
        );
    }

    public void logDoubleFormatArray(Double[][][] array, String name){
        Log.w(TAG, String.format("Double[][][] %s = {%s", name, System.getProperty("line.separator")));
        for (Double[][] anArray : array)
            Log.w(TAG, String.format("            {{%f, %f, %f},{%f, %f, %f}},{%f, %f, %f},{%f, %f, %f}, {%f, %f, %f}},%s",
                    anArray[0][0],
                    anArray[0][1],
                    anArray[0][2],
                    anArray[1][0],
                    anArray[1][1],
                    anArray[1][2],
                    anArray[2][0],
                    anArray[2][1],
                    anArray[2][2],
                    anArray[3][0],
                    anArray[3][1],
                    anArray[3][2],
                    anArray[4][0],
                    anArray[4][1],
                    anArray[4][2],
                    System.getProperty("line.separator")
            ));
        Log.w(TAG, ("    };"));
    }

    public void logSmallDoubleFormatArray(Double[][][] array, String name){
        Log.w(TAG, String.format("Double[][][] %s = {%s", name, System.getProperty("line.separator")));
        for (Double[][] anArray : array) {
            Log.w(TAG, String.format("            {{%f, %f, %f},{%f, %f, %f}},%s",
                    anArray[0][0],
                    anArray[0][1],
                    anArray[0][2],
                    anArray[1][0],
                    anArray[1][1],
                    anArray[1][2],
                    System.getProperty("line.separator")
            ));
        }
        Log.w(TAG, ("    };"));
    }

    //End My Shit

    private void initIHM ()
    {
        view = (RelativeLayout) findViewById(R.id.piloting_view);

        emergencyBt = (Button) findViewById(R.id.emergencyBt);
        emergencyBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if ((deviceController != null) && (deviceController.getFeatureARDrone3() != null))
                {
                    ARCONTROLLER_ERROR_ENUM error = deviceController.getFeatureARDrone3().sendPilotingEmergency();
                }
            }
        });

        takeoffBt = (Button) findViewById(R.id.takeoffBt);
        takeoffBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if ((deviceController != null) && (deviceController.getFeatureARDrone3() != null))
                {
                    //send takeOff
                    ARCONTROLLER_ERROR_ENUM error = deviceController.getFeatureARDrone3().sendPilotingTakeOff();
                }
            }
        });
        landingBt = (Button) findViewById(R.id.landingBt);
        landingBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if ((deviceController != null) && (deviceController.getFeatureARDrone3() != null))
                {
                    //send landing
                    ARCONTROLLER_ERROR_ENUM error = deviceController.getFeatureARDrone3().sendPilotingLanding();
                }
            }
        });

        gazUpBt = (Button) findViewById(R.id.gazUpBt);
        gazUpBt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDGaz((byte) 50);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDGaz((byte)0);

                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        gazDownBt = (Button) findViewById(R.id.gazDownBt);
        gazDownBt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDGaz((byte)-50);

                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDGaz((byte)0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });
        yawLeftBt = (Button) findViewById(R.id.yawLeftBt);
        yawLeftBt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDYaw((byte)-50);

                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDYaw((byte) 0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });
        yawRightBt = (Button) findViewById(R.id.yawRightBt);
        yawRightBt.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDYaw((byte) 50);

                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDYaw((byte) 0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        forwardBt = (Button) findViewById(R.id.forwardBt);
        forwardBt.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDPitch((byte) 50);
                            deviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 1);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDPitch((byte) 0);
                            deviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });
        backBt = (Button) findViewById(R.id.backBt);
        backBt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDPitch((byte)-50);
                            deviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte)1);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDPitch((byte)0);
                            deviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte)0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });
        rollLeftBt = (Button) findViewById(R.id.rollLeftBt);
        rollLeftBt.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDRoll((byte) -50);
                            deviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 1);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDRoll((byte) 0);
                            deviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });
        rollRightBt = (Button) findViewById(R.id.rollRightBt);
        rollRightBt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDRoll((byte)50);
                            deviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte)1);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureARDrone3().setPilotingPCMDRoll((byte)0);
                            deviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte)0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        batteryLabel = (TextView) findViewById(R.id.batteryLabel);
    }



    private void stopDeviceController()
    {
        if (deviceController != null)
        {
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PilotingActivity.this);

            // set title
            alertDialogBuilder.setTitle("Disconnecting ...");

            // show it
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // create alert dialog
                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();

                    ARCONTROLLER_ERROR_ENUM error = deviceController.stop();

                    if (error != ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                        finish();
                    }
                }
            });
            //alertDialog.show();

        }
    }



    @Override
    public void onBackPressed()
    {
        stopDeviceController();
    }

    public void onUpdateBattery(final int percent)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                batteryLabel.setText(String.format("%d%%", percent));
            }
        });

    }

    @Override
    public void onStateChanged (ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error)
    {
        Log.i(TAG, "onStateChanged ... newState:" + newState+" error: "+ error );

        switch (newState)
        {
            case ARCONTROLLER_DEVICE_STATE_RUNNING:
                //The deviceController is started
                Log.i(TAG, "ARCONTROLLER_DEVICE_STATE_RUNNING ....." );
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //alertDialog.hide();
                        alertDialog.dismiss();
                    }
                });
                deviceController.getFeatureARDrone3().sendMediaStreamingVideoEnable((byte)1);
                break;

            case ARCONTROLLER_DEVICE_STATE_STOPPED:
                //The deviceController is stoped
                Log.i(TAG, "ARCONTROLLER_DEVICE_STATE_STOPPED ....." );

                deviceController.dispose();
                deviceController = null;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        //alertDialog.hide();
                        alertDialog.dismiss();
                        finish();
                    }
                });
                break;

            default:
                break;
        }
    }

    @Override
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary)
    {
        if (elementDictionary != null)
        {
            if (commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED)
            {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);

                if (args != null)
                {
                    Integer batValue = (Integer) args.get("arcontroller_dictionary_key_common_commonstate_batterystatechanged_percent");

                    onUpdateBattery(batValue);
                }
            }
        }
        else
        {
            Log.e(TAG, "elementDictionary is null");
        }
    }



    /*
    @Override
    public void onFrameReceived(ARDeviceController deviceController, ARFrame frame)
    {
        readyLock.lock();

        if ((mediaCodec != null))
        {
            if (!isCodecConfigured && frame.isIFrame())
            {
                csdBuffer = getCSD(frame);
                if (csdBuffer != null)
                {
                    configureMediaCodec();
                }
            }
            if (isCodecConfigured && (!waitForIFrame || frame.isIFrame()))
            {
                waitForIFrame = false;

                // Here we have either a good PFrame, or an IFrame
                int index = -1;

                try
                {
                    index = mediaCodec.dequeueInputBuffer(VIDEO_DEQUEUE_TIMEOUT);
                }
                catch (IllegalStateException e)
                {
                    Log.e(TAG, "Error while dequeue input buffer");
                }
                if (index >= 0)
                {
                    ByteBuffer b = buffers[index];
                    b.clear();
                    b.put(frame.getByteData(), 0, frame.getDataSize());
                    //ByteBufferDumper.dumpBufferStartEnd("PFRAME", b, 10, 4);
                    int flag = 0;
                    if (frame.isIFrame())
                    {
                        //flag = MediaCodec.BUFFER_FLAG_SYNC_FRAME | MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
                    }

                    try
                    {
                        mediaCodec.queueInputBuffer(index, 0, frame.getDataSize(), 0, flag);
                    }
                    catch (IllegalStateException e)
                    {
                        Log.e(TAG, "Error while queue input buffer");
                    }

                }
                else
                {
                    waitForIFrame = true;
                }
            }

            // Try to display previous frame
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outIndex = -1;
            try
            {
                outIndex = mediaCodec.dequeueOutputBuffer(info, 0);

                while (outIndex >= 0)
                {
                    mediaCodec.releaseOutputBuffer(outIndex, true);
                    outIndex = mediaCodec.dequeueOutputBuffer(info, 0);
                }
            }
            catch (IllegalStateException e)
            {
                Log.e(TAG, "Error while dequeue input buffer (outIndex)");
            }
        }


        readyLock.unlock();
    }

    @Override
    public void onFrameTimeout(ARDeviceController deviceController)
    {
        //Log.i(TAG, "onFrameTimeout ..... " );
    }

    //region video
    public void initVideoVars()
    {
        readyLock = new ReentrantLock();
        applySetupVideo();
    }


    private void applySetupVideo()
    {
        String deviceModel = Build.DEVICE;
        Log.d(TAG, "configuring HW video codec for device: [" + deviceModel + "]");
        sfView = new SurfaceView(getApplicationContext());
        sfView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        sfView.getHolder().addCallback(this);

        view.addView(sfView, 0);
    }

    @SuppressLint("NewApi")
    public void reset()
    {
        // This will be run either before or after decoding a frame.
        readyLock.lock();

        view.removeView(sfView);
        sfView = null;

        //releaseMediaCodec();

        readyLock.unlock();
    }

    */



    /*
     * Configure and start media codec
     * @param type
     */

    /*

    @SuppressLint("NewApi")
    private void initMediaCodec(String type)
    {
        try
        {
            mediaCodec = MediaCodec.createDecoderByType(type);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (csdBuffer != null)
        {
            configureMediaCodec();
        }
    }

    @SuppressLint("NewApi")
    private void configureMediaCodec()
    {
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", VIDEO_WIDTH, VIDEO_HEIGHT);
        format.setByteBuffer("csd-0", csdBuffer);

        mediaCodec.configure(format, sfView.getHolder().getSurface(), null, 0);
        mediaCodec.start();

        buffers = mediaCodec.getInputBuffers();

        isCodecConfigured = true;
    }

    @SuppressLint("NewApi")
    private void releaseMediaCodec()
    {
        if ((mediaCodec != null) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN))
        {
            if (isCodecConfigured)
            {
                mediaCodec.stop();
                mediaCodec.release();
            }
            isCodecConfigured = false;
            mediaCodec = null;
        }
    }

    public ByteBuffer getCSD(ARFrame frame)
    {
        int spsSize = -1;
        if (frame.isIFrame())
        {
            byte[] data = frame.getByteData();
            int searchIndex = 0;
            // we'll need to search the "00 00 00 01" pattern to find each header size
            // Search start at index 4 to avoid finding the SPS "00 00 00 01" tag
            for (searchIndex = 4; searchIndex <= frame.getDataSize() - 4; searchIndex ++)
            {
                if (0 == data[searchIndex  ] &&
                        0 == data[searchIndex+1] &&
                        0 == data[searchIndex+2] &&
                        1 == data[searchIndex+3])
                {
                    break;  // PPS header found
                }
            }
            spsSize = searchIndex;

            // Search start at index 4 to avoid finding the PSS "00 00 00 01" tag
            for (searchIndex = spsSize+4; searchIndex <= frame.getDataSize() - 4; searchIndex ++)
            {
                if (0 == data[searchIndex  ] &&
                        0 == data[searchIndex+1] &&
                        0 == data[searchIndex+2] &&
                        1 == data[searchIndex+3])
                {
                    break;  // frame header found
                }
            }
            int csdSize = searchIndex;

            byte[] csdInfo = new byte[csdSize];
            System.arraycopy(data, 0, csdInfo, 0, csdSize);
            return ByteBuffer.wrap(csdInfo);
        }
        return null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        readyLock.lock();
        initMediaCodec(VIDEO_MIME_TYPE);
        readyLock.unlock();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    }


    @SuppressLint("NewApi")
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        readyLock.lock();
        releaseMediaCodec();
        readyLock.unlock();
    }

    */

    //endregion video
}

