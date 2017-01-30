package com.eoinclancy.bluetoothrxtx;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class CalibrationScreen extends AppCompatActivity {

    RadioGroup radioGroup;
    RadioButton right;
    Button setupComplete;
    int checked;
    int angle = 90;
    int numSquats = 10;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_screen);


        right = (RadioButton) findViewById(R.id.rightLegRB);            //Reference to right leg radio button
        right.setChecked(true);                                         //Right leg radio button visually shown to be checked
        radioGroup = (RadioGroup) findViewById(R.id.radioGr);           //Reference to radioButton Group
        radioGroup.check(right.getId());                                //Ensuring that it is checked as part of radioGroup too, Right id = 1, Left id = 0
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {      //Listener for monitoring changes to selected radio buttons
                checked = radioGroup.indexOfChild(findViewById(i));     //Get index of currently selected radio button
                switch (checked) {
                    case 0:
                        Toast.makeText(getBaseContext(), "Left Leg Selected", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Toast.makeText(getBaseContext(), "Right Leg Selected", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        });

        setupComplete = (Button) findViewById(R.id.proceed);            //Reference to button user presses when setup is complete
        setupComplete.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent i = new Intent(CalibrationScreen.this, DeviceListActivity.class);
                int selectedLeg = 0;                                    //0 for left leg
                if (right.getId() == radioGroup.getCheckedRadioButtonId()){
                    selectedLeg = 1;                                    //Set to 1 where right leg is selected (default)
                }
                int[] values = {angle, numSquats, selectedLeg};         //Values to be passed to next activity
                i.putExtra("setupDetails", values); //Values are available in the directed to activity, under the setupDetails key
                startActivity(i);
            }
        });


    }

    ///////////////////////////////////// Methods for handling the number of squats ///////////////////////////////////////////

    //Handles - button presses for decrementing the number of squats to be performed
    public void decSquat(View view) {
        if (numSquats > 2) {
            numSquats--;
            displaySquat(numSquats);
        } else {
            Toast.makeText(getBaseContext(), "2 Squats is Minimum ", Toast.LENGTH_SHORT).show();
        }

    }

    //Handles + button presses for decrementing the number of squats to be performed
    public void incrSquat(View view) {
        if (numSquats < 100) {
            numSquats++;
            displaySquat(numSquats);
        } else {
            Toast.makeText(getBaseContext(), "100 Squats is Maximum!", Toast.LENGTH_SHORT).show();
        }
    }

    //Update the values when the value has changed
    private void displaySquat(int sqt) {
        TextView SquatText = (TextView) findViewById(R.id.NumSqts);
        SquatText.setText("" + sqt);
    }


    ///////////////////////////////////// End of methods for handling the number of squats ///////////////////////////////////////////

    ///////////////////////////////////// Methods for handling the Joint angle ///////////////////////////////////////////

    //Handles - button presses for decrementing the joint angle
    public void decAngle(View view) {

        if (angle > 31) {
            angle--;
            displayAngle(angle);
        } else {
            Toast.makeText(getBaseContext(), "30° is the smallest available angle", Toast.LENGTH_SHORT).show();
        }

    }

    //Handles + button presses for incrementing the joint angle
    public void incrAngle(View view) {
        if (angle < 130) {
            angle++;
            displayAngle(angle);
        } else {
            Toast.makeText(getBaseContext(), "130° is the largest available angle", Toast.LENGTH_SHORT).show();
        }
    }

    //Update the values when the value has changed
    private void displayAngle(int angle) {
        TextView AngleText = (TextView) findViewById(R.id.SquatAngle);
        AngleText.setText(angle + "°");
    }

    ///////////////////////////////////// End of methods for handling the Joint angle ///////////////////////////////////////////


}
