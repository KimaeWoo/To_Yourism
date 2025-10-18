package com.example.to_yoursim

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.to_yoursim.databinding.ItemImageBinding

class ImageAdapter(private val images: List<String>) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUri = images[position]
        val fullImageUrl = "${BuildConfig.SERVER_URL}$imageUri"  // 서버의 IP 주소와 포트를 포함한 전체 URL

        // 디버그 로그로 이미지 URL 확인
        Log.d("ImageLoading", "Loading image from: $fullImageUrl")

        Glide.with(holder.binding.imageView.context)
            .load(fullImageUrl)
            .error(R.drawable.error) // 에러 이미지 설정 (옵션)
            .into(holder.binding.imageView)
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)
}
