package com.jesiontrop.justchatting.feature.sign_in;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jesiontrop.justchatting.R;

public class AuthenticationActivity extends AppCompatActivity
    implements SignInFragment.Callbacks, SignUpFragment.Callbacks{

    private NavController navController;
    private BottomNavigationView mBottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        mBottomNavigationView = findViewById(R.id.navigation);
        NavigationUI.setupWithNavController(mBottomNavigationView, navController);
    }

    @Override
    public void onSignUpSelected() {
        navController.navigate(R.id.action_signInFragment_to_signUpFragment);
    }


    @Override
    public void onSignInSelected() {
        navController.navigate(R.id.action_signUpFragment_to_signInFragment);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
    }
}
