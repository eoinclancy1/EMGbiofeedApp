package com.eoinclancy.bluetoothrxtx;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by eoin on 28/09/2016.
 */
public class Display extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display);
        String username = getIntent().getStringExtra("Username");  //Fetching the username
        TextView tv = (TextView)findViewById(R.id.TVusername);      //for TextView use TextView
        tv.setText(username);                                           //displaying the text in the text view
    }

}
