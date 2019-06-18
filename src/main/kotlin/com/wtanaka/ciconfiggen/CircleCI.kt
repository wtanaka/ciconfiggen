package com.wtanaka.ciconfiggen

fun circleConfig(config: ConfigProto.Configuration): Map<String, Any> {
    return mapOf(
        "version" to 2,
        "jobs" to envMatrix(config,
            ConfigProto.Configuration.CiService.CIRCLECI).map { it.shellString() }.map {
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
                "jobs" to envMatrix(config,
                    ConfigProto.Configuration.CiService.CIRCLECI).map { it.shellString() }
                    .toList()
            )
        )
    )
}