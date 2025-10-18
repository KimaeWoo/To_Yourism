package com.example.to_yoursim

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class MyFragment : Fragment() {

    private lateinit var textViewUsername: TextView
    private lateinit var textViewNickname: TextView
    private lateinit var textViewPhone: TextView
    private lateinit var imageViewProfile: ImageView
    private lateinit var btnChangeProfileImage: Button
    private lateinit var btnChangeUserInfo: Button
    private lateinit var photoUri: Uri // URI for the photo taken

    private val getImageResult: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                uploadProfileImage(uri, null)
            }
        }

    private val takePhotoResult: ActivityResultLauncher<Uri> =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                photoUri?.let {
                    uploadProfileImage(it, null)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewUsername = view.findViewById(R.id.textViewUsername)
        textViewNickname = view.findViewById(R.id.textViewNickname)
        textViewPhone = view.findViewById(R.id.textViewPhone)
        imageViewProfile = view.findViewById(R.id.imageViewProfile)
        btnChangeProfileImage = view.findViewById(R.id.btnChangeProfileImage)
        btnChangeUserInfo = view.findViewById(R.id.btnChangeUserInfo)

        val sharedPref = requireContext().getSharedPreferences("UserPref", Activity.MODE_PRIVATE)
        val email = sharedPref.getString("username", null)
        Log.d("MyFragment", "Retrieved email: $email")

        if (email != null) {
            getUserData(email)
        } else {
            Toast.makeText(requireContext(), "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
        }

        btnChangeProfileImage.setOnClickListener {
            showBottomSheetDialog()
        }

        btnChangeUserInfo.setOnClickListener {
            showChangeUserInfoDialog()
        }

        val btnLogout: Button = view.findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()

            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun getUserData(email: String) {
        val call = ServerApiClient.apiService.getUserData(UserDataRequest(email))
        call.enqueue(object : Callback<UserDataResponse> {
            override fun onResponse(call: Call<UserDataResponse>, response: Response<UserDataResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()?.user
                    if (response.body()?.success == true && user != null) {
                        textViewUsername.text = "이메일: ${user.email}"
                        textViewNickname.text = "닉네임: ${user.nickname}"
                        textViewPhone.text = "휴대폰 번호: ${formatPhoneNumber(user.phone_number)}"

                        user.profile_image?.let { url ->
                            val fullImageUrl = "${BuildConfig.SERVER_URL}${user.profile_image}"

                            Log.d("MyFragment", "Profile image URL: $fullImageUrl")

                            Glide.with(requireContext())
                                .load(fullImageUrl)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .error(R.drawable.default_profile)
                                .placeholder(R.drawable.placeholder)
                                .into(imageViewProfile)
                        }
                    } else {
                        Toast.makeText(requireContext(), "사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserDataResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "서버와의 통신에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUserData(email: String, newNickname: String, newPhoneNumber: String, newPassword: String?) {
        val call = ServerApiClient.apiService.updateUserData(
            UserUpdateRequest(
                email = email,
                newPassword = newPassword?.takeIf { it.isNotBlank() },
                newNickname = newNickname,
                newPhoneNumber = newPhoneNumber
            )
        )
        call.enqueue(object : Callback<UpdateUserResponse> {
            override fun onResponse(call: Call<UpdateUserResponse>, response: Response<UpdateUserResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(requireContext(), "사용자 정보가 성공적으로 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "사용자 정보 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdateUserResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "서버와의 통신에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun formatPhoneNumber(phoneNumber: String): String {
        val digits = phoneNumber.replace(Regex("\\D"), "")
        return if (digits.length == 11) {
            "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
        } else {
            phoneNumber
        }
    }

    private fun showChangeUserInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_user_info, null)
        val newNicknameEditText: EditText = dialogView.findViewById(R.id.editTextNewNickname)
        val newPasswordEditText: EditText = dialogView.findViewById(R.id.editTextNewPassword)
        val newPhoneNumberEditText: EditText = dialogView.findViewById(R.id.editTextNewPhoneNumber)

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialogView.findViewById<Button>(R.id.btnChangeNickname)?.setOnClickListener {
            val newNickname = newNicknameEditText.text.toString().trim()
            if (newNickname.isNotEmpty()) {
                updateUserData(
                    email = textViewUsername.text.toString().substringAfter(": "),
                    newNickname = newNickname,
                    newPhoneNumber = textViewPhone.text.toString().substringAfter(": "),
                    newPassword = null
                )
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.btnChangePassword)?.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString().trim()
            if (newPassword.isNotEmpty()) {
                updateUserData(
                    email = textViewUsername.text.toString().substringAfter(": "),
                    newNickname = textViewNickname.text.toString().substringAfter(": "),
                    newPhoneNumber = textViewPhone.text.toString().substringAfter(": "),
                    newPassword = newPassword
                )
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.btnChangePhoneNumber)?.setOnClickListener {
            val newPhoneNumber = newPhoneNumberEditText.text.toString().trim()
            if (newPhoneNumber.isNotEmpty()) {
                updateUserData(
                    email = textViewUsername.text.toString().substringAfter(": "),
                    newNickname = textViewNickname.text.toString().substringAfter(": "),
                    newPhoneNumber = newPhoneNumber,
                    newPassword = null
                )
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "휴대폰 번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showBottomSheetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_profile_image, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialogView.findViewById<Button>(R.id.btnChooseImage)?.setOnClickListener {
            getImageResult.launch("image/*")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnTakePhoto)?.setOnClickListener {
            photoUri = Uri.fromFile(File.createTempFile("photo", ".jpg", requireContext().cacheDir))
            takePhotoResult.launch(photoUri)
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun uploadProfileImage(uri: Uri?, bitmap: Bitmap?) {
        val imageFile = if (bitmap != null) {
            val file = File(requireContext().cacheDir, "profile_image.jpg")
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
            }
            file
        } else {
            uri?.let {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val file = File(requireContext().cacheDir, "profile_image.jpg")
                FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                file
            }
        }

        imageFile?.let { file ->
            // Create RequestBody for the file
            val requestFile = file.asRequestBody("image/jpeg".toMediaType())
            val body = MultipartBody.Part.createFormData("profile-image", file.name, requestFile)

            // Create RequestBody for the email
            val sharedPref = requireContext().getSharedPreferences("UserPref", Activity.MODE_PRIVATE)
            val email = sharedPref.getString("username", null)
            val emailBody = email?.toRequestBody("text/plain".toMediaType()) ?: return

            val call = ServerApiClient.apiService.uploadProfileImage(body, emailBody)
            call.enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        val imageUrl = response.body()?.get("imageUrl")
                        imageUrl?.let { path ->
                            val fullImageUrl = "${BuildConfig.SERVER_URL}$path"
                            Glide.with(requireContext()).load(fullImageUrl).into(imageViewProfile)
                        }
                        Toast.makeText(requireContext(), "이미지 업로드 성공", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Toast.makeText(requireContext(), "서버 통신 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: Toast.makeText(requireContext(), "이미지 파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}
