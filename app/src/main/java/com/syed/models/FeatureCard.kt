package com.syed.models

data class FeatureCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val action: FeatureAction,
)

enum class FeatureAction {
    ALL_PETS,
    DOGS,
    CATS,
    OTHER_PETS,
    RESCUE,
    SHELTERS,
    NEARBY_SHELTERS,
    REGISTER_SHELTER,
    HEALTH_TRACKER,
    PET_IDENTIFICATION,
    MY_REQUESTS,
    TIPS,
    CONTACT,
    PROFILE,
    ALL_FEATURES,
}
