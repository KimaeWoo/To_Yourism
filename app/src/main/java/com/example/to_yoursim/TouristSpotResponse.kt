package com.example.to_yoursim

data class TouristSpotResponse(
    val response: Response
)

data class Response(
    val body: Body
)

data class Body(
    val items: Items
)

data class Items(
    val item: List<TouristSpotItem>
)

data class TouristSpotItem(
    val title: String,
    val firstimage: String?,  // 이미지 URL
    val addr1: String  // 주소 또는 설명
)

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val success: Boolean, val message: String)

data class RegisterRequest(val email: String, val password: String, val nickname: String, val phone_number: String)
data class RegisterResponse(val success: Boolean, val message: String)

data class GoogleLoginRequest(val google_email: String)

data class UserDataRequest(val email: String)

data class UserDataResponse(
    val success: Boolean,
    val user: User?
)

data class User(
    val email: String?,
    val password: String?,
    val nickname: String,
    val phone_number: String,
    val profile_image: String?
)
data class UserUpdateRequest(
    val email: String,
    val newPassword: String?, // 비밀번호는 선택 사항
    val newNickname: String,
    val newPhoneNumber: String
)

data class UpdateUserResponse(
    val success: Boolean,
    val message: String?
)

//게시글
data class Post(
    val id: Int,
    val title: String,
    val content: String,
    val region: String,
    val images: List<String>,
    val author: String, // 닉네임
    val email: String, // 이메일 추가
    val likes: Int,
    val createdAt: Long
)


