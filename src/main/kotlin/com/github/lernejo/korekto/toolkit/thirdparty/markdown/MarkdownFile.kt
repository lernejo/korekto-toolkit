package com.github.lernejo.korekto.toolkit.thirdparty.markdown

import org.commonmark.node.Document
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import java.io.IOException
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

class MarkdownFile(path: Path) {
    private var lineCount = 0
    private var root: Node? = null
    fun exists(): Boolean {
        return lineCount > -1
    }

    @Throws(IOException::class)
    private fun countLines(path: Path): Int {
        var lineCountAcc = 0
        val eolMatcher = EOL_REGEX.matcher(Files.readString(path))
        while (eolMatcher.find()) {
            lineCountAcc++
        }
        return lineCountAcc + 1
    }

    fun getLineCount() = lineCount

    fun getTitlesOfLevel(level: Int): List<Title> {
        val visitor = TitleVisitor(level)
        root!!.accept(visitor)
        return visitor.titles
    }

    val bulletPoints: List<String>
        get() {
            val visitor = BulletPointVisitor()
            root!!.accept(visitor)
            return visitor.items
        }
    val links: List<Link>
        get() {
            val visitor = LinkVisitor()
            root!!.accept(visitor)
            return visitor.links
        }
    val badges: List<Badge>
        get() {
            val visitor = BadgeVisitor()
            root!!.accept(visitor)
            return visitor.badges
        }

    companion object {
        private val PARSER = Parser.builder().build()
        private val EOL_REGEX = Pattern.compile("\r?\n")
    }

    init {
        if (!Files.exists(path)) {
            lineCount = -1
            root = Document()
        } else {
            try {
                Files.newInputStream(path).use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        root = PARSER.parseReader(reader)
                        lineCount = countLines(path)
                    }
                }
            } catch (e: IOException) {
                throw UncheckedIOException("Unable to read $path", e)
            }
        }
    }
}
