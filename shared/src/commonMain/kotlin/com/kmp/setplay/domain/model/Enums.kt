package com.kmp.setplay.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BracketFormat {
    @SerialName("SINGLE_ELIMINATION") SINGLE_ELIMINATION,
    @SerialName("DOUBLE_ELIMINATION") DOUBLE_ELIMINATION,
    @SerialName("ROUND_ROBIN") ROUND_ROBIN,
    @SerialName("SWISS") SWISS,
    @SerialName("LEAGUE") LEAGUE,
    @SerialName("THREE_GAME_GUARANTEE") THREE_GAME_GUARANTEE
}

@Serializable
enum class TournamentStatus {
    @SerialName("DRAFT") DRAFT,
    @SerialName("REGISTRATION") REGISTRATION,
    @SerialName("IN_PROGRESS") IN_PROGRESS,
    @SerialName("COMPLETED") COMPLETED
}

@Serializable
enum class MatchStatus {
    @SerialName("SCHEDULED") SCHEDULED,
    @SerialName("IN_PROGRESS") IN_PROGRESS,
    @SerialName("COMPLETED") COMPLETED,
    @SerialName("BYE") BYE
}

@Serializable
enum class DevicePlatform {
    @SerialName("ANDROID") ANDROID,
    @SerialName("WEB") WEB
}