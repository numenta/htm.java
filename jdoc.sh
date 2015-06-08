#!/bin/bash

javadoc -private -sourcepath src/main/java:src/test/java:src/jmh/java -use -version -author -d doc org.numenta.nupic org.numenta.nupic.util org.numenta.nupic.model org.numenta.nupic.research org.numenta.nupic.integration org.numenta.nupic.encoders org.numenta.nupic.algorithms org.numenta.nupic.examples org.numenta.nupic.examples.sp org.numenta.nupic.examples.qt org.numenta.nupic.benchmarks org.numenta.nupic.network org.numenta.nupic.network.sensor org.numenta.nupic.examples.network org.numenta.nupic.datagen


