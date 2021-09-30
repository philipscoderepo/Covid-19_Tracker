package com.example.covid_19_tracker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.TestLooperManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.jsoup.parser.ParseError;
import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    class CaseInfo{
        String confirmed;
        String deaths;
        String recovered;
        //Aggregated confirmed cases that have not been resolved (Active cases = total cases - total recovered - total deaths)
        String active;
        //cases per 100,000 persons
        String incidentRate;
        //Number recorded deaths * 100/ Number confirmed cases.
        String caseFatalityRatio;
        //Total test results per 100,000 persons. The "total test results" are equal to "Total test results (Positive + Negative)"
        String testingRate;
        String totalTests;
    }

    class County{
        String FIPS;
        String county;
        String state;
        String lastUpdate;
        String latitude;
        String longitude;
        CaseInfo caseInfo;
    }

    class State{
        String state;
        String lastUpdate;
        String latitude;
        String longitude;
        CaseInfo caseInfo;
        Vector<County> counties;
    }

    //This table will be used to store bitmaps
    Hashtable<String, Bitmap> stateBitmaps;
    Bitmap usMapBitmap;
    Bitmap coloredMapBitmap;

    //This table will be used to find full state names using abbreviations
    Hashtable<String, String> stateTable; //Abrev, Full Name

    class UserData{
        String userCounty;
        String userState;
    }

    //County data
    Hashtable<String, County> counties;
    //State data
    Hashtable<String, State> states;
    int totalDeaths;

    UserData userData;

    final String countyDatabaseLink = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports/";
    final String stateDatabaseLink = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports_us/";
    final String pathName = "data/data/com.example.covid_19_tracker/files/";
    final String countyDataFileName = "countyData.csv";
    final String stateDataFileName = "stateData.csv";
    final String userDataFileName = "userData.txt";

    boolean countyDataLoaded = false;
    boolean stateDataLoaded = false;

    final int legendTop = 15000;
    final int legendBottom = 1000;
    Bitmap legend;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadCountyData();
        loadStateData();
        loadStateSpinnerData();
        loadUserData();
    }

    public void startLoad(){
        Thread load = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    loadMap();
                }
                ImageView mapImageView = findViewById(R.id.mapImageView);
                mapImageView.setImageBitmap(coloredMapBitmap);
            }
        });

        load.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Color generateHeatColor(String value){
        //Calculate what color on the legend the state will be
        double percent = (Double.parseDouble(value) - legendBottom) / (legendTop);
        int height = legend.getHeight();
        double yValue = height - height * percent;
        //Log.i("info", " yValue:" + Double.toString(yValue));
        Color color = new Color();
        if(yValue > height){
            color = Color.valueOf(40, 0, 43);
        }else if(yValue < 1){
            color = Color.valueOf(33, 89, 0);
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                color = legend.getColor(0, (int) yValue);
            }
        }
        return color;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void colorState(final Color color, final Bitmap state){
        //Loop through the pixels and replace the color with the state
        Thread coloring = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int y=0; y<usMapBitmap.getHeight(); y++){
                    for(int x=0; x<usMapBitmap.getWidth(); x++){
                        if((x < state.getWidth() && y < state.getHeight()) && state.getColor(x,y).alpha() >= 0.3f){
                            coloredMapBitmap.setPixel(x,y, color.toArgb());
                        }
                    }
                }
            }
        });

        coloring.start();
    }

    public void loadBitmaps(){
        //Very boiler plate code because I couldn't find a smarter solution....
        try{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                //Log.i("info", " State: Alabama");
                Bitmap stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.alabama);
                colorState(generateHeatColor(states.get("Alabama").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.alaska);
                colorState(generateHeatColor(states.get("Alaska").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.arizona);
                colorState(generateHeatColor(states.get("Arizona").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.arkansas);
                colorState(generateHeatColor(states.get("Arkansas").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.california);
                colorState(generateHeatColor(states.get("California").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.colorado);
                colorState(generateHeatColor(states.get("Colorado").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.connecticut);
                colorState(generateHeatColor(states.get("Connecticut").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.delaware);
                colorState(generateHeatColor(states.get("Delaware").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.florida);
                colorState(generateHeatColor(states.get("Florida").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.georgia);
                colorState(generateHeatColor(states.get("Georgia").caseInfo.incidentRate), stateBitmap);

                //Log.i("info", " State: Hawaii");
                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hawaii);
                colorState(generateHeatColor(states.get("Hawaii").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.idaho);
                colorState(generateHeatColor(states.get("Idaho").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.illinois);
                colorState(generateHeatColor(states.get("Illinois").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.indiana);
                colorState(generateHeatColor(states.get("Indiana").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.iowa);
                colorState(generateHeatColor(states.get("Iowa").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.kansas);
                colorState(generateHeatColor(states.get("Kansas").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.kentucky);
                colorState(generateHeatColor(states.get("Kentucky").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.louisiana);
                colorState(generateHeatColor(states.get("Louisiana").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.maine);
                colorState(generateHeatColor(states.get("Maine").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.maryland);
                colorState(generateHeatColor(states.get("Maryland").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.massachusetts);
                colorState(generateHeatColor(states.get("Massachusetts").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.michigan);
                colorState(generateHeatColor(states.get("Michigan").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.minnesota);
                colorState(generateHeatColor(states.get("Minnesota").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mississippi);
                colorState(generateHeatColor(states.get("Mississippi").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.missouri);
                colorState(generateHeatColor(states.get("Missouri").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.montana);
                colorState(generateHeatColor(states.get("Montana").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.nebraska);
                colorState(generateHeatColor(states.get("Nebraska").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.nevada);
                colorState(generateHeatColor(states.get("Nevada").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.new_hampshire);
                colorState(generateHeatColor(states.get("New Hampshire").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.new_jersey);
                colorState(generateHeatColor(states.get("New Jersey").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.new_mexico);
                colorState(generateHeatColor(states.get("New Mexico").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.new_york);
                colorState(generateHeatColor(states.get("New York").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.north_carolina);
                colorState(generateHeatColor(states.get("North Carolina").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.north_dakota);
                colorState(generateHeatColor(states.get("North Dakota").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ohio);
                colorState(generateHeatColor(states.get("Ohio").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.oklahoma);
                colorState(generateHeatColor(states.get("Oklahoma").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.oregon);
                colorState(generateHeatColor(states.get("Oregon").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pennsylvania);
                colorState(generateHeatColor(states.get("Pennsylvania").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rhode_island);
                colorState(generateHeatColor(states.get("Rhode Island").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.south_carolina);
                colorState(generateHeatColor(states.get("South Carolina").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.south_dakota);
                colorState(generateHeatColor(states.get("South Dakota").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tennessee);
                colorState(generateHeatColor(states.get("Tennessee").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.texas);
                colorState(generateHeatColor(states.get("Texas").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.utah);
                colorState(generateHeatColor(states.get("Utah").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.vermont);
                colorState(generateHeatColor(states.get("Vermont").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.virginia);
                colorState(generateHeatColor(states.get("Virginia").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.washington);
                colorState(generateHeatColor(states.get("Washington").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.west_virginia);
                colorState(generateHeatColor(states.get("West Virginia").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wisconsin);
                colorState(generateHeatColor(states.get("Wisconsin").caseInfo.incidentRate), stateBitmap);

                stateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wyoming);
                colorState(generateHeatColor(states.get("Wyoming").caseInfo.incidentRate), stateBitmap);

                Bitmap lines = BitmapFactory.decodeResource(getResources(), R.drawable.map_lines);
                for(int y=0; y<usMapBitmap.getHeight(); y++){
                    for(int x=0; x<usMapBitmap.getWidth(); x++){
                        if((x < lines.getWidth() && y < lines.getHeight()) && lines.getColor(x,y).alpha() >= 0.25f){
                            coloredMapBitmap.setPixel(x,y, Color.argb(1.0f,0,0, 0));
                        }
                    }
                }
            }
        }catch (NullPointerException n){
            n.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void loadMap(){
        try{
            InputStream is = getResources().openRawResource(R.raw.imagelist);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            //Read in the image names
            List<String> imageList = new ArrayList<>();
            String line;
            while((line = reader.readLine()) != null){
                imageList.add(line);
            }
            reader.close();
            is.close();

            usMapBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.map);
            coloredMapBitmap = usMapBitmap.copy(Bitmap.Config.ARGB_8888, true);
            legend = BitmapFactory.decodeResource(getResources(), R.drawable.legend);

            loadBitmaps();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadStateSpinnerData(){
        final List<String> stateList = new ArrayList<>();
        Thread fetch = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Get the raw stateList file
                    InputStream is = getResources().openRawResource(R.raw.statelist);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    stateTable = new Hashtable<>();
                    String line;
                    while((line = reader.readLine()) != null){
                        String[] s = line.split(",");
                        stateList.add(s[1]);
                        stateTable.put(s[1], s[0]);
                    }
                    reader.close();
                    is.close();
                    //Set the spinner data using the stateList
                    setSpinnerData(stateList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        fetch.start();

    }

    public void setSpinnerData(List<String> stateList){
        //Find the spinner object
        Spinner stateSpinner = findViewById(R.id.stateSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, stateList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Set the items in the spinner
        stateSpinner.setAdapter(adapter);
    }

    public void loadCountyData() {
        try{
            Thread fetch = new Thread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.R)
                @Override
                public void run() {
                    try {
                        //Try will always check the current date, then if it fails we search to find the most recent file
                        //Check to see if there has been an update to the database
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
                        LocalDateTime now = LocalDateTime.now();
                        URL url = new URL(countyDatabaseLink + dateTimeFormatter.format(now) +".csv");
                        BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
                        String i;
                        //If the file exists and no error is thrown, then load the new data
                        Log.i("Info", "New data, using current date");
                        //Read until the line before Alabama which in this file is Turkey
                        while((i = read.readLine()) != null){
                            if(i.contains("Turkey"))break;
                        }
                        Log.i("Info", i);

                        //use the pathname to locate the file directory
                        File dir = new File(pathName);
                        //use the pathname and datafile name to locate the data file
                        File dataFile = new File(pathName+ countyDataFileName);
                        //Check to see if the file exists
                        if(!dataFile.exists()){
                            //If not then make the directory and the new file
                            dir.mkdirs();
                            dataFile.createNewFile();
                        }
                        else{
                            //Overwrite the file
                            dataFile.delete();
                            dataFile.createNewFile();
                        }
                        //Open a stream to write to the data file
                        FileOutputStream fileOut = new FileOutputStream(dataFile);
                        OutputStreamWriter oStream = new OutputStreamWriter(fileOut);
                        //Write
                        while((i = read.readLine()) != null){
                            oStream.write(i);
                            oStream.write("\n");
                            if(i.contains("56045,Weston"))break;
                        }
                        Log.i("Info", i);
                        oStream.close();
                        fileOut.close();
                        read.close();
                    }
                    catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.i("Info", "No update, using old link");
                        try{
                            //The most recent date doesn't exist
                            //Send today's date and work back from there
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
                            LocalDateTime now = LocalDateTime.now();
                            //Search the database for a valid date
                            String validDate = searchDatabase(countyDatabaseLink, dateTimeFormatter.format(now));
                            Log.i("Info", validDate);
                            URL url = new URL(countyDatabaseLink + validDate +".csv");
                            BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
                            //Read until Turkey
                            String i;
                            while((i = read.readLine()) != null){
                                if(i.contains("Turkey"))break;
                            }
                            Log.i("Info", i);

                            File dir = new File(pathName);
                            File dataFile = new File(pathName + countyDataFileName);
                            if(!dataFile.exists()){
                                dir.mkdirs();
                                dataFile.createNewFile();
                            }
                            else{
                                dataFile.delete();
                                dataFile.createNewFile();
                            }

                            FileOutputStream fileOut = new FileOutputStream(dataFile);
                            OutputStreamWriter oStream = new OutputStreamWriter(fileOut);

                            while((i = read.readLine()) != null){
                                oStream.write(i);
                                oStream.write("\n");
                                if(i.contains("56045,Weston"))break;
                            }
                            Log.i("Info", i);
                            oStream.close();
                            fileOut.close();
                            read.close();

                        } catch (FileNotFoundException fileNotFoundException) {
                            fileNotFoundException.printStackTrace();
                        } catch (MalformedURLException malformedURLException) {
                            malformedURLException.printStackTrace();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        } catch (ParseException parseException) {
                            parseException.printStackTrace();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try{
                        //Read the data into the table for searching
                        counties = new Hashtable<String, County>();
                        BufferedReader reader = new BufferedReader(new FileReader(pathName + countyDataFileName));
                        //ss will store the line
                        String ss;
                        while((ss = reader.readLine()) != null){
                            //s will hold the split data
                            String[] s = ss.split(",");
                            County county = new County();
                            if(s.length > 0 && s[0] != null){county.FIPS = s[0];}
                            if(s.length > 1 && s[1] != null){county.county = s[1].toUpperCase();}
                            if(s.length > 2 && s[2] != null){county.state = s[2];}
                            if(s.length > 4 && s[4] != null){county.lastUpdate = s[4];}
                            if(s.length > 5 && s[5] != null){county.latitude = s[5];}
                            if(s.length > 6 && s[6] != null){county.longitude = s[6];}
                            county.caseInfo = new CaseInfo();
                            if(s.length > 7 && s[7] != null){county.caseInfo.confirmed = s[7];}
                            if(s.length > 8 && s[8] != null){county.caseInfo.deaths = s[8];}
                            if(s.length > 9 && s[9] != null){county.caseInfo.recovered = s[9];}
                            if(s.length > 10 && s[10] != null){county.caseInfo.active = s[10];}
                            if(s.length > 14 && s[14] != null){county.caseInfo.incidentRate = s[14];}
                            if(s.length > 15 && s[15] != null){county.caseInfo.caseFatalityRatio = s[15];}
                            counties.put(county.county  + " , " + county.state, county);
                        }
                        Log.i("Info", "Reader closed");
                        reader.close();

                        countyDataLoaded = true;
                        final Button loadDataButton = findViewById(R.id.loadDataButton);
                        if(stateDataLoaded && countyDataLoaded){
                            MainActivity.this.runOnUiThread(new Runnable(){
                                public void run(){
                                    loadDataButton.setEnabled(true);
                                }
                            });
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

            fetch.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadStateData(){
        try{
            Thread fetch = new Thread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.R)
                @Override
                public void run() {
                    try {
                        //Try will always check the current date, then if it fails we search to find the most recent file
                        //Check to see if there has been an update to the database
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
                        LocalDateTime now = LocalDateTime.now();
                        URL url = new URL(stateDatabaseLink + dateTimeFormatter.format(now) +".csv");
                        BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
                        //If the file exists and no error is thrown, then load the new data
                        Log.i("Info", "New data, using current date");
                        //use the pathname to locate the file directory
                        File dir = new File(pathName);
                        //use the pathname and datafile name to locate the data file
                        File dataFile = new File(pathName+ stateDataFileName);
                        //Check to see if the file exists
                        if(!dataFile.exists()){
                            //If not then make the directory and the new file
                            dir.mkdirs();
                            dataFile.createNewFile();
                        }
                        else{
                            //Overwrite the file
                            dataFile.delete();
                            dataFile.createNewFile();
                        }
                        //Open a stream to write to the data file
                        FileOutputStream fileOut = new FileOutputStream(dataFile);
                        OutputStreamWriter oStream = new OutputStreamWriter(fileOut);
                        //Write
                        String i;
                        while((i = read.readLine()) != null){
                            oStream.write(i);
                            oStream.write("\n");
                        }
                        oStream.close();
                        fileOut.close();
                        read.close();

                    }
                    catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.i("Info", "No update, using old link");
                        try{
                            //The most recent date doesn't exist
                            //Send today's date and work back from there
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
                            LocalDateTime now = LocalDateTime.now();
                            //Search the database for a valid date
                            String validDate = searchDatabase(stateDatabaseLink, dateTimeFormatter.format(now));
                            Log.i("Info", validDate);
                            URL url = new URL(stateDatabaseLink + validDate +".csv");
                            BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));

                            File dir = new File(pathName);
                            File dataFile = new File(pathName + stateDataFileName);
                            if(!dataFile.exists()){
                                dir.mkdirs();
                                dataFile.createNewFile();
                            }
                            else{
                                dataFile.delete();
                                dataFile.createNewFile();
                            }

                            FileOutputStream fileOut = new FileOutputStream(dataFile);
                            OutputStreamWriter oStream = new OutputStreamWriter(fileOut);

                            String i;
                            while((i = read.readLine()) != null){
                                oStream.write(i);
                                oStream.write("\n");
                            }
                            oStream.close();
                            fileOut.close();
                            read.close();

                        } catch (FileNotFoundException fileNotFoundException) {
                            fileNotFoundException.printStackTrace();
                        } catch (MalformedURLException malformedURLException) {
                            malformedURLException.printStackTrace();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        } catch (ParseException parseException) {
                            parseException.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try{
                        //Read the data into the table for searching
                        states = new Hashtable<String, State>();
                        BufferedReader reader = new BufferedReader(new FileReader(pathName + stateDataFileName));
                        //ss will store the line
                        String ss;
                        while((ss = reader.readLine()) != null){
                            //s will hold the split data
                            String[] s = ss.split(",");
                            State state = new State();
                            if(s.length > 0 && s[0] != null){state.state = s[0];}
                            if(s.length > 2 && s[2] != null){state.lastUpdate = s[2];}
                            if(s.length > 3 && s[3] != null){state.latitude = s[3];}
                            if(s.length > 4 && s[4] != null){state.longitude = s[4];}
                            state.caseInfo = new CaseInfo();
                            if(s.length > 5 && s[5] != null){state.caseInfo.confirmed = s[5];}
                            if(s.length > 6 && s[6] != null){
                                state.caseInfo.deaths = s[6];
                                try{
                                    totalDeaths += Integer.parseInt(s[6]);
                                }catch (NumberFormatException n){
                                    n.printStackTrace();
                                }
                            }
                            if(s.length > 7 && s[7] != null){state.caseInfo.recovered = s[7];}
                            if(s.length > 8 && s[8] != null){state.caseInfo.active = s[8];}
                            if(s.length > 10 && s[10] != null){state.caseInfo.incidentRate = s[10];}
                            if(s.length > 11 && s[11] != null){state.caseInfo.totalTests = s[11];}
                            if(s.length > 13 && s[13] != null){state.caseInfo.caseFatalityRatio = s[13];}
                            if(s.length > 16 && s[16] != null){state.caseInfo.testingRate = s[16];}
                            states.put(state.state, state);
                        }
                        Log.i("Info", "Reader closed");
                        reader.close();
                        TextView totalDeathsView = findViewById(R.id.unitedStatesDeathsTextView);
                        totalDeathsView.setText(Integer.toString(totalDeaths));

                        stateDataLoaded = true;
                        final Button loadDataButton = findViewById(R.id.loadDataButton);
                        if(stateDataLoaded && countyDataLoaded){
                            MainActivity.this.runOnUiThread(new Runnable(){
                                public void run(){
                                    loadDataButton.setEnabled(true);
                                }
                            });
                        }

                        startLoad();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            fetch.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String searchDatabase(String database, String date) throws ParseException {
        try{
            //Try the date to see if there is a valid file at the url
            URL url = new URL(database + date + ".csv");
            BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
            //If there is the log will print the valid date and return the date as a string
            Log.i("Info", "Valid date at: " + date);
            return date;
        }catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
            //In order to easily traverse the dates I used a calendar
            Calendar calendar = Calendar.getInstance();
            //Create a formater for the date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
            //Parse the date that failed the openStream test
            Date myDate = dateFormat.parse(date);
            //Then set the calendar's current date to that date
            calendar.setTime(myDate);
            //Then move back one calendar day
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            //Get the new date
            myDate = calendar.getTime();
            Log.i("Info", "Searching for this: " + dateFormat.format(myDate));
            //Recursively call the date until a valid date is found
            date = searchDatabase(database, dateFormat.format(myDate));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return date;
        }
    }

    //Not in use
    public void loadUserData(){
        userData = new UserData();
        try{
            //Check to see if a saved file exists
            //use the pathname to locate the file directory
            File dir = new File(pathName);
            //use the pathname and datafile name to locate the data file
            File dataFile = new File(pathName+ userDataFileName);
            //Check to see if the file exists. if so then load from there otherwise do nothing
            if(dataFile.exists()){
                BufferedReader reader = new BufferedReader(new FileReader(pathName + userDataFileName));
                String ss = reader.readLine();
                String[] s = new String[2];
                if(ss != null){s = ss.split(",");}
                if(s[0] != null){userData.userCounty = s[0];}
                if(s[1] != null){userData.userState = s[1];}
                reader.close();
            }

            if(userData.userCounty != null && userData.userState != null){
                Spinner stateSpinner = findViewById(R.id.stateSpinner);
                TextView userCounty = findViewById(R.id.countyNameText);

                stateSpinner.setSelection(5);
                //Trim off whitespace
                //userCounty.setText(userData.userCounty.trim());
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Not in use
    public void writeUserData(){
        try{
            //use the pathname to locate the file directory
            File dir = new File(pathName);
            //use the pathname and datafile name to locate the data file
            File dataFile = new File(pathName+ userDataFileName);
            //Check to see if the file exists
            if(!dataFile.exists()){
                //If not then make the directory and the new file
                dir.mkdirs();
                dataFile.createNewFile();
            }
            else{
                //Overwrite the file
                dataFile.delete();
                dataFile.createNewFile();
            }
            //Open a stream to write to the data file
            FileOutputStream fileOut = new FileOutputStream(dataFile);
            OutputStreamWriter oStream = new OutputStreamWriter(fileOut);

            oStream.write(userData.userCounty + " , " + userData.userState);
            oStream.close();
            fileOut.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void displayData(View v){
        ToggleButton dataToggleButton = findViewById(R.id.dataToggleButton);
        //Get the county from the text field
        TextView userCounty = findViewById(R.id.countyNameText);
        //Get the state abreviation spinner
        Spinner stateSpinner = findViewById(R.id.stateSpinner);
        //Get the current selection of the spinner
        String stateAbv = stateSpinner.getSelectedItem().toString();
        //Set the user data object
        userData.userCounty = userCounty.getText().toString();
        String userState = stateTable.get(stateAbv);
        userData.userState = userState;
        Log.i("Info", userData.userCounty);
        //Log.i("Info", Integer.toString(counties.size()));
        //isChecked is State
        if(dataToggleButton.isChecked()){
            updateTextState(userState);
        }else{
            //Format the search to match the key in the counties hashtable
            String search = userData.userCounty.toUpperCase() + " , " + userState;
            updateTextCounty(search);

        }

    }

    @SuppressLint("SetTextI18n")
    public void updateTextCounty(String search){
        //Load the textviews for the data
        TextView deaths = findViewById(R.id.deathsTextView);
        TextView confirmed = findViewById(R.id.confirmedTextView);
        //Active and Recovered are both not being updated by the database currently
        TextView recovered = findViewById(R.id.recoveredTextView);
        TextView active = findViewById(R.id.activeCasesTextView);
        TextView incidentRate = findViewById(R.id.incidentRateTextView);
        TextView lastUpdateTextView = findViewById(R.id.lastUpdateTextView);
        //Log.i("Info", search);
        if(counties.get(search) != null){
            if(counties.get(search).caseInfo.confirmed != null){
                confirmed.setText("Cases Confirmed: " + counties.get(search).caseInfo.confirmed);
            }else {
                confirmed.setText("Cases Confirmed: Data not available");
            }
            if(counties.get(search).caseInfo.deaths != null){
                deaths.setText("Deaths: " + counties.get(search).caseInfo.deaths);
            }else{
                deaths.setText("Deaths: Data not available");
            }
            if(counties.get(search).caseInfo.recovered != null){
                recovered.setText("Recovered: " + counties.get(search).caseInfo.recovered);
            }else{
                recovered.setText("Recovered: Data not available");
            }
            if(counties.get(search).caseInfo.active != null){
                active.setText("Active Cases: " + counties.get(search).caseInfo.active);
            }else{
                active.setText("Active Cases: Data not available");
            }
            if(counties.get(search).caseInfo.incidentRate != null){
                incidentRate.setText("Incident Rate / 100k: " + counties.get(search).caseInfo.incidentRate);
            }else{
                incidentRate.setText("Incident Rate: Data not available");
            }
            if(counties.get(search).lastUpdate != null){
                lastUpdateTextView.setText(counties.get(search).lastUpdate + " UTC");
            }

        }
    }

    @SuppressLint("SetTextI18n")
    public void updateTextState(String search){
        //Load the textviews for the data
        TextView deaths = findViewById(R.id.deathsTextView);
        TextView confirmed = findViewById(R.id.confirmedTextView);
        //Active and Recovered are both not being updated by the database currently
        TextView recovered = findViewById(R.id.recoveredTextView);
        TextView active = findViewById(R.id.activeCasesTextView);
        TextView incidentRate = findViewById(R.id.incidentRateTextView);
        TextView lastUpdateTextView = findViewById(R.id.lastUpdateTextView);
        Log.i("Info", search);
        if(states.get(search) != null){
            if(states.get(search).caseInfo.confirmed != null){
                confirmed.setText("Cases Confirmed: " + states.get(search).caseInfo.confirmed);
            }else {
                confirmed.setText("Cases Confirmed: Data not available");
            }
            if(states.get(search).caseInfo.deaths != null){
                deaths.setText("Deaths: " + states.get(search).caseInfo.deaths);
            }else{
                deaths.setText("Deaths: Data not available");
            }
            if(states.get(search).caseInfo.recovered != null){
                recovered.setText("Recovered: " + states.get(search).caseInfo.recovered);
            }else{
                recovered.setText("Recovered: Data not available");
            }
            if(states.get(search).caseInfo.active != null){
                active.setText("Active Cases: " + states.get(search).caseInfo.active);
            }else{
                active.setText("Active Cases: Data not available");
            }
            if(states.get(search).caseInfo.incidentRate != null){
                incidentRate.setText("Incident Rate / 100k: " + states.get(search).caseInfo.incidentRate);
            }else{
                incidentRate.setText("Incident Rate: Data not available");
            }
            if(states.get(search).lastUpdate != null){
                lastUpdateTextView.setText(states.get(search).lastUpdate + " UTC");
            }

        }
    }

    public void CountyNameTextEnable(View v){
        TextView userCounty = findViewById(R.id.countyNameText);
        if(userCounty.isEnabled()){
            userCounty.setEnabled(false);
        }else{
            userCounty.setEnabled(true);
        }
    }
}