load("@io_bazel_rules_scala//scala:scala_toolchain.bzl", "scala_toolchain")

scala_toolchain(
    name = "my_toolchain_impl",
    scalacopts = ["-Xelide-below", "0"],
    unused_dependency_checker_mode = "error",
    visibility = ["//visibility:public"]
)

toolchain(
    name = "my_scala_toolchain",
    toolchain_type = "@io_bazel_rules_scala//scala:toolchain_type",
    toolchain = "my_toolchain_impl",
    visibility = ["//visibility:public"]
)
