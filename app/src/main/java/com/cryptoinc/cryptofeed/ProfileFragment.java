package com.cryptoinc.cryptofeed;


import android.app.AlertDialog;
import android.app.Dialog;
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

    private View view;
    private LayoutInflater inflater;
    private ViewGroup container;
    private FirebaseUser user;

    Button suggestion;
    Button donate;
    Button logout;

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

    private void setViewsUser(LayoutInflater inflater, ViewGroup container) {
        view = inflater.inflate(R.layout.profile_view, container, false);
        showUserLoggedInView();
    }

    private void showUserLoggedInView() {
        suggestion = view.findViewById(R.id.suggestion);
        donate = view.findViewById(R.id.donate);
        logout = view.findViewById(R.id.logout);

        suggestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupWindow popupWindow;
                View popUp = inflater.inflate(R.layout.feedback_alert, container, false);
                Button submit = popUp.findViewById(R.id.send);
                final EditText editText = popUp.findViewById(R.id.feedbackBox);
                submit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent email = new Intent(Intent.ACTION_SENDTO);
                        email.setData(Uri.parse("mailto:"));
                        email.putExtra(Intent.EXTRA_EMAIL, "taylor.gerard13@gmail.com");
                        email.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
                        email.putExtra(Intent.EXTRA_TEXT, editText.getText().toString());
                        email.setType("message,rfc822");
                        v.getContext().startActivity(Intent.createChooser(email, "Choose an Email Client"));
                    }
                });
                popupWindow = new PopupWindow(popUp, getActivity().getWindow().getAttributes().width, getActivity().getWindow().getAttributes().height, true);
                popupWindow.showAtLocation(popUp, Gravity.CENTER_VERTICAL, 0, 0);
            }
        });

        donate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Donate to any of the Following Addresses:")
                        .setMessage("ET:\n\n0xF8D182dA0aD3365715D6D8FF94fFD70C59543547\n\n"+
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

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(user != null){
                    FirebaseAuth.getInstance().signOut();
                    ((MainActivity)getActivity()).clearBackStackAfterLogout();
                }
            }
        });
    }

    // If user is NOT logged in set view to this

    private void setViewsNoUser(LayoutInflater inflater, ViewGroup container) {
        view = inflater.inflate(R.layout.profile_view_no_user, container, false);
        final EditText email = view.findViewById(R.id.email_prof);
        final EditText password = view.findViewById(R.id.password_prof);
        final Button login = view.findViewById(R.id.signup_prof);
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
                authenticateNewUser(email, password);
            }
        });
    }

    //User Authentication

    private void authenticateNewUser(final EditText email, final EditText password) {
        if(email.getText().toString().contains("@") && password.getText().toString().length() != 0){

            //Try to sign in user first

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(FirebaseAuth.getInstance().getCurrentUser() != null){
                        Toast.makeText(getActivity(), "Sign In Successful!", Toast.LENGTH_LONG).show();
                        view = inflater.inflate(R.layout.profile_view, container, false);
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
                                ((MainActivity)getActivity()).bottomNavigation.setCurrentItem(1, false);
                                getActivity().getSupportFragmentManager().popBackStackImmediate();
                            }
                        }
                    });
                }
            });
        }
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
