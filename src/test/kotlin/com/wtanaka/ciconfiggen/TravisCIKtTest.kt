package com.wtanaka.ciconfiggen

import org.junit.Test

class TravisCIKtTest {
    @Test
    fun testTravisConfig() {
        travisConfig(ConfigProto.Configuration.newBuilder().build())
    }
}
