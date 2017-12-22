package com.cryptoinc.cryptofeed;

import android.content.Context;
import android.net.ParseException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import Utilities.NewsInfo;
import Utilities.NewsInfoAdapter;

public class NewsFragment extends Fragment {

    public String coinDeskNewsURL = "https://www.coindesk.com/feed/";
    //private String coinTelegraphNewURL = "https://cointelegraph.com/rss";
    public String bitcoinNewsURL = "https://news.bitcoin.com/feed/";

    public OnNewsFragmentItemSelectedListener mListener;
    public View view;
    RecyclerView recyclerView;
    NewsInfoAdapter newsInfoAdapter;
    LinearLayoutManager manager;
    ArrayList<NewsInfo> newsList = new ArrayList<>();
    public RequestQueue queue;

    volatile int numberReturned = 0;

    public NewsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(view == null) {
            view = inflater.inflate(R.layout.fragment_news, container, false);
            recyclerView = view.findViewById(R.id.newsList);
            newsInfoAdapter = new NewsInfoAdapter(getActivity().getLayoutInflater(), newsList);
            newsInfoAdapter.setNewsInfoListListener(new NewsInfoAdapter.NewsInfoListListener() {
                @Override
                public void newsSelected(NewsInfo info) {
                    mListener.onNewsItemSelected(info);
                }
            });
            recyclerView.setAdapter(newsInfoAdapter);
            manager = new LinearLayoutManager(getActivity().getApplicationContext());
            manager.setOrientation(LinearLayout.VERTICAL);
            recyclerView.setLayoutManager(manager);
        }
        return view;
    }

    public StringRequest getCoinDeskNews(){
        return new StringRequest(Request.Method.GET, coinDeskNewsURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(new StringReader(response));
                    parser.nextTag();
                    parser.nextTag();
                    String name = "CoinDesk.com";
                    newsList.addAll(readFeed(parser, name));
                    numberReturned++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
    }

    public StringRequest getBitcoinNews(){
        return new StringRequest(Request.Method.GET, bitcoinNewsURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(new StringReader(response));
                    parser.nextTag();
                    parser.nextTag();
                    String name = "Bitcoin.com";
                    newsList.addAll(readFeed(parser, name));
                    numberReturned++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
    }

    public ArrayList<NewsInfo> readFeed(XmlPullParser parser, String source) throws XmlPullParserException, IOException, ParseException{
        ArrayList<NewsInfo> list = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, null, "channel");
        while(parser.next() != XmlPullParser.END_TAG){
            if(parser.getEventType() != XmlPullParser.START_TAG){
                continue;
            }
            String name = parser.getName();
            if(name.equals("item")){
                list.add(readItem(parser, source));
            } else {
                skip(parser);
            }
        }
        return list;
    }


    public NewsInfo readItem(XmlPullParser parser, String source) {
        NewsInfo info = new NewsInfo();
        info.setSource(source);
        try {
            while(parser.next() != XmlPullParser.END_TAG){
                if(parser.getEventType() != XmlPullParser.START_TAG){
                    continue;
                }
                String name = parser.getName();
                if(name != null) {
                    if (name.equals("title")) {
                        info.setTitle(readText(parser));
                    } else if (name.equals("link")) {
                        info.setLink(readText(parser));
                    } else if (name.equals("pubDate")) {
                        info.setPubDate(readText(parser));
                    } else if (name.equals("description")) {
                        info.setDescription(readText(parser));
                    } else {
                        skip(parser);
                    }
                }
            }
        } catch (Exception e){
            //
        }
        return info;
    }

    public void skip(XmlPullParser parser)  throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    public String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = null;
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        numberReturned = 0;
        ((MainActivity)getActivity()).progressBar.setVisibility(View.VISIBLE);
        queue.add(getCoinDeskNews());
        queue.add(getBitcoinNews());
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(numberReturned < 2){
                    //
                }
                (getActivity()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MainActivity)getActivity()).progressBar.setVisibility(View.INVISIBLE);
                        newsInfoAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnNewsFragmentItemSelectedListener) {
            mListener = (OnNewsFragmentItemSelectedListener) context;
            queue = Volley.newRequestQueue(getActivity().getApplicationContext());
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNewsFragmentItemSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnNewsFragmentItemSelectedListener {
        // TODO: Update argument type and name
        void onNewsItemSelected(NewsInfo info);
    }
}
