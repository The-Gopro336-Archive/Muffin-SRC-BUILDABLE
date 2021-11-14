package me.han.muffin.client.utils.network

import java.io.IOException

/**
 * HTTP request exception whose cause is always an [IOException]
 * Create a new HttpRequestException with the given cause
 * @param cause
 */
class HttpRequestException(cause: IOException): RuntimeException(cause) {

    /**
     * Get [IOException] that triggered this request exception
     *
     * @return [IOException] cause
     */
    override val cause: IOException get() = super.cause as IOException

    companion object {
        private const val serialVersionUID = -1170466989781746231L
    }

}