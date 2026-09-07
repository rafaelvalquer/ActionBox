package com.luminor.actionbox.ui.events

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

sealed interface UiText {
    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Dynamic(val value: String) : UiText
    fun resolve(context: Context): String = when (this) {
        is Resource -> context.getString(id, *args.toTypedArray())
        is Dynamic -> value
    }
}
class TextResources @Inject constructor(@ApplicationContext private val context: Context) {
    fun getString(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)
}
