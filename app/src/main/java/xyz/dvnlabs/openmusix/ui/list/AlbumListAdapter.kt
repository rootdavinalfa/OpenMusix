/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.list

import android.content.ContentUris
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.android.synthetic.main.rv_media.view.*
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaAlbum
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.ui.fragment.FragmentAlbumsDirections
import xyz.dvnlabs.openmusix.ui.fragment.FragmentArtistDetailDirections

class AlbumListAdapter(val itemResource: Int) :
    RecyclerView.Adapter<AlbumListAdapter.ViewHolder>() {
    private var mediaDB: MediaDB? = null
    private var mediaList: List<MediaAlbum> = emptyList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(this.itemResource, parent, false)
        mediaDB = MediaDB.getDatabase(parent.context)
        return ViewHolder(view, parent.context)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem()
    }

    fun setMediaList(media: List<MediaAlbum>) {
        val diffCallback = MediaDiff(media, this.mediaList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.mediaList = media
        diffResult.dispatchUpdatesTo(this)
    }


    inner class ViewHolder(itemView: View, private val context: Context) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var media: MediaAlbum? = null
        val container = itemView.mediaContainer
        val title = itemView.mediaTitle
        val detailView = itemView.mediaDetail

        init {
            itemView.setOnClickListener(this)
        }

        fun bindItem() {
            media = mediaList[absoluteAdapterPosition]

            val imageURL = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                media!!.albumID
            )

            Glide.with(context)
                .applyDefaultRequestOptions(
                    RequestOptions()
                        .placeholder(R.drawable.ic_song)
                        .error(R.drawable.ic_song)
                )
                .load(imageURL).transform(RoundedCorners(30))
                .transition(
                    DrawableTransitionOptions()
                        .crossFade()
                ).apply(
                    RequestOptions()
                        .override(600, 600)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                ).into(object : CustomTarget<Drawable?>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable?>?
                    ) {
                        container.background = resource
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        container.background = placeholder
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        container.background = errorDrawable
                    }

                    override fun onLoadStarted(placeholder: Drawable?) {
                        container.background = placeholder
                    }
                })
            detailView.text = media?.artistName
            title.text = media?.albumName
        }

        override fun onClick(v: View?) {
            val navController = itemView.findNavController()
            when (navController.currentDestination?.id) {
                R.id.fragmentArtistDetail -> {
                    media?.albumID?.let {
                        val action =
                            FragmentArtistDetailDirections.actionFragmentArtistDetailToFragmentAlbumDetail()
                                .setAlbumID(
                                    it
                                )
                        navController.navigate(action)
                    }
                }

                R.id.fragmentAlbums -> {
                    media?.albumID?.let {
                        val action =
                            FragmentAlbumsDirections.actionFragmentAlbumsToFragmentAlbumDetail()
                                .setAlbumID(
                                    it
                                )
                        navController.navigate(action)
                    }
                }
            }
            /*OpenMusixAPI.api?.playDefault(media)
            navController.navigate(R.id.fragmentPlayer)*/
        }

    }

    inner class MediaDiff(
        private val newList: List<MediaAlbum>,
        private val oldList: List<MediaAlbum>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].albumID == newList[newItemPosition].albumID
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