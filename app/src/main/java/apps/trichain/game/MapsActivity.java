package apps.trichain.game;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import apps.trichain.game.model.Game;
import apps.trichain.game.model.Player;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST_CODE = 35;
    private GoogleMap mMap;
    private static final String TAG = "MapsActivity";
    private RecyclerView gameRecyclerView;
    private List<Game> gamesList = new ArrayList<>();
    private GamesAdapter adapter;
    private RelativeLayout rlGameList;
    private AppCompatButton btnSelectGame;
    private boolean isHidden = true;
    private List<Player> playerList = new ArrayList<>();
    private Bitmap bmp;
    private boolean isPermissionsGranted = false;
    private FusedLocationProviderClient locationProviderClient;
    private double playerLat = 0.0, playerLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        btnSelectGame = findViewById(R.id.btnSelectGame);
        rlGameList = findViewById(R.id.rlGameList);
        gameRecyclerView = findViewById(R.id.recyclerViewGames);
        gameRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GamesAdapter(gamesList, this);
        gameRecyclerView.setAdapter(adapter);

        btnSelectGame.setOnClickListener(v -> {
            if (isHidden) {
                showView(rlGameList);
                isHidden = false;
            } else {
                hideView(rlGameList);
                isHidden = true;
            }
        });

        initData();

    }

    private void initData() {
        Game[] games = {
                new Game("Call Of Duty", "", R.drawable.placeholder),
                new Game("Clash Royale", "", R.drawable.clash_royale),
                new Game("Battle Grounds", "", R.drawable.battle_grounds),
                new Game("Dragon Nest", "", R.drawable.dragon_nest)
        };

        gamesList.addAll(Arrays.asList(games));
        adapter.notifyDataSetChanged();
    }

    private void generateDummyList() {
        playerList.addAll(Arrays.asList(
                new Player("Joseph", 51, "https://api.adorable.io/avatars/40/abott@adorable.png",
                        1.732642, 35.62385, 32.263F),
                new Player("Kamau", 51, "https://api.adorable.io/avatars/40/abott@adorable.png",
                        1.538242, 35.62385, 32.263F),
                new Player("Denno", 51, "https://api.adorable.io/avatars/40/abott@adorable.png",
                        1.738242, 35.42685, 32.263F),
                new Player("Joseph", 51, "https://api.adorable.io/avatars/40/abott@adorable.png",
                        1.738242, 35.78285, 32.263F)
        ));

        for (Player pl : playerList) {
            addMarkerForPlayer(pl);
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
        } else {
            initMap();
            getLastKnownLocation();
        }
        generateDummyList();
    }

    private void initMap() {
        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }

    private void moveCamera(LatLng playerLatLng) {
        float zoomLevel = 12.0f;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(playerLatLng, zoomLevel));
    }

    private void addMarkerForPlayer(Player p) {
        Glide.with(this)
                .asBitmap()
                .load(p.getPlayerPhoto())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        bmp = resource;
                        MarkerOptions options = new MarkerOptions()
                                .position(new LatLng(p.getPlayerLat(), p.getPlayerLng()))
                                .title(p.getPlayerName())
                                .icon(BitmapDescriptorFactory.fromBitmap(bmp));
                        mMap.addMarker(options);


                        CircleOptions circleOptions = new CircleOptions();
                        circleOptions.center(new LatLng(p.getPlayerLat(), p.getPlayerLng()));
                        circleOptions.radius(p.getPlayerDomination() * 500);
                        circleOptions.fillColor(0x992196F3);
                        circleOptions.strokeColor(0x992196F3);

                        mMap.addCircle(circleOptions);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == RESULT_OK && grantResults[1] == RESULT_OK) {
                isPermissionsGranted = true;
                initMap();
                getLastKnownLocation();
                Log.i(TAG, "onRequestPermissionsResult: Permissions granted");
            } else {
                isPermissionsGranted = false;
                Log.e(TAG, "onRequestPermissionsResult: Permissions denied!");
            }
        }
    }

    private void getLastKnownLocation() {
        locationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        playerLat = location.getLatitude();
                        playerLng = location.getLongitude();

                        LatLng myLocation = new LatLng(playerLat, playerLng);
                        mMap.addMarker(new MarkerOptions().position(myLocation).title("You are here"));

                        moveCamera(myLocation);
                    }
                    Log.e(TAG, "getLastKnownLocation: current location: " + playerLat + ", " + playerLng);
                });
    }

    private void showView(View v) {
        v.setVisibility(View.VISIBLE);
    }


    private void hideView(View v) {
        v.setVisibility(View.GONE);
    }
}
