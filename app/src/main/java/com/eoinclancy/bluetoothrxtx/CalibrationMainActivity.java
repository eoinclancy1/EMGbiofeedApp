package com.eoinclancy.bluetoothrxtx;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class CalibrationMainActivity extends AppCompatActivity {

    Button btnOn, btnOff;
    TextView jointAngleText,jointAngleProgress;
    Handler bluetoothIn;

    final int handlerState = 0;                          //used to identify handler message
    private BluetoothAdapter btAdapter = null;           // Used to discover BT devices
    private BluetoothSocket btSocket = null;             // Socket must be open to communicate via BT
    private StringBuilder recDataString = new StringBuilder();  //Used to store the raw Arduino data as it arrives
    private String displayDataRec;                        // Can be used to print raw data to console as it arrives
    private ConnectedThread mConnectedThread;             //Thread to handle BT communication


    //Additions for circular progress bar
    CircularProgressBar circularProgressBar;            //Circular Progress bar displays the joint angle
    TextView angle;                                     //Shows the angle in text format
    ProgressBar vProgBar1;                              //Shows the EMG data
    float currentHighScore = 0;                         //Stores user high score read from memory
    float progress = 0;                                 //
    float setSquatAngle = 90;                           //Default 90 deg
    int setNumSquats = 10;                              //Default 10 squats
    int setLegForJointAngle = 1;                        //Right leg by default;
    float jointAngleVal;

    //Buttons to select which EMG sensor to calibrate
    Button vmoLBtn;
    Button vmoRBtn;
    Button calfLBtn;
    Button calfRBtn;

    //Booleans to determine which EMG sensor is currently being calibrated
    boolean vmoLEnabled = true;         // Calibrate the vmoL EMG Sensor first
    boolean vmoREnabled = false;        // Other EMG sensors not displayed on screen - disabled for now
    boolean calfLEnabled = false;
    boolean calfREnabled = false;

    TextView muscleName;                // Displays the muscle for which the EMG sensor is currently being calibrated
    int[] setupValues = {0, 0, 0};      // Initialised during the onCreate method, where setup values from previous activity are assigned

    Button calibrationCompleteButton;   //Button selected when calibration is complete

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_calibration);

        Bundle extras = getIntent().getExtras();

        //Read setup data from the Intent
        if (extras != null){
            setupValues = extras.getIntArray("setupDetails");
            setSquatAngle = setupValues[0];          //has squatting angle
            setNumSquats = setupValues[1];           //has number of squats
            setLegForJointAngle = setupValues[2];    //setupValues[2] has 0 for left leg, 1 for right leg

        }

        //Get the user profile highscore from SQLite DB
        currentHighScore = getHighscore();
        System.out.println("Current Highscore is " + currentHighScore);

        //Additions for the circular progress bar material
        circularProgressBar = (CircularProgressBar)findViewById(R.id.circularPbar);
        circularProgressBar.setProgressBarWidth(getResources().getDimension(R.dimen.activity_horizontal_margin));
        circularProgressBar.setBackgroundProgressBarWidth(getResources().getDimension(R.dimen.default_background_stroke_width));
        int animationDuration = 2; // 2500ms = 2,5s
        circularProgressBar.setProgressWithAnimation(2, animationDuration); // Default duration = 1500ms

        //Link the buttons, textViews etc to respective views
        vProgBar1 = (ProgressBar)findViewById(R.id.vert_progress_bar1);

        angle = (TextView)findViewById(R.id.jointAngle);
        jointAngleText = (TextView) findViewById(R.id.JointAngleText);
        jointAngleProgress = (TextView) findViewById(R.id.JointAngleProgress);

        calibrationCompleteButton = (Button) findViewById(R.id.calibrationCompleteButton);
        vmoLBtn = (Button) findViewById(R.id.VMOLbutton);
        vmoRBtn = (Button) findViewById(R.id.VMORbutton);
        calfLBtn = (Button) findViewById(R.id.CalfLbutton);
        calfRBtn = (Button) findViewById(R.id.CalfRbutton);

        vmoLBtn.setBackgroundColor(getResources().getColor(R.color.buttonSelect));          //VMO Left Button enabled first

        muscleName = (TextView) findViewById(R.id.muscleNameTextBox);

        try {
            showStartCalibrationNotification();        //Display alert to user showing that calibration is required + instructions
        }catch(Exception e){}


