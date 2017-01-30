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
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button btnOn, btnOff;
    TextView txtArduino, txtString, txtStringLength, jointAngleText,jointAngleProgress, sensorView1, sensorView2, sensorView3;
    Handler bluetoothIn;

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private String displayDataRec;
    private ConnectedThread mConnectedThread;


    //Additions for circular progress bar
    CircularProgressBar circularProgressBar;
    TextView angle;
    TextView currBestVal;
    TextView highscoreVal;
    TextView numSquats;
    ProgressBar vProgBar1;
    ProgressBar vProgBar2;
    ProgressBar vProgBar3;
    ProgressBar vProgBar4;
    float currentBestValue = 0;
    float currentHighScore = 0;
    float standAngle = 10;
    float progress = 0;
    float jointAngleDifference = 0;
    float emg1MaxVal = 0;
    float emg2MaxVal = 0;
    float emg3MaxVal = 0;
    float emg4MaxVal = 0;
    float setSquatAngle = 90; //Default 90 deg
    int setNumSquats = 10; //Default 10 squats
    int setLegForJointAngle = 1; //Right leg by default;
    boolean metDesiredAngle = false;
    int countSquats = 0;
    private Random random = new Random();
    float jointAngleVal;
    boolean setGIFStatus = false;

    Button statusButton;


    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Bundle extras = getIntent().getExtras();
        if (extras != null){
            int[] setupValues = extras.getIntArray("setupDetails");
            setSquatAngle = setupValues[0];          //has squatting angle
            setNumSquats = setupValues[1];           //has number of squats
            setLegForJointAngle = setupValues[2];    //setupValues[2] has 0 for left leg, 1 for right leg

        }

        updateGIFStatus(0);                           //Setting the GIF to be displayed on results page, 0 corresponds to fail, no squats performed
        currentHighScore = getHighscore();
        System.out.println("Current Highscore is " + currentHighScore);
        //Additions for the circular progress bar material
        circularProgressBar = (CircularProgressBar)findViewById(R.id.circularPbar);
        //circularProgressBar.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        //circularProgressBar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
        circularProgressBar.setProgressBarWidth(getResources().getDimension(R.dimen.activity_horizontal_margin));
        circularProgressBar.setBackgroundProgressBarWidth(getResources().getDimension(R.dimen.default_background_stroke_width));
        int animationDuration = 2; // 2500ms = 2,5s
        circularProgressBar.setProgressWithAnimation(2, animationDuration); // Default duration = 1500ms


        vProgBar1 = (ProgressBar)findViewById(R.id.vert_progress_bar1);
        vProgBar2 = (ProgressBar)findViewById(R.id.vert_progress_bar2);
        vProgBar3 = (ProgressBar)findViewById(R.id.vert_progress_bar3);
        vProgBar4 = (ProgressBar)findViewById(R.id.vert_progress_bar4);

        statusButton = (Button) findViewById(R.id.finishButton);

        currBestVal = (TextView)findViewById(R.id.currentBestValue);
        currBestVal.setText("0.0°");
        highscoreVal = (TextView)findViewById(R.id.highscoreValue);
        highscoreVal.setText(currentHighScore + "°");

        angle = (TextView)findViewById(R.id.jointAngle);
        numSquats = (TextView)findViewById(R.id.NumSquats);
        numSquats.setText("Squats Completed: \t 0/" + setNumSquats);



        //Link the buttons and textViews to respective views
        //btnOn = (Button) findViewById(R.id.buttonOn);
        //btnOff = (Button) findViewById(R.id.buttonOff);
        txtString = (TextView) findViewById(R.id.txtString);
        txtStringLength = (TextView) findViewById(R.id.testView1);
        jointAngleText = (TextView) findViewById(R.id.JointAngleText);
        jointAngleProgress = (TextView) findViewById(R.id.JointAngleProgress);
        sensorView1 = (TextView) findViewById(R.id.sensorView1);
        sensorView2 = (TextView) findViewById(R.id.sensorView2);
        sensorView3 = (TextView) findViewById(R.id.sensorView3);

