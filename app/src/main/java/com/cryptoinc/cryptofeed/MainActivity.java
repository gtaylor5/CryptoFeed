package com.cryptoinc.cryptofeed;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import Utilities.CurrencyInfo;
import Utilities.NewsInfo;
import Utilities.RequestSingleton;
import Utilities.Requests;
import Utilities.Sort;
import io.socket.client.IO;
import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnHomeFragmentListener, NewsFragment.OnNewsFragmentItemSelectedListener {

    // UI

    AHBottomNavigation bottomNavigation;
    ProgressBar progressBar;

    // Models

    CurrencyInfo currentCurrency;
    NewsInfo currentNews;

    // Ads

    int numberOfClicks = 1;
    long adTimeStamp = 0L;
    InterstitialAd mInterstitialAd;
    RewardedVideoAd mRewardVideoAd;

    // User Information / Database

    public FirebaseUser currentUser;
    DatabaseReference favoritesRef;
    DatabaseReference adsRef;
    HashSet<String> favorites = new HashSet<>();

    // Currency Information

    private static final LinkedHashMap<String, Integer> currencySocketFields;
    private static final HashMap<String, Integer> fragmentTabMap;
    HashMap<String, String> currencySymbolMap = new HashMap<>();
    ArrayList<CurrencyInfo> currencies = new ArrayList<>();

    boolean favoritesChecked = false;
    boolean coinsLoaded = false;
    double BTC_USD = 0.0;
    String currencyMetaDataURL = "https://bittrex.com/api/v1.1/public/getmarkets";

    Socket socket;
    JSONArray subarr = new JSONArray();

    int navBarPosition = 1;


    static {
        fragmentTabMap = new HashMap<>();
        fragmentTabMap.put(CurrencyDetailFragment.class.getSimpleName(), 1);
        fragmentTabMap.put(ProfileFragment.class.getSimpleName(), 0);
        fragmentTabMap.put(HomeFragment.class.getSimpleName(), 1);
        fragmentTabMap.put(PortfolioFragment.class.getSimpleName(), 2);
        fragmentTabMap.put(NewsFragment.class.getSimpleName(), 3);

        currencySocketFields = new LinkedHashMap<>();
        currencySocketFields.put("TYPE", 0x0);
        currencySocketFields.put("MARKET", 0x0);
        currencySocketFields.put("FROMSYMBOL", 0x0);
        currencySocketFields.put("TOSYMBOL", 0x0);
        currencySocketFields.put("FLAGS", 0x0);
        currencySocketFields.put("PRICE", 0x1);
        currencySocketFields.put("BID", 0x2);
        currencySocketFields.put("OFFER", 0x4);
        currencySocketFields.put("LASTUPDATE", 0x8);
        currencySocketFields.put("AVG", 0x10);
        currencySocketFields.put("LASTVOLUME", 0x20);
        currencySocketFields.put("LASTVOLUMETO", 0x40);
        currencySocketFields.put("LASTTRADEID", 0x80);
        currencySocketFields.put("VOLUMEHOUR", 0x100);
        currencySocketFields.put("VOLUMEHOURTO", 0x200);
        currencySocketFields.put("VOLUME24HOUR", 0x400);
        currencySocketFields.put("VOLUME24HOURTO",0x800);
        currencySocketFields.put("OPENHOUR", 0x1000);
        currencySocketFields.put("HIGHHOUR", 0x2000);
        currencySocketFields.put("LOWHOUR", 0x4000);
        currencySocketFields.put("OPEN24HOUR", 0x8000);
        currencySocketFields.put("HIGH24HOUR", 0x10000);
        currencySocketFields.put("LOW24HOUR", 0x20000);
        currencySocketFields.put("LASTMARKET", 0x40000);
    }


    // Socket and Streaming


    private void startSocket() throws URISyntaxException {

        // BTC/USD pair used to convert all other currencies to USD

        subarr.put("5~CCCAGG~BTC~USD");

        // Fill Sub Array with all currencies with BTC as base currency.

        for(String key : currencySymbolMap.keySet()) {
            subarr.put("5~CCCAGG~" + key + "~BTC");
        }

        // socket setup
        socket = IO.socket("https://streamer.cryptocompare.com/");
        socket.on(Socket.EVENT_CONNECT, args -> {

            // once socket is connected subscribe to the currencies in the subarr.

            try {

                JSONObject eventArgs = new JSONObject();
                eventArgs.put("subs", subarr);
                socket.emit("SubAdd", eventArgs); // subscription step

            } catch (JSONException e){}


        })

                // on response from socket.


                // Message Format:

                // '{SubscriptionId}~{ExchangeName}~{FromCurrency}~{ToCurrency}~{Flag}~{Price}~{LastUpdate}~{LastVolume}~{LastVolumeTo}~{LastTradeId}~{Volume24h}~{Volume24hTo}~{LastMarket}'
                // Subscription ID = { 0 : Trade , 2 : Current, 5 : Current Agg }
                // Flag = { 1 : Price Up, 2 : Price Down, 4 : Price Unchanged


                .on("m", args -> {
                    String[] message = ((String) args[0]).split("~");
                    if(coinsLoaded) {

                        //if bitcoin to usd save that value
                        setBTCUSD(message);
                        if(message.length > 5) {
                            if (!message[4].equalsIgnoreCase("4")) {
                                parseCurrencyPrices((String) args[0]);
                                runOnUiThread(this::updateTable);
                            }
                        }

                    } else {

                        if(((String)args[0]).split("~")[1].equalsIgnoreCase("LOADCOMPLETE")) {

                            coinsLoaded = true;
                            runOnUiThread(this::updateTable);

                        } else {

                            setBTCUSD(message);
                            initializeCurrencies((String) args[0]);

                        }
                    }

                }); // on response

        socket.connect(); // connect to socket defined above.
    }

    private void setBTCUSD(String[] message) {
        if(message[2].equalsIgnoreCase("BTC") && message.length > 5 && !message[4].equalsIgnoreCase("4")) {
            this.BTC_USD = Double.parseDouble(message[5]);
        } else if(message[2].equalsIgnoreCase("BTC") && !coinsLoaded  && message.length > 5) {
            this.BTC_USD = Double.parseDouble(message[5]);
        }
    }

    public void initializeCurrencies(String response) {
        try {
            JSONObject currencyObject = getJsonObjectFromString(response);
            if (currencyObject == null) return;
            currencies.add(setCurrencyInfo(currencyObject));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private JSONObject getJsonObjectFromString(String response) throws JSONException {
        String[] values = response.split("~");
        if(values.length < 3) return null;
        String mask = values[values.length - 1];
        Long maskAsInt = 0L;
        try {
            maskAsInt = Long.parseLong(mask, 16);
        } catch (Exception e) {
            return null;
        }
        int currentField = 0;
        JSONObject currencyObject = new JSONObject();
        for (String key : currencySocketFields.keySet()) {
            if (currencySocketFields.get(key) == 0x0) {
                currencyObject.put(key, values[currentField]);
                currentField++;
            } else if ((maskAsInt & currencySocketFields.get(key)) != 0L) {
                if (key.equalsIgnoreCase("LASTMARKET")) {
                    currencyObject.put(key, values[currentField]);
                } else {
                    try {
                        if(key.equalsIgnoreCase("LASTUPDATE")) {
                            currencyObject.put(key, values[currentField]);
                        } else {
                            currencyObject.put(key, Float.parseFloat(values[currentField]));
                        }
                    } catch (Exception e) {
                        // transaction id was a SHA-digest
                    }

                }
                currentField++;
            }
        }
        return currencyObject;
    }

    public void parseCurrencyPrices(String response) {
        try {
            Log.d("RESP", "parseCurrencyPrices: " + response);
            JSONObject currencyObject = getJsonObjectFromString(response);
            for(int i = 0; i < currencies.size(); i++) {
                if(currencies.get(i).getSymbol().equalsIgnoreCase(currencyObject.getString("FROMSYMBOL"))) {
                    if(currencies.get(i).getSymbol().equalsIgnoreCase("BTC")){
                        currencies.get(i).setLast(currencyObject.getDouble("PRICE"));
                        if(currencyObject.has("LASTVOLUMETO")) {
                            currencies.get(i).setVolume(currencyObject.getDouble("LASTVOLUMETO"));
                        }
                    } else {
                        currencies.get(i).setLast(currencyObject.getDouble("PRICE") * BTC_USD);
                        if(currencyObject.has("LASTVOLUMETO")) {
                            currencies.get(i).setVolume(currencyObject.getDouble("LASTVOLUMETO") * BTC_USD);
                        }
                    }
                    if(currencyObject.has("LASTUPDATE")) {
                        currencies.get(i).setTimeStamp(currencyObject.getString("LASTUPDATE"));
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTable() {
        Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer1);
        if(currentFrag == null){
            currentFrag = getSupportFragmentManager().findFragmentById(R.id.list_view);
        }
        if(currentFrag != null && currentFrag.getClass().getSimpleName().equalsIgnoreCase("HomeFragment")){
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
        } else if (currentFrag != null && currentFrag.getClass().getSimpleName().equalsIgnoreCase("CurrencyDetailFragment")) {
            ((CurrencyDetailFragment) currentFrag).updateViews();
        }
        try {
            progressBar.setVisibility(View.INVISIBLE);
        }catch (Exception e){
            //
        }
    }


    //Currency meta data, etc.


    public void getCurrencyMetaData() {
        RequestSingleton.getInstance(this).addToRequestQueue(Requests.getStringRequest(currencyMetaDataURL, response -> {
            parseCurrencyNames(response);
            try {
                startSocket();
            } catch (Exception e){}
        }));
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

            //get conversion to bitcoin.

        } catch (Exception e){
            Log.d("ERROR", "parseCurrencyNames: " + e.getMessage());
        }
    }

    public CurrencyInfo setCurrencyInfo(JSONObject jsonObject) throws JSONException {
        CurrencyInfo currencyInfo = new CurrencyInfo();
        if(jsonObject.getString("FROMSYMBOL").equalsIgnoreCase("BTC")) {
            currencyInfo.setLast(jsonObject.getDouble("PRICE"));
            currencyInfo.setLow(jsonObject.getDouble("LOWHOUR"));
            currencyInfo.setPrevDay(jsonObject.getDouble("HIGH24HOUR"));
            currencyInfo.setHigh24Hr(jsonObject.getDouble("HIGH24HOUR"));
            currencyInfo.setLow24Hr(jsonObject.getDouble("LOW24HOUR"));
            currencyInfo.setVolume(jsonObject.getDouble("VOLUME24HOUR"));
            currencyInfo.setVolume24Hr(jsonObject.getDouble("VOLUME24HOUR"));
        } else {
            currencyInfo.setLast(jsonObject.getDouble("PRICE")*BTC_USD);
            currencyInfo.setLow(jsonObject.getDouble("LOWHOUR")*BTC_USD);
            currencyInfo.setPrevDay(jsonObject.getDouble("HIGH24HOUR")*BTC_USD);
            currencyInfo.setHigh24Hr(jsonObject.getDouble("HIGH24HOUR")*BTC_USD);
            currencyInfo.setLow24Hr(jsonObject.getDouble("LOW24HOUR")*BTC_USD);
            currencyInfo.setVolume(jsonObject.getDouble("VOLUME24HOUR")*BTC_USD);
            currencyInfo.setVolume24Hr(jsonObject.getDouble("VOLUME24HOUR")*BTC_USD);
        }

        currencyInfo.setSymbol(jsonObject.getString("FROMSYMBOL"));
        currencyInfo.setTimeStamp(jsonObject.getString("LASTUPDATE"));

        currencyInfo.setName(currencySymbolMap.get(currencyInfo.getSymbol()));
        return currencyInfo;
    }


    // Activity LifeCycle Methods


    @Override
    protected void onResume() {
        super.onResume();
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
            getCurrencyMetaData();
        } else {
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            socket.emit("SubRemove", (new JSONObject()).put("subs", subarr));
        } catch (JSONException e){}
    }

    // RecyclerView item selection handlers.

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

    // Fragment / UI Managment

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
    public void onBackPressed() {
        if(getSupportFragmentManager().getBackStackEntryCount() > 0){
            RequestSingleton.getInstance(this).getRequestQueue().cancelAll("");
            backstackFragment();
        } else {
            super.onBackPressed();
        }
    }

    public void showPopUpWindow(final int sender, String signUpHeading, String loginHeading, String signUpAction, String loginAction) {
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
            switchView.setOnClickListener(v -> {
                if(switchView.getText().toString().contains("Already Registered?")) {
                    heading.setText(loginHeading);
                    login.setText(loginAction);
                    switchView.setText("New User? Sign Up");
                } else {
                    heading.setText(signUpHeading);
                    login.setText(signUpAction);
                    switchView.setText("Already Registered? Sign In");
                }
            });

            login.setOnClickListener(view -> authenticateNewUser(materialDialog, email, password, sender));
            materialDialog.show();
            materialDialog.setOnDismissListener(dialog -> bottomNavigation.setCurrentItem(navBarPosition, false));
        }
    }


    // Firebase

    // Called in on Create
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
            this.currentUser = user;
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
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    private void showAd() {
        if(adTimeStamp <= System.currentTimeMillis()) {
            if (numberOfClicks % 7 == 0) {
                mInterstitialAd.show();
            }
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
            public void onRewardedVideoAdLoaded() {}

            @Override
            public void onRewardedVideoAdOpened() {}

            @Override
            public void onRewardedVideoStarted() {}

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
            public void onRewardedVideoAdLeftApplication() {}

            @Override
            public void onRewardedVideoAdFailedToLoad(int i) {}
        });
    }

    public void authenticateNewUser(final MaterialDialog popupWindow, final EditText email, final EditText password, final int sender) {
        if(email.getText().toString().contains("@") && password.getText().toString().length() != 0){
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(task -> {
                if(FirebaseAuth.getInstance().getCurrentUser() != null) {
                    Toast.makeText(getApplicationContext(), "Sign In Successful!", Toast.LENGTH_LONG).show();
                    currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    getFavorites();
                    bottomNavigation.setCurrentItem(sender, true);
                    popupWindow.dismiss();
                }
            }).addOnFailureListener(e -> FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(task -> {
                if(FirebaseAuth.getInstance().getCurrentUser() != null){
                    Toast.makeText(getApplicationContext(), "Sign Up Successful!", Toast.LENGTH_LONG).show();
                    currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    getFavorites();
                    popupWindow.dismiss();
                }
            }));
        } else if (!email.getText().toString().contains("@")){
            Toast.makeText(getApplicationContext(), "Invalid Email Address. Please try again.", Toast.LENGTH_LONG).show();
        } else if (password.getText().toString().length() == 0){
            Toast.makeText(getApplicationContext(), "You must enter a pasaword.", Toast.LENGTH_LONG).show();
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
        bottomNavigation.setOnTabSelectedListener((position, wasSelected) -> {
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
                    navBarPosition = 0;
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
                    navBarPosition = 1;
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
                        navBarPosition = 2;
                        break;
                    } else {
                        showPopUpWindow(2, "Please Sign Up to Create a Portfolio", "Sign In To View Portfolio", "Sign Up", "Login");
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
                    navBarPosition = 3;
                    break;
                }
            }
            return true;
        });
        bottomNavigation.enableItemAtPosition(1);
        bottomNavigation.setCurrentItem(1);
    }






}
