package com.gamestore;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class GameStoreMain {
    public static void main(String[] args) {
        try {
            // Get a JADE runtime instance
            Runtime runtime = Runtime.instance();
            
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.MAIN_PORT, "1100"); 
            profile.setParameter(Profile.GUI, "true"); // Enable JADE GUI
            
            AgentContainer mainContainer = runtime.createMainContainer(profile);
            
            // TODO, Try to remove sleep
            Thread.sleep(1000);
            
            AgentController gameStopAgent = mainContainer.createNewAgent(
                "gamestop", 
                "com.gamestore.agents.GameStopAgent", 
                new Object[]{});
            
            AgentController distributorAgent = mainContainer.createNewAgent(
                "distributor", 
                "com.gamestore.agents.DistributorAgent", 
                new Object[]{});
            
            // Agents start
            gameStopAgent.start();
            distributorAgent.start();
            
            System.out.println("Game Store system is running...");
            
        } catch (StaleProxyException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}