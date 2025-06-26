package com.example.smartscreenguard

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BlockedKeywordManagerActivity : AppCompatActivity() {

    private lateinit var adapter: BlockedKeywordAdapter
    private lateinit var keywordSet: MutableSet<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_keyword_manager)

        val prefs = getSharedPreferences("BlockedKeywords", Context.MODE_PRIVATE)
        keywordSet = prefs.getStringSet("keywords", emptySet())?.toMutableSet() ?: mutableSetOf()

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_keywords)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = BlockedKeywordAdapter(keywordSet.toMutableList()) { keyword ->
            keywordSet.remove(keyword)
            prefs.edit().putStringSet("keywords", keywordSet).apply()
            adapter.updateList(keywordSet.toList())
            Toast.makeText(this, "\"$keyword\" removed", Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = adapter

        val editText = findViewById<EditText>(R.id.edit_text_keyword)
        val addButton = findViewById<Button>(R.id.button_add_keyword)

        addButton.setOnClickListener {
            val newKeyword = editText.text.toString().trim()
            if (newKeyword.isNotEmpty()) {
                if (keywordSet.contains(newKeyword)) {
                    Toast.makeText(this, "Keyword already exists.", Toast.LENGTH_SHORT).show()
                } else {
                    keywordSet.add(newKeyword)
                    prefs.edit().putStringSet("keywords", keywordSet).apply()
                    adapter.updateList(keywordSet.toList())
                    Toast.makeText(this, "\"$newKeyword\" added", Toast.LENGTH_SHORT).show()
                    editText.text.clear()
                }
            } else {
                Toast.makeText(this, "Please enter a keyword.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
