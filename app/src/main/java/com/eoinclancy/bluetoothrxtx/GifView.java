package com.eoinclancy.bluetoothrxtx;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

/**
 * Created by eoin on 05/01/2017.
 * Class handles the display of a GIF image
 */

public class GifView extends View {

    private InputStream gifInputStream;
    private Movie gifMovie;
    private int movieWidth, movieHeight;
    private long movieDur;
    private long movieStart;

    public GifView(Context context){
        super(context);
        if(!isInEditMode())
            init(context);
    }

    public GifView(Context context, AttributeSet attrs){
        super(context, attrs);
        if(!isInEditMode())
            init(context);
    }

    public GifView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        if(!isInEditMode())
            init(context);
    }


    private void init(Context context){
        setFocusable(true);

        //Using shared preferences to get the value of the GIF to display
        SharedPreferences sharedPref = context.getSharedPreferences("GIF_Status",Context.MODE_PRIVATE);
        String GIFresult = sharedPref.getString("Status",null);
        Integer displayGIF = Integer.parseInt(GIFresult);

        switch (displayGIF){
            case 0:
                gifInputStream = context.getResources().openRawResource(R.raw.fail);
                break;
            case 1:
                gifInputStream = context.getResources().openRawResource(R.raw.betterlucknexttime);
                break;
            case 2:
                gifInputStream = context.getResources().openRawResource(R.raw.applause);
                break;
            default:
                gifInputStream = context.getResources().openRawResource(R.raw.applause);
                break;
        }



        gifMovie = Movie.decodeStream(gifInputStream);
        movieWidth = gifMovie.width();
        movieHeight = gifMovie.height();
        movieDur = gifMovie.duration();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        setMeasuredDimension(movieWidth,movieHeight);
    }

    public int getMovieWidth() {
        return movieWidth;
    }

    public int getMovieHeight(){
        return movieHeight;
    }

    public long getMovieDur(){
        return movieDur;
    }

    //Allows for continuous looping of the GIF
    protected void onDraw(Canvas canvas){

        long now = SystemClock.uptimeMillis();


        if(movieStart == 0){
            movieStart = now;
        }
        if(gifMovie != null) {
            int dur = gifMovie.duration();
            if (dur == 0) {
                dur = 1000;
            }

            int relTime = (int) ((now - movieStart) % dur);

            gifMovie.setTime(relTime);
            gifMovie.draw(canvas, 0, 0);
            invalidate();
        }

    }

}
