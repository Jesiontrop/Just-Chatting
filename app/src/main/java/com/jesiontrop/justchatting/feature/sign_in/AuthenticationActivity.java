package com.jesiontrop.justchatting.feature.sign_in;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jesiontrop.justchatting.R;
import com.jesiontrop.justchatting.base.core_ui.SingleFragmentActivity;

public class AuthenticationActivity extends SingleFragmentActivity
    implements SignInFragment.Callbacks, SignUpFragment.Callbacks{

    private Fragment mFragment;

    @Override
    protected Fragment createFragment() {
        return mFragment = SignInFragment.newInstance();
    }

    @Override
    public void onSignUpSelected() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = SignUpFragment.newInstance();

        fm.beginTransaction()
                .remove(mFragment)
                .add(R.id.fragment_container, fragment)
                .commit();
        mFragment = fragment;
    }


    @Override
    public void onSignInSelected() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = SignInFragment.newInstance();

        fm.beginTransaction()
                .remove(mFragment)
                .add(R.id.fragment_container, fragment)
                .commit();
        mFragment = fragment;
    }
}
