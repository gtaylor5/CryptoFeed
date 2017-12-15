package Utilities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cryptoinc.cryptofeed.R;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Locale;

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

    public CurrencyInfoViewHolder(View itemView) {
        super(itemView);
        currencyName = (TextView) itemView.findViewById(R.id.curreny_name);
        currencySymbol = (TextView) itemView.findViewById(R.id.currency_symbol);
        currencyPrice = (TextView) itemView.findViewById(R.id.currencyPrice);
        currencyPercentageChange = (TextView)itemView.findViewById(R.id.currencyPercentageChange);
        currencyLogo = (ImageView) itemView.findViewById(R.id.currency_logo);
        shareImage = (ImageView)itemView.findViewById(R.id.share);
        favoriteImage = (ImageView)itemView.findViewById(R.id.favorite);

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
                sharingIntent.setType("text/plain");
                String body = info.getName() + "'s current price is: " + defaultformatter.format(info.getLast() * info.getBTC_USD()) + ".\n"
                        + "Download CryptoFeels for real-time prices and sentiment analysis of 100+ cryptocurrencies.";
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, info.getName() + " is on the move!");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, body);
                v.getContext().startActivity(Intent.createChooser(sharingIntent, "Share Via"));
            }
        });
        setPercentageChangeTextColor(info);
        handleBitcoinSpecialCase(info);
        setImageView(info);
    }

    void authenticateNewUser(final Dialog alertDialog, final EditText email, final EditText password) {
        if(email.getText().toString().contains("@") && password.getText().toString().length() != 0){
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(FirebaseAuth.getInstance().getCurrentUser() != null){
                        Toast.makeText(getV().getContext(), "Sign In Successful!", Toast.LENGTH_LONG).show();
                        favoriteImage.setColorFilter(getV().getResources().getColor(R.color.negative_red, null));
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        final DatabaseReference reference = database.getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("favorites");
                        reference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                boolean inFaves = false;
                                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                                    if(snapshot.getValue().toString().equalsIgnoreCase(currencySymbol.getText().toString())){
                                        inFaves = true;
                                        break;
                                    }
                                }
                                if(!inFaves) {
                                    reference.push().setValue(currencySymbol.getText().toString());
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                        alertDialog.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(FirebaseAuth.getInstance().getCurrentUser() != null){
                                Toast.makeText(getV().getContext(), "Sign Up Successful!", Toast.LENGTH_LONG).show();
                                favoriteImage.setColorFilter(getV().getResources().getColor(R.color.negative_red, null));
                                FirebaseDatabase database = FirebaseDatabase.getInstance();
                                DatabaseReference reference = database.getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("favorites");
                                reference.push().setValue(currencySymbol.getText().toString());
                                alertDialog.dismiss();
                            }
                        }
                    });
                }
            });
        }
    }


    private void setImageView(CurrencyInfo info) {
        StorageReference reference;
        if(info.getSymbol().equalsIgnoreCase("BCC")){
            info.setSymbol("BCH");
            currencySymbol.setText("BCH");
            reference = firebaseStorage.getReference("icons/BCH.png");
        }else {
           reference = firebaseStorage.getReference("icons/" + info.getSymbol() + ".png");
        }
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

    public void setV(View v) {
        this.v = v;
    }
}
