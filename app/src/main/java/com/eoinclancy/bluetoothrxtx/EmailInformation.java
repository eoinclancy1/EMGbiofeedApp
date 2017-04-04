package com.eoinclancy.bluetoothrxtx;

/**
 * Created by eoin on 06/01/2017.
 * Used to store information regarding active user and their trainer
 */

public class EmailInformation {

    String userName;
    String userEmail;
    String trainerEmail;

    public EmailInformation(String n, String uEmail, String tEmail){
        userName = n;
        userEmail = uEmail;
        trainerEmail = tEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getTrainerEmail() {
        return trainerEmail;
    }

    public void setTrainerEmail(String trainerEmail) {
        this.trainerEmail = trainerEmail;
    }



}
