package com.example.smartscreenguard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val btnSignIn: Button = findViewById(R.id.btn_sign_in)
        val tvSignUp: TextView = findViewById(R.id.tv_sign_up)
        val etEmail: EditText = findViewById(R.id.et_email)
        val etPassword: EditText = findViewById(R.id.et_password)

        // Show/hide password logic
        val ivShowPassword: ImageView = findViewById(R.id.iv_show_password)
        ivShowPassword.setOnClickListener {
            if (etPassword.inputType == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivShowPassword.setImageResource(R.drawable.ic_eyeclose) // Update with the "closed eye" icon
            } else {
                etPassword.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivShowPassword.setImageResource(R.drawable.ic_eye) // Update with the "open eye" icon
            }
            etPassword.setSelection(etPassword.text.length) // Move cursor to the end
        }

        // Sign in button logic
        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Proceed with Firebase authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, OnCompleteListener<AuthResult> { task ->
                    if (task.isSuccessful) {
                        // Sign in success, go to HomePageActivity
                        val user = auth.currentUser
                        Toast.makeText(this, "Welcome back, ${user?.email}", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomePageActivity::class.java))
                        finish() // Close the LoginActivity
                    } else {
                        // If sign in fails, display a message to the user
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // Navigate to Sign Up screen
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
