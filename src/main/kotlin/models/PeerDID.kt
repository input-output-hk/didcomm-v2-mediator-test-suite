package models

import com.nimbusds.jose.jwk.OctetKeyPair
import org.didcommx.didcomm.common.VerificationMaterial
import org.didcommx.didcomm.common.VerificationMaterialFormat
import org.didcommx.didcomm.common.VerificationMethodType
import org.didcommx.didcomm.diddoc.DIDDocResolver
import org.didcommx.didcomm.secret.Secret
import org.didcommx.didcomm.secret.SecretResolverInMemory
import org.didcommx.didcomm.utils.fromMulticodec
import org.didcommx.peerdid.VerificationMaterialFormatPeerDID
import org.didcommx.peerdid.VerificationMaterialPeerDID
import org.didcommx.peerdid.VerificationMethodTypePeerDID
import org.didcommx.peerdid.core.fromBase58
import org.didcommx.peerdid.core.fromBase58Multibase
import org.didcommx.peerdid.core.fromJwk
import org.didcommx.peerdid.core.toBase58Multibase
import org.didcommx.peerdid.core.toMulticodec
import org.didcommx.peerdid.resolvePeerDID

class PeerDID(
    val did: String,
    val jwkForKeyAgreement: List<OctetKeyPair>,
    val jwkForKeyAuthentication: List<OctetKeyPair>
) {
    val didDocument: String
        get() = resolvePeerDID(did, VerificationMaterialFormatPeerDID.JWK)

    fun getSecrets(): Map<String, Secret> {
        fun validateRawKeyLength(key: ByteArray) {
            // for all supported key types now (ED25519 and X25510) the expected size is 32
            if (key.size != 32) {
                throw IllegalArgumentException("Invalid key $key")
            }
        }

        fun createMultibaseEncnumbasis(key: VerificationMaterialPeerDID<out VerificationMethodTypePeerDID>): String {
            val decodedKey = when (key.format) {
                VerificationMaterialFormatPeerDID.BASE58 -> fromBase58(key.value.toString())
                VerificationMaterialFormatPeerDID.MULTIBASE -> fromMulticodec(fromBase58Multibase(key.value.toString()).second).second
                VerificationMaterialFormatPeerDID.JWK -> fromJwk(key)
            }
            validateRawKeyLength(decodedKey)
            return toBase58Multibase(toMulticodec(decodedKey, key.type))
        }

        val keyAgreement =
            AgentPeerService.keyAgreemenFromPublicJWK(this.jwkForKeyAgreement.first())
        val keyAuthentication =
            AgentPeerService.keyAuthenticationFromPublicJWK(this.jwkForKeyAuthentication.first())

        val keyIdAgreement = createMultibaseEncnumbasis(keyAgreement).drop(1)
        val keyIdAuthentication = createMultibaseEncnumbasis(keyAuthentication).drop(1)
        val keyIdAgreementIndex = this.did.indexOf(keyIdAgreement)
        val keyIdAuthenticationIndex = this.did.indexOf(keyIdAuthentication)
        val keyAgreementId: Int
        val keyAuthenticationId: Int
        if (keyIdAgreementIndex < keyIdAuthenticationIndex) {
            keyAgreementId = 1
            keyAuthenticationId = 2
        } else {
            keyAgreementId = 2
            keyAuthenticationId = 1
        }

        val secretKeyAgreement = Secret(
            "${this.did}#key-$keyAgreementId",
            VerificationMethodType.JSON_WEB_KEY_2020,
            VerificationMaterial(VerificationMaterialFormat.JWK, this.jwkForKeyAgreement.first().toJSONString())
        )
        val secretKeyAuthentication = Secret(
            "${this.did}#key-$keyAuthenticationId",
            VerificationMethodType.JSON_WEB_KEY_2020,
            VerificationMaterial(VerificationMaterialFormat.JWK, this.jwkForKeyAuthentication.first().toJSONString())
        )

        return mapOf(
            "${this.did}#key-$keyAgreementId" to secretKeyAgreement,
            "${this.did}#key-$keyAuthenticationId" to secretKeyAuthentication
        )
    }

    fun getSecretResolverInMemory(): SecretResolverInMemory {
        return SecretResolverInMemory(getSecrets())
    }

    fun getDidDocResolverInMemory(): DIDDocResolver {
        return DIDDocResolverPeerDID()
    }

    fun getServiceEndpoint(): String {
        return DIDDocResolverPeerDID().resolve(this.did).get().didCommServices.map { it.serviceEndpoint }.first()
    }
}
