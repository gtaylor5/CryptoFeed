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
import com.google.firebase.analytics.FirebaseAnalytics;
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

    FirebaseAnalytics mFirebaseAnalytics;

    // Currency Related Data

    int sortType = 1;
    boolean favoritesChecked = false;


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
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());
        recyclerView = view.findViewById(R.id.currencyList);
        adapter = new CurrencyInfoAdapter(inflater, ((MainActivity)getActivity()).currencies, ((MainActivity)getActivity()).favorites, getActivity());
        recyclerView.setAdapter(adapter);
        manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayout.VERTICAL);
        recyclerView.setLayoutManager(manager);
        adapter.setCurrencyInfoListListener(info -> mListener.onHomeFragmentItemSelected(info));
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
                    if(getActivity() != null) {
                        adapter.updateList(((MainActivity) getActivity()).currencies);
                    }
                }
            }
        });
        setSearchBarMenu();
    }

    public void trackAnalytics(String key, String value, String event){
        Bundle params = new Bundle();
        params.putString(key, value);
        if(mFirebaseAnalytics != null) {
            mFirebaseAnalytics.logEvent(event, params);
        }
    }

    public void setSearchBarMenu() {
        searchBar.getMenu().setOnMenuItemClickListener(item -> {
            if(getActivity()!= null) {
                ((MainActivity) getActivity()).progressBar.setVisibility(View.VISIBLE);
            }
            switch (item.getItemId()){
                case R.id.pricehilo:
                    sortType = 1;
                    trackAnalytics("sort", "price_hi_to_low", "sort_menu_clicked");
                    break;
                case R.id.pricelohi:
                    sortType = 2;
                    trackAnalytics("sort", "price_low_to_high", "sort_menu_clicked");
                    checkIfChecked(item);
                    break;
                case R.id.percenthilo:
                    sortType = 3;
                    trackAnalytics("sort", "percent_high_to_low", "sort_menu_clicked");
                    checkIfChecked(item);
                    break;
                case R.id.percentlohi:
                    sortType = 4;
                    trackAnalytics("sort", "price_high_to_low", "sort_menu_clicked");
                    checkIfChecked(item);
                    break;
                case R.id.fav:
                    trackAnalytics("sort", "favorites checked", "sort_menu_clicked");
                    if(!item.isChecked()) {
                        item.setChecked(true);
                        ((MainActivity)getActivity()).favoritesChecked = true;
                        FirebaseUser user = ((MainActivity)getActivity()).currentUser;
                        if (user == null) {
                            ((MainActivity)getActivity()).showPopUpWindow(1, "Sign Up to Add Favorites",
                                    "Login To See Favorites", "Sign Up", "Login");
                            item.setChecked(false);
                            ((MainActivity)getActivity()).favoritesChecked = false;
                        }
                    } else {
                        item.setChecked(false);
                        ((MainActivity)getActivity()).favoritesChecked = false;
                    }
                    break;
            }
            selectOne();
            return  false;
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

    void filter(String text){
        if(getActivity() != null) {
            List<CurrencyInfo> temp = new ArrayList<>();
            for (CurrencyInfo currencyInfo : ((MainActivity) getActivity()).currencies) {
                if (currencyInfo.getSymbol().toLowerCase().contains(text.toLowerCase()) || (currencyInfo.getName() != null && currencyInfo.getName().toLowerCase().contains(text.toLowerCase()))) {
                    temp.add(currencyInfo);
                }

            }
            adapter.updateList(temp);
        }
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
        if(getActivity() != null) {
            adapter.currencies = ((MainActivity) getActivity()).currencies;
            adapter.notifyDataSetChanged();
            selectOne();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public interface OnHomeFragmentListener {
        void onHomeFragmentItemSelected(CurrencyInfo currencyInfo);
    }
}
