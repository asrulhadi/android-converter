package com.mantaray.android.converter;

import android.app.Activity;
import android.app.VoiceInteractor;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MainActivity extends Activity {

    public static final String PREFS_NAME = "CONVERTER";
    private static final String DATABASE_NAME = "converter.db";
    private static final String TABLE_NAME = "conversion";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    ///// request to the network using the Volley Framework
    ///// For more details see https://developer.android.com/training/volley/index.html
    //// query https://www.islamicfinder.org/islamic-date-converter/convertdate?day=13&month=11&year=2016&dateType=Gregorian
    //// response ==> see below
    public void startRequest(View v) {
        // gate the date from the widget
        DatePicker dp = (DatePicker)findViewById(R.id.datePicker);
        int d = dp.getDayOfMonth();
        int m = dp.getMonth();
        int y = dp.getYear();

        String baseurl = "https://www.islamicfinder.org/islamic-date-converter/convertdate";
        String url = String.format("%s?day=%d&month=%d&year=%d&dateType=Gregorian", baseurl,d,m,y);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        // can enable the button
                        toogleConvertBtn(true);
                        setHijri(response);
                        //mTxtDisplay.setText("Response: " + response.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError e) {
                        // TODO Auto-generated method stub
                        Log.d("debug","Error in Volley");
                        toogleConvertBtn(true);
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Access the RequestQueue through your singleton class.
        VolleySingleton.getInstance(this).addToRequestQueue(jsObjRequest);

        // wait until the response arrive. Disable the button
        toogleConvertBtn(false);
    }

    private void setHijri(JSONObject resp) {
        // received the response
        // need to convert the json and parse it
        // {"convertedDate":"22nd Rabi Al-Akhar, 1437h","convertedDay":"Monday","gregorianDate":"01","gregorianMonth":1,"gregorianYear":"2016","hijriDate":"22","hijriMonth":4,"hijriYear":"1437"}
        try {
            ((TextView) findViewById(R.id.h_month)).setText(resp.getString("convertedDate"));
        } catch (Exception e) {
            Log.d("debug",e.toString());
        }
    }

    private void toogleConvertBtn(boolean b) {
        Button btn = (Button)findViewById(R.id.btn_getit);
        // change status
        btn.setEnabled(b);
        // change the text
        if (b) btn.setText("Convert");
        else btn.setText("Please Wait...");
    }

    public void saveDate(View v) {
        Toast.makeText(this, "Saving conversion", Toast.LENGTH_LONG).show();
        DatePicker dp = (DatePicker)findViewById(R.id.datePicker);
        Calendar c = new GregorianCalendar(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());

        // date in string
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String g_date = formatter.format(c.getTime());// shared
        String h_date = ((TextView)findViewById(R.id.h_month)).getText().toString();

        // check where to save
        boolean sharedPref = ((CheckBox)findViewById(R.id.chk_pref)).isChecked();
        if ( sharedPref ) {
            // in shared preferences -- /data/data/PACKAGE_NAME/shared_prefs/PREFS_NAME.xml
            // We need an Editor object to make preference changes.
            // All objects are from android.context.Context
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(g_date, h_date);
            editor.commit();
        }
        boolean sqlite = ((CheckBox)findViewById(R.id.chk_sql)).isChecked();
        if ( sqlite ) {
            AsyncTask<String, Void, Boolean> mytask = new AsyncTask<String, Void, Boolean>() {
                DBHelper dbHelper = new DBHelper(MainActivity.this);
                SQLiteDatabase db;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    DBHelper dbhelper = new DBHelper(MainActivity.this);
                }

                @Override
                protected Boolean doInBackground(String... params) {
                    try {
                        String g_date = params[0];
                        String h_date = params[1];
                        db = dbHelper.getWritableDatabase();
                        // prepare value to insert
                        ContentValues value = new ContentValues();
                        value.put("gregorian", g_date);
                        value.put("hijri", h_date);
                        // now can insert the value
                        long i = db.insert(TABLE_NAME, null, value);
                        Log.d("LocaleSetting", "Result from inserting " + i);
                        // close db
                        db.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new Boolean(false);
                    }
                    return new Boolean(true);
                }

                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    super.onPostExecute(aBoolean);
                    if (aBoolean.booleanValue()) {
                        Toast.makeText(MainActivity.this, "Value inserted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            mytask.execute(g_date, h_date);
        }
    }

    private class DBHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 1;
        private static final String DICTIONARY_TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        "gregorian TEXT PRIMARY KEY, hijri TEXT);";

        DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DICTIONARY_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
