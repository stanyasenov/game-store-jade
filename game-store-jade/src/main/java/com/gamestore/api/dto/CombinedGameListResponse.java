package com.gamestore.api.dto;

import java.util.List;
import com.gamestore.model.Game;

public class CombinedGameListResponse {
    private List<Game> allGames;
    private List<Game> sqliteGames;
    private List<Game> ontologyGames;
    private String message;
    private int totalCount;
    private int sqliteCount;
    private int ontologyCount;

    public CombinedGameListResponse() {}

    public CombinedGameListResponse(List<Game> allGames, List<Game> sqliteGames, 
                                   List<Game> ontologyGames, String message) {
        this.allGames = allGames;
        this.sqliteGames = sqliteGames;
        this.ontologyGames = ontologyGames;
        this.message = message;
        this.totalCount = allGames.size();
        this.sqliteCount = sqliteGames.size();
        this.ontologyCount = ontologyGames.size();
    }

    public List<Game> getAllGames() { return allGames; }
    public void setAllGames(List<Game> allGames) { this.allGames = allGames; }
    public List<Game> getSqliteGames() { return sqliteGames; }
    public void setSqliteGames(List<Game> sqliteGames) { this.sqliteGames = sqliteGames; }
    public List<Game> getOntologyGames() { return ontologyGames; }
    public void setOntologyGames(List<Game> ontologyGames) { this.ontologyGames = ontologyGames; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getSqliteCount() { return sqliteCount; }
    public void setSqliteCount(int sqliteCount) { this.sqliteCount = sqliteCount; }
    public int getOntologyCount() { return ontologyCount; }
    public void setOntologyCount(int ontologyCount) { this.ontologyCount = ontologyCount; }
}