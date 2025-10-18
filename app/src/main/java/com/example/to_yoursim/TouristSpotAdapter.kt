package com.example.to_yoursim

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class TouristSpotAdapter(private val spots: List<TouristSpotItem>) : RecyclerView.Adapter<TouristSpotAdapter.TouristSpotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TouristSpotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tourist_spot, parent, false)
        return TouristSpotViewHolder(view)
    }

    private fun loadImage(url: String?, imageView: ImageView) {
        val fullUrl = if (url.isNullOrEmpty()) {
            null
        } else if (url.startsWith("http")) {
            url
        } else {
            "http://apis.data.go.kr" + url
        }

        // URL 로그로 출력
        Log.d("ImageLoader", "Loading image URL: $fullUrl")

        Glide.with(imageView.context)
            .load(fullUrl)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error)
            .into(imageView)
    }


    override fun onBindViewHolder(holder: TouristSpotViewHolder, position: Int) {
        val spot = spots[position]
        holder.titleTextView.text = spot.title
        holder.addressTextView.text = spot.addr1
        loadImage(spot.firstimage, holder.imageView)
    }

    override fun getItemCount(): Int = spots.size

    inner class TouristSpotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.touristSpotTitle)
        val addressTextView: TextView = itemView.findViewById(R.id.touristSpotAddress)
        val imageView: ImageView = itemView.findViewById(R.id.touristSpotImage)
    }
}
