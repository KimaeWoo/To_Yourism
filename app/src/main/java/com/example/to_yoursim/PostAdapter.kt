package com.example.to_yoursim

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PostAdapter(
    private var posts: List<Post>,
    private val context: Context,
    private val itemClickListener: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorTextView: TextView = itemView.findViewById(R.id.tvAuthor)
        val titleTextView: TextView = itemView.findViewById(R.id.tvTitle)
        val regionTextView: TextView = itemView.findViewById(R.id.tvRegion)
        val likesTextView: TextView = itemView.findViewById(R.id.tvLikes)
        val photoImageView: ImageView = itemView.findViewById(R.id.imageView_photo)

        fun bind(post: Post) {
            authorTextView.text = "작성자 : ${post.author}"
            titleTextView.text = post.title
            regionTextView.text = post.region
            likesTextView.text = "${post.likes}"

            val imageUrl = post.images.firstOrNull()?.let { "${BuildConfig.SERVER_URL}$it" }

            Log.d("PostAdapter", "Image URL: $imageUrl")  // 로그로 URL 확인

            if (imageUrl != null) {
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error)  // 올바른 오류 이미지 리소스
                    .into(photoImageView)
            } else {
                photoImageView.setImageResource(R.drawable.placeholder_image)
            }

            itemView.setOnClickListener { itemClickListener(post) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
