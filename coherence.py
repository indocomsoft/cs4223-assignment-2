"""
The main module
"""

import sys


def print_usage():
    """ Prints help message """
    print("""
Usage: {} protocol input_file_prefix cache_size associativity block_size

protocol = MESI or Dragon
The sizes are in bytes
          """.format(sys.argv[0]))


def main():
    """ The main function """
    if len(sys.argv) != 6:
        print_usage()
        sys.exit(1)


if __name__ == "__main__":
    main()
