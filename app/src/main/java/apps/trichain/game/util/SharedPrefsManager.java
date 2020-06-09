package apps.trichain.game.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import apps.trichain.game.model.Challenge;
import apps.trichain.game.model.Game;
import apps.trichain.game.model.Player;

public class SharedPrefsManager {
    private static final String SHARED_PREFS_NAME = "my_game";
    private static final String TAG = "SharedPrefsManager";
    private static SharedPrefsManager mInstance;

    private static SharedPreferences sharedPreferences;

    private SharedPrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefsManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefsManager(context);
        }
        return mInstance;
    }

    public void isLoggedIn(boolean isLoggedIn) {
        sharedPreferences.edit().putBoolean("is_logged_in", isLoggedIn).apply();
    }

    public boolean checkLoggedIn() {
        return sharedPreferences.getBoolean("is_logged_in", false);
    }

    public void savePlayerData(String player) {
        sharedPreferences.edit().putString("player", player).apply();
    }

    public Player getSavedPlayer() {
        return Player.create(sharedPreferences.getString("player", ""));
    }

    public void storeGame(Game mGame) {
        boolean isGameAlreadySaved = false;
        Log.e(TAG, "storeGame: Attempting to save game: " + mGame);
        List<Game> savedGames = retrieveStoredGames();
        Log.e(TAG, "storeGame: savedGames: " + savedGames);
        if (savedGames.size() > 0) {
            for (Game g : savedGames) {
                if (g.getExternal_url().equals(mGame.getExternal_url())) {
                    isGameAlreadySaved = true;
                    Log.e(TAG, "storeGame: Game already saved. Skipping...");
                    break;
                }
            }
            if (!isGameAlreadySaved) savedGames.add(mGame);
        } else {
            Log.e(TAG, "storeGame: List is empty");
            Log.e(TAG, "storeGame: Saving first game: " + mGame);
            savedGames.add(mGame);
        }
        Log.e(TAG, "storeGame: savedGames (new): " + savedGames);
        sharedPreferences.edit().putString("saved_games", String.valueOf(savedGames)).apply();
    }

    public List<Game> retrieveStoredGames() {
        List<Game> gamesList = new ArrayList<>();
        String mStoredGames = sharedPreferences.getString("saved_games", null);

        if (mStoredGames != null && mStoredGames.length() > 0) {
            try {
                JSONArray jsonArray = new JSONArray(mStoredGames);
                for (int y = 0; y < jsonArray.length(); y++) {
                    Game savedGame = Game.create(jsonArray.getJSONObject(y).toString());
                    Log.e(TAG, "retrieveStoredGames: Adding game to list: " + savedGame);
                    gamesList.add(savedGame);
                }
            } catch (JSONException joe) {
                joe.printStackTrace();
            }
        }

        return gamesList;
    }

    public void saveChallenge(Challenge challenge) {
        sharedPreferences.edit().putString("active_challenge", challenge.toString()).apply();
    }
}
