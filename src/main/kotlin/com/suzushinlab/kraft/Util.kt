package com.suzushinlab.kraft

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

object Util {
    fun getHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val fileBytes = Files.readAllBytes(file.toPath())
        val hashBytes = digest.digest(fileBytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun getHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}