package com.gamestore.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import jade.wrapper.AgentController;

@Service
public class JadeGatewayService {
    private AgentController gatewayAgent;
    
    // Store game search results with request IDs
    private ConcurrentHashMap<String, CompletableFuture<String>> responseMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CompletableFuture<CombinedGamesResponse>> combinedResponseMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> sqliteResponseMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> ontologyResponseMap = new ConcurrentHashMap<>();
    
    public void setGatewayAgent(AgentController gatewayAgent) {
        this.gatewayAgent = gatewayAgent;
    }

    public CompletableFuture<String> searchGame(String title) {
        String requestId = "req_" + System.currentTimeMillis();
        CompletableFuture<String> future = new CompletableFuture<>();
        responseMap.put(requestId, future);
        
        try {
            gatewayAgent.putO2AObject(new SearchGameRequest(title, requestId), AgentController.ASYNC);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    public CompletableFuture<String> getAllGamesFromSQLite() {
        String requestId = "getAllSQLite_" + System.currentTimeMillis();
        CompletableFuture<String> future = new CompletableFuture<>();
        responseMap.put(requestId, future);
        
        try {
            gatewayAgent.putO2AObject(new GetAllGamesRequest(requestId, "SQLITE"), AgentController.ASYNC);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    public CompletableFuture<String> getAllGamesFromOntology() {
        String requestId = "getAllOntology_" + System.currentTimeMillis();
        CompletableFuture<String> future = new CompletableFuture<>();
        responseMap.put(requestId, future);
        
        try {
            gatewayAgent.putO2AObject(new GetAllGamesRequest(requestId, "ONTOLOGY"), AgentController.ASYNC);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    public CompletableFuture<CombinedGamesResponse> getAllGamesFromBoth() {
        String requestId = "getAllBoth_" + System.currentTimeMillis();
        CompletableFuture<CombinedGamesResponse> future = new CompletableFuture<>();
        combinedResponseMap.put(requestId, future);
        
        try {
            gatewayAgent.putO2AObject(new GetAllGamesRequest(requestId, "BOTH"), AgentController.ASYNC);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    public void receiveResponse(String requestId, String response) {
        CompletableFuture<String> future = responseMap.remove(requestId);
        if (future != null) {
            future.complete(response);
        }
    }
    
    public void receiveGetAllGamesResponse(String requestId, String response) {
        System.out.println("JadeGatewayService: Received getAllGames response for ID: " + requestId);
        System.out.println("JadeGatewayService: Response content: " + (response != null ? response.substring(0, Math.min(100, response.length())) + "..." : "null"));
        
        if (requestId.contains("_SQLITE")) {
            String baseRequestId = requestId.replace("_SQLITE", "");
            sqliteResponseMap.put(baseRequestId, response);
            System.out.println("JadeGatewayService: Stored SQLite response for base ID: " + baseRequestId);
            
            if (!baseRequestId.startsWith("getAllBoth_")) {
                CompletableFuture<String> future = responseMap.remove(baseRequestId);
                if (future != null) {
                    System.out.println("JadeGatewayService: Completing SQLite-only future");
                    future.complete(response);
                } else {
                    System.out.println("JadeGatewayService: WARNING - No future found for SQLite request: " + baseRequestId);
                }
            } else {
                checkAndCompleteCombinedResponse(baseRequestId);
            }
            
        } else if (requestId.contains("_ONTOLOGY")) {
            String baseRequestId = requestId.replace("_ONTOLOGY", "");
            ontologyResponseMap.put(baseRequestId, response);
            System.out.println("JadeGatewayService: Stored ontology response for base ID: " + baseRequestId);
            
            if (!baseRequestId.startsWith("getAllBoth_")) {
                CompletableFuture<String> future = responseMap.remove(baseRequestId);
                if (future != null) {
                    System.out.println("JadeGatewayService: Completing ontology-only future");
                    future.complete(response);
                } else {
                    System.out.println("JadeGatewayService: WARNING - No future found for ontology request: " + baseRequestId);
                }
            } else {
                checkAndCompleteCombinedResponse(baseRequestId);
            }
        } else if (requestId.startsWith("getAllSQLite_")) {
            CompletableFuture<String> future = responseMap.remove(requestId);
            if (future != null) {
                System.out.println("JadeGatewayService: Completing SQLite future for direct request");
                future.complete(response);
            } else {
                System.out.println("JadeGatewayService: WARNING - No future found for direct SQLite request: " + requestId);
            }
        } else if (requestId.startsWith("getAllOntology_")) {
            CompletableFuture<String> future = responseMap.remove(requestId);
            if (future != null) {
                System.out.println("JadeGatewayService: Completing ontology future for direct request");
                future.complete(response);
            } else {
                System.out.println("JadeGatewayService: WARNING - No future found for direct ontology request: " + requestId);
            }
        }
    }
    
    private void checkAndCompleteCombinedResponse(String requestId) {
        String sqliteResponse = sqliteResponseMap.get(requestId);
        String ontologyResponse = ontologyResponseMap.get(requestId);
        
        if (sqliteResponse != null && ontologyResponse != null) {
            CompletableFuture<CombinedGamesResponse> future = combinedResponseMap.remove(requestId);
            if (future != null) {
                CombinedGamesResponse combined = new CombinedGamesResponse(sqliteResponse, ontologyResponse);
                future.complete(combined);
            }
            sqliteResponseMap.remove(requestId);
            ontologyResponseMap.remove(requestId);
        }
    }
    
    public static class SearchGameRequest {
        private String title;
        private String requestId;
        
        public SearchGameRequest(String title, String requestId) {
            this.title = title;
            this.requestId = requestId;
        }
        
        public String getTitle() { return title; }
        public String getRequestId() { return requestId; }
    }
    
    public static class GetAllGamesRequest {
        private String requestId;
        private String source; // "SQLITE", "ONTOLOGY", or "BOTH"
        
        public GetAllGamesRequest(String requestId, String source) {
            this.requestId = requestId;
            this.source = source;
        }
        
        public String getRequestId() { return requestId; }
        public String getSource() { return source; }
    }
    
    public static class CombinedGamesResponse {
        private String sqliteGames;
        private String ontologyGames;
        
        public CombinedGamesResponse(String sqliteGames, String ontologyGames) {
            this.sqliteGames = sqliteGames;
            this.ontologyGames = ontologyGames;
        }
        
        public String getSqliteGames() { return sqliteGames; }
        public String getOntologyGames() { return ontologyGames; }
    }
}