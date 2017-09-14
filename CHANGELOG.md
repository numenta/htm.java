# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

# Section Groups
* **Added** - for new features
* **Changed** - for changes in existing functionality
* **Deprecated** - for once-stable features removed in upcoming releases
* **Removed** - for deprecated features removed in this release
* **Fixed** - for any bug fixes
* **Security** - to invite users to upgrade in case of vulnerabilities
* **Unreleased** - shows changes as they occur - allows users to anticipate new functionality.

***

## Unreleased [0.6.14-SNAPSHOT]
#### Removed
#### Added
#### Changed  
#### Fixed 

***

## [v0.6.13-alpha]  Hot Fix!
#### Removed
#### Added
* [[PR #518](https://github.com/numenta/htm.java/pull/518)] Added FieldMetaTypeTest.java
#### Changed  
#### Fixed
* [[PR #518](https://github.com/numenta/htm.java/pull/518)] Fixed "bool" type decode bug

***

## [v0.6.12-alpha]
#### Removed
#### Added
* [[PR #511](https://github.com/numenta/htm.java/pull/511)] SDRClassifier Network API Integration
* [[PR #511](https://github.com/numenta/htm.java/pull/511)] Added new Classifier.java interface
* [[PR #511](https://github.com/numenta/htm.java/pull/511)] Added Tests for new integration
#### Changed  
#### Fixed

***

## [v0.6.10-alpha]
#### Removed
#### Added
#### Changed  
* [[PR #496](https://github.com/numenta/htm.java/pull/496)] Moved ```Connections.java``` and ```ComputeCycle.java``` and other "model" files to package: org.numenta.nupic.model
* [[PR #496](https://github.com/numenta/htm.java/pull/496)] Updated the TemporalMemory and all unit/compatibility tests to be in sync with latest NuPIC version (Updated: 2016-09-23 SHA 1036f25e7223471d72cebc536d6734f78d37b6c7)
* [[PR #486](https://github.com/numenta/htm.java/pull/486)] Updated the SpatialPooler and all unit/compatibility tests to be in sync with latest NuPIC version (Updated: 2016-09-22 SHA a3587db662ddc365ed371c81eb4166a41ad4bc3d)  

#### Fixed

***

## [v0.6.9-alpha] - 2016-09-29
#### Removed
* [[Issues #471](https://github.com/numenta/htm.java/issues/471)] Removed all references to "verbosity" in Connections and Parameters classes
* [[Issues #470](https://github.com/numenta/htm.java/issues/470)] Removed old JSON serializer/deserializer from CLAClassifer 
* All references to "Verbosity" as its a Python convention and not Java
* JSON CLAClassifier serializer / deserializer, replaced with new PersistenceAPI

#### Added
* [[PR #480](https://github.com/numenta/htm.java/pull/480)] Added new RDSECompatibilityTest
* [[PR #478](https://github.com/numenta/htm.java/pull/478)] Added Post-init method to Connections to correct non-initialized derived SpatialPooler parameters.
* [[PR #467](https://github.com/numenta/htm.java/pull/467)] Added new SpatialPoolerCompatibilityTest
* Added new SpatialPooler class
* Added new SpatialPoolerTest class
* Added new TemporalMemory class 
* Added new TemporalMemoryTest class
* Added ```Connections.getPrintString()``` to return parameter printout
* Parameters key ```Parameters.KEY.MAX_SEGMENTS_PER_CELL```
* Parameters key ```Parameters.KEY.MAX_NEW_SYNAPSE_COUNT```
* Added new ```UniversalRandom``` RNG which has an [allegory in python](https://gist.github.com/cogmission/c4cb8feaba19595dae8ff964e18b05d0#file-universal_random-py) which can be used to compare versions 
* Added new methods on the MTJ matrices to add and remove rows and columns
* Added new Algorithm Foundry culled jar containing Sandia wrapper for the MTJ linear algebra lib
* [[PR #453](https://github.com/numenta/htm.java/pull/453)] Added gradlew functionality for quick builds from scratch without any prior installation of Java or Gradle.
* [[PR #439](https://github.com/numenta/htm.java/pull/439)] **Added a new SDRClassifier !!!**
* [[PR #435](https://github.com/numenta/htm.java/pull/435)] Added ObservableTestBase class for improved AssertFailure detection in tests using Observables 

#### Changed
* ```Connections.apply()``` now sets the seed value from the ```Parameters``` object.
* Parameters.(g|s)etParameterByKey to simplified get() / set()
* ```KEY.MIN_PCT_OVERLAP_DUTY_CYCLE``` to ```KEY.MIN_PCT_OVERLAP_DUTY_CYCLES```
* ```KEY.MIN_PCT_ACTIVE_DUTY_CYCLE``` to ```KEY.MIN_PCT_ACTIVE_DUTY_CYCLES```

#### Fixed
* [[Issues #468](https://github.com/numenta/htm.java/issues/468)] Parameters.checkRange now checks values "at" the range boundary as well.
* Fixed documentation of SpatialPooler.inhibitColumnsLocal - added parameter documentation
* [[PR #451](https://github.com/numenta/htm.java/pull/451)] Fixed not filled overlaps array in updateDutyCycles in SP
* [[PR #435](https://github.com/numenta/htm.java/pull/435)] Fixed AssertFailure detection within onNext() method in NAPI Observable tests
* [[PR #431](https://github.com/numenta/htm.java/pull/431)] BitHistory re-scaling fix (in CLAClassifier functionality)
* [[PR #431](https://github.com/numenta/htm.java/pull/431)] Cleaned up tabs and spaces in BitHistory

***

## [v0.6.8-alpha] - 2016-05-02
#### Changed
* [[PR #419](https://github.com/numenta/htm.java/pull/419)] Added additional check for running thread before calling halt during ```store()``` operation. This avoids calling ```halt()``` via the ```Persistable``` interface during serialize calls of ```HTMObjectOutput``` for our partners at [flink-htm](https://github.com/nupic-community/flink-htm)
* [[Issue #418](https://github.com/numenta/htm.java/issues/418)] Internal change to ```HTMObjectOutput``` and ```HTMObjectInput``` constructors to pass in the ```FSTConfiguration``` to avoid creating it twice.
* [[PR #419](https://github.com/numenta/htm.java/pull/419)] Changed default mode of Gradle build script to **not** write debug output to standard out

***

## [v0.6.6-alpha] - 2016-04-14
#### Removed
* [[PR #384](https://github.com/numenta/htm.java/pull/384)] Removed obsolete demo jars which are in htm.java-examples and don't need to be here.
* Removed setSpVerbosity() method from Parameters.java

#### Added
* [[PR #412](https://github.com/numenta/htm.java/pull/412)]  Added new PersistenceAPI (includes serialization; stream handling; new classes and new "serialize" package)
* [[PR #412](https://github.com/numenta/htm.java/pull/412)] Added changes to build script for forcing tests to run when source hasn't changed
* [[PR #412](https://github.com/numenta/htm.java/pull/412)] Added changes to build script for easy turn on of debug output
* [[PR #405](https://github.com/numenta/htm.java/pull/402)] Added new [Docker File reference-build environment](https://github.com/numenta/htm.java/wiki/Build-Instructions#reference-build-environment)!
* [[PR #397](https://github.com/numenta/htm.java/pull/397)] Added close() method to the Network class to bring it inline with Region & Layer - Added Tests for new functionality. (@Mandarx)
* [[PR #396](https://github.com/numenta/htm.java/pull/396)] Added Local.US to MetricsTrace to ensure (expected) dots.
* [[PR #386](https://github.com/numenta/htm.java/pull/386)] Added test for close() method in LayerTest
* [[PR #378](https://github.com/numenta/htm.java/pull/378)] Added more utility methods to ArrayUtils in prep for KNNClassifier development
* [[PR #375](https://github.com/numenta/htm.java/pull 375)] Added new override of rightVecSumAtNZ() method to Matrix classes.
* [[PR #373](https://github.com/numenta/htm.java/pull/373)] Added "activeCells" field and to ManualInput.copy()
* [[PR #370](https://github.com/numenta/htm.java/pull/370)] Added FastRandom implementation (yields 2x speed increase to codebase!) from MoClu's (@antidata)
* [[PR #370](https://github.com/numenta/htm.java/pull/370)] Added Tests for FastRandom (util package)
* [[PR #359](https://github.com/numenta/htm.java/pull/359)] Added HourOfWeek to DateEncoder.
* [[PR #364](https://github.com/numenta/htm.java/pull/364)] Added 15min Hot Gym data file.

#### Changed
* [[PR #412](https://github.com/numenta/htm.java/pull/412)] Renamed class EncoderResult to Encoding
* [[PR #412](https://github.com/numenta/htm.java/pull/412)] Renamed class ClassifierResult to Classification
* [[PR #404](https://github.com/numenta/htm.java/pull/404)] LayerTest & PALayerTest, subscribe() and start() call order to eliminate Thread Race Condition and run on [OpenJDK](http://openjdk.java.net)
* (3-1-2016) Updated htm.java-examples for change to GLOBAL_INHIBITION parameter (sync)
* [[PR #391](https://github.com/numenta/htm.java/pull/391)] Incremented the Gradle Shade Plugin version to 1.2.3
* [[PR #379](https://github.com/numenta/htm.java/pull/379)] Changed SYN_PERM_THRESHOLD to be in line with SYN_PERM_CONNECTED
* [[PR #375](https://github.com/numenta/htm.java/pull 375)] Optimized SpatialPooler.calculateOverlaps by adding the stimulus threshold to the rightVecSumAtNZ() method so we only loop once instead of twice.
* [[PR #373](https://github.com/numenta/htm.java/pull/373)] Changed Matrix class hierarchy naming from XXXSupport to AbstractXXX - more conventional
* [[PR #373](https://github.com/numenta/htm.java/pull/373)] Parameters.KEY_GLOBALINHIBITIONS to KEY_GLOBALINHIBITION
* [[PR #365](https://github.com/numenta/htm.java/pull/365)] Changed inhibitColumnsGlobal() to use Java 8 Streams - 50% performance increase! Fixes [Issue #354](https://github.com/numenta/htm.java/issues/354)
* [[PR #370](https://github.com/numenta/htm.java/pull/370)] Changed Connections initialization to be able to specify RNG implementation from Parameters value.

#### Fixed
* [[PR #386](https://github.com/numenta/htm.java/pull/386)] Fixed floating point problem in Layer dimension inference
* [[PR #373](https://github.com/numenta/htm.java/pull/373)] Rooted out cause of codebase indeterminacy!
* [[PR #360](https://github.com/numenta/htm.java/pull/360)] Fixed dayOfWeek to accept fractional values.
* [[PR #362](https://github.com/numenta/htm.java/pull/362)] Fix for Generic Observable execution order. Fixes [Issue #363](https://github.com/numenta/htm.java/issues/363)
* [[PR #367](https://github.com/numenta/htm.java/pull/367)] Fixed some class and method java docs.

***

## [v0.6.5-alpha] - 2015-11-11
#### Added
* Added Class MultiEncoderAssembler for ME construction
* Added Tests for new MultiEncoderAssembler
* Added @fergalbyrne added PASpatialPooler
* Added @fergalbyrne added PALayer, and PALayerTest
* Added .classpath file back into repo with correct classpath (includes dependencies) TemporalMemory and the input of the CLAClassifier

#### Changed
* Changed Refactored MultiEncoder building so that manual construction has benefit of evolved code
* [Network API Re-write](https://github.com/numenta/htm.java/pull/335) Changed data transformed between a 

#### Removed
* Removed Problematic test in LayerTest and PALayerTest which hung build
* Removed .classpath from .gitignore

***

## [v0.6.4-alpha] - 2015-10-23
#### Added
* New [SDR](https://github.com/numenta/htm.java/blob/master/src/main/java/org/numenta/nupic/SDR.java) class - for sdr related convenience methods
* [[PR #329](https://github.com/numenta/htm.java/pull/329)] - Added low memory implementation of SparseMatrix classes and including tests
* [[Issue #323 PR #324](https://github.com/numenta/htm.java/pull/324)] - Added / Fixed ability to input Coordinate data into the NAPI sensors.
* [Issue #319] - Added feedback for Network thread start detection
* [Issue #319] - (same issue) Added Thread Start detection for computeImmediate() synchronous call failure.
* [Issue #319] - (same issue) Added new tests in NetworkTest.java to ensure above functionality

#### Changed
* [[PR #335](https://github.com/numenta/htm.java/pull/335)] - Re-write of TemporalMemory to CLAClassifier data type for input of _Active Cells_ instead of Predicted Column-cells
* [[PR #333](https://github.com/numenta/htm.java/pull/333)] - Corrected the getNeighborsND method to use topology matrix instead of input matrix for dimension calculations

## [v0.6.2-alpha] - 2015-09-15
#### Added
- [[Issue #308](https://github.com/numenta/htm.java/issues/308)] - Added ability to specify "isLearn" programmatically, instead of just through the stream input
- [[Issue #286](https://github.com/numenta/htm.java/issues/286)] - Work on feeding arrays into NAPI - can now read binary arrays (as Strings) directly from file or manually using ObservableSensor
- Added new SDRPassThroughEncoder which more efficiently sends data through to output and handles both dense and sparse inputs
- Added 2 new FieldMetaType Enums with their decodeType() methods for int[]s as Strings
- Added new tests in LayerTest, HTMSensorTest, ObservableSensorTest
- [[Issue #300] Added wiki help for Linux Gradle Issue](https://github.com/numenta/htm.java/wiki/Gradle---JAVA_HOME-Issue-Resolution)

#### Changed
- Changed ExtensiveTemporalMemoryTest to include fixes for test "H10" (Orphan Decay)
- Fixed PassThroughEncoder and SparsePassThroughEncoder initialization of n and w
- Generified PassThroughEncoder and SparsePassThroughEncoder for efficient subclassing

#### Fixed
- Fixed "isLearn" programmatic setting propagation through the network, added test in RegionTest
- Fixed default anomaly score in Layer.java's Anomaly Func (was 0.0, now 1.0)
- [[Issue #305](https://github.com/numenta/htm.java/issues/305)] - Fixed Synapses not completely removed
- [[PR #294](https://github.com/numenta/htm.java/pull/294)] - Fixes JaCoCo Coverage Reports (by Evgeny Mandrikov)

## [0.6.1] - 2015-08-23
#### Changed
- Removed nested local repo declarations from pom.xml and build.gradle files to finally fix maven project inclusion

## [0.6.0] - 2015-08-23
#### Added
- **htm.java-examples!!** - Split off Demos/Examples repo which can be seen [here](https://github.com/nument/htm.java-examples)
- Newly ported MonitorMixinFramework for algorithm monitoring and commandline printing of the internals of the TemporalMemory and can be used for other algorithms.

#### Changed
- [Issue #270] - Sync Up TemporalMemory with current version (evolved into total re-write)
- Moved the algorithms to the "algorithms" package from the "research package". The research package will be used for additions to research.
- The TemporalMemory now doesn't create container collections to wrap the paramter arguments to the compute(). This should result in faster executions and less memory consumption.
- Cells, Columns, Synapses now cache their hashcodes since they are immutable which should save time when using them as indexes to maps and during comparisons.
- Re-written unit and integration tests to use MonitorMixinFramework.
- Extracted all extraneous files and classes to make the demo jars smaller.
- Other small efficiency changes...


#### Removed
- BasicTemoralMemoryTest - from old integration test module
- TemporalMemoryPatternMachine - from old integration test module

## [0.5.5] - 2015-08-15
#### Added
- New Hot Gym Demo and README.md landing page

#### Fixed
- All demo jar sizes by taking out unneeded artifacts and files

## [0.5.4] - 2015-08-12
#### Changed
- pom.xml Added In-Project maven repo as attempt to solve transitive dependency failure (not working)

## [0.5.3] - 2015-08-11
#### Changed
- [Issue #260] - Add [synchronous compute call](https://github.com/numenta/htm.java/issues/260) to get an immediate response back in a "blocking way" in the same thread that calls the network. Solved in [PR #263]
- [PR #265] - Updated Javadocs for #260

## [0.5.2] - 2015-07-26
#### Added 
- This change log 
- 2 new Cortical.io demos "FoxEatsDemo" & "BreakingNews"
- Tests for the above demos
- Executable Jar files for the above demos
- New wiki pages for demo orientation and instructions
- New JavaDocs for demo packages and classes (2015-07-27, Brev Patterson)
