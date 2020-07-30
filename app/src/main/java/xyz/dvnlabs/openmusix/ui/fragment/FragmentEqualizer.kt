/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.databinding.FragmentEqualizerBinding

class FragmentEqualizer : FragmentHost() {
    private var binding: FragmentEqualizerBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEqualizerBinding.bind(
            inflater.inflate(
                R.layout.fragment_equalizer,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}