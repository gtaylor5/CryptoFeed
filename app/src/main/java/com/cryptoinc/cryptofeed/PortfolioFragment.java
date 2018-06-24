package com.cryptoinc.cryptofeed;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Utilities.CurrencyInfo;
import Utilities.CurrencyInfoPortfolioAdapter;

import android.support.v7.widget.helper.ItemTouchHelper.*;

public class PortfolioFragment extends Fragment {

    FloatingActionButton addToPortfolio;
    TextView portfolioValue;
    TextView portfolioDollarChange;
    TextView portfolioPercentageChange;
    RecyclerView portfolioList;
    CurrencyInfoPortfolioAdapter adapter;
    PieChart pieChart;
    LinearLayoutManager manager;

    NestedScrollView scrollView;

    FirebaseUser currentUser;
    DatabaseReference databaseReference;


    String[] currencyList;
    HashMap<String, Double> portfolio = new HashMap<>(); // (BTC, 23.0000)

    private DecimalFormat defaultformatter = new DecimalFormat("$#,###.00");
    private DecimalFormat percentFormatter = new DecimalFormat("#,###.00 %");
    private DecimalFormat percentFormatterLessThanZero = new DecimalFormat("0.000 %");
    private DecimalFormat priceFormatterLessThanZero = new DecimalFormat("$ 0.000");
    private double value;

