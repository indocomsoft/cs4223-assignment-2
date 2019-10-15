scala_library(
    name = "message",
    srcs = [
        "Message.scala",
    ],
)

scala_library(
    name = "simulator",
    srcs = [
        "Simulator.scala",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":cache",
        ":memory",
        ":message",
        "//src/coherence:debug",
        "//src/coherence/bus",
        "//src/coherence/devices:processor",
    ],
)

scala_library(
    name = "cache",
    srcs = [
        "Cache.scala",
    ],
    deps = [
        ":memory",
        ":message",
        ":state",
        "//src/coherence:debug",
        "//src/coherence/cache:lru_cache",
        "//src/coherence/devices:cache",
    ],
)

scala_library(
    name = "state",
    srcs = ["State.scala"],
)

scala_library(
    name = "memory",
    srcs = ["Memory.scala"],
    exports = [
        ":message",
        ":reply",
        ":state",
        "//src/coherence:address",
        "//src/coherence/bus",
        "//src/coherence/devices:memory",
    ],
    deps = [
        ":message",
        ":reply",
        ":state",
        "//src/coherence:address",
        "//src/coherence:debug",
        "//src/coherence/bus",
        "//src/coherence/devices:memory",
    ],
)

scala_library(
    name = "reply",
    srcs = ["Reply.scala"],
)
