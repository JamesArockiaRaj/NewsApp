package com.example.zohotask.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.zohotask.R;

public class DetailedNewsActivity extends AppCompatActivity {

    private WebView webView;
    private ImageView backimg;
    private LinearLayout noInternetLayout;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_news);
        initValues();
    }

    //Init values and handling webview
    private void initValues() {
        //Getting url from MainActivity
        String url = getIntent().getStringExtra("url");

        // Initializing UI components
        webView = findViewById(R.id.webview);
        backimg = findViewById(R.id.backIcon);
        noInternetLayout = findViewById(R.id.noInternetLayout);
        progressBar = findViewById(R.id.progressBar);

        // Setting up a click listener for the back button
        backimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // Configuring WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Handling WebView events
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        // Checking internet connectivity before loading the URL
        NoInternet(DetailedNewsActivity.this);

        // Configuring WebChromeClient for progress tracking
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });
        // Loading the URL in the WebView
        webView.loadUrl(url);

    }

    // Checking whether internet connectivity is enabled or not
    public static boolean isInternetEnabled(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    //handle UI changes based on internet connectivity
    private void NoInternet(Context context) {
        if (!(isInternetEnabled(context))) {
            webView.setVisibility(View.GONE);
            noInternetLayout.setVisibility(View.VISIBLE);
        } else {
            webView.setVisibility(View.VISIBLE);
            noInternetLayout.setVisibility(View.GONE);
        }
    }

    //Handling back button
    @Override
    public void onBackPressed() {
        if(webView.canGoBack())
            // Navigate back in WebView history if possible; otherwise, proceeding with default behavior
            webView.goBack();
        else
            super.onBackPressed();
    }
}