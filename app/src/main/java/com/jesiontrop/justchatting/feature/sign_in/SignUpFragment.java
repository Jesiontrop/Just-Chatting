package com.jesiontrop.justchatting.feature.sign_in;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.jesiontrop.justchatting.R;
import com.jesiontrop.justchatting.app.ChatActivity;

public class SignUpFragment extends Fragment {
    private static final String TAG = "SignUpFragment";

    private EditText mUsernameEditText;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mConfirmPasswordEditText;
    private Button mSignInButton;
    private Button mSignUpButton;

    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mAuth;

    private Callbacks mCallbacks;

    public static SignUpFragment newInstance() {
        return new SignUpFragment();
    }

    public interface Callbacks {
        void onSignInSelected();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle saveInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sign_up, container,
                false);

        mUsernameEditText = (EditText) v.findViewById(R.id.username_edit_text);
        mEmailEditText = (EditText) v.findViewById(R.id.email_edit_text);
        mPasswordEditText = (EditText) v.findViewById(R.id.password_edit_text);
        mConfirmPasswordEditText = (EditText) v.findViewById(R.id.confirm_password_edit_text);

        mSignInButton = (Button) v.findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.onSignInSelected();
            }
        });

        mSignUpButton = (Button) v.findViewById(R.id.sign_up_button);
        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });
        return v;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    private void signUp() {
        String email = mEmailEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String confirmPassword = mConfirmPasswordEditText.getText().toString();

        if ((!email.equals("") && !password.equals("")) && password.equals(confirmPassword)){
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>(){
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                mFirebaseUser = mAuth.getCurrentUser();
                                updateProfile();
                            } else {
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(getActivity(), "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else if (! password.equals(confirmPassword)){
            Toast.makeText(getActivity(), "Password mismatch", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Fields are empty", Toast.LENGTH_SHORT).show();
        }

    }

    private void updateProfile() {
        String name = mUsernameEditText.getText().toString();
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        mFirebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
               if (task.isSuccessful()) {
                   Log.d(TAG, "Username: " + mFirebaseUser.getDisplayName());
                   quit();
               } else {
                   Log.i(TAG, "Can't set username");
                   Toast.makeText(getActivity(), "Can't set username", Toast.LENGTH_SHORT)
                           .show();
               }
            }
        });

    }

    private void quit() {
        startActivity(new Intent(getActivity(),
                ChatActivity.class));
        getActivity().finish();
    }
}
