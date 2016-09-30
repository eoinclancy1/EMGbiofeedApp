package com.eoinclancy.bluetoothrxtx;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class LoginMain extends ActionBarActivity {

    DatabaseHelper helper = new DatabaseHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login_main, menu);
        return true;
    }

    public void onButtonClick(View v){
        if(v.getId() == R.id.Blogin){       //Checking if the ID of the button selected is the Login Button

            EditText a = (EditText)findViewById(R.id.TFusername);   //EditText is used for a textField
            String str = a.getText().toString();
            EditText b = (EditText)findViewById(R.id.TFpassword);   //Get Password
            String pass = b.getText().toString();                   //Ath this point hava password and username

            String password =  helper.searchPass(str);
            if (pass.equals(password)){
                //Intent i = new Intent(LoginMain.this, Display.class);     //Can uncomment to use this screen again
                Intent i = new Intent(LoginMain.this, DeviceListActivity.class);
                i.putExtra("Username", str);    //Putting the username
                startActivity(i);
            }
            else{
                Toast.makeText(LoginMain.this, "Username and Password don't match!", Toast.LENGTH_SHORT).show(); //Format context, message, length
            }

            //Check if username and password match


        }
        if(v.getId() == R.id.Bsignup){      //Checking if user clicks on the signup button

            Intent i = new Intent(LoginMain.this, SignUp.class);
            //startActivity(i);
            startActivityForResult(i,1);        //User presented with signup screen, from which once completed will return here to execute onActivityResult method

        }
    }

    //Deals with control being handed back to login screen after new user is created
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1){
            if(resultCode == Activity.RESULT_OK){
                String result=data.getStringExtra("username");
                Toast.makeText(LoginMain.this, "Welcome: " + result, Toast.LENGTH_SHORT).show(); //Format context, message, length
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Can write code here to deal with user not completing the signup
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
