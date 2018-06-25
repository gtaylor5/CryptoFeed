package com.cryptoinc.cryptofeed;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.crashlytics.android.Crashlytics;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Scanner;

import Utilities.CurrencyInfo;
import Utilities.DateFormatter;
import Utilities.RequestSingleton;

public class CurrencyDetailFragment extends Fragment {

    CurrencyInfo currencyInfo;

    //social media stats

    TextView twitterSocialActivity;
    TextView redditSocialActivity;

    // Header Info

    TextView currencySymbol;
    TextView percentageChange;
    TextView price;

    // Price Stats

    TextView priceView;
    TextView timestamp;
    TextView fearOrGreed;
    TextView priceHigh;
    TextView priceLow;
    TextView volume;

    //headings

    TextView socialMediaActivity;
    TextView statisticsView;
    TextView aboutView;

    //sentiment

    TextView happySentiment;
    TextView sadSentiment;
    TextView neutralSentiment;
    TextView aboutText;

    //db

    FirebaseDatabase firebaseDatabase;
    DatabaseReference reference;

    LineChart priceGraph;

    ProgressBar chartProgress;

    Button minuteData;
    Button hourData;
    Button dayData;

    CardView aboutCardView;

    NestedScrollView parentView;

    ArrayList<Entry> minuteChartData = new ArrayList<Entry>();
    ArrayList<Entry> hourChartData = new ArrayList<Entry>();
    ArrayList<Entry> dayChartData = new ArrayList<Entry>();

    boolean chartClicked = false;

    DecimalFormat formatter = new DecimalFormat("$ #,###.00");
    DecimalFormat lessThanOne = new DecimalFormat("$ 0.000");

    final String baseSocialActivityURL = "https://frypto-backend.herokuapp.com/api/coins?auth=90886b14-0fd5-4b0f-a9a1-9bd847ebd92e&symbol=";
    final String baseCurrencyAboutURL = "https://www.cryptocompare.com/api/data/coinsnapshotfullbyid/?id=";

    public CurrencyDetailFragment() {
        // Required empty public constructor
    }

    // Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_currency_detail, container, false);
        View v = view;
        initializeViews(v);
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        minuteChartData.clear();
        hourChartData.clear();
        dayChartData.clear();
        priceGraph.clear();
        this.currencyInfo = ((MainActivity)getActivity()).currentCurrency;
        priceView.setText(formatter.format(currencyInfo.getLast()));
        if(currencyInfo != null) {
            updateViews();
            setGlobalGraphProperties();
            loadInitialChart();
            getFirebaseData();
            getSocialMediaActivity();
            getFullCoinSnapshotById();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    // Chart stuff

    public void loadInitialChart() {
        minuteData.setPressed(true);
        minuteData.setPressed(true);
        hourData.setTextColor(Color.WHITE);
        dayData.setTextColor(Color.WHITE);
        hourData.setBackgroundColor(getResources().getColor(R.color.card_color,null));
        dayData.setBackgroundColor(getResources().getColor(R.color.card_color,null));

        minuteData.setTextColor(getResources().getColor(R.color.graph_line_color, null));
        minuteData.setBackgroundColor(getResources().getColor(R.color.card_text, null));

        chartProgress.setVisibility(View.VISIBLE);
        if(isAdded()) {
            getMinuteChartData();
            getHourChartData();
            getDayChartData();
        }
        parseDate();
    }

    public void setStyling(ArrayList<Entry> chartData, String label) {

        if(chartData.size() == 0){
            Crashlytics.log("The chart data size is zero for: " + currencyInfo.getName());
            Toast.makeText(getActivity(), "Chart data could not be loaded.", Toast.LENGTH_LONG).show();
            return;
        }

        LineDataSet lineDataSet = new LineDataSet(chartData, label);
        int color = getResources().getColor(R.color.graph_line_color, null);
        lineDataSet.setColor(color);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setHighlightEnabled(true);


        //axes
        XAxis xAxis = priceGraph.getXAxis();
        xAxis.setLabelCount(3);
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.card_text, null));
        xAxis.setDrawAxisLine(true);
        xAxis.setEnabled(true);
        xAxis.setGranularity(50f);
        xAxis.setGranularityEnabled(true);
        xAxis.setValueFormatter(new DateFormatter(label));


        YAxis yAxis = priceGraph.getAxisLeft();
        yAxis.setDrawGridLines(false);
        yAxis.setTextColor(getResources().getColor(R.color.card_text, null));
        yAxis.setDrawAxisLine(false);
        yAxis.setEnabled(false);


