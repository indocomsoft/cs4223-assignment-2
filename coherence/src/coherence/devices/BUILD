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
