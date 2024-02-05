package models

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import org.didcommx.peerdid.VerificationMaterialFormatPeerDID
import org.didcommx.peerdid.VerificationMaterialPeerDID
import org.didcommx.peerdid.VerificationMethodTypeAgreement
import org.didcommx.peerdid.VerificationMethodTypeAuthentication

object AgentPeerService {

    fun createServiceJson(serviceEndpoint: String): String {
        return """
        {
            "type": "DIDCommMessaging",
            "serviceEndpoint": {
              "uri":"$serviceEndpoint",
              "routingKeys": [],
              "accept": ["didcomm/v2"]
            }
        }
    """
    }

    fun makeNewJwkKeyX25519(): OctetKeyPair = OctetKeyPairGenerator(Curve.X25519).generate()
    fun makeNewJwkKeyEd25519(): OctetKeyPair = OctetKeyPairGenerator(Curve.Ed25519).generate()
    fun keyAgreemenFromPublicJWK(key: OctetKeyPair): VerificationMaterialPeerDID<VerificationMethodTypeAgreement> =
        VerificationMaterialPeerDID(
            VerificationMaterialFormatPeerDID.JWK,
            key.toPublicJWK(),
            VerificationMethodTypeAgreement.JSON_WEB_KEY_2020
        )

    fun keyAuthenticationFromPublicJWK(key: OctetKeyPair): VerificationMaterialPeerDID<VerificationMethodTypeAuthentication> =
        VerificationMaterialPeerDID(
            VerificationMaterialFormatPeerDID.JWK,
            key.toPublicJWK(),
            VerificationMethodTypeAuthentication.JSON_WEB_KEY_2020
        )

    fun makePeerDid(
        jwkForKeyAgreement: OctetKeyPair = makeNewJwkKeyX25519(),
        jwkForKeyAuthentication: OctetKeyPair = makeNewJwkKeyEd25519(),
        serviceEndpoint: String? = null
    ): PeerDID {
        val did = org.didcommx.peerdid.createPeerDIDNumalgo2(
            listOf(keyAgreemenFromPublicJWK(jwkForKeyAgreement)),
            listOf(keyAuthenticationFromPublicJWK(jwkForKeyAuthentication)),
            serviceEndpoint?.let { createServiceJson(serviceEndpoint) }
        )
        return PeerDID(did, listOf(jwkForKeyAgreement), listOf(jwkForKeyAuthentication))
    }

    fun makeAgent(
        serviceEndpoint: String? = null
    ): PeerDID {
        return makePeerDid(
            makeNewJwkKeyX25519(),
            makeNewJwkKeyEd25519(),
            serviceEndpoint = serviceEndpoint
        )
    }

    fun makeAgentFromPeerDid(
        did: String
    ): PeerDID {
        return PeerDID(did, listOf(), listOf())
    }
}
