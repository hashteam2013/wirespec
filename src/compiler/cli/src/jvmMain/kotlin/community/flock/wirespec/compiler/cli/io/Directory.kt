package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.FullFilePath
import java.io.File

actual class Directory actual constructor(private val path: String) {
    actual fun wirespecFiles(): Set<WirespecFile> = File(path).listFiles()
        .orEmpty()
        .filter { it.isFile }
        .filter { it.extension == Extension.Wirespec.ext }
        .map { it.name }
        .map { it.dropLast(Extension.Wirespec.ext.length + 1) }
        .map { FullFilePath(path, it) }
        .map(::WirespecFile)
        .toSet()
}
