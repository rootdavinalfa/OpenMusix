/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.app

import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.ui.viewmodel.ListViewModel

val appModule = module {
    single {
        MediaDB.getDatabase(androidContext())
    }
    viewModel {
        ListViewModel(androidApplication())
    }
}