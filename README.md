# ![Numenta Logo](http://numenta.org/images/numenta-icon128.png)  
htm.java
========

* Build: [![Build Status](https://travis-ci.org/numenta/htm.java.png?branch=master)](https://travis-ci.org/numenta/htm.java)
* Unit Test Coverage: [![Coverage Status](https://coveralls.io/repos/numenta/htm.java/badge.png?branch=master)](https://coveralls.io/r/numenta/htm.java?branch=master)

Official community-supported Java implementation of [Hierarchal Temporal Memory (HTM)](http://numenta.org/htm-white-paper.html), ported from the [Numenta Platform for Intelligent Computing](https://github.com/numenta/nupic) python project.

**NOTE: Minimum JavaSE version is 1.8**   

### In The News...
* [HTM.java Now versioned and up on Maven Central!](http://search.maven.org/#search%7Cga%7C1%7Chtm.java) (06/12/2015)
* [HTM.java Recieves New Network API](http://numenta.org/blog/2015/06/08/htm-java-receives-new-network-api.html) (06/08/2015)
* HTM.java is now [**OFFICIAL!**](https://github.com/numenta/htm.java/issues/193) See the [_**announcement**_](http://lists.numenta.org/pipermail/nupic_lists.numenta.org/2015-February/010404.html) (02/25/2015)
* [HTM.java Now Has Anomaly Detection & Anomaly Likelihood Prediction!](https://github.com/numenta/htm.java/wiki/Anomaly-Detection-Module) (02/22/2015)
* [HTM.java Recieves New Benchmarking Tools](http://numenta.org/blog/2015/02/10/htm-java-receives-benchmark-harness.html) (02/2015)
* [HTM.java Reaches Functional Completion](http://numenta.org/blog/2014/12/03/htm-on-the-jvm.html) (12/2014)

For a more detailed discussion of <b>htm.java</b> see: <BR>
* [htm.java Wiki](https://github.com/numenta/htm.java/wiki)
* [Java Docs](http://numenta.org/docs/htm.java/)

See the [Test Coverage Reports](https://coveralls.io/jobs/4164658) - For more information on where you can contribute! Extend the tests and get your name in bright lights!

For answers to more detailed questions, email the [nupic-discuss](http://lists.numenta.org/mailman/listinfo/nupic_lists.numenta.org) mailing list, or chat with us on Gitter.

[![Gitter](https://img.shields.io/badge/gitter-join_chat-blue.svg?style=flat)](https://gitter.im/numenta/public?utm_source=badge)

***

### Call to Arms: [HTM.java needs you!](http://lists.numenta.org/pipermail/nupic-hackers_lists.numenta.org/2014-November/002819.html)

## Goals

The primary goal of this library development is to provide a Java version of NuPIC that has a 1-to-1 correspondence to all systems, functionality and tests provided by Numenta's open source implementation; while observing the tenets, standards and conventions of Java language best practices and development.

By working closely with Numenta and receiving their enthusiastic support and guidance, it is intended that this library be maintained as a viable Java language alternative to Numenta's C++ and Python offerings. However it must be understood that "official" support is (for the time being) currently limited to community resources such as the maintainers of this library and Numenta Forums / Message Lists and IRC:

 * [NuPIC Community](http://numenta.org/community.html)

## Installation - [***Updated!(05/2015) Eclipse Environment Setup Wiki***](https://github.com/numenta/htm.java/wiki/Eclipse-Setup-Tips)

An Eclipse IDE .project and .classpath file are provided so that the cloned project can be easily set up inside of Eclipse. For the time being, the Eclipse IDE is the only "pre-made" project configuration.

In addition, there are "launch configurations" for all of the tests and any runnable entities off of the "htm.java" main directory. These may be run directly in Eclipse by right-clicking them and choosing "run".

## After download by clone or fork:    

Execute a quick sanity check by running all the tests from within the \<path to git repo\>/htm.java
```
gradle check  # Executes the tests and runs the benchmarks

--or--

gradle -Pskipbench check  # Executes the tests w/o running the benchmarks
```
**Note:** Info on installing **gradle** can be found on the wiki (look at #3.) [here](https://github.com/numenta/htm.java/wiki/Eclipse-Setup-Tips)

## Project Integration (New)
For tips and insights on how to use the Network API to add HTM's into your own applications, see:

[Quick Start Guide](https://github.com/numenta/htm.java/wiki/NAPI-Quick-Start-Guide)

...and for more in-depth answers see:

[NAPI Overview](https://github.com/numenta/htm.java/wiki/NAPI-In-Depth-Component-Overview)


## For Updates Follow

* [#HtmJavaDevUpdates](https://twitter.com/hashtag/HtmJavaDevUpdates?src=hash)
