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
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import xyz.dvnlabs.openmusix.data.*

@KoinApiExtension
class QuickListViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    private val mediaDB: MediaDB by inject { parametersOf(application) }

    private val _recentPlay =
        fetchRecentPlay().asLiveData(viewModelScope.coroutineContext).distinctUntilChanged()
    val recentPlay: LiveData<List<MediaData>> = _recentPlay

    private fun fetchRecentPlay() = flow {
        while (true) {
            delay(500)
            val recent = mediaDB.mediaDataDAO().getLimitedMediaByRecentAdded()
            emit(recent)
        }
    }

    private val _topPlay =
        fetchTopPlay().asLiveData(viewModelScope.coroutineContext).distinctUntilChanged()
    val topPlay: LiveData<List<MediaData>> = _topPlay

    private fun fetchTopPlay() = flow {
        while (true) {
            delay(500)
            val recent = mediaDB.mediaDataDAO().getTopPlayed()
            emit(recent)
        }
    }

    private val _genre =
        fetchGenre().asLiveData(viewModelScope.coroutineContext).distinctUntilChanged()
    val genre: LiveData<List<MediaGenre>> = _genre

    private fun fetchGenre() = flow {
        while (true) {
            delay(500)
            val recent = mediaDB.mediaGenreDAO().getGenreMeta()
            emit(recent)
        }
    }

    private val _recently =
        fetchRecently().asLiveData(viewModelScope.coroutineContext).distinctUntilChanged()
    val recently: LiveData<List<MediaData>> = _recently

    private fun fetchRecently() = flow {
        while (true) {
            delay(500)
            val recent = mediaDB.mediaDataDAO().getMediaByRecentAdded()
            emit(recent)
        }
    }

    private val _artist =
        fetchArtist().asLiveData(viewModelScope.coroutineContext).distinctUntilChanged()
    val artist: LiveData<List<MediaArtist>> = _artist

    private fun fetchArtist() = flow {
        while (true) {
            delay(500)
            val recent = mediaDB.mediaArtistDAO().getAllArtist()
            emit(recent)
        }
    }

    private val _playlist =
        fetchPlaylist().asLiveData(viewModelScope.coroutineContext).distinctUntilChanged()
    val playlist: LiveData<List<MediaQueue>> = _playlist

    private fun fetchPlaylist() = flow {
        while (true) {
            delay(500)
            val recent = mediaDB.mediaQueueDAO().getQueueUser()
            emit(recent)
        }
    }
}