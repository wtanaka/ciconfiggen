package com.wtanaka.ciconfiggen

import com.wtanaka.ciconfiggen.ConfigProto.Configuration.Environment
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.EnvironmentNameValuePair
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class CiConfigGenTest {
    @Test
    fun testEnvironmentToStringEmpty() {
        assertEquals("", Environment
            .newBuilder()
            .build().shellString())
    }

    @Test
    fun testEnvironmentOneVar() {
        assertEquals("FOO=bar", Environment
            .newBuilder()
            .addPair(EnvironmentNameValuePair
                .newBuilder().setName("FOO").setValue("bar"))
            .build()
            .shellString())
    }

    @Test
    fun testEnvironmentTwoVars() {
        assertEquals("FOO=bar DEI=feif", Environment
            .newBuilder()
            .addPair(EnvironmentNameValuePair
                .newBuilder().setName("FOO").setValue("bar"))
            .addPair(EnvironmentNameValuePair
                .newBuilder().setName("DEI").setValue("feif"))
            .build()
            .shellString())
    }

    @Test
    fun testCanUsePairAsHashKey() {
        Assert.assertEquals(
            EnvironmentNameValuePair.newBuilder().setName("FOO").setValue(
                "bar").build().hashCode(),
            EnvironmentNameValuePair.newBuilder().setName("FOO").setValue
            ("bar").build().toByteArray().let {
                EnvironmentNameValuePair
                    .parseFrom(it)
            }.hashCode())
    }
}