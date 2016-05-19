package com.parrot.bebopdronepilotingnewapi;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;
import com.parrot.arsdk.arsal.ARSALPrint;
import com.parrot.arsdk.arsal.ARSAL_PRINT_LEVEL_ENUM;
import com.parrot.bebopdronepiloting.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;

public class MainActivity extends AppCompatActivity implements
        ARDiscoveryServicesDevicesListUpdatedReceiverDelegate,
        SensorEventListener,
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    private static String TAG = "TAGM";

    static
    {
        try
        {
            System.loadLibrary("arsal");
            System.loadLibrary("arsal_android");
            System.loadLibrary("arnetworkal");
            System.loadLibrary("arnetworkal_android");
            System.loadLibrary("arnetwork");
            System.loadLibrary("arnetwork_android");
            System.loadLibrary("arcommands");
            System.loadLibrary("arcommands_android");
            System.loadLibrary("json");
            System.loadLibrary("ardiscovery");
            System.loadLibrary("ardiscovery_android");
            System.loadLibrary("arstream");
            System.loadLibrary("arstream_android");
            System.loadLibrary("arcontroller");
            System.loadLibrary("arcontroller_android");

            ARSALPrint.setMinimumLogLevel(ARSAL_PRINT_LEVEL_ENUM.ARSAL_PRINT_INFO);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Oops (LoadLibrary)", e);
        }
    }



    private ListView listView ;
    private List<ARDiscoveryDeviceService> deviceList;
    private String[] deviceNameList;

    private ARDiscoveryService ardiscoveryService;
    private boolean ardiscoveryServiceBound = false;
    private ServiceConnection ardiscoveryServiceConnection;
    public IBinder discoveryServiceBinder;

    private BroadcastReceiver ardiscoveryServicesDevicesListUpdatedReceiver;


    //My Shit

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
            xChange.setText(String.format("pitch: %.3f", cPitch));

            TextView yChange = (TextView) findViewById(R.id.yawid);
            yChange.setText(String.format("yaw: %.3f", yaw));

            TextView zChange = (TextView) findViewById(R.id.rollid);
            zChange.setText(String.format("roll: %.3f", roll));
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
    Double[] differences = {0.0,0.0,0.0};

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

    float[][] cordinateChangeFiveLayerWatchData = {
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

    /*float[][][] tracerMedianValues = {

            {{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f}},
            {{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f}},
            {{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f}},
            {{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f}},
            {{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f},{0.0f, 0.0f, 0.0f}},
    };*/

    float[][][] tracerMedianValues = {
            {{0.066080f,-0.049560f,0.056503f},{-0.011492f,-0.027294f,0.068953f},{0.007901f,-0.028012f,-0.057461f},{-0.002873f,0.047405f,-0.067038f},{0.032322f,-0.017238f,0.055545f}},
            {{-0.022266f,-0.024542f,0.006704f},{0.098402f,1.316571f,0.542048f},{-0.089783f,-1.952232f,-1.268927f},{0.035195f,0.063925f,-0.059377f},{-0.028730f,-0.015802f,0.067995f}},
            {{0.155863f,-1.823664f,0.790087f},{-0.005028f,-0.453941f,-0.011492f},{-0.061770f,-0.079727f,-0.223140f},{-0.0494f,-0.761356f,-0.885854f},{-0.313162f,-4.247074f,-2.654691f}},
            {{-0.045969f,0.064643f,0.005746f},{0.412282f,0.959596f,1.616565f},{0.229843f,0.517148f,2.081998f},{-0.022984f,0.520739f,-0.131202f},{0.096247f,0.613394f,-0.333273f}},
            {{0.034477f,-0.165200f,0.122583f},{0.412282f,0.959596f,1.616565f},{0.229843f,0.517148f,2.081998f},{0.041659f,-0.774284f,0.113007f},{0.101993f,0.637815f,-1.402045f}},
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
            {{0.059875, 0.0598755, -0.053648},{0.059875, 0.05987555, -0.053648}},
            {{-0.002395,-0.0023975, 0.268243},{-0.002395,-0.00239975, 0.268243}},
            {{0.172441, 0.172441, -0.295066},{0.172441, 0.172441, -0.295066}},
            {{-0.055085,-0.055086, 0.074725},{-0.055085, -0.055086, 0.074725}},
            {{-0.522146,-0.522145, -0.019160},{-0.122146,-0.5221485, -0.019160}},
    };

    Double[][][] actionSidetoSideArrayChangedValue = new Double[actionSteadyArray.length][actionSteadyArray[0].length][actionSteadyArray[0][0].length];


    Double[][][] actionTwistArray = {
                {{0.007901, -0.027294, 0.055545},{0.007901, -0.027294, 0.055545}},
                {{-0.022266, -0.015802, 0.006704},{-0.022266, -0.015802, 0.006704}},
                {{-0.049400, -0.761356, -0.223140},{-0.049400, -0.761356, -0.223140}},
                {{0.096247, 0.520739, 0.005746},{0.096247, 0.520739, 0.005746}},
                {{0.101993, 0.517148, 0.122583},{0.101993, 0.517148, 0.122583}},
        };

    /* {{-0.162861, 0.316143, 0.083826},{-0.033530, 0.055085, 0.153281},{-
 {{0.009580, 0.050295, 0.019160},{-0.076641, 0.119751, -0.047900},{0
 {{0.292192, -0.119751, 0.428708},{0.079036, -0.177231, 0.184417},{-
 {{-0.399968, -0.021555, 0.287402},{-0.016765, -0.052690, -0.002395}
 {{0.079036, -0.045505, 0.093406},{-0.014370, -0.141306, 0.098196},{*/

    

    Double[][][] actionTwistArrayChangedValue = new Double[actionSteadyArray.length][actionSteadyArray[0].length][actionSteadyArray[0][0].length];
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
    TextView buttonActiveTextView;

    int switcherForRegisterButton = 1;
    int ifStatementSwitch = 1;
    int microSecondSensorDelay = 5000;
    int RunnableRecorderA = 0;
    int RunnableRecorderB = 0;
    boolean counterForActions = false;

    int switcherRoo = 0;

    boolean cont;

    String currentDate;

    double futureTime = 0;
    double waitTime = 250;
    double differenceFromSeekBarForRecorder = 0.0;
    double differenceFromSeekBarForRecorderMax = 0.0;

    double cPitch = 0;
    double aPitch = 0;
    double pitchChange = 0;
    double yaw = 0;
    double roll = 0;

    public int publicProgressValueOfSeekBarForExtreme;
    public int publicProgressValueOfSeekBarForDifference;

    SeekBar seekBarForExtreme;
    SeekBar seekBarForStationaryDifference;

    OutputStreamWriter outputStreamWriter;

    PutDataMapRequest putDataMapRequest;

    Button registryButton;

    Timer timer;

    Thread recorderThread;
    Thread messageRequestThread;
    Thread allActionRunnableThread;
    Thread getFiveCordinateThread;
    Thread actionThread;

    Intent myService;

    GoogleApiClient mGoogleApiClient;

    int request = 0;

    float[][] fiveLayerChangeService = {
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 0.0f},
    };

    private int requestOnDataChanged = 1;

    public static String EXTRA_DEVICE_SERVICE = "pilotingActivity.extra.device.service";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initBroadcastReceiver();
        initServiceConnection();

        listView = (ListView) findViewById(R.id.list);

        deviceList = new ArrayList<ARDiscoveryDeviceService>();
        deviceNameList = new String[]{};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, deviceNameList);


        // Assign adapter to ListView
        listView.setAdapter(adapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {

                ARDiscoveryDeviceService service = deviceList.get(position);

                Intent intent = new Intent(MainActivity.this, PilotingActivity.class);
                intent.putExtra(PilotingActivity.EXTRA_DEVICE_SERVICE, service);


                startActivity(intent);
            }

        });

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

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
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
    public void onResume()
    {
        super.onResume();

        Log.d(TAG, "onResume ...");

        onServicesDevicesListUpdated();

        registerReceivers();

        initServices();

        mGoogleApiClient.connect();

    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause ...");

        unregisterReceivers();
        closeServices();

        super.onPause();
        unregister();

        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    public void logArrayFromService(View view){
        for (float[] aFiveLayerChangeService : fiveLayerChangeService) {
            Log.w(TAG, Arrays.toString(aFiveLayerChangeService));
        }
    }

    public void calculate(View view){
        for(int a = 0; a<cordinateChangeFiveLayer.length; a++){
            Log.w(TAG, String.format("{%f,%f,%f}, %s", cordinateChangeFiveLayer[a][0], cordinateChangeFiveLayer[a][1], cordinateChangeFiveLayer[a][2], System.getProperty("line.separator")));
        }
    }


    public void calculateNextTrial(View view){


        //Log.w(TAG, String.format("--%f--", tracerMedianValues[0][0][0]));
        for(int row = 0; row<tracerMedianValues.length; row++){
            tracerMedianValues[row][switcherRoo] = cordinateChangeFiveLayer[row];
            //Log.w(TAG, String.format("%d", switcherRoo));
        }

        logDoubleFormatArray(tracerMedianValues, "calculateNextTrial");

        switcherRoo++;

    }

    public double findMedianFromArray(float[] numArray){
        Arrays.sort(numArray);
        if (numArray.length % 2 == 0) {
            return (numArray[numArray.length / 2] + (double) numArray[numArray.length / 2 - 1]) / 2;
        }else {
            return numArray[numArray.length / 2];
        }
    }

    public void findMedianArray(View view){
        float[][][] cordinate = new float[5][3][5];
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

                    } else if(nameOfArray.equals("sideToSide")) {
                        Log.w(TAG, String.format("%s RAN", nameOfArray));

                    }else if(nameOfArray.equals("upDown")){
                        Log.w(TAG, String.format("%s RAN", nameOfArray));

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

    public void getFiveCordinates(){

        Runnable recorder = new Runnable() {
            @Override

            public void run() {

                cordinateChangeFiveLayer = fiveLayerChangeService;

                for (float[] aFiveLayerChangeService : cordinateChangeFiveLayer) {
                    Log.w(TAG, Arrays.toString(aFiveLayerChangeService));
                }

                counterForActions = false;

                actionRun("steady", actionSteadyArray, actionSteadyChangedValue, 0.18, 0.18, 0.18);
                actionRun("upDown", actionTwistArray, actionTwistArrayChangedValue, .4, 1, 2);
                actionRun("sideToSide", actionSidetoSideArray, actionSidetoSideArrayChangedValue, 0.60, 0.60, 1.0);



            }
        };

        getFiveCordinateThread = new Thread(recorder);
        getFiveCordinateThread.start();
    }




    public void actions(View view){
        getFiveCordinates();
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

    public void logDoubleFormatArray(float[][][] array, String name){
        Log.w(TAG, String.format("float[][][] %s = {%s",name, System.getProperty("line.separator")));
        for (float[][] anArray : array)
            Log.w(TAG, String.format("            {{%f%s, %f%s, %f%s},{%f%s, %f%s, %f%s},{%f%s, %f%s, %f%s},{%f%s, %f%s, %f%s}, {%f%s, %f%s, %f%s}},%s",
                    anArray[0][0],
                    "f",
                    anArray[0][1],
                    "f",
                    anArray[0][2],
                    "f",
                    anArray[1][0],
                    "f",
                    anArray[1][1],
                    "f",
                    anArray[1][2],
                    "f",
                    anArray[2][0],
                    "f",
                    anArray[2][1],
                    "f",
                    anArray[2][2],
                    "f",
                    anArray[3][0],
                    "f",
                    anArray[3][1],
                    "f",
                    anArray[3][2],
                    "f",
                    anArray[4][0],
                    "f",
                    anArray[4][1],
                    "f",
                    anArray[4][2],
                    "f",
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

    public void seekBarForDifference(){
        seekBarForStationaryDifference = (SeekBar) findViewById(R.id.seekBarForStationaryDifferenceid);
        seekBarForDifferenceText = (TextView) findViewById(R.id.seekBarForDifferenceTextid);

        seekBarForDifferenceText.setText(String.format("Covered: %d / %d", seekBarForStationaryDifference.getProgress(), seekBarForStationaryDifference.getMax()));

        seekBarForStationaryDifference.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        publicProgressValueOfSeekBarForDifference = progress;
                        differenceFromSeekBarForRecorderMax = progress;
                        differenceFromSeekBarForRecorderMax = differenceFromSeekBarForRecorderMax/50;
                        seekBarForDifferenceText.setText(String.format("Covered: %.2f / %d", differenceFromSeekBarForRecorderMax, seekBar.getMax()));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBarForDifferenceText.setText(String.format("Covered: %.2f / %d", differenceFromSeekBarForRecorderMax, seekBar.getMax()));

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

    public double returnPitch(){
        return -Math.atan2(accelRaw[0], Math.sqrt(accelRaw[1] * accelRaw[1] + accelRaw[2] * accelRaw[2]));
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {

        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            initialCordinate = endCordinate;

            for (int a = 0; a < accelRaw.length; a++) {
                accelRaw[a] = (double) event.values[a];
            }


            pitchChange = cPitch - aPitch;
            Log.w(TAG, String.format("pitch: %f - pitch Change: %f", cPitch, pitchChange));

            if (ifStatementSwitch == 1) {
                System.arraycopy(accelRaw, 0, initAccel, 0, initAccel.length);
                cPitch = returnPitch();
            } else {
                System.arraycopy(accelRaw, 0, endAccel, 0, endAccel.length);
                aPitch = returnPitch();
            }

            ifStatementSwitch = ifStatementSwitch * -1;

            for(int a = 0; a<cordinateChange.length; a++){
                cordinateChange[a] = endAccel[a] - initAccel[a];
            }
            //Log.w(TAG, String.format("%s --- %s = %s", Arrays.toString(initAccel), Arrays.toString(endAccel), Arrays.toString(cordinateChange)));

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

    //Begin Watch Shit

    //End Watch Shit


    //End My Shit

    private void initServices()
    {
        if (discoveryServiceBinder == null)
        {
            Intent i = new Intent(getApplicationContext(), ARDiscoveryService.class);
            getApplicationContext().bindService(i, ardiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        }
        else
        {
            ardiscoveryService = ((ARDiscoveryService.LocalBinder) discoveryServiceBinder).getService();
            ardiscoveryServiceBound = true;

            ardiscoveryService.start();
        }
    }

    private void closeServices()
    {
        Log.d(TAG, "closeServices ...");

        if (ardiscoveryServiceBound)
        {
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    ardiscoveryService.stop();

                    getApplicationContext().unbindService(ardiscoveryServiceConnection);
                    ardiscoveryServiceBound = false;
                    discoveryServiceBinder = null;
                    ardiscoveryService = null;
                }
            }).start();
        }
    }

    private void initBroadcastReceiver()
    {
        ardiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
    }

    private void initServiceConnection()
    {
        ardiscoveryServiceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service)
            {
                discoveryServiceBinder = service;
                ardiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();
                ardiscoveryServiceBound = true;

                ardiscoveryService.start();
            }

            @Override
            public void onServiceDisconnected(ComponentName name)
            {
                ardiscoveryService = null;
                ardiscoveryServiceBound = false;
            }
        };
    }

    private void registerReceivers()
    {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(ardiscoveryServicesDevicesListUpdatedReceiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));

    }

    private void unregisterReceivers()
    {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.unregisterReceiver(ardiscoveryServicesDevicesListUpdatedReceiver);
    }




    @Override
    public void onServicesDevicesListUpdated()
    {
        Log.d(TAG, "onServicesDevicesListUpdated ...");

        List<ARDiscoveryDeviceService> list;

        if (ardiscoveryService != null)
        {
            list = ardiscoveryService.getDeviceServicesArray();

            deviceList = new ArrayList<ARDiscoveryDeviceService> ();
            List<String> deviceNames = new ArrayList<String>();

            if(list != null)
            {
                for (ARDiscoveryDeviceService service : list)
                {
                    Log.e(TAG, "service :  "+ service + " name = " + service.getName());
                    ARDISCOVERY_PRODUCT_ENUM product = ARDiscoveryService.getProductFromProductID(service.getProductID());
                    Log.e(TAG, "product :  "+ product);
                    // only display Bebop drones
                    if (ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE.equals(product))
                    {
                        deviceList.add(service);
                        deviceNames.add(service.getName());
                    }
                }
            }

            deviceNameList = deviceNames.toArray(new String[deviceNames.size()]);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, deviceNameList);

            // Assign adapter to ListView
            listView.setAdapter(adapter);
        }
    }



}
