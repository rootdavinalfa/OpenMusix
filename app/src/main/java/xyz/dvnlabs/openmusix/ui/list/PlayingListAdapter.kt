package xyz.dvnlabs.openmusix.ui.list

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.Player
import kotlinx.android.synthetic.main.rv_now_play.view.*
import org.greenrobot.eventbus.EventBus
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaData
import xyz.dvnlabs.openmusix.data.MediaDataDB
import xyz.dvnlabs.openmusix.event.PlayerBusState

class PlayingListAdapter(private val itemResource: Int) :
    RecyclerView.Adapter<PlayingListAdapter.ViewHolder>() {
    private var mediaDB: MediaDataDB? = null
    private var mediaList: List<MediaData> = emptyList()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlayingListAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(this.itemResource, parent, false)
        mediaDB = MediaDataDB.getDatabase(parent.context)
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

        fun bindItem() {
            media = mediaList[bindingAdapterPosition]
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
                .load(imageURL).transform(RoundedCorners(10))
                .transition(
                    DrawableTransitionOptions()
                        .crossFade()
                ).apply(
                    RequestOptions()
                        .override(600, 600)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                ).into(image)
            title.text = media!!.title
            detailText.text = detail
            val sharedPref = context.getSharedPreferences("current", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putLong("file_id", media!!.fileID)
                commit()
            }
        }

        override fun onClick(v: View?) {
            val navController = itemView.findNavController()
            navController.navigate(R.id.fragmentLibrary)
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