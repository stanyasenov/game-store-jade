package com.gamestore.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.gamestore.model.Game;
import com.gamestore.api.dto.GameSearchResponse;
import com.gamestore.api.dto.GameListResponse;
import com.gamestore.api.dto.CombinedGameListResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/games")
@Tag(name = "Games API", description = "Comprehensive API for game operations using JADE agents")
public class GameController {

    @Autowired
    private JadeGatewayService jadeGatewayService;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    // SEARCH ENDPOINTS
    
    @GetMapping("/search")
    @Operation(
        summary = "Search for a game by title", 
        description = "Searches for a game by title using JADE agents (GameStop and Distributor). The response includes where the game was found.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Game found", 
                content = @Content(schema = @Schema(implementation = GameSearchResponse.class))),
            @ApiResponse(responseCode = "404", description = "Game not found"),
            @ApiResponse(responseCode = "500", description = "Error communicating with agents")
        }
    )
    public ResponseEntity<?> searchGame(
            @Parameter(description = "Game title to search for") 
            @RequestParam String title) {
        
        try {
            CompletableFuture<String> future = jadeGatewayService.searchGame(title);
            
            String response = future.get(15, TimeUnit.SECONDS);
            
            if (response.equals("NOT_FOUND")) {
                return ResponseEntity.notFound().build();
            } else {
                try {
                    Game game = objectMapper.readValue(response, Game.class);
                    
                    // Create response with source information
                    String source = game.getSource() != null ? game.getSource() : "Unknown";
                    String message = "Game found in " + source;
                    
                    GameSearchResponse searchResponse = new GameSearchResponse(game, source, message);
                    return ResponseEntity.ok(searchResponse);
                } catch (Exception e) {
                    // If parsing fails, return the raw response
                    return ResponseEntity.ok(response);
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return ResponseEntity.status(500)
                    .body("Error communicating with game agents: " + e.getMessage());
        }
    }
    
    // LIST ALL GAMES ENDPOINTS

    @GetMapping("/sqlite/all")
    @Operation(
        summary = "Get all games from SQLite database (via GameStop agent)",
        description = "Returns all games stored in the local SQLite database through the GameStop agent",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all games from SQLite",
                content = @Content(schema = @Schema(implementation = GameListResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error communicating with GameStop agent")
        }
    )
    public ResponseEntity<?> getAllGamesFromSQLite() {
        try {
            CompletableFuture<String> future = jadeGatewayService.getAllGamesFromSQLite();
            String response = future.get(10, TimeUnit.SECONDS);
            
            List<Game> games = objectMapper.readValue(response, new TypeReference<List<Game>>(){});
            
            GameListResponse listResponse = new GameListResponse(
                games, 
                "SQLite Database (via GameStop Agent)", 
                games.size() + " games found in SQLite database"
            );
            
            return ResponseEntity.ok(listResponse);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return ResponseEntity.status(500)
                    .body("Error retrieving games from GameStop agent: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error parsing response: " + e.getMessage());
        }
    }

    @GetMapping("/ontology/all")
    @Operation(
        summary = "Get all games from ontology database (via Distributor agent)",
        description = "Returns all games stored in the ontology database through the Distributor agent",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all games from ontology",
                content = @Content(schema = @Schema(implementation = GameListResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error communicating with Distributor agent")
        }
    )
    public ResponseEntity<?> getAllGamesFromOntology() {
        try {
            CompletableFuture<String> future = jadeGatewayService.getAllGamesFromOntology();
            String response = future.get(10, TimeUnit.SECONDS);
            
            // Parse the JSON array of games
            List<Game> games = objectMapper.readValue(response, new TypeReference<List<Game>>(){});
            
            GameListResponse listResponse = new GameListResponse(
                games, 
                "Ontology Database (via Distributor Agent)", 
                games.size() + " games found in ontology database"
            );
            
            return ResponseEntity.ok(listResponse);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return ResponseEntity.status(500)
                    .body("Error retrieving games from Distributor agent: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error parsing response: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    @Operation(
        summary = "Get all games from both databases (via both agents)",
        description = "Returns all games from both SQLite and ontology databases through their respective agents",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all games from both databases",
                content = @Content(schema = @Schema(implementation = CombinedGameListResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error communicating with agents")
        }
    )
    public ResponseEntity<?> getAllGamesFromBothDatabases() {
        try {
            CompletableFuture<JadeGatewayService.CombinedGamesResponse> future = 
                jadeGatewayService.getAllGamesFromBoth();
            JadeGatewayService.CombinedGamesResponse response = future.get(15, TimeUnit.SECONDS);
            
            List<Game> sqliteGames = objectMapper.readValue(
                response.getSqliteGames(), new TypeReference<List<Game>>(){});
            List<Game> ontologyGames = objectMapper.readValue(
                response.getOntologyGames(), new TypeReference<List<Game>>(){});
            
            List<Game> allGames = new ArrayList<>();
            allGames.addAll(sqliteGames);
            allGames.addAll(ontologyGames);
            
            allGames.sort((g1, g2) -> g1.getTitle().compareToIgnoreCase(g2.getTitle()));
            
            CombinedGameListResponse combinedResponse = new CombinedGameListResponse(
                allGames,
                sqliteGames,
                ontologyGames,
                "Total: " + allGames.size() + " games (" + 
                sqliteGames.size() + " from SQLite via GameStop Agent, " + 
                ontologyGames.size() + " from Ontology via Distributor Agent)"
            );
            
            return ResponseEntity.ok(combinedResponse);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return ResponseEntity.status(500)
                    .body("Error retrieving games from agents: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error parsing response: " + e.getMessage());
        }
    }
    
    // UTILITY ENDPOINTS
    
    @GetMapping("/health")
    @Operation(
        summary = "Health check endpoint",
        description = "Simple health check to verify the API is running"
    )
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Games API is running and ready!");
    }
}