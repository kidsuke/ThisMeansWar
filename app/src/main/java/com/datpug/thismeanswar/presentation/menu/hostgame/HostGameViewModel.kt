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
    // Binding fields for layout
    val showLoadingSpinner: ObservableBoolean = ObservableBoolean()
    val playersLeft: ObservableField<String> = ObservableField<String>()
    val playerEntries: ObservableArrayList<Player> = ObservableArrayList()

    // Public fields
    var navigator: MenuNavigator? = null

    // Local fields
    private val totalPlayers = multiplayerService.totalPlayers
    private val disposables = CompositeDisposable()
    private var waitingForPlayers: Boolean = true

    override fun onCreated() {
        // Subscribe to connected players stream
        multiplayerService.getPlayers()
                .subscribeBy (
                        onNext = {
                            playerEntries.clear()
                            playerEntries.addAll(it)
                            setupView()
                        },
                        onComplete = {
                            waitingForPlayers = false
                            setupView()
                        },
                        onError = {}
                )
        .addTo(disposables)
        waitingForPlayers = multiplayerService.startHostingGame()
        setupView()
    }

    override fun onResume() {

    }

    override fun onPause() {
        //if (!disposables.isDisposed) disposables.clear()
    }

    override fun setupView() {
        showLoadingSpinner.set(waitingForPlayers)
        playersLeft.set("${playerEntries.size} / $totalPlayers")
    }
}