/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.fragment.bottom

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.databinding.FragmentPlayingMenuBinding
import xyz.dvnlabs.openmusix.service.OpenMusixAPI

class PlayingMenuSelector : BottomSheetDialogFragment() {
    var binding: FragmentPlayingMenuBinding? = null
    private val mediaDB: MediaDB by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPlayingMenuBinding.bind(
            inflater.inflate(
                R.layout.fragment_playing_menu,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val data = OpenMusixAPI.api?.liveDataChange?.value
        data?.let {
            lifecycleScope.launch {
                val retriever = MediaMetadataRetriever()
                try {
                    val media = mediaDB.mediaDataDAO().getMediaByID(it.currentTag as Long)
                    binding?.playingMenuTitle?.text = media.title
                    binding?.playingMenuPlayCount?.text = media.playedCount.toString()
                    binding?.playingMenuPath?.text = media.path
                    binding?.playingMenuRating?.text = media.rating.toString()
                    binding?.playingMenuYear?.text = media.year.toString()
                    retriever.setDataSource(requireContext(), Uri.parse(media.contentURI))
                    binding?.playingMenuAlbum?.text =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    binding?.playingMenuArtist?.text =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    binding?.playingMenuGenre?.text =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.close()
                }
            }
            binding?.playingMenuAddPlaylist?.setOnClickListener {
                this.dismiss()
                val fragment = FragmentAddPlaylist()
                fragment.show(requireActivity().supportFragmentManager, "AddPlaylist")
            }
        }
    }
}