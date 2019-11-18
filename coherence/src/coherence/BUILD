scala_binary(
    name = "main",
    srcs = [
        "Main.scala",
    ],
    main_class = "coherence.Main",
    deps = [
        ":protocol",
        "//src/coherence/dragon:simulator",
        "//src/coherence/mesi:simulator",
        "//src/coherence/moesi:simulator",
    ],
)

scala_library(
    name = "protocol",
    srcs = ["Protocol.scala"],
)

scala_repl(
    name = "repl",
    deps = [":main"],
)

scala_library(
    name = "address",
    srcs = ["Address.scala"],
    visibility = ["//visibility:public"],
)

scala_library(
    name = "debug",
    srcs = ["Debug.scala"],
    visibility = ["//visibility:public"],
)
