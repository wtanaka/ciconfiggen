package com.wtanaka.ciconfiggen

import com.google.protobuf.TextFormat
import com.wtanaka.ciconfiggen.ConfigProto.Configuration
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer
import java.io.File

fun envMatrix(
    config: Sequence<Configuration.EnvironmentVariableAxis>
): Sequence<String> {
    val first = config.firstOrNull()
    return when (first) {
        null -> emptySequence<String>()
        else -> {
            val restSeq = envMatrix(config.drop(1))
            first.valueList.asSequence().map { value ->
                val thisEnv = "${first.name}=$value"
                when (restSeq.firstOrNull()) {
                    null -> sequenceOf(thisEnv)
                    else -> restSeq.map { "$it $thisEnv" }
                }
            }.flatten()
        }
    }
}

fun envMatrix(config: Configuration): Sequence<String> {
    val list = config.envAxisList.mapNotNull { it }.asSequence()
    return envMatrix(list)
}

fun travisConfig(config: Configuration): Map<String, Any> {
    return mapOf(
        "language" to "ruby",
        "rvm" to listOf("2.2", "1.9.3"),
        "env" to envMatrix(config).toList(),
        "services" to "docker",
        "script" to "wget -O- bit.ly/ansibletest | sh -x",
        "after_failure" to listOf(
            "cat role-tester-ansible-master/.kitchen.yml",
            "cat role-tester-ansible-master/.kitchen.local.yml",
            "cat role-tester-ansible-master/.kitchen/logs/*.log | grep -v '^I, '"
        ),
        "notifications" to mapOf(
            "webhooks" to "https://galaxy.ansible.com/api/v1/notifications/"
        )
    )
}

fun travisYaml(): Yaml {
    val repr = Representer()
    repr.propertyUtils = TravisPropertyUtils()
    val dumperOptions = DumperOptions()
    dumperOptions.defaultFlowStyle = BLOCK
    return Yaml(repr, dumperOptions)
}

fun circleConfig(config: Configuration): Map<String, Any> {
    return mapOf(
        "version" to 2,
        "jobs" to envMatrix(config).map {
            it to mapOf(
                "machine" to true,
                "steps" to listOf(
                    "checkout",
                    mapOf("run" to "sudo apt-get update -qq"),
                    mapOf("run" to "sudo apt-get install -y wget make ruby-bundler python-virtualenv"),
                    // Needed to upgrade requests[security] on Ubuntu 14.04
                    mapOf("run" to "sudo apt-get install -y python-dev libffi-dev libssl-dev"),
//            -      # Not needed for machine executor
//            -      #- setup_remote_docker
                    mapOf("run" to "sudo apt-get install -y docker.io"),
                    mapOf("run" to "wget -qO- bit.ly/ansibletest | env $it sh")
                ))
        }.toMap(),
        "workflows" to mapOf(
            "version" to 2,
            "test" to mapOf(
                "jobs" to envMatrix(config).map { it }.toList()
            )
        )
    )
}

fun main(args: Array<String>) {
    val config = File(".ciconfiggen.config")
        .inputStream().reader(Charsets.UTF_8).use {
            val config = Configuration.newBuilder()
            TextFormat.getParser().merge(it, config)
            config
        }
    listOf(
        Triple(".travis.yml", travisYaml(), travisConfig(config.build())),
        Triple(".circleci/config.yml", travisYaml(),
            circleConfig(config.build()))
    ).forEach { triple ->
        File(triple.first).outputStream().writer(Charsets.UTF_8).use {
            it.write(config?.let {
                triple.second.dump(triple.third)
            })
        }
    }
}
