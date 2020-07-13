package com.trichain.territory.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.trichain.territory.databinding.ItemGameBinding;
import com.trichain.territory.model.Game;

public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {
    private List<Game> gamesList;
    private Context context;

    public GamesAdapter(List<Game> gamesList, Context context) {
        this.gamesList = gamesList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ItemGameBinding itemGameBinding = ItemGameBinding.inflate(inflater, parent, false);
        return new ViewHolder(itemGameBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Game g = gamesList.get(position);
        ItemGameBinding binding = holder.getBinding();
        holder.bind(g);
        binding.setImageUrl(g.getImage_url());
    }

    @Override
    public int getItemCount() {
        return gamesList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemGameBinding binding;

        ViewHolder(ItemGameBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Game game) {
            binding.setGame(game);
            binding.executePendingBindings();
        }

        ItemGameBinding getBinding(){
            return this.binding;
        }
    }
}
