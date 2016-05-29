#!/bin/bash

javadoc -private -sourcepath src/main/java:src/test/java:src/jmh/java -use -version -author -d doc org.numenta.nupic org.numenta.nupic.util org.numenta.nupic.model org.numenta.nupic.research org.numenta.nupic.integration org.numenta.nupic.encoders org.numenta.nupic.algorithms org.numenta.nupic.benchmarks org.numenta.nupic.network org.numenta.nupic.network.sensor org.numenta.nupic.datagen org.numenta.nupic.monitor org.numenta.nupic.monitor.mixin org.numenta.nupic.research.sensorimotor org.numenta.nupic.serialize



