package com.wtanaka.ciconfiggen

import org.junit.Test
import org.junit.Assert.*

class CircleCIKtTest {
    @Test
    fun testCircleConfig() {
        circleConfig(ConfigProto.Configuration.newBuilder().build())
    }
}