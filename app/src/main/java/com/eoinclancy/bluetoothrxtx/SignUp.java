package com.eoinclancy.bluetoothrxtx;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by eoin on 29/09/2016.
 */
public class SignUp extends Activity {

    DatabaseHelper helper = new DatabaseHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
    }

    public void onSignUpClick(View v){
        if (v.getId() == R.id.Bsignupbutton){ //SignUp button selected -- store all values

            /*          Fetch all the text values           */
            EditText name = (EditText)findViewById(R.id.TFname);
            EditText email = (EditText)findViewById(R.id.TFemail);
            EditText username = (EditText)findViewById(R.id.TFuname);
            EditText pass1 = (EditText)findViewById(R.id.TFpass1);
            EditText pass2 = (EditText)findViewById(R.id.TFpass2);
            EditText trainerEmail = (EditText)findViewById(R.id.TFtrainerEmail);

            /*         cast data to strings                 */
            String namestr = name.getText().toString();
            String emailstr = email.getText().toString();
            String usernamestr = username.getText().toString();
            String pass1str = pass1.getText().toString();
            String pass2str = pass2.getText().toString();
            String trainerEmailstr = trainerEmail.getText().toString();

            if(!pass1str.equals(pass2str)){
                //Display a popup message to a user
                Toast.makeText(SignUp.this, "Passwords do not match!", Toast.LENGTH_SHORT).show(); //Format context, message, length
            }
            else{                               //insert details in the database
                Contact c = new Contact();                  //Need a contact object to pass in all signup details
                c.setName(namestr);
                c.setEmail(emailstr);
                c.setUname(usernamestr);
                c.setPass(pass1str);
                c.setTrainerEmail(trainerEmailstr);

                helper.insertContact(c);

                Intent returnIntent = new Intent();
                returnIntent.putExtra("username",usernamestr);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }

        }
    }
}
