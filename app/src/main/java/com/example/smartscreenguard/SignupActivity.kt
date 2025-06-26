package com.example.smartscreenguard

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
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

        mAuth = FirebaseAuth.getInstance()

        val btnSignup: Button = findViewById(R.id.btn_signup)
        val cbTerms: CheckBox = findViewById(R.id.cb_terms)
        val tvTerms: TextView = findViewById(R.id.tv_terms)
        val emailEditText: EditText = findViewById(R.id.et_email)
        val passwordEditText: EditText = findViewById(R.id.et_password)
        val confirmPasswordEditText: EditText = findViewById(R.id.et_confirm_password)
        val ivShowPassword: ImageView = findViewById(R.id.iv_show_password)
        val ivShowConfirmPassword: ImageView = findViewById(R.id.iv_show_confirm_password)
        progressBar = findViewById(R.id.progressBar)

        // Reusable toggle logic for both password fields
        setupPasswordToggle(passwordEditText, ivShowPassword)
        setupPasswordToggle(confirmPasswordEditText, ivShowConfirmPassword)

        btnSignup.setOnClickListener {
            if (cbTerms.isChecked) {
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                val confirmPassword = confirmPasswordEditText.text.toString().trim()

                if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                    if (password.length >= 6) {
                        if (password == confirmPassword) {
                            showLoading(true)
                            mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, onCompleteListener)
                        } else {
                            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Email, Password, and Confirm Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please agree to the Terms and Conditions", Toast.LENGTH_SHORT).show()
            }
        }

        tvTerms.setOnClickListener {
            startActivity(Intent(this, TermsAndConditionsActivity::class.java))
        }
    }

    private fun setupPasswordToggle(editText: EditText, toggleIcon: ImageView) {
        toggleIcon.setOnClickListener {
            val isPasswordVisible = editText.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            if (isPasswordVisible) {
                // Hide password
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleIcon.setImageResource(R.drawable.ic_eyeclose)
            } else {
                // Show password
                editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleIcon.setImageResource(R.drawable.ic_eye)
            }
            editText.setSelection(editText.text.length)
        }
    }

    private val onCompleteListener = OnCompleteListener<AuthResult> { task ->
        showLoading(false)
        if (task.isSuccessful) {
            val user: FirebaseUser? = mAuth.currentUser
            Toast.makeText(this, "Sign up successful! Welcome, ${user?.email}", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Sign up failed: " + task.exception?.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) ProgressBar.VISIBLE else ProgressBar.GONE
    }
}
