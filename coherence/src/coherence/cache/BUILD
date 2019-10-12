scala_library(
    name = "cache_line",
    srcs = ["CacheLine.scala"],
)

scala_library(
    name = "lru_cache",
    srcs = ["LRUCache.scala"],
    visibility = ["//visibility:public"],
    exports = [":cache_line"],
    deps = [":cache_line"],
)
