'''
Created on Jul 31, 2016

@author: cogmission
'''

import numpy
import unittest

from ctypes import c_longlong as ll

uintType = "uint32"
    
    

class UniversalRandom(object):
    '''
    classdocs
    '''
    
    def __init__(self, seed, compatibility_mode=True):
        '''
        Constructor
        '''
        self.seed = seed
        self.uintType = "uint32"
        self.max_java_int = 2147483647
        self.compatibility_mode = compatibility_mode
             
    
    def setSeed(self, seed):
        self.seed = seed
        
    def getSeed(self):
        return self.seed
        
    def rshift(self, val, n): 
        #return val>>n if val >= 0 else (val+0x100000000)>>n
        if val >= 0:
            return val>>n
        else:
            return (val+0x100000000)>>n
    
    def _private_sampleWithPrintouts(self, choices, selectedIndices, collectedRandoms):
        """
        Private method which is identical to the sample() method of this class.
        This method is meant for testing of identical behavior with the Java 
        method of the same class. 
        
        Normal use would entail calling sample() instead of this method
        """
        
        choiceSupply = list(choices)
        sampleSize = int(selectedIndices.size)
        upperBound = len(choices)
        for i in xrange(sampleSize):
            randomIdx = self.nextInt(upperBound)
            print "randomIdx: " + str(randomIdx)
            collectedRandoms.append(randomIdx)
            selectedIndices.itemset(i, choiceSupply[randomIdx])
            choiceSupply.remove(choiceSupply[randomIdx])
            upperBound -= 1
            
        selectedIndices.sort()
        return selectedIndices;
    
    def sample(self, choices, selectedIndices):
        """
        Returns a random, sorted, and  unique list of the specified sample size of
        selections from the specified list of choices.
        """
        choiceSupply = list(choices)
        sampleSize = int(selectedIndices.size)
        upperBound = len(choices)
        for i in xrange(sampleSize):
            randomIdx = self.nextInt(upperBound)
            selectedIndices.itemset(i, choiceSupply[randomIdx])
            choiceSupply.remove(choiceSupply[randomIdx])
            upperBound -= 1
            
        selectedIndices.sort()
        print "sample: " + str(list(selectedIndices))
        return selectedIndices;
    
    def shuffle(self, collection):
        """
        Modeled after Fisher - Yates implementation
        """
        index = None
        for i in range(len(collection) - 1, 0, -1):
            index = self.nextInt(i + 1)
            if index != i:
                collection[index] ^= collection[i]
                collection[i] ^= collection[index]
                collection[index] ^= collection[i]
        
        return collection
    
    def rand(self, rows, cols):
        """
        Returns an array of floating point values of the specified shape
        
        @param rows       (int) the number of rows
        @param cols       (int) the number of columns
        """
        
        retval = numpy.empty((0, cols))
        for _ in range(rows):
            row = numpy.empty((cols))
            for j in range(cols):
                row[j] = self.nextDouble()
                 
            retval = numpy.append(retval, [row], axis=0)
         
        return retval
    
    
    def bin_distrib(self, rows, cols, sparsity):
        """
        Returns an array of binary values of the specified shape whose
        total number of "1's" will reflect the sparsity specified.
        
        @param rows       (int) the number of rows
        @param cols       (int) the number of columns
        @param sparsity   (float) number between 0 and 1, indicating percentage
                           of "on" bits
        """
        
        if sparsity < 0 or sparsity > 1:
            raise ValueError('Sparsity must be a number between 0 - 1')
        
        retval = self.rand(rows, cols)
        
        for i in range(len(retval)):
            sub = numpy.where(retval[i] >= sparsity)[0]
            
            sublen = len(sub)
            target = int(sparsity * cols)
            
            if sublen < target:
                full = numpy.arange(0, cols, 1)
                to_fill = numpy.delete(full, sub)
                cnt = len(to_fill)
                for _ in range(target - sublen): 
                    ind = self.nextInt(cnt)
                    item = to_fill[ind]
                    to_fill = numpy.delete(to_fill, ind)
                    retval[i][item] = sparsity
                    print "retval = " + str(list(retval[i]))
                    cnt -= 1
            elif sublen > target:
                cnt = sublen
                for _ in range(sublen - target):
                    ind = self.nextInt(cnt)
                    item = sub[ind]
                    sub = numpy.delete(sub, ind)
                    retval[i][item] = 0.0
                    print "retval = " + str(list(retval[i]))
                    cnt -= 1
        
        retval = (retval >= sparsity).astype(uintType)           
        return retval    
        
    def nextDouble(self):
        nd = self.nextInt(10000)
        retVal = nd * .0001
        print("nextDouble: " + str(retVal))
        return retVal
    
    def nextIntNB(self):
        """
        Next int - No Bounds
        
        Uses maximum Java integer value.
        
        """
        retVal = self.nextInt(self.max_java_int)
        print("nextIntNB: " + str(retVal))
        return retVal
         
    def nextInt(self, bound):
        ''' doc '''
        
        if bound <= 0:
            raise ValueError('bound must be positive')
        
        r = self._next_java_compatible(31) \
            if self.compatibility_mode else self._next(31)
        m = bound - 1
        if (bound & m) == 0:
            r = (bound * r) >> 31
        else:
            r = r % bound
