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
    val totalPlayers = multiplayerService.totalPlayers

    private val disposables = CompositeDisposable()
    private var waitingForPlayers: Boolean = true

    override fun onCreated() {
        multiplayerService.startHostingGame()
        setupView()
    }

    override fun onResume() {
        // Subscribe to connected players stream
        multiplayerService.getConnectedPlayers()
        .subscribeBy (
            onNext = { playerEntries.add(it) },
            onComplete = {
                waitingForPlayers = false
                setupView()
            },
            onError = {}
        )
        .addTo(disposables)

        // Subscribe to disconnected players stream
        multiplayerService.getDisconnectedPlayers()
        .subscribeBy (
            onNext = {
                val player: Player? = playerEntries.find { _player -> _player.playerId == it.playerId }
                if (player != null) playerEntries.remove(player)
            },
            onComplete = {
                waitingForPlayers = false
                setupView()
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