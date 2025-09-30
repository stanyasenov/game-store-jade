package com.gamestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.gamestore.api.JadeGatewayService;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class GameStoreApplication {
    
    @Autowired
    private JadeGatewayService jadeGatewayService;

    public static void main(String[] args) {
        SpringApplication.run(GameStoreApplication.class, args);
    }
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Game Store API")
                        .version("1.0")
                        .description("REST API for GameStop and Distributor Agent Integration"));
    }
    
    @PostConstruct
    public void startJadeSystem() {
        try {
            // Get a JADE runtime instance
            Runtime jadeRuntime = Runtime.instance();
            
            // Create a default profile
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.MAIN_PORT, "1100");
            
            // Create the main container
            AgentContainer container = jadeRuntime.createMainContainer(profile);
            
            // Start the agents
            AgentController gameStopAgent = container.createNewAgent(
                    "gamestop", 
                    "com.gamestore.agents.GameStopAgent", 
                    new Object[]{});
            
            AgentController distributorAgent = container.createNewAgent(
                    "distributor", 
                    "com.gamestore.agents.DistributorAgent", 
                    new Object[]{});
            
            AgentController apiGatewayAgent = container.createNewAgent(
                    "apigateway", 
                    "com.gamestore.agents.ApiGatewayAgent", 
                    new Object[]{jadeGatewayService});
            
            // Set the gateway agent in the service
            jadeGatewayService.setGatewayAgent(apiGatewayAgent);
            
            // Start the agents
            gameStopAgent.start();
            distributorAgent.start();
            apiGatewayAgent.start();
            
            System.out.println("JADE agents started in Spring application");
            
        } catch (StaleProxyException e) {
            System.err.println("Error starting JADE agents: " + e.getMessage());
            e.printStackTrace();
        }
    }
}