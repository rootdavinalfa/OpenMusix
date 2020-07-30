/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import xyz.dvnlabs.openmusix.data.MediaAlbum
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.data.MediaData

class ListViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    private val mediaDB: MediaDB by inject { parametersOf(application) }

    private var _listMedia =
        fetchMedia().asLiveData(viewModelScope.coroutineContext).distinctUntilChanged()
    val listMedia: LiveData<List<MediaData>> = _listMedia
    val currentFileID: MutableLiveData<Long> = MutableLiveData(-1)


    fun changeFileID(fileID: Long) {
        currentFileID.value = fileID
    }


    private fun fetchMedia() = flow {
        while (true) {
            delay(500)
            val data = mediaDB.mediaDataDAO().getMedia()
            emit(data)
        }
    }

    private val _albumsList =
        fetchAlbumsList().asLiveData(viewModelScope.coroutineContext).distinctUntilChanged()
    val albumsList: LiveData<List<MediaAlbum>> = _albumsList

    private fun fetchAlbumsList() = flow {
        while (true) {
            delay(500)
            val recent = mediaDB.mediaAlbumDAO().getAllAlbum()
            emit(recent)
        }
    }
}