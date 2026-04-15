package com.ibragimdekkushev.intervaltimer.presentation.ui.extention

import android.content.Context
import com.ibragimdekkushev.intervaltimer.R
import java.net.UnknownHostException

fun Throwable.toReadableMessage(context: Context): String = when (this) {
    is UnknownHostException -> context.getString(R.string.error_no_internet)
    is retrofit2.HttpException -> when (code()) {
        404 -> context.getString(R.string.error_not_found)
        else -> context.getString(R.string.error_server, code())
    }
    else -> context.getString(R.string.error_unknown)
}
