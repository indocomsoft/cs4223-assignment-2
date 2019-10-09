scala_binary(
    name = "main",
    main_class = "coherence.Main",
    srcs = [
        "main.scala",
        "protocol.scala",
        "helper.scala",
    ],
)

scala_repl(
    name = "repl",
    deps = [":main"],
)