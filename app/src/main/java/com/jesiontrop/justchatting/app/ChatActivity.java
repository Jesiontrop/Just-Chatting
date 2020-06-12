package com.jesiontrop.justchatting.app;

import androidx.fragment.app.Fragment;

import com.jesiontrop.justchatting.feature.chat.ChatFragment;
import com.jesiontrop.justchatting.base.core_ui.SingleFragmentActivity;

public class ChatActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return ChatFragment.newInstance();
    }
}
