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

## Unreleased

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
