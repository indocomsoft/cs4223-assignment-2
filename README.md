# cs4223-assignment-2

To use, please install Bazel first

Then, run

```bash
cd coherence
bazel run //src/coherence:main
```
The command to run the code on a given benchmark follows the folowing format:
`./coherence <protcol> <path-to-benchmark-trace> <cache-size> <associativity> <block size>`
Example run:
```bash
cd coherence
./coherence Dragon ../bodytrack_four/bodytrack 4096 2 32
```

Otherwise, if you have the compiled jar file, replace `./coherence` with `java -jar coherence.jar`.
For example:
```bash
java -jar coherence.jar Dragon bodytrack_four/bodytrack 4096 2 32
```
