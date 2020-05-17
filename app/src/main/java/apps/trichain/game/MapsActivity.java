package apps.trichain.game;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import apps.trichain.game.model.Game;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final String TAG = "MapsActivity";
    private RecyclerView gameRecyclerView;
    private List<Game> gamesList = new ArrayList<>();
    private GamesAdapter adapter;
    private RelativeLayout rlGameList;
    private AppCompatButton btnSelectGame;
    private boolean isHidden = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        /*LatLng sydney = new LatLng(1.2921, 36.8219);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    private void showView(View v) {
        v.setVisibility(View.VISIBLE);
    }


    private void hideView(View v) {
        v.setVisibility(View.GONE);
    }
}
