package com.gamestore.api.dto;

import java.util.List;
import com.gamestore.model.Game;

public class GameListResponse {
    private List<Game> games;
    private String source;
    private String message;
    private int count;

    public GameListResponse() {}

    public GameListResponse(List<Game> games, String source, String message) {
        this.games = games;
        this.source = source;
        this.message = message;
        this.count = games.size();
    }

    public List<Game> getGames() { return games; }
    public void setGames(List<Game> games) { this.games = games; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}