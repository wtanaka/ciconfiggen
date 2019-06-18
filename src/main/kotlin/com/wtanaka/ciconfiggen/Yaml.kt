package com.wtanaka.ciconfiggen

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer

fun defaultYaml(): Yaml {
    val repr = Representer()
    repr.propertyUtils = DefaultPropertyUtils()
    val dumperOptions = DumperOptions()
    dumperOptions.defaultFlowStyle =
        DumperOptions.FlowStyle.BLOCK
    return Yaml(repr, dumperOptions)
}