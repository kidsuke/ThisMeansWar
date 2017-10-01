package com.datpug.androidcore.app.mvvm

/**
 * @author longv
 * Created on 14-Aug-17.
 */
interface MVVMView<out VM : MVVMViewModel> {
    val viewModel : VM
}