package com.suzushinlab.kraft

import java.io.File
import java.nio.file.*
import javax.imageio.ImageIO
import kotlin.io.path.extension
import kotlin.io.path.pathString

/**
 * FileProcessorクラス
 */
class FileProcessor {
    private fun minify(content: String, isCss: Boolean): String {
        return if (isCss) {
            content.replace("/\\*.*?\\*/".toRegex(), "")  // コメント削除
                .replace("\\s*(?=[{};:,])".toRegex(), "")  // { } ; : , の前のスペース削除
                .replace("(?<=[{};:,])\\s+".toRegex(), "")  // { } ; : , の後のスペース削除
                .replace("\\s+([>+~])\\s+".toRegex(), "$1")  // > + ~ の周りのスペース削除
                .replace("\\s+".toRegex(), " ")  // 余分なスペースを1つに縮小
                .replace("[\\n\\r]".toRegex(), "")  // 改行削除
                .trim()
        } else {
            content.replace("//.*".toRegex(), "")
                .replace("/\\*.*?\\*/".toRegex(), "")
                .replace("\\s+".toRegex(), " ").trim()
        }
    }

    private fun compressImage(inputPath: Path, outputDir: Path) {
        val image = ImageIO.read(inputPath.toFile())
        val outputFile = outputDir.resolve(inputPath.fileName.toString().replace(".", "-min."))
        if(!Files.exists(outputFile.parent)) {
            Files.createDirectories(outputFile.parent)
        }
        // ファイルが存在しない場合は作成する
        if(!Files.exists(outputFile)) {
            val writer = ImageIO.getImageWritersByFormatName(outputFile.extension).next()
            ImageIO.createImageOutputStream(outputFile.toFile()).use { ios ->
                writer.output = ios
                writer.write(null, javax.imageio.IIOImage(image, null, null), writer.defaultWriteParam.apply {
                    compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = 0.7f
                })
            }
            println("IMAGE: ${outputFile.pathString}を作成しました。")
        } else {
            // 既にファイルが存在する場合はハッシュ値を比較。
            // ハッシュ値が異なる場合はファイルを上書きする
            val tmp = Files.createTempFile("tmp", ".${outputFile.extension}")
            val writer = ImageIO.getImageWritersByFormatName(outputFile.extension).next()
            ImageIO.createImageOutputStream(tmp.toFile()).use { ios ->
                writer.output = ios
                writer.write(null, javax.imageio.IIOImage(image, null, null), writer.defaultWriteParam.apply {
                    compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = 0.7f
                })
            }
            val newHash = Util.getHash(tmp.toFile())
            val oldHash = Util.getHash(outputFile.toFile())
            if (newHash == oldHash) {
                println("IMAGE: ${outputFile.pathString}は変更がないため、上書きしません。")
            } else {
                Files.copy(tmp, outputFile, StandardCopyOption.REPLACE_EXISTING)
                println("IMAGE: ${outputFile.pathString}を更新しました。")
            }
        }
    }

    private fun processFiles(inputDir: Path, outputDir: Path, extensions: List<String>, processFunc: (Path, Path) -> Unit) {
        Files.walk(inputDir).filter { it.toString().endsWithAny(extensions) }.forEach {
            val relativePath = inputDir.relativize(it)
            val outputPath = outputDir.resolve(relativePath).parent
            processFunc(it, outputPath)
        }
    }

