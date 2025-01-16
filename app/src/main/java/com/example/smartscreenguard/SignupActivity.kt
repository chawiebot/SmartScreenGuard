package com.example.smartscreenguard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignupActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance()

        // Cache views
        val btnSignup: Button = findViewById(R.id.btn_signup)
        val cbTerms: CheckBox = findViewById(R.id.cb_terms)
        val tvTerms: TextView = findViewById(R.id.tv_terms)
        val emailEditText: EditText = findViewById(R.id.et_email)
        val passwordEditText: EditText = findViewById(R.id.et_password)
        progressBar = findViewById(R.id.progressBar) // Assuming you have a ProgressBar in your layout

        btnSignup.setOnClickListener {
            if (cbTerms.isChecked) {
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    if (password.length >= 6) { // Basic password validation
                        showLoading(true)
                        mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this, onCompleteListener)
                    } else {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please agree to the Terms and Conditions", Toast.LENGTH_SHORT).show()
            }
        }

        tvTerms.setOnClickListener {
            // Open Terms and Conditions activity
            startActivity(Intent(this, TermsAndConditionsActivity::class.java))
        }
    }

    private val onCompleteListener = OnCompleteListener<AuthResult> { task ->
        showLoading(false) // Hide loading when task completes
        if (task.isSuccessful) {
            // Successfully signed up
            val user: FirebaseUser? = mAuth.currentUser
            Toast.makeText(this, "Sign up successful! Welcome, ${user?.email}", Toast.LENGTH_SHORT).show()

            // Redirect to another activity (e.g., MainActivity)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()  // Close the SignupActivity
        } else {
            // Sign up failed
            val e = task.exception
            Toast.makeText(this, "Sign up failed: " + e?.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) ProgressBar.VISIBLE else ProgressBar.GONE
    }
}
