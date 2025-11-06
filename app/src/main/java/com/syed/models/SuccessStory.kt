package com.syed.models

data class SuccessStory(
    var id: String = "",
    var petName: String = "",
    var ownerName: String = "",
    var title: String = "",
    var story: String = "",
    var imageUrl: String = "",
    var dateAdded: Long = System.currentTimeMillis(),
    var isActive: Boolean = true,
) {
    constructor() : this("", "", "", "", "", "", System.currentTimeMillis(), true)
}
