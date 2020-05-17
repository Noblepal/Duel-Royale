package apps.trichain.game;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import apps.trichain.game.model.Game;

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
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_game, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Game g = gamesList.get(position);
        holder.gameLogo.setImageResource(g.getGameResID());
    }

    @Override
    public int getItemCount() {
        return gamesList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView gameLogo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            gameLogo = itemView.findViewById(R.id.imgGameLogo);
        }
    }
}
