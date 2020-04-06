package com.example.video.util

import android.content.Context
import java.lang.StringBuilder

class TextResourceReader {
    companion object {
        fun readTextFileFromResource(context:Context, resId:Int): String {
            val inputStream = context.resources.openRawResource(resId)
            val result = StringBuilder()
            inputStream.bufferedReader().useLines { lines -> lines.forEach {
                result.append(it)
                result.append("\n")
            } }
            return result.toString()
        }
    }
}