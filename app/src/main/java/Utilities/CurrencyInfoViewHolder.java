package Utilities;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cryptoinc.cryptofeed.R;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Locale;

public class CurrencyInfoViewHolder extends RecyclerView.ViewHolder{
    TextView currencySymbol;
    TextView currencyName;
    TextView currencyPrice;
    TextView currencyPercentageChange;
    ImageView currencyLogo;
    ImageView shareImage;
    ImageView favoriteImage;
    View v;
    FirebaseStorage firebaseStorage;

    private DecimalFormat defaultformatter = new DecimalFormat("$ #,###.00");
    private DecimalFormat lessThanOne = new DecimalFormat("$ 0.000");


    CurrencyInfoViewHolder(View v) {
        super(v);
        currencySymbol = v.findViewById(R.id.currency_symbol);
        currencyName = v.findViewById(R.id.currency_name);
        currencyPrice = v.findViewById(R.id.currencyPrice);
        currencyPercentageChange = v.findViewById(R.id.currencyPercentageChange);
        currencyLogo = v.findViewById(R.id.currency_logo);
        shareImage = v.findViewById(R.id.share);
        favoriteImage = v.findViewById(R.id.favorite);

        this.v = v;
        firebaseStorage = FirebaseStorage.getInstance();
    }


    public void setViews(final CurrencyInfo info, HashSet<String> favorites){
        currencySymbol.setText(info.getSymbol());
        currencyName.setText(info.getName());
        if(favorites.contains(info.getSymbol())){
            favoriteImage.setColorFilter(getV().getResources().getColor(R.color.negative_red, null));
        } else {
            favoriteImage.setColorFilter(getV().getResources().getColor(R.color.white, null));
        }

        shareImage.setOnClickListener(v -> {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/html");
            String body = info.getName() + "'s current price is: " + defaultformatter.format(info.getLast()) + ".\n"
                    + "Download CryptoFeels for real-time prices and sentiment analysis of 100+ cryptocurrencies.";
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, info.getName() + " is on the move!");
            sharingIntent.putExtra(Intent.EXTRA_TEXT, body);
            v.getContext().startActivity(Intent.createChooser(sharingIntent, "Share"));
        });
        setPercentageChangeTextColor(info);
        handleBitcoinSpecialCase(info);
        setImageView(info);
    }

    private void setImageView(CurrencyInfo info) {
        StorageReference reference = firebaseStorage.getReference("icons/" + info.getSymbol() + ".png");
        Glide.with(v.getContext())
                .using(new FirebaseImageLoader()).load(reference).into(currencyLogo);
    }

    private void handleBitcoinSpecialCase(CurrencyInfo info) {
        if(info.getSymbol().equalsIgnoreCase("BTC")){
            currencyPrice.setText(defaultformatter.format(info.getLast()));
        }else {
            if (info.getLast() < 1) {
                currencyPrice.setText(lessThanOne.format(info.getLast()));
            } else {
                currencyPrice.setText(defaultformatter.format(info.getLast()));
            }
        }
    }

    private void setPercentageChangeTextColor(CurrencyInfo info) {
        if(info.getPercentageChange() < 0){
            currencyPercentageChange.setText(String.format(Locale.US,"%.2f%%", info.getPercentageChange()));
            currencyPercentageChange.setTextColor(Color.parseColor("#de6b77"));
        }else{
            currencyPercentageChange.setText(String.format(Locale.US,"+%.2f%%", info.getPercentageChange()));
            currencyPercentageChange.setTextColor(Color.parseColor("#1be2b4"));
        }
    }

    public View getV() {
        return v;
    }
}
