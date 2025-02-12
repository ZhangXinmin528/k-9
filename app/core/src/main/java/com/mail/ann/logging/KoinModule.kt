package com.mail.ann.logging

import org.koin.dsl.module

val loggingModule = module {
    factory<ProcessExecutor> { RealProcessExecutor() }
    factory<LogFileWriter> { LogcatLogFileWriter(contentResolver = get(), processExecutor = get()) }
}
