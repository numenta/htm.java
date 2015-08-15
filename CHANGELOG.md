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
- [Issue #270 (In Progress)] - Sync Up TemporalMemory with current version 

## [0.5.5] - 2015-08-15
### Added
- New Hot Gym Demo and README.md landing page

### Fixed
- All demo jar sizes by taking out unneeded artifacts and files

## [0.5.4] - 2015-08-12
### Changed
- pom.xml Added In-Project maven repo as attempt to solve transitive dependency failure (not working)

## [0.5.3] - 2015-08-11
### Changed
- [Issue #260] - Add [synchronous compute call](https://github.com/numenta/htm.java/issues/260) to get an immediate response back in a "blocking way" in the same thread that calls the network. Solved in [PR #263]
- [PR #265] - Updated Javadocs for #260

## [0.5.2] - 2015-07-26
### Added 
- This change log 
- 2 new Cortical.io demos "FoxEatsDemo" & "BreakingNews"
- Tests for the above demos
- Executable Jar files for the above demos
- New wiki pages for demo orientation and instructions
- New JavaDocs for demo packages and classes (2015-07-27, Brev Patterson)
