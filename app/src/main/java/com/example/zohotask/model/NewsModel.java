package com.example.zohotask.model;

public class NewsModel {
    private String title;
    private String description;
    private String imageResource;
    private String url;
    private boolean fullDescriptionVisible;

    //Constructor to initialize the NewsModel object with values
    public NewsModel(String title, String description, String imageResource, String Url) {
        this.title = title;
        this.description = description;
        this.imageResource = imageResource;
        this.url = Url;
        this.fullDescriptionVisible = false;
    }

    //Getter and Setter Methods for each values
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageResource() {
        return imageResource;
    }
    public String getUrl() {
        return url;
    }

    public boolean isFullDescriptionVisible() {
        return fullDescriptionVisible;
    }

    public void setFullDescriptionVisible(boolean fullDescriptionVisible) {
        this.fullDescriptionVisible = fullDescriptionVisible;
    }
}

