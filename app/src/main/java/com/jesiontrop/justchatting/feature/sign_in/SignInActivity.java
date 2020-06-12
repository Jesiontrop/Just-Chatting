package com.jesiontrop.justchatting.feature.sign_in;

import androidx.fragment.app.Fragment;

import com.jesiontrop.justchatting.base.core_ui.SingleFragmentActivity;

public class SignInActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return SignInFragment.newInstance();
    }
}
