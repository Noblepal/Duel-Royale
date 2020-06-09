package apps.trichain.game.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import apps.trichain.game.R;
import apps.trichain.game.adapter.GamesAdapter;
import apps.trichain.game.databinding.BottomSheetAddGameBinding;
import apps.trichain.game.model.Game;
import apps.trichain.game.util.RecyclerItemClickListener;
import apps.trichain.game.util.SharedPrefsManager;
import apps.trichain.game.util.util;
import apps.trichain.game.viewModel.PlayerViewModel;

import static apps.trichain.game.util.util.extractPackageName;

public class AddGameDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetAddGameBinding b;
    private DatabaseReference dbReference;
    private ValueEventListener gameListener;
    private List<Game> gamesList = new ArrayList<>();
    private Game g = null, selectedGame = null;
    private static final String TAG = "AddGameDialogFragment";
    private GamesAdapter adapter;
    private PlayerViewModel viewModel;
    private int YOUR_REQUEST_CODE = 242;
    private String appPackageName = "";
    private SharedPrefsManager sharedPrefsManager;
    private PackageManager pManager;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NO_FRAME, R.style.CustomBottomSheetDialogTheme);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_add_game, container, false);
        dbReference = FirebaseDatabase.getInstance().getReference();
        adapter = new GamesAdapter(gamesList, getContext());
        b.tvNumAvailableGames.setText(getResources().getString(R.string.games_available, 0));
        b.recyclerViewGamesAdd.setLayoutManager(new LinearLayoutManager(getContext()));
        b.recyclerViewGamesAdd.setAdapter(adapter);
        pManager = getActivity().getPackageManager();
        sharedPrefsManager = SharedPrefsManager.getInstance(getContext());

        ViewModelProvider.AndroidViewModelFactory factory = new ViewModelProvider
                .AndroidViewModelFactory(getActivity().getApplication());
        viewModel = new ViewModelProvider(getActivity(), factory).get(PlayerViewModel.class);

        /*viewModel.getGamesLiveData().observe(getViewLifecycleOwner(), mGamesList -> {
            gamesList.clear();
            Log.e(TAG, "onCreateView: Received " + mGamesList);
            gamesList.addAll(mGamesList);
        });*/

        downloadGamesList();

        b.recyclerViewGamesAdd.addOnItemTouchListener(new RecyclerItemClickListener(getContext(),
                b.recyclerViewGamesAdd, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                selectedGame = gamesList.get(position);
                Toast.makeText(getContext(), "Clicked " + selectedGame.getGameName(), Toast.LENGTH_SHORT).show();
                String playStoreURL = selectedGame.getExternal_url();
                appPackageName = extractPackageName(playStoreURL);

                if (util.isPackageInstalled(appPackageName, pManager)) {
                    Log.e(TAG, "onItemClick: Game is installed");
                    addGameToLocalList();
                } else {
                    addGameToLocalList();
                    Log.e(TAG, "onItemClick: Game is NOT installed");
                    new AlertDialog.Builder(getActivity())
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
        return b.getRoot();
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
                Toast.makeText(getContext(), "Not installed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addGameToLocalList() {
        sharedPrefsManager.storeGame(selectedGame);
        Toast.makeText(getContext(), "Game added to list", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "An error has occurred", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onCancelled: " + databaseError.getMessage());
            }
        });
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
