package apps.trichain.game.fragment;

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
import apps.trichain.game.viewModel.PlayerViewModel;

public class AddGameDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetAddGameBinding b;
    private DatabaseReference dbReference;
    private ValueEventListener gameListener;
    private List<Game> gamesList = new ArrayList<>();
    private Game g = null;
    private static final String TAG = "AddGameDialogFragment";
    private GamesAdapter adapter;
    private PlayerViewModel viewModel;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_add_game, container, false);
        dbReference = FirebaseDatabase.getInstance().getReference();
        adapter = new GamesAdapter(gamesList, getContext());
        b.recyclerViewGamesAdd.setLayoutManager(new LinearLayoutManager(getContext()));
        b.recyclerViewGamesAdd.setAdapter(adapter);

        ViewModelProvider.AndroidViewModelFactory factory = new ViewModelProvider
                .AndroidViewModelFactory(getActivity().getApplication());
        viewModel = new ViewModelProvider(getActivity(), factory).get(PlayerViewModel.class);

        viewModel.getGamesLiveData().observe(getViewLifecycleOwner(), mGamesList -> {
            gamesList.clear();
            Log.e(TAG, "onCreateView: Received " + mGamesList);
            gamesList.addAll(mGamesList);
        });

        downloadGamesList();

        return b.getRoot();
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
