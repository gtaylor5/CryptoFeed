package Utilities;

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

/**
 * Created by Gerard on 11/10/2017.
 */

public class CurrencyInfoPortfolioViewHolder extends RecyclerView.ViewHolder{
    TextView currencyName;
    TextView currencyQuantity;
    TextView currencyValue;
    ImageView currencyLogo;
    View v;
    FirebaseStorage firebaseStorage;

    private DecimalFormat defaultformatter = new DecimalFormat("$ #,###.00");

    CurrencyInfoPortfolioViewHolder(View itemView) {
        super(itemView);
        currencyName = itemView.findViewById(R.id.currency_name);
        currencyQuantity = itemView.findViewById(R.id.currencyQuantity);
        currencyValue = itemView.findViewById(R.id.currencyValue);
        currencyLogo = itemView.findViewById(R.id.currency_logo);
        v = itemView;
        firebaseStorage = FirebaseStorage.getInstance();
    }

    public void setViews(final CurrencyInfo info, double quantity){
        currencyName.setText(info.getName());
        String quantityText = String.valueOf(quantity) + " " + info.getSymbol();
        currencyQuantity.setText(quantityText);
        if(info.getSymbol().equalsIgnoreCase("BTC")) {
            currencyValue.setText(defaultformatter.format(quantity * info.getBTC_USD()));
        } else {
            currencyValue.setText(defaultformatter.format(quantity * info.getBTC_USD() * info.getLast()));
        }
        setImageView(info);
    }

    private void setImageView(CurrencyInfo info) {
        StorageReference reference = firebaseStorage.getReference("icons/" + info.getSymbol() + ".png");
        Glide.with(v.getContext())
                .using(new FirebaseImageLoader()).load(reference).into(currencyLogo);
    }

    public View getV() {
        return v;
    }
}
