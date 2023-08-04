package common

object DidcommMessageTypes {
    const val PING_REQUEST = "https://didcomm.org/trust-ping/2.0/ping"
    const val PING_RESPONSE = "https://didcomm.org/trust-ping/2.0/ping-response"
    const val MEDIATE_REQUEST = "https://didcomm.org/coordinate-mediation/2.0/mediate-request"
    const val MEDIATE_GRANT = "https://didcomm.org/coordinate-mediation/2.0/mediate-grant"
    const val MEDIATE_DENY = "https://didcomm.org/coordinate-mediation/2.0/mediate-deny"
    const val MEDIATE_KEYLIST_UPDATE = "https://didcomm.org/coordinate-mediation/2.0/keylist-update"
    const val MEDIATE_KEYLIST_UPDATE_RESPONSE = "https://didcomm.org/coordinate-mediation/2.0/keylist-update-response"
    const val MEDIATE_KEYLIST = "https://didcomm.org/coordinate-mediation/2.0/keylist"
    const val MEDIATE_KEYLIST_QUERY = "https://didcomm.org/coordinate-mediation/2.0/keylist-query"
    const val BASIC_MESSAGE = "https://didcomm.org/basicmessage/2.0/message"
    const val PICKUP_STATUS_REQUEST = "https://didcomm.org/messagepickup/3.0/status-request"
    const val PICKUP_DELIVERY_REQUEST = "https://didcomm.org/messagepickup/3.0/delivery-request"
    const val PICKUP_DELIVERY = "https://didcomm.org/messagepickup/3.0/delivery"
    const val PICKUP_MESSAGES_RECEIVED = "https://didcomm.org/messagepickup/3.0/messages-received"
    const val PICKUP_STATUS = "https://didcomm.org/messagepickup/3.0/status"
}
