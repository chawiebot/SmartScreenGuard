package com.example.smartscreenguard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BlockedKeywordAdapter(
    private var keywords: MutableList<String>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<BlockedKeywordAdapter.KeywordViewHolder>() {

    inner class KeywordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val keywordText: TextView = itemView.findViewById(R.id.text_keyword)
        val deleteBtn: ImageButton = itemView.findViewById(R.id.button_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeywordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_keyword, parent, false)
        return KeywordViewHolder(view)
    }

    override fun onBindViewHolder(holder: KeywordViewHolder, position: Int) {
        val keyword = keywords[position]
        holder.keywordText.text = keyword
        holder.deleteBtn.setOnClickListener {
            onDeleteClick(keyword)
        }
    }

    override fun getItemCount(): Int = keywords.size

    fun updateList(newList: List<String>) {
        keywords = newList.toMutableList()
        notifyDataSetChanged()
    }
}