        LineData lineData = new LineData(lineDataSet);
        lineData.setDrawValues(false);
        priceGraph.setData(lineData);
        priceGraph.fitScreen();
        priceGraph.animateXY(500, 0, Easing.EasingOption.Linear, Easing.EasingOption.Linear);
    }

    public void setGlobalGraphProperties() {
        priceGraph.setNoDataText("");
        priceGraph.setEnabled(false);
        priceGraph.setDrawGridBackground(false);
        priceGraph.getAxisRight().setEnabled(false);
        priceGraph.setDrawBorders(false);
        Description description = new Description();
        description.setText("");
        priceGraph.setDescription(description);
        Legend legend = priceGraph.getLegend();
        legend.setEnabled(false);
        priceGraph.setHighlightPerDragEnabled(true);
        priceGraph.setTouchEnabled(true);
        priceGraph.setViewPortOffsets(-40,0,0,50);
        setPriceGraphListeners();
    }

    public void setPriceGraphListeners() {
        priceGraph.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                priceView.setText(String.format(Locale.US,"$%.3f", e.getY()));
                timestamp.setText(convertToDateAndTime(e.getX()));
            }

            @Override
            public void onNothingSelected() {
            }
        });

        priceGraph.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                priceGraph.getData().setHighlightEnabled(true);
                chartClicked = true;
                parentView.requestDisallowInterceptTouchEvent(true);
            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

                if(me.getAction() == 1){
                    chartClicked = false;
                    priceGraph.getData().setHighlightEnabled(false);
                    parentView.requestDisallowInterceptTouchEvent(false);
                    updateViews();
                    chartClicked = false;
                }

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                if(priceGraph.getData() != null) {
                    priceGraph.getData().setHighlightEnabled(false);
                }
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                if(priceGraph.getData() != null) {
                    priceGraph.getData().setHighlightEnabled(false);
                }
            }
        });
    }

    public void initializeViews(View v) {

            price = v.findViewById(R.id.price);
            percentageChange = v.findViewById(R.id.percentageChange);
            currencySymbol = v.findViewById(R.id.currencyLabel);

            twitterSocialActivity = v.findViewById(R.id.twitterSocialActivity);
            redditSocialActivity = v.findViewById(R.id.redditSocialActivity);
            fearOrGreed = v.findViewById(R.id.fearOrGreed);

            priceView = v.findViewById(R.id.priceView);
            timestamp = v.findViewById(R.id.timestamp);
            priceHigh = v.findViewById(R.id.high);
            priceLow = v.findViewById(R.id.low);
            volume = v.findViewById(R.id.volume);

            socialMediaActivity = v.findViewById(R.id.socialMediaActivity);
            statisticsView = v.findViewById(R.id.statisticsView);
            aboutView = v.findViewById(R.id.aboutView);

            happySentiment = v.findViewById(R.id.positiveSentiment);
            sadSentiment = v.findViewById(R.id.negativeSentiment);
            neutralSentiment = v.findViewById(R.id.neutralSentiment);

            parentView = v.findViewById(R.id.activity_currency_detail);

            priceGraph = v.findViewById(R.id.currencyChart);
            chartProgress = v.findViewById(R.id.chartProgress);

            minuteData = v.findViewById(R.id.minuteChart);
            hourData = v.findViewById(R.id.hourChart);
            dayData = v.findViewById(R.id.dayChart);

            aboutCardView = v.findViewById(R.id.aboutCardView);
            aboutText = v.findViewById(R.id.aboutText);

            setOnClickListeners();
    }

    public void updateViews(){
        if(currencyInfo.getSymbol().equalsIgnoreCase("btc")){
            if(!chartClicked) {
                parseDate();
            }
        } else {
            if(!chartClicked) {
                if(currencyInfo.getLast() < 1) {
                    priceView.setText(lessThanOne.format(currencyInfo.getLast()));
                } else {
                    priceView.setText(formatter.format(currencyInfo.getLast()));
                }
            }
            parseDate();
        }

        percentageChange.setText(String.format(Locale.US,"%.3f",currencyInfo.getPercentageChange()) + " %");
        if(currencyInfo.getPercentageChange() < 0) {
            percentageChange.setTextColor(getResources().getColor(R.color.negative_red, null));
        } else {
            percentageChange.setTextColor(getResources().getColor(R.color.positive, null));
        }
        currencySymbol.setText(currencyInfo.getName() + " (" + currencyInfo.getSymbol() + ")");

        if(currencyInfo.getLast() < 1) {
            price.setText(lessThanOne.format(currencyInfo.getLast()));
        } else {
            price.setText(formatter.format(currencyInfo.getLast()));
        }

        priceHigh.setText(formatter.format(currencyInfo.getHigh24Hr()));
        priceLow.setText(formatter.format(currencyInfo.getLow24Hr()));
        volume.setText(formatter.format(currencyInfo.getVolume24Hr()));

        if(isAdded()) {
            statisticsView.setText(R.string.stats);
            aboutView.setText(R.string.about);
            if (currencyInfo.getOpenBuyOrders() >= currencyInfo.getOpenSellOrders()) {
                fearOrGreed.setText(R.string.buy);
                fearOrGreed.setTextColor(getResources().getColor(R.color.positive, null));
            } else if (currencyInfo.getOpenSellOrders() > currencyInfo.getOpenBuyOrders()) {
                fearOrGreed.setText(R.string.sell);
                fearOrGreed.setTextColor(getResources().getColor(R.color.negative_red, null));
            }
        }
    }

    public void setOnClickListeners() {
        minuteData.setOnClickListener(v -> {
            priceGraph.setData(null);
            if (hourData.isPressed()) {
                hourData.setPressed(false);
            }
            if (dayData.isPressed()) {
                dayData.setPressed(false);
            }
            minuteData.setPressed(true);
            hourData.setTextColor(Color.WHITE);
            dayData.setTextColor(Color.WHITE);
            if(isAdded()) {
                hourData.setBackgroundColor(getResources().getColor(R.color.card_color, null));
                dayData.setBackgroundColor(getResources().getColor(R.color.card_color, null));

                minuteData.setTextColor(getResources().getColor(R.color.graph_line_color, null));
                minuteData.setBackgroundColor(getResources().getColor(R.color.card_text, null));
                setStyling(minuteChartData, "Minutely Chart");
            }
        });

        hourData.setOnClickListener(v -> {
            priceGraph.setData(null);
            if (minuteData.isPressed()) {
                minuteData.setPressed(false);
            }
            if (dayData.isPressed()) {
                dayData.setPressed(false);
            }
            if(isAdded()) {
                hourData.setPressed(true);
                minuteData.setTextColor(Color.WHITE);
                dayData.setTextColor(Color.WHITE);

                minuteData.setBackgroundColor(getResources().getColor(R.color.card_color, null));
                dayData.setBackgroundColor(getResources().getColor(R.color.card_color, null));

                hourData.setTextColor(getResources().getColor(R.color.graph_line_color, null));
                hourData.setBackgroundColor(getResources().getColor(R.color.card_text, null));
                setStyling(hourChartData, "Hourly Chart");
            }
        });

        dayData.setOnClickListener(v -> {
            priceGraph.setData(null);
            if (hourData.isPressed()) {
                hourData.setPressed(false);
            }
            if (minuteData.isPressed()) {
                minuteData.setPressed(false);
            }
            if(isAdded()) {
                hourData.setTextColor(Color.WHITE);
                minuteData.setTextColor(Color.WHITE);
                dayData.setPressed(true);

                minuteData.setBackgroundColor(getResources().getColor(R.color.card_color, null));
                hourData.setBackgroundColor(getResources().getColor(R.color.card_color, null));

                dayData.setTextColor(getResources().getColor(R.color.graph_line_color, null));
                dayData.setBackgroundColor(getResources().getColor(R.color.card_text, null));
                setStyling(dayChartData, "Daily Chart");
            }
        });

    }

    // Requests

    public void getHourChartData() {
        String requestString = "https://min-api.cryptocompare.com/data/histohour?fsym="+currencyInfo.getSymbol()+"&tsym=USD&limit=2000&aggregate=3&e=CCCAGG";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, requestString, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("Data");
                for(int i = 0; i < jsonArray.length(); i++){
                    JSONObject object = jsonArray.getJSONObject(i);
                    String timeStr = object.get("time").toString();
                    String closeStr = object.get("close").toString();
                    float time = Float.parseFloat(timeStr);
                    float value = Float.parseFloat(closeStr);
                    if(value == 0f)
                        continue;
                    hourChartData.add(new Entry(time, value));
                }
            }catch (JSONException e){
                //
            }
        }, error -> {

        });
        RequestSingleton.getInstance(getActivity()).addToRequestQueue(stringRequest);
    }

    public void getDayChartData() {
        String requestString = "https://min-api.cryptocompare.com/data/histoday?fsym="+currencyInfo.getSymbol()+"&tsym=USD&limit=2000&aggregate=3&e=CCCAGG";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, requestString, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("Data");
                for(int i = 0; i < jsonArray.length(); i++){
                    JSONObject object = jsonArray.getJSONObject(i);
                    String timeStr = object.get("time").toString();
                    String closeStr = object.get("close").toString();
                    float time = Float.parseFloat(timeStr);
                    float value = Float.parseFloat(closeStr);
                    if(value == 0f)
                        continue;
                    dayChartData.add(new Entry(time, value));
                }
            }catch (JSONException e){
                //
            }
        }, error -> {

        });

        RequestSingleton.getInstance(getActivity()).addToRequestQueue(stringRequest);
    }

    private void getMinuteChartData() {
        String requestString = "https://min-api.cryptocompare.com/data/histominute?fsym="+currencyInfo.getSymbol()+"&tsym=USD&limit=2000&aggregate=3&e=CCCAGG";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, requestString, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("Data");
                for(int i = 0; i < jsonArray.length(); i++){
                    JSONObject object = jsonArray.getJSONObject(i);
                    String timeStr = object.get("time").toString();
                    String closeStr = object.get("close").toString();
                    float time = Float.parseFloat(timeStr);
                    float value = Float.parseFloat(closeStr);
                    if(value == 0f)
                        continue;
                    minuteChartData.add(new Entry(time, value));
                }
                chartProgress.setVisibility(View.INVISIBLE);
                minuteData.callOnClick();
            }catch (JSONException e){
                //
            }
        }, error -> {

        });
        RequestSingleton.getInstance(getActivity()).addToRequestQueue(stringRequest);
    }

    public void getFullCoinSnapshotById(){
        try {
            Scanner scanner = new Scanner(getActivity().getApplicationContext().getAssets().open("currency_data.txt"));
            String id = "";
            while(scanner.hasNextLine()){
                String line = scanner.nextLine();
                String[] arr = line.split(",");
                if(currencyInfo.getSymbol().equalsIgnoreCase(arr[2])){
                    id = arr[1];
                    break;
                }
            }

            String requestString = baseCurrencyAboutURL + id;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, requestString, response -> {
                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONObject dataObject = responseObject.getJSONObject("Data");
                    JSONObject generalObject = dataObject.getJSONObject("General");
                    Iterator<String> it = generalObject.keys();
                    while(it.hasNext()){
                        String key = it.next();
                        if(key.equalsIgnoreCase("description")){
                            String value = generalObject.getString(key);
                            if(value == null){
                                break;
                            }
                            aboutText.setText(trim(Html.fromHtml(value), 0, Html.fromHtml(value).length()));
                            aboutText.setTextColor(getResources().getColor(R.color.white, null));
                            break;
                        }
                    }
                    if(aboutText.getText() == null){
                        aboutCardView.setVisibility(View.INVISIBLE);
                    }
                } catch (Exception e){
                    //
                }
            }, error -> {

            });
            RequestSingleton.getInstance(getActivity()).addToRequestQueue(stringRequest);
        } catch(Exception e){
            //
        }
    }

    public void getSocialMediaActivity(){
        String requestString = baseSocialActivityURL + currencyInfo.getSymbol();
        StringRequest request = new StringRequest(Request.Method.GET, requestString, response -> {
            try {
                JSONObject json = new JSONObject(response);
                JSONObject jsonObject = json.getJSONObject(currencyInfo.getSymbol());
                DecimalFormat decimalFormat = new DecimalFormat("#,###");
                if(isAdded()) {
                    if (jsonObject.getString("reddit_volume_24h") != null) {
                        int val = Integer.parseInt(jsonObject.getString("reddit_volume_24h"));
                        redditSocialActivity.setText(decimalFormat.format(val));
                        redditSocialActivity.setTextColor(getResources().getColor(R.color.white, null));
                    } else {
                        redditSocialActivity.setText("--");
                        redditSocialActivity.setTextColor(getResources().getColor(R.color.white, null));
                    }
                    if (jsonObject.getString("twitter_volume_24h") != null) {
                        int val = Integer.parseInt(jsonObject.getString("twitter_volume_24h"));
                        twitterSocialActivity.setText(decimalFormat.format(val));
                        twitterSocialActivity.setTextColor(getResources().getColor(R.color.white, null));

                    } else {
                        twitterSocialActivity.setText("--");
                        twitterSocialActivity.setTextColor(getResources().getColor(R.color.white, null));
                    }
                }
            }catch (JSONException e){
                //
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RequestSingleton.getInstance(getActivity()).addToRequestQueue(request);
    }

    //Util

    public static CharSequence trim(CharSequence s, int start, int end) {
        while (start < end && Character.isWhitespace(s.charAt(start))) {
            start++;
        }

        while (end > start && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }

        return s.subSequence(start, end);
    }

    public String convertToDateAndTime(float f){
        Date date = new Date(((long)f)*1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy H:mm", Locale.US);
        return sdf.format(date);
    }

    public void parseDate() {
        try {
            Date date = new Date(Long.parseLong(currencyInfo.getTimeStamp())*1000);
            SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy H:mm:ss", Locale.US);
            timestamp.setText(sdf.format(date));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void getFirebaseData() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = firebaseDatabase.getReference("sentiment").child(currencyInfo.getSymbol());
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    if(snapshot.getValue() != null) {
                        String s = snapshot.getValue().toString() + "%";
                        switch (snapshot.getKey()){
                            case "positive":
                                happySentiment.setText(s);
                                break;
                            case "negative":
                                sadSentiment.setText(s);
                                break;
                            case "neutral":
                                neutralSentiment.setText(s);
                                break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }



}
