package com.example.to_yoursim

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Path

interface TouristApiService {
    @GET("areaBasedList1")
    fun getTouristSpots(
        @Query("ServiceKey") serviceKey: String,
        @Query("contentTypeId") contentTypeId: Int = 12,
        @Query("areaCode") areaCode: Int,
        @Query("numOfRows") numOfRows: Int = 10,
        @Query("pageNo") pageNo: Int = 1,
        @Query("MobileOS") mobileOS: String = "ETC",
        @Query("MobileApp") mobileApp: String = "To yourism",
        @Query("_type") type: String = "json"
    ): Call<TouristSpotResponse>
}
interface ApiService {
    @POST("login")
    fun loginUser(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @POST("register")
    fun registerUser(@Body registerRequest: RegisterRequest): Call<RegisterResponse>

    @POST("google-login")
    fun googleLoginUser(@Body googleLoginRequest: GoogleLoginRequest): Call<Void>

    @POST("get-user-data")
    fun getUserData(@Body userDataRequest: UserDataRequest): Call<UserDataResponse>

    @POST("update-user-data")
    fun updateUserData(@Body userUpdateRequest: UserUpdateRequest): Call<UpdateUserResponse>

    @GET("posts")
    fun getPosts(
        @Query("sort") sort: String,
        @Query("location") location: String
    ): Call<List<Post>>

    @Multipart
    @POST("upload-image")
    fun uploadImage(@Part image: MultipartBody.Part): Call<Map<String, String>>

    @Multipart
    @POST("upload-profile-image")
    fun uploadProfileImage(
        @Part profileImage: MultipartBody.Part,
        @Part("email") email: RequestBody
    ): Call<Map<String, String>>

    @POST("create-post")
    fun createPost(@Body post: Post): Call<Void>

    @GET("posts/{id}")
    fun getPostById(@Path("id") id: Int): Call<Post>

    @POST("/posts/{id}/like")
    fun likePost(@Path("id") postId: Int): Call<Void>

}
