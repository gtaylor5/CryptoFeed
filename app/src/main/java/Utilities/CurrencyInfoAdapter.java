package Utilities;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.cryptoinc.cryptofeed.MainActivity;
import com.cryptoinc.cryptofeed.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
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

        popupWindow = new PopupWindow(popUp, container.getWidth(),container.getHeight(), true);
        popupWindow.showAtLocation(popUp, Gravity.CENTER_VERTICAL, 0, 0);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromInputMethod(v.getWindowToken(), 0);
                }
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
                        Toast.makeText(container.getContext(), "Sign In Successful!", Toast.LENGTH_LONG).show();
                        if(currentActivity.getClass().getSimpleName().equalsIgnoreCase("MainActivity")) {
                            MainActivity activity = (MainActivity) currentActivity;
                            activity.currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            activity.initializeFirebaseDB();
                        }
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
                                Toast.makeText(container.getContext(), "Sign Up Successful!", Toast.LENGTH_LONG).show();
                                if(currentActivity.getClass().getSimpleName().equalsIgnoreCase("MainActivity")) {
                                    MainActivity activity = (MainActivity) currentActivity;
                                    activity.currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                    activity.initializeFirebaseDB();
                                }
                                popupWindow.dismiss();
                            }
                        }
                    });
                }
            });
        } else if (!email.getText().toString().contains("@")){
            Toast.makeText(container.getContext(), "Invalid Email Address. Please try again.", Toast.LENGTH_LONG).show();
        } else if (password.getText().toString().length() == 0){
            Toast.makeText(container.getContext(), "You must enter a pasaword.", Toast.LENGTH_LONG).show();

        }
    }

    @Override
    public CurrencyInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.currencyinfo, parent, false);
        this.container = parent;
        return new CurrencyInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final CurrencyInfoViewHolder holder, int position) {
        final CurrencyInfo currencyInfo = currencies.get(position);
        holder.setViews(currencyInfo, favorites);
        holder.getV().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle params = new Bundle();
                params.putString("currency_name", currencyInfo.getName());
                mFirebaseAnalytics.logEvent("currency_clicked", params);
                currencyInfoListListener.currencySelected(currencyInfo);
            }
        });
        
        holder.favoriteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user == null){
                    showPopUpWindow(layoutInflater, container);
                } else {
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    final DatabaseReference reference = database.getReference("users").child(user.getUid()).child("favorites");
                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                                if(snapshot.getValue() != null) {
                                    if (((String) (snapshot.getValue())).equalsIgnoreCase(holder.currencySymbol.getText().toString())) {
                                        DatabaseReference ref = reference.child(snapshot.getKey());
                                        favorites.remove(holder.currencySymbol.getText().toString());
                                        ref.setValue(null);
                                        holder.favoriteImage.setColorFilter(holder.getV().getResources().getColor(R.color.white, null));
                                        //notifyDataSetChanged();
                                        return;
                                    }
                                }
                            }
                            reference.push().setValue(holder.currencySymbol.getText().toString());
                            holder.favoriteImage.setColorFilter(holder.getV().getResources().getColor(R.color.negative_red, null));
                            favorites.add(holder.currencySymbol.getText().toString());
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
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
