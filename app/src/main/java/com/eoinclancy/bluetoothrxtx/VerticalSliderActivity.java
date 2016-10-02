package com.eoinclancy.bluetoothrxtx;

/**
 * Created by eoin on 01/10/2016.
 */

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import java.util.Random;

public class VerticalSliderActivity extends Activity {

    VerticalSeekBar verticalSeebar;
    VerticalSeekBar verticalSeebar2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slider);
        //final TextView sliderText = (TextView)findViewById(R.id.verticalSeekbarText);
        //sliderText.setTextSize(48);
        verticalSeebar = (VerticalSeekBar)findViewById(R.id.verticalSeekbar);
        verticalSeebar.setProgress(255);
        verticalSeebar2 = (VerticalSeekBar)findViewById(R.id.verticalSeekbar2);
        verticalSeebar2.setProgress(255);

/*
        verticalSeebar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {


            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // sliderText.setText(""+progress);
            }


        });

*/
    }

    public void onButtonClick(View v){
        Random rand = new Random();
        int min = 0;
        int max = 255;
        int value1 = 0;
        int value2 = 0;
        if(v.getId() == R.id.btn){       //Checking if the ID of the button selected is the Login Button
            value1 = rand.nextInt((max - min) + 1) + min;
            value2 = rand.nextInt((max - min) + 1) + min;
            //verticalSeebar.setProgress(200);
            //verticalSeebar2.setProgress(100);
            verticalSeebar.setProgress(value1);
            verticalSeebar2.setProgress(value2);

        }
    }
}
