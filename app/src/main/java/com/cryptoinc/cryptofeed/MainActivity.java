package com.cryptoinc.cryptofeed;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import Utilities.CurrencyInfo;
import Utilities.NewsInfo;
import Utilities.RequestSingleton;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnHomeFragmentListener, NewsFragment.OnNewsFragmentItemSelectedListener{

    AdView mAdView;
    AHBottomNavigation bottomNavigation;
    ProgressBar progressBar;

    CurrencyInfo currentCurrency;
    NewsInfo currentNews;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setNavBar();
        setAdView();
    }

    // Initialize Views

    public void setNavBar() {

        progressBar = findViewById(R.id.progress_home);

        AHBottomNavigationItem profileItem = new AHBottomNavigationItem("Profile", R.drawable.ic_person_white_24dp);
        AHBottomNavigationItem newsItem = new AHBottomNavigationItem("News", R.drawable.news);
        AHBottomNavigationItem homeItem = new AHBottomNavigationItem("Home", R.drawable.home);

        bottomNavigation = findViewById(R.id.bottom_nav);
        bottomNavigation.addItem(profileItem);
        bottomNavigation.addItem(homeItem);
        bottomNavigation.addItem(newsItem);
        bottomNavigation.setDefaultBackgroundColor(getResources().getColor(R.color.card_color, null));
        bottomNavigation.setAccentColor(getResources().getColor(R.color.negative_red, null));
        bottomNavigation.setInactiveColor(getResources().getColor(R.color.white, null));
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
        bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                switch (position) {
                    case 0: {
                        Fragment fragment = fragmentManager.findFragmentByTag(ProfileFragment.class.getSimpleName());
                        if (fragment == null) {
                            showFragment(new ProfileFragment());
                        } else if (fragment.isVisible()) {
                            //
                        } else {
                            showFragment(fragment);
                        }
                        bottomNavigation.enableItemAtPosition(0);
                        break;
                    }
                    case 1: {
                        Fragment fragment = fragmentManager.findFragmentByTag(HomeFragment.class.getSimpleName());
                        if(fragment == null){
                            showFragment(new HomeFragment());
                        } else if(fragment.isVisible()){
                            HomeFragment homeFragment = (HomeFragment)fragment;
                            homeFragment.recyclerView.scrollToPosition(0);
                        } else {
                            showFragment(fragment);
                        }
                        bottomNavigation.enableItemAtPosition(1);
                        break;
                    }
                    case 2: {
                        Fragment fragment = fragmentManager.findFragmentByTag(NewsFragment.class.getSimpleName());
                        if(fragment == null){
                            showFragment(new NewsFragment());
                        } else {
                            showFragment(fragment);
                        }
                        bottomNavigation.enableItemAtPosition(2);
                        break;
                    }
                }
                return true;
            }
        });
        bottomNavigation.enableItemAtPosition(1);
        bottomNavigation.setCurrentItem(1);
    }

    public void setAdView() {
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    // Fragment Managment

    public void showFragment(Fragment fragment){
        String tag = fragment.getClass().getSimpleName();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragmentContainer1, fragment, tag);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void backstackFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            finish();
        }
        try {
            getSupportFragmentManager().popBackStackImmediate();
        } catch (Exception e) {
            //
        } finally {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer1);
            if (currentFragment != null) {
                if (currentFragment.getClass().getSimpleName().equalsIgnoreCase("CurrencyDetailFragment") || currentFragment.getClass().getSimpleName().equalsIgnoreCase("HomeFragment")) {
                    bottomNavigation.enableItemAtPosition(1);
                    bottomNavigation.setCurrentItem(1, false);
                } else if (currentFragment.getClass().getSimpleName().equalsIgnoreCase("NewsFragment")) {
                    bottomNavigation.enableItemAtPosition(2);
                }
            }
        }
    }

    public void clearBackStackAfterLogout() {
        FragmentManager fm = getSupportFragmentManager();
        for(int i = 0; i < fm.getBackStackEntryCount(); i++){
            fm.popBackStackImmediate();
        }
        bottomNavigation.setCurrentItem(1, true);
    }

    //Override Methods

    @Override
    public void onHomeFragmentItemSelected(CurrencyInfo currencyInfo) {
        currentCurrency = currencyInfo;
        View v = getCurrentFocus();
        if(v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm != null) {
                imm.hideSoftInputFromInputMethod(v.getWindowToken(), 0);
            }
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(CurrencyDetailFragment.class.getSimpleName());
        if(fragment == null){
            showFragment(new CurrencyDetailFragment());
        } else {
            showFragment(fragment);
        }
    }

    @Override
    public void onNewsItemSelected(NewsInfo info) {
        currentNews = info;
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(WebFragment.class.getSimpleName());
        if(fragment == null){
            showFragment(new WebFragment());
        }
    }

    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().getBackStackEntryCount() > 0){
            RequestSingleton.getInstance(this).getRequestQueue().cancelAll("");
            backstackFragment();
        } else {
            super.onBackPressed();
        }
    }

}
