package com.syed.models

data class AdminAction(
    var id: String = "",
    var adminId: String = "",
    var adminName: String = "",
    var action: String = "", // "approved", "rejected", "added", "deleted", "updated"
    var entityType: String = "", // "pet", "adoption_request", "rescue_request", "tip", "message"
    var entityId: String = "",
    var description: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var details: Map<String, Any> = emptyMap(),
) {
    constructor() : this("", "", "", "", "", "", "", System.currentTimeMillis(), emptyMap())
}
