package com.datpug.thismeanswar.presentation.menu.hostgame

import android.databinding.ObservableArrayList
import android.databinding.ObservableBoolean
import com.datpug.androidcore.app.mvvm.MVVMViewModel
import com.datpug.thismeanswar.model.Player
import com.datpug.thismeanswar.multiplayer.MultiplayerService
import com.datpug.thismeanswar.presentation.menu.MenuNavigator
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

/**
 * Created by longvu on 16/09/2017.
 */
class HostGameViewModel @Inject constructor(val multiplayerService: MultiplayerService): MVVMViewModel {

    val showLoadingSpinner: ObservableBoolean = ObservableBoolean()
    val playerEntries: ObservableArrayList<Player> = ObservableArrayList()

    var navigator: MenuNavigator? = null

    private var players: List<Player> = listOf()
    private val disposables = CompositeDisposable()
    private var waitingForPlayers: Boolean = true

    override fun onCreated() {
        multiplayerService.startHosting()

        setupView()val i = com.datpug.thismeanswar.BR.data
    }

    override fun onResume() {
        multiplayerService.getPlayers()
        .subscribeBy (
            onNext = {
                playerEntries.add(it)
            },
            onComplete = {

            },
            onError = {}
        )
        .addTo(disposables)
    }

    override fun onPause() {
        if (!disposables.isDisposed) disposables.clear()
    }

    override fun setupView() {
        showLoadingSpinner.set(waitingForPlayers)
    }
}