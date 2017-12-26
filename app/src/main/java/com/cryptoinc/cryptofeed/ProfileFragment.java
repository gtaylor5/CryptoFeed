package com.cryptoinc.cryptofeed;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    public View view;
    public LayoutInflater inflater;
    public ViewGroup container;
    public FirebaseUser user;

    Button donate;
    Button logout;
    Button removeAds;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.inflater = inflater;
        this.container = container;
        if(view == null){
            if(user == null) {
                setViewsNoUser(inflater, container);
            } else {
                setViewsUser(inflater, container);
            }
        }
        return view;
    }

    // If user is logged in set the view to this.

    public void setViewsUser(LayoutInflater inflater, ViewGroup container) {
        view = inflater.inflate(R.layout.profile_view, container, false);
        showUserLoggedInView();
    }

    public void showUserLoggedInView() {
        donate = view.findViewById(R.id.donate);
        logout = view.findViewById(R.id.logout);
        removeAds = view.findViewById(R.id.removeAds);
        donate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final PopupWindow popupWindow;
                View popUp = inflater.inflate(R.layout.donate_alert, container, false);
                final TextView eth = popUp.findViewById(R.id.ETHaddress);
                final TextView btc = popUp.findViewById(R.id.BTCAddress);

                eth.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {

                        ClipboardManager manager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        if(manager != null) {
                            manager.setPrimaryClip(ClipData.newPlainText("ETH TEXT", eth.getText().toString()));
                            Toast.makeText(getContext(), "ETH Addresss Added to Clipboard", Toast.LENGTH_LONG).show();
                            return true;
                        }
                        return false;

                    }
                });

                btc.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {

                        ClipboardManager manager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        if(manager != null) {
                            manager.setPrimaryClip(ClipData.newPlainText("BTC TEXT", btc.getText().toString()));
                            Toast.makeText(getContext(), "BTC Addresss Added to Clipboard", Toast.LENGTH_LONG).show();
                            return true;
                        }
                        return false;

                    }
                });

                popupWindow = new PopupWindow(popUp, container.getWidth(),container.getHeight(), true);
                popupWindow.showAtLocation(popUp, Gravity.CENTER_VERTICAL, 0, 0);
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((MainActivity)getActivity()).currentUser != null){
                    ((MainActivity)getActivity()).favoritesRef = null;
                    ((MainActivity)getActivity()).currentUser = null;
                    ((MainActivity)getActivity()).favorites.clear();
                    FirebaseAuth.getInstance().signOut();
                    ((MainActivity)getActivity()).clearBackStackAfterLogout();
                }
            }
        });

        removeAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
                dialog.setTitle("Remove Ads");
                dialog.setMessage("Would you like to watch a short video to remove ads for 24 hours?");
                dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No, Thanks.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialog.dismiss();
                    }
                });

                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(((MainActivity)getActivity()).mRewardVideoAd.isLoaded()) {
                            ((MainActivity)getActivity()).mRewardVideoAd.show();
                        }
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });
    }

    // If user is NOT logged in set view to this

    public void setViewsNoUser(LayoutInflater inflater, ViewGroup container) {
        view = inflater.inflate(R.layout.profile_view_no_user, container, false);
        final EditText email = view.findViewById(R.id.email_prof);
        final EditText password = view.findViewById(R.id.password_prof);
        final Button login = view.findViewById(R.id.signup_prof);
        Button donate = view.findViewById(R.id.donate);
        final TextView switchview = view.findViewById(R.id.switchview);
        final TextView heading = view.findViewById(R.id.alertHeading);
        switchview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if(switchview.getText().toString().contains("Sign In")){
                    switchview.setText(R.string.newuser);
                    heading.setText(R.string.login);
                } else {
                    switchview.setText(R.string.already);
                    heading.setText(R.string.sign_up);
                }
            }
        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromInputMethod(v.getWindowToken(), 0);
                }
                authenticateNewUser(email, password);
            }
        });
        donate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Donate to any of the Following Addresses:")
                        .setMessage("ETH:\n\n0xF8D182dA0aD3365715D6D8FF94fFD70C59543547\n\n"+
                                "BTC:\n\n1CKwRMbrp8dthrRkSepmqHRDULC8yPwQ3y")
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
            }
        });
    }

    //User Authentication

    public void authenticateNewUser(final EditText email, final EditText password) {
        if(email.getText().toString().contains("@") && password.getText().toString().length() != 0){

            //Try to sign in user first

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(FirebaseAuth.getInstance().getCurrentUser() != null){
                        Toast.makeText(getActivity(), "Sign In Successful!", Toast.LENGTH_LONG).show();
                        view = inflater.inflate(R.layout.profile_view, container, false);
                        setCurrentUser();
                        ((MainActivity)getActivity()).bottomNavigation.setCurrentItem(1, false);
                        getActivity().getSupportFragmentManager().popBackStackImmediate();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    //if Sign in fails, create new user.

                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(FirebaseAuth.getInstance().getCurrentUser() != null){
                                Toast.makeText(getActivity(), "Sign Up Successful!", Toast.LENGTH_LONG).show();
                                view = inflater.inflate(R.layout.profile_view, container, false);
                                setCurrentUser();
                                ((MainActivity)getActivity()).bottomNavigation.setCurrentItem(1, false);
                                getActivity().getSupportFragmentManager().popBackStackImmediate();
                            }
                        }
                    });
                }
            });
        } else if (!email.getText().toString().contains("@")){
            Toast.makeText(getActivity(), "Invalid Email Address. Please try again.", Toast.LENGTH_LONG).show();
        } else if (password.getText().toString().length() == 0){
            Toast.makeText(getActivity(), "You must enter a pasaword.", Toast.LENGTH_LONG).show();
        }
    }

    private void setCurrentUser() {
        ((MainActivity)getActivity()).currentUser = FirebaseAuth.getInstance().getCurrentUser();
        ((MainActivity)getActivity()).initializeFirebaseDB();
        ((MainActivity)getActivity()).getFavorites();
    }

    //Life cycle methods.

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
