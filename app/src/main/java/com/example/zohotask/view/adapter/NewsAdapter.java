package com.example.zohotask.view.adapter;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zohotask.view.DetailedNewsActivity;
import com.example.zohotask.R;
import com.example.zohotask.model.NewsModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
    private List<NewsModel> newsModelList;
    private List<NewsModel> filteredList;

    // Constructor to initialize the adapter
    public NewsAdapter(List<NewsModel> newsModelList) {
        this.newsModelList = new ArrayList<>(newsModelList);
        this.filteredList = new ArrayList<>(newsModelList);
    }

    // Called when RecyclerView needs a new ViewHolder to represent an item
    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_rv, parent, false);
        return new NewsViewHolder(view);
    }

    // Called to display the data at the specified position
    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsModel article = filteredList.get(position);
        holder.titleTv.setText(article.getTitle());

        // Handling description
        String title = article.getTitle();
        String description = article.getDescription();
        int TotalLen = title.length()+description.length();
        Log.d("Lenof", String.valueOf(TotalLen));

        if (article.isFullDescriptionVisible()) {
            holder.descTv.setText(description);
            holder.readMoreTextView.setVisibility(View.GONE);
        } else {
            int maxLength = Math.min(TotalLen, 25);
            String shortDescription = description.substring(0, maxLength);
            holder.descTv.setText(shortDescription + "...");
            holder.readMoreTextView.setVisibility(View.VISIBLE);
        }
        //Used Picasso library for loading image
        if (article.getImageResource() != null && !article.getImageResource().isEmpty()) {
            Picasso.get().load(article.getImageResource())
                    .error(R.drawable.no_img)
                    .resize(600, 800)
                    .onlyScaleDown()
                    .placeholder(R.drawable.no_img)
                    .into(holder.imageView);

            //Handling moving to Detailed News Activity
            holder.itemView.setOnClickListener((view -> {
                Intent intent = new Intent(view.getContext(), DetailedNewsActivity.class);
                intent.putExtra("url",article.getUrl());
                view.getContext().startActivity(intent);
            }));
        } else {
            holder.imageView.setImageResource(R.drawable.no_img);
        }

        //Handling read more functionality
        holder.readMoreTextView.setOnClickListener(v -> {
            article.setFullDescriptionVisible(!article.isFullDescriptionVisible());
            notifyItemChanged(position);
        });
    }

    //Total no. of items set in adapter
    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // Updating the filtered list based on search
    public void filterList(List<NewsModel> filteredList) {
        this.filteredList.clear();
        this.filteredList.addAll(filteredList);
        notifyDataSetChanged();
    }

    // Holding references to the views in a news item layout
    static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView titleTv, descTv, readMoreTextView;
        ImageView imageView;

        // Constructor to initialize the views
        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTv = itemView.findViewById(R.id.news_title);
            descTv = itemView.findViewById(R.id.news_desc);
            imageView = itemView.findViewById(R.id.news_img);
            readMoreTextView = itemView.findViewById(R.id.read_more_text);

        }
    }
}
