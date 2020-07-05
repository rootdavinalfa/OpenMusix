package xyz.dvnlabs.openmusix.app

import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.dvnlabs.openmusix.data.MediaDataDB
import xyz.dvnlabs.openmusix.ui.viewmodel.ListViewModel

val appModule = module {
    single {
        MediaDataDB.getDatabase(androidContext())
    }
    viewModel {
        ListViewModel(androidApplication())
    }
}