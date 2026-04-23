package com.borderline.feature.overlay.panels

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.borderline.core.models.AccessibilityContent
import com.borderline.feature.overlay.dp

class SmartContentAdapter(
    private val items: MutableList<AccessibilityContent>,
    private val onItemClick: (AccessibilityContent) -> Unit,
    private val onItemLongPress: (AccessibilityContent) -> Unit,
    private val isEditMode: () -> Boolean
) : RecyclerView.Adapter<SmartContentAdapter.ViewHolder>() {

    class ViewHolder(
        itemView: View,
        val title: TextView,
        val subtitle: TextView
    ) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context

        val title = TextView(context).apply {
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
        }

        val subtitle = TextView(context).apply {
            textSize = 12f
            maxLines = 1
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dp, 10.dp, 12.dp, 10.dp)
            addView(title)
            addView(subtitle)
        }

        return ViewHolder(layout, title, subtitle)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.label
        holder.subtitle.text = item.javaClass.simpleName + if (isEditMode()) " • Edit" else ""
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener {
            onItemLongPress(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun setEditMode(editMode: Boolean) {
        notifyDataSetChanged()
    }

    fun submitItems(newItems: List<AccessibilityContent>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
