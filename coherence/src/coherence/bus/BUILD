scala_library(
    name = "bus",
    deps = [
        ":device",
    ],
    srcs = [
        "Bus.scala",
    ],
)

scala_library(
    name = "device",
    deps = [
        ":message_metadata",
    ],
    srcs = [
        "Device.scala",
    ],
    exports = [
        ":message_metadata",
    ],
)

scala_library(
    name = "message_metadata",
    deps = [
        "//src/coherence:unit",
    ],
    exports =[
        "//src/coherence:unit",
    ],
    srcs = [
        "MessageMetadata.scala",
    ]
)