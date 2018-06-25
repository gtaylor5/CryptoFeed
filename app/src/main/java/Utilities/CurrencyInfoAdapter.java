package Utilities;


import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cryptoinc.cryptofeed.MainActivity;
import com.cryptoinc.cryptofeed.R;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Gerard on 11/10/2017.
 */

public class CurrencyInfoAdapter extends RecyclerView.Adapter<CurrencyInfoViewHolder> {

    public ArrayList<CurrencyInfo> currencies;
    public HashSet<String> favorites = new HashSet<>();
    private LayoutInflater layoutInflater;
    private ViewGroup container;
    private FirebaseAnalytics mFirebaseAnalytics;
    Activity currentActivity;

    public CurrencyInfoAdapter(LayoutInflater layoutInflater, ArrayList<CurrencyInfo> currencies, HashSet<String> favorites, Activity activity){
        this.layoutInflater = layoutInflater;
        this.currencies = currencies;
        this.favorites = favorites;
        this.currentActivity = activity;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(layoutInflater.getContext());
    }

    @Override
    public CurrencyInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = layoutInflater.inflate(R.layout.currencyinfo, parent, false);
        this.container = parent;
        return new CurrencyInfoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final CurrencyInfoViewHolder holder, int position) {
        final CurrencyInfo currencyInfo = currencies.get(position);
        holder.setViews(currencyInfo, favorites);
        holder.getV().setOnClickListener(v -> {
            Bundle params = new Bundle();
            params.putString("currency_name", currencyInfo.getName());
            mFirebaseAnalytics.logEvent("currency_clicked", params);
            currencyInfoListListener.currencySelected(currencyInfo);
        });
        
        holder.favoriteImage.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user == null){
                MainActivity activity = (MainActivity) currentActivity;
                activity.showPopUpWindow(1, "Sign Up To Add Favorites",
                        "Login To Add Favorites",
                        "Sign Up", "Login");
            } else {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                final DatabaseReference reference = database.getReference("users").child(user.getUid()).child("favorites");
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                            if(snapshot.getValue() != null) {
                                if (((String) (snapshot.getValue())).equalsIgnoreCase(currencyInfo.getSymbol())) {
                                    DatabaseReference ref = reference.child(snapshot.getKey());
                                    favorites.remove(currencyInfo.getSymbol());
                                    ref.setValue(null);
                                    holder.favoriteImage.setColorFilter(holder.getV().getResources().getColor(R.color.white, null));
                                    return;
                                }
                            }
                        }
                        reference.push().setValue(currencyInfo.getSymbol());
                        holder.favoriteImage.setColorFilter(holder.getV().getResources().getColor(R.color.negative_red, null));
                        favorites.add(currencyInfo.getSymbol());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
        
    }

    @Override
    public int getItemCount() {
        return this.currencies.size();
    }

    public CurrencyInfoListListener currencyInfoListListener;

    public void setCurrencyInfoListListener(CurrencyInfoListListener currencyInfoListListener){
        this.currencyInfoListListener = currencyInfoListListener;
    }

    public void updateList(List<CurrencyInfo> temp) {
        currencies = (ArrayList<CurrencyInfo>)temp;
        notifyDataSetChanged();
    }

    public interface CurrencyInfoListListener {
        void currencySelected(CurrencyInfo info);
    }

}