#             u = r
#             r = u % bound
#             while u - r + m > self.max_java_int:
#                 u = self._next_java_compatible(31) \
#                     if self.compatibility_mode else self._next(31)
#                 r = u % bound
        
        print("nextInt(" + str(bound) + "): " + str(r))
        return r
        
    def _next_java_compatible(self, nbits):
        ''' doc '''
        
        x = self.seed & 0xffffffffffffffff              #Preserve 64 bits
        x ^= ll(x << 21).value & 0xffffffffffffffff     #Preserve 64 bits
        x ^= ll(self.rshift(x, 35)).value
        x ^= ll(x << 4).value
        self.seed = x
        x &= ((1 << nbits) - 1)
        
        return x
    
    def _next(self, nbits):
        ''' doc '''
        
        x = self.seed
        x ^= x << 21
        x ^= self.rshift(x, 35)
        x ^= x << 4
        self.seed = x
        x &= ((1 << nbits) - 1)
        
        return x
    
    
if __name__ == '__main__':

    random = UniversalRandom(42)
    
    s = 2858730232218250
    e = random.rshift(s, 35)
    print "e = " + str(e)
    
    x = random.nextInt(50)
    print "x = " + str(x)
    
    x = random.nextInt(50)
    print "x = " + str(x)
    
    x = random.nextInt(50)
    print "x = " + str(x)
    
    x = random.nextInt(50)
    print "x = " + str(x)
    
    x = random.nextInt(50)
    print "x = " + str(x)
    
    for i in xrange(0, 10):
        o = random.nextInt(50)
        print "x = " + str(o)
    
    
    ######################################
    ##       Values Seen in Java        ##
    ######################################
    random = UniversalRandom(42)
    for i in range(10):
        o = random.nextDouble()
        print "d = " + str(o)
    
    '''
    e = 83200
    x = 0
    x = 26
    x = 14
    x = 15
    x = 38
    x = 47
    x = 13
    x = 9
    x = 15
    x = 31
    x = 6
    x = 3
    x = 0
    x = 21
    x = 45
    d = 0.945
    d = 0.2426
    d = 0.5214
    d = 0.0815
    d = 0.0988
    d = 0.5497
    d = 0.4013
    d = 0.4559
    d = 0.5415
    d = 0.2381

    '''
    
    # The "expected" values are the same as produced in Java
    random = UniversalRandom(42)
    choices = [1,2,3,4,5,6,7,8,9]
    sampleSize = 6
    selectedIndices = numpy.empty(sampleSize, dtype="uint32")
    collectedRandoms = []
    expectedSample = [1,2,3,7,8,9]
    expectedRandoms = [0,0,0,5,3,3]
    retVal = random._private_sampleWithPrintouts(choices, selectedIndices, collectedRandoms)
    print "samples are equal ? " + str(retVal == expectedSample) + "  ---  " + str(selectedIndices)
    print "used randoms are equal ? " + str(collectedRandoms == expectedRandoms) + "  ---  " + str(collectedRandoms)
    
 
    
