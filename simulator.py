"""
Provides an abstract base class for a simulator
"""

from abc import ABC, abstractmethod
from typing import Iterator, Tuple
from enum import Enum

from constants import NUM_CORE


class Label(Enum):
    """ Represents the different kinds of labels possible """
    LOAD = '0'
    STORE = '1'
    OTHER = '2'


class Simulator(ABC):
    """ The abstract base class for a simulator """
    @staticmethod
    def generate_file_names(prefix: str) -> Iterator[str]:
        """ Generates the file names out of the prefix """
        return ("{}_{}.data".format(prefix, x) for x in range(NUM_CORE))

    @staticmethod
    def read_file(filename: str) -> Iterator[Tuple[Label, int]]:
        """ Reads the given filename and parses it """
        def process_line(line: str) -> Tuple[Label, int]:
            label, value = line.strip().split(" ")
            return (Label(label), int(value, 16))

        return (process_line(line) for line in open(filename))

    @staticmethod
    @abstractmethod
    def simulate(prefix: str, cache: int, assoc: int, block: int) -> None:
        """ Simulate the trace with the given parameters """
        return
