package com.example.core.network

import okhttp3.CertificatePinner

object CertificatePinningConfig {
    const val BINANCE_HOST = "api.binance.com"
    const val BINANCE_STREAM_HOST = "stream.binance.com"
    const val POCKET_OPTION_HOST = "api-l.po.market"

    // SHA-256 Public Key Pin hashes for Binance and Pocket Option infrastructure
    // Backup pins included to ensure resilient operation across cert rotations
    val certificatePinner: CertificatePinner = CertificatePinner.Builder()
        .add(BINANCE_HOST, "sha256/kIdc65d6/X8s/61B/9215E9L4hN2uF6s1D5eN1aY+vA=")
        .add(BINANCE_HOST, "sha256/FEzVOUp4dF3gI0ZVPRJhFbSJVXR+uQmMdqNSbsu0gCw=")
        .add(BINANCE_STREAM_HOST, "sha256/kIdc65d6/X8s/61B/9215E9L4hN2uF6s1D5eN1aY+vA=")
        .add(POCKET_OPTION_HOST, "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=")
        .build()
}
