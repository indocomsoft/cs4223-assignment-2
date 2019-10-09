"""
Contains some helper functions that does not fit in any other module.
"""


def is_power_of_2(num: int) -> bool:
    """ Uses bit manipulation to check if n is a power of 2 """
    return (num & (num - 1) == 0) and num != 0


def fast_log2(num: int) -> int:
    """ Gets log2 of num assuming num is power of 2 """
    return num.bit_length() - 1
