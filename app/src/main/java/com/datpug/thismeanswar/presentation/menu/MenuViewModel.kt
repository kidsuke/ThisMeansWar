package com.datpug.thismeanswar.presentation.menu

import com.datpug.androidcore.app.mvvm.MVVMViewModel

/**
 * Created by longvu on 16/09/2017.
 */
class MenuViewModel: MVVMViewModel {

    var navigator: MenuNavigator? = null

    override fun onCreated() {}

    override fun onResume() {}

    override fun onPause() {}

    override fun setupView() {}

    fun hostGame() {
        navigator?.showHostGameFragment()
    }

    fun findGame() {
        navigator?.showFindGameFragment()
    }

    fun quitGame() {
        navigator?.exitApplication()
    }
}