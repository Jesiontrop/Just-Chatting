package com.jesiontrop.justchatting.feature.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.jesiontrop.justchatting.base.utils.data.ChatMessage;
import com.jesiontrop.justchatting.R;
import com.jesiontrop.justchatting.feature.sign_in.AuthenticationActivity;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatFragment extends Fragment
        implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ChatFragment";
    private static final String MESSAGES_CHILD = "messages";
    private static final String CHAT_MSG_LENGTH = "chat_msg_length";
    private static final int DEFAULT_MSG_LENGTH_LIMIT = 200;
    private static final String ANONYMOUS = "anonymous";
    private String mUsername;
    private String mPhotoUrl;

    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<ChatMessage, ChatFragment.MessageViewHolder> mFirebaseAdapter;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    public ChatFragment() {

    }

    public static ChatFragment newInstance() {
        return new ChatFragment();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mUsername = ANONYMOUS;

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if(mFirebaseUser == null) {
            startActivity(new Intent(getActivity(), AuthenticationActivity.class));
            getActivity().finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            Log.d(TAG, "Username: " + mUsername);
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(3600L)
                        .build();

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(CHAT_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT);

        mFirebaseRemoteConfig.setConfigSettingsAsync(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(defaultConfigMap);

        fetchConfig();

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle saveInstanceState) {
        View v = inflater.inflate(R.layout.chat_fragment, container,
                false);

        mProgressBar = v.findViewById(R.id.progressBar);
        mMessageRecyclerView = v.findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        SnapshotParser<ChatMessage> parser = new SnapshotParser<ChatMessage>() {
            @Override
            public ChatMessage parseSnapshot(DataSnapshot dataSnapshot) {
                Log.d(TAG, dataSnapshot.toString());
                String name = dataSnapshot.child("name").getValue(String.class);
                String text = dataSnapshot.child("text").getValue(String.class);
                String photoUrl = dataSnapshot.child("photoUrl").getValue(String.class);
                String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                ChatMessage chatMessage = new ChatMessage(text, name, photoUrl, imageUrl);
                if (chatMessage != null) {
                    chatMessage.setId(dataSnapshot.getKey());
                }
                return chatMessage;
            }
        };
        DatabaseReference messagesRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        FirebaseRecyclerOptions<ChatMessage> options =
                new FirebaseRecyclerOptions.Builder<ChatMessage>()
                        .setQuery(messagesRef, parser)
                        .build();

        mFirebaseAdapter = new FirebaseAdapter(options);

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int messageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (messageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        mMessageEditText = v.findViewById(R.id.messageEditText);
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = v.findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatMessage message = new ChatMessage(mMessageEditText.getText().toString(),
                        mUsername,
                        mPhotoUrl,
                        null);
                Log.i(TAG, "Send Message: " + message.getText());
                mFirebaseDatabaseReference.child(MESSAGES_CHILD)
                        .push().setValue(message);
                mMessageEditText.setText("");
            }
        });

        mAddMessageImageView = (ImageView) v.findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        mFirebaseAdapter.stopListening();
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAdapter.startListening();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(getActivity(), "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.fresh_config_menu:
                fetchConfig();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void fetchConfig() {
        long cacheExpiration = 3600;

        if (mFirebaseRemoteConfig.getInfo().getConfigSettings()
                .getFetchTimeoutInSeconds() == 3600) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mFirebaseRemoteConfig.activateFetched();
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error fetching config: " +
                                e.getMessage());
                        applyRetrievedLengthLimit();
                    }
                });
        FirebaseInstanceId
                .getInstance()
                .getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Remote instance ID toke " + task.getResult().getToken());
                        }
                    }
                });
    }

    private void applyRetrievedLengthLimit() {
        Long chat_msg_length =
                mFirebaseRemoteConfig.getLong(CHAT_MSG_LENGTH);
        mMessageEditText.setFilters(new InputFilter[]
                { new InputFilter.LengthFilter(chat_msg_length.intValue())});
        Log.d(TAG, "FML is:" + chat_msg_length);
    }

    private static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;

        MessageViewHolder(View v) {
            super(v);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            messengerTextView = itemView.findViewById(R.id.messengerTextView);
            messengerImageView = itemView.findViewById(R.id.messengerImageView);
        }
    }

    private class FirebaseAdapter extends FirebaseRecyclerAdapter<ChatMessage, ChatFragment.MessageViewHolder> {
        FirebaseAdapter(@NonNull FirebaseRecyclerOptions<ChatMessage> options) {
            super(options);
        }

        @NonNull
        @Override
        public ChatFragment.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message, parent, false);
            return new ChatFragment.MessageViewHolder(view);
        }

        @Override
        protected void onBindViewHolder(@NonNull final ChatFragment.MessageViewHolder viewHolder,
                                        int position,
                                        ChatMessage message) {
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);

            if (message.getText() != null) {
                viewHolder.messageTextView.setText(message.getText());
                viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                viewHolder.messageImageView.setVisibility(ImageView.GONE);
            } else if (message.getImageUrl() != null) {
                String imageUrl = message.getImageUrl();
                if (imageUrl.startsWith("gs://")) {
                    StorageReference storageReference = FirebaseStorage.getInstance()
                            .getReferenceFromUrl(imageUrl);
                    storageReference.getDownloadUrl().addOnCompleteListener(
                            new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        String downloadUrl = task.getResult().toString();
                                        Glide.with(viewHolder.messageImageView.getContext())
                                                .load(downloadUrl)
                                                .into(viewHolder.messageImageView);
                                    } else {
                                        Log.w(TAG, "Getting download url was not successful.",
                                                task.getException());
                                    }
                                }
                            });
                } else {
                    Glide.with(viewHolder.messageImageView.getContext())
                            .load(message.getImageUrl())
                            .into(viewHolder.messageImageView);
                }
                viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                viewHolder.messageTextView.setVisibility(TextView.GONE);
            }


            viewHolder.messengerTextView.setText(message.getName());
            if (message.getPhotoUrl() == null) {
                viewHolder.messengerImageView
                        .setImageDrawable(ContextCompat.getDrawable(getActivity(),
                                R.drawable.ic_account_circle_black_36dp));
            } else {
                Glide.with(getActivity())
                        .load(message.getPhotoUrl())
                        .into(viewHolder.messengerImageView);
            }

        }
    }
}

