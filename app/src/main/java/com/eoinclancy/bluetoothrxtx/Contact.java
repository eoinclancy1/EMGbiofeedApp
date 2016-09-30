package com.eoinclancy.bluetoothrxtx;

/**
 * Created by eoin on 29/09/2016.
 */
public class Contact {
    String name, email, uname, pass, trainerEmail;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }


    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUname() {
        return this.uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getPass() {
        return this.pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getTrainerEmail(){
        return this.trainerEmail;
    }

    public void setTrainerEmail(String trainerEmail){
        this.trainerEmail = trainerEmail;
    }

}
