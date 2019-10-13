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
    deps = [
        ":cache",
        ":message",
        ":memory",
        "//src/coherence/bus",
        "//src/coherence/devices:processor",
    ],
    visibility = ["//visibility:public"],
)

scala_library(
    name = "cache",
    srcs = [
        "Cache.scala",
    ],
    deps = [
        ":message",
        ":memory",
        ":state",
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
        ":state",
        "//src/coherence/bus",
        "//src/coherence/devices:memory",
    ],
    deps = [
        ":message",
        ":state",
        "//src/coherence/bus",
        "//src/coherence/devices:memory",
    ],
)
