package Utilities;

import android.app.Dialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.cryptoinc.cryptofeed.R;
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

    private ArrayList<CurrencyInfo> currencies;
    public HashSet<String> favorites = new HashSet<>();
    private LayoutInflater layoutInflater;
    public CurrencyInfoAdapter(LayoutInflater layoutInflater, ArrayList<CurrencyInfo> currencies){
        this.layoutInflater = layoutInflater;
        this.currencies = currencies;
    }

    @Override
    public CurrencyInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.currencyinfo, parent, false);
        return new CurrencyInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final CurrencyInfoViewHolder holder, int position) {
        final CurrencyInfo currencyInfo = currencies.get(position);
        holder.setViews(currencyInfo, favorites);
        holder.getV().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currencyInfoListListener.currencySelected(currencyInfo);
            }
        });
        
        holder.favoriteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user == null){
                    final Dialog alertDialog = new Dialog(v.getContext());
                    alertDialog.setContentView(R.layout.sign_up_alert);
                    final EditText email = (EditText) alertDialog.findViewById(R.id.email);
                    final EditText password = (EditText)alertDialog.findViewById(R.id.password);
                    final Button login = (Button)alertDialog.findViewById(R.id.signUp);
                    login.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            holder.authenticateNewUser(alertDialog,email, password);
                        }
                    });
                    alertDialog.show();
                } else {
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    final DatabaseReference reference = database.getReference("users").child(user.getUid()).child("favorites");
                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                                if(((String)(snapshot.getValue())).equalsIgnoreCase(holder.currencySymbol.getText().toString())){
                                    DatabaseReference ref = reference.child(snapshot.getKey());
                                    favorites.remove(holder.currencySymbol.getText().toString());
                                    ref.setValue(null);
                                    holder.favoriteImage.setColorFilter(holder.getV().getResources().getColor(R.color.white, null));
                                    notifyDataSetChanged();
                                    return;
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

    private CurrencyInfoListListener currencyInfoListListener;

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
