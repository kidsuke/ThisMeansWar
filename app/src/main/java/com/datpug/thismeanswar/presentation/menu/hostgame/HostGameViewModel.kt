package com.datpug.thismeanswar.presentation.menu.hostgame

import android.databinding.ObservableArrayList
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.databinding.ObservableInt
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
class HostGameViewModel @Inject constructor(private val multiplayerService: MultiplayerService): MVVMViewModel {

    val showLoadingSpinner: ObservableBoolean = ObservableBoolean()
    val playersLeft: ObservableField<String> = ObservableField<String>()
    val playerEntries: ObservableArrayList<Player> = ObservableArrayList()

    var navigator: MenuNavigator? = null

    private val totalPlayers = multiplayerService.totalPlayers
    private val disposables = CompositeDisposable()
    private var waitingForPlayers: Boolean = true

    override fun onCreated() {
        waitingForPlayers = multiplayerService.startHostingGame()
        setupView()
    }

    override fun onResume() {
        // Subscribe to connected players stream
        multiplayerService.getConnectedPlayer()
        .subscribeBy (
            onNext = {
                playerEntries.add(it)
                setupView()
            },
            onComplete = {
                waitingForPlayers = false
                setupView()
            },
            onError = {}
        )
        .addTo(disposables)

        // Subscribe to disconnected players stream
        multiplayerService.getDisconnectedPlayer()
        .subscribeBy (
            onNext = {
                val player: Player? = playerEntries.find { _player -> _player.playerId == it.playerId }
                if (player != null) playerEntries.remove(player)
                setupView()
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
        playersLeft.set("${playerEntries.size} / $totalPlayers")
    }
}