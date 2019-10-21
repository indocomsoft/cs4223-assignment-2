scala_library(
    name = "bus",
    srcs = [
        "Bus.scala",
    ],
    visibility = ["//visibility:public"],
    exports = [
        ":bus_statistics",
    ],
    deps = [
        ":bus_delegate",
        ":bus_statistics",
        ":message_metadata",
        "//src/coherence:address",
        "//src/coherence:debug",
        "//src/coherence/devices:device",
    ],
)

scala_library(
    name = "bus_delegate",
    srcs = [
        "BusDelegate.scala",
    ],
    visibility = ["//visibility:public"],
    exports = [
        ":message_metadata",
        ":reply_metadata",
        "//src/coherence:address",
    ],
    deps = [
        ":message_metadata",
        ":reply_metadata",
        "//src/coherence:address",
    ],
)

scala_library(
    name = "message_metadata",
    srcs = [
        "MessageMetadata.scala",
    ],
    visibility = ["//visibility:public"],
    exports = ["//src/coherence:address"],
    deps = ["//src/coherence:address"],
)

scala_library(
    name = "reply_metadata",
    srcs = [
        "ReplyMetadata.scala",
    ],
    visibility = ["//visibility:public"],
)

scala_library(
    name = "bus_statistics",
    srcs = [
        "BusStatistics.scala",
    ],
    deps = [":reply_metadata"],
    exports = [":reply_metadata"],
    visibility = ["//visibility:public"],
)
