package com.eoinclancy.bluetoothrxtx;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/* This class is used to display the entire list of activites that are made visible to the user */

public class ExerciseList extends ActionBarActivity {

    private List<Exercise> myExcercises = new ArrayList<Exercise>();
    private  ListView list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excercise_list);

        populateExerciseList();                                                              //Populate the list items required
        populateListView();                                                             //Populate the ListView that is setup in the exercise_item_view.xml file
        registerClickCallback();                                                        //Handles clicks
    }


    /* Used to add items to the list */
    private void populateExerciseList() {
        myExcercises.add(new Exercise("Full Squat", "Available", R.drawable.squat));
        myExcercises.add(new Exercise("Single Squat", "Available", R.drawable.single_squat));
        myExcercises.add(new Exercise("OptoJump","Coming Soon!",R.drawable.optojump));
        myExcercises.add(new Exercise("Y-Balance Func. Test", "Coming Soon!",R.drawable.ybalance));
        myExcercises.add(new Exercise("Arm Extension", "Coming Soon!", R.drawable.bicep_curl));
        myExcercises.add(new Exercise("Lunge Test", "Coming Soon!", R.drawable.lunge_test2));
    }


    private void populateListView() {
        ArrayAdapter<Exercise> adapter = new MyListAdapter();                       //Calls the constructor of the inner class MyListAdapter
        ListView list = (ListView)findViewById(R.id.exerciseListView);          //Gets a reference to the listView in file activity_exercise_list.xml
        list.setAdapter(adapter);                                               //Basically plugs the adapter (new layout) into the ListView
    }

    //Inner Class
    private class MyListAdapter extends ArrayAdapter<Exercise>{                 //Extends as need it above in populateListView()
        public MyListAdapter(){                                                 //Don't need take in anything: Inner class has full access to outer class variables //Tells you what you are working with
            super(ExerciseList.this, R.layout.exercise_item_view, myExcercises);                //Calling the base class constructor + give it: Context, what view is on screen in the list, what objects are going in
        }

        @Override //What happens with each list item - Overrides a method in ArrayAdapter
        public View getView(int position, View convertView, ViewGroup parent) { //The position gives the num of the selected item
            // Make sure we have a view to work with, may have been given null
            View itemView = convertView;                                                            //convertView can be reused if already created or could be null
            if (itemView == null){
                itemView = getLayoutInflater().inflate(R.layout.exercise_item_view, parent, false); //Inflate a layout - takes xml code and inflates it into fully fledged object that can go on the screen, parent is the root to work with, false to not attach to parent/root
            }

            //Find the exercise to work with
            Exercise currentExercise = myExcercises.get(position);

            //Fill the view - pull out each thing to work with + can setup the view
            ImageView imageView = (ImageView)itemView.findViewById(R.id.exercise_imageView);                //finding the image from within the imageView declared above
            imageView.setImageResource(currentExercise.getIconID());

            //Exercise Name:
            TextView exerciseNameText = (TextView) itemView.findViewById(R.id.exercise_itemName);           //Generating a reference to the textView of the item with which the name can be changed to match the currExercise name
            exerciseNameText.setText(currentExercise.getExcercise());


            //Exercise Status:
            TextView exerciseStatusText = (TextView) itemView.findViewById(R.id.exercise_itemStatus);
            exerciseStatusText.setText(currentExercise.getstatus());
            if (currentExercise.getstatus().equals("Available")){
                exerciseStatusText.setTextColor(Color.parseColor("#0c7605"));
            }
            else{
                exerciseStatusText.setTextColor(Color.RED);
            }
            //The android framework will automatically add this to the listView and handles the scrolling

            return itemView;

        }
    }

    /* Determine which exercise was clicked and then can do anyting with that object */
    private void registerClickCallback() {
        list = (ListView) findViewById(R.id.exerciseListView);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {             //Sets a click listener on the list as a whole
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {
                Exercise clickedExercise = myExcercises.get(position);                  //position gives the position within the list, top item is 0 etc
                String message = "Selected:" + clickedExercise.getExcercise();

                if (clickedExercise.getstatus().equals("Available")){                   //If user selects an exercise that is currently available - load it up for them
                    String exerciseDetails = clickedExercise.getExcercise();            //Get exercise name

                    if (exerciseDetails.equals("Full Squat")){
                        Intent i = new Intent(ExerciseList.this, InstructionActivity.class);
                        i.putExtra("excerciseDetails", exerciseDetails);    //Putting the exercise name, available in the class the intent points to, see http://stackoverflow.com/questions/24436682/android-why-use-intent-putextra-method
                        startActivity(i);
                    }

                    else{                                                               //All other available selections go to the bluetooth setup class
                        Intent i = new Intent(ExerciseList.this, VerticalSliderActivity.class);
                        i.putExtra("excerciseDetails", exerciseDetails);    //Putting the exercise name, available in the class the intent points to, see http://stackoverflow.com/questions/24436682/android-why-use-intent-putextra-method
                        startActivity(i);
                    }
                }
                else{
                    message = clickedExercise.getExcercise() + " is not availble yet. Sorry!";
                }
                Toast.makeText(ExerciseList.this, message, Toast.LENGTH_LONG).show();   //Display the selection on screen
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_excercise_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, NotificationActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause(){
        super.onPause();
        list.setOnItemClickListener(null);
    }

    @Override
    public void onResume() {

        super.onResume();
        if(list != null){
            registerClickCallback();
        }
    }

}
