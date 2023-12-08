package com.example.zohotask.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import com.android.volley.RequestQueue;
import com.example.zohotask.view.adapter.NewsAdapter;
import com.example.zohotask.db.NewsDatabase;
import com.example.zohotask.model.NewsEntity;
import com.example.zohotask.R;
import com.example.zohotask.model.NewsModel;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<NewsModel> articleList = new ArrayList<>();
    private NewsAdapter adapter;
    private LinearProgressIndicator progressIndicator;
    private SearchView searchView;
    private String url;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private TextView city, DateTime, temperatureTextView, statusTextView, humidityTv, pressureTv, airQualityTextView, nosearchTv;

    private ImageView weatherImg, noDataImageView;
    private int offset = 0;
    private LinearLayout weatherDetailsLayout, weatherTopLayout, noResultsLayout;
    private String longitude, latitude, searchQuery;
    private Date currentDate = new Date();

    SimpleDateFormat customFormat = new SimpleDateFormat("E MMM d | hh:mm a", Locale.getDefault());

    private String formattedDateTime = customFormat.format(currentDate);

    private final static int REQUEST_CODE = 100;
    private NewsDatabase newsDatabase;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateTime();
            EnableGpsForFirstTime();
            NoInternet(MainActivity.this);
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initValues();
    }

    // Initialization of UI components and setup
    private void initValues() {
        //url to fetch data from Spaceflight News API
        url = "https://api.spaceflightnewsapi.net/v4/articles/?format=json&limit=10";
        humidityTv = findViewById(R.id.humidity);
        pressureTv = findViewById(R.id.pressure);
        city = findViewById(R.id.city);
        DateTime = findViewById(R.id.dateTime);
        weatherImg = findViewById(R.id.weatherImg);
        noDataImageView = findViewById(R.id.noDataImageView);
        noResultsLayout = findViewById(R.id.noResultsLayout);
        nosearchTv = findViewById(R.id.nosearch);
        weatherDetailsLayout = findViewById(R.id.weatherDetailsLayout);
        weatherTopLayout = findViewById(R.id.weatherTopLayout);
        recyclerView = findViewById(R.id.news_rv);
        progressIndicator = findViewById(R.id.progressbar);
        searchView = findViewById(R.id.search_view);
        temperatureTextView = findViewById(R.id.degree);
        statusTextView = findViewById(R.id.weatherStatus);
        airQualityTextView = findViewById(R.id.windDeg);
        searchView.setQueryHint("Search News...");

        //Initializing DB
        newsDatabase = NewsDatabase.getInstance(this);
        // Setting up UI components and adapters for rv
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NewsAdapter(articleList);
        recyclerView.setAdapter(adapter);

        //Checking if user reached the end of RV, if it's the case updating offset by adding 10 more and calling getNews()
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    changeInProgress(true);
                    offset += 10;
                    getNews(MainActivity.this);
                }
            }
        });

        //Initializing Location services
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        Log.e("Size of the list", String.valueOf(articleList.size()));

        //Handling search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        // Updating date, GPS and some other continuously using a handler
        handler.post(updateTimeRunnable);

        // Checking Internet connectivity and location permissions
        NoInternet(this);
        CheckPermissions();

        // Fetching initial news data
        getNews(this);
    }

    //Updating Date and Time
    private void updateDateTime() {
        formattedDateTime = customFormat.format(new Date());
        DateTime.setText(formattedDateTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimeRunnable);
    }

    //Handling UI and GPS if no internet
    private void NoInternet(Context context) {
        if (!(isInternetEnabled(context))) {
            temperatureTextView.setText("No Internet \uD83D\uDCF5");
            weatherTopLayout.setVisibility(View.GONE);
        } else {
            EnableGpsForFirstTime();
        }
    }

    //Saving News Data to Room Local DB
    private void saveDataToRoom(List<NewsModel> newArticleList) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                // Clearing existing data from DB
                newsDatabase.newsDao().deleteAll();

                // Converting NewsModel objects to NewsEntity and saving to DB
                List<NewsEntity> newsEntities = new ArrayList<>();
                for (NewsModel article : newArticleList) {
                    NewsEntity entity = new NewsEntity();
                    entity.title = article.getTitle();
                    entity.description = article.getDescription();
                    entity.imageResource = article.getImageResource();
                    entity.url = article.getUrl();
                    newsEntities.add(entity);
                }

                newsDatabase.newsDao().insertAll(newsEntities);
                return null;
            }
        }.execute();
    }

    //Converting NewsEntity objects to NewsModel
    private List<NewsModel> convertEntitiesToArticles(List<NewsEntity> entities) {
        // Create a new list to store the converted NewsModel objects
        List<NewsModel> articles = new ArrayList<>();
        // Iterate through each NewsEntity and create a corresponding NewsModel
        for (NewsEntity entity : entities) {
            // Create a NewsModel using the fields of the NewsEntity
            NewsModel article = new NewsModel(entity.title, entity.description, entity.imageResource, entity.url);
            // Adding the created NewsModel to the list
            articles.add(article);
        }
        // Returning the list of converted NewsModel objects
        return articles;
    }

    //Checking for location permission, if not given asking for it
    private void CheckPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                EnableGps();
            } else {
                askLocationPermission();
            }
        }
    }

    private void EnableGps() {
        if (isGPSEnabled()) {
            //If GPS enabled,getting location
            getLocation();
        } else {
            //else asking user to turn on gps by creating alertdialog
            turnOnGPS();
        }
    }
    private void EnableGpsForFirstTime() {
        //If GPS enabled,getting location and updating weather UI
        if (isGPSEnabled()) {
            getLocation();
            weatherTopLayout.setVisibility(View.VISIBLE);
            city.setVisibility(View.VISIBLE);
            //else updating ui to turn on location
        } else {
            temperatureTextView.setText("Turn On Location \uD83D\uDCCD");
            weatherTopLayout.setVisibility(View.GONE);
            city.setVisibility(View.GONE);
        }
    }

    //Method to turn on GPS
    private void turnOnGPS() {
        // AlertDialog to ask the user to enable GPS
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you willing to enable GPS for getting weather data?")
                .setCancelable(false)
                // Navigate to location settings
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                //Cancelling the dialog and update UI
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        temperatureTextView.setText("Turn On Location \uD83D\uDCCD");
                    }
                });
        // Create and displaying the AlertDialog
        final AlertDialog alert = builder.create();
        alert.show();
    }

    //Checking whether GPS is enabled or not and returning bool value
    private boolean isGPSEnabled() {
        LocationManager locationManager = null;
        boolean isEnabled = false;
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;
    }

    //Checking location permission, if permitted fetching location and calling updateLocationUI
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    updateLocationUI(location);
                }
            });
        }
    }

    //Updating Location UI
    private void updateLocationUI(Location location) {
        if (location != null) {
            try {
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                latitude = String.valueOf((addresses.get(0).getLatitude()));
                longitude = String.valueOf((addresses.get(0).getLongitude()));
                city.setText(addresses.get(0).getLocality());
                getWeather(this);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //Requesting user for location permission
    private void askLocationPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Checking if the request code matches the location permission request code
        if (requestCode==REQUEST_CODE){
            // Checking if the location permission is granted
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                // If permission is granted, call EnableGps()
                EnableGps();
            } else {
                // If permission is denied, show a toast and update UI
                Toast.makeText(this, "Please enable location services for better experience", Toast.LENGTH_SHORT).show();
                statusTextView.setText("");
                temperatureTextView.setText("Provide Location Permission");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //Changing visibility of progress indicator based on various conditions
    void changeInProgress(boolean show){
        if(show)
            progressIndicator.setVisibility(View.VISIBLE);
        else
            progressIndicator.setVisibility(View.INVISIBLE);
    }

    void getNews(Context context) {
        changeInProgress(true);
        // Executing AsyncTask to perform database query in the background
        new AsyncTask<Void, Void, List<NewsEntity>>() {
            @Override
            protected List<NewsEntity> doInBackground(Void... voids) {
                // Performing DB query to get all stored news entities
                return newsDatabase.newsDao().getAll();
            }

            @Override
            protected void onPostExecute(List<NewsEntity> newsEntities) {
                super.onPostExecute(newsEntities);
                // Check if internet connection is available
                if (isInternetEnabled(context)) {
                    // If internet is available, fetching news from API
                    fetchNewsFromApi(context);
                } else if (newsEntities != null && newsEntities.size() > 0) {
                    // If no internet but local data is available, load data from the local DB
                    List<NewsModel> newArticleList = convertEntitiesToArticles(newsEntities);
                    articleList.clear();
                    articleList.addAll(newArticleList);
                    adapter.filterList(articleList);
                    adapter.notifyDataSetChanged();

                    // Update visibility of Recyclerview and ImageView based on articleList
                    if (articleList != null && !articleList.isEmpty()) {
                        recyclerView.setVisibility(View.VISIBLE);
                        noDataImageView.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        noDataImageView.setVisibility(View.VISIBLE);
                    }
                    changeInProgress(false);
                } else {
                    // If no internet and no local data, hide RecyclerView and show no data message & img
                    changeInProgress(false);
                    recyclerView.setVisibility(View.GONE);
                    noDataImageView.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    private void fetchNewsFromApi(Context context) {
        // Constructing complete API url to fetch news data with  offset
        String apiUrl = url+"&offset="+offset;
        // JsonObjectRequest to fetch news info
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, apiUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    // Extract the "results" array from the JSON response
                    JSONArray results = response.getJSONArray("results");

                    // Parsing the JSON array into a list of NewsModel objects
                    List<NewsModel> newArticleList = parseJsonArray(results);

                    // Updating UI components
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Clear existing articles if offset is 0
                            if (offset == 0) {
                                articleList.clear();
                            }
                            // Adding new articles to the list
                            articleList.addAll(newArticleList);
                            // Updating adapter and UI
                            adapter.filterList(articleList);
                            adapter.notifyDataSetChanged();

                            // Save new articles to Room DB
                            saveDataToRoom(newArticleList);

                            // Update visibility of RV and IV based on articleList
                            if (articleList != null && !articleList.isEmpty()) {
                                recyclerView.setVisibility(View.VISIBLE);
                                noDataImageView.setVisibility(View.GONE);
                            } else {
                                recyclerView.setVisibility(View.GONE);
                                noDataImageView.setVisibility(View.VISIBLE);
                            }
                            // Stopping progressIndicator
                            changeInProgress(false);
                        }
                    });
                } catch (JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("JSONException", e.getMessage());
                            changeInProgress(false);
                        }
                    });
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("VolleyError", error.getMessage());
                        changeInProgress(false);
                    }
                });
            }
        });
        // Adding the constructed request to the Volley request queue
        Volley.newRequestQueue(context).add(request);
    }

    // Checking for internet connectivity
    public static boolean isInternetEnabled(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    // Getting weather info based on user's device location by calling api using volley
    private void getWeather(Context context) {
        //url for fetching weather details
        String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=6ab2af46409b9f26dfdd36e9b742f527&units=metric";
        //url for fetching air quality
        String airUrl = "https://api.openweathermap.org/data/2.5/air_pollution?lat=" + latitude + "&lon=" + longitude + "&appid=6ab2af46409b9f26dfdd36e9b742f527";

        // JsonObjectRequest to fetch weather info
        JsonObjectRequest weatherRequest = new JsonObjectRequest(Request.Method.GET, weatherUrl, null,
                response -> {
                    try {
                        // Parsing the JSON response to extract weather details
                        JSONObject mainObject = response.getJSONObject("main");
                        JSONArray weatherArray = response.getJSONArray("weather");
                        JSONObject weatherObject = weatherArray.getJSONObject(0);
                        // Extracting data like temperature, humidity, etc
                        double temperature = mainObject.getDouble("temp");
                        String weatherStatus = weatherObject.getString("main");
                        String humidity = mainObject.getString("humidity");
                        String pressure = mainObject.getString("pressure");

                        // Updating UI based on weather status
                        if ("Clouds".equals(weatherStatus)) {
                            statusTextView.setText("Cloudy");
                            Picasso.get().load(R.drawable.cloudy)
                                    .error(R.drawable.cloudy_sunny)
                                    .placeholder(R.drawable.cloudy_sunny)
                                    .into(weatherImg);
                        } else if("Rain".equals(weatherStatus)) {
                            statusTextView.setText("Rainy");
                            Picasso.get().load(R.drawable.rainy)
                                    .error(R.drawable.cloudy_sunny)
                                    .placeholder(R.drawable.cloudy_sunny)
                                    .into(weatherImg);
                        } else {
                            statusTextView.setText(weatherStatus);
                            Picasso.get().load(R.drawable.cloudy_sunny)
                                    .error(R.drawable.cloudy_sunny)
                                    .placeholder(R.drawable.cloudy_sunny)
                                    .into(weatherImg);
                        }
                        humidityTv.setText(humidity+"%");
                        pressureTv.setText(pressure+" hPa");
                        temperatureTextView.setText(temperature + " °C");


                    } catch (JSONException e) {
                        e.printStackTrace();
                        temperatureTextView.setText("No Data");
                    }
                },
                error -> {
                    Log.e("Weather API Error", error.toString());

                });

        // JsonObjectRequest to fetch air quality
        JsonObjectRequest airRequest = new JsonObjectRequest(Request.Method.GET, airUrl, null,
                response -> {
                    try {
                        // Parsing the JSON response to extract air quality
                        JSONArray list = response.getJSONArray("list");
                        if (list.length() > 0) {
                            int aqi = list.getJSONObject(0).getJSONObject("main").getInt("aqi");
                            //Setting corresponding string value to airQualityTextView
                            String airQualityString = getAirQualityString(aqi);
                            airQualityTextView.setText(airQualityString);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.e("Air Quality API Error", error.toString());
                });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(weatherRequest);
        requestQueue.add(airRequest);
    }

    //Converting int value to corresponding string value based on provided details in api docs
    private String getAirQualityString(int aqi) {
        String airQualityString;

        if (aqi == 1) {
            airQualityString = "Good";
        } else if (aqi == 2) {
            airQualityString = "Fair";
        } else if (aqi == 3) {
            airQualityString = "Moderate";
        } else if (aqi == 4) {
            airQualityString = "Poor";
        } else if (aqi == 5) {
            airQualityString = "Very Poor";
        } else {
            airQualityString = "Unknown";
        }

        return airQualityString;
    }

    //Parsing JSON array obtained from news API
    private List<NewsModel> parseJsonArray(JSONArray jsonArray) {
        List<NewsModel> articles = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                // Extracting title, description, image URL, and URL from the JSON array
                JSONObject result = jsonArray.getJSONObject(i);
                String title = result.getString("title");
                String description = result.getString("summary");
                String imageUrl = result.getString("image_url");
                String url = result.getString("url");

                // Creating a NewsModel object and adding it to the list
                NewsModel article = new NewsModel(title, description, imageUrl, url);
                articles.add(article);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return articles;
    }

    // Filtering news articles based on the search query
    void filter(String query) {
        // New list to store filtered articles
        List<NewsModel> filteredList = new ArrayList<>();
        searchQuery = query;
        // Iterating through the original list of articles
        for (NewsModel article : articleList) {
            // Check if either the title or description of the article contains the search query
            if (article.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    article.getDescription().toLowerCase().contains(query.toLowerCase())) {
                // If the article matches the search query, adding to the filtered list
                filteredList.add(article);
            }
        }
        // Updating the adapter with the filtered list
        adapter.filterList(filteredList);

        // Updating the visibility of UI elements based on the filtered list
        if (filteredList != null && !filteredList.isEmpty()) {
            recyclerView.setVisibility(View.VISIBLE);
            noResultsLayout.setVisibility(View.GONE);
            nosearchTv.setVisibility(View.GONE);

        } else {
            recyclerView.setVisibility(View.GONE);
            noResultsLayout.setVisibility(View.VISIBLE);
            nosearchTv.setVisibility(View.VISIBLE);

        }
        // Toggle the visibility of weather details layout based on whether the search query is empty for better UX
        if (query.isEmpty()) {
            weatherDetailsLayout.setVisibility(View.VISIBLE);
        } else {
            weatherDetailsLayout.setVisibility(View.GONE);
        }
    }

    // Handling back press and show weather details or exit the app
    @Override
    public void onBackPressed() {
//        If user is currently searching means, the weatherDetailsLayout is gone initially, if it's the case while pressing
//        back, weatherDetailsLayout will be visible. Made this for better UX
        if (weatherDetailsLayout.getVisibility() == View.GONE) {
            weatherDetailsLayout.setVisibility(View.VISIBLE);
            searchView.setQuery("", false);
        } else {
            super.onBackPressed();
        }
    }

}