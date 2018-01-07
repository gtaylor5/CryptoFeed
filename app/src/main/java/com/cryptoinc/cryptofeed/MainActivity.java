package com.cryptoinc.cryptofeed;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

import Utilities.CurrencyInfo;
import Utilities.NewsInfo;
import Utilities.RequestSingleton;
import Utilities.Requests;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnHomeFragmentListener, NewsFragment.OnNewsFragmentItemSelectedListener {

    AHBottomNavigation bottomNavigation;
    ProgressBar progressBar;

    CurrencyInfo currentCurrency;
    NewsInfo currentNews;

    public int numberOfClicks = 1;
    public long adTimeStamp = 0L;
    public InterstitialAd mInterstitialAd;
    public RewardedVideoAd mRewardVideoAd;

    FirebaseUser currentUser;
    DatabaseReference favoritesRef;
    DatabaseReference adsRef;

    HashSet<String> favorites = new HashSet<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration config = getResources().getConfiguration();
        if(config.smallestScreenWidthDp >= 600) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_main_large);
        } else {
            setContentView(R.layout.activity_main);
        }
        setNavBar();
        initializeAds();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null) {
            initializeFirebaseDB();
            getFavorites();
        }
    }

    private void initializeAds() {
        MobileAds.initialize(this, "ca-app-pub-3404074879352583~7944780383");
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.interstitial));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                AdRequest adRequest = new AdRequest.Builder().build();
                mInterstitialAd.loadAd(adRequest);
            }
        });
        mRewardVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardVideoAd.loadAd(getString(R.string.rewardVideo), new AdRequest.Builder().build());
        mRewardVideoAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
            @Override
            public void onRewardedVideoAdLoaded() {

            }

            @Override
            public void onRewardedVideoAdOpened() {

            }

            @Override
            public void onRewardedVideoStarted() {

            }

            @Override
            public void onRewardedVideoAdClosed() {

                mRewardVideoAd.loadAd(getString(R.string.rewardVideo), new AdRequest.Builder().build());
            }

            @Override
            public void onRewarded(RewardItem rewardItem) {
                long time = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Ads").child(currentUser.getUid());
                reference.child("time").setValue(time);
            }

            @Override
            public void onRewardedVideoAdLeftApplication() {

            }

            @Override
            public void onRewardedVideoAdFailedToLoad(int i) {

            }
        });
    }

    public void initializeFirebaseDB() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        favoritesRef = database.getReference("users").child(currentUser.getUid()).child("favorites");
        adsRef = database.getReference("Ads").child(currentUser.getUid());
        adsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getValue() != null) {
                    adTimeStamp = Long.parseLong(dataSnapshot.getValue().toString());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getValue() != null) {
                    adTimeStamp = Long.parseLong(dataSnapshot.getValue().toString());
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        favoritesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getValue() != null) {
                    favorites.add(dataSnapshot.getValue().toString());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null) {
                    favorites.remove(dataSnapshot.getValue().toString());
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getFavorites() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            favoritesRef = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).child("favorites");
            favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    favorites.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.getValue() != null) {
                            favorites.add(snapshot.getValue().toString());
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
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

    // Fragment Managment

    public void showFragment(Fragment fragment){
        if(findViewById(R.id.activity_main) != null) {
            String tag = fragment.getClass().getSimpleName();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragmentContainer1, fragment, tag);
            ft.addToBackStack(null);
            ft.commit();
        } else {
            String name = fragment.getClass().getSimpleName();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if(name.equalsIgnoreCase("NewsFragment") || name.equalsIgnoreCase("HomeFragment") || name.equalsIgnoreCase("ProfileFragment")){
                ft.replace(R.id.list_view, fragment);
                ft.addToBackStack(null);
            } else {
                ft.replace(R.id.detailView, fragment);
            }
            ft.commit();
        }
    }

    public void backstackFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            finish();
        }
        if(findViewById(R.id.activity_main) != null) {
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
        } else {
            try {
                getSupportFragmentManager().popBackStackImmediate();
            } catch (Exception e) {
                //
            } finally {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.list_view);
                if (currentFragment != null) {
                    if (currentFragment.getClass().getSimpleName().equalsIgnoreCase("HomeFragment")) {
                        bottomNavigation.enableItemAtPosition(1);
                        bottomNavigation.setCurrentItem(1, false);
                    } else if (currentFragment.getClass().getSimpleName().equalsIgnoreCase("NewsFragment")) {
                        bottomNavigation.enableItemAtPosition(2);
                    }
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
        numberOfClicks++;
        showAd();
    }

    private void showAd() {
        if(adTimeStamp <= System.currentTimeMillis()) {
            if (numberOfClicks % 5 == 0) {
                mInterstitialAd.show();
            }
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
        numberOfClicks++;
        showAd();
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
