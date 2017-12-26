package com.cryptoinc.cryptofeed;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import Utilities.NewsInfo;

public class WebFragment extends Fragment {

    NewsInfo info;
    View view;
    public WebView webView;

    public WebFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(view == null) {
            view = inflater.inflate(R.layout.web_news_fragment, container, false);
            webView = view.findViewById(R.id.webFrag);
            webView.setWebViewClient(new WebViewClient(){
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    if(isAdded()) {
                        ((MainActivity) getActivity()).progressBar.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if(isAdded()) {
                        ((MainActivity)getActivity()).progressBar.setVisibility(View.INVISIBLE);
                    }

                }
            });
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        info = ((MainActivity)getActivity()).currentNews;
        if(info != null){
            webView.loadUrl(info.getLink());
        }
    }
}
