package com.gamestore.api.dto;

import com.gamestore.model.Game;

public class GameSearchResponse {
    private Game game;
    private String source;
    private String message;
    
    public GameSearchResponse() {}
    
    public GameSearchResponse(Game game, String source, String message) {
        this.game = game;
        this.source = source;
        this.message = message;
    }
    
    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}