    public PortfolioFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portfolio, container, false);
        addToPortfolio = view.findViewById(R.id.addToPortfolio);
        portfolioValue = view.findViewById(R.id.portfolio_value);
        portfolioDollarChange = view.findViewById(R.id.portfolioDollarChange);
        portfolioPercentageChange = view.findViewById(R.id.portfolioPercentageChange);
        scrollView = view.findViewById(R.id.scrollView);

        pieChart = view.findViewById(R.id.portfolioChart);

        portfolioList = view.findViewById(R.id.portfolioList);
        if(getActivity() != null) {
            adapter = new CurrencyInfoPortfolioAdapter(inflater, portfolio);
            currentUser = ((MainActivity) getActivity()).currentUser;
        }

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new Callback() {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(ItemTouchHelper.RIGHT, ItemTouchHelper.LEFT);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                databaseReference.child(adapter.currencies.get(viewHolder.getAdapterPosition()).getSymbol()).setValue(null);
            }
        });
        itemTouchHelper.attachToRecyclerView(portfolioList);

        if(getActivity() != null) {
            currencyList = ((MainActivity) getActivity()).currencySymbolMap.keySet().toArray(new String[0]);
        }
        manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayout.VERTICAL);
        portfolioList.setLayoutManager(manager);
        portfolioList.setAdapter(adapter);
        addToPortfolio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentUser != null && getActivity() != null) {
                    showDialog();
                    /*
                    new MaterialDialog.Builder(getActivity())
                            .title("Choose a Currency")
                            .customView(R.layout.auto_complete, false)
                            .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View itemView, int which, final CharSequence text) {
                                    new MaterialDialog.Builder(getActivity())
                                            .title("Set Quantity")
                                            .content(text)
                                            .inputType(InputType.TYPE_NUMBER_FLAG_DECIMAL)
                                            .input("1.2345", null, false, new MaterialDialog.InputCallback() {
                                                @Override
                                                public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                                                    dialog.dismiss();
                                                }
                                            }).show();
                                    dialog.dismiss();
                                    return true;
                                }
                            }).show();
                            */
                }
            }
        });

        return view;
    }

    void showDialog() {
        if(getActivity() != null) {

            MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
                    .title("Add To Portfolio")
                    .backgroundColor(getResources().getColor(R.color.background, null))
                    .titleColor(getResources().getColor(R.color.white, null))
                    .customView(R.layout.auto_complete, true)
                    .positiveText("Add").build();
            View view = materialDialog.getCustomView();
            if (view != null ) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, currencyList);
                final AutoCompleteTextView autoCompleteTextView = view.findViewById(R.id.autoCompleteTextView);
                autoCompleteTextView.setThreshold(1);
                autoCompleteTextView.setAdapter(adapter);
                final EditText quantity = view.findViewById(R.id.quantity);

                materialDialog.getBuilder().onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            if(quantity.getText().length() != 0 && ((MainActivity)getActivity()).currencySymbolMap.keySet().contains(autoCompleteTextView.getText().toString())) {
                                databaseReference.child(autoCompleteTextView.getText().toString()).setValue(Double.parseDouble(quantity.getText().toString()));
                            } else if(quantity.getText().length() == 0){
                                Toast.makeText(getActivity(),"Please enter a valid quantity.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(),"Invalid Symbol. Please try again.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e){
                            Toast.makeText(getActivity(),"Error adding to your portfolio. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                materialDialog.show();
            }
        }
    }

    public void notifyDataSetChanged() {
        if(isAdded()) {
            calculatePortfolioValue();
            calculatePortfolioBreakdown();
            getCurrencySubSet();
        }
    }

    public void getCurrencySubSet() {
        ArrayList<CurrencyInfo> temp = new ArrayList<>();
        if(getActivity() != null){
            for(int i = 0; i < ((MainActivity)getActivity()).currencies.size(); i++){
                if(portfolio.keySet().contains(((MainActivity)getActivity()).currencies.get(i).getSymbol())){
                    temp.add(((MainActivity)getActivity()).currencies.get(i));
                }
            }
            adapter.updateList(temp);
        }
    }

    private void calculatePortfolioValue() {
        value = 0.0;
        double percentageChange = 0.0;
        double sumPrevDay = 0;
        for(String key : portfolio.keySet()) {
            if(getActivity() != null) {
                for (int i = 0; i < ((MainActivity) getActivity()).currencies.size(); i++) {
                    if (key.equalsIgnoreCase(((MainActivity) getActivity()).currencies.get(i).getSymbol())) {
                        value += (((MainActivity) getActivity()).currencies.get(i).getLast() * ((MainActivity) getActivity()).BTC_USD * portfolio.get(key));
                        sumPrevDay += (((MainActivity) getActivity()).currencies.get(i).getPrevDay() * ((MainActivity) getActivity()).BTC_USD *portfolio.get(key));
                        break;
                    }
                }
            }
        }
        percentageChange = ((value /sumPrevDay)-1);
        if(value < 1){
            portfolioValue.setText(priceFormatterLessThanZero.format(value));
        } else {
            portfolioValue.setText(defaultformatter.format(value));
        }
        portfolioDollarChange.setText((value -sumPrevDay) > 1 ? defaultformatter.format((value -sumPrevDay)) : priceFormatterLessThanZero.format((value -sumPrevDay)));
        portfolioDollarChange.setTextColor((value -sumPrevDay) < 0 ? getResources().getColor(R.color.negative_red, null) : getResources().getColor(R.color.positive, null));
        portfolioPercentageChange.setText(Math.abs(percentageChange) > 1 ? percentFormatter.format(percentageChange) : percentFormatterLessThanZero.format(percentageChange));
        portfolioPercentageChange.setTextColor((value -sumPrevDay) < 0 ? getResources().getColor(R.color.negative_red, null) : getResources().getColor(R.color.positive, null));
    }

    public void calculatePortfolioBreakdown() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for(String key : portfolio.keySet()) {
            if(getActivity() != null) {

                for (int i = 0; i < ((MainActivity) getActivity()).currencies.size(); i++) {
                    double currencyValue = 0.0;
                    if (key.equalsIgnoreCase(((MainActivity) getActivity()).currencies.get(i).getSymbol())) {
                        currencyValue = (((MainActivity) getActivity()).currencies.get(i).getLast() * ((MainActivity) getActivity()).BTC_USD * portfolio.get(key));
                        entries.add(new PieEntry((float)((currencyValue/value)*100), key));
                        break;
                    }
                }
            }
        }
        PieDataSet dataSet = new PieDataSet(entries,"");
        ArrayList<Integer> colors = new ArrayList<>();
        for(int c : ColorTemplate.COLORFUL_COLORS) {
            colors.add(c);
        }
        dataSet.setColors(colors);
        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setDrawValues(false);
        data.setValueFormatter(new PercentFormatter());
        pieChart.setData(data);
        pieChart.highlightValue(null);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.setHoleColor(getResources().getColor(R.color.card_color, null));
        pieChart.setRotationEnabled(false);
        pieChart.setDrawHoleEnabled(false);
        Legend l = pieChart.getLegend();
        l.setTextColor(getResources().getColor(R.color.white, null));
        pieChart.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(currentUser != null) {
            if(getActivity() != null) {
                ((MainActivity) getActivity()).favoritesChecked = false;
            }
            initializeFirebaseDB();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    }catch (Exception e){
                        Thread.interrupted();
                    }
                }
            }).start();
        }
    }

    private void initializeFirebaseDB() {
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid()).child("portfolio");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    if(snapshot.getValue() != null) {
                        try {
                            if(dataSnapshot.getValue() != null) {
                                portfolio.put(dataSnapshot.getKey(), Double.valueOf(dataSnapshot.getValue().toString()));
                            }
                        } catch (Exception e){
                            if(getActivity() != null) {
                                FirebaseAnalytics.getInstance(getActivity()).logEvent("Error_converting_to_double", new Bundle());
                            }
                        }
                    }
                }
                notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                try {
                    if(dataSnapshot.getValue() != null) {
                        portfolio.put(dataSnapshot.getKey(), Double.valueOf(dataSnapshot.getValue().toString()));
                    }
                    notifyDataSetChanged();
                } catch (Exception e){
                    if(getActivity() != null) {
                        FirebaseAnalytics.getInstance(getActivity()).logEvent("Error_converting_to_double", new Bundle());
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getValue() != null && dataSnapshot.getKey() != null) {
                    try {
                        if(dataSnapshot.getValue() != null) {
                            portfolio.put(dataSnapshot.getKey(), Double.valueOf(dataSnapshot.getValue().toString()));
                        }
                        notifyDataSetChanged();
                    } catch (Exception e){
                        if(getActivity() != null) {
                            FirebaseAnalytics.getInstance(getActivity()).logEvent("Error_converting_to_double", new Bundle());
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                portfolio.remove(dataSnapshot.getKey());
                notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
