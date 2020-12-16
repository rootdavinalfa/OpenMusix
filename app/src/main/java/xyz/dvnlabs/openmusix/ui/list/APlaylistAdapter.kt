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
import kotlinx.android.synthetic.main.rv_playlist.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.data.MediaQueue
import xyz.dvnlabs.openmusix.ui.fragment.FragmentMenuDirections
import xyz.dvnlabs.openmusix.ui.fragment.FragmentPlaylistDirections

@KoinApiExtension
class APlaylistAdapter(val itemResource: Int) :
    RecyclerView.Adapter<APlaylistAdapter.ViewHolder>(), KoinComponent {
    private var mediaList: List<MediaQueue> = emptyList()
    private var mediaDB: MediaDB? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mediaDB = MediaDB.getDatabase(parent.context)
        val view = LayoutInflater.from(parent.context).inflate(this.itemResource, parent, false)
        return ViewHolder(view, parent.context)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem()
    }

    fun setMediaList(media: List<MediaQueue>) {
        val diffCallback = MediaDiff(media, this.mediaList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.mediaList = media
        diffResult.dispatchUpdatesTo(this)
    }


    inner class ViewHolder(itemView: View, private val context: Context) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var media: MediaQueue? = null

        init {
            itemView.setOnClickListener(this)
        }

        fun bindItem() = CoroutineScope(Dispatchers.Main).launch {
            media = mediaList[absoluteAdapterPosition]
            itemView.playlistName.text = media?.name
            media?.uid?.let {
                val count = mediaDB?.mediaQueueDetailDAO()?.getCountDetailByQueueID(it)
                itemView.playlistTrack.text = count.toString()
            }
        }

        override fun onClick(v: View?) {
            val navController = itemView.findNavController()
            media?.let {
                val action = when (navController.currentDestination?.id) {
                    R.id.fragmentPlaylist -> {
                        FragmentPlaylistDirections.actionFragmentPlaylistToFragmentPlaylistDetail()
                            .setQueueID(it.uid)
                    }
                    R.id.fragmentMenu -> {
                        FragmentMenuDirections.actionFragmentMenuToFragmentPlaylistDetail()
                            .setQueueID(it.uid)
                    }
                    else -> null
                }

                if (action != null) {
                    navController.navigate(action)
                }
            }

        }

    }

    inner class MediaDiff(
        private val newList: List<MediaQueue>,
        private val oldList: List<MediaQueue>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].uid == newList[newItemPosition].uid
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