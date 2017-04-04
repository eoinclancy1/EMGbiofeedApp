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
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView txtArduino, txtString, txtStringLength, jointAngleText,jointAngleProgress, sensorView1, sensorView2, sensorView3;
    Handler bluetoothIn;

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;         //Used to discover BT devices
    private BluetoothSocket btSocket = null;           //Used to connect to BT device
    private StringBuilder recDataString = new StringBuilder();  //Used to store the raw Arduino data as it arrives
    private String displayDataRec;
    private ConnectedThread mConnectedThread;           //Thread to handle BT communication


    //Additions for circular progress bar
    CircularProgressBar circularProgressBar;        //Displays joint angle data on circular progress bar
    TextView angle;                                 //Displays Current joint angle in text format
    TextView currBestVal;                           //Displays highest joint angle in this session
    TextView highscoreVal;                          //Displays user profile highest joint angle
    TextView numSquats;                             //Displays number of squats completed

    //Display EMG Data
    ProgressBar vProgBar1;
    ProgressBar vProgBar2;
    ProgressBar vProgBar3;
    ProgressBar vProgBar4;

    float currentBestValue = 0;         //Highest joint angle in this session
    float currentHighScore = 0;         //User profile highest joint angle
    float standAngle = 15;              //User must reach this joint angle to confirm a squat completion, used to be 10 deg
    float progress = 0;                 //Value for setting circular progress bar
    float jointAngleDifference = 0;     //Difference between measured joint angles

    float emg1MaxVal = 0;               //
    float emg2MaxVal = 0;
    float emg3MaxVal = 0;
    float emg4MaxVal = 0;

    float setSquatAngle = 90;           //Default 90 deg
    int setNumSquats = 10;              //Default 10 squats
    int setLegForJointAngle = 1;        //Right leg by default;
    boolean metDesiredAngle = false;    //Value true when user squats deeper than desired angle, otherwise false
    int countSquats = 0;                //Count number of squats performed
    float jointAngleVal;                //Stores the joint angle wanted by the user
    boolean setGIFStatus = false;       //Initially false as no squats performed, set to true when at least one squat is done

    //Memory accessing and saving variables
    final StringBuilder build = new StringBuilder();  //Stores the data received to be written to memory
    MyFile myFile = new MyFile();                   //Used to create the file and to write to it
    Date date = new Date();                         //Used to assign a timestamp to the data received
    int numberOfSamplesReceived = 0;                //Tracks the number of samples received from the Arduino, when = 20,
                                                            // then flush data to memory

    Button statusButton;


    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myFile.createFile("EMG Data.txt");						//Creating a txt file at 'root//EMG Data.txt' to store the streamed data


        //Get the setup information from the Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            int[] setupValues = extras.getIntArray("setupDetails");
            setSquatAngle = setupValues[0];          //has squatting angle
            setNumSquats = setupValues[1];           //has number of squats
            setLegForJointAngle = setupValues[2];    //setupValues[2] has 0 for left leg, 1 for right leg

        }


        updateGIFStatus(0);                           //Setting the GIF to be displayed on results page, 0 corresponds to fail, no squats performed
        currentHighScore = getHighscore();              // Read user profile high score from SQLite DB
        System.out.println("Current Highscore is " + currentHighScore);

        //Setup Circular progress bar parameters
        circularProgressBar = (CircularProgressBar)findViewById(R.id.circularPbar);
        circularProgressBar.setProgressBarWidth(getResources().getDimension(R.dimen.activity_horizontal_margin));
        circularProgressBar.setBackgroundProgressBarWidth(getResources().getDimension(R.dimen.default_background_stroke_width));
        int animationDuration = 2; // 2500ms = 2,5s
        circularProgressBar.setProgressWithAnimation(2, animationDuration); // Default duration = 1500ms

        //Generate reference to progress bar from layout file
        vProgBar1 = (ProgressBar)findViewById(R.id.vert_progress_bar1);
        vProgBar2 = (ProgressBar)findViewById(R.id.vert_progress_bar2);
        vProgBar3 = (ProgressBar)findViewById(R.id.vert_progress_bar3);
        vProgBar4 = (ProgressBar)findViewById(R.id.vert_progress_bar4);

        statusButton = (Button) findViewById(R.id.finishButton);

        //References to on screen text boxes
        currBestVal = (TextView)findViewById(R.id.currentBestValue);
        currBestVal.setText("0.0°");
        highscoreVal = (TextView)findViewById(R.id.highscoreValue);
        highscoreVal.setText(currentHighScore + "°");

        angle = (TextView)findViewById(R.id.jointAngle);
        numSquats = (TextView)findViewById(R.id.NumSquats);
        //numSquats.setText("Squats Completed: \t 0/" + setNumSquats);
        numSquats.setText("Squats Completed: \t " + countSquats + "/" + setNumSquats);

        //Link the buttons and textViews to respective views
        txtString = (TextView) findViewById(R.id.txtString);
        txtStringLength = (TextView) findViewById(R.id.testView1);
        jointAngleText = (TextView) findViewById(R.id.JointAngleText);
        jointAngleProgress = (TextView) findViewById(R.id.JointAngleProgress);
        sensorView1 = (TextView) findViewById(R.id.sensorView1);
        sensorView2 = (TextView) findViewById(R.id.sensorView2);
        sensorView3 = (TextView) findViewById(R.id.sensorView3);

        //showStartCalibrationNotification();                                         //Display alert to user showing that calibration is required

    //Handler executed on receiving new data from BT device
        bluetoothIn =  new Handler() {
            public void handleMessage(android.os.Message msg) {
                System.out.println("In loop");
                if (msg.what == handlerState) {                                     //If message correspondes to desired input
                    String readMessage = (String) msg.obj;                          // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                             // keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");               // determine the end-of-line

                    //System.out.println(readMessage);                              //Used to print out raw data
                    System.out.println(recDataString);
                    //System.out.println("\t\t\t\t &&&&&&&&& " + endOfLineIndex);

                    if (recDataString.length() > 32 && endOfLineIndex!= -1) {             //32 is min size, - 8 is num of parse characters - this allows app to function properly (Was 20 for HC-05_in_clean for 2 with 1 myoware, and 6 parse char)
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);  // extract string

                        //displayDataRec = "Data Received = " + dataInPrint;            //Display raw data for debugging
                        //int dataLength = dataInPrint.length();                        //get length of data received

                        if (recDataString.charAt(0) == '#')                             //if it starts with # message matches required format
                        {
                            System.out.println("\t\t\t Data to be displayed: "+recDataString);
                            String[] recDataArray = recDataString.toString().split("\\+");  //Data arrives as "#LeftLegDeg+RightLegDeg+Emg1+Emg2+Emg3+Emg4~"
                                                                                            //e.g "#101.20+102.30+2.55+1.67+3.23+1.98+~"

                            String sensorAngleL = recDataArray[0].replace("#","");                      //Left Leg Angle
                            sensorAngleL = sensorAngleL.substring(0,sensorAngleL.length()-1);           //scaling the angle value to 1 decimal place
                            String sensorAngleR = recDataArray[1];                                      //Right leg Angle
                            sensorAngleR = sensorAngleR.substring(0,sensorAngleR.length()-1);           //scaling the angle value to 1 decimal place
                            String EMGsensor1 = recDataArray[2];                            //stores EMG 1 value - VMO Left
                            String EMGsensor2 = recDataArray[3];                            //stores EMG 2 value - VMO Right
                            String EMGsensor3 = recDataArray[4];                            //stores EMG 3 value - Calf Left
                            String EMGsensor4 = recDataArray[5];                            //stores EMG 3 value - Calf Right

                            // Checking which joint angle the user wants displayed and calculating the joint angle
                            if  (setLegForJointAngle == 0){
                                jointAngleVal = Float.parseFloat(sensorAngleL);
                            }
                            else{
                                jointAngleVal = Float.parseFloat(sensorAngleR);
                            }
                            if (jointAngleVal < 0) jointAngleVal = 0;   //Only display positive values to user, greater than 0
                            jointAngleVal =  Math.abs(jointAngleVal);   //Ensure the value is non-negative

                            String.format("%.1f", jointAngleVal);           //Display joint angle to one decimal place

                            angle.setText(jointAngleVal+"°");               //Display the joint angle

                            progress = (jointAngleVal/setSquatAngle)*100;   //Map the joint angle to a 0-100 scale

                            if ( jointAngleVal > setSquatAngle)           // Covers case where user squats above set limit
                                { progress = 100; }                         // Sets progress bar to max value


                            circularProgressBar.setProgress(progress);      //Set progress of circular buffer

                            if (jointAngleVal > currentBestValue) {          //Set the best value for this session
                                currBestVal.setText(jointAngleVal+"°");
                                currentBestValue = jointAngleVal;
                            }

                            if(jointAngleVal > currentHighScore){            //Set the highscore if a new best has been achieved
                                highscoreVal.setText(jointAngleVal + "°");
                                updateHighscore(jointAngleVal);              //Update the value stored in the SQLite DB
                                System.out.println("\t\t\t\t\t\t\t\t\t\tUpdate Highscore called");
                                currentHighScore = jointAngleVal;
                            }


                            //Count up squats
                            squatCounter(jointAngleVal);                                //Increments the on-screen counter when a squat has been successfully perfromed

                            //Max difference in joint angle between legs
                            jointAngleDifference(sensorAngleL, sensorAngleR);



                            /* EMG values coming in are in range 0-5V
                                Plotting them requires mapping to a 0-100 scale */

                            //First EMG signal
                            float vmoL = Float.parseFloat(EMGsensor1);                     //Get first EMG Value - VMO Left
                            vProgBar1.setProgress((int)(vmoL * 20.0));                   //Set progress of EMG progress bar

                            //Second EMG signal
                            float vmoR = Float.parseFloat(EMGsensor2);                     //Get second EMG Value - VMO Right
                            vProgBar2.setProgress((int)(vmoR * 20.0));                   //Set progress of EMG progress bar

                            //Third EMG signal
                            float calfL = Float.parseFloat(EMGsensor3);                     //Get third EMG Value
                            vProgBar3.setProgress((int)(calfL * 20.0));                   //Set progress of EMG progress bar

                            //Fourth EMG signal
                            float calfR = Float.parseFloat(EMGsensor4);                     //Get fourth EMG Value
                            vProgBar4.setProgress((int)(calfR * 20.0));                   //Set progress of EMG progress bar


                            updateMaxEmgValues(vmoL, vmoR, calfL, calfR);                   //Used to store the max Emg results for highlights page


                            try {
                                date = new Date();                                      //Reference to date object - must do this each time
                                Timestamp t = new Timestamp(date.getTime());            //Generate a timestamp, with nanosecond accuracy
                                build.append(t.toString() + "," + sensorAngleL + "," + sensorAngleR + "," + EMGsensor1 + "," + EMGsensor2 + "," +
                                        EMGsensor3 + "," + EMGsensor4 + "\n");
                                numberOfSamplesReceived++;                              //Increment the number of samples received
                                if (numberOfSamplesReceived == 20) {                     //Flush the StringBuilder data to memory on receiving 20 samples
                                    myFile.write(build.toString());                     //Write to file
                                    build.setLength(0);                                 //Reset the StringBuilder so that it is empty
                                    numberOfSamplesReceived = 0;                        //Reset the number of samples that have been received
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                        }
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                    }



                }

            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

    }

    //Used to increment the squat counter when the desired squatting angle has been completed
    private void squatCounter(float currAngle){
        if (currAngle <= standAngle && metDesiredAngle == true){
            metDesiredAngle = false;
            countSquats++;
            numSquats.setText("Squats Completed: \t " + countSquats + "/" + setNumSquats);

            if(setGIFStatus == false){                                  //Method called only when first squat is completed
                updateGIFStatus(1);                                     //Updates the GIF to display on results screen
                setGIFStatus = true;
            }

            if (countSquats == setNumSquats){
                statusButton.setText("Finished!");
                showCompleteNotification();                             //Displays notification for 10 seconds informing user that exercise is complete
                                                                        //If user doesnt respond, they can continue the exercise or click the 'Finish' button
                updateGIFStatus(2);
                //also must prepare the values for passing to next activity.
            }
        }
        else if (currAngle >= setSquatAngle && metDesiredAngle == false){
            metDesiredAngle = true;
        }
    }


    //Updates the highscore stored in a users entry within the database
    private void updateHighscore(float highscore){
        SharedPreferences sharedPref = this.getSharedPreferences("FYP_Username",Context.MODE_PRIVATE);  //SharedPreferences used for passing data between activities
        String id = sharedPref.getString("Username",null);                                              //This sharedPref stores the current user's username
        if (id != null){
            DatabaseHelper helper = new DatabaseHelper(this);                                           //Reference to serving DB
            helper.updateHighscore(id, highscore);                                                      //Pass curr. user and new highscore to DB
        }
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

    private void jointAngleDifference(String left, String right){
        float leftAngle = Math.abs(Float.parseFloat(left));
        float rightAngle = Math.abs(Float.parseFloat(right));
        jointAngleDifference = Math.abs(leftAngle - rightAngle);
        if (jointAngleDifference > 5){
            //set text to red
        }
        else{
            //set text to green
        }
    }

    private void updateMaxEmgValues(float emg1, float emg2, float emg3, float emg4){
        if (emg1 > emg1MaxVal) emg1MaxVal = emg1;
        if (emg2 > emg2MaxVal) emg2MaxVal = emg2;
        if (emg3 > emg3MaxVal) emg3MaxVal = emg3;
        if (emg4 > emg4MaxVal) emg4MaxVal = emg4;
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
                System.out.println("Socket closed in try block");
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming/beginning transmission to check device is connected
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
            System.out.println("Closing Socket on Pause");
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

    //create new class for connect thread, runs BT in parallel
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
                Toast.makeText(getBaseContext(), "Connection Failure in Main Activity", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

/*    private void showStartCalibrationNotification() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle("Calibration Required").setMessage("You must complete the calibration procedure to begin!\n\t Please perform 3 squats at your set squatting angle.");
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

        handler.postDelayed(runnable, 6000);
    }*/



    //Informs the user that they have completed their set number of squats
    private void showCompleteNotification() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle("Exercise Complete").setMessage("You have completed your exercise!\n\t Click Finish to see a summary of your results.");
        dialog.setPositiveButton("Finish", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                //exitLauncher();
                System.out.println("Confirm Selected");
                try{myFile.close();}                                                              // Close the file the data is written to
                catch(Exception e){ System.out.println("Exception thrown");}
                Intent i = new Intent(MainActivity.this, ResultsScreen.class);               //Goes to the list Exercises activity
                float[] exerciseHighlights = {currentBestValue, jointAngleDifference, emg1MaxVal, emg2MaxVal, emg3MaxVal, emg4MaxVal, setNumSquats, countSquats};
                i.putExtra("exHighlights", exerciseHighlights);    //Make the results available in the target intent,
                startActivity(i);
            }
        });
        final AlertDialog alert = dialog.create();
        alert.show();

            // Hide after 10 seconds
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

        handler.postDelayed(runnable, 10000);
    }



    public void onButtonClick(View v) {
        System.out.println("ButtonPressed");
        if (v.getId() == R.id.finishButton) {       //Checking if the ID of the button selected is the finish Button
            System.out.println("******************ButtonPressed");
            try{myFile.close();}                                                              // Close the file the data is written to
            catch(Exception e){ System.out.println("Exception thrown");}
            Intent i = new Intent(MainActivity.this, ResultsScreen.class);               //Goes to the list Results screen
            float[] exerciseHighlights = {currentBestValue, jointAngleDifference, emg1MaxVal, emg2MaxVal, emg3MaxVal, emg4MaxVal, setNumSquats, countSquats};
            i.putExtra("exHighlights", exerciseHighlights);    //Make the results available in the target intent,
            startActivity(i);  //Switch to highlights screen    //see http://stackoverflow.com/questions/24436682/android-why-use-intent-putextra-method

        }
    }

    //If back in bottom bar selected, direct user back to exercise setup screen
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, CalibrationScreen.class); //second is classname.class
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    //Updates shared preferences, storing a value regarding which GIF to display on results screen
    // 0 -> no squat performed (default at start)
    // 1 -> some squats performed
    // 2 -> all squats completed
    private void updateGIFStatus(int current){
        SharedPreferences sharedPref = getSharedPreferences("GIF_Status",Context.MODE_PRIVATE);     //Used for sharing data between activities - accessible within the app only
        SharedPreferences.Editor editor = sharedPref.edit();                                        //Must use an editor to modifiy/create the sharedPreferences
        String value = current+"";                                                                  //Storing the current value
        editor.putString("Status",value);                                                            //Store the value in the sharedPreferences
        editor.apply();
    }

}