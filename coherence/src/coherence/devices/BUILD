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
    exports = [
        ":device",
        ":memory_delegate",
        "//src/coherence/bus:bus_delegate",
    ],
    deps = [
        ":device",
        ":memory_delegate",
        "//src/coherence/bus:bus_delegate",
    ],
)
