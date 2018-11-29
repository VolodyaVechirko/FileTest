package com.vvechirko.filetest.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.vvechirko.filetest.data.Photo
import com.vvechirko.filetest.R

class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

    val data = mutableListOf<Photo>()
    var interaction: Interaction? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = data.get(position)
        holder.itemView.setOnClickListener { interaction?.open(item) }
        holder.btnDownload.setOnClickListener { interaction?.download(item) }

        Glide.with(holder.imgThumb)
            .load(item.thumbnailUrl)
            .into(holder.imgThumb)
    }

    override fun getItemCount(): Int = data.size

    fun setData(list: List<Photo>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumb: ImageView = view.findViewById(R.id.imgThumb)
        val btnDownload: ImageView = view.findViewById(R.id.btnDownload)
    }

    interface Interaction {
        fun download(photo: Photo)

        fun open(photo: Photo)
    }
}