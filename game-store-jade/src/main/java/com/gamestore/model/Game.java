package com.gamestore.model;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

public class Game {
    private int id;
    private String title;
    private String genre;
    private double price;
    private int stock;
    private Date releaseDate;
    private String esrbRating;
    private String description;
    private String publisher;
    private String developer;
    private List<String> platforms;
    private List<String> features;
    private String source;
    
    public Game() {
        platforms = new ArrayList<>();
        features = new ArrayList<>();
    }
    
    public Game(int id, String title, String genre, double price, int stock) {
        this();
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.price = price;
        this.stock = stock;
    }

    // Getters and Setters
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    
    public Date getReleaseDate() { return releaseDate; }
    public void setReleaseDate(Date releaseDate) { this.releaseDate = releaseDate; }
    
    public String getEsrbRating() { return esrbRating; }
    public void setEsrbRating(String esrbRating) { this.esrbRating = esrbRating; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    
    public String getDeveloper() { return developer; }
    public void setDeveloper(String developer) { this.developer = developer; }
    
    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }
    public void addPlatform(String platform) { this.platforms.add(platform); }
    
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
    public void addFeature(String feature) { this.features.add(feature); }
    
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\"id\":").append(id)
            .append(",\"title\":\"").append(title).append("\"")
            .append(",\"genre\":\"").append(genre).append("\"")
            .append(",\"price\":").append(price)
            .append(",\"stock\":").append(stock);
            
        if (description != null) {
            json.append(",\"description\":\"").append(description).append("\"");
        }
        
        if (publisher != null) {
            json.append(",\"publisher\":\"").append(publisher).append("\"");
        }
        
        if (developer != null) {
            json.append(",\"developer\":\"").append(developer).append("\"");
        }
        
        if (esrbRating != null) {
            json.append(",\"esrbRating\":\"").append(esrbRating).append("\"");
        }
        
        if (releaseDate != null) {
            // Format date as ISO string for proper JSON parsing
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            json.append(",\"releaseDate\":\"").append(sdf.format(releaseDate)).append("\"");
        }
        
        if (source != null) {
            json.append(",\"source\":\"").append(source).append("\"");
        }
        
        if (!platforms.isEmpty()) {
            json.append(",\"platforms\":[");
            for (int i = 0; i < platforms.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(platforms.get(i)).append("\"");
            }
            json.append("]");
        }
        
        if (!features.isEmpty()) {
            json.append(",\"features\":[");
            for (int i = 0; i < features.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(features.get(i)).append("\"");
            }
            json.append("]");
        }
        
        json.append("}");
        return json.toString();
    }
    
    @Override
    public String toString() {
        return "Game [id=" + id + ", title=" + title + ", genre=" + genre + ", price=" + price +
               ", publisher=" + publisher + ", source=" + source + ", platforms=" + platforms + "]";
    }
}