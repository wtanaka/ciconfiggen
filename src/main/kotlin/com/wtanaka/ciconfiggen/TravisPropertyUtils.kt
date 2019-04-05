package com.wtanaka.ciconfiggen

import org.yaml.snakeyaml.introspector.BeanAccess
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.introspector.PropertyUtils
import java.util.Collections
import java.util.TreeSet

class TravisPropertyUtils : PropertyUtils() {
    override fun createPropertySet(
        type: Class<out Any>?, bAccess: BeanAccess?
    ): MutableSet<Property> {
        val result = TreeSet<Property>(Collections.reverseOrder())
        result.addAll(super.createPropertySet(type, bAccess))
        return result
    }
}