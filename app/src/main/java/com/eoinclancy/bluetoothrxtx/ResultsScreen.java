package com.eoinclancy.bluetoothrxtx;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;


/*
    Class used to manage the display of the exercise results to the user
 */
public class ResultsScreen extends AppCompatActivity {

    GifView gif;                        //Reference to GIF to be displayed
    TextView ExerciseResultText;        //Reference to Text displayed that gives feedback to user based on performance
    int ExerciseProgress;                     //
    Button btn;
    TextView jointAngle;
    TextView vmoLeft;
    TextView calfLeft;
    TextView angleDiff;
    TextView vmoRight;
    TextView calfRight;
    float[] results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_screen);                                       //Layout file

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);                                              //Used for enabling a back button on the action bar

        gif = (GifView)findViewById(R.id.gif);                                                  //Reference to GIF on layout file
        ExerciseResultText = (TextView)findViewById(R.id.ResultMessage);                        //Ref. to message text view

        float[] exerciseResults = getExerciseResults();
        results = exerciseResults;
        exerciseResults[1] = round2Dec(exerciseResults[1]);                                     //Ensures the value is only 2 decimal places
        jointAngle = (TextView)findViewById(R.id.jointAngle);
        jointAngle.setText("Max Joint Angle: " + exerciseResults[0] + "°");
        vmoLeft = (TextView)findViewById(R.id.vmoLeft);
        vmoLeft.setText("VMO Left Max: " + exerciseResults[2]);
        calfLeft = (TextView)findViewById(R.id.calfLeft);
        calfLeft.setText("Calf Left Max: " + exerciseResults[4]);
        angleDiff = (TextView)findViewById(R.id.angleDiff);
        angleDiff.setText("Max difference in joint angles: " + exerciseResults[1] + "°");
        vmoRight = (TextView)findViewById(R.id.vmoRight);
        vmoRight.setText("VMO Right Max: " + exerciseResults[3]);
        calfRight = (TextView)findViewById(R.id.calfRight);
        calfRight.setText("Calf Right Max: " + exerciseResults[5]);


        final EmailInformation emailInfo = getEmailInfo();                                      //Object stores all relevant info for sending email to trainer
        final String emailSubj = emailInfo.getUserName() + " exercises on " + getDate();        //Email subject
        final String emailBody = getEmailBody(emailInfo);                                       //Email body

        btn = (Button) findViewById(R.id.sendEmail);                                            //Ref. to button for sending email to trainer
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_SEND);                                 //Intend to send data between activities/boundaries
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailInfo.getTrainerEmail()}); //To part of email, trainer email address
                intent.putExtra(Intent.EXTRA_CC,  new String[]{emailInfo.getUserEmail()});      //CC part of email, send copy to user themselves
                intent.putExtra(Intent.EXTRA_SUBJECT, emailSubj);                               //Subject of email
                intent.putExtra(Intent.EXTRA_TEXT, emailBody);                                  //Body of email

                try{
                    String targetFilePath = Environment.getExternalStorageDirectory().getPath() + "//" + "EMG Data.txt"; //References /storage/emulated/0//EMG Data.txt
                    Uri attachmentUri = Uri.parse(targetFilePath);                                                       //Identifies resource by location
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + attachmentUri));                          //Add the .txt file as an attachment to the email
                }
                catch(Exception e){e.printStackTrace();}


                intent.setType("message/rfc822");                                               //Default email style
                startActivity(Intent.createChooser(intent, "Choose email provider..."));        //Opens up menu for user to choose email provider from (GMail, YMail etc)
            }
        });

        ExerciseProgress = getExerciseProgress();                       //Returns number corresponding to progress through exercise
        String info = "";
        switch(ExerciseProgress){
            case 2:                                                 //All exercies completed
                info += "Congratulations! Exercise Complete!";
                break;
            case 1:                                                 //Between 1 and targeted num of squats done
                info += "So close! You nearly made it!";
                break;
            case 0:                                                 //No squat completed
                info += "Good Effort! Better luck next time!";
                break;
            default:
                info += "Congratulations! Exercise Complete!";
                break;

        }

        ExerciseResultText.setText(info);

    }

    //If back in action bar is pressed, direct user back to exercise list
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, ExerciseList.class); //second is classname.class
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //If back in bottom bar, direct user back to exercise list
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ExerciseList.class); //second is classname.class
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    //Get the user's highscore from the DB entry
    private EmailInformation getEmailInfo(){
        SharedPreferences sharedPref = this.getSharedPreferences("FYP_Username", Context.MODE_PRIVATE); //SharedPreferences used for passing data between activities
        String id = sharedPref.getString("Username",null);                                              //This sharedPref stores the current user's username
        EmailInformation info = new EmailInformation("User Name", "User Email", "Trainer Email");                                                                           //Default value
        if (id != null){
            DatabaseHelper helper = new DatabaseHelper(this);                                           //Reference to serving DB
            info = helper.getEmailInfo(id);                                                             //Get the highscore for the desired user based on their username
        }
        return info;
    }

    //Returns todays date as dd-MM-yyyy
    private String getDate(){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        return df.format(c.getTime());
    }

    //Used to construct the main results part of email to send to trainer
    public String getEmailBody(EmailInformation info) {

        String s1 = "Patient " + info.userName + ", performed the full squat exercise\n\n";
        String s2 = "Number of squats targeted: " + results[6] + "\n";
        String s3 = "Number of squats completed: " + results[7] + "\n";

        String s4 = "Max Squatting Angle = " + results[0] +"\n";
        String s5 = "Max Difference in Squatting Angles = " + results[1] + "\n\n";

        String s6 = "Additional Notes: (e.g Any pain or discomfort observed)";

        return s1+s2+s3+s4+s5+s6;
    }

    //Get number representing how much of exercise the user completed
    private int getExerciseProgress(){
        SharedPreferences sharedPref = this.getSharedPreferences("GIF_Status",Context.MODE_PRIVATE);        //Reference to location where Exercise completion status is stored
        String ExerciseResult = sharedPref.getString("Status",null);                                             //Get the exercise completion status: 0-> no squats, 1-> 1 or more squats done, 2 -> all squats completed
        Integer result = Integer.parseInt(ExerciseResult);
        return result;
    }


    private float[] getExerciseResults(){
        Bundle extras = getIntent().getExtras();
        float[] setupValues = {0,0,0,0,0,0,0,0};
        if (extras != null){
            setupValues = extras.getFloatArray("exHighlights");
        }
        return setupValues;
    }

    public static float round2Dec(float value) {
        int scale = 2; //Number of decimal places
        return (float)(Math.round(value * Math.pow(10, scale)) / Math.pow(10, scale));
    }

}
