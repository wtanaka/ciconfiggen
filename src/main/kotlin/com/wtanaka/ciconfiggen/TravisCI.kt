package com.wtanaka.ciconfiggen

/**
 * Returns TravisCI config in the form of a Map.
 */
fun travisConfig(config: ConfigProto.Configuration): Map<String, Any> = mapOf(
    "language" to "ruby",
    "cache" to mapOf(
        "directories" to listOf(
            "\$HOME/.cache/pip",
            "\$HOME/.rvm"
//                , "\$HOME/.dockerimages"
        )
    ),
    "rvm" to listOf("1.9.3"),
    "env" to envMatrix(config,
        ConfigProto.Configuration.CiService.TRAVIS)
        .map { it.shellString() }
        .toList(),
    "services" to "docker",
    "script" to
//            "if [ -d \$HOME/.dockerimages ]; then " +
//            "  for i in \$HOME/.dockerimages/*; do " +
//            "    docker load < \$i; " +
//            "  done; " +
//            "fi; " +
        "wget -O- bit.ly/ansibletest | sh -x; "
//            + "mkdir -p \$HOME/.dockerimages; "
//            + "for i in \$(docker images -q); do "
//            + "  docker save \$i > \$HOME/.dockerimages/\$i.tar; "
//            + "done"
    ,
    "after_failure" to listOf(
        "cat role-tester-ansible-master/.kitchen.yml",
        "cat role-tester-ansible-master/.kitchen.local.yml",
        "cat role-tester-ansible-master/.kitchen/logs/*.log | grep -v '^I, '"
    ),
    "notifications" to mapOf(
        "webhooks" to "https://galaxy.ansible.com/api/v1/notifications/"
    )
)
