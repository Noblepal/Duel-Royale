package apps.trichain.game.activity;

import android.Manifest;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import apps.trichain.game.R;
import apps.trichain.game.adapter.GamesAdapter;
import apps.trichain.game.databinding.ActivityMainBinding;
import apps.trichain.game.model.Game;
import apps.trichain.game.model.Player;
import apps.trichain.game.util.CircleImageView;
import apps.trichain.game.util.RecyclerItemClickListener;
import apps.trichain.game.util.SharedPrefsManager;
import apps.trichain.game.util.util;
import apps.trichain.game.viewModel.PlayerViewModel;

import static apps.trichain.game.util.util.hideView;
import static apps.trichain.game.util.util.showView;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener,
        GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MapsActivity";
    private static final int PERMISSION_REQUEST_CODE = 35;
    private FusedLocationProviderClient locationProviderClient;
    private double playerLat = 0.0, playerLng = 0.0;
    private GoogleMap mMap;
    private Player currentPlayer;
    private List<Game> gamesList = new ArrayList<>();
    private List<Player> playerList = new ArrayList<>();
    private boolean isHidden = true;
    private GamesAdapter adapter;
    private Bitmap bmp;
    private ActivityMainBinding b;
    private SharedPrefsManager sharedPrefsManager;
    private PlayerViewModel playerViewModel;
    private Dialog d;
    private ValueEventListener currentPlayerListener, rivalPlayerListener;
    private DatabaseReference dbReference;
    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */
    private Marker currentRivalMarker;
    private String selectedGame = "";

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
        b.recyclerViewGames.setLayoutManager(new LinearLayoutManager(this));
        b.recyclerViewGames.setAdapter(adapter);

        b.btnSelectGame.setOnClickListener(v -> {
            showView(b.rlGameList);
        });

        d = new Dialog(this);
        b.imgCurrentPlayer.setOnClickListener(v -> {
            playerViewModel.setRivalPlayerData(currentPlayer);
            showPlayerDialog();
        });

        util.loadImage(b.imgCurrentPlayer, currentPlayer.getPlayerPhoto(), false);

        b.btnRivalPlayer.setOnClickListener(v -> {
            Player dummyPlayer = new Player("33$#Gg5325greESTHb5sT$%gser", "ArchNemesis", 393,
                    "null", 38.352, -2.415, 1252.14f);
            playerViewModel.setRivalPlayerData(dummyPlayer);
            showPlayerDialog();
        });

        initData();

        initListeners();

    }

    private void initListeners() {
        currentPlayerListener = dbReference.child("players").child(currentPlayer.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        currentPlayer = dataSnapshot.getValue(Player.class);
                        playerViewModel.setPlayerData(currentPlayer);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "onCancelled: " + databaseError.getMessage());
                    }
                });

        playerViewModel.getPlayer().observe(this, player -> sharedPrefsManager.savePlayerData(player.jsonify()));

    }

    private void showPlayerDialog() {
        playerViewModel.getRivalPlayer().observe(this, player -> {
            if (!d.isShowing()) {
                TextView tvName, tvLevel, tvMessage, btnAccept, btnDecline;
                CircleImageView imageView;
                View view = player == currentPlayer ?
                        LayoutInflater.from(this).inflate(R.layout.player_profile, null)
                        : LayoutInflater.from(this).inflate(R.layout.gamer_profile, null);
                d.setContentView(view);
                d.setCancelable(true);

                tvName = view.findViewById(R.id.tvRivalName);
                tvLevel = view.findViewById(R.id.tvRivalLevel);
                imageView = view.findViewById(R.id.imgRivalProfile);

                tvName.setText(player.getPlayerName());
                tvLevel.setText(String.valueOf(player.getPlayerLevel()));

                if (player != currentPlayer) {
                    tvMessage = view.findViewById(R.id.tvMessage);
                    btnAccept = view.findViewById(R.id.btnAcceptChallenge);
                    btnDecline = view.findViewById(R.id.btnRejectChallenge);

                    tvMessage.setText(getString(R.string.player_wants_to_play, player.getPlayerName()));
                    btnAccept.setOnClickListener(this);
                    btnDecline.setOnClickListener(this);
                }
                util.loadImage(imageView, player.getPlayerPhoto(), false);

                d.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
                CircleImageView playerImg = d.findViewById(R.id.imgRivalProfile);
                util.loadImage(playerImg, player.getPlayerPhoto(), false);
                d.show();
            }
        });

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
                tvLevel.setText(String.valueOf(player.getPlayerLevel()));

                if (player != currentPlayer) {
                    tvMessage = view.findViewById(R.id.tvMessageC);
                    btnChallengePlayer = view.findViewById(R.id.btnChallengePlayerC);

                    tvMessage.setText(getString(R.string.challenge_player, player.getPlayerName(), selectedGame));
                    btnChallengePlayer.setOnClickListener(this);
                }
                util.loadImage(imageView, player.getPlayerPhoto(), false);

                d.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
                CircleImageView playerImg = d.findViewById(R.id.imgRivalProfileC);
                util.loadImage(playerImg, player.getPlayerPhoto(), false);
                d.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (b.rlGameList.getVisibility() == View.VISIBLE) {
            hideView(b.rlGameList);
        } else {
            finish();
        }
    }

    private void initData() {
        Game[] games = {
                new Game("Call Of Duty", "", R.drawable.placeholder, ""),
                new Game("Clash Royale", "", R.drawable.clash_royale, ""),
                new Game("Battle Grounds", "", R.drawable.battle_grounds, ""),
                new Game("Dragon Nest", "", R.drawable.dragon_nest, "")
        };

        gamesList.addAll(Arrays.asList(games));
        adapter.notifyDataSetChanged();

        b.recyclerViewGames.addOnItemTouchListener(new RecyclerItemClickListener(this, b.recyclerViewGames,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Game g = gamesList.get(position);
                        selectedGame = g.getGameName();
                        swapViews(true);
                        util.loadImage(b.selectedGameImage, g.getGameResID(), true);
                        //Toast.makeText(MainActivity.this, "Selected " + g.getGameName(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }
                }));

        b.btnCancelGame.setOnClickListener(v -> swapViews(false));
    }

    private void swapViews(boolean isGameSelected) {
        if (isGameSelected) {
            hideView(b.rlGameList);
            showView(b.selectedGame);
            hideView(b.btnSelectGame);
        } else {
            showView(b.rlGameList);
            hideView(b.selectedGame);
            showView(b.btnSelectGame);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        statusCheck();
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

        loadPlayers();
    }

    private void moveCamera(LatLng playerLatLng) {
        float zoomLevel = 12.0f;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(playerLatLng, zoomLevel));
    }

    private void addMarkerForPlayer(Player p) {
        LatLng latLng = new LatLng(p.getPlayerLat(), p.getPlayerLng());
        Glide.with(this)
                .asBitmap()
                .load(p.getPlayerPhoto())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        bmp = resource;
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

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        locationProviderClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do work here
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }

    public void onLocationChanged(Location location) {
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());

        Log.e(TAG, msg);

        currentPlayer.setPlayerLat(location.getLatitude());
        currentPlayer.setPlayerLng(location.getLongitude());

        playerViewModel.setPlayerData(currentPlayer);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAcceptChallenge:
                startProcessForAcceptChallenge();
                break;

            case R.id.btnRejectChallenge:
                Log.e(TAG, "onClick: Challenge declined");
                break;
        }
    }

    private void startProcessForAcceptChallenge() {
        //TODO: start intent to selected game
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
    protected void onDestroy() {
        super.onDestroy();
        try {
            dbReference.removeEventListener(currentPlayerListener);
            dbReference.removeEventListener(rivalPlayerListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        Player rivalPlayer = (Player) marker.getTag();
        playerViewModel.setRivalPlayerData(rivalPlayer);
        showChallengePlayerDialog();
        return false;
    }
}
