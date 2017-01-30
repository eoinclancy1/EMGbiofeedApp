package com.eoinclancy.bluetoothrxtx;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by eoin on 29/09/2016.
 */
// Extends SQLite OpenHelper class
public class DatabaseHelper extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "contacts.db";
    private static final String TABLE_NAME = "contacts";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_UNAME = "uname";
    private static final String COLUMN_PASS = "pass";
    private static final String COLUMN_TRAINEREMAIL = "trainer_email";
    private static final String COLUMN_SQT_HS = "squat_hs";
    SQLiteDatabase db;
    private static final String TABLE_CREATE = "create table contacts (id integer primary key not null , " +
            "name text not null, email text not null, uname text not null, pass text not null, trainer_email text not null, squat_hs text not null);";       //'contacts' could be written as TABLE_NAME

    /* Constructor could take (Context context, String name, SQLiteDatabase.CursorFactory factory, int version, SQLiteDatabase db) to be fully customisable - not needed */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override   //Must be implemented from parent class
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        System.out.println("New table created!");
        this.db = db;
    }

    @Override   //Must be implemented from parent class
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(query);
        this.onCreate(db);
        /* Use this style for adding a new column to the SQLite DB */
        //if (newVersion > oldVersion) {
        //        db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_SQT_HS + " INTEGER DEFAULT 0");  //Replace COLUMN_SQT_HS with the name of the new col + change the ver num up top
        //}
    }

    public void insertContact(Contact c){
        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String query = "select * from contacts";            //* means all values
        Cursor cursor = db.rawQuery(query, null);           // Using these two lines to get the number of enteries in the db so can create a unique id for new user
        int count = cursor.getCount();
        System.out.println("The new id is: " + count);
        values.put(COLUMN_ID,count);
        values.put(COLUMN_NAME, c.getName());
        values.put(COLUMN_EMAIL, c.getEmail());
        values.put(COLUMN_UNAME, c.getUname());
        values.put(COLUMN_PASS, c.getPass());
        values.put(COLUMN_TRAINEREMAIL, c.getTrainerEmail());
        values.put(COLUMN_SQT_HS, "0");                                 //Default starting highscore of 0 degrees

        db.insert(TABLE_NAME, null, values);            //inserts the contact object into the database
        db.close();
    }


    //Takes username as argument and returns the corresponding password if the username exists in the database
    public String searchPass(String uname){
        db = this.getReadableDatabase();
        String query = "select uname, pass from " + TABLE_NAME; //getting the corresponding password for a username
        Cursor cursor = db.rawQuery(query, null);
        String a,b;                                             //a = username, b = password
        b = "not found";                                        //default value for case where password not found
        if(cursor.moveToFirst()){
            do{                                                 //Sequentially searching through all database enteries
                a = cursor.getString(0);                        //See query to see why locations are 0 and 1
                                                                //0 for 'uname' , 1 for 'pass'
                if(a.equals(uname)){
                    b = cursor.getString(1);                    //Get the associated password
                    break;
                }
            }
            while(cursor.moveToNext());                         //While more enteries exist, loop to find username
        }
        return b;
    }


    //Used to update the joint angle high score of a user
    public void updateHighscore(String ID, float newHS){
        db = this.getReadableDatabase();                    //Reference to serving DB
        ContentValues values = new ContentValues();         //Used to construct message
        String hs = newHS+"";
        values.put(COLUMN_SQT_HS, hs);                      //Add values to message
        //Integer id = Integer.parseInt(ID);
        db.update(TABLE_NAME, values,"id="+ID, null );      //Update the SQLite DB, format (table name, message, id, null)
    }

    //Returns the joint angle highscore for the associated user id provided
    public String getHighscore(String ID){
        Integer id = Integer.parseInt(ID) + 1;                      //Must increment by 1 to get correct row!
        db = this.getReadableDatabase();
        String query = "select id, squat_hs from " + TABLE_NAME;    //Construct query message
        Cursor cursor = db.rawQuery(query, null);                   //Store results from query
        String result = "0.00";                                     //Default value
        if (cursor.move(id)){                                       //Point to the entry corresponding to the provided username
            result = cursor.getString(1);                           //Get the associated highscore
        }
        return result;                                              //Return the highscore
    }

    //Returns the SQLite DB ID for a provided username
    public String searchID(String uname){
        db = this.getReadableDatabase();
        String query = "select id, uname from " + TABLE_NAME; //getting the corresponding password for a username
        Cursor cursor = db.rawQuery(query, null);
        String a,b;                                             //a = username, b = password
        b = "not found";                                        //default value for case where password not found
        if(cursor.moveToFirst()){
            do{                                                 //Sequentially searching through all database enteries
                a = cursor.getString(1);                        //See query to see why locations are 0 and 1
                System.out.println("The username is: " + a);
                if(a.equals(uname)){
                    b = cursor.getString(0);
                    System.out.println("The ID is: " + b);
                    break;
                }
            }
            while(cursor.moveToNext());
        }
        return b;
    }

    public EmailInformation getEmailInfo(String ID){
        Integer id = Integer.parseInt(ID) + 1;                      //Must increment by 1 to get correct row!
        db = this.getReadableDatabase();
        String query = "select id, name, email, trainer_email  from " + TABLE_NAME;    //Construct query message
        Cursor cursor = db.rawQuery(query, null);                   //Store results from query
        String name = "";                                                //Default values
        String userEmail = "";
        String trainerEmail = "";
        if (cursor.move(id)){                                       //Point to the entry corresponding to the provided username
            name = cursor.getString(1);                             //Get the associated name
            userEmail = cursor.getString(2);                        //Get associated user email address
            trainerEmail = cursor.getString(3);                     //Get associated trainer email address
            System.out.println(name);
            System.out.println(userEmail);
            System.out.println(trainerEmail);
        }

        EmailInformation info = new EmailInformation(name, userEmail, trainerEmail);

        return info;
    }

}
