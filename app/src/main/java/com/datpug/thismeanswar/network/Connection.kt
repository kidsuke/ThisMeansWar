package com.datpug.thismeanswar.network

import java.io.InputStream
import java.io.OutputStream

/**
 * Created by longvu on 15/09/2017.
 */
interface Connection {
    val connectionId: String
    val inputStream: InputStream
    val outputStream: OutputStream
}