package com.gamestore.ontology;

import java.io.File;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.HermiT.Reasoner;

import com.gamestore.model.Game;
import java.util.List;
import java.util.ArrayList;

public class OntologyDBConnector {
    private OWLOntology ontology;
    private OWLOntologyManager manager;
    private OWLDataFactory factory;
    private OWLReasoner reasoner;
    
    private static final String BASE_IRI = "http://www.semanticweb.org/rujam/ontologies/2025/3/games/ontology#";
    
    public OntologyDBConnector() {
        try {
            manager = OWLManager.createOWLOntologyManager();
            File ontologyFile = new File("src/main/resources/games_ontology.rdf");
            ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
            factory = manager.getOWLDataFactory();
            
            // Create reasoner
            OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
            reasoner = reasonerFactory.createReasoner(ontology);
            
            System.out.println("Ontology loaded successfully: " + ontology.getOntologyID());
            System.out.println("Total axioms: " + ontology.getAxiomCount());
        } catch (Exception e) {
            System.err.println("Error loading ontology: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public Game findGame(String title) {
        try {
            String cleanTitle = title.replace("\"", "");
            System.out.println("Searching ontology for: '" + cleanTitle + "'");
            
            System.out.println("Number of individuals in ontology: " + ontology.getIndividualsInSignature().size());
            
            System.out.println("Data properties in ontology:");
            for (OWLDataProperty prop : ontology.getDataPropertiesInSignature()) {
                System.out.println(" - " + prop.getIRI().toString());
            }
            
            OWLDataProperty hasTitleProperty = factory.getOWLDataProperty(IRI.create(BASE_IRI + "hasTitle"));
            System.out.println("Looking for property: " + hasTitleProperty.getIRI().toString());
            
            for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
                System.out.println("Checking individual: " + individual.getIRI().toString());
                
                Set<OWLLiteral> titleValues = reasoner.getDataPropertyValues(individual, hasTitleProperty);
                System.out.println("  Title values found: " + titleValues.size());
                
                for (OWLLiteral titleLiteral : titleValues) {
                    String literalValue = titleLiteral.getLiteral();
                    System.out.println("  Found title: '" + literalValue + "'");
                    
                    if (literalValue.toLowerCase().contains(cleanTitle.toLowerCase())) {
                        System.out.println("  MATCH FOUND!");
                        return createGameFromIndividual(individual);
                    }
                }
            }
            
            System.out.println("No game found with title containing: '" + cleanTitle + "'");
        } catch (Exception e) {
            System.err.println("Error querying ontology: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    
    public List<Game> getAllGames() {
        List<Game> games = new ArrayList<>();
        try {
            for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
                Set<OWLClass> types = reasoner.getTypes(individual, false).getFlattened();
                boolean isVideoGame = false;
                
                for (OWLClass type : types) {
                    String className = type.getIRI().getFragment();
                    if (className.endsWith("Game")) {
                        isVideoGame = true;
                        break;
                    }
                }
                
                if (isVideoGame) {
                    Game game = createGameFromIndividual(individual);
                    if (game != null && game.getTitle() != null) {
                        games.add(game);
                    }
                }
            }
            
            games.sort((g1, g2) -> g1.getTitle().compareToIgnoreCase(g2.getTitle()));
            
        } catch (Exception e) {
            System.err.println("Error querying all games from ontology: " + e.getMessage());
            e.printStackTrace();
        }
        return games;
    }
    
    private Game createGameFromIndividual(OWLNamedIndividual individual) {
        Game game = new Game();
        
        try {
        	
        	 game.setSource("Ontology Database");
        	 
            OWLDataProperty hasTitleProperty = factory.getOWLDataProperty(IRI.create(BASE_IRI + "hasTitle"));
            OWLDataProperty hasDescriptionProperty = factory.getOWLDataProperty(IRI.create(BASE_IRI + "hasDescription"));
            OWLDataProperty hasPriceProperty = factory.getOWLDataProperty(IRI.create(BASE_IRI + "hasPrice"));
            OWLDataProperty hasESRB_RatingProperty = factory.getOWLDataProperty(IRI.create(BASE_IRI + "hasESRB_Rating"));
            
            OWLObjectProperty hasPublisherProperty = factory.getOWLObjectProperty(IRI.create(BASE_IRI + "hasPublisher"));
            OWLObjectProperty hasDeveloperProperty = factory.getOWLObjectProperty(IRI.create(BASE_IRI + "hasDeveloper"));
            OWLObjectProperty runsOnPlatformProperty = factory.getOWLObjectProperty(IRI.create(BASE_IRI + "runsOnPlatform"));
            OWLObjectProperty hasFeatureProperty = factory.getOWLObjectProperty(IRI.create(BASE_IRI + "hasFeature"));
            
            Set<OWLLiteral> titleValues = reasoner.getDataPropertyValues(individual, hasTitleProperty);
            if (!titleValues.isEmpty()) {
                game.setTitle(titleValues.iterator().next().getLiteral());
            }
            
            Set<OWLLiteral> descriptionValues = reasoner.getDataPropertyValues(individual, hasDescriptionProperty);
            if (!descriptionValues.isEmpty()) {
                game.setDescription(descriptionValues.iterator().next().getLiteral());
            }
            
            Set<OWLLiteral> priceValues = reasoner.getDataPropertyValues(individual, hasPriceProperty);
            if (!priceValues.isEmpty()) {
                game.setPrice(Double.parseDouble(priceValues.iterator().next().getLiteral()));
            }
            
            Set<OWLLiteral> ratingValues = reasoner.getDataPropertyValues(individual, hasESRB_RatingProperty);
            if (!ratingValues.isEmpty()) {
                game.setEsrbRating(ratingValues.iterator().next().getLiteral());
            }
            
            Set<OWLClass> types = reasoner.getTypes(individual, false).getFlattened();
            for (OWLClass type : types) {
                String className = type.getIRI().getFragment();
                if (className.endsWith("Game") && !className.equals("VideoGame")) {
                    game.setGenre(className.replace("Game", ""));
                    break;
                }
            }
            
            Set<OWLNamedIndividual> publishers = reasoner.getObjectPropertyValues(individual, hasPublisherProperty).getFlattened();
            if (!publishers.isEmpty()) {
                OWLNamedIndividual publisher = publishers.iterator().next();
                OWLDataProperty hasNameProperty = factory.getOWLDataProperty(IRI.create(BASE_IRI + "hasName"));
                Set<OWLLiteral> publisherNames = reasoner.getDataPropertyValues(publisher, hasNameProperty);
                if (!publisherNames.isEmpty()) {
                    game.setPublisher(publisherNames.iterator().next().getLiteral());
                }
            }
            
            Set<OWLNamedIndividual> developers = reasoner.getObjectPropertyValues(individual, hasDeveloperProperty).getFlattened();
            if (!developers.isEmpty()) {
                OWLNamedIndividual developer = developers.iterator().next();
                OWLDataProperty hasNameProperty = factory.getOWLDataProperty(IRI.create(BASE_IRI + "hasName"));
                Set<OWLLiteral> developerNames = reasoner.getDataPropertyValues(developer, hasNameProperty);
                if (!developerNames.isEmpty()) {
                    game.setDeveloper(developerNames.iterator().next().getLiteral());
                }
            }
            
            Set<OWLNamedIndividual> platforms = reasoner.getObjectPropertyValues(individual, runsOnPlatformProperty).getFlattened();
            for (OWLNamedIndividual platform : platforms) {
                OWLDataProperty hasNameProperty = factory.getOWLDataProperty(IRI.create(BASE_IRI + "hasName"));
                Set<OWLLiteral> platformNames = reasoner.getDataPropertyValues(platform, hasNameProperty);
                if (!platformNames.isEmpty()) {
                    game.addPlatform(platformNames.iterator().next().getLiteral());
                }
            }
            
            Set<OWLNamedIndividual> features = reasoner.getObjectPropertyValues(individual, hasFeatureProperty).getFlattened();
            for (OWLNamedIndividual feature : features) {
                OWLDataProperty hasNameProperty = factory.getOWLDataProperty(IRI.create(BASE_IRI + "hasName"));
                Set<OWLLiteral> featureNames = reasoner.getDataPropertyValues(feature, hasNameProperty);
                if (!featureNames.isEmpty()) {
                    game.addFeature(featureNames.iterator().next().getLiteral());
                }
            }
            
            // Set ID and stock (not in ontology, but needed for model)
            game.setId(-1);  // -1 indicates from ontology
            game.setStock(0); // No stock info in ontology
            
        } catch (Exception e) {
            System.err.println("Error extracting game data from ontology: " + e.getMessage());
            e.printStackTrace();
        }
        
        return game;
    }
    
    
    
    public void close() {
        if (reasoner != null) {
            reasoner.dispose();
        }
    }
}