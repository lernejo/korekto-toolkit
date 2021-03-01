package com.github.lernejo.korekto.toolkit.thirdparty.markdown

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Heading
import org.commonmark.node.ListItem
import org.commonmark.node.Text
import java.util.*

internal class BulletPointVisitor : AbstractVisitor() {
    @JvmField
    val items: MutableList<String> = ArrayList()
    override fun visit(listItem: ListItem) {
        if (listItem.firstChild != null && listItem.firstChild.firstChild is Text) {
            items.add((listItem.firstChild.firstChild as Text).literal)
        }
    }
}

internal class LinkVisitor : AbstractVisitor() {
    @JvmField
    val links: MutableList<Link> = ArrayList()
    override fun visit(link: org.commonmark.node.Link) {
        if (link.destination != null && link.firstChild is Text) {
            val title = (link.firstChild as Text).literal
            links.add(Link(title, link.destination))
        }
    }
}

internal class TitleVisitor(private val matchingLevel: Int) : AbstractVisitor() {
    val titles: MutableList<Title> = ArrayList()
    override fun visit(heading: Heading) {
        if (heading.level == matchingLevel && heading.firstChild is Text) {
            titles.add(Title(heading.level, (heading.firstChild as Text).literal))
        }
    }
}
