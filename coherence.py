"""
The main module
"""

import os
import sys

from simulator import Simulator
from mesi import MESI
from dragon import Dragon


def print_usage(error_message: str) -> None:
    """ Prints help message """
    print("""Error: {}
------
Usage:
{} protocol input_file_prefix cache_size associativity block_size

protocol = MESI or Dragon
The sizes are in bytes
          """.format(error_message, sys.argv[0]))
    sys.exit(1)


def main() -> None:
    """ The main function """
    try:
        # pylint: disable=unbalanced-tuple-unpacking
        _, protocol, prefix, cache_str, assoc_str, block_str = sys.argv
    except ValueError:
        print_usage("Not enough arguments")
    try:
        cache = int(cache_str, 10)
    except ValueError:
        print_usage("cache not a number")
    try:
        assoc = int(assoc_str, 10)
    except ValueError:
        print_usage("assoc not a number")
    try:
        block = int(block_str, 10)
    except ValueError:
        print_usage("block not a number")
    try:
        assert all(
            os.path.isfile(x) for x in Simulator.generate_file_names(prefix))
    except AssertionError:
        print_usage("Input files with the given prefix not found")
    if protocol == "MESI":
        MESI.simulate(prefix, cache, assoc, block)
    elif protocol == "Dragon":
        Dragon.simulate(prefix, cache, assoc, block)
    else:
        print_usage("Unrecognised protocol {}".format(protocol))


if __name__ == "__main__":
    main()
