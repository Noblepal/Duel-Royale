package com.trichain.territory.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trichain.territory.R;
import com.trichain.territory.adapter.GamesAdapter;
import com.trichain.territory.databinding.ActivityMainBinding;
import com.trichain.territory.fragment.AddGameDialogFragment;
import com.trichain.territory.model.Challenge;
import com.trichain.territory.model.Game;
import com.trichain.territory.model.Player;
import com.trichain.territory.util.CircleImageView;
import com.trichain.territory.util.RecyclerItemClickListener;
import com.trichain.territory.util.SharedPrefsManager;
import com.trichain.territory.util.util;
import com.trichain.territory.viewModel.PlayerViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;
import static com.trichain.territory.util.util.hideView;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener,
        GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MapsActivity";
    private static final int PERMISSION_REQUEST_CODE = 35;
    private FusedLocationProviderClient locationProviderClient;
    private double playerLat = 0.0, playerLng = 0.0;
    private GoogleMap mMap;
    private Player currentPlayer, rivalPlayer, challengingPlayer;
    private List<Game> gamesList = new ArrayList<>();
    private List<Player> playerList = new ArrayList<>();
    private boolean isHidden = true;
    private GamesAdapter adapter;
    private Bitmap bmp;
    private ActivityMainBinding b;
    private SharedPrefsManager sharedPrefsManager;
    private PlayerViewModel playerViewModel;
    private Dialog d;
    private ValueEventListener currentPlayerListener, rivalPlayerListener, challengeListener, pendingChallengeListener;
    private DatabaseReference dbReference;
    private ValueEventListener gameListener;
    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 60 * 1000;  /* 60 secs */
    private long FASTEST_INTERVAL = 15000; /* 15 sec */
    private Marker currentRivalMarker;
    private String selectedGameName = "";
    private Game chosenGame;
    AddGameDialogFragment addGameDialog;
    private boolean hasChosenGame, hasAcceptedChallenge = false, hasRivalAcceptedChallenge = false;
    LocationCallback locationCallback;
    private Challenge challengeRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = DataBindingUtil.setContentView(this, R.layout.activity_main);

        sharedPrefsManager = SharedPrefsManager.getInstance(this);

        dbReference = FirebaseDatabase.getInstance().getReference();

        ViewModelProvider.AndroidViewModelFactory factory = new ViewModelProvider.AndroidViewModelFactory(getApplication());
        factory.create(PlayerViewModel.class);

        playerViewModel = new ViewModelProvider(this, factory).get(PlayerViewModel.class);

        currentPlayer = sharedPrefsManager.getSavedPlayer();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationProviderClient = getFusedLocationProviderClient(this);

        adapter = new GamesAdapter(gamesList, this);

        b.btnSelectGame.setOnClickListener(v -> {
            toggleGamesShowing();
        });

        d = new Dialog(this);
        b.imgCurrentPlayer.setOnClickListener(v -> {
            mStartActivity(new Intent(this, ProfileActivity.class), true);
            //showCurrentPlayerDialog();
        });

        util.loadImage(b.imgCurrentPlayer, currentPlayer.getPlayerPhoto(), false);

        addGameDialog = new AddGameDialogFragment();

        b.btnAddGame.setOnClickListener(v -> {
            /*Player dummyPlayer = new Player("33$#Gg5325greESTHb5sT$%gser", "ArchNemesis", 393,
                    "null", 38.352, -2.415, 1252.14f);
            playerViewModel.setRivalPlayerData(dummyPlayer);*/
            mStartActivity(new Intent(this, SelectGameActivity.class), false);
            //showAddGameDialog();
            //showPlayerDialog();
        });

        initData();

        initListeners();

    }

    private void showAddGameDialog() {
        addGameDialog.show(getSupportFragmentManager(), "add_game_dialog");
    }

    private void initListeners() {
        currentPlayerListener = dbReference.child("players").child(currentPlayer.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        currentPlayer = dataSnapshot.getValue(Player.class);
                        //playerViewModel.setPlayerData(currentPlayer);
                        sharedPrefsManager.savePlayerData(currentPlayer.jsonify());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "onCancelled: " + databaseError.getMessage());
                    }
                });

        //playerViewModel.getPlayerData().observe(this, player -> sharedPrefsManager.savePlayerData(player.jsonify()));

        challengeListener = dbReference.child(util.DB_CHALLENGES).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren())
                    for (DataSnapshot challengeSnapShot : dataSnapshot.getChildren()) {
                        challengeRequest = challengeSnapShot.getValue(Challenge.class);
                        if (challengeRequest.getOpponentID().equals(currentPlayer.getId())) {
                            Log.e(TAG, "onDataChange: Opponent: " + challengeRequest.getOpponentID());
                            Log.e(TAG, "onDataChange: currentPlayer: " + currentPlayer.getId());
                            if (challengeRequest.getChallengeStatus().equals(util.CHALLENGE_WAITING)) {
                                dbReference.child(util.DB_PLAYERS).child(challengeRequest.getPlayerID())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                chosenGame = challengeRequest.getSelectedGame();
                                                challengingPlayer = dataSnapshot.getValue(Player.class);
                                                showAcceptChallengeDialog();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                /*Log.e(TAG, "onDataChange: Searching for challenger");
                                int nn = 1;
                                for (Player potentialChallenger : playerList) {
                                    Log.e(TAG, "onDataChange: Potential challenger [" + nn + "]: " + potentialChallenger.getId());
                                    Log.e(TAG, "onDataChange: Comparing: " + potentialChallenger.getId() + " with: " + challengeRequest.getPlayerID());
                                    if (potentialChallenger.getId().equals(challengeRequest.getPlayerID())
                                            && challengeRequest.getOpponentID().equals(currentPlayer.getId())) {
                                        Log.e(TAG, "onDataChange: Challenger found! -> " + potentialChallenger.getId());
                                        challengingPlayer = potentialChallenger;
                                        showAcceptChallengeDialog();
                                        break;
                                    }
                                    nn++;
                                }*/
                            } else if (challengeRequest.getChallengeStatus().equals(util.CHALLENGE_ACCEPTED)) {
                                Log.e(TAG, "onDataChange: Challenge accepted by opponent");
                            } else {
                                Log.e(TAG, "onDataChange: No challenger found");
                            }
                            break;
                        }
                    }
                else Log.e(TAG, "onDataChange: No challenges database reference found");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    Challenge pendingChallenge;

    private void startListeningToChallengeEvent(boolean isChallenger) {
        if (isChallenger) {
            dbReference.child(util.DB_CHALLENGES).child(currentPlayer.getId()).addValueEventListener((new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        //Toast.makeText(MainActivity.this, "My challenge exists", Toast.LENGTH_SHORT).show();
                        pendingChallenge = dataSnapshot.getValue(Challenge.class);
                        if (pendingChallenge.getChallengeStatus().equals(util.CHALLENGE_ACCEPTED)) {
                            // Toast.makeText(MainActivity.this, "Challenge Accepted", Toast.LENGTH_SHORT).show();
                            b.tvSearching.setText("Accepted! Opening + " + chosenGame.getGameName() + "...");
                            hasRivalAcceptedChallenge = true;
                            dbReference.child(util.DB_CHALLENGES).child(currentPlayer.getId()).removeValue();
                        } else if (pendingChallenge.getChallengeStatus().equals(util.CHALLENGE_WAITING)) {
                            //Toast.makeText(MainActivity.this, "Waiting...", Toast.LENGTH_SHORT).show();
                            hasRivalAcceptedChallenge = false;
                            hideView(b.rlProgress);
                            //Toast.makeText(MainActivity.this, rivalPlayer.getPlayerName() + " has REJECTED your challenge.", Toast.LENGTH_SHORT).show();
                        } else {
                            //Toast.makeText(MainActivity.this, "Declined...", Toast.LENGTH_SHORT).show();
                            hideView(b.rlProgress);
                            hasRivalAcceptedChallenge = false;
                            dbReference.child(util.DB_CHALLENGES).child(currentPlayer.getId()).removeValue();
                        }
                    } else {
                        Log.e(TAG, "onDataChange: No challenge by me");
                    }
                    if (hasRivalAcceptedChallenge) {
                        Toast.makeText(MainActivity.this, "Challenge accepted! Launching " + chosenGame.getGameName(), Toast.LENGTH_SHORT).show();
                        launchSelectedApp(util.extractPackageName(chosenGame.getExternal_url()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            }));
        } else {
            dbReference.child(util.DB_CHALLENGES).child(challengingPlayer.getId()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        //Toast.makeText(MainActivity.this, "Rival challenge exists", Toast.LENGTH_SHORT).show();
                        pendingChallenge = dataSnapshot.getValue(Challenge.class);
                        if (pendingChallenge.getChallengeStatus().equals(util.CHALLENGE_ACCEPTED)) {
                            hasAcceptedChallenge = true;
                            dbReference.child(util.DB_CHALLENGES).child(challengingPlayer.getId()).removeValue();
                        } else if (pendingChallenge.getChallengeStatus().equals(util.CHALLENGE_WAITING)) {
                            hasAcceptedChallenge = false;
                        } else {
                            hasAcceptedChallenge = false;
                            dbReference.child(util.DB_CHALLENGES).child(challengingPlayer.getId()).removeValue();
                        }
                    } else {
                        Log.e(TAG, "onDataChange: No challenges for me");
                    }

                    if (hasAcceptedChallenge) {
                        Toast.makeText(MainActivity.this, "Challenge accepted! Launching " + chosenGame.getGameName(), Toast.LENGTH_SHORT).show();
                        launchSelectedApp(util.extractPackageName(chosenGame.getExternal_url()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        /*dbReference.child(DB_CHALLENGES).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isChallenger) {
                    if (dataSnapshot.hasChild(currentPlayer.getId())) {
                        Toast.makeText(MainActivity.this, "Challenge exists", Toast.LENGTH_SHORT).show();
                        challengeSnapShot = dataSnapshot.child(currentPlayer.getId());
                        pendingChallenge = challengeSnapShot.getValue(Challenge.class);
                        if (pendingChallenge.getChallengeStatus().equals(CHALLENGE_ACCEPTED)) {
                            b.tvSearching.setText("Accepted! Opening + " + chosenGame.getGameName() + "...");
                            hasRivalAcceptedChallenge = true;
                        } else {
                            Toast.makeText(MainActivity.this, "Declined", Toast.LENGTH_SHORT).show();
                            hasRivalAcceptedChallenge = false;
                            util.hideView(b.rlProgress);
                            //Toast.makeText(MainActivity.this, rivalPlayer.getPlayerName() + " has REJECTED your challenge.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Challenge NOT exists", Toast.LENGTH_SHORT).show();
                    }
                    //dismissThisDialog();
                } else {
                    if (dataSnapshot.hasChild(challengingPlayer.getId())) {
                        Toast.makeText(MainActivity.this, "Challenge exists", Toast.LENGTH_SHORT).show();
                        challengeSnapShot = dataSnapshot.child(challengingPlayer.getId());
                        pendingChallenge = challengeSnapShot.getValue(Challenge.class);
                        if (pendingChallenge.getChallengeStatus().equals(CHALLENGE_ACCEPTED)) {
                            hasAcceptedChallenge = true;
                        } else {
                            hasAcceptedChallenge = false;
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Challenge NOT exists", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onDataChange: Challenge data not found");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });*/

        hideView(b.rlProgress);
        if (hasRivalAcceptedChallenge || hasAcceptedChallenge) {
            Toast.makeText(MainActivity.this, "Challenge accepted! Launching " + chosenGame.getGameName(), Toast.LENGTH_SHORT).show();
            launchSelectedApp(util.extractPackageName(chosenGame.getExternal_url()));
        }

    }

    private void dismissThisDialog() {
        if (d.isShowing()) d.dismiss();
    }

    private void showCurrentPlayerDialog() {
        if (!d.isShowing()) {
            TextView tvName, tvLevel;
            CircleImageView imageView;
            View view = LayoutInflater.from(this).inflate(R.layout.player_profile, null);
            d.setContentView(view);
            d.setCancelable(true);

            tvName = view.findViewById(R.id.tvRivalName);
            tvLevel = view.findViewById(R.id.tvRivalLevel);
            imageView = view.findViewById(R.id.imgRivalProfile);

            tvName.setText(currentPlayer.getPlayerName());
            tvLevel.setText("Level" + currentPlayer.getPlayerLevel());

            util.loadImage(imageView, currentPlayer.getPlayerPhoto(), false);

            d.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
            CircleImageView playerImg = d.findViewById(R.id.imgRivalProfile);
            util.loadImage(playerImg, currentPlayer.getPlayerPhoto(), false);
            d.show();
        }
    }

    private void showAcceptChallengeDialog() {
        startListeningToChallengeEvent(false);
        if (!d.isShowing()) {
            TextView tvName, tvLevel, tvMessage, btnAccept, btnDecline;
            CircleImageView imageView;
            View view = LayoutInflater.from(this).inflate(R.layout.gamer_profile, null);
            d.setContentView(view);
            d.setCancelable(false);

            tvName = view.findViewById(R.id.tvRivalName);
            tvLevel = view.findViewById(R.id.tvRivalLevel);
            imageView = view.findViewById(R.id.imgRivalProfile);

            tvName.setText(challengingPlayer.getPlayerName());
            tvLevel.setText(String.valueOf(challengingPlayer.getPlayerLevel()));

            tvMessage = view.findViewById(R.id.tvMessage);
            btnAccept = view.findViewById(R.id.btnAcceptChallenge);
            btnDecline = view.findViewById(R.id.btnRejectChallenge);

            tvMessage.setText(getString(R.string.player_wants_to_play, challengingPlayer.getPlayerName()));
            btnAccept.setOnClickListener(this);
            btnDecline.setOnClickListener(this);

            util.loadImage(imageView, challengingPlayer.getPlayerPhoto(), false);

            d.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
            CircleImageView playerImg = d.findViewById(R.id.imgRivalProfile);
            util.loadImage(playerImg, challengingPlayer.getPlayerPhoto(), false);
            d.show();
        }

    }

    private void showChallengePlayerDialog() {
        playerViewModel.getRivalPlayer().observe(this, player -> {
            if (!d.isShowing()) {
                TextView tvName, tvLevel, tvMessage, btnChallengePlayer;
                CircleImageView imageView;
                View view = LayoutInflater.from(this).inflate(R.layout.challenge_player_dialog, null);
                d.setContentView(view);
                d.setCancelable(true);

                tvName = view.findViewById(R.id.tvRivalNameC);
                tvLevel = view.findViewById(R.id.tvRivalLevelC);
                imageView = view.findViewById(R.id.imgRivalProfileC);

                tvName.setText(player.getPlayerName());
                tvLevel.setText("Level" + player.getPlayerLevel());

                if (player != currentPlayer) {
                    tvMessage = view.findViewById(R.id.tvMessageC);
                    btnChallengePlayer = view.findViewById(R.id.btnChallengePlayerC);

                    tvMessage.setText(getString(R.string.challenge_player, player.getPlayerName(), selectedGameName));
                    btnChallengePlayer.setOnClickListener(this);
                }
                util.loadImage(imageView, player.getPlayerPhoto(), false);

                d.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
                CircleImageView playerImg = d.findViewById(R.id.imgRivalProfileC);
                util.loadImage(playerImg, player.getPlayerPhoto(), false);
                d.show();
            }
        });
        startListeningToChallengeEvent(true);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void toggleGamesShowing() {
        if (!d.isShowing()) {
            RecyclerView rvGameList;
            TextView tvNoGames;
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_select_game, null);
            d.setContentView(view);
            d.setCancelable(true);
            rvGameList = d.findViewById(R.id.recyclerViewGames);
            tvNoGames = d.findViewById(R.id.tvNoGames);

            rvGameList.setLayoutManager(new LinearLayoutManager(this));
            rvGameList.setAdapter(adapter);

            if (gamesList.size() > 0) {
                hideView(tvNoGames);
            } else {
                util.showView(tvNoGames);
            }

            rvGameList.addOnItemTouchListener(new RecyclerItemClickListener(this, rvGameList,
                    new RecyclerItemClickListener.OnItemClickListener() {
                        @Override
                        public void onItemClick(View view, int position) {
                            chosenGame = gamesList.get(position);
                            selectedGameName = chosenGame.getGameName();
                            hasChosenGame = true;
                            swapViews(true);
                            util.loadImage(b.selectedGameImage, chosenGame.getImage_url(), true);
                            d.dismiss();
                            autoChallengeNearestRival();
                        }

                        @Override
                        public void onLongItemClick(View view, int position) {

                        }
                    }));

            d.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
            d.show();
        }
    }

    private void initData() {

        gamesList.addAll(sharedPrefsManager.retrieveStoredGames());
        Log.e(TAG, "initData: This is saved games list: " + gamesList);
        adapter.notifyDataSetChanged();


        b.btnCancelGame.setOnClickListener(v -> {
            hasChosenGame = false;
            swapViews(false);
        });
    }

    private void swapViews(boolean isGameSelected) {
        if (isGameSelected) {
            //hideView(b.rlGameList);
            util.showView(b.selectedGame);
            hideView(b.btnSelectGame);
        } else {
            //showView(b.rlGameList);
            hideView(b.selectedGame);
            util.showView(b.btnSelectGame);
        }
    }


    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        AlertDialog alert = new AlertDialog.Builder(this)
                .setMessage("This application requires GPS enabled to work properly")
                .setCancelable(false)
                .setPositiveButton("Enable", (dialog, id) ->
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Decline", (dialog, id) -> {
                    dialog.cancel();
                    finish();
                }).create();
        alert.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
        } else {
            initMap();
        }
        mMap.setOnMarkerClickListener(this);
    }

    private void initMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        startLocationUpdates();

        b.fabMyLocation.setOnClickListener(v -> {
            getLastKnownLocation();
        });

        getLastKnownLocation();
        //Map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        b.btnZoomIn.setOnClickListener(v -> {
            mMap.animateCamera(CameraUpdateFactory.zoomIn());
        });

        b.btnZoomOut.setOnClickListener(v -> {
            mMap.animateCamera(CameraUpdateFactory.zoomOut());
        });

        loadPlayers();
    }

    private void moveCamera(LatLng playerLatLng) {
        float zoomLevel = 8.0f;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(playerLatLng, zoomLevel));
    }

    private void addMarkerForPlayer(Player p) {
        LatLng latLng = new LatLng(p.getPlayerLat(), p.getPlayerLng());
        try {
            Glide.with(this)
                    .asBitmap()
                    .load(p.getPlayerPhoto())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            bmp = util.getCroppedBitmap(resource);
                            int height = 100;
                            int width = 100;
                            Bitmap smallMarker = Bitmap.createScaledBitmap(bmp, width, height, false);
                            BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);
                            MarkerOptions options = new MarkerOptions()
                                    .position(latLng)
                                    .title(p.getPlayerName())
                                    .icon(smallMarkerIcon);
                            currentRivalMarker = mMap.addMarker(options);
                            currentRivalMarker.setTag(p);

                            CircleOptions circleOptions = new CircleOptions();
                            circleOptions.center(new LatLng(p.getPlayerLat(), p.getPlayerLng()));
                            circleOptions.radius(p.getPlayerDomination());
                            circleOptions.fillColor(0x44FF4433);
                            circleOptions.strokeColor(0x0000000);

                            mMap.addCircle(circleOptions);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int denied = 0;
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "onRequestPermissionsResult: Permission granted");
                } else {
                    denied++;
                    Log.e(TAG, "onRequestPermissionsResult: Permission  denied!");
                }
            }
            if (denied == 0) {
                initMap();
            } else {
                Toast.makeText(this, "This app needs location permissions to work properly", Toast.LENGTH_SHORT).show();
                requestLocationPermissions();
            }
        }
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            return;
        }
        locationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        moveCamera(latLng);
                        onLocationChanged(location);
                    }
                    Log.e(TAG, "getLastKnownLocation: current location: " + playerLat + ", " + playerLng);
                });
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // do work here
                onLocationChanged(locationResult.getLastLocation());
            }
        };

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            return;
        }
        locationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback,
                Looper.myLooper());
    }

    public void onLocationChanged(Location location) {
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());

        Log.e(TAG, msg);

        playerViewModel.setLocationLiveData(location);

        currentPlayer.setPlayerLat(location.getLatitude());
        currentPlayer.setPlayerLng(location.getLongitude());

        playerViewModel.setPlayerData(currentPlayer);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAcceptChallenge:
                dbReference.child(util.DB_CHALLENGES).child(challengingPlayer.getId()).child("challengeStatus").setValue(util.CHALLENGE_ACCEPTED);
                dbReference.child(util.DB_CHALLENGES).child(challengingPlayer.getId()).child("challengeAccepted").setValue(true);
                challengeRequest.setChallengeAccepted(true);
                challengeRequest.setChallengeStatus(util.CHALLENGE_ACCEPTED);
                sharedPrefsManager.saveChallenge(challengeRequest);
                dismissThisDialog();
                break;

            case R.id.btnRejectChallenge:
                dbReference.child(util.DB_CHALLENGES).child(challengingPlayer.getId()).child("challengeStatus").setValue(util.CHALLENGE_REJECTED);
                challengeRequest.setChallengeAccepted(false);
                challengeRequest.setChallengeStatus(util.CHALLENGE_REJECTED);
                sharedPrefsManager.saveChallenge(challengeRequest);
                dismissThisDialog();
                Log.e(TAG, "onClick: Challenge declined");
                break;

            case R.id.btnChallengePlayerC:
                if (hasChosenGame) startProcessForAcceptChallenge();
                else Toast.makeText(this, "Please select game first", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void startProcessForAcceptChallenge() {
        autoChallengeNearestRival();
    }

    /*Attempt to get nearest rival*/
    boolean isFirstPlayerInList = true;
    double smallestDistance = 0.0;
    double currentRivalDistance = 0.0;
    LatLng rivalPlayerLatLng;
    LatLng currentPlayerLatLng;

    @SuppressLint("StaticFieldLeak")
    private void autoChallengeNearestRival() {
        util.showView(b.rlProgress);
        new AsyncTask<Void, Void, Player>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                startListeningToChallengeEvent(true);
                currentPlayerLatLng = new LatLng(currentPlayer.getPlayerLat(), currentPlayer.getPlayerLng());
            }

            @Override
            protected Player doInBackground(Void... voids) {
                rivalPlayer = null;
                for (Player x : playerList) {
                    if (!x.getId().equals(currentPlayer.getId()) && x.isOnline()) {
                        rivalPlayer = x;
                        rivalPlayerLatLng = new LatLng(rivalPlayer.getPlayerLat(), rivalPlayer.getPlayerLng());
                        currentRivalDistance = util.calculateDistance(currentPlayerLatLng, rivalPlayerLatLng);
                        Log.w(TAG, "autoChallengeNearestRival: currentRivalDistance: " + currentRivalDistance);
                        if (isFirstPlayerInList) {
                            Log.w(TAG, "autoChallengeNearestRival: isFirstPlayer: " + currentPlayerLatLng);
                            Log.w(TAG, "autoChallengeNearestRival: isFirstPlayer -> smallestDistance: " + smallestDistance);
                            isFirstPlayerInList = false;
                            smallestDistance = currentRivalDistance;
                            break;
                        }

                        if (currentRivalDistance < smallestDistance) {
                            Log.w(TAG, "autoChallengeNearestRival: " + currentRivalDistance + " < " + smallestDistance);
                            smallestDistance = currentRivalDistance;
                            Log.w(TAG, "autoChallengeNearestRival: New smallestDistance -> " + smallestDistance);
                            Log.w(TAG, "autoChallengeNearestRival: Current rival:  " + rivalPlayer);
                        }
                    }
                }
                return rivalPlayer;
            }

            @Override
            protected void onPostExecute(Player player) {
                super.onPostExecute(player);
                if (player != null) {
                    Challenge challenge = new Challenge();
                    challenge.setChallengeID(UUID.randomUUID().toString());
                    challenge.setPlayerID(currentPlayer.getId());
                    challenge.setOpponentID(player.getId());
                    challenge.setChallengeTime(Calendar.getInstance().getTime().toString());
                    challenge.setSelectedGame(chosenGame);
                    challenge.setChallengeStatus(util.CHALLENGE_WAITING);
                    challenge.setWinnerID("");
                    sharedPrefsManager.saveChallenge(challenge);
                    dbReference.child(util.DB_CHALLENGES).child(currentPlayer.getId()).setValue(challenge);
                    b.tvSearching.setText("Found " + player.getPlayerName() + ". Requesting challenge...");
                    b.tvSearching.setText("Awaiting " + player.getPlayerName() + "'s response...");
                    //util.hideView(b.rlProgress);
                } else {
                    hideView(b.rlProgress);
                    Toast.makeText(MainActivity.this, "No online players found!", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();


    }

    private void launchSelectedApp(String appPackageName) {
        Log.e(TAG, "launchSelectedApp: pgk name: " + appPackageName);
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(appPackageName);
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, "Not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPlayers() {
        rivalPlayerListener = dbReference.child("players").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                playerList.clear();
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    try {
                        Log.i(TAG, "loadPlayers: onDataChange: Adding to list: " + messageSnapshot.getValue(Player.class).getId());
                        playerList.add(messageSnapshot.getValue(Player.class));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                displayPlayers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled");
            }
        });
    }

    private void displayPlayers() {
        mMap.clear();
        if (playerList.size() > 0) {
            for (Player rival : playerList) {
                if (rival != currentPlayer) {
                    addMarkerForPlayer(rival);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        currentPlayer.setOnline(false);
        playerViewModel.setPlayerData(currentPlayer);
        //dbReference.child(util.DB_PLAYERS).child(currentPlayer.getId()).child("online").setValue(false);
        super.onStop();
    }

    @Override
    protected void onResume() {
        gamesList.clear();
        gamesList.addAll(sharedPrefsManager.retrieveStoredGames());
        currentPlayer.setOnline(true);
        playerViewModel.setPlayerData(currentPlayer);
        //dbReference.child(util.DB_PLAYERS).child(currentPlayer.getId()).child("online").setValue(true);
        statusCheck();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        try {
            stopLocationUpdates();
            dbReference.removeEventListener(currentPlayerListener);
            dbReference.removeEventListener(rivalPlayerListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void stopLocationUpdates() {
        locationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void mStartActivity(Intent intent, boolean isProfileIntent) {
        startActivity(intent);
        if (isProfileIntent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.fade_out);
        else overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        rivalPlayer = (Player) marker.getTag();
        if (rivalPlayer.getId().equals(currentPlayer.getId()))
            //showCurrentPlayerDialog();
            mStartActivity(new Intent(this, ProfileActivity.class), true);
        else {
            playerViewModel.setRivalPlayerData(rivalPlayer);
            showChallengePlayerDialog();
        }
        return false;
    }
}
