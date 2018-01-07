package com.cryptoinc.cryptofeed;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mancj.materialsearchbar.MaterialSearchBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import Utilities.CurrencyInfo;
import Utilities.CurrencyInfoAdapter;
import Utilities.RequestSingleton;
import Utilities.Requests;
import Utilities.Sort;

public class HomeFragment extends Fragment {

    OnHomeFragmentListener mListener;
    CurrencyInfoAdapter adapter;

    // Views and View Management

    RecyclerView recyclerView;
    MaterialSearchBar searchBar;
    LinearLayoutManager manager;
    View view;


    // Currency Related Data

    HashMap<String, String> currencySymbolMap = new HashMap<>();
    volatile ArrayList<CurrencyInfo> currencies = new ArrayList<>();
    int sortType = 1;
    boolean favoritesChecked = false;
    double BTC_USD = 0.0;
    final String marketSummariesURL ="https://bittrex.com/api/v1.1/public/getmarketsummaries";
    final String currencyMetaDataURL = "https://bittrex.com/api/v1.1/public/getmarkets";

    // Threading

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


    public HomeFragment(){}

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        if(view == null) {
            setViews(inflater, container);
        }
        return view;
    }

    // Initialize Views, and Search bar filtering algorithm

    public void setViews(final LayoutInflater inflater, final ViewGroup container) {
        view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.currencyList);
        adapter = new CurrencyInfoAdapter(inflater, currencies, ((MainActivity)getActivity()).favorites);
        recyclerView.setAdapter(adapter);
        manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayout.VERTICAL);
        recyclerView.setLayoutManager(manager);
        adapter.setCurrencyInfoListListener(new CurrencyInfoAdapter.CurrencyInfoListListener() {
            @Override
            public void currencySelected(CurrencyInfo info) {
                mListener.onHomeFragmentItemSelected(info);
            }
        });
        adapter.favorites.addAll(((MainActivity)getActivity()).favorites);
        setSearchBar(inflater, container);
    }

    public void setSearchBar(final LayoutInflater inflater, final ViewGroup container) {
        searchBar = view.findViewById(R.id.searchBar);
        searchBar.hideSuggestionsList();
        searchBar.setArrowIconTint(getResources().getColor(R.color.negative_red, null));
        searchBar.setClearIconTint(getResources().getColor(R.color.negative_red, null));
        searchBar.setCardViewElevation((int)getResources().getDimension(R.dimen.elevation));
        searchBar.setNavButtonEnabled(false);
        searchBar.setMenuIconTint(getResources().getColor(R.color.negative_red, null));
        searchBar.inflateMenu(R.layout.top_menu);
        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() != 0) {
                    filter(s.toString());
                } else {
                    adapter.updateList(currencies);
                }
            }
        });
        setSearchBarMenu(inflater, container);
    }

    public void setSearchBarMenu(final LayoutInflater inflater, final ViewGroup container) {
        searchBar.getMenu().setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ((MainActivity)getActivity()).progressBar.setVisibility(View.VISIBLE);
                switch (item.getItemId()){
                    case R.id.pricehilo:
                        sortType = 1;
                        break;
                    case R.id.pricelohi:
                        sortType = 2;
                        checkIfChecked(item);
                        break;
                    case R.id.percenthilo:
                        sortType = 3;
                        checkIfChecked(item);
                        break;
                    case R.id.percentlohi:
                        sortType = 4;
                        checkIfChecked(item);
                        break;
                    case R.id.fav:
                        if(!item.isChecked()) {
                            item.setChecked(true);
                            favoritesChecked = true;
                            FirebaseUser user = ((MainActivity)getActivity()).currentUser;
                            if (user == null) {
                                showPopUpWindow(inflater, container);
                                item.setChecked(false);
                                favoritesChecked = false;
                            }
                        } else {
                            item.setChecked(false);
                            favoritesChecked = false;
                        }
                        break;
                }
                selectOne();
                return  false;
            }
        });
    }

    public void checkIfChecked(MenuItem item) {
        if(item.isChecked()){
            item.setChecked(false);
            sortType = 1;
        }
    }

    public void selectOne(){
        for(int i = 0; i < searchBar.getMenu().getMenu().size() - 1; i++){
            if(i == sortType-1){
                searchBar.getMenu().getMenu().getItem(i).setChecked(true);
            } else {
                searchBar.getMenu().getMenu().getItem(i).setChecked(false);
            }
        }
    }

    public void showPopUpWindow(LayoutInflater inflater, ViewGroup container) {
        final PopupWindow popupWindow;
        View popUp = inflater.inflate(R.layout.sign_up_alert, container, false);
        final EditText email = popUp.findViewById(R.id.email);
        final EditText password = popUp.findViewById(R.id.password);
        final Button login = popUp.findViewById(R.id.signUp);
        final TextView switchView = popUp.findViewById(R.id.switchview);
        final TextView heading = popUp.findViewById(R.id.alertHeading);
        switchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchView.getText().toString().contains("Sign In")) {
                    switchView.setText(R.string.newuser);
                    heading.setText(R.string.please_sign_in_to_add_favorites);
                } else {
                    switchView.setText(R.string.already);
                    heading.setText(R.string.please_sign_up_to_add_favorites);
                }
            }
        });

        popupWindow = new PopupWindow(popUp, getActivity().getWindow().getAttributes().width, getActivity().getWindow().getAttributes().height, true);
        popupWindow.showAtLocation(popUp, Gravity.CENTER_VERTICAL, 0, 0);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authenticateNewUser(popupWindow, email, password);
            }
        });
    }

    public void authenticateNewUser(final PopupWindow popupWindow, final EditText email, final EditText password) {
        if(email.getText().toString().contains("@") && password.getText().toString().length() != 0){
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(FirebaseAuth.getInstance().getCurrentUser() != null){
                        Toast.makeText(getActivity(), "Sign In Successful!", Toast.LENGTH_LONG).show();
                        ((MainActivity)getActivity()).currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        ((MainActivity)getActivity()).initializeFirebaseDB();
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
                                Toast.makeText(getActivity(), "Sign Up Successful!", Toast.LENGTH_LONG).show();
                                ((MainActivity)getActivity()).currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                ((MainActivity)getActivity()).initializeFirebaseDB();
                                popupWindow.dismiss();
                            }
                        }
                    });
                }
            });
        } else if (!email.getText().toString().contains("@")){
            Toast.makeText(getActivity(), "Invalid Email Address. Please try again.", Toast.LENGTH_LONG).show();
        } else if (password.getText().toString().length() == 0){
            Toast.makeText(getActivity(), "You must enter a pasaword.", Toast.LENGTH_LONG).show();
        }
    }

    public void getFavorites() {
        FirebaseUser user = ((MainActivity)getActivity()).currentUser;
        if(user != null) {
            if (((MainActivity)getActivity()).favorites.size() == 0) {
                Toast.makeText(getActivity(), "You do not currently have any fravorites.", Toast.LENGTH_LONG).show();
                return;
            }
            adapter.favorites = (((MainActivity)getActivity()).favorites);
            adapter.notifyDataSetChanged();
        }

    }

    void filter(String text){
        List<CurrencyInfo> temp = new ArrayList<>();
        for(CurrencyInfo currencyInfo : currencies){
            if(currencyInfo.getSymbol().toLowerCase().contains(text.toLowerCase()) || (currencyInfo.getName() != null && currencyInfo.getName().toLowerCase().contains(text.toLowerCase()))){
                temp.add(currencyInfo);
            }

        }
        adapter.updateList(temp);
    }

    //Currency Data Requests / Parsing

    public void getCurrencyNames() {
        RequestSingleton.getInstance(getActivity()).addToRequestQueue(Requests.getStringRequest(currencyMetaDataURL, new Requests.RequestFinishedListener() {
            @Override
            public void onRequestFinished(String response) {
                parseCurrencyNames(response);
            }
        }));
    }

    public void getCurrencyData () {
        RequestSingleton.getInstance(getActivity()).addToRequestQueue(Requests.getStringRequest(marketSummariesURL, new Requests.RequestFinishedListener() {
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
                        if(!favoritesChecked || ((MainActivity)getActivity()).favorites.contains(jsonObject.getString("MarketName").split("-")[1])){
                            currencies.add(setCurrencyInfo(jsonObject));
                        }
                        BTC_USD = jsonObject.getDouble("Last");
                    }
                    if(!jsonObject.getString("MarketName").split("-")[0].equalsIgnoreCase("USDT")){
                        if(favoritesChecked){
                            if(((MainActivity)getActivity()).favorites.contains(jsonObject.getString("MarketName").split("-")[1])
                                    ||(((MainActivity)getActivity()).favorites.contains("BCH") && jsonObject.getString("MarketName").split("-")[1].equalsIgnoreCase("BCC"))){
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
            switch (sortType) {
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
            if(favoritesChecked){
                if (((MainActivity)getActivity()).favoritesRef != null) {
                    getFavorites();
                    adapter.notifyDataSetChanged();
                }
            }else{
                adapter.notifyDataSetChanged();
            }

            try {
                ((MainActivity) getActivity()).progressBar.setVisibility(View.INVISIBLE);
            }catch (Exception e){
                //
            }
        }catch (JSONException e){
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

    // Lifecycle Methods

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnHomeFragmentListener) {
            mListener = (OnHomeFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNewsFragmentItemSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if(currencySymbolMap.size() == 0) {
            ((MainActivity)getActivity()).progressBar.setVisibility(View.VISIBLE);
            getCurrencyNames();
            selectOne();
        }else {
            start();
            new Thread(runnable).start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        terminate();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public interface OnHomeFragmentListener {
        void onHomeFragmentItemSelected(CurrencyInfo currencyInfo);
    }
}
