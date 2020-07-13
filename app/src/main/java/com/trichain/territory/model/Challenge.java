package com.trichain.territory.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class Challenge {

    private String challengeID;
    private String playerID;
    private String opponentID;
    private boolean challengeAccepted;
    private String challengeTime;
    private Game selectedGame;
    private String winnerID;
    private String challengeStatus;

    public Challenge() {
    }

    public String getChallengeID() {
        return challengeID;
    }

    public void setChallengeID(String challengeID) {
        this.challengeID = challengeID;
    }

    public String getPlayerID() {
        return playerID;
    }

    public void setPlayerID(String playerID) {
        this.playerID = playerID;
    }

    public String getOpponentID() {
        return opponentID;
    }

    public void setOpponentID(String opponentID) {
        this.opponentID = opponentID;
    }

    public boolean isChallengeAccepted() {
        return challengeAccepted;
    }

    public void setChallengeAccepted(boolean challengeAccepted) {
        this.challengeAccepted = challengeAccepted;
    }

    public String getChallengeTime() {
        return challengeTime;
    }

    public void setChallengeTime(String challengeTime) {
        this.challengeTime = challengeTime;
    }

    public Game getSelectedGame() {
        return selectedGame;
    }

    public void setSelectedGame(Game selectedGame) {
        this.selectedGame = selectedGame;
    }

    public String getWinnerID() {
        return winnerID;
    }

    public void setWinnerID(String winnerID) {
        this.winnerID = winnerID;
    }

    public String getChallengeStatus() {
        return challengeStatus;
    }

    public void setChallengeStatus(String challengeStatus) {
        this.challengeStatus = challengeStatus;
    }

    private static Challenge create(String serialized) {
        return new Gson().fromJson(serialized, Challenge.class);
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
