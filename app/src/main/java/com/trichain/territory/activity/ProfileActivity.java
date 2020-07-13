package com.trichain.territory.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.trichain.territory.R;
import com.trichain.territory.databinding.ActivityProfileBinding;
import com.trichain.territory.model.Game;
import com.trichain.territory.model.Player;
import com.trichain.territory.util.SharedPrefsManager;
import com.trichain.territory.util.util;
import com.trichain.territory.viewModel.PlayerViewModel;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    ActivityProfileBinding b;
    FirebaseAuth firebaseAuth;
    SharedPrefsManager sharedPrefsManager;
    Player currentPlayer;
    PlayerViewModel playerViewModel;
    String playerAddress;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = DataBindingUtil.setContentView(this, R.layout.activity_profile);
        firebaseAuth = FirebaseAuth.getInstance();
        sharedPrefsManager = SharedPrefsManager.getInstance(this);
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        currentPlayer = sharedPrefsManager.getSavedPlayer();

        List<Game> gamesList = sharedPrefsManager.retrieveStoredGames();

        Double lat = currentPlayer.getPlayerLat();
        Double lng = currentPlayer.getPlayerLng();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                getAddress(lat, lng);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                b.tvPlayerDomination.setText(null != playerAddress && !playerAddress.equals("") ?
                        playerAddress : "Unknown location");
            }
        }.execute();

        util.loadImage(b.imgProfile, currentPlayer.getPlayerPhoto(), false);

        b.btnBackProfile.setOnClickListener(v -> onBackPressed());
        b.tvPlayerName.setText(currentPlayer.getPlayerName());
        b.tvPlayerLevel.setText("" + currentPlayer.getPlayerLevel());
        b.tvGamesInstalled.setText("" + gamesList.size());
        b.tvPlayerWins.setText("" + currentPlayer.getPlayerWins());
        b.tvPlayerEmail.setText(firebaseUser.getEmail());
        b.tvPlayerID.setText(firebaseUser.getUid());
        b.tvAppVersion.setText("Version 1.0");
        b.btnLogout.setOnClickListener(v -> {
            sharedPrefsManager.isLoggedIn(false);
            firebaseAuth.signOut();
            Intent t = new Intent(ProfileActivity.this, SignInActivity.class);
            t.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(t);
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left);
            finish();
        });

    }

    public String getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);
            playerAddress = playerAddress + obj.getLocality();
            String address = playerAddress + ", " + obj.getCountryName();
            playerAddress = address.replace("null", "");
            Log.v("IGA", "Address" + playerAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return playerAddress;
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left);
    }
}
