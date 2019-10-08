"""
The cache module
"""

from collections import OrderedDict
from typing import List

HIT_LATENCY: int = 1
MEM_LATENCY: int = 100
EVICT_LATENCY: int = 100
SEND_WORD_LATENCY: int = 2


class Cache:
    """ Represents a cache """

    # List is indexed by the set index,
    # the OrderedDict has key of address, and value of None
    associativity: int
    data: List[OrderedDict[int, None]]  # pylint: disable=E1136

    def __init__(self, cache_size: int, associativity: int, block_size: int):
        assert cache_size % block_size == 0
        num_block = cache_size // block_size
        self.associativity = associativity
        self.data = [OrderedDict() for _ in range(num_block)]

    def load(self, address: int) -> int:
        """
        Perform a load operation from an address,
        returns how many cycles this will take.
        """
        return 0

    def store(self, address: int) -> int:
        """
        Performs a store operation to an address,
        returns how many cycles this will take.
        """
        return 0
