package com.wtanaka.ciconfiggen

import org.junit.Test

class CircleCIKtTest {
    @Test
    fun testCircleConfig() {
        circleConfig(ConfigProto.Configuration.newBuilder().build())
    }
}