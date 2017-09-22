package com.datpug.thismeanswar.presentation.menu

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.datpug.androidcore.app.mvvm.MVVMFragment
import com.datpug.thismeanswar.databinding.FragmentMenuBinding
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

/**
 * Created by longvu on 16/09/2017.
 */
class MenuFragment: MVVMFragment<MenuActivity, MenuViewModel>() {
    override lateinit var viewModel: MenuViewModel
        @Inject set

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = MenuViewModel()
        val binding: FragmentMenuBinding = FragmentMenuBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.viewModel.navigator = hostActivity
        return binding.root
    }

    override fun initView() {}
}