package com.example.to_yoursim

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.to_yoursim.ServerApiClient.apiService
import com.example.to_yoursim.databinding.ActivityCreatePostBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private val images = mutableListOf<String>()
    private lateinit var imageAdapter: ImageAdapter
    private var selectedRegion: String? = null
    private var userNickname: String? = null

    // 도/시/군 데이터를 포함
    private val regionData = mapOf(
        "서울특별시" to listOf("강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"),
        "부산광역시" to listOf("강서구", "금정구", "기장군", "동구", "부산진구", "사상구", "사하구", "서구", "수영구", "해운대구", "중구", "남구", "북구", "영도구", "연제구"),
        "대구광역시" to listOf("중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군"),
        "인천광역시" to listOf("남동구", "부평구", "서구", "연수구", "중구", "동구", "강화군", "옹진군"),
        "광주광역시" to listOf("동구", "서구", "남구", "북구", "광산구"),
        "대전광역시" to listOf("동구", "중구", "서구", "유성구", "대덕구"),
        "울산광역시" to listOf("남구", "동구", "중구", "북구", "울주군"),
        "세종특별자치시" to listOf("세종시"),
        "경기도" to listOf("수원시", "용인시", "성남시", "화성시", "안양시", "안산시", "평택시", "광명시", "오산시", "이천시", "여주군", "군포시", "포천시", "양평군", "남양주시", "부천시", "구리시", "하남시"),
        "강원특별자치도" to listOf("춘천시", "강릉시", "동해시", "속초시", "삼척시", "원주", "홍천군", "횡성군", "영월군", "평창군", "정선군", "철원군", "화천군", "양구군", "인제군", "양양군"),
        "충청북도" to listOf("청주시", "충주시", "제천시", "옥천군", "보은군", "영동군", "진천군", "음성군", "괴산군", "단양군"),
        "충청남도" to listOf("천안시", "공주시", "보령시", "아산시", "서산시", "홍성군", "논산시", "계룡시", "당진시", "서천군"),
        "전라북도" to listOf("전주시", "익산시", "군산시", "정읍시", "남원시", "완주군", "진안군", "무주군", "장수군", "임실군", "순창군", "고창군", "부안군"),
        "전라남도" to listOf("목포시", "여수시", "순천시", "광양시", "나주시", "무안군", "해남군", "완도군", "진도군", "장흥군", "강진군", "영암군", "구례군", "담양군", "함평군", "영광군", "신안군"),
        "경상북도" to listOf("포항시", "경산시", "구미시", "안동시", "영천시", "김천시", "상주시", "문경시", "예천군", "봉화군", "울진군", "울릉군"),
        "경상남도" to listOf("창원시", "김해시", "양산시", "진주시", "밀양시", "거제시", "함안군", "함양군", "하동군", "산청군", "의령군", "창녕군", "고성군"),
        "제주특별자치도" to listOf("제주시", "서귀포시")
    )

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleImageUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageAdapter = ImageAdapter(images)
        binding.viewPager.adapter = imageAdapter

        binding.btnSelectRegion.setOnClickListener {
            showProvinceDialog()
        }

        binding.btnAddPhoto.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnSubmit.setOnClickListener {
            submitPost()
        }
    }
    private fun submitPost() {
        if (selectedRegion.isNullOrEmpty()) {
            Toast.makeText(this, "지역을 선택해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("UserPref", MODE_PRIVATE)
        val email = sharedPref.getString("username", null)

        if (email == null) {
            Toast.makeText(this, "로그인 상태를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val post = Post(
            id = 0, // 서버에서 생성된 ID로 대체될 것입니다.
            title = binding.etTitle.text.toString(),
            content = binding.etContent.text.toString(),
            region = selectedRegion ?: "",
            images = images,
            author = "", // 닉네임은 서버에서 처리됩니다.
            email = email, // 이메일 추가
            likes = 0, // 초기값으로 설정
            createdAt = System.currentTimeMillis()
        )

        ServerApiClient.apiService.createPost(post).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CreatePostActivity, "게시글이 성공적으로 생성되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CreatePostActivity, "게시글 생성에 실패했습니다. 상태 코드: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@CreatePostActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showProvinceDialog() {
        val provinces = arrayOf("모든 지역") + regionData.keys.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("도/시 선택")
            .setItems(provinces) { _, which ->
                if (which == 0) {
                    selectedRegion = null
                    binding.btnSelectRegion.text = "모든 지역"
                } else {
                    val selectedProvince = provinces[which]
                    showCityDialog(selectedProvince)
                }
            }
            .show()
    }

    private fun showCityDialog(province: String) {
        val cities = arrayOf("전체") + (regionData[province]?.toTypedArray() ?: emptyArray())

        AlertDialog.Builder(this)
            .setTitle("구/군 선택")
            .setItems(cities) { _, which ->
                selectedRegion = if (which == 0) {
                    "$province 전체"
                } else {
                    "$province ${cities[which]}"
                }
                binding.btnSelectRegion.text = selectedRegion
            }
            .show()
    }

    private fun uploadImageFromUri(uri: Uri) {
        val contentResolver = contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".jpg", cacheDir)

        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)

        ServerApiClient.apiService.uploadImage(body).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    val imageUrl = response.body()?.get("imageUrl") ?: ""
                    images.add(imageUrl)
                    imageAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@CreatePostActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    Log.e("ImageUpload", "Upload failed with code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Toast.makeText(this@CreatePostActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("ImageUpload", "Network error", t)
            }
        })
    }

    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun handleImageUri(uri: Uri) {
        if (images.size < 5) {
            uploadImageFromUri(uri)
        } else {
            Toast.makeText(this, "You can only add up to 5 images.", Toast.LENGTH_SHORT).show()
        }
    }
}
