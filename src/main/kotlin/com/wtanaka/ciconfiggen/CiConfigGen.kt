package com.wtanaka.ciconfiggen

import com.google.protobuf.TextFormat
import com.wtanaka.ciconfiggen.ConfigProto.Configuration
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.CiService
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.CiService.CIRCLECI
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.CiService.TRAVIS
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.Environment
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.EnvironmentNameValuePair
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.EnvironmentVariableAxis
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.Suppression
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer
import java.io.File

fun envMatrixTyped(
    config: Sequence<EnvironmentVariableAxis>
): Sequence<Environment> {
    val first = config.firstOrNull()
    return when (first) {
        null -> emptySequence()
        else -> {
            val restSeq = envMatrixTyped(config.drop(1))
            first.valueList.asSequence().map { value ->
                val thisEnv = EnvironmentNameValuePair
                    .newBuilder()
                    .setName(first.name)
                    .setValue(value)

                when (restSeq.firstOrNull()) {
                    null -> sequenceOf(
                        Environment.newBuilder().addPair(thisEnv).build())
                    else -> restSeq.map {
                        it.toBuilder().addPair(thisEnv).build()
                    }
                }
            }.flatten()
        }
    }
}

private fun Environment.matches(suppression: Suppression): Boolean =
    this.pairList.toSet().let { envSet ->
        suppression.envList.all { it in envSet }
    }

fun Sequence<Environment>.suppress(suppression: Suppression) = this.filter {
    !it.matches(suppression)
}

fun Environment.shellString(): String =
    this.pairList.map { "${it.name}=${it.value}" }.joinToString(" ")

fun envMatrix(
    config: Sequence<EnvironmentVariableAxis>
): Sequence<String> {
    val first = config.firstOrNull()
    return when (first) {
        null -> emptySequence()
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

fun envMatrix(
    config: Configuration, service: CiService
): Sequence<Environment> = config
    .envAxisList
    .asSequence()
    .filterNotNull()
    .let { envMatrixTyped(it) }
    .let { sequenceOfEnv ->
        config.suppressList
            .filter { service == it.service }
            .fold(sequenceOfEnv) { envSeq, suppression ->
                envSeq.suppress(suppression)
            }
    }

fun travisConfig(config: Configuration): Map<String, Any> {
    return mapOf(
        "language" to "ruby",
        "rvm" to listOf("1.9.3"),
        "env" to envMatrix(config, TRAVIS).map { it.shellString() }.toList(),
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
        "jobs" to envMatrix(config, CIRCLECI).map { it.shellString() }.map {
            it to mapOf(
                "machine" to true,
                "steps" to listOf(
                    "checkout",
                    mapOf("run" to "sudo apt-get update -qq"),
                    mapOf(
                        "run" to "sudo apt-get install -y wget make ruby-bundler python-virtualenv"),
                    // Needed to upgrade requests[security] on Ubuntu 14.04
                    mapOf(
                        "run" to "sudo apt-get install -y python-dev libffi-dev libssl-dev"),
//            -      # Not needed for machine executor
//            -      #- setup_remote_docker
                    mapOf("run" to "sudo apt-get install -y docker.io"),
                    mapOf("run" to "wget -qO- bit.ly/ansibletest | env $it sh")
                ))
        }.toMap(),
        "workflows" to mapOf(
            "version" to 2,
            "test" to mapOf(
                "jobs" to envMatrix(config, CIRCLECI).map { it.shellString() }
                    .toList()
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
    File(".circleci/").mkdir()
    listOf(
        Triple(".travis.yml", travisYaml(), travisConfig(config.build())),
        Triple(".circleci/config.yml", travisYaml(),
            circleConfig(config.build()))
    ).forEach { triple ->
        File(triple.first).outputStream().writer(Charsets.UTF_8).use {
            it.write(
                "# This file was generated by https://github.com/wtanaka/ciconfiggen\n")
            it.write("# DO NOT EDIT\n")
            it.write(config?.let {
                triple.second.dump(triple.third)
            })
        }
    }
}
