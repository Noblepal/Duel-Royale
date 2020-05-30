package apps.trichain.game.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.FirebaseDatabase;

import apps.trichain.game.model.Player;

public class PlayerViewModel extends ViewModel {
    /*Current player data*/
    private MutableLiveData<Player> playerMutableLiveData = new MutableLiveData<>();
    private LiveData<Player> playerLiveData = Transformations.map(playerMutableLiveData, player -> player);

    public void setPlayerData(Player player) {
        playerMutableLiveData.setValue(player);
        FirebaseDatabase.getInstance().getReference().child("players").child(player.getId()).setValue(player);
    }

    public LiveData<Player> getPlayer() {
        return playerLiveData;
    }

    /*Rival player data*/
    private MutableLiveData<Player> rivalPlayerMutableLiveData = new MutableLiveData<>();
    private LiveData<Player> rivalPlayerLiveData = Transformations.map(rivalPlayerMutableLiveData, player -> player);

    public void setRivalPlayerData(Player player) {
        rivalPlayerMutableLiveData.setValue(player);
    }

    public LiveData<Player> getRivalPlayer() {
        return rivalPlayerLiveData;
    }
}
