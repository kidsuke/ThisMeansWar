package com.datpug.thismeanswar.presentation.menu.hostgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.datpug.androidcore.app.mvvm.MVVMFragment
import com.datpug.thismeanswar.databinding.FragmentHostGameBinding
import com.datpug.thismeanswar.presentation.menu.MenuActivity
import javax.inject.Inject

/**
 * Created by longvu on 16/09/2017.
 */
class HostGameFragment: MVVMFragment<MenuActivity, HostGameViewModel>() {
    override lateinit var viewModel: HostGameViewModel
        @Inject set

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentHostGameBinding = FragmentHostGameBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.viewModel.navigator = hostActivity
        return binding.root
    }

    override fun initView() {}
}