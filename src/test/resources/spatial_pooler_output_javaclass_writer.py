'''
Created on Sep 15, 2016

@author: cogmission
'''

import numpy
from nupic.bindings.math import GetNTAReal

realType = GetNTAReal()

class SpatialPoolerOutputWriter(object):
    
    def __init__(self, base_path):
        '''
        Construct using a base_path string which points to the directory
        of your htm.java root directory (ends with htm.java or whatever your
        repo is titled)
        
        @param    base_path    the path leading to your htm.java repo (hint: has
                               "src" as one of its subdirectories)
        '''
        
        self.base_path = base_path
        self.activesPath = base_path + "/src/test/java/org/numenta/nupic/algorithms/Actives.java"
        self.permsPath = base_path + "/src/test/java/org/numenta/nupic/algorithms/Permanences.java"
        self.activesFile = open(self.activesPath, 'w')
        self.permsFile = open(self.permsPath, 'w')
        
        self.actives = []
        
    
    def closeOutputs(self):
        self.activesFile.flush()
        self.activesFile.close()
        self.permsFile.flush()
        self.permsFile.close()
    
  
    def collectActive(self, active):
        self.actives.append(active)  
      
      
    def writeActives(self):
        self.activesFile.write("package org.numenta.nupic.algorithms;\n\n\n")
        self.activesFile.write("public class Actives {\n")
        self.activesFile.write("\tpublic int[][] getActivations() {\n")
        self.activesFile.write("\t\treturn new int[][] {\n")
        for i in range(len(self.actives)):
            line = str(list(self.actives[i]))
            line = "\t\t\t{ " + line[1:-1] + " },\n"
            self.activesFile.write(line)
    
        self.activesFile.write("\t\t};\n")
        self.activesFile.write("\t}\n")
        self.activesFile.write("}\n")
    
    
    def writePermanences(self, sp, iteration):
        if iteration == 0:
            self.permsFile.write("package org.numenta.nupic.algorithms;\n\n\n")
            self.permsFile.write("public class Permanences {\n")
      
        self.permsFile.write("\tpublic double[][] getPermanences" + str(iteration) + "() {\n")
        self.permsFile.write("\t\treturn new double[][] {\n")
        for i in xrange(sp.getNumColumns()):
            perm = numpy.zeros(sp.getNumInputs()).astype(realType)
            sp.getPermanence(i, perm)
            line = str(list(perm))
            line = line = "\t\t\t{ " + line[1:-1] + " },\n"
            self.permsFile.write(line)
    
        self.permsFile.write("\t\t};\n")
        self.permsFile.write("\t}\n\n")
    
        if iteration == 99:
            self.permsFile.write("}\n")
            
            