//Handler executed on receiving new data from BT device
        bluetoothIn =  new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                  // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line

                    //System.out.println(readMessage);
                    //System.out.println(recDataString);
                    //System.out.println("\t\t\t\t &&&&&&&&& " + endOfLineIndex);

                    //if (endOfLineIndex > 0) {                                           // make sure there data before ~ -- this often caused app to seize up
                    if (recDataString.length() > 32 && endOfLineIndex!= -1) {           //32 is min size, - 8 is num of parse characters - this allows app to function properly (Was 20 for HC-05_in_clean for 2 with 1 myoware, and 6 parse char)
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        displayDataRec = "Data Received = " + dataInPrint;

                        int dataLength = dataInPrint.length();                          //get length of data received

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

                            // First joint angle
                            if  (setLegForJointAngle == 0){
                                jointAngleVal = Float.parseFloat(sensorAngleL);
                            }
                            else{
                                jointAngleVal = Float.parseFloat(sensorAngleR);
                            }
                            if (jointAngleVal < 0) jointAngleVal = 0;
                            jointAngleVal =  Math.abs(jointAngleVal);

                            String.format("%.1f", jointAngleVal);
                            //if (jointAngleVal>100){                                     //To avoid case of value too large - could be set to custom value in config setup
                            //    jointAngleVal = 100;
                            //}
                            angle.setText(jointAngleVal+"°");                           //Display the joint angle

                            progress = (jointAngleVal/setSquatAngle)*100;

                            circularProgressBar.setProgress(progress);             //Set progress of circular buffer

                            if (jointAngleVal > currentBestValue) {                     //Set the best value for this session
                                currBestVal.setText(jointAngleVal+"°");
                                currentBestValue = jointAngleVal;
                            }
                                //could below if go inside above if? save time
                            if(jointAngleVal > currentHighScore){                       //Set the highscore if a new best has been achieved
                                highscoreVal.setText(jointAngleVal + "°");
                                updateHighscore(jointAngleVal);                         //Update the value stored in the SQLite DB
                                System.out.println("\t\t\t\t\t\t\t\t\t\tUpdate Highscore called");
                                currentHighScore = jointAngleVal;
                            }

                            //Count up squats
                            squatCounter(jointAngleVal);                                //Increments the on-screen counter when a squat has been successfully perfromed

                            //Max difference in joint angle between legs
                            jointAngleDifference(sensorAngleL, sensorAngleR);

                            //First EMG signal
                            float vmo1 = Float.parseFloat(EMGsensor1);                     //Get first EMG Value
                            vProgBar1.setProgress((int)(vmo1 * 10));                   //Set progress of EMG progress bar

                            //Second EMG signal
                            float vmo2 = Float.parseFloat(EMGsensor2);                     //Get second EMG Value
                            vProgBar2.setProgress((int)(vmo2 * 10));                   //Set progress of EMG progress bar

                            //Third EMG signal
                            float vmo3 = Float.parseFloat(EMGsensor3);                     //Get third EMG Value
                            vProgBar3.setProgress((int)(vmo3 * 10));                   //Set progress of EMG progress bar

                            //Fourth EMG signal
                            float vmo4 = Float.parseFloat(EMGsensor4);                     //Get fourth EMG Value
                            vProgBar4.setProgress((int)(vmo4 * 10));                   //Set progress of EMG progress bar


                            updateMaxEmgValues(vmo1, vmo2, vmo3, vmo4);
                            //jointAngleText.setText(" Joint Angle = " + sensor0 + "°");    //update the textviews with sensor values
                            //sensorView1.setText(" Sensor 1 Voltage = " + sensor1 + "V");
                            //sensorView2.setText(" Sensor 2 Voltage = " + sensor2 + "V");
                            //sensorView3.setText(" Sensor 3 Voltage = " + sensor3 + "V");

                            //if (jointAngleValue < 40){
                            //    jointAngleProgress.setText("Standing");
                            //}
                            //else if (jointAngleValue >= 40 && jointAngleValue < 70 ){
                            //    jointAngleProgress.setText("Partial Squat");
                            //}
                            //else if (jointAngleValue >= 70 && jointAngleValue < 100){
                            //    jointAngleProgress.setText("Half Squat");
                            //}
                            //else{
                            //    jointAngleProgress.setText("Deep Squat");
                            //}

                        }
                        recDataString.delete(0, recDataString.length());                    //clear all string data

                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        // Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        //btnOff.setOnClickListener(new OnClickListener() {
          //  public void onClick(View v) {
            //    mConnectedThread.write("0");    // Send "0" via Bluetooth
              //  Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            //}
        //});

        //btnOn.setOnClickListener(new OnClickListener() {
          //  public void onClick(View v) {
            //    mConnectedThread.write("1");    // Send "1" via Bluetooth
             //   Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
                //Intent i = new Intent(MainActivity.this, LoginMain.class);
                //startActivity(i);
            //}
        //});
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
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

    private void showCompleteNotification() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle("Exercise Complete").setMessage("You have completed your exercise!\n\t Click Finish to see a summary of your results.");
        dialog.setPositiveButton("Finish", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                //exitLauncher();
                System.out.println("Confirm Selected");
                Intent i = new Intent(MainActivity.this, ResultsScreen.class);               //Goes to the list Exercises activity
                float[] exerciseHighlights = {currentBestValue, jointAngleDifference, emg1MaxVal, emg2MaxVal, emg3MaxVal, emg4MaxVal};
                i.putExtra("exHighlights", exerciseHighlights);    //Putting the results, available in the class the intent points to, see http://stackoverflow.com/questions/24436682/android-why-use-intent-putextra-method
                startActivity(i);
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

        handler.postDelayed(runnable, 10000);
    }



    public void onButtonClick(View v) {
        System.out.println("ButtonPressed");
        if (v.getId() == R.id.finishButton) {       //Checking if the ID of the button selected is the finish Button
            System.out.println("******************ButtonPressed");
            Intent i = new Intent(MainActivity.this, ResultsScreen.class);               //Goes to the list Exercises activity
            float[] exerciseHighlights = {currentBestValue, jointAngleDifference, emg1MaxVal, emg2MaxVal, emg3MaxVal, emg4MaxVal};
            i.putExtra("exHighlights", exerciseHighlights);    //Putting the results, available in the class the intent points to, see http://stackoverflow.com/questions/24436682/android-why-use-intent-putextra-method
            startActivity(i);

        }
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