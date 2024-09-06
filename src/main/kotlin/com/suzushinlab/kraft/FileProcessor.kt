package com.suzushinlab.kraft

import java.io.File
import java.nio.file.*
import javax.imageio.ImageIO
import kotlin.io.path.extension

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

    private fun compressImage(inputPath: Path, outputDir: Path): Path {
        val image = ImageIO.read(inputPath.toFile())
        val outputFile = outputDir.resolve(inputPath.fileName.toString().replace(".", "-min."))
        Files.createDirectories(outputFile.parent)
        val writer = ImageIO.getImageWritersByFormatName(outputFile.extension).next()
        ImageIO.createImageOutputStream(outputFile.toFile()).use { ios ->
            writer.output = ios
            writer.write(null, javax.imageio.IIOImage(image, null, null), writer.defaultWriteParam.apply {
                compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                compressionQuality = 0.7f
            })
        }
        return outputFile
    }

    private fun processFiles(inputDir: Path, outputDir: Path, extensions: List<String>, processFunc: (Path, Path) -> Path) {
        Files.walk(inputDir).filter { it.toString().endsWithAny(extensions) }.forEach {
            val relativePath = inputDir.relativize(it)
            val outputPath = outputDir.resolve(relativePath).parent
            processFunc(it, outputPath)
        }
    }

    private fun processCSSFiles(inputDir: String, outputDir: String) {
        processFiles(Paths.get(inputDir), Paths.get(outputDir), listOf(".css")) { file, outputDir ->
            val outputFileName = outputDir.resolve(file.fileName.toString().replace(".css", "-min.css"))
            Files.createDirectories(outputFileName.parent)
            Files.writeString(outputFileName, minify(Files.readString(file), true))
        }
        // outputDirにある全てのminify済みのCSSを結合して1つのファイルにする
        // ファイル名はstyles-min.cssとする
        val output = File(outputDir)
        val outputFileName = File("$outputDir/styles-min.css")
        output.walk().filter { it.isFile && it.extension == "css" }.toList()
            .map { Files.readString(it.toPath()) }
            .joinToString("\n") { it }
            .let { outputFileName.writeText(it) }
    }

    private fun processJSFiles(inputDir: String, outputDir: String) {
        processFiles(Paths.get(inputDir), Paths.get(outputDir), listOf(".js")) { file, outputDir ->
            val outputFileName = outputDir.resolve(file.fileName.toString().replace(".js", "-min.js"))
            Files.createDirectories(outputFileName.parent)
            Files.writeString(outputFileName, minify(Files.readString(file), false))
        }
    }

    private fun processImageFiles(inputDir: String, outputDir: String) {
        processFiles(Paths.get(inputDir), Paths.get(outputDir), listOf(".jpg", ".jpeg", ".png"), ::compressImage)
    }

    fun compressAndCopyStaticFiles(inputDir: String, outputDir: String) {
        processJSFiles("$inputDir/js", "$outputDir/js")
        processCSSFiles("$inputDir/css", "$outputDir/css")
        processImageFiles("$inputDir/images", "$outputDir/images")
        if(File("$inputDir/.htaccess").exists()) {
            Files.copy(File("$inputDir/.htaccess").toPath(), File("$outputDir/.htaccess").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        if(File("$inputDir/robots.txt").exists()) {
            Files.copy(File("$inputDir/robots.txt").toPath(), File("$outputDir/robots.txt").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        if(File("$inputDir/ads.txt").exists()) {
            Files.copy(File("$inputDir/ads.txt").toPath(), File("$outputDir/ads.txt").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        println("静的ファイルのコピーが完了しました。")
    }
}

fun String.endsWithAny(extensions: List<String>): Boolean {
    return extensions.any { this.endsWith(it) }
}
