package com.example.oulunchturku;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private boolean boolDone = false;

    public ListView lv_eatteri;
    public ListView lv_radio;

    public int dayofweek;

    private String eatteri_URL = "https://eatteri.fi:443";
    private String radio_URL = "https://www.herkkupiste.fi:443/lounaslista.html";

    ArrayList<HashMap<String, String>> eatteri_lunch;
    ArrayList<HashMap<String, String>> radio_lunch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("MainActivity", "onCreate triggered");

        setContentView(R.layout.activity_main); //activity_main

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(myToolbar);

        ActionBar ab = getSupportActionBar();

        TabLayout tabLayout = findViewById(R.id.tabs);
        ViewPager2 viewPager2 = findViewById(R.id.view_pager);

        ViewPagerAdapter VPadapter = new ViewPagerAdapter(this);
        viewPager2.setAdapter(VPadapter);

        new TabLayoutMediator(tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText("Tab " + (position + 1));
            }
        }).attach();

        tabLayout.getTabAt(0).setText("K-Rauta");
        tabLayout.getTabAt(1).setText("Radiometer");

        eatteri_lunch = new ArrayList<>();
        radio_lunch = new ArrayList<>();

        viewPager2.setOffscreenPageLimit(3);

        dayofweek = (LocalDate.now().getDayOfWeek().getValue())-1; //get day of week -1 because ListView starts from 0

        new GetLunch().execute();

    }
        @Override
        protected void onStart() {
            super.onStart();

            Log.d("MainActivity", "onStart triggered");

        }

    private class GetLunch extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this,"Loading data..." ,Toast.LENGTH_SHORT).show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {

            HtmlCleaner cleaner = new HtmlCleaner();

            CleanerProperties props = cleaner.getProperties();
            props.setRecognizeUnicodeChars(true);

            TagNode eNode = null;
            TagNode rNode = null;

            TagNode testinode = null;

            HttpHandler sh = new HttpHandler();

            String eatteriStr = sh.makeServiceCall(eatteri_URL);
            String radioStr = sh.makeServiceCall(radio_URL);

            List<String> weekdays = new ArrayList<String>();

            final String eweekdays ="//article//section//section//section//div//h2[@class='elementor-heading-title elementor-size-default']"; //Weekdays as string
            final String elunch ="//section//section//section//div//div[@class='elementor-widget-container']//p";  //K-RAUTA
            //final String elunch ="//section//section//section//div//div[@class='elementor-widget-container']//p";  //K-RAUTA


            final String rweekdays ="//div[@class='wp-block-herkkupiste-lunch-item']//div[1]//div//div//h3"; //WEEKDAYS FOR RADIOMETER
            final String rlunch ="//div[@class='wp-block-herkkupiste-lunch-item']//div[1]//div//div//p"; //LUNCH


            if (eatteriStr != null && radioStr != null) {
                eNode = cleaner.clean(eatteriStr);
                rNode = cleaner.clean(radioStr);

                try {

                    Object[] wdays = eNode.evaluateXPath(eweekdays);   //Eatteri days
                    Object[] foods = eNode.evaluateXPath(elunch);       //Eatteri lunch

                    int round = 0;
                    for (int i = 0; i<5;i++){

                        StringBuffer date_buffer = new StringBuffer();
                        StringBuffer lunch_buffer = new StringBuffer();
                        StringBuffer lunch_buffer_three = new StringBuffer();
                        HashMap<String, String> temp = new HashMap<>();

                        int foodperday=0;

                        TagNode resultNode = (TagNode)wdays[i];
                        getPlainText(date_buffer, resultNode, true);

                        resultNode = (TagNode)foods[i];
                        getPlainText(lunch_buffer, resultNode, false  );

/*                        for (int loopthree =0; foodperday<3; loopthree++) {

                            resultNode = (TagNode)foods[round];



                            getPlainText(lunch_buffer_three, resultNode, false  );

                                if (lunch_buffer_three.length() > 3) {
                                getPlainText(lunch_buffer, resultNode, false  );
                                    foodperday++;
                                }
                                else {}

                            round += 1;
                        }*/

                        if (date_buffer.length() !=0 && lunch_buffer.length() != 0){
                            temp.put("day", date_buffer.toString());
                            temp.put("lunch",lunch_buffer.toString());}
                        else {
                            temp.put("day", "EMPTY");
                            temp.put("lunch","EMPTY"); }

                        weekdays.add(date_buffer.toString());

                        eatteri_lunch.add(temp);
                    }

                    wdays = rNode.evaluateXPath(rweekdays);             //Radiometer weekdays
                    foods = rNode.evaluateXPath(rlunch);                //Radiometer lunch


                    for (int i = 0; i<5; i=i+1){

                        StringBuffer daybuffer = new StringBuffer();
                        StringBuffer lunchbuffer = new StringBuffer();

                        HashMap<String, String> temp = new HashMap<>();

                        TagNode lunchNode = (TagNode)wdays[i];
                        getPlainText(daybuffer, lunchNode, false  );

                        lunchNode = (TagNode)foods[i];   // get lunch
                        getPlainText(lunchbuffer, lunchNode, false  );

                    //    lunchbuffer.toString().replace("&auml;","ä");
                    //    temp.put("day", weekdays.get(round));
                    //    temp.put("day", daybuffer.toString());

                        if (daybuffer.length() != 0) {
                            temp.put("day", daybuffer.toString());}
                        else {
                            temp.put("day", "EMPTY");}


                        if (lunchbuffer.length() != 0) {
                            temp.put("lunch", lunchbuffer.toString());}
                        else {
                            temp.put("lunch", "EMPTY");}

                        radio_lunch.add(temp);
                    }



                } catch (XPatherException e) {
                    Log.d("GetLunch/doInBackground ", e.getMessage());
                }


            } else {
                Log.e("GetLunch ", "Couldn't get data from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get data from server!",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            ListAdapter adapter = new SimpleAdapter(MainActivity.this, eatteri_lunch,
                    R.layout.list_item, new String[]{ "day","lunch"},
                    new int[]{R.id.day, R.id.lunch});
            lv_eatteri = (ListView) findViewById(R.id.eatterilist);
            lv_eatteri.setAdapter(adapter);


           ListAdapter adapter2 = new SimpleAdapter(MainActivity.this, radio_lunch,
                    R.layout.list_item, new String[]{ "day","lunch"},
                    new int[]{R.id.day, R.id.lunch});
            lv_radio = (ListView) findViewById(R.id.radiolist);
            lv_radio.setAdapter(adapter2);

            if(dayofweek < 6) {
                lv_eatteri.setItemChecked(dayofweek, true);
                lv_radio.setItemChecked(dayofweek, true);
                lv_eatteri.setSelection(dayofweek);
                lv_radio.setSelection(dayofweek);

                lv_eatteri.setOnItemClickListener(null);

                Toast.makeText(MainActivity.this,"Data loaded..." ,Toast.LENGTH_SHORT).show();

            }
        }
    }


    private void getPlainText(StringBuffer buffer, Object node, Boolean addNewLine) {
        if (node instanceof ContentNode) {
            ContentNode contentNode = (ContentNode) node;
            String text = contentNode.getContent().toString();

            String fixed_html = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();

            if(addNewLine){
                buffer.append(fixed_html + "\n");}

        } else if (node instanceof TagNode) {
            TagNode tagNode = (TagNode) node;
            for (Object child : tagNode.getAllChildren()) {
                this.getPlainText(buffer, child, true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
/*            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;

            case R.id.action_favorite:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                return true;*/

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menusetup,menu);
        return super.onCreateOptionsMenu(menu);
    }
}


