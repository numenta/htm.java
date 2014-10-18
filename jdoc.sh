#!/bin/bash

javadoc -private -sourcepath src/main/java:src/test/java -use -version -author -d doc org.numenta.nupic.data org.numenta.nupic.model org.numenta.nupic.research org.numenta.nupic.integration org.numenta.nupic.unit


