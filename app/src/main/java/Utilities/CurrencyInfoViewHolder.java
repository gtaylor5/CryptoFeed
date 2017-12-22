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
import java.util.concurrent.ExecutionException;

/**
 * Created by Gerard on 11/10/2017.
 */

public class CurrencyInfoViewHolder extends RecyclerView.ViewHolder{
    TextView currencyName;
    TextView currencySymbol;
    TextView currencyPrice;
    TextView currencyPercentageChange;
    ImageView currencyLogo;
    ImageView shareImage;
    ImageView favoriteImage;
    View v;
    FirebaseStorage firebaseStorage;

    private DecimalFormat defaultformatter = new DecimalFormat("$ #,###.00");
    private DecimalFormat lessThanOne = new DecimalFormat("$ 0.000");

    CurrencyInfoViewHolder(View itemView) {
        super(itemView);
        currencyName = itemView.findViewById(R.id.curreny_name);
        currencySymbol = itemView.findViewById(R.id.currency_symbol);
        currencyPrice = itemView.findViewById(R.id.currencyPrice);
        currencyPercentageChange = itemView.findViewById(R.id.currencyPercentageChange);
        currencyLogo = itemView.findViewById(R.id.currency_logo);
        shareImage = itemView.findViewById(R.id.share);
        favoriteImage = itemView.findViewById(R.id.favorite);

        v = itemView;
        firebaseStorage = FirebaseStorage.getInstance();
    }

    public void setViews(final CurrencyInfo info, HashSet<String> favorites){
        currencyName.setText(info.getName());
        currencySymbol.setText(info.getSymbol());
        if(favorites.contains(info.getSymbol())){
            favoriteImage.setColorFilter(getV().getResources().getColor(R.color.negative_red, null));
        } else {
            favoriteImage.setColorFilter(getV().getResources().getColor(R.color.white, null));
        }

        shareImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/html");
                String body = info.getName() + "'s current price is: " + defaultformatter.format(info.getLast() * info.getBTC_USD()) + ".\n"
                        + "Download CryptoFeels for real-time prices and sentiment analysis of 100+ cryptocurrencies.";
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, info.getName() + " is on the move!");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, body);
                v.getContext().startActivity(Intent.createChooser(sharingIntent, "Share"));
            }
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
            currencyPrice.setText(defaultformatter.format(info.getBTC_USD()));
        }else {
            if (info.getLast() < 1) {
                currencyPrice.setText(lessThanOne.format(info.getLast() * info.getBTC_USD()));
            } else {
                currencyPrice.setText(defaultformatter.format(info.getLast() * info.getBTC_USD()));
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
