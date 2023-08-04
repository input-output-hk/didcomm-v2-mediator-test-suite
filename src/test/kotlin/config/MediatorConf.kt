package config

import com.sksamuel.hoplite.ConfigAlias

data class MediatorConf(
    val url: String,
    var did: String?,
    @ConfigAlias("invitation_endpoint") val invitationEndpoint: String
)
