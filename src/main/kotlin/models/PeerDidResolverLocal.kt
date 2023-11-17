
import com.google.gson.JsonSyntaxException
import io.ipfs.multibase.binary.Base64
import org.didcommx.peerdid.*
import org.didcommx.peerdid.PeerDID
import org.didcommx.peerdid.core.*

object PeerDidResolverLocal {

    fun isPeerDID(peerDID: String): Boolean {
        val regex =
            (
                "^did:peer:(([0](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))" +
                    "|(2((.[AEVID](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))+(.(S)[0-9a-zA-Z=]*)*)))$"
                ).toRegex()
        return regex.matches(peerDID)
    }

    fun resolvePeerDID(
        peerDID: PeerDID,
        format: VerificationMaterialFormatPeerDID = VerificationMaterialFormatPeerDID.MULTIBASE
    ): String {
        if (!isPeerDID(peerDID)) {
            throw MalformedPeerDIDException("Does not match peer DID regexp: $peerDID")
        }
        val didDoc = when (peerDID[9]) {
            '0' -> buildDIDDocNumalgo0(peerDID, format)
            '2' -> buildDIDDocNumalgo2(peerDID, format)
            else -> throw IllegalArgumentException("Invalid numalgo of Peer DID: $peerDID")
        }
        return didDoc.toJson()
    }

    private fun buildDIDDocNumalgo0(peerDID: PeerDID, format: VerificationMaterialFormatPeerDID): DIDDocPeerDID {
        val inceptionKey = peerDID.substring(10)
        val decodedEncumbasis = decodeMultibaseEncnumbasisAuth(inceptionKey, format)
        return DIDDocPeerDID(
            did = peerDID,
            authentication = listOf(getVerificationMethod(peerDID, decodedEncumbasis))
        )
    }

    private fun buildDIDDocNumalgo2(peerDID: PeerDID, format: VerificationMaterialFormatPeerDID): DIDDocPeerDID {
        val keys = peerDID.drop(11)
        val encodedServicesJson = mutableListOf<JSON>()
        val authentications = mutableListOf<VerificationMethodPeerDID>()
        val keyAgreement = mutableListOf<VerificationMethodPeerDID>()

        keys.split(".").forEach {
            val prefix = it[0]
            val value = it.drop(1)

            when (prefix) {
                Numalgo2Prefix.SERVICE.prefix -> {
                    encodedServicesJson.add(value)
                }

                Numalgo2Prefix.AUTHENTICATION.prefix -> {
                    val decodedEncumbasis = decodeMultibaseEncnumbasisAuth(value, format)
                    authentications.add(getVerificationMethod(peerDID, decodedEncumbasis))
                }

                Numalgo2Prefix.KEY_AGREEMENT.prefix -> {
                    val decodedEncumbasis = decodeMultibaseEncnumbasisAgreement(value, format)
                    keyAgreement.add(getVerificationMethod(peerDID, decodedEncumbasis))
                }

                else -> throw IllegalArgumentException("Unsupported transform part of PeerDID: $prefix")
            }
        }
        val services = doDecodeService(encodedServicesJson, peerDID)
        return DIDDocPeerDID(
            did = peerDID,
            authentication = authentications,
            keyAgreement = keyAgreement,
            service = services
        )
    }

    private fun decodeMultibaseEncnumbasisAuth(
        multibase: String,
        format: VerificationMaterialFormatPeerDID
    ): DecodedEncumbasis {
        try {
            val decodedEncumbasis = decodeMultibaseEncnumbasis(multibase, format)
            validateAuthenticationMaterialType(decodedEncumbasis.verMaterial)
            return decodedEncumbasis
        } catch (e: IllegalArgumentException) {
            throw MalformedPeerDIDException("Invalid key $multibase", e)
        }
    }

    private fun decodeMultibaseEncnumbasisAgreement(
        multibase: String,
        format: VerificationMaterialFormatPeerDID
    ): DecodedEncumbasis {
        try {
            val decodedEncumbasis = decodeMultibaseEncnumbasis(multibase, format)
            validateAgreementMaterialType(decodedEncumbasis.verMaterial)
            return decodedEncumbasis
        } catch (e: IllegalArgumentException) {
            throw MalformedPeerDIDException("Invalid key $multibase", e)
        }
    }

    private fun doDecodeService(service: List<String>, peerDID: String): List<Service>? {
        try {
            return decodeService(service, peerDID)
        } catch (e: IllegalArgumentException) {
            throw MalformedPeerDIDException("Invalid service", e)
        }
    }

    private fun getVerificationMethod(did: String, decodedEncumbasis: DecodedEncumbasis) =
        VerificationMethodPeerDID(
            id = "$did#${decodedEncumbasis.encnumbasis}",
            controller = did,
            verMaterial = decodedEncumbasis.verMaterial
        )
    private data class DecodedEncumbasis(
        val encnumbasis: String,
        val verMaterial: VerificationMaterialPeerDID<out VerificationMethodTypePeerDID>
    )

    private enum class Numalgo2Prefix(val prefix: Char) {
        AUTHENTICATION('V'),
        KEY_AGREEMENT('E'),
        SERVICE('S');
    }

