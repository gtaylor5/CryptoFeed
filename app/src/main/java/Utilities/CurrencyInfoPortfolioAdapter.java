package Utilities;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.cryptoinc.cryptofeed.R;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Gerard on 11/10/2017.
 */

public class CurrencyInfoPortfolioAdapter extends RecyclerView.Adapter<CurrencyInfoPortfolioViewHolder> {

    public ArrayList<CurrencyInfo> currencies = new ArrayList<>();
    public HashMap<String, Double> portfolio;
    private LayoutInflater layoutInflater;
    private FirebaseAnalytics mFirebaseAnalytics;

    public CurrencyInfoPortfolioAdapter(LayoutInflater layoutInflater, HashMap<String, Double> portfolio){
        this.layoutInflater = layoutInflater;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(layoutInflater.getContext());
        this.portfolio = portfolio;
    }

    @Override
    public CurrencyInfoPortfolioViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.currencyinfo_portfolio, parent, false);
        return new CurrencyInfoPortfolioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final CurrencyInfoPortfolioViewHolder holder, int position) {
        final CurrencyInfo currencyInfo = currencies.get(position);
        holder.setViews(currencyInfo, portfolio.get(currencyInfo.getSymbol()));
        holder.getV().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle params = new Bundle();
                params.putString("currency_name", currencyInfo.getName());
                mFirebaseAnalytics.logEvent("currency_clicked", params);
                new MaterialDialog.Builder(layoutInflater.getContext())
                        .backgroundColor(layoutInflater.getContext().getResources().getColor(R.color.background, null))
                        .title("Set Quantity")
                        .titleColor(layoutInflater.getContext().getResources().getColor(R.color.white, null))
                        .content(currencyInfo.getSymbol())
                        .contentColor(layoutInflater.getContext().getResources().getColor(R.color.white, null))
                        .inputType(InputType.TYPE_NUMBER_FLAG_DECIMAL)
                        .input("You have: "+holder.currencyQuantity.getText(), null, false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                try {
                                    FirebaseDatabase.getInstance().getReference().child("users")
                                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                            .child("portfolio")
                                            .child(currencyInfo.getSymbol()).setValue(Double.parseDouble(input.toString()));
                                } catch (Exception e){
                                    Toast.makeText(layoutInflater.getContext(),"Error editing your portfolio. Please try again.", Toast.LENGTH_LONG).show();
                                }
                                dialog.dismiss();
                            }
                        }).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.currencies.size();
    }

    public void updateList(List<CurrencyInfo> temp) {
        currencies = (ArrayList<CurrencyInfo>)temp;
        notifyDataSetChanged();
    }

}
