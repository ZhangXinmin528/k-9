package com.mail.ann.storage.messages

import java.io.File
import java.util.UUID

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
fun createRandomTempDirectory(): File {
    val tempDirectory = File(System.getProperty("java.io.tmpdir", "."))
    return File(tempDirectory, UUID.randomUUID().toString()).also { it.mkdir() }
}
