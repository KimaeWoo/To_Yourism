package com.example.to_yoursim

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    // ServerApiClient를 사용하도록 수정
    private val apiService: ApiService by lazy {
        ServerApiClient.apiService
    }

    // ActivityResultLauncher 정의
    private val googleSignInLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SharedPreferences에서 로그인 정보 확인
        val sharedPref = getSharedPreferences("UserPref", MODE_PRIVATE)
        val savedUsername = sharedPref.getString("username", null)

        if (savedUsername != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish() // MainActivity 종료
            return
        }
        val textViewAppName: TextView = findViewById(R.id.textViewAppName)

        val text = "To Yourism"
        val spannableString = SpannableString(text)

        // "To"를 파란색으로 설정
        spannableString.setSpan(
            ForegroundColorSpan(Color.BLUE),
            0, 2,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // "Yourism"을 검정색으로 설정
        spannableString.setSpan(
            ForegroundColorSpan(Color.BLACK),
            3, text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textViewAppName.text = spannableString
        // EditText와 Button 참조하기
        val usernameEditText = findViewById<EditText>(R.id.editTextId)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val googleSignInButton = findViewById<SignInButton>(R.id.btnGoogleSignIn)

        // 로그인 버튼 클릭 시
        btnLogin.setOnClickListener {
            val email = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            val loginRequest = LoginRequest(email, password)
            apiService.loginUser(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val contentType = response.headers()["Content-Type"]
                        Log.d("ResponseHeaders", "Content-Type: $contentType")

                        if (contentType?.startsWith("application/json") == true) {
                            val loginResponse = response.body()
                            if (loginResponse?.success == true) {
                                // 로그인 성공 처리
                                val sharedPref = getSharedPreferences("UserPref", MODE_PRIVATE)
                                val editor = sharedPref.edit()
                                editor.putString("username", email)
                                editor.apply()

                                Toast.makeText(this@MainActivity, "로그인 성공", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                                finish()
                            } else {
                                // 로그인 실패 처리
                                Toast.makeText(this@MainActivity, "로그인 실패: ${loginResponse?.message}", Toast.LENGTH_SHORT).show()
                                Log.d("LoginError", "Response message: ${loginResponse?.message}")
                            }
                        } else {
                            // JSON 응답이 아닌 경우 처리
                            Toast.makeText(this@MainActivity, "응답 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 서버 에러 처리
                        val errorBody = response.errorBody()?.string()
                        Log.d("LoginError", "HTTP error: ${response.message()} - $errorBody")
                        Toast.makeText(this@MainActivity, "로그인 실패: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // 회원가입 버튼 클릭 시
        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Google Sign-In 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // 구글 로그인 버튼 클릭 시
        googleSignInButton.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val userEmail = account?.email ?: return

            val googleLoginRequest = GoogleLoginRequest(userEmail)
            apiService.googleLoginUser(googleLoginRequest).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        val sharedPref = getSharedPreferences("UserPref", MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("username", userEmail)
                        editor.apply()

                        Toast.makeText(this@MainActivity, "Google 계정으로 로그인 성공", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@MainActivity, "Google 로그인 실패: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: ApiException) {
            Toast.makeText(this, "로그인 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            e.printStackTrace() // 스택 트레이스 출력
        }
    }
}
