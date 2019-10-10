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
        ":message",
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
        ":state",
        ":message",
        "//src/coherence/cache:lru_cache",
        "//src/coherence/devices:cache",
    ],
)

scala_library(
    name = "state",
    srcs = ["State.scala"],
)
