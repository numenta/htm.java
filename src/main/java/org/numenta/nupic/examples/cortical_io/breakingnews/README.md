# ![Numenta Logo](http://numenta.org/images/numenta-icon128.png) ![Cortical.io Logo](https://avatars0.githubusercontent.com/u/7721887?v=3&amp;s=200)
# Breaking News
####_This the [Java™](http://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html) version of the Cortical.io team's 2015 Spring Hackathon demo._

This demo demonstrates the integration of the Java version of  [NuPIC](https://github.com/numenta/nupic) ([htm.java](https://github.com/numenta/htm.java)) and [Cortical.io's](http://www.cortical.io/technology.html) Natural Language Programming (NLP) - using htm.java's [Network API (NAPI)](http://numenta.org/blog/2015/06/08/htm-java-receives-new-network-api.html)

This demo uses a previously collected file of data (Tweets) as input in order to detect conversational trends which are grouped according to their contextual similarity to one another. 

![](http://cognitionmission.com/BreakingNews.png)

> Each Tweet is sent to Cortical.io's processing engine which returns a 
> ["Fingerprint"](http://www.cortical.io/technology_semantic.html) (Cortical.io's special representation of a word or 
> body of text in [SDR](http://www.cortical.io/technology_representations.html) form). The Tweet SDR is then inputted 
> into the HTM (via the NAPI) which then produces a prediction. The prediction is then compared to the input Tweet 
> SDR (using Cortical.io's Compare API - part of the ["Semantic Folding 
> Engine"](http://www.cortical.io/technology.html)) which returns an "io.cortical.rest.model.Metric" containing 
> several useful analytics used to compute their degree of anomaly from the preceding Tweet.

#### Demo Usage
```
java -jar breaking-news-demo-1.0.0.jar -K<Cortical.io API Key>

(no space between the "-K" flag and the key -> -K###-####)
```

The execution of this demo requires the use of a **Free** API Key from [Cortical.io](http://www.cortical.io/resources_apikey.html). Just follow the link. It's real easy and the whole process takes just a few minutes (5 secs. on your part, and 2mins. 55secs. on Cortical.io's part) - and an internet connection. Once the demo has been run once, the fingerprints for the terms in the demo are cached locally - however the demo still requires an internet connection.

* For more information on Cortical.io API usage you can have a look at their online [API interactive website](http://api.cortical.io); and their [Java SDK here...](https://github.com/cortical-io)
* For more information on NuPIC and Numenta's HTM technology, go [here...](http://numenta.org) 
* For more information about [HTM.java...](https://github.com/numenta/htm.java)

