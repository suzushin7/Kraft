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
            val outputFileName = outputDir.resolve(file.fileName.toString())
            // ファイルが存在しない場合は作成する
            if(!Files.exists(outputFileName)) {
                if(!Files.exists(outputFileName.parent)) {
                    Files.createDirectories(outputFileName.parent)
                }
                Files.copy(file, outputFileName, StandardCopyOption.REPLACE_EXISTING)
                println("CSS: ${outputFileName.pathString}を作成しました。")
            } else {
                // ファイルが存在する場合はハッシュ値を比較、異なる場合はファイルを上書きする
                val newOutputContent = Files.readString(file)
                val oldOutputContent = Files.readString(outputFileName)
                val newHash = Util.getHash(newOutputContent)
                val oldHash = Util.getHash(oldOutputContent)
                if (newHash == oldHash) {
                    println("CSS: ${outputFileName.pathString}は変更がないため、上書きしません。")
                } else {
                    Files.copy(file, outputFileName, StandardCopyOption.REPLACE_EXISTING)
                    println("CSS: ${outputFileName.pathString}を更新しました。")
                }
            }
        }
    }

    private fun processJSFiles(inputDir: String, outputDir: String) {
        processFiles(Paths.get(inputDir), Paths.get(outputDir), listOf(".js")) { file, outputDir ->
            val outputFileName = outputDir.resolve(file.fileName.toString())
            // ファイルが存在しない場合は作成する。
            // ファイルが存在する場合はハッシュ値を比較、異なる場合はファイルを上書きする
            if(!Files.exists(outputFileName)) {
                if(!Files.exists(outputFileName.parent)) {
                    Files.createDirectories(outputFileName.parent)
                }
                Files.copy(file, outputFileName, StandardCopyOption.REPLACE_EXISTING)
                println("JS: ${outputFileName.pathString}を作成しました。")
            } else {
                val newOutputContent = Files.readString(file)
                val oldOutputContent = Files.readString(outputFileName)
                if (Util.getHash(newOutputContent) == Util.getHash(oldOutputContent)) {
                    println("JS: ${outputFileName.pathString}は変更がないため、上書きしません。")
                } else {
                    Files.copy(file, outputFileName, StandardCopyOption.REPLACE_EXISTING)
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
        copyPHPFiles("$inputDir/php", "$outputDir/php")

        val textFiles = listOf("$inputDir/.htaccess", "$inputDir/robots.txt", "$inputDir/ads.txt")
        textFiles.forEach {
            if(File(it).exists()) {
                Files.copy(File(it).toPath(), File("$outputDir/${File(it).name}").toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    // PHPフォルダ内のファイルをコピーする
    fun copyPHPFiles(inputDir: String, outputDir: String) {
        processFiles(Paths.get(inputDir), Paths.get(outputDir), listOf(".php")) { file, outputDir ->
            val outputFileName = outputDir.resolve(file.fileName.toString())
            // ファイルが存在しない場合は作成する
            if(!Files.exists(outputFileName)) {
                if(!Files.exists(outputFileName.parent)) {
                    Files.createDirectories(outputFileName.parent)
                }
                Files.copy(file, outputFileName, StandardCopyOption.REPLACE_EXISTING)
                println("PHP: ${outputFileName.pathString}を作成しました。")
            } else {
                // ファイルが存在する場合はハッシュ値を比較、異なる場合はファイルを上書きする
                val newOutputContent = Files.readString(file)
                val oldOutputContent = Files.readString(outputFileName)
                val newHash = Util.getHash(newOutputContent)
                val oldHash = Util.getHash(oldOutputContent)
                if (newHash == oldHash) {
                    println("PHP: ${outputFileName.pathString}は変更がないため、上書きしません。")
                } else {
                    Files.copy(file, outputFileName, StandardCopyOption.REPLACE_EXISTING)
                    println("PHP: ${outputFileName.pathString}を更新しました。")
                }
            }
        }
    }
}

fun String.endsWithAny(extensions: List<String>): Boolean {
    return extensions.any { this.endsWith(it) }
}
