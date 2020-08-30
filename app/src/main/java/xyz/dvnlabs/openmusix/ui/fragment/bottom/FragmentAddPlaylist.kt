/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.fragment.bottom

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.data.MediaQueue
import xyz.dvnlabs.openmusix.databinding.FragmentAddPlaylistBinding
import xyz.dvnlabs.openmusix.ui.list.PlaylistAdapter
import xyz.dvnlabs.openmusix.ui.viewmodel.QuickListViewModel
import kotlin.math.hypot

class FragmentAddPlaylist : BottomSheetDialogFragment() {

    var binding: FragmentAddPlaylistBinding? = null
    private val mediaDB: MediaDB by inject()
    private val quickVM: QuickListViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddPlaylistBinding.bind(
            inflater.inflate(
                R.layout.fragment_add_playlist,
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
        val adapter = PlaylistAdapter(R.layout.rv_playlist)
        val layoutManager = LinearLayoutManager(requireContext())
        binding?.playListSelect?.layoutManager = layoutManager
        binding?.playListSelect?.adapter = adapter
        binding?.addNewPlaylist?.setOnClickListener {
            binding?.addNewPlaylist?.let { it1 -> changeVisibility(false, it1) }
            binding?.addPlaylistNewInputContainer?.let { it1 -> changeVisibility(true, it1) }
        }
        binding?.addPlaylistConfirm?.setOnClickListener {
            lifecycleScope.launch {
                //Create new queue
                binding?.addNewPlaylistName?.text?.toString()?.let { it1 ->
                    mediaDB.mediaQueueDAO().newQueue(
                        MediaQueue(
                            name = it1,
                            systemGenerated = false,
                            created = System.currentTimeMillis()
                        )
                    )
                }
                Toast.makeText(
                    requireContext(),
                    "Successfully adding new playlist!",
                    Toast.LENGTH_SHORT
                ).show()

                binding?.addPlaylistNewInputContainer?.let { it1 -> changeVisibility(false, it1) }
                binding?.addNewPlaylist?.let { it1 -> changeVisibility(true, it1) }
            }
        }

        quickVM.playlist.observe(viewLifecycleOwner, Observer {
            binding?.addPlaylistSelectContainer?.isExpanded = it.isNotEmpty()
            adapter.setMediaList(it)
        })

    }

    private fun changeVisibility(makeVisible: Boolean, view: View) {
        val cx = view.width / 2
        val cy = view.height / 2

        // get the initial radius for the clipping circle
        val radius = hypot(cx.toDouble(), cy.toDouble()).toFloat()

        // create the animation (the final radius is zero)
        val anim = if (makeVisible) {
            ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, radius)
        } else {
            ViewAnimationUtils.createCircularReveal(view, cx, cy, radius, 0f)
        }

        // make the view invisible when the animation is done
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                view.visibility = if (makeVisible) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        })

        // start the animation
        anim.start()
    }
}