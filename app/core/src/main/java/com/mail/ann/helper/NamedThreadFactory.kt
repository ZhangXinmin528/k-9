package com.mail.ann.helper

import java.util.concurrent.ThreadFactory

class NamedThreadFactory(private val threadNamePrefix: String) : ThreadFactory {
    var counter: Int = 0

    override fun newThread(runnable: Runnable) = Thread(runnable, "$threadNamePrefix-${ counter++ }")
}
