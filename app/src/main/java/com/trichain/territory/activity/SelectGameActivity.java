package com.trichain.territory.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trichain.territory.R;
import com.trichain.territory.adapter.GamesAdapter;
import com.trichain.territory.databinding.ActivitySelectGameBinding;
import com.trichain.territory.model.Game;
import com.trichain.territory.util.RecyclerItemClickListener;
import com.trichain.territory.util.SharedPrefsManager;
import com.trichain.territory.util.util;
import com.trichain.territory.viewModel.PlayerViewModel;

import java.util.ArrayList;
import java.util.List;

public class SelectGameActivity extends AppCompatActivity {

    private ActivitySelectGameBinding b;
    private DatabaseReference dbReference;
    private ValueEventListener gameListener;
    private List<Game> gamesList = new ArrayList<>();
    private List<Game> installedGames = new ArrayList<>();
    private Game g = null, selectedGame = null;
    private static final String TAG = "AddGameDialogFragment";
    private GamesAdapter adapter;
    private PlayerViewModel viewModel;
    private int YOUR_REQUEST_CODE = 242;
    private String appPackageName = "";
    private SharedPrefsManager sharedPrefsManager;
    private PackageManager pManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = DataBindingUtil.setContentView(this, R.layout.activity_select_game);
        dbReference = FirebaseDatabase.getInstance().getReference();
        adapter = new GamesAdapter(gamesList, this);
        b.tvNumAvailableGames.setText(getResources().getString(R.string.games_available, 0));
        b.recyclerViewGamesAdd.setLayoutManager(new LinearLayoutManager(this));
        b.recyclerViewGamesAdd.setAdapter(adapter);
        pManager = this.getPackageManager();
        sharedPrefsManager = SharedPrefsManager.getInstance(this);

        ViewModelProvider.AndroidViewModelFactory factory = new ViewModelProvider
                .AndroidViewModelFactory(this.getApplication());
        viewModel = new ViewModelProvider(this, factory).get(PlayerViewModel.class);

       /* viewModel.getGamesLiveData().observe(this, mGamesList -> {
            gamesList.clear();
            Log.e(TAG, "onCreateView: Received " + mGamesList);
        });*/

        b.btnBackGame.setOnClickListener(v -> onBackPressed());

        downloadGamesList();

        b.recyclerViewGamesAdd.addOnItemTouchListener(new RecyclerItemClickListener(this,
                b.recyclerViewGamesAdd, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                selectedGame = gamesList.get(position);
                //Toast.makeText(SelectGameActivity.this, "Clicked " + selectedGame.getGameName(), Toast.LENGTH_SHORT).show();
                String playStoreURL = selectedGame.getExternal_url();
                appPackageName = util.extractPackageName(playStoreURL);

                if (util.isPackageInstalled(appPackageName, pManager)) {
                    Log.e(TAG, "onItemClick: Game is installed");
                    addGameToLocalList();
                } else {
                    //addGameToLocalList();
                    Log.e(TAG, "onItemClick: Game is NOT installed");
                    new AlertDialog.Builder(SelectGameActivity.this)
                            .setTitle(selectedGame.getGameName() + " is not installed")
                            .setMessage("Please install " + selectedGame.getGameName() + " from Google Play Store")
                            .setPositiveButton("Install", (dialog, which) -> launchGooglePlayStore(playStoreURL))
                            .show();

                }
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));
    }

    private void launchGooglePlayStore(String playStoreURL) {
        Log.e(TAG, "launchGooglePlayStore: Attempting to open play store");
        try {
            startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(playStoreURL)), YOUR_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException anfe) {
            anfe.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == YOUR_REQUEST_CODE) {
            if (util.isPackageInstalled(appPackageName, pManager)) {
                addGameToLocalList();
            } else {
                Toast.makeText(this, "Not installed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addGameToLocalList() {
        installedGames.addAll(sharedPrefsManager.retrieveStoredGames());
        installedGames.add(selectedGame);
        //viewModel.setGamesLiveData(installedGames);
        sharedPrefsManager.storeGame(selectedGame);
        Toast.makeText(this, "Game added to list", Toast.LENGTH_SHORT).show();
    }


    private void downloadGamesList() {
        gameListener = dbReference.child("games").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                gamesList.clear();
                for (DataSnapshot gameSnapShot : dataSnapshot.getChildren()) {
                    g = gameSnapShot.getValue(Game.class);
                    Log.e(TAG, "onDataChange: Adding: " + g);
                    gamesList.add(g);
                }

                adapter.notifyDataSetChanged();
                b.tvNumAvailableGames.setText(getResources().getString(R.string.games_available, gamesList.size()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SelectGameActivity.this, "An error has occurred", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onCancelled: " + databaseError.getMessage());
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            dbReference.removeEventListener(gameListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
