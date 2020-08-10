/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.rv_genre_selector.view.*
import xyz.dvnlabs.openmusix.data.MediaGenre
import xyz.dvnlabs.openmusix.ui.fragment.FragmentGenreDirections

class GenreMetaAdapter(val itemResource: Int) :
    RecyclerView.Adapter<GenreMetaAdapter.ViewHolder>() {
    private var mediaList: List<MediaGenre> = emptyList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(this.itemResource, parent, false)
        return ViewHolder(view, parent.context)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem()
    }

    fun setMediaList(media: List<MediaGenre>) {
        val diffCallback = MediaDiff(media, this.mediaList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.mediaList = media
        diffResult.dispatchUpdatesTo(this)
    }


    inner class ViewHolder(itemView: View, private val context: Context) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var media: MediaGenre? = null

        init {
            itemView.setOnClickListener(this)
        }

        fun bindItem() {
            media = mediaList[absoluteAdapterPosition]
            var name = media?.genreName
            if (name?.isBlank()!!) {
                name = "Unknown Genre"
            }
            itemView.metaText.text = name
        }

        override fun onClick(v: View?) {
            val navController = itemView.findNavController()
            media?.genreID?.let {
                val action =
                    FragmentGenreDirections.actionFragmentGenreToFragmentGenreDetail().setGenreID(
                        it
                    )
                navController.navigate(action)
            }
        }

    }

    inner class MediaDiff(
        private val newList: List<MediaGenre>,
        private val oldList: List<MediaGenre>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].genreID == newList[newItemPosition].genreID
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        @Nullable
        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            return super.getChangePayload(oldItemPosition, newItemPosition)
        }

    }

}