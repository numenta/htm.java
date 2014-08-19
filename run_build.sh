#!/bin/bash

cd src
javac -d ../build -cp ../resources/btc-ascii-table-1.0.jar:../resources/trove-3.1a1.jar org/numenta/nupic/data/* org/numenta/nupic/model/* org/numenta/nupic/research/*

