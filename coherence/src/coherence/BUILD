scala_binary(
    name = "main",
    main_class = "coherence.Main",
    srcs = [
        "Main.scala",
        "Protocol.scala",
        "Helper.scala",
    ],
)

scala_repl(
    name = "repl",
    deps = [":main"],
)