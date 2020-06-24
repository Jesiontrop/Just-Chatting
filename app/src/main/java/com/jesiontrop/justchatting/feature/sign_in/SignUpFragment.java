package com.jesiontrop.justchatting.feature.sign_in;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jesiontrop.justchatting.R;
import com.jesiontrop.justchatting.app.MainActivity;

import java.io.File;


public class SignUpFragment extends Fragment
        implements GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = "SignUpFragment";
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int RC_SIGN_IN = 9001;

    private EditText mUsernameEditText;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mConfirmPasswordEditText;
    private ImageView mPhotoImageView;
    private Button mSignInButton;
    private Button mSignUpButton;
    private SignInButton mGoogleSignInButton;

    private GoogleApiClient mGoogleApiClient;

    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mAuth;

    private Uri mSelectedUri;

    private FirebaseAuth mFireBaseAuth;
    private Callbacks mCallbacks;
    private NavController navController;


    public interface Callbacks {
        void onSignInSelected();
    }

    public SignUpFragment() {}

    public static SignUpFragment newInstance() {
        return new SignUpFragment();
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

        mPhotoImageView = (ImageView) v.findViewById(R.id.photo_image_view);
        mPhotoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPhoto();
            }
        });

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

        mGoogleSignInButton = (SignInButton) v.findViewById(R.id.google_sign_in_button);

        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });

        navController = Navigation.findNavController(getActivity(),
                R.id.nav_host_fragment);

        return v;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        File compressedImageFile;

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK
                && null != data) {
            Uri uri = data.getData();
            mSelectedUri = uri;
            Log.d(TAG, "PhotoUri: " + mSelectedUri );
            mPhotoImageView.setImageURI(mSelectedUri);
        } else if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                Log.e(TAG, "Google Sign In failed.");
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(getActivity(), "Google Play Services error.", Toast.LENGTH_SHORT)
                .show();
    }

    private void pickPhoto() {
        Intent intent;
        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/*");

        startActivityForResult(Intent.createChooser(intent, "Choose avatar"), PICK_IMAGE_REQUEST);
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
                                setSettingProfile();
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

    private void setSettingProfile() {
        String name = mUsernameEditText.getText().toString();

        final UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest
                .Builder()
                .setDisplayName(name);

        if (mSelectedUri != null){

            StorageReference databaseReference = FirebaseStorage.getInstance().getReference()
                    .child("users")
                    .child(mFirebaseUser.getUid())
                    .child("photoUrl");

            UploadTask uploadTask = databaseReference.putFile(mSelectedUri);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d(TAG, "User PhotoUrl can't uploaded");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> uri = taskSnapshot.getStorage().getDownloadUrl();
                    while(!uri.isComplete());
                    Uri url = uri.getResult();
                    profileUpdatesBuilder
                            .setPhotoUri(url);
                    UserProfileChangeRequest profileChangeRequest = profileUpdatesBuilder.build();
                    updateProfile(profileChangeRequest);
                    Log.d(TAG, "User PhotoUrl is uploaded: " + url.toString());
                }
            });
        } else {
            UserProfileChangeRequest profileChangeRequest = profileUpdatesBuilder.build();

            updateProfile(profileChangeRequest);
        }

    }

    private void updateProfile(UserProfileChangeRequest profileChangeRequest) {
        mFirebaseUser.updateProfile(profileChangeRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Username: " + mFirebaseUser.getDisplayName());
                            quit();
                        } else {
                            Log.i(TAG, "Can't set username");
                            Toast.makeText(getActivity(), "Can't set username",
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
    }

    private void googleSignIn() {

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.IdToken))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(getActivity() , this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogle-" + account.getId());
        AuthCredential credential = GoogleAuthProvider
                .getCredential(account.getIdToken(), null);
        mFireBaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential-onComplete-" + task.isSuccessful());

                        if (task.isSuccessful()) {
                            quit();
                        } else {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(getActivity(),
                                    "Authentication failed", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    private void quit() {
        navController.navigate(R.id.action_global_chatActivity);
        getActivity().finish();
    }
}
