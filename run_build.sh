#!/bin/bash

echo `pwd`
echo `ls -al`
cd src
echo `pwd`
javac -g -d ../build -cp ../resources/btc-ascii-table-1.0.jar:../resources/trove-3.1a1.jar org/numenta/nupic/data/* org/numenta/nupic/model/* org/numenta/nupic/research/*

