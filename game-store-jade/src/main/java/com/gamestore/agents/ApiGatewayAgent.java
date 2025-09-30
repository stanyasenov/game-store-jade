package com.gamestore.agents;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.gamestore.api.JadeGatewayService;
import com.gamestore.api.JadeGatewayService.SearchGameRequest;
import com.gamestore.api.JadeGatewayService.GetAllGamesRequest;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ApiGatewayAgent extends Agent {
    private JadeGatewayService gatewayService;
    private Queue<Object> objectQueue = new LinkedBlockingQueue<>();
    
    protected void setup() {
        System.out.println("API Gateway Agent " + getLocalName() + " starting.");
        
        // Get the gateway service from the arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof JadeGatewayService) {
            gatewayService = (JadeGatewayService) args[0];
        } else {
            System.err.println("API Gateway Agent: JadeGatewayService not provided!");
            doDelete();
            return;
        }
        
        // Add behavior to process API requests
        addBehaviour(new ProcessApiRequests(this));
        
        // Add behavior to handle responses - using one unified behavior
        addBehaviour(new HandleAllResponses());
        
        System.out.println("API Gateway Agent ready.");
    }
    
    // !!! This method allows Spring to send objects to the agent
    public void putO2AObject(Object object, boolean blocking) {
        objectQueue.add(object);
        System.out.println("API Gateway Agent: Added object to queue: " + object.getClass().getSimpleName());
    }
    
    private class ProcessApiRequests extends TickerBehaviour {
        public ProcessApiRequests(Agent agent) {
            super(agent, 100); // Check every 100ms
        }
        
        @Override
        protected void onTick() {
            Object obj = objectQueue.poll();
            
            if (obj != null) {
                System.out.println("API Gateway Agent: Processing request of type: " + obj.getClass().getSimpleName());
            }
            
            if (obj instanceof SearchGameRequest) {
                SearchGameRequest request = (SearchGameRequest) obj;
                
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(new AID("gamestop", AID.ISLOCALNAME));
                msg.setContent(request.getTitle());
                msg.setReplyWith(request.getRequestId());
                myAgent.send(msg);
                
                System.out.println("API Gateway Agent: Sent game search request to GameStop: " + request.getTitle());
                
            } else if (obj instanceof GetAllGamesRequest) {
                GetAllGamesRequest request = (GetAllGamesRequest) obj;
                System.out.println("API Gateway Agent: Processing GetAllGamesRequest with source: " + request.getSource());
                
                if ("SQLITE".equals(request.getSource())) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(new AID("gamestop", AID.ISLOCALNAME));
                    msg.setContent("GET_ALL_GAMES_SQLITE");
                    msg.setReplyWith(request.getRequestId());
                    myAgent.send(msg);
                    
                    System.out.println("API Gateway Agent: Sent get all SQLite games request to GameStop with ID: " + request.getRequestId());
                    
                } else if ("ONTOLOGY".equals(request.getSource())) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(new AID("distributor", AID.ISLOCALNAME));
                    msg.setContent("GET_ALL_GAMES_ONTOLOGY");
                    msg.setReplyWith(request.getRequestId());
                    myAgent.send(msg);
                    
                    System.out.println("API Gateway Agent: Sent get all ontology games request to Distributor with ID: " + request.getRequestId());
                    
                } else if ("BOTH".equals(request.getSource())) {
                    // Send requests to both agents
                    String sqliteRequestId = request.getRequestId() + "_SQLITE";
                    String ontologyRequestId = request.getRequestId() + "_ONTOLOGY";
                    
                    ACLMessage sqliteMsg = new ACLMessage(ACLMessage.REQUEST);
                    sqliteMsg.addReceiver(new AID("gamestop", AID.ISLOCALNAME));
                    sqliteMsg.setContent("GET_ALL_GAMES_SQLITE");
                    sqliteMsg.setReplyWith(sqliteRequestId);
                    myAgent.send(sqliteMsg);
                    
                    ACLMessage ontologyMsg = new ACLMessage(ACLMessage.REQUEST);
                    ontologyMsg.addReceiver(new AID("distributor", AID.ISLOCALNAME));
                    ontologyMsg.setContent("GET_ALL_GAMES_ONTOLOGY");
                    ontologyMsg.setReplyWith(ontologyRequestId);
                    myAgent.send(ontologyMsg);
                    
                    System.out.println("API Gateway Agent: Sent get all games requests to both agents with IDs: " + sqliteRequestId + ", " + ontologyRequestId);
                }
            }
        }
    }
    
    // Unified response handler
    private class HandleAllResponses extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                String requestId = msg.getInReplyTo();
                String content = msg.getContent();
                
                System.out.println("API Gateway Agent: Received response for request ID: " + requestId);
                System.out.println("API Gateway Agent: Response content length: " + (content != null ? content.length() : "null"));
                
                if (requestId != null && gatewayService != null) {
                    // Check if this is a get all games response
                    if (requestId.startsWith("getAllSQLite_") || requestId.startsWith("getAllOntology_") || 
                        requestId.contains("_SQLITE") || requestId.contains("_ONTOLOGY")) {
                        
                        System.out.println("API Gateway Agent: Forwarding get all games response to service");
                        gatewayService.receiveGetAllGamesResponse(requestId, content);
                    } else {
                        // This is a regular game search response
                        System.out.println("API Gateway Agent: Forwarding game search response to service");
                        gatewayService.receiveResponse(requestId, content);
                    }
                }
            } else {
                block();
            }
        }
    }
}