package com.example.to_yoursim

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton  // Button 대신 ImageButton 임포트
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.to_yoursim.databinding.FragmentHomeBinding  // 여기가 중요합니다.
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null  // 여기도 FragmentHomeBinding으로 변경
    private val binding get() = _binding!!

    private lateinit var touristSpotAdapter: TouristSpotAdapter
    private lateinit var topPostAdapter: PostAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var topPostRecyclerView: RecyclerView
    private lateinit var spinner: Spinner
    private lateinit var prevPageButton: ImageButton  // Button 대신 ImageButton
    private lateinit var nextPageButton: ImageButton  // Button 대신 ImageButton
    private lateinit var pageNumberTextView: TextView

    private var currentPage = 1
    private var selectedRegionCode = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)  // 여기도 FragmentHomeBinding

        spinner = binding.regionSpinner

        // 'regions' 변수를 먼저 선언
        val regions = listOf(
            "서울", "부산", "인천", "대전", "대구", "광주", "울산", "세종", "경기",
            "강원", "충북", "충남", "경북", "경남", "전북", "전남", "제주"
        )

        val regionCodes = listOf(1, 2, 3, 4, 5, 6, 7, 8, 31, 32, 33, 34, 35, 36, 37, 38, 39)

        // 스피너 어댑터 설정
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, regions)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
        binding.regionSpinner.adapter = spinnerAdapter

        // RecyclerView를 설정 (관광지 목록)
        recyclerView = binding.touristSpotRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // 상단 포스트 RecyclerView 설정
        topPostRecyclerView = binding.topPostRecyclerView
        topPostRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)  // 수직 스크롤로 변경
        topPostAdapter = PostAdapter(emptyList(), requireContext()) { post -> openPostDetail(post.id) }
        binding.topPostRecyclerView.adapter = topPostAdapter

        pageNumberTextView = binding.tvPageNumber

        touristSpotAdapter = TouristSpotAdapter(emptyList())
        binding.touristSpotRecyclerView.adapter = touristSpotAdapter

        prevPageButton = binding.btnPrevPage
        nextPageButton = binding.btnNextPage

        prevPageButton.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePageNumber()
                fetchTouristSpots(selectedRegionCode, currentPage)
            } else {
                Toast.makeText(requireContext(), "첫 페이지입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        nextPageButton.setOnClickListener {
            currentPage++
            updatePageNumber()
            fetchTouristSpots(selectedRegionCode, currentPage)
        }

        binding.regionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRegionCode = regionCodes[position]
                currentPage = 1
                updatePageNumber()
                fetchTouristSpots(selectedRegionCode, currentPage)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No-op
            }
        }

        fetchTopPost()

        return binding.root
    }

    private fun fetchTouristSpots(regionCode: Int, page: Int) {
        val call = PublicDataApiClient.touristApiService.getTouristSpots(
            serviceKey = "KrB4OfOKPpufk7PLRngB3saSjurJaWGOehcwkFO2OXSCOtnjiMze7VISdxvryADMgIvfHNwqEnFuiaelUs4+aw==",
            areaCode = regionCode,
            pageNo = page
        )

        call.enqueue(object : Callback<TouristSpotResponse> {
            override fun onResponse(
                call: Call<TouristSpotResponse>,
                response: Response<TouristSpotResponse>
            ) {
                if (response.isSuccessful) {
                    val spots = response.body()?.response?.body?.items?.item ?: emptyList()
                    touristSpotAdapter = TouristSpotAdapter(spots)
                    recyclerView.adapter = touristSpotAdapter
                } else {
                    Log.e("API Error", "Failed to load data: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TouristSpotResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchTopPost() {
        val apiService = ServerApiClient.apiService

        apiService.getPosts("likes", "all").enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    val posts = response.body() ?: emptyList()
                    val topPost = posts.maxByOrNull { it.likes }
                    topPost?.let {
                        topPostAdapter.updatePosts(listOf(it))
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load top post", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                // 뷰가 파괴되었거나, 프래그먼트가 액티비티와 연결이 끊어졌으면 아무 작업도 하지 않고 함수를 종료
                if (view == null || !isAdded) {
                    return
                }

                // 이 아래의 코드는 프래그먼트가 안전한 상태일 때만 실행됩니다.
                // 예: 에러 메시지를 토스트로 보여주는 코드
                Toast.makeText(requireContext(), "데이터 로딩에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updatePageNumber() {
        pageNumberTextView.text = "Page $currentPage"
    }

    private fun openPostDetail(postId: Int) {
        val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
            putExtra("POST_ID", postId)
        }
        startActivity(intent)
    }
}
