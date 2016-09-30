package com.eoinclancy.bluetoothrxtx;

/**
 * Created by eoin on 30/09/2016.
 * Stores all information about an individual exercise
 */
public class Exercise {
    private String excercise;
    private String status;
    private int iconID;

    public Exercise(String excercise, String status, int iconID){
        this.excercise = excercise;
        this.status = status;
        this.iconID = iconID;
    }

    public String getExcercise() {
        return excercise;
    }

    public String getstatus() {
        return status;
    }

    public int getIconID() {
        return iconID;
    }
}
