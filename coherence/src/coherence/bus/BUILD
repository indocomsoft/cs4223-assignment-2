scala_library(
    name = "bus",
    srcs = [
        "Bus.scala",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":bus_delegate",
        ":message_metadata",
        "//src/coherence/devices:device",
    ],
)

scala_library(
    name = "bus_delegate",
    srcs = [
        "BusDelegate.scala",
    ],
    exports = [":message_metadata"],
    deps = [":message_metadata"],
)

scala_library(
    name = "message_metadata",
    srcs = [
        "MessageMetadata.scala",
    ],
    visibility = ["//visibility:public"],
    exports = [
        "//src/coherence:unit",
    ],
    deps = [
        "//src/coherence:unit",
    ],
)
