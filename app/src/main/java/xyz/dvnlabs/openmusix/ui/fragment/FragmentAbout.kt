/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.databinding.FragmentAboutBinding

class FragmentAbout : FragmentHost() {
    var binding: FragmentAboutBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAboutBinding.bind(
            inflater.inflate(
                R.layout.fragment_about,
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
        binding?.linkToGithub?.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rootdavinalfa/OpenMusix"))
            startActivity(intent)
        }
    }
}