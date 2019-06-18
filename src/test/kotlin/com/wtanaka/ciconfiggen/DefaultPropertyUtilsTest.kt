package com.wtanaka.ciconfiggen

import org.junit.Test

class DefaultPropertyUtilsTest {
    @Test
    fun createPropertySet() {
        DefaultPropertyUtils().getProperties(String::class.java)
    }
}