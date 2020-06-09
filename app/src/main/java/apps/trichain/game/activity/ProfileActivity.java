package apps.trichain.game.activity;

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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import apps.trichain.game.R;
import apps.trichain.game.databinding.ActivityProfileBinding;
import apps.trichain.game.model.Game;
import apps.trichain.game.model.Player;
import apps.trichain.game.util.SharedPrefsManager;
import apps.trichain.game.util.util;
import apps.trichain.game.viewModel.PlayerViewModel;

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
                b.tvPlayerDomination.setText(!playerAddress.equals("") ? playerAddress : "Unknown location");
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
            playerAddress = playerAddress + ", " + obj.getCountryName();
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
