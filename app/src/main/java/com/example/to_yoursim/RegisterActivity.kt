package com.example.to_yoursim

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private val apiService: ApiService by lazy { ServerApiClient.apiService }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val confirmPasswordEditText = findViewById<EditText>(R.id.editTextConfirmPassword)
        val nicknameEditText = findViewById<EditText>(R.id.editTextNickname)
        val phoneEditText = findViewById<EditText>(R.id.editTextPhone)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()
            val nickname = nicknameEditText.text.toString()
            val phone = phoneEditText.text.toString()

            if (password != confirmPassword) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val registerRequest = RegisterRequest(email, password, nickname, phone)

            apiService.registerUser(registerRequest).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    Log.d("RegisterActivity", "Response Code: ${response.code()}")
                    Log.d("RegisterActivity", "Response Body: ${response.body()}")
                    Log.d("RegisterActivity", "Response Message: ${response.message()}")

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.success) {
                            Toast.makeText(this@RegisterActivity, "회원가입 성공", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, "회원가입 실패: ${body?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@RegisterActivity, "회원가입 실패: 서버 응답 오류", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Log.e("RegisterActivity", "Network Error: ${t.message}", t)
                    Toast.makeText(this@RegisterActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