    private fun processCSSFiles(inputDir: String, outputDir: String) {
        processFiles(Paths.get(inputDir), Paths.get(outputDir), listOf(".css")) { file, outputDir ->
            val outputFileName = outputDir.resolve(file.fileName.toString().replace(".css", "-min.css"))
            // ファイルが存在しない場合は作成する
            if(!Files.exists(outputFileName)) {
                if(!Files.exists(outputFileName.parent)) {
                    Files.createDirectories(outputFileName.parent)
                }
                Files.writeString(outputFileName, minify(Files.readString(file), true))
                println("CSS: ${outputFileName.pathString}を作成しました。")
            } else {
                // ファイルが存在する場合はハッシュ値を比較、異なる場合はファイルを上書きする
                val newOutputContent = minify(Files.readString(file), true)
                val oldOutputContent = Files.readString(outputFileName)
                val newHash = Util.getHash(newOutputContent)
                val oldHash = Util.getHash(oldOutputContent)
                if (newHash == oldHash) {
                    println("CSS: ${outputFileName.pathString}は変更がないため、上書きしません。")
                } else {
                    Files.writeString(outputFileName, newOutputContent)
                    println("CSS: ${outputFileName.pathString}を更新しました。")
                }
            }
        }
        // outputDirにある全てのminify済みのCSSを結合して1つのファイルにする
        // ファイル名はstyles-min.cssとする
        val output = File(outputDir)
        val outputFileName = File("$outputDir/styles-min.css")

        // ファイルが存在する場合はハッシュ値を比較、異なる場合はファイルを上書きする
        if (outputFileName.exists()) {
            val newOutputContent = output.walk().filter { it.isFile && it.extension == "css" && !it.name.equals(outputFileName.name) }.toList()
                .map { Files.readString(it.toPath()).trim() }
                .joinToString("\n") { it }
            val oldOutputContent = Files.readString(outputFileName.toPath())
            val newHash = Util.getHash(newOutputContent)
            val oldHash = Util.getHash(oldOutputContent)
            if (newHash == oldHash) {
                println("CSS: ${outputFileName.path}は変更がないため、上書きしません。")
            } else {
                Files.writeString(outputFileName.toPath(), newOutputContent)
                println("CSS: ${outputFileName.path}を更新しました。")
            }
        } else {
            // ファイルが存在しない場合は作成する
            val newOutputContent = output.walk().filter { it.isFile && it.extension == "css" && !it.name.equals(outputFileName.name) }.toList()
                .map { Files.readString(it.toPath()) }
                .joinToString("\n") { it }
            Files.writeString(outputFileName.toPath(), newOutputContent)
            println("CSS: ${outputFileName.path}を作成しました。")
        }
    }

    private fun processJSFiles(inputDir: String, outputDir: String) {
        processFiles(Paths.get(inputDir), Paths.get(outputDir), listOf(".js")) { file, outputDir ->
            val outputFileName = outputDir.resolve(file.fileName.toString().replace(".js", "-min.js"))
            // ファイルが存在しない場合は作成する。
            // ファイルが存在する場合はハッシュ値を比較、異なる場合はファイルを上書きする
            if(!Files.exists(outputFileName)) {
                if(!Files.exists(outputFileName.parent)) {
                    Files.createDirectories(outputFileName.parent)
                }
                Files.writeString(outputFileName, minify(Files.readString(file), false))
                println("JS: ${outputFileName.pathString}を作成しました。")
            } else {
                val newOutputContent = minify(Files.readString(file), false)
                val oldOutputContent = Files.readString(outputFileName)
                if (newOutputContent.hashCode() == oldOutputContent.hashCode()) {
                    println("JS: ${outputFileName.pathString}は変更がないため、上書きしません。")
                } else {
                    Files.writeString(outputFileName, newOutputContent)
                    println("JS: ${outputFileName.pathString}を更新しました。")
                }
            }
        }
    }

    private fun processImageFiles(inputDir: String, outputDir: String) {
        processFiles(Paths.get(inputDir), Paths.get(outputDir), listOf(".jpg", ".jpeg", ".png"), ::compressImage)
    }

    fun compressAndCopyStaticFiles(inputDir: String, outputDir: String) {
        processJSFiles("$inputDir/js", "$outputDir/js")
        processCSSFiles("$inputDir/css", "$outputDir/css")
        processImageFiles("$inputDir/images", "$outputDir/images")

        val textFiles = listOf("$inputDir/.htaccess", "$inputDir/robots.txt", "$inputDir/ads.txt")
        textFiles.forEach {
            if(File(it).exists()) {
                Files.copy(File(it).toPath(), File("$outputDir/${File(it).name}").toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}

fun String.endsWithAny(extensions: List<String>): Boolean {
    return extensions.any { this.endsWith(it) }
}