    private fun decodeMultibaseEncnumbasis(
        multibase: String,
        format: VerificationMaterialFormatPeerDID
    ): DecodedEncumbasis {
        val (encnumbasis, decodedEncnumbasis) = fromBase58Multibase(multibase)
        val (codec, decodedEncnumbasisWithoutPrefix) = fromMulticodec(decodedEncnumbasis)
        validateRawKeyLength(decodedEncnumbasisWithoutPrefix)

        val verMaterial = when (format) {
            VerificationMaterialFormatPeerDID.BASE58 ->
                when (codec) {
                    Codec.X25519 -> VerificationMaterialAgreement(
                        format = format,
                        type = VerificationMethodTypeAgreement.X25519_KEY_AGREEMENT_KEY_2019,
                        value = toBase58(decodedEncnumbasisWithoutPrefix)
                    )
                    Codec.ED25519 -> VerificationMaterialAuthentication(
                        format = format,
                        type = VerificationMethodTypeAuthentication.ED25519_VERIFICATION_KEY_2018,
                        value = toBase58(decodedEncnumbasisWithoutPrefix)
                    )
                }
            VerificationMaterialFormatPeerDID.MULTIBASE ->
                when (codec) {
                    Codec.X25519 -> VerificationMaterialAgreement(
                        format = format,
                        type = VerificationMethodTypeAgreement.X25519_KEY_AGREEMENT_KEY_2020,
                        value = toBase58Multibase(
                            toMulticodec(
                                decodedEncnumbasisWithoutPrefix,
                                VerificationMethodTypeAgreement.X25519_KEY_AGREEMENT_KEY_2020
                            )
                        )
                    )
                    Codec.ED25519 -> VerificationMaterialAuthentication(
                        format = format,
                        type = VerificationMethodTypeAuthentication.ED25519_VERIFICATION_KEY_2020,
                        value = toBase58Multibase(
                            toMulticodec(
                                decodedEncnumbasisWithoutPrefix,
                                VerificationMethodTypeAuthentication.ED25519_VERIFICATION_KEY_2020
                            )
                        )
                    )
                }
            VerificationMaterialFormatPeerDID.JWK ->
                when (codec) {
                    Codec.X25519 -> VerificationMaterialAgreement(
                        format = format,
                        type = VerificationMethodTypeAgreement.JSON_WEB_KEY_2020,
                        value = toJwk(decodedEncnumbasisWithoutPrefix, VerificationMethodTypeAgreement.JSON_WEB_KEY_2020)
                    )
                    Codec.ED25519 -> VerificationMaterialAuthentication(
                        format = format,
                        type = VerificationMethodTypeAuthentication.JSON_WEB_KEY_2020,
                        value = toJwk(
                            decodedEncnumbasisWithoutPrefix,
                            VerificationMethodTypeAuthentication.JSON_WEB_KEY_2020
                        )
                    )
                }
        }

        return DecodedEncumbasis(encnumbasis, verMaterial)
    }

    private fun validateAuthenticationMaterialType(verificationMaterial: VerificationMaterialPeerDID<out VerificationMethodTypePeerDID>) {
        if (verificationMaterial.type !is VerificationMethodTypeAuthentication) {
            throw IllegalArgumentException("Invalid verification material type: ${verificationMaterial.type} instead of VerificationMaterialAuthentication")
        }
    }

    private fun validateAgreementMaterialType(verificationMaterial: VerificationMaterialPeerDID<out VerificationMethodTypePeerDID>) {
        if (verificationMaterial.type !is VerificationMethodTypeAgreement) {
            throw IllegalArgumentException("Invalid verification material type: ${verificationMaterial.type} instead of VerificationMaterialAgreement")
        }
    }

    private fun validateRawKeyLength(key: ByteArray) {
        // for all supported key types now (ED25519 and X25510) the expected size is 32
        if (key.size != 32) {
            throw IllegalArgumentException("Invalid key $key")
        }
    }

    private fun decodeService(encodedServices: List<JSON>, peerDID: PeerDID): List<Service>? {
        if (encodedServices.isEmpty()) {
            return null
        }

        val decodedServices = encodedServices.map { encodedService ->
            Base64.decodeBase64(encodedService).decodeToString()
        }
        val decodedServicesJson = decodedServices.joinToString(separator = ",", prefix = "[", postfix = "]")
        val serviceMapList = try {
            fromJsonToList(decodedServicesJson)
        } catch (e: JsonSyntaxException) {
            try {
                listOf(fromJsonToMap(decodedServicesJson))
            } catch (e: JsonSyntaxException) {
                throw IllegalArgumentException("Invalid JSON $decodedServices")
            }
        }

        val services = serviceMapList.mapIndexed { index, serviceMap ->
            if (!serviceMap.containsKey(ServicePrefix.getValue(SERVICE_TYPE))) {
                throw IllegalArgumentException("Service doesn't contain a type")
            }

            val serviceType = serviceMap.getValue(ServicePrefix.getValue(SERVICE_TYPE)).toString()
                .replace(ServicePrefix.getValue(SERVICE_DIDCOMM_MESSAGING), SERVICE_DIDCOMM_MESSAGING)

            val service = mutableMapOf<String, Any>(
                "id" to "$peerDID#${serviceType.lowercase()}-$index",
                "type" to serviceType
            )

            serviceMap[ServicePrefix.getValue(SERVICE_ENDPOINT)]?.let { service.put(SERVICE_ENDPOINT, it) }
            serviceMap[ServicePrefix.getValue(SERVICE_ROUTING_KEYS)]?.let { service.put(SERVICE_ROUTING_KEYS, it) }
            serviceMap[ServicePrefix.getValue(SERVICE_ACCEPT)]?.let { service.put(SERVICE_ACCEPT, it) }

            OtherService(service)
        }

        return services.toList()
    }

    private val ServicePrefix = mapOf(
        SERVICE_TYPE to "t",
        SERVICE_ENDPOINT to "s",
        SERVICE_DIDCOMM_MESSAGING to "dm",
        SERVICE_ROUTING_KEYS to "r",
        SERVICE_ACCEPT to "a"
    )
}
