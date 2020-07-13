package com.trichain.territory.model;

import com.google.gson.Gson;

import java.io.Serializable;

public class Player implements Serializable {
    private String id;
    private String playerName;
    private Integer playerLevel;
    private String playerPhoto;
    private Double playerLat;
    private Double playerLng;
    private Float playerDomination;
    private boolean isOnline;
    private int playerWins;

    public Player() {
    }

    public Player(String id, String playerName, Integer playerLevel, String playerPhoto,
                  Double playerLat, Double playerLng, Float playerDomination, boolean isOnline, int playerWins) {
        this.id = id;
        this.playerName = playerName;
        this.playerLevel = playerLevel;
        this.playerPhoto = playerPhoto;
        this.playerLat = playerLat;
        this.playerLng = playerLng;
        this.playerDomination = playerDomination;
        this.isOnline = isOnline;
        this.playerWins = playerWins;
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

    public int getPlayerWins() {
        return playerWins;
    }

    public void setPlayerWins(int playerWins) {
        this.playerWins = playerWins;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String jsonify() {
        return new Gson().toJson(this);
    }

    public static Player create(String serializedPlayer) {
        return new Gson().fromJson(serializedPlayer, Player.class);
    }
}
