package apps.trichain.game.model;

import java.io.Serializable;

public class Game implements Serializable {
    private String gameName;
    private String image_url;
    private int gameResID;
    private String external_url;

    public Game() {
    }

    public Game(String gameName, String image_url, int gameResID, String external_url) {
        this.gameName = gameName;
        this.image_url = image_url;
        this.gameResID = gameResID;
        this.external_url = external_url;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public int getGameResID() {
        return gameResID;
    }

    public void setGameResID(int gameResID) {
        this.gameResID = gameResID;
    }

    public String getExternal_url() {
        return external_url;
    }

    public void setExternal_url(String external_url) {
        this.external_url = external_url;
    }
}
