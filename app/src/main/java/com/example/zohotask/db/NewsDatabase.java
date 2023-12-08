package com.example.zohotask.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.zohotask.dao.NewsDao;
import com.example.zohotask.model.NewsEntity;

//@Database annotation specifying values like entities
@Database(entities = {NewsEntity.class}, version = 1, exportSchema = false)
public abstract class NewsDatabase extends RoomDatabase {

    // Declaring DB name
    private static final String DATABASE_NAME = "news_database";

    // Singleton instance of DB
    private static NewsDatabase instance;

    // Abstract method to get the NewsDao (Data Access Object)
    public abstract NewsDao newsDao();

    // Singleton pattern to ensure only one instance of the database
    public static synchronized NewsDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            NewsDatabase.class, DATABASE_NAME)
                    .fallbackToDestructiveMigration() // Recreate the database if version is incremented
                    .build();
        }
        return instance;
    }
}

