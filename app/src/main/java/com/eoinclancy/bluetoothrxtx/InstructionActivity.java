package com.eoinclancy.bluetoothrxtx;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InstructionActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private int[] layouts;
    private TextView[] dots;
    private ViewPagerAdapter viewPagerAdapter;
    Button next, skip;
    private LinearLayout dotsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruction);

        if(Build.VERSION.SDK_INT >= 21){      //Make status bar transparent
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE| View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_instruction);

        viewPager = (ViewPager)findViewById(R.id.view_pager);
        dotsLayout = (LinearLayout)findViewById(R.id.layoutDots);
        skip = (Button)findViewById(R.id.skip_btn);
        next = (Button)findViewById(R.id.next_btn);

        layouts = new int[]{R.layout.inst_screen_1, R.layout.inst_screen_2, R.layout.inst_screen_3,
                            R.layout.inst_screen_4, R.layout.inst_screen_5, R.layout.inst_screen_6,
                            R.layout.inst_screen_7, R.layout.inst_screen_8, R.layout.inst_screen_9,
                            R.layout.inst_screen_10};
        addDotsBottom(0);
        changeStatusBarColour();
        viewPagerAdapter = new ViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(viewListener);                //Needed modification of gradle file - compile 'com.android.support:support-v4:22.2.0'

        skip.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent i = new Intent(InstructionActivity.this, DeviceListActivity.class);
                i.putExtra("previous","InstructionActivity.class");
                startActivity(i);
            }
        });

        next.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                int current = getItem(+1);
                if(current<layouts.length){
                    viewPager.setCurrentItem(current);
                }
                else{
                    Intent i = new Intent(InstructionActivity.this, DeviceListActivity.class);
                    i.putExtra("previous","InstructionActivity.class");
                    startActivity(i);   // Add in the activity in this section that you want to go to
                }
            }
        });

    }

    private void addDotsBottom(int position){
        dots = new TextView[layouts.length];
        int[] colourActive = getResources().getIntArray(R.array.dot_active);
        int[] colourInactive = getResources().getIntArray(R.array.dot_inactive);
        dotsLayout.removeAllViews();
        for(int i=0; i<dots.length; i++){
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colourInactive[position]);
            dotsLayout.addView(dots[i]);
        }

        if(dots.length>0){
            dots[position].setTextColor(colourActive[position]);
        }

    }

    private int getItem(int i){
        return viewPager.getCurrentItem() + 1;
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addDotsBottom(position);
            if(position == layouts.length-1){               //Reached the last page
                next.setText("PROCEED");
                skip.setVisibility((View.GONE));
            }
            else{
                next.setText("NEXT");
                skip.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private void changeStatusBarColour(){
        if(Build.VERSION.SDK_INT== Build.VERSION_CODES.LOLLIPOP){
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    public class ViewPagerAdapter extends PagerAdapter {

        private LayoutInflater layoutInflater;

        public Object instantiateItem(ViewGroup container, int position){
            layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = layoutInflater.inflate(layouts[position], container, false);
            container.addView(v);
            return v;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View v = (View)object;
            container.removeView(v);
        }
    }
}
