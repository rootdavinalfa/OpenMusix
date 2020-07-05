package xyz.dvnlabs.openmusix.ui.list

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaData
import xyz.dvnlabs.openmusix.data.MediaDataDB
import xyz.dvnlabs.openmusix.data.MediaQueue
import xyz.dvnlabs.openmusix.data.QueueDetail

class MediaListAdapter(val itemResource: Int) :
    RecyclerView.Adapter<MediaListAdapter.ViewHolder>() {
    private var mediaDB: MediaDataDB? = null
    private var mediaList: List<MediaData> = emptyList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(this.itemResource, parent, false)
        mediaDB = MediaDataDB.getDatabase(parent.context)
        return ViewHolder(view, parent.context)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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
        val container = itemView.mediaContainer
        val title = itemView.mediaTitle
        val detailView = itemView.mediaDetail

        init {
            itemView.setOnClickListener(this)
        }

        fun bindItem() {
            media = mediaList[absoluteAdapterPosition]
            val projection = arrayOf(
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST
            )
            val selection = "${MediaStore.Audio.AudioColumns._ID} == ${media!!.fileID}"
            val query = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, null
            )
            var detail = ""
            query.use { x ->
                val albumColumn = query?.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)
                val artistColumn =
                    query?.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
                while (x!!.moveToNext()) {
                    val album = query?.getString(albumColumn!!)
                    val artist = query?.getString(artistColumn!!)
                    detail = "$artist - $album"
                }
            }

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
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
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
            detailView.text = detail
            title.text = media!!.title
        }

        override fun onClick(v: View?) {
            val navController = itemView.findNavController()
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    val sharedPref = context.getSharedPreferences("current", Context.MODE_PRIVATE)
                    withContext(Dispatchers.Main) {
                        with(sharedPref.edit()) {
                            putLong("file_id", media!!.fileID)
                            commit()
                        }
                        navController.navigate(R.id.fragmentPlayer)
                    }
                }

            }
        }

    }

    inner class MediaDiff(
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