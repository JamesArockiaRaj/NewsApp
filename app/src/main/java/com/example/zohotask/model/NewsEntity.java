package com.example.zohotask.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

//Instance of this class will be stored in "news_table" in Room DB
@Entity(tableName = "news_table")
public class NewsEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public String description;
    public String imageResource;
    public String url;
}
