package apps.trichain.game.model;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Player implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("player_name")
    @Expose
    private String playerName;

    @SerializedName("player_level")
    @Expose
    private Integer playerLevel;

    @SerializedName("player_photo")
    @Expose
    private String playerPhoto;

    @SerializedName("player_lat")
    @Expose
    private Double playerLat;

    @SerializedName("player_lng")
    @Expose
    private Double playerLng;

    @SerializedName("player_domination")
    @Expose
    private Float playerDomination;

    public Player() {
    }

    public Player(String id, String playerName, Integer playerLevel, String playerPhoto, Double playerLat, Double playerLng, Float playerDomination) {
        this.id = id;
        this.playerName = playerName;
        this.playerLevel = playerLevel;
        this.playerPhoto = playerPhoto;
        this.playerLat = playerLat;
        this.playerLng = playerLng;
        this.playerDomination = playerDomination;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Integer getPlayerLevel() {
        return playerLevel;
    }

    public void setPlayerLevel(Integer playerLevel) {
        this.playerLevel = playerLevel;
    }

    public String getPlayerPhoto() {
        return playerPhoto;
    }

    public void setPlayerPhoto(String playerPhoto) {
        this.playerPhoto = playerPhoto;
    }

    public Double getPlayerLat() {
        return playerLat;
    }

    public void setPlayerLat(Double playerLat) {
        this.playerLat = playerLat;
    }

    public Double getPlayerLng() {
        return playerLng;
    }

    public void setPlayerLng(Double playerLng) {
        this.playerLng = playerLng;
    }

    public Float getPlayerDomination() {
        return playerDomination;
    }

    public void setPlayerDomination(Float playerDomination) {
        this.playerDomination = playerDomination;
    }

    public String jsonify() {
        return new Gson().toJson(this);
    }

    public static Player create(String serializedPlayer) {
        return new Gson().fromJson(serializedPlayer, Player.class);
    }
}
