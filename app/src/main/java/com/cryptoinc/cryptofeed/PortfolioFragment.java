package com.cryptoinc.cryptofeed;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.google.android.gms.plus.PlusOneButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import Utilities.CurrencyInfoAdapter;
import Utilities.CurrencyInfoViewHolder;

public class PortfolioFragment extends Fragment {

    FloatingActionButton addToPortfolio;
    TextView portfolioValue;
    TextView portfolioDollarChange;
    TextView portfolioPercentageChange;
    RecyclerView portfolioList;
    CurrencyInfoAdapter adapter;
    PieChart pieChart;
    LinearLayoutManager manager;

    FirebaseUser currentUser;
    DatabaseReference databaseReference;

    HashMap<String, Double> portfolio = new HashMap<>(); // (BTC, 23.0000)

    private DecimalFormat defaultformatter = new DecimalFormat("$ #,###.00");
    private DecimalFormat percentFormatter = new DecimalFormat("#,###.00 %");

    public PortfolioFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = ((MainActivity)getActivity()).currentUser;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portfolio, container, false);
        addToPortfolio = view.findViewById(R.id.addToPortfolio);
        portfolioValue = view.findViewById(R.id.portfolio_value);
        portfolioDollarChange = view.findViewById(R.id.portfolioDollarChange);
        portfolioPercentageChange = view.findViewById(R.id.portfolioPercentageChange);

        pieChart = view.findViewById(R.id.portfolioChart);

        portfolioList = view.findViewById(R.id.portfolioList);
        adapter = new CurrencyInfoAdapter(inflater, ((MainActivity)getActivity()).currencies, ((MainActivity)getActivity()).favorites, getActivity());
        manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayout.VERTICAL);
        portfolioList.setLayoutManager(manager);

        if(getActivity() != null) {
            currentUser = ((MainActivity) getActivity()).currentUser;
        }

        addToPortfolio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                
            }
        });

        return view;
    }

    public void notifyDataSetChanged() {
        calculatePortfolioValue();
        adapter.notifyDataSetChanged();
    }

    private void calculatePortfolioValue() {
        double value = 0.0;
        double percentageChange = 0.0;
        double sumPrevDay = 0;
        for(String key : portfolio.keySet()) {
            for(int i = 0; i < ((MainActivity)getActivity()).currencies.size(); i++) {
                if (key.equalsIgnoreCase(((MainActivity) getActivity()).currencies.get(i).getSymbol())) {
                    value += (((MainActivity) getActivity()).currencies.get(i).getLast() * ((MainActivity) getActivity()).currencies.get(i).getBTC_USD());
                    sumPrevDay += (((MainActivity) getActivity()).currencies.get(i).getPrevDay() * ((MainActivity) getActivity()).currencies.get(i).getBTC_USD());
                    break;
                }
            }
        }
        percentageChange = ((value/sumPrevDay)-1)*100;
        portfolioValue.setText(defaultformatter.format(value));
        portfolioDollarChange.setText(defaultformatter.format((value-sumPrevDay)));
        portfolioPercentageChange.setText(percentFormatter.format(percentageChange));
    }

    @Override
    public void onResume() {
        super.onResume();
        if(currentUser != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid()).child("portfolio");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        if(snapshot.getValue() != null) {
                            portfolio.put(snapshot.getKey(), Double.parseDouble(snapshot.getValue().toString()));
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            databaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    portfolio.put(dataSnapshot.getKey(), (Double)dataSnapshot.getValue());
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    if(dataSnapshot.getValue() != null && dataSnapshot.getKey() != null) {
                        portfolio.put(dataSnapshot.getKey(), (Double)dataSnapshot.getValue());
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    portfolio.remove(dataSnapshot.getKey());
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
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
