package com.example.to_yoursim

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.to_yoursim.databinding.FragmentCourseListBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CourseListFragment : Fragment() {

    private var _binding: FragmentCourseListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PostAdapter

    private var sortOrder: String = "latest"
    private var location: String = "all" // 기본값을 'all'로 설정

    // 도/시/군 데이터
    private val regionData = mapOf(
        "서울특별시" to listOf(
            "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구",
            "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구",
            "용산구", "은평구", "종로구", "중구", "중랑구"
        ),
        "부산광역시" to listOf(
            "강서구", "금정구", "기장군", "동구", "부산진구", "사상구", "사하구", "서구", "수영구", "해운대구",
            "중구", "남구", "북구", "영도구", "연제구"
        ),
        "대구광역시" to listOf(
            "중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군"
        ),
        "인천광역시" to listOf(
            "남동구", "부평구", "서구", "연수구", "중구", "동구", "강화군", "옹진군"
        ),
        "광주광역시" to listOf(
            "동구", "서구", "남구", "북구", "광산구"
        ),
        "대전광역시" to listOf(
            "동구", "중구", "서구", "유성구", "대덕구"
        ),
        "울산광역시" to listOf(
            "남구", "동구", "중구", "북구", "울주군"
        ),
        "세종특별자치시" to listOf(
            "세종시"
        ),
        "경기도" to listOf(
            "수원시", "용인시", "성남시", "화성시", "안양시", "안산시", "평택시", "광명시", "오산시", "이천시",
            "여주군", "군포시", "포천시", "양평군", "남양주시", "부천시", "구리시", "하남시"
        ),
        "강원특별자치도" to listOf(
            "춘천시", "강릉시", "동해시", "속초시", "삼척시", "원주", "홍천군", "횡성군", "영월군", "평창군",
            "정선군", "철원군", "화천군", "양구군", "인제군", "양양군"
        ),
        "충청북도" to listOf(
            "청주시", "충주시", "제천시", "옥천군", "보은군", "영동군", "진천군", "음성군", "괴산군", "단양군"
        ),
        "충청남도" to listOf(
            "천안시", "공주시", "보령시", "아산시", "서산시", "홍성군", "논산시", "계룡시", "당진시", "서천군"
        ),
        "전라북도" to listOf(
            "전주시", "익산시", "군산시", "정읍시", "남원시", "완주군", "진안군", "무주군", "장수군", "임실군",
            "순창군", "고창군", "부안군"
        ),
        "전라남도" to listOf(
            "목포시", "여수시", "순천시", "광양시", "나주시", "무안군", "해남군", "완도군", "진도군", "장흥군",
            "강진군", "영암군", "구례군", "담양군", "함평군", "영광군", "신안군"
        ),
        "경상북도" to listOf(
            "포항시", "경산시", "구미시", "안동시", "영천시", "김천시", "상주시", "문경시", "예천군", "봉화군",
            "울진군", "울릉군"
        ),
        "경상남도" to listOf(
            "창원시", "김해시", "양산시", "진주시", "밀양시", "거제시", "함안군", "함양군", "하동군", "산청군",
            "의령군", "창녕군", "고성군"
        ),
        "제주특별자치도" to listOf(
            "제주시", "서귀포시"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCourseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PostAdapter(emptyList(), requireContext()) { post -> openPostDetail(post.id) }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        fetchPosts()

        binding.btnSortOptions.setOnClickListener {
            showSortDialog()
        }

        binding.btnLocationOptions.setOnClickListener {
            showProvinceDialog()
        }

        binding.btnCreatePost.setOnClickListener {
            val intent = Intent(requireContext(), CreatePostActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchPosts() {
        val apiService = ServerApiClient.apiService
        // location 변수에 따라 쿼리 파라미터 설정
        val locationParam = if (location == "all") "" else location

        apiService.getPosts(sortOrder, locationParam).enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    val posts = response.body() ?: emptyList()
                    adapter.updatePosts(posts)
                } else {
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showSortDialog() {
        val options = arrayOf("최신순", "인기순")
        AlertDialog.Builder(requireContext())
            .setTitle("정렬")
            .setItems(options) { dialog, which ->
                sortOrder = if (which == 0) "latest" else "likes" // 정렬 옵션 설정
                updateSortButtonText()
                fetchPosts()
            }
            .show()
    }
    private fun updateSortButtonText() {
        val sortText = if (sortOrder == "latest") "최신순" else "인기순"
        binding.btnSortOptions.text = sortText
    }

    private fun showProvinceDialog() {
        val provinces = arrayOf("모든 지역") + regionData.keys.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("도/시 선택")
            .setItems(provinces) { dialog, which ->
                if (which == 0) {
                    location = "all" // 모든 지역 선택
                    binding.btnLocationOptions.text = "모든 지역"
                } else {
                    val selectedProvince = provinces[which]
                    showCityDialog(selectedProvince)
                }
                fetchPosts()
            }
            .show()
    }

    private fun showCityDialog(province: String) {
        val cities = arrayOf("전체") + (regionData[province]?.toTypedArray() ?: emptyArray())

        AlertDialog.Builder(requireContext())
            .setTitle("구/군 선택")
            .setItems(cities) { dialog, which ->
                if (which == 0) {
                    location = province // 도/시 선택 상태를 유지하면서 모든 구/군을 포함
                    binding.btnLocationOptions.text = "$province 전체"
                } else {
                    val selectedCity = cities[which]
                    location = "$province $selectedCity" // 선택된 도시와 지역 설정
                    binding.btnLocationOptions.text = location
                }
                fetchPosts()
            }
            .show()
    }

    private fun openPostDetail(postId: Int) {
        val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
            putExtra("POST_ID", postId)
        }
        startActivity(intent)
    }
}