//Handler executed on receiving new data from BT device
        bluetoothIn =  new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                          //if message is what we want
                    String readMessage = (String) msg.obj;                               // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                   //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line


                    if (recDataString.length() > 32 && endOfLineIndex!= -1) {           //32 is min size, - 8 is num of parse characters - this allows app to function properly (Was 20 for HC-05_in_clean for 2 with 1 myoware, and 6 parse char)
                        //String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        //displayDataRec = "Data Received = " + dataInPrint;

                        if (recDataString.charAt(0) == '#')                             //if it starts with # we know it is what we are looking for
                        {
                            System.out.println("\t\t\t Data to be displayed: "+recDataString);
                            String[] recDataArray = recDataString.toString().split("\\+");  //Data arrives as "#LeftLegDeg+RightLegDeg+Emg1+Emg2+Emg3+Emg4~"
                                                                                            //e.g "#101.20+102.30+2.55+1.67+3.23+1.98+~"

                            String sensorAngleL = recDataArray[0].replace("#","");                      //Left Leg Angle
                            sensorAngleL = sensorAngleL.substring(0,sensorAngleL.length()-1);           //scaling the angle value to 1 decimal place
                            String sensorAngleR = recDataArray[1];                                      //Right leg Angle
                            sensorAngleR = sensorAngleR.substring(0,sensorAngleR.length()-1);           //scaling the angle value to 1 decimal place
                            String EMGsensor1 = recDataArray[2];                            //stores EMG 1 value
                            String EMGsensor2 = recDataArray[3];                            //stores EMG 2 value
                            String EMGsensor3 = recDataArray[4];                            //stores EMG 3 value
                            String EMGsensor4 = recDataArray[5];                            //stores EMG 3 value

                            // Checking which joint angle the user wants displayed and calculating the joint angle
                            if  (setLegForJointAngle == 0){
                                jointAngleVal = Float.parseFloat(sensorAngleL);
                            }
                            else{
                                jointAngleVal = Float.parseFloat(sensorAngleR);
                            }
                            if (jointAngleVal < 0) jointAngleVal = 0;
                            jointAngleVal =  Math.abs(jointAngleVal);

                            String.format("%.1f", jointAngleVal);         //Convert to one decimal place

                            angle.setText(jointAngleVal+"°");             //Display the joint angle

                            progress = (jointAngleVal/setSquatAngle)*100; // joint angle mapped to 0-100 scale

                            if ( jointAngleVal > setSquatAngle)           // Covers case where user squats above set limit
                            { progress = 100; }                         // Sets progress bar to max value

                            circularProgressBar.setProgress(progress);    //Set progress of circular buffer with joint angle

                            //Display the selected muscle EMG information on the progress bar
                                if (vmoLEnabled){
                                    float vmoL = Float.parseFloat(EMGsensor1);                     //Get first EMG Value
                                    vProgBar1.setProgress((int)(vmoL * 20.0));                   //Set progress of EMG progress bar
                                }
                                else if(vmoREnabled){
                                    float vmoR = Float.parseFloat(EMGsensor2);                     //Get second EMG Value
                                    vProgBar1.setProgress((int)(vmoR * 20.0));                   //Set progress of EMG progress bar
                                }
                                else if(calfLEnabled){
                                    float calfL = Float.parseFloat(EMGsensor3);                     //Get third EMG Value
                                    vProgBar1.setProgress((int)(calfL * 20.0));                   //Set progress of EMG progress bar
                                }
                                else {  //calfREnabled is true
                                    float calfR = Float.parseFloat(EMGsensor4);                     //Get fourth EMG Value
                                    vProgBar1.setProgress((int)(calfR * 20.0));                   //Set progress of EMG progress bar
                                }

                        }
                        recDataString.delete(0, recDataString.length());                            //clear all string data

                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        // Set up onClick listeners for buttons to for each of the muscle buttons
        vmoLBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vmoLEnabled = true;         // Enable Left Vmo EMG info - disable others
                vmoREnabled = false;
                calfLEnabled = false;
                calfREnabled = false;
                muscleName.setText(R.string.vmoL);
                vmoLBtn.setBackgroundColor(getResources().getColor(R.color.buttonSelect));          //Button colour darkens to show active
                vmoRBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));          //Button colour lightens to show inactive
                calfLBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));
                calfRBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));
            }
        });

        vmoRBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vmoLEnabled = false;
                vmoREnabled = true;        // Enable Right Vmo EMG info - disable others
                calfLEnabled = false;
                calfREnabled = false;
                muscleName.setText(R.string.vmoR);
                vmoRBtn.setBackgroundColor(getResources().getColor(R.color.buttonSelect));          //Button colour darkens to show active
                vmoLBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));          //Button colour lightens to show inactive
                calfLBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));
                calfRBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));
            }
        });

        calfLBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vmoLEnabled = false;
                vmoREnabled = false;
                calfLEnabled = true;        // Enable Left Calf EMG info - disable others
                calfREnabled = false;
                muscleName.setText(R.string.calfL);
                calfLBtn.setBackgroundColor(getResources().getColor(R.color.buttonSelect));          //Button colour darkens to show active
                vmoLBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));          //Button colour lightens to show inactive
                vmoRBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));
                calfRBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));
            }
        });

        calfRBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vmoLEnabled = false;
                vmoREnabled = false;
                calfLEnabled = false;
                calfREnabled = true;        // Enable Right Calf EMG info - disable others
                muscleName.setText(R.string.calfR);
                calfRBtn.setBackgroundColor(getResources().getColor(R.color.buttonSelect));          //Button colour darkens to show active
                vmoLBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));          //Button colour lightens to show inactive
                vmoRBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));
                calfLBtn.setBackgroundColor(getResources().getColor(R.color.buttonUnselect));

            }
        });
    }


    //Get the user's highscore from the DB entry
    private float getHighscore(){
        SharedPreferences sharedPref = this.getSharedPreferences("FYP_Username", Context.MODE_PRIVATE); //SharedPreferences used for passing data between activities
        String id = sharedPref.getString("Username",null);                                              //This sharedPref stores the current user's username
        float result = 0.0f;                                                                            //Default value
        if (id != null){
            DatabaseHelper helper = new DatabaseHelper(this);                                           //Reference to serving DB
            String r = helper.getHighscore(id);                                                         //Get the highscore for the desired user based on their username
            result = Float.parseFloat(r);                                                               //Convert highscore to type float
        }
        return result;
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                    //System.out.println(readMessage);      //Debug ensure still gets info when screen freezes - data keeps being recorded here ok
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure in Calibration Activity", Toast.LENGTH_LONG).show();
                finish();

            }
        }
        //Can be used to close i/p and o/p streams before socket is closed
        public void closeStreams(){
            try {
                if(mmInStream != null){
                    mmInStream.close();
                }
                if(mmOutStream != null){
                    mmOutStream.close();
                }

            }
            catch (IOException e){

            }
        }
    }

    //Displays pop-up to user indicating the instructions for calibration
    private void showStartCalibrationNotification() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle("EMG Calibration Required").setMessage(
                "You must complete the calibration procedure to begin!\n\n\t " +
                "Please perform maximum voluntary contractions (MVC) for \n\t each of the muscles of the muscles below.\n\n\t" +
                "1. Select the correct muscle button to display the \n\t corresponding EMG data.\n\t" +
                "2. Perform a maximum voluntary contraction for the selected \n\t muscle. Adjust the EMG gain accordingly to ensure the MVC " +
                        "\n\t lies in the MVC band.\n\t" +
                "3. Repeat for all monitored muscles.\n\t" +
                "4. Select 'Calibration Complete' when all EMG sensors have \n\t been calibrated.");
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                //exitLauncher();
                dialog.dismiss();                                       //Close the alert box if OK is selected
            }
        });
        final AlertDialog alert = dialog.create();
        alert.show();

        // Hide after some seconds
        final Handler handler  = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (alert.isShowing()) {
                    alert.dismiss();
                }
            }
        };

        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, 25000);
    }


    //Handles the clicking of the 'Calibration Complete' button
    public void onButtonClick(View v) {
        System.out.println("ButtonPressed");
        if (v.getId() == R.id.calibrationCompleteButton) {       //Checking if the ID of the button selected is the finish Button
            try
            {
                mConnectedThread.closeStreams();
                mConnectedThread = null;
                if (btSocket != null) {
                    btSocket.close();       //Don't leave Bluetooth sockets open when leaving activity
                    System.out.println("Close command run");
                }
                while(btSocket.isConnected()){}
                System.out.println(btSocket.isConnected() + " Socket is now closed");
            } catch (IOException e2) {
                System.out.println("Failed to close"); //insert code to deal with this
            }
            System.out.println("******************ButtonPressed");
            System.out.println("******************Waiting for realease of BTSocket");
            Intent i = new Intent(CalibrationMainActivity.this, MainActivity.class);        //Goes to the list Exercises activity
            int[] setupDetails = {setupValues[0], setupValues[1], setupValues[2]};         // setSquatAngle, setNumSquats, setLegForJointAngle
                                                                                            // Forwarding the setup details to the next class
            i.putExtra("setupDetails", setupDetails);    //Putting the results, available in the class the intent points to,
                                                    // see http://stackoverflow.com/questions/24436682/android-why-use-intent-putextra-method
            i.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, address);
            i.putExtra("setupDetails", setupValues);
            startActivity(i);

        }
    }

    //If back in bottom bar, direct user back to exercise setup screen
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, CalibrationScreen.class); //second is classname.class
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}