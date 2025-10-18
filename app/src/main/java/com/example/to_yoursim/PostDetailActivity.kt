package com.example.to_yoursim

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostDetailActivity : AppCompatActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var postImageViewPager: ViewPager2
    private lateinit var progressBar: ProgressBar
    private lateinit var regionTextView: TextView
    private lateinit var contentTextView: TextView
    private lateinit var likesTextView: TextView
    private lateinit var likeButton: ImageButton  // ImageButton으로 수정
    private lateinit var authorTextView: TextView
    private val apiService: ApiService by lazy { ServerApiClient.apiService }

    private var postId: Int = -1  // postId 변수 초기화

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                val postId = intent.getIntExtra("POST_ID", -1)
                if (postId != -1) {
                    loadPostDetails(postId)
                }
            } else {
                showError("이미지 접근 권한이 필요합니다.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        titleTextView = findViewById(R.id.titleTextView)
        postImageViewPager = findViewById(R.id.postImageViewPager)
        progressBar = findViewById(R.id.progressBar)
        regionTextView = findViewById(R.id.regionTextView)
        contentTextView = findViewById(R.id.contentTextView)
        likesTextView = findViewById(R.id.likesTextView)
        likeButton = findViewById(R.id.likeButton)
        authorTextView = findViewById(R.id.authorTextView)

        // Intent로 전달된 POST_ID를 받음
        postId = intent.getIntExtra("POST_ID", -1)

        checkPermissionAndLoadPost()

        // 좋아요 버튼 클릭 이벤트 처리
        likeButton.setOnClickListener {
            if (postId != -1) {
                increaseLikes(postId)
            } else {
                showError("게시글 ID를 찾을 수 없습니다.")
            }
        }
    }

    private fun checkPermissionAndLoadPost() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED -> {
                val postId = intent.getIntExtra("POST_ID", -1)
                if (postId != -1) {
                    loadPostDetails(postId)
                }
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }

    private fun loadPostDetails(postId: Int) {
        progressBar.visibility = View.VISIBLE

        apiService.getPostById(postId).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val post = response.body()
                    titleTextView.text = post?.title
                    regionTextView.text = post?.region
                    contentTextView.text = post?.content
                    likesTextView.text = "${post?.likes ?: 0}"
                    authorTextView.text = "작성자 :${post?.author}"

                    if (post?.images != null && post.images.isNotEmpty()) {
                        val imageUrls = post.images.map { "${BuildConfig.SERVER_URL}$it" }
                        val adapter = ImagePagerAdapter(imageUrls)
                        postImageViewPager.adapter = adapter
                    } else {
                        postImageViewPager.adapter = ImagePagerAdapter(listOf())  // 빈 리스트 처리
                    }
                } else {
                    showError("게시글 정보를 불러오는 데 실패했습니다.")
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                progressBar.visibility = View.GONE
                showError("네트워크 오류: ${t.message}")
            }
        })
    }

    private fun increaseLikes(postId: Int) {
        apiService.likePost(postId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PostDetailActivity, "좋아요를 눌렀습니다!", Toast.LENGTH_SHORT).show()
                    loadPostDetails(postId)  // 게시글 다시 로드해서 좋아요 수 업데이트
                } else {
                    showError("좋아요를 업데이트하는 데 실패했습니다.")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showError("네트워크 오류: ${t.message}")
            }
        })
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
