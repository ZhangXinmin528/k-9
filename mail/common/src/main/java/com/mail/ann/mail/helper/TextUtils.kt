package com.mail.ann.mail.helper

object TextUtils {
    @JvmStatic
    fun isEmpty(text: String?) = text.isNullOrEmpty()

    @JvmStatic
    fun join(separator: String, items: Array<Any>): String {
        return items.joinToString(separator)
    }
}
