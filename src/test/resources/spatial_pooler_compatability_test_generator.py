#! /usr/bin/env python
# ----------------------------------------------------------------------
# Numenta Platform for Intelligent Computing (NuPIC)
# Copyright (C) 2013, Numenta, Inc.  Unless you have an agreement
# with Numenta, Inc., for a separate license for this software code, the
# following terms and conditions apply:
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero Public License version 3 as
# published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU Affero Public License for more details.
#
# You should have received a copy of the GNU Affero Public License
# along with this program.  If not, see http://www.gnu.org/licenses.
#
# http://numenta.org/licenses/
# ----------------------------------------------------------------------

import cPickle as pickle
import numpy
import unittest2 as unittest
import time
import traceback

from nupic.research.spatial_pooler import SpatialPooler as PySpatialPooler
from nupic.math.universal_random import UniversalRandom
from nupic.math.spatial_pooler_output_javaclass_writer import SpatialPoolerOutputWriter

realType = "NTA_Real"
uintType = "uint32"
numRecords = 100



class SpatialPoolerCompatabilityTest(unittest.TestCase):
  """
  Tests to ensure that the PY and CPP implementations of the spatial pooler
  are functionally identical.
  """

  def setUp(self):
    # Set to 1 for more verbose debugging output
    self.verbosity = 1
   
 
  def runSideBySide(self, params):
    """
    Run the PY and CPP implementations side by side on random inputs.
    If seed is None a random seed will be chosen based on time, otherwise
    the fixed seed will be used.
    
    If learnMode is None learning will be randomly turned on and off.
    If it is False or True then set it accordingly.
    
    If convertEveryIteration is True, the CPP will be copied from the PY
    instance on every iteration just before each compute.
    """
    pySp = self.CreateSP("py", params)
    numColumns = pySp.getNumColumns()
    numInputs = pySp.getNumInputs()
    
    self.printPotentials(pySp, numInputs)
    print("\n\n")
    self.printConnects(pySp, numInputs)
    print("\n\n")
    self.printPermanences(pySp, numInputs)
    
    threshold = 0.4
    random = UniversalRandom(42)
    inputMatrix = random.bin_distrib(numRecords, numInputs, threshold).astype(uintType)
#     for i in xrange(len(inputMatrix)):
#         print str(list(inputMatrix[i]))
    
    writer = SpatialPoolerOutputWriter("/Users/cogmission/git/newspatial/htm.java")
    random = UniversalRandom(42)
    # Run side by side for numRecords iterations
    for i in xrange(numRecords):
      learn = (random.nextDouble() > 0.5)
      
      if self.verbosity > 1:
        print "Iteration:",i,"learn=",learn
      PyActiveArray = numpy.zeros(numColumns).astype(uintType)
      inputVector = inputMatrix[i,:]
      
      pySp.compute(inputVector, learn, PyActiveArray)
      
      writer.collectActive(PyActiveArray)
      writer.writePermanences(pySp, i)
    
    writer.writeActives()
    writer.closeOutputs()
      

  def testCompatability1(self):
    params = {
      "inputDimensions": [4,4],
      "columnDimensions": [5,3],
      "potentialRadius": 20,
      "potentialPct": 0.5,
      "globalInhibition": True,
      "localAreaDensity": 0,
      "numActiveColumnsPerInhArea": 5,
      "stimulusThreshold": 0,
      "synPermInactiveDec": 0.01,
      "synPermActiveInc": 0.1,
      "synPermConnected": 0.10,
      "minPctOverlapDutyCycle": 0.001,
      "minPctActiveDutyCycle": 0.001,
      "dutyCyclePeriod": 30,
      "maxBoost": 10.0,
      "seed": 4,
      "spVerbosity": 0,
      "urandom": UniversalRandom(42)
    }
    # This seed used to cause problems if learnMode is set to None
    self.runSideBySide(params)


  def CreateSP(self, imp, params):
    """
    Helper class for creating an instance of the appropriate spatial pooler using
    given parameters. 

    Parameters:
    ----------------------------
    imp:       Either 'py' or 'cpp' for creating the appropriate instance.
    params:    A dict for overriding constructor parameters. The keys must
             correspond to contructor parameter names.
  
    Returns the SP object.
    """
    if (imp == "py"):
      spClass = PySpatialPooler
    elif (imp == "cpp"):
      spClass = CPPSpatialPooler
    else:
      raise RuntimeError("unrecognized implementation")

    sp = spClass(**params)
  
    return sp
 
 
  def printPotentials(self, pySp, numInputs):
    for i in xrange(pySp.getNumColumns()):
      pyPot = numpy.zeros(numInputs).astype(uintType)
      pySp.getPotential(i, pyPot)
      print str(list(pyPot))
      
      
  def printConnects(self, pySp, numInputs):
    for i in xrange(pySp.getNumColumns()):
      pyCon = numpy.zeros(numInputs).astype(uintType)
      pySp.getConnectedSynapses(i, pyCon)
      print str(list(pyCon))
      
      
  def printPermanences(self, pySp, numInputs):
    for i in xrange(pySp.getNumColumns()):
      pyPerm = numpy.zeros(numInputs).astype(numpy.float32)
      pySp.getPermanence(i, pyPerm)
      print str(list(pyPerm)) #[ float("{:.6f}".format(d)) for d in pyPerm]))
      
if __name__ == "__main__":
  unittest.main()
