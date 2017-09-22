package com.datpug.thismeanswar.presentation.menu

import android.os.Bundle
import com.datpug.androidcore.app.AbstractActivity
import com.datpug.thismeanswar.R
import com.datpug.thismeanswar.presentation.menu.hostgame.HostGameFragment

class MenuActivity : AbstractActivity(), MenuNavigator {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        showMenuFragment()
    }

    override fun showMenuFragment() {
        showFragment(MenuFragment::class)
    }

    override fun showHostGameFragment() {
        showFragment(HostGameFragment::class)
    }

    override fun showFindGameFragment() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exitApplication() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
