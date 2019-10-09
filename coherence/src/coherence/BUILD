scala_binary(
    name = "main",
    srcs = [
        "Main.scala",
    ],
    main_class = "coherence.Main",
    deps = [
        ":helper",
        ":protocol",
    ],
)

scala_library(
    name = "protocol",
    srcs = ["Protocol.scala"],
)

scala_library(
    name = "helper",
    srcs = ["Helper.scala"],
)

scala_repl(
    name = "repl",
    deps = [":main"],
)
