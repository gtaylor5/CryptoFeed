package com.cryptoinc.cryptofeed;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import Utilities.CurrencyInfo;
import Utilities.NewsInfo;
import Utilities.RequestSingleton;
import Utilities.Requests;
import Utilities.Sort;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnHomeFragmentListener, NewsFragment.OnNewsFragmentItemSelectedListener {

    AHBottomNavigation bottomNavigation;
    ProgressBar progressBar;

    CurrencyInfo currentCurrency;
    NewsInfo currentNews;

    public int numberOfClicks = 1;
    public long adTimeStamp = 0L;
    public InterstitialAd mInterstitialAd;
    public RewardedVideoAd mRewardVideoAd;

    public FirebaseUser currentUser;
    DatabaseReference favoritesRef;
    DatabaseReference adsRef;

    HashSet<String> favorites = new HashSet<>();

    HashMap<String, String> currencySymbolMap = new HashMap<>();
    volatile ArrayList<CurrencyInfo> currencies = new ArrayList<>();
    boolean favoritesChecked = false;
    double BTC_USD = 0.0;
    final String marketSummariesURL ="https://bittrex.com/api/v1.1/public/getmarketsummaries";
    final String currencyMetaDataURL = "https://bittrex.com/api/v1.1/public/getmarkets";

    private static final HashMap<String, Integer> fragmentTabMap;

    static {
        fragmentTabMap = new HashMap<>();
        fragmentTabMap.put(CurrencyDetailFragment.class.getSimpleName(), 1);
        fragmentTabMap.put(ProfileFragment.class.getSimpleName(), 0);
        fragmentTabMap.put(HomeFragment.class.getSimpleName(), 1);
        fragmentTabMap.put(PortfolioFragment.class.getSimpleName(), 2);
        fragmentTabMap.put(NewsFragment.class.getSimpleName(), 3);
    }

    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            while(running){
                getCurrencyData();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }
        }
    };

    public boolean running = true;

    public void start() {
        running = true;
    }

    public void terminate() {
        running = false;
    }

    public void getCurrencyNames() {
        RequestSingleton.getInstance(this).addToRequestQueue(Requests.getStringRequest(currencyMetaDataURL, new Requests.RequestFinishedListener() {
            @Override
            public void onRequestFinished(String response) {
                parseCurrencyNames(response);
            }
        }));
    }

    public void getCurrencyData () {
        RequestSingleton.getInstance(this).addToRequestQueue(Requests.getStringRequest(marketSummariesURL, new Requests.RequestFinishedListener() {
            @Override
            public void onRequestFinished(String response) {
                parseCurrencyPrices(response);
            }
        }));
    }

    public CurrencyInfo setCurrencyInfo(JSONObject jsonObject) throws JSONException {
        CurrencyInfo currencyInfo = new CurrencyInfo();
        currencyInfo.setAsk(jsonObject.getDouble("Ask"));
        currencyInfo.setBid(jsonObject.getDouble("Bid"));
        currencyInfo.setCreated(jsonObject.getString("Created"));
        currencyInfo.setHigh(jsonObject.getDouble("High"));
        currencyInfo.setLast(jsonObject.getDouble("Last"));
        currencyInfo.setLow(jsonObject.getDouble("Low"));
        currencyInfo.setOpenBuyOrders(jsonObject.getInt("OpenBuyOrders"));
        currencyInfo.setOpenSellOrders(jsonObject.getInt("OpenSellOrders"));
        currencyInfo.setPrevDay(jsonObject.getDouble("PrevDay"));
        if (jsonObject.getString("MarketName").split("-")[1].equalsIgnoreCase("BCC")){
            currencyInfo.setSymbol("BCH");
        } else {
            currencyInfo.setSymbol(jsonObject.getString("MarketName").split("-")[1]);
        }
        currencyInfo.setTimeStamp(jsonObject.getString("TimeStamp"));
        currencyInfo.setTimeStamp(jsonObject.getString("Volume"));
        currencyInfo.setName(currencySymbolMap.get(currencyInfo.getSymbol()));
        return currencyInfo;
    }

    public void parseCurrencyPrices(String response) {
        try {
            JSONObject object = new JSONObject(response);
            JSONArray array = object.getJSONArray("result");
            currencies.clear();
            for(int i = 0; i < array.length(); i++){
                JSONObject jsonObject = array.getJSONObject(i);
                if(!jsonObject.getString("MarketName").split("-")[0].equalsIgnoreCase("ETH")){
                    if(jsonObject.getString("MarketName").split("-")[1].equalsIgnoreCase("BTC")){
                        if(!favoritesChecked || favorites.contains(jsonObject.getString("MarketName").split("-")[1])){
                            currencies.add(setCurrencyInfo(jsonObject));
                        }
                        BTC_USD = jsonObject.getDouble("Last");
                    }
                    if(!jsonObject.getString("MarketName").split("-")[0].equalsIgnoreCase("USDT")){
                        if(favoritesChecked){
                            if(favorites.contains(jsonObject.getString("MarketName").split("-")[1])
                                    ||(favorites.contains("BCH") && jsonObject.getString("MarketName").split("-")[1].equalsIgnoreCase("BCC"))){
                                currencies.add(setCurrencyInfo(jsonObject));
                            }
                        } else {
                            currencies.add(setCurrencyInfo(jsonObject));
                        }
                    }
                }
            }
            for(CurrencyInfo info : currencies){
                info.setBTC_USD(BTC_USD);
            }

            Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer1);
            if(currentFrag == null){
                currentFrag = getSupportFragmentManager().findFragmentById(R.id.list_view);
            }
            if(currentFrag != null){
                switch(currentFrag.getClass().getSimpleName()){
                    case "HomeFragment": {
                        switch (((HomeFragment) currentFrag).sortType) {
                            case 1:
                                Collections.sort(currencies, Sort.sortPriceHighToLow);
                                break;
                            case 2:
                                Collections.sort(currencies, Sort.sortPriceLowToHigh);
                                break;
                            case 3:
                                Collections.sort(currencies, Sort.sortPercentHighToLow);
                                break;
                            case 4:
                                Collections.sort(currencies, Sort.sortPercentLowToHigh);
                                break;
                            default:
                                Collections.sort(currencies, Sort.sortPercentHighToLow);
                        }
                        if (((HomeFragment) currentFrag).favoritesChecked) {
                            if (favoritesRef != null) {
                                getFavorites();
                                ((HomeFragment) currentFrag).adapter.notifyDataSetChanged();
                            }
                        } else {
                            ((HomeFragment) currentFrag).adapter.notifyDataSetChanged();
                        }
                        break;
                    }
                }
            } 
            
            try {
                progressBar.setVisibility(View.INVISIBLE);
            }catch (Exception e){
                //
            }
        }catch (JSONException e) {
            //
        }
    }

    public void parseCurrencyNames(String response) {
        try {
            JSONObject object = new JSONObject(response);
            JSONArray array = object.getJSONArray("result");
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                if (jsonObject.getString("MarketCurrencyLong") == null) {
                    continue;
                }
                if (jsonObject.getString("MarketCurrency").equalsIgnoreCase("bcc")) {
                    currencySymbolMap.put("BCH", jsonObject.getString("MarketCurrencyLong"));
                } else {
                    currencySymbolMap.put(jsonObject.getString("MarketCurrency"), jsonObject.getString("MarketCurrencyLong"));
                }
            }
        } catch (Exception e){
            Log.d("ERROR", "parseCurrencyNames: " + e.getMessage());
        }
        start();
        new Thread(runnable).start();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getResources().getBoolean(R.bool.set_landscape)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_main_large);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_main);
        }
        setNavBar();
        initializeAds();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null) {
            initializeFirebaseDB();
            getFavorites();
        }

        if(currencySymbolMap.size() == 0){
            progressBar.setVisibility(View.VISIBLE);
            getCurrencyNames();
        } else {
            start();
            new Thread(runnable).start();
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
                if(currentUser != null) {
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Ads").child(currentUser.getUid());
                    reference.child("time").setValue(time);
                }
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
        AHBottomNavigationItem portfolioItem = new AHBottomNavigationItem("Portfolio", R.drawable.ic_trending_up_black_24dp);

        bottomNavigation = findViewById(R.id.bottom_nav);
        bottomNavigation.addItem(profileItem);
        bottomNavigation.addItem(homeItem);
        bottomNavigation.addItem(portfolioItem);
        bottomNavigation.addItem(newsItem);
        bottomNavigation.setDefaultBackgroundColor(getResources().getColor(R.color.card_color, null));
        bottomNavigation.setAccentColor(getResources().getColor(R.color.negative_red, null));
        bottomNavigation.setInactiveColor(getResources().getColor(R.color.white, null));
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.SHOW_WHEN_ACTIVE);
        bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                numberOfClicks++;
                showAd();
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
                            homeFragment.adapter.notifyDataSetChanged();
                        } else {
                            showFragment(fragment);
                        }
                        bottomNavigation.enableItemAtPosition(1);
                        break;
                    }
                    case 2: {
                        if(currentUser != null) {
                            Fragment fragment = fragmentManager.findFragmentByTag(PortfolioFragment.class.getSimpleName());
                            if (fragment == null) {
                                showFragment(new PortfolioFragment());
                            } else if (fragment.isVisible()) {
                                //
                            } else {
                                showFragment(fragment);
                            }
                            bottomNavigation.enableItemAtPosition(2);
                            break;
                        } else {
                            showPopUpWindow(2);
                            break;
                        }
                    }
                    case 3:  {
                        Fragment fragment = fragmentManager.findFragmentByTag(NewsFragment.class.getSimpleName());
                        if(fragment == null){
                            showFragment(new NewsFragment());
                        } else {
                            showFragment(fragment);
                        }
                        bottomNavigation.enableItemAtPosition(3);
                        break;
                    }
                }
                return true;
            }
        });
        bottomNavigation.enableItemAtPosition(1);
        bottomNavigation.setCurrentItem(1);
    }

    public void showPopUpWindow(final int sender) {
        final MaterialDialog materialDialog = new MaterialDialog.Builder(this)
                .backgroundColor(getResources().getColor(R.color.background, null))
                .titleColor(getResources().getColor(R.color.white, null))
                .contentColor(getResources().getColor(R.color.white, null))
                .customView(R.layout.sign_up_alert_2, false).build();
        View popUp = materialDialog.getCustomView();
        if(popUp != null) {
            final EditText email = popUp.findViewById(R.id.email);
            final EditText password = popUp.findViewById(R.id.password);
            final TextView switchView = popUp.findViewById(R.id.switchview);
            final TextView heading = popUp.findViewById(R.id.alertHeading);
            final Button login = popUp.findViewById(R.id.login);
            switchView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (switchView.getText().toString().contains("Sign In")) {
                        switchView.setText(R.string.newuser);
                        heading.setText("Sign In to Create Portfolio");
                        login.setText("Sign In");
                    } else {
                        switchView.setText(R.string.already);
                        heading.setText("Sign Up to Create Portfolio");
                        login.setText("Sign Up");
                    }
                }
            });

            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    authenticateNewUser(materialDialog, email, password, sender);
                }
            });
            materialDialog.show();
        }
    }

    public void authenticateNewUser(final MaterialDialog popupWindow, final EditText email, final EditText password, final int sender) {
        if(email.getText().toString().contains("@") && password.getText().toString().length() != 0){
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(FirebaseAuth.getInstance().getCurrentUser() != null) {
                        Toast.makeText(getApplicationContext(), "Sign In Successful!", Toast.LENGTH_LONG).show();
                        currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        getFavorites();
                        bottomNavigation.setCurrentItem(sender, true);
                        popupWindow.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(FirebaseAuth.getInstance().getCurrentUser() != null){
                                Toast.makeText(getApplicationContext(), "Sign Up Successful!", Toast.LENGTH_LONG).show();
                                currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                getFavorites();
                                popupWindow.dismiss();
                            }
                        }
                    });
                }
            });
        } else if (!email.getText().toString().contains("@")){
            Toast.makeText(getApplicationContext(), "Invalid Email Address. Please try again.", Toast.LENGTH_LONG).show();
        } else if (password.getText().toString().length() == 0){
            Toast.makeText(getApplicationContext(), "You must enter a pasaword.", Toast.LENGTH_LONG).show();

        }
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
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer1);
                if (currentFragment != null) {
                    //bottomNavigation.enableItemAtPosition(fragmentTabMap.get(currentFragment.getClass().getSimpleName()));
                    if(currentFragment.getClass().getSimpleName().equalsIgnoreCase("CurrencyDetailFragment")){
                        bottomNavigation.setCurrentItem(fragmentTabMap.get(currentFragment.getClass().getSimpleName()), false);
                    } else {
                        bottomNavigation.setCurrentItem(fragmentTabMap.get(currentFragment.getClass().getSimpleName()), true);
                    }
                }
            } catch (Exception e) {
                Log.d("ERROR",  e.getMessage());
            }
        } else {
            try {
                getSupportFragmentManager().popBackStackImmediate();
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.list_view);
                if (currentFragment != null) {
                    bottomNavigation.enableItemAtPosition(fragmentTabMap.get(currentFragment.getClass().getSimpleName()));
                    bottomNavigation.setCurrentItem(fragmentTabMap.get(currentFragment.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                //
            }
        }
    }

    public void clearBackStackAfterLogout() {
        FragmentManager fm = getSupportFragmentManager();
        for(int i = 0; i < fm.getBackStackEntryCount(); i++){
            fm.popBackStackImmediate();
        }
        bottomNavigation.setCurrentItem(1, true);
        if(fm.findFragmentById(R.id.detailView) != null){
            Fragment currentFragment = fm.findFragmentById(R.id.detailView);
            if(currentFragment.getClass().getSimpleName().equalsIgnoreCase("PortfolioFragment")){
                FragmentTransaction ft = fm.beginTransaction();
                ft.remove(currentFragment);
                ft.commit();
            }
        }
    }

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

    @Override
    protected void onPause() {
        super.onPause();
        terminate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
        new Thread(runnable).start();
    }
}
