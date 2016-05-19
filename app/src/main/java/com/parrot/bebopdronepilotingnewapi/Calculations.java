package com.parrot.bebopdronepilotingnewapi;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity; //if this is an error - go to modules and add dependency for v7 appcompact
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.parrot.bebopdronepiloting.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;


public class Calculations extends AppCompatActivity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{


    Handler accelValueHandlerTotal = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView xAccelValue = (TextView) findViewById(R.id.xAccelValueid);
            xAccelValue.setText(String.format("XAccelMod: %.3f", Accel[0]));

            TextView yAccelValue = (TextView) findViewById(R.id.yAccelValueid);
            yAccelValue.setText(String.format("YAccelMod: %.3f", Accel[1]));

            TextView zAccelValue = (TextView) findViewById(R.id.zAccelValueid);
            zAccelValue.setText(String.format("ZAccelMod: %.3f", Accel[2]));
        }
    };

    Handler AccelValueHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            accelValueX = (TextView) findViewById(R.id.AccelValueXid);
            accelValueY = (TextView) findViewById(R.id.AccelValueYid);
            accelValueZ = (TextView) findViewById(R.id.AccelValueZid);

            accelValueX.setText(String.format("AccelValueX %.3f %.3f %.3f", initAccel[0], endAccel[0], cordinateChange[0]));
            accelValueY.setText(String.format("AccelValueY %.3f %.3f %.3f", initAccel[1], endAccel[1], cordinateChange[1]));
            accelValueZ.setText(String.format("AccelValueZ %.3f %.3f %.3f", initAccel[2], endAccel[2], cordinateChange[2]));
        }
    };

    Handler initialCoordinateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            initialCordinateValues = (TextView) findViewById(R.id.initialCordinateValuesid);
            initialCordinateValues.setText(String.format("C1: %.3f %.3f %.3f", initialCordinate[0], initialCordinate[1], initialCordinate[2]));
        }
    };

    Handler runnableHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            buttonActiveTextView = (TextView) findViewById(R.id.buttonActiveTextViewid);
            buttonActiveTextView.setText(String.format("%d - %d - %f",RunnableRecorderA, RunnableRecorderB, cordinateChange[0]));


        }
    };

    Handler endCordinateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            endCordinateValues = (TextView) findViewById(R.id.endCordinateValuesid);
            endCordinateValues.setText(String.format("C2: %.3f %.3f %.3f", endCordinate[0], endCordinate[1], endCordinate[2]));
        }
    };


    Handler distanceHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            xDistance = (TextView) findViewById(R.id.xDistanceid);
            xDistance.setText(String.format("XDistance:%f", cordinateChange[0]));

            yDistance = (TextView) findViewById(R.id.yDistanceid);
            yDistance.setText(String.format("YDistance:%f", cordinateChange[1]));

            zDistance = (TextView) findViewById(R.id.zDistanceid);
            zDistance.setText(String.format("ZDistance:%f", cordinateChange[2]));
        }
    };

    Handler rawAccelHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView xChange = (TextView) findViewById(R.id.pitchid);
            xChange.setText(String.format("XRawAccel: %.3f", accelRaw[0]));

            TextView yChange = (TextView) findViewById(R.id.yawid);
            yChange.setText(String.format("YRawAccel: %.3f", accelRaw[1]));

            TextView zChange = (TextView) findViewById(R.id.rollid);
            zChange.setText(String.format("ZRawAccel: %.3f", accelRaw[2]));
        }
    };


    SensorManager smAccel;
    SensorManager smOrientation;

    Sensor orientationSensor;
    Sensor accelSensor;

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

    Double[][] cordinateChangeFiveLayer = {
            {0.0, 0.0, 0.0},
            {0.0, 0.0, 0.0},
            {0.0, 0.0, 0.0},
            {0.0, 0.0, 0.0},
            {0.0, 0.0, 0.0},
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

    Double[][][] tracerMedianValues = { //test SidetoSide
            {{-0.162861, 0.316143, 0.083826},{-0.033530, 0.055085, 0.153281},{-1.017883, 0.091011, -0.287402},{0.649050, 0.622705, -0.239502},{0.699346, -0.129331, 0.107776}},
            {{0.009580, 0.050295, 0.019160},{-0.076641, 0.119751, -0.047900},{0.150887, -0.007185, 0.095800},{-0.598755, 0.057480, -0.390388},{0.196392, -0.148491, 0.102985}},
            {{0.292192, -0.119751, 0.428708},{0.079036, -0.177231, 0.184417},{-0.658630, 0.146096, -0.364043},{-0.416733, 0.146096, -0.040715},{-0.411943, 0.079036, -0.332909}},
            {{-0.399968, -0.021555, 0.287402},{-0.016765, -0.052690, -0.002395},{0.000000, -0.014370, -0.0814},{0.486189, -0.021555, -0.052691},{0.002395, -0.028740, -0.028740}},
            {{0.079036, -0.045505, 0.093406},{-0.014370, -0.141306, 0.098196},{0.004790, 0.019160, 0.002396},{0.071851, 0.000000, -0.263453},{-0.021555, 0.050295, 0.102985}}
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

    String[] arrayNames = {"Steady", "SideToSide"};

    TextView ActionText;
    TextView seekBarForExtremeText;
    TextView seekBarForDifferenceText;
    TextView accelValueX;
    TextView accelValueY;
    TextView accelValueZ;
    TextView xDistance;
    TextView yDistance;
    TextView zDistance;
    TextView initialCordinateValues;
    TextView endCordinateValues;
    public TextView buttonActiveTextView;

    int switcherForRegisterButton = 1;
    int ifStatementSwitch = 1;
    int microSecondSensorDelay = 5000;
    int RunnableRecorderA = 0;
    int RunnableRecorderB = 0;
    int CordinateFiveRunnableRecorderA = 0;
    int CordinateFiveRunnableRecorderB = 0;

    int switcherRoo = 0;

    boolean cont;

    String currentDate;

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

    private static final String TAG = "bepop";
    public static String EXTRA_DEVICE_SERVICE = "pilotingActivity.extra.device.service";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        smAccel = (SensorManager) getSystemService(SENSOR_SERVICE);
        smOrientation = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelSensor = smAccel.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        registryButton = (Button) findViewById(R.id.Registryid);
        registryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (switcherForRegisterButton == 1) {
                    register();
                    buttonActiveTextView = (TextView) findViewById(R.id.buttonActiveTextViewid);
                    buttonActiveTextView.setText("Active for Register");
                } else {
                    unregister();
                    buttonActiveTextView = (TextView) findViewById(R.id.buttonActiveTextViewid);
                    buttonActiveTextView.setText("Not Active for Register");
                }
                switcherForRegisterButton = switcherForRegisterButton * -1;
            }
        });

        timer = new Timer();

        seekBarForExtreme();
        seekBarForDifference();

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

                    runnableHandler.sendEmptyMessage(0);
                }

                RunnableRecorderA++;

            }
            RunnableRecorderA = 0;
            RunnableRecorderB = 0;

        }
    };

    public void calculate(View view){
        recorderThread = new Thread(recorder);
        recorderThread.start();
    }


    public void calculateNextTrial(View view){


        //Log.w(TAG, String.format("--%f--", tracerMedianValues[0][0][0]));
        for(int row = 0; row<tracerMedianValues.length; row++){
            tracerMedianValues[row][switcherRoo] = trackerOne[row][0];
            //Log.w(TAG, String.format("%d", switcherRoo));
        }

        trackerOne = test;

        logDoubleFormatArray(tracerMedianValues, "calculateNextTrial");

        switcherRoo++;

    }

    public double findMedianFromArray(Double[] numArray){
        Arrays.sort(numArray);
        if (numArray.length % 2 == 0) {
            return (numArray[numArray.length / 2] + (double) numArray[numArray.length / 2 - 1]) / 2;
        }else {
            return numArray[numArray.length / 2];
        }
    }

    public void findMedianArray(View view){
        Double[][][] cordinate = new Double[5][3][5];
        for(int a = 0; a<tracerMedianValues.length; a++){
            for(int b = 0; b<tracerMedianValues[0].length; b++){
                cordinate[a][0][b] = tracerMedianValues[a][b][0];
                cordinate[a][1][b] = tracerMedianValues[a][b][1];
                cordinate[a][2][b] = tracerMedianValues[a][b][2];
            }
        }

        for(int a = 0; a<tracerComplete.length; a++){
            for(int b=0;b<3;b++){
                tracerComplete[a][0][b]=findMedianFromArray(cordinate[a][b]);
                tracerComplete[a][1][b]=findMedianFromArray(cordinate[a][b]);
            }
        }
        logSmallDoubleFormatArray(tracerComplete, "tracerComplete");
    }

    public void closeFileButton(View view){
        closeFile();
    }

    public void write(){

        writeFile(String.format("Double[][][] trackerOneTia = {%s", System.getProperty("line.separator")));
        for (Double[][] aTrackerOne : trackerOne) {
            writeFile(String.format("            {{%f, %f, %f},{%f, %f, %f}},%s",
                    aTrackerOne[0][0],
                    aTrackerOne[0][1],
                    aTrackerOne[0][2],
                    aTrackerOne[1][0],
                    aTrackerOne[1][1],
                    aTrackerOne[1][2],
                    System.getProperty("line.separator")
            ));
        }
        writeFile("    };");
    }

    public void writeMedian(View view){

        logSmallDoubleFormatArray(trackerOne, "trackerOneTia");

        Log.w(TAG, String.format("%s%s%s", System.getProperty("line.separator"), System.getProperty("line.separator"), System.getProperty("line.separator")));

        logDoubleFormatArray(tracerMedianValues, "tracerMedianValues");

        Log.w(TAG, String.format("%s%s%s", System.getProperty("line.separator"), System.getProperty("line.separator"), System.getProperty("line.separator")));

        logSmallDoubleFormatArray(trackerOne, "trackerComplete");

    }

    public boolean determineOne(Double[][] action, Double[] cordinateChangeA, String name){
        if(action[0][0] < cordinateChangeA[0] && action[1][0] > cordinateChangeA[0]){
            if(action[0][1] < cordinateChangeA[1] && action[1][1] > cordinateChangeA[1]){
                if(action[0][2] < cordinateChangeA[2] && action[1][2] > cordinateChangeA[2]){
                    Log.w(TAG, String.format("%s ---- %f < %f < %f ------- %f - SENDTRUE", name, action[0][2], cordinateChangeA[2], action[1][2], differenceFromSeekBarForRecorder));
                    return true;
                } else {
                    Log.w(TAG, String.format("%s ---- %f < %f < %f ------- %f - SendFALSE", name, action[0][2], cordinateChangeA[2], action[1][2], differenceFromSeekBarForRecorder));
                    return false;
                }
            } else {
                Log.w(TAG, String.format("%s ---- %f < %f < %f ------- %f - sf", name, action[0][0], cordinateChangeA[0], action[1][0], differenceFromSeekBarForRecorder));
                return false;
            }
        } else {
            Log.w(TAG, String.format("%s ---- %f < %f < %f ------- %f - F", name, action[0][0], cordinateChangeA[0], action[1][0], differenceFromSeekBarForRecorder));
            return false;
        }
    }

    public void actionRun(final Double[][][] actionArray, final String nameOfArray){
        Runnable allActionRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    boolean actionCont = true;
                    for (int a = 0; a < actionArray.length; a++) {
                        if (determineOne(actionArray[a], cordinateChangeFiveLayer[a], nameOfArray)) {
                            actionCont = true;
                            Log.w(TAG, String.format("%s - %f %f %f -TT", nameOfArray, cordinateChangeFiveLayer[a][0], cordinateChangeFiveLayer[a][1], cordinateChangeFiveLayer[a][2]));
                        } else {
                            actionCont = false;
                            Log.w(TAG, String.format("%s - %f %f %f -FF", nameOfArray, cordinateChangeFiveLayer[a][0], cordinateChangeFiveLayer[a][1], cordinateChangeFiveLayer[a][2]));
                            break;
                        }
                    }

                    for (String arrayName : arrayNames) {
                        if ((arrayName).matches(nameOfArray)) {
                            if (actionCont) {
                                Log.w(TAG, arrayName + " is ALIVE!!!");
                            } else {
                                Log.w(TAG, arrayName + " has died");
                            }
                        }
                    }
                }
            }
        };
        allActionRunnableThread = new Thread(allActionRunnable);
        allActionRunnableThread.start();
    }



    public void getFiveCordinates(){

        Runnable recorder = new Runnable() {
            @Override

            public void run() {

                futureTime = System.currentTimeMillis() + (waitTime*5);
                while(System.currentTimeMillis()<futureTime && CordinateFiveRunnableRecorderA<trackerOne.length) {

                    synchronized (this) {
                        try {
                            wait((long) (waitTime));


                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    //keep this for loop - it syncs the cordinates properly
                    for(CordinateFiveRunnableRecorderB = 0; CordinateFiveRunnableRecorderB<3; CordinateFiveRunnableRecorderB++) {
                        cordinateChangeFiveLayer[CordinateFiveRunnableRecorderA][CordinateFiveRunnableRecorderB] = cordinateChange[CordinateFiveRunnableRecorderB];
                    }

                    CordinateFiveRunnableRecorderA++;

                }
                CordinateFiveRunnableRecorderA = 0;
                CordinateFiveRunnableRecorderB = 0;

            }
        };

        getFiveCordinateThread = new Thread(recorder);
        getFiveCordinateThread.start();
    }

    public void actions(View view){
        getFiveCordinates();

        for (Double[] aCordinateChangeFiveLayer : cordinateChangeFiveLayer) {
            Log.w(TAG, (Arrays.deepToString(aCordinateChangeFiveLayer)));
        }

        steady();
        sideToSide();
    }

    public void steady(){

        for(int row = 0; row<actionSteadyArray.length; row++){
            for(int column = 0; column<actionSteadyArray[0].length; column++){
                if(column == 0){
                    for(int elements = 0; elements<actionSteadyArray[0][0].length; elements++){
                        actionSteadyChangedValue[row][0][elements] = actionSteadyArray[row][0][elements] - differenceFromSeekBarForRecorder;
                    }
                } else if( column == 1){
                    for(int elements = 0; elements<actionSteadyArray[0][0].length; elements++){
                        actionSteadyChangedValue[row][1][elements] = actionSteadyArray[row][1][elements] + differenceFromSeekBarForRecorder;
                    }
                }
            }
        }

        logSmallDoubleFormatArray(actionSteadyChangedValue, "steady");

        actionRun(actionSteadyChangedValue, "Steady");
    }

    public void sideToSide(){

        for(int row = 0; row<actionSidetoSideArray.length; row++){
            for(int column = 0; column<actionSidetoSideArray[0].length; column++){
                if(column == 0){
                    for(int elements = 0; elements<actionSidetoSideArray[0][0].length; elements++){
                        actionSidetoSideArrayChangedValue[row][0][elements] = actionSidetoSideArray[row][0][elements] - differenceFromSeekBarForRecorder;
                    }
                } else if( column == 1){
                    for(int elements = 0; elements<actionSteadyArray[0][0].length; elements++){
                        actionSidetoSideArrayChangedValue[row][1][elements] = actionSidetoSideArray[row][1][elements] + differenceFromSeekBarForRecorder;
                    }
                }

            }
        }

        logSmallDoubleFormatArray(actionSidetoSideArrayChangedValue, "sideToside");

        actionRun(actionSidetoSideArrayChangedValue, "SideToSide");
    }

    public void createFile() {
        try {
            Calendar cal = Calendar.getInstance();
            currentDate = cal.getTime().toString();
            outputStreamWriter = new OutputStreamWriter(openFileOutput(currentDate + ".txt", Context.MODE_PRIVATE));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void writeFile(String data){
        try {
            outputStreamWriter.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void closeFile(){
        try {
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        Log.w(TAG, String.format("Double[][][] %s = {%s",name, System.getProperty("line.separator")));
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




    public void seekBarForExtreme() {
        seekBarForExtreme = (SeekBar) findViewById(R.id.seekBarForExtremeid);
        seekBarForExtremeText = (TextView) findViewById(R.id.seekBarTextid);

        seekBarForExtremeText.setText(String.format("Covered: %d / %d", seekBarForExtreme.getProgress(), seekBarForExtreme.getMax()));


        seekBarForExtreme.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        publicProgressValueOfSeekBarForExtreme = progress;
                        differenceFromSeekBarForRecorder = progress;
                        differenceFromSeekBarForRecorder = differenceFromSeekBarForRecorder/50;
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

    public void seekBarForDifference(){
        seekBarForStationaryDifference = (SeekBar) findViewById(R.id.seekBarForStationaryDifferenceid);
        seekBarForDifferenceText = (TextView) findViewById(R.id.seekBarForDifferenceTextid);

        seekBarForDifferenceText.setText(String.format("Covered: %d / %d", seekBarForStationaryDifference.getProgress(), seekBarForStationaryDifference.getMax()));

        seekBarForStationaryDifference.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        publicProgressValueOfSeekBarForDifference = progress;
                        seekBarForDifferenceText.setText(String.format("Covered: %d / %d", publicProgressValueOfSeekBarForDifference, seekBar.getMax()));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBarForDifferenceText.setText(String.format("Covered: %d / %d", publicProgressValueOfSeekBarForDifference, seekBar.getMax()));

                    }
                }
        );
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
//
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

            for(int a = 0; a<cordinateChange.length; a++){
                cordinateChange[a] = endAccel[a] - initAccel[a];
            }

            cordinateChange[2] = cordinateChange[2] * 0.8;

            endCordinate = cordinateDetect(initialCordinate,accelRaw,(microSecondSensorDelay/(Math.pow(10,6))));

            //timer.scheduleAtFixedRate(new Delay(), 0, 1000);

            //writeFile(String.format("%f,%f,%f%s", cordinateChange[0], cordinateChange[1], cordinateChange[2], System.getProperty("line.separator")));

            rawAccelHandler.sendEmptyMessage(0);
            accelValueHandlerTotal.sendEmptyMessage(0);
            AccelValueHandler.sendEmptyMessage(0);
            initialCoordinateHandler.sendEmptyMessage(0);
            endCordinateHandler.sendEmptyMessage(0);
            distanceHandler.sendEmptyMessage(0);

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

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
        unregister();

    }


}