package com.syed.models

data class Tip(
    var id: String = "",
    var title: String = "",
    var content: String = "",
    var category: String = "",
    var imageUrl: String = "",
    var author: String = "",
    var dateAdded: Long = System.currentTimeMillis(),
    var lastUpdated: Long = System.currentTimeMillis(),
    var isPublished: Boolean = true,
    var tags: List<String> = emptyList(),
    var views: Int = 0,
    var isActive: Boolean = true,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var authorId: String = "",
    var authorName: String = "",
) {
    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        "",
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        true,
        emptyList(),
        0,
        true,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        "",
        "",
    )
}
