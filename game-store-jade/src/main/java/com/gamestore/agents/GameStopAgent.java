package com.gamestore.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import com.gamestore.db.RelationalDBConnector;
import com.gamestore.model.Game;
import java.util.List;
import java.util.ArrayList;
public class GameStopAgent extends Agent {
    private RelationalDBConnector dbConnector;
    
    protected void setup() {
        System.out.println("GameStop Agent " + getLocalName() + " starting.");
        
        // Initialize database connection
        dbConnector = new RelationalDBConnector();
        
        // Add behavior to handle game search requests
        addBehaviour(new GameSearchRequestServer());
        
        // Add behavior to handle get all games requests
        addBehaviour(new GetAllGamesRequestServer());
        
        System.out.println("GameStop Agent ready.");
    }
    
    protected void takeDown() {
        // Close database connection
        if (dbConnector != null) {
            dbConnector.close();
        }
        System.out.println("GameStop Agent " + getLocalName() + " terminating.");
    }
    
    private class GameSearchRequestServer extends CyclicBehaviour {
        public void action() {
            // Listen for search requests, but exclude GET_ALL_GAMES requests
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.not(MessageTemplate.MatchContent("GET_ALL_GAMES_SQLITE"))
            );
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                // REQUEST message received
                String title = msg.getContent();
                String requestId = msg.getReplyWith();
                
                System.out.println("GameStop Agent received search request for: " + title);
                
                // Search the database
                Game game = dbConnector.findGame(title);
                
                if (game != null) {
                    // Game found in local database - respond immediately
                    ACLMessage reply = msg.createReply();
                    reply.setInReplyTo(requestId);
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(game.toJSON());
                    myAgent.send(reply);
                    System.out.println("GameStop Agent found game: " + game.getTitle());
                } else {
                    // Game not found, ask distributor and wait for response
                    System.out.println("GameStop Agent did not find game in local DB, asking Distributor...");
                    addBehaviour(new AskDistributorAndWaitForReply(msg));
                }
            } else {
                block();
            }
        }
    }
    
    private class GetAllGamesRequestServer extends CyclicBehaviour {
        public void action() {
            // Listen for "GET_ALL_GAMES" requests
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchContent("GET_ALL_GAMES_SQLITE")
            );
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                String requestId = msg.getReplyWith();
                System.out.println("GameStop Agent received get all games request");
                
                // Get all games from SQLite database
                List<Game> games = dbConnector.getAllGames();
                
                // Convert games list to JSON array
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < games.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(games.get(i).toJSON());
                }
                json.append("]");
                
                // Send response back
                ACLMessage reply = msg.createReply();
                reply.setInReplyTo(requestId);
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(json.toString());
                myAgent.send(reply);
                
                System.out.println("GameStop Agent: Sent " + games.size() + " games from SQLite database");
            } else {
                block();
            }
        }
    }
    
    private class AskDistributorAndWaitForReply extends Behaviour {
        private ACLMessage originalRequest;
        private String distributorRequestId;
        private boolean responseReceived = false;
        private int step = 0;
        
        public AskDistributorAndWaitForReply(ACLMessage originalRequest) {
            this.originalRequest = originalRequest;
            this.distributorRequestId = "dist_" + originalRequest.getReplyWith();
        }
        
        public void action() {
            switch (step) {
                case 0:
                    // Send request to distributor
                    ACLMessage distributorMsg = new ACLMessage(ACLMessage.REQUEST);
                    distributorMsg.addReceiver(new AID("distributor", AID.ISLOCALNAME));
                    distributorMsg.setContent(originalRequest.getContent());
                    distributorMsg.setReplyWith(distributorRequestId);
                    myAgent.send(distributorMsg);
                    step = 1;
                    break;
                    
                case 1:
                    // Wait for distributor's response
                    MessageTemplate mt = MessageTemplate.MatchInReplyTo(distributorRequestId);
                    ACLMessage distributorReply = myAgent.receive(mt);
                    
                    if (distributorReply != null) {
                        // Got response from distributor, forward to API Gateway
                        ACLMessage reply = originalRequest.createReply();
                        reply.setInReplyTo(originalRequest.getReplyWith());
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent(distributorReply.getContent());
                        myAgent.send(reply);
                        
                        if (!distributorReply.getContent().equals("NOT_FOUND")) {
                            System.out.println("GameStop Agent: Distributor found the game");
                        } else {
                            System.out.println("GameStop Agent: Game not found in either database");
                        }
                        
                        responseReceived = true;
                    } else {
                        block();
                    }
                    break;
            }
        }
        
        public boolean done() {
            return responseReceived;
        }
    }
}