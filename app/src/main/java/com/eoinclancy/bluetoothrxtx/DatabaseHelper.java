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

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "contacts.db";
    private static final String TABLE_NAME = "contacts";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_UNAME = "uname";
    private static final String COLUMN_PASS = "pass";
    private static final String COLUMN_TRAINEREMAIL = "trainer_email";
    SQLiteDatabase db;
    private static final String TABLE_CREATE = "create table contacts (id integer primary key not null , " +
            "name text not null, email text not null, uname text not null, pass text not null, trainer_email text not null);";       //'contacts' could be written as TABLE_NAME

    /* Constructor could take (Context context, String name, SQLiteDatabase.CursorFactory factory, int version, SQLiteDatabase db) to be fully customisable - not needed */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override   //Must be implemented from parent class
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        this.db = db;
    }

    @Override   //Must be implemented from parent class
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(query);
        this.onCreate(db);
        /* Use this style for adding a new column to the SQLite DB */
        //if (newVersion > oldVersion) {
        //    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_TRAINEREMAIL + " INTEGER DEFAULT 0");
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

        db.insert(TABLE_NAME, null, values);            //inserts the contact object into the database
        db.close();
    }

    public String searchPass(String uname){
        db = this.getReadableDatabase();
        String query = "select uname, pass from " + TABLE_NAME; //getting the corresponding password for a username
        Cursor cursor = db.rawQuery(query, null);
        String a,b;                                             //a = username, b = password
        b = "not found";                                        //default value for case where password not found
        if(cursor.moveToFirst()){
            do{                                                 //Sequentially searching through all database enteries
                a = cursor.getString(0);                        //See query to see why locations are 0 and 1

                if(a.equals(uname)){
                    b = cursor.getString(1);
                    break;
                }
            }
            while(cursor.moveToNext());
        }
        return b;
    }
}
