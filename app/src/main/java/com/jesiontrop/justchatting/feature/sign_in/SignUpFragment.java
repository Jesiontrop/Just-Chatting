package com.jesiontrop.justchatting.feature.sign_in;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jesiontrop.justchatting.R;
import com.jesiontrop.justchatting.app.ChatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;


public class SignUpFragment extends Fragment {
    private static final String TAG = "SignUpFragment";
    private static final int PICK_IMAGE_REQUEST = 100;

    private EditText mUsernameEditText;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mConfirmPasswordEditText;
    private ImageView mPhotoImageView;
    private Button mSignInButton;
    private Button mSignUpButton;

    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mAuth;

    private Uri mSelectedUri;

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
        }
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

    private void quit() {
        startActivity(new Intent(getActivity(),
                ChatActivity.class));
        getActivity().finish();
    }
}
