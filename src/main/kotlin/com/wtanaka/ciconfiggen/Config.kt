package com.wtanaka.ciconfiggen

import com.wtanaka.ciconfiggen.ConfigProto.Configuration
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.CiService
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.Environment
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.EnvironmentVariableAxis
import com.wtanaka.ciconfiggen.ConfigProto.Configuration.Suppression

/**
 * Converts an Environment object into a "key=value" string.
 */
fun Environment.shellString(): String =
    this.pairList.map { "${it.name}=${it.value}" }.joinToString(" ")

private fun envMatrixTyped(
    config: Sequence<EnvironmentVariableAxis>
): Sequence<Environment> {
    val first = config.firstOrNull()
    return when (first) {
        null -> emptySequence()
        else -> {
            val restSeq = envMatrixTyped(config.drop(1))
            first.valueList.asSequence().map { value ->
                val thisEnv =
                    Configuration.EnvironmentNameValuePair.newBuilder()
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

private fun Sequence<Environment>.suppress(suppression: Suppression) =
    this.filter {
        !it.matches(suppression)
    }

/**
 * Flattens the Configuration into a sequence of Environment.
 */
fun envMatrix(
    config: Configuration,
    service: CiService
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
