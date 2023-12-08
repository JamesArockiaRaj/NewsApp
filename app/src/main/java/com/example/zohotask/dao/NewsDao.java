package com.example.zohotask.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.zohotask.model.NewsEntity;

import java.util.List;

//Declaring as a Dao
@Dao
public interface NewsDao {

    //Inserting data with OnConflictStrategy to avoid duplicate data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<NewsEntity> newsList);

    //Retrievimg all rows from news_table
    @Query("SELECT * FROM news_table")
    List<NewsEntity> getAll();

    //Deleting all rows from news_table
    @Query("DELETE FROM news_table")
    void deleteAll();

}
