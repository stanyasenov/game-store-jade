package com.gamestore.agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import com.gamestore.ontology.OntologyDBConnector;
import com.gamestore.model.Game;
import java.util.List;
import java.util.ArrayList;

public class DistributorAgent extends Agent {
    private OntologyDBConnector ontologyDB;
    
    protected void setup() {
        System.out.println("Distributor Agent " + getLocalName() + " starting.");
        
        // Initialize ontology database connection
        ontologyDB = new OntologyDBConnector();
        
        // Add behavior to handle game search requests
        addBehaviour(new GameSearchRequestServer());
        
     // Add behavior to handle get all games requests
        addBehaviour(new GetAllGamesRequestServer());
        
        System.out.println("Distributor Agent ready.");
    }
    
    protected void takeDown() {
        // Close ontology connection
        if (ontologyDB != null) {
            ontologyDB.close();
        }
        System.out.println("Distributor Agent " + getLocalName() + " terminating.");
    }
    
    private class GetAllGamesRequestServer extends CyclicBehaviour {
        public void action() {
            // Listen for "GET_ALL_GAMES" requests
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchContent("GET_ALL_GAMES_ONTOLOGY")
            );
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                String requestId = msg.getReplyWith();
                System.out.println("Distributor Agent received get all games request");
                
                // Get all games from ontology database
                List<Game> games = ontologyDB.getAllGames();
                
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
                
                System.out.println("Distributor Agent: Sent " + games.size() + " games from ontology database");
            } else {
                block();
            }
        }
    }
    
    private class GameSearchRequestServer extends CyclicBehaviour {
        public void action() {
            // Listen for search requests, but exclude GET_ALL_GAMES requests
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.not(MessageTemplate.MatchContent("GET_ALL_GAMES_ONTOLOGY"))
            );
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                // REQUEST message received
                String title = msg.getContent();
                String requestId = msg.getReplyWith();
                
                System.out.println("Distributor Agent received search request for: " + title);
                
                ACLMessage reply = msg.createReply();
                reply.setInReplyTo(requestId);
                reply.setPerformative(ACLMessage.INFORM);
                
                // Search the ontology
                Game game = ontologyDB.findGame(title);
                
                if (game != null) {
                    // Game found in ontology
                    reply.setContent(game.toJSON());
                    System.out.println("Distributor Agent found game: " + game.getTitle());
                } else {
                    // Game not found
                    reply.setContent("NOT_FOUND");
                    System.out.println("Distributor Agent did not find game: " + title);
                }
                
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
}