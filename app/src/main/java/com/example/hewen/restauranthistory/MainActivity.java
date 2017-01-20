package com.example.hewen.restauranthistory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.text.SimpleDateFormat;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;


public class MainActivity extends AppCompatActivity {
    //private ListView mLvShow;
    //private List<Map<String, String>> dataList;
    //private SimpleAdapter adapter;
    private static final String TAG = "ASYNC_TASK";

    private Button execute;
    private Button cancel;
    private MyTask mTask;
    private ProgressBar progressBar;
    private TextView textView;
    private List<Map<String,String>> phoneNumberList;
    private Button place;
    private Button histogram;

    private String comInfo;

    private BarChart barChart;
    private XAxis xAxis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        histogram = (Button) findViewById(R.id.histogram);
        histogram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barChart = (BarChart)findViewById(R.id.barChart);
                xAxis = barChart.getXAxis();
                xAxis.setDrawAxisLine(true);
                xAxis.setDrawGridLines(false);
                // x-axis
                barChart.setDrawGridBackground(true); // whether or not to display the color of background
                barChart.getAxisLeft().setDrawAxisLine(false);
                barChart.setTouchEnabled(true);
                barChart.setDragEnabled(true);
                barChart.setScaleEnabled(true);
                // y-axis
                barChart.setDescription("Number of Calls");
                barChart.getAxisLeft().setEnabled(false);
                barChart.getAxisRight().setEnabled(false);
                Legend legend = barChart.getLegend();// hide legend
                legend.setEnabled(false);

                ArrayList<String> xValues = new ArrayList<String>();
                xValues.add("一季度");
                xValues.add("二季度");
                xValues.add("三季度");
                xValues.add("四季度");

                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);//数据位于底部
                ArrayList<BarEntry> yValues = new ArrayList<BarEntry>();

                yValues.add(new BarEntry(20, 0));
                yValues.add(new BarEntry(18, 1));
                yValues.add(new BarEntry(4, 2));
                yValues.add(new BarEntry(45, 3));

                BarDataSet barDataSet=new BarDataSet(yValues,"");
                barDataSet.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                        int n = (int) value;
                        return n + " ";
                    }
                });
                //6、设置柱状图的颜色
                barDataSet.setColors(new int[]{Color.rgb(247, 211, 149), Color.rgb(209, 252, 153),
                        Color.rgb(161, 232, 251), Color.rgb(239, 147, 156)});
                //7、显示，柱状图的宽度和动画效果
                BarData barData = new BarData(xValues, barDataSet);
                barDataSet.setBarSpacePercent(40f);//值越大，柱状图就越宽度越小；
                barChart.animateY(1000);
                barChart.setData(barData); //
            }
        });

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

        phoneNumberList = getDataList();

        for (int i = 0 ; i < phoneNumberList.size() ; i++){
            textView.setText(phoneNumberList.get(i).get("number"));
        }

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
        int total_duration = 0;
        int total_calls = 0;
        Map<String, Integer> call_times = new HashMap<>();
        while (cursor.moveToNext()) {
            total_calls++;
            String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
            if (name == null){
                // if the number is saved in the phone, then we prune that out (unlikely to be the business)
                continue;
            }
            String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
            if (call_times.containsKey(number)){
                int num = call_times.get(number);
                call_times.put(number, num++);
            }
            else{
                call_times.put(number, 1);
            }
            long dateLong = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
            //String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(dateLong));
            int duration = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));
            total_duration += duration; // in order to calculate the average duration
            Map<String, String> map = new HashMap<>();
            map.put("number", number);
            map.put("duration", duration+"");
            //map.put("date", date);
            list.add(map);
        }
        int avg_duration = total_duration / total_calls;
        List<String> prune_out_list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry: call_times.entrySet()){
            if (entry.getValue() > 1 / 3 * total_calls){
                // if call more than 1/3 of the total times, prune that out
                prune_out_list.add(entry.getKey());
            }
        }

        for (int i = 0 ; i < list.size() ; i++){
            if (Integer.parseInt(list.get(i).get("duration")) > avg_duration){
                //if the duration is long, prune that out
                list.remove(i);
                continue;
            }
            String number = list.get(i).get("number");
            if (prune_out_list.contains(number)){
                list.remove(i);
            }
        }
        return list;
    }

    /*public void export_click(View view){
        mLvShow = (ListView) findViewById(R.id.lv_show);
        dataList = getDataList();
        adapter = new SimpleAdapter(this, dataList, R.layout.simple_callog_item, new String[]{"name", "number", "date", "duration", "type"}
                , new int[]{R.id.tv_name, R.id.tv_number, R.id.tv_date, R.id.tv_duration, R.id.tv_type});
        mLvShow.setAdapter(adapter);
    }*/
}
