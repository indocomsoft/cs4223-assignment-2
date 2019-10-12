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
    deps = [
        ":device",
        ":memory_delegate",
        ":memory_op",
    ],
)

scala_library(
    name = "memory_delegate",
    srcs = ["MemoryDelegate.scala"],
    exports = [":memory_op"],
    deps = [":memory_op"],
)

scala_library(
    name = "memory_op",
    srcs = [
        "MemoryOp.scala",
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
        ":memory",
        ":memory_delegate",
        "//src/coherence/bus",
        "//src/coherence/bus:bus_delegate",
        "//src/coherence/cache:lru_cache",
    ],
    deps = [
        ":cache_delegate",
        ":cache_op",
        ":device",
        ":memory",
        ":memory_delegate",
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
