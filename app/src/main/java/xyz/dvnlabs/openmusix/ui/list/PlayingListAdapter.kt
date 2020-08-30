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
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.rv_now_play.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.data.MediaData
import xyz.dvnlabs.openmusix.ui.fragment.bottom.PlayingMenuSelector

class PlayingListAdapter(private val itemResource: Int) :
    RecyclerView.Adapter<PlayingListAdapter.ViewHolder>() {
    val retriever = MediaMetadataRetriever()
    private var mediaDB: MediaDB? = null
    private var mediaList: List<MediaData> = emptyList()
    private var lastPosition: Int = 0
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlayingListAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(this.itemResource, parent, false)
        mediaDB = MediaDB.getDatabase(parent.context)
        return ViewHolder(view, parent.context)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onBindViewHolder(holder: PlayingListAdapter.ViewHolder, position: Int) {
        holder.bindItem()
    }

    fun setMediaList(media: List<MediaData>) {
        val diffCallback = MediaDiff(media, this.mediaList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.mediaList = media
        diffResult.dispatchUpdatesTo(this)
    }


    inner class ViewHolder(itemView: View, private val context: Context) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var media: MediaData? = null
        val image = itemView.playerViewImage
        val title: TextView = itemView.playerViewTitle
        val detailText = itemView.playerViewDetail

        init {
            image.setOnClickListener(this)
        }

        fun bindItem() = CoroutineScope(Dispatchers.Main).launch {
            media = mediaList[bindingAdapterPosition]
            media?.let {
                retriever.setDataSource(context, Uri.parse(it.contentURI))
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val imageURL = retriever.embeddedPicture
                val detail = "$artist - $album"

                Glide.with(context)
                    .applyDefaultRequestOptions(
                        RequestOptions()
                            .placeholder(R.drawable.ic_song)
                            .error(R.drawable.ic_song)
                    )
                    .load(imageURL).transform(RoundedCorners(10))
                    .transition(
                        DrawableTransitionOptions()
                            .crossFade()
                    ).apply(
                        RequestOptions()
                            .override(600, 600)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                    ).into(image)
                title.text = it.title
                detailText.text = detail

                //Read rating
                val data = mediaDB?.mediaDataDAO()?.getMediaByID(it.fileID)
                when (data?.rating) {
                    3.0 -> {
                        itemView.playerViewDislike.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                R.color.white
                            )
                        )
                        itemView.playerViewLike.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                R.color.whiteShadow
                            )
                        )
                    }
                    5.0 -> {
                        itemView.playerViewDislike.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                R.color.whiteShadow
                            )
                        )
                        itemView.playerViewLike.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                R.color.white
                            )
                        )
                    }
                    else -> {
                        itemView.playerViewDislike.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                R.color.whiteShadow
                            )
                        )
                        itemView.playerViewLike.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                R.color.whiteShadow
                            )
                        )
                    }
                }
            }

            itemView.playerViewLike.setOnClickListener {
                Toast.makeText(context, "Liked", Toast.LENGTH_SHORT).show()
                media?.fileID?.let { it1 -> likeDislike(5.0, it1) }
                itemView.playerViewDislike.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.whiteShadow
                    )
                )
                itemView.playerViewLike.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.white
                    )
                )
            }
            itemView.playerViewDislike.setOnClickListener {
                Toast.makeText(context, "Disliked", Toast.LENGTH_SHORT).show()
                media?.fileID?.let { it1 -> likeDislike(3.0, it1) }
                itemView.playerViewDislike.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.white
                    )
                )
                itemView.playerViewLike.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.whiteShadow
                    )
                )
            }

            itemView.playerViewMore.setOnClickListener {
                val fragment = PlayingMenuSelector()
                val activity = context as AppCompatActivity
                fragment.show(activity.supportFragmentManager, "PlayingMenuSelector")
            }

        }

        private fun likeDislike(rating: Double, fileID: Long) =
            CoroutineScope(Dispatchers.Main).launch {
                mediaDB?.mediaDataDAO()?.changeRating(rating, fileID)
            }

        override fun onClick(v: View?) {
            val navController = itemView.findNavController()
            navController.navigate(R.id.fragmentMenu)
        }
    }

    class MediaDiff(
        private val newList: List<MediaData>,
        private val oldList: List<MediaData>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].fileID == newList[newItemPosition].fileID
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