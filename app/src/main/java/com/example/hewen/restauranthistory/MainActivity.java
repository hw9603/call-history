package com.example.hewen.restauranthistory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.text.SimpleDateFormat;

import android.os.AsyncTask;
import android.util.Log;
import android.provider.CallLog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.content.ContentResolver;
import android.database.Cursor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {
    private ListView mLvShow;
    private List<Map<String, String>> dataList;
    private SimpleAdapter adapter;
    private static final String TAG = "ASYNC_TASK";

    private Button execute;
    private Button cancel;
    private MyTask mTask;
    private ProgressBar progressBar;
    private TextView textView;
    private List<String> phoneNumberList;
    private Button place;

    private String comInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String google_url_front = "https://maps.googleapis.com/maps/api/place/search/json?";
        final String google_url_back = "&key=AIzaSyDt3UWjvleAATeUilvewMm5gYpTDCsNgeY";
        final String wp_url_front = "https://proapi.whitepages.com/3.0/phone?phone=";
        final String wp_url_back = "&api_key=0a72b3db937a46c28eb1db04df8eab54";
        final String phone_number;
        execute = (Button) findViewById(R.id.execute);
        cancel = (Button) findViewById(R.id.cancel);
        place = (Button) findViewById(R.id.placeType);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        textView = (TextView) findViewById(R.id.text_view);
        place.setEnabled(false);

        phoneNumberList = getPhoneNumberList();

        execute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTask = new MyTask();
                //mTask.execute("https://proapi.whitepages.com/3.0/phone?phone=7347692748&api_key=0a72b3db937a46c28eb1db04df8eab54");
                mTask.execute("http://www.baidu.com");
                mTask = new MyTask();
                execute.setEnabled(false);
                cancel.setEnabled(true);
                place.setEnabled(true);
            }
        });

        place.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mTask = new MyTask();
                mTask.execute(google_url_front+comInfo+google_url_back);
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTask.cancel(true);
            }
        });

        /*for (int i = 0 ; i < phoneNumberList.size() ; i++) {
            mTask = new MyTask();
            phone_number = phoneNumberList.get(i);
            wp_url = wp_url_front + phone_number + wp_url_back;
            //mTask.execute(wp_url);
            mTask.execute("http://www.baidu.com");
            mTask = new MyTask();
            google_url = google_url_front + comInfo + google_url_back;
            mTask.execute(google_url);
        }*/
    }

    private class MyTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            Log.i(TAG, "doInBackground(Params... params) called");
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet get = new HttpGet(params[0]);
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    String test = "{\"phone_number\":\"7347692748\",\"is_valid\":true,\"country_calling_code\":\"1\",\"line_type\":\"FixedVOIP\"," +
                            "\"carrier\":\"Comcast\",\"is_prepaid\":false,\"is_commercial\":true,\"belongs_to\":[{\"name\":\"Evergreen Restaurant\"," +
                            "\"age_range\":null,\"gender\":null,\"type\":\"Business\"}],\"current_addresses\":[{\"street_line_1\":\"2771 Plymouth Rd\"," +
                            "\"street_line_2\":null,\"city\":\"Ann Arbor\",\"postal_code\":\"48105\",\"zip4\":\"2427\",\"state_code\":\"MI\"," +
                            "\"country_code\":\"US\",\"lat_long\":{\"latitude\":42.302571,\"longitude\":-83.705776,\"accuracy\":\"RoofTop\"},\"is_active\":true," +
                            "\"delivery_point\":\"SingleUnit\"}],\"associated_people\":[],\"alternate_phones\":[\"7347693118\"],\"warnings\":[]}";
                    HttpEntity entity = response.getEntity();
                    InputStream is = entity.getContent();
                    //ObjectMapper mapper = new ObjectMapper();
                    //JSONObject dataJson=new JSONObject(is.toString());
                    if (params[0].indexOf("m") == 8){ // url is from google place api
                        long total = entity.getContentLength();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[1024];
                        int count = 0;
                        int length = -1;
                        while ((length = is.read(buf)) != -1) {
                            baos.write(buf, 0, length);
                            count += length;
                            publishProgress((int) ((count / (float) total) * 100));
                        }
                        String content = new String(baos.toByteArray(),"gb2312");
                        JSONObject googleJson = new JSONObject(content);
                        JSONArray res = googleJson.getJSONArray("results");
                        String type = res.getJSONObject(0).getString("types");
                        return type;
                    }
                    else{ // url is from whitepages api
                        JSONObject dataJson = new JSONObject(test); //for testing purpose

                        // first sort out those that are not commercial
                        boolean is_commercial = dataJson.getBoolean("is_commercial");

                        if (!is_commercial){
                            String e = "";
                            return e;
                        }

                        JSONArray addr = dataJson.getJSONArray("current_addresses");
                        JSONObject latLong = addr.getJSONObject(0).getJSONObject("lat_long");

                        // transfer the location and name information to Google place api
                        String information = "location=";
                        information += String.valueOf(latLong.getDouble("latitude"));
                        information += ",";
                        information += String.valueOf(latLong.getDouble("longitude"));
                        information += "&name=";
                        JSONArray belong = dataJson.getJSONArray("belongs_to");
                        String name = belong.getJSONObject(0).getString("name");
                        name = name.replaceAll(" ", "+");
                        information += name;

                        long total = entity.getContentLength();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[1024];
                        int count = 0;
                        int length = -1;
                        while ((length = is.read(buf)) != -1) {
                            baos.write(buf, 0, length);
                            count += length;
                            publishProgress((int) ((count / (float) total) * 100));
                        }
                        return information;
                    }

                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "onPostExecute(Result result) called");
            //textView.setText(result);
            comInfo = result;
            if (!comInfo.isEmpty()){
                textView.append(comInfo);
            }

            execute.setEnabled(true);
            cancel.setEnabled(false);
        }
    }

    private List<Map<String, String>> getDataList(){
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, // URI for searching phone call history
                new String[] { CallLog.Calls.CACHED_NAME // contact name of the number you dialed
                        , CallLog.Calls.NUMBER // phone number of the phone call history
                        , CallLog.Calls.DATE // date of the phone call
                        , CallLog.Calls.DURATION // duration of the phone call
                        , CallLog.Calls.TYPE } // type of the pnoe call
                , null, null, CallLog.Calls.DEFAULT_SORT_ORDER // show from the most recently call to the least recently call
        );
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
            String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
            long dateLong = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(dateLong));
            int duration = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));
            int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
            String typeString = "";
            switch (type) {
                case CallLog.Calls.INCOMING_TYPE:
                    typeString = "Incoming";
                    break;
                case CallLog.Calls.OUTGOING_TYPE:
                    typeString = "Outgoing";
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    typeString = "Missed";
                    break;
                case CallLog.Calls.REJECTED_TYPE:
                    typeString = "Rejected";
                    break;
                default:
                    break;
            }
            Map<String, String> map = new HashMap<String, String>();
            map.put("name", (name == null) ? "Unknown" : name);
            map.put("number", number);
            map.put("date", date);
            map.put("duration", (duration / 60) + "min");
            map.put("type", typeString);
            list.add(map);
        }
        return list;
    }

    private List<String> getPhoneNumberList(){
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, // URI for searching phone call history
                new String[] { CallLog.Calls.CACHED_NAME // contact name of the number you dialed
                        , CallLog.Calls.NUMBER // phone number of the phone call history
                        , CallLog.Calls.DATE // date of the phone call
                        , CallLog.Calls.DURATION // duration of the phone call
                        , CallLog.Calls.TYPE } // type of the pnoe call
                , null, null, CallLog.Calls.DEFAULT_SORT_ORDER // show from the most recently call to the least recently call
        );
        List<String> list = new ArrayList<>();
        while(cursor.moveToNext()){
            String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
            list.add(number);
        }
        return list;
    }

    public void export_click(View view){
        mLvShow = (ListView) findViewById(R.id.lv_show);
        dataList = getDataList();
        adapter = new SimpleAdapter(this, dataList, R.layout.simple_callog_item, new String[]{"name", "number", "date", "duration", "type"}
                , new int[]{R.id.tv_name, R.id.tv_number, R.id.tv_date, R.id.tv_duration, R.id.tv_type});
        mLvShow.setAdapter(adapter);
    }
}
