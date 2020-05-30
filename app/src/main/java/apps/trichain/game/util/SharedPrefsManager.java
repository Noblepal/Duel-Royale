package apps.trichain.game.util;

import android.content.Context;
import android.content.SharedPreferences;

import apps.trichain.game.model.Player;

public class SharedPrefsManager {
    private static final String SHARED_PREFS_NAME = "my_game";
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
}
