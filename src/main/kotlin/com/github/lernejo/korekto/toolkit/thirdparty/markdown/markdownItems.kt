package com.github.lernejo.korekto.toolkit.thirdparty.markdown

data class Title(val level: Int, val text: String)

data class Link(val text: String, val url: String)

data class Badge(val imageUrl: String, val targetUrl: String?)
