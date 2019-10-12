scala_library(
    name = "device",
    srcs = [
        "Device.scala",
    ],
    visibility = ["//visibility:public"],
)

scala_library(
    name = "memory",
    srcs = [
        "Memory.scala",
    ],
    visibility = ["//visibility:public"],
    exports = [
        ":cache",
        ":device",
        "//src/coherence/bus",
        "//src/coherence/bus:bus_delegate",
    ],
    deps = [
        ":cache",
        ":device",
        "//src/coherence/bus",
        "//src/coherence/bus:bus_delegate",
    ],
)

scala_library(
    name = "processor_op",
    srcs = ["ProcessorOp.scala"],
)

scala_library(
    name = "processor",
    srcs = [
        "Processor.scala",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":cache",
        ":device",
        ":processor_op",
    ],
)

scala_repl(
    name = "processor_repl",
    deps = [":processor"],
)

scala_library(
    name = "cache",
    srcs = ["Cache.scala"],
    visibility = ["//visibility:public"],
    exports = [
        ":cache_delegate",
        ":cache_op",
        ":device",
        "//src/coherence/bus",
        "//src/coherence/bus:bus_delegate",
        "//src/coherence/cache:lru_cache",
    ],
    deps = [
        ":cache_delegate",
        ":cache_op",
        ":device",
        "//src/coherence/bus",
        "//src/coherence/bus:bus_delegate",
        "//src/coherence/cache:lru_cache",
    ],
)

scala_library(
    name = "cache_op",
    srcs = ["CacheOp.scala"],
)

scala_library(
    name = "cache_delegate",
    srcs = ["CacheDelegate.scala"],
    exports = [":cache_op"],
    deps = [":cache_op"],
)
