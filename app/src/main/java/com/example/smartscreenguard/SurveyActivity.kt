package com.example.smartscreenguard

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SurveyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey)

        val radioGroup: RadioGroup = findViewById(R.id.rg_age_ranges)
        val btnSubmit: Button = findViewById(R.id.btn_submit_survey)

        btnSubmit.setOnClickListener {
            // Get the selected radio button ID
            val selectedId = radioGroup.checkedRadioButtonId

            if (selectedId == -1) {
                // No option selected
                Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show()
            } else {
                // Find the selected RadioButton
                val selectedRadioButton: RadioButton = findViewById(selectedId)
                val selectedOption = selectedRadioButton.text.toString()

                // Show a Toast or perform your next action
                Toast.makeText(this, "You selected: $selectedOption", Toast.LENGTH_SHORT).show()

                // Navigate to the next activity or save the result
                // Example: startActivity(Intent(this, NextActivity::class.java))
            }
        }
    }
}
