# ![Numenta Logo](http://numenta.org/images/numenta-icon128.png) ![Cortical.io Logo](https://avatars0.githubusercontent.com/u/7721887?v=3&amp;s=200)
# What Does A Fox Eat?

This the [Java™](http://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html) version of Subutai Ahmad's [2013 Fall Hackathon Demo](http://numenta.org/blog/2013/11/06/2013-fall-hackathon-outcome.html#fox) illustrating the 
integration of HTM technology ([NuPIC](https://github.com/numenta/nupic)); [Cortical.io's](http://www.cortical.io/technology.html) Natural Language Programming (NLP)
technology; and the [HTM.java Network API](http://numenta.org/blog/2015/06/08/htm-java-receives-new-network-api.html).

This demo demonstrates the powerful similarity and word association technology of
Cortical.io and the generalization power of HTM technology by "teaching" the HTM triplets
of animals, actions, and objects such as "frog eats flies", and "cow eat grain". After
presenting the HTM with 36 different examples of triplet animal "preferences", we then
ask the HTM what a "fox" would eat.

![](http://cognitionmission.com/foxeats.png)

The HTM, having never "seen" the word fox before, comes back with "rodent" or "squirrel",
which is what an animal that is "fox-like" might eat. Cortical.io's "Semantic Folding" 
technology, utilizes SDRs ([Sparse Data Representations](http://www.cortical.io/technology_representations.html)) to encode property qualities to
sparse data bits. The use of this technology to reverse engineer the HTM's "prediction"
to see what "meal" the HTM generalizes for foxes, exhibits vast potential in the combination
of these two advanced Machine Intelligence technologies.

#### Demo Usage
```
java -jar FoxEatsDemo.jar -K<Cortical.io API Key>
```

The execution of this demo requires the use of a **Free** API Key from [Cortical.io](http://www.cortical.io/resources_apikey.html). Just follow the link. It's real easy and the whole process takes just a few minutes (5 secs. on your part, and 2mins. 55secs. on Cortical.io's part) - and an internet connection. Once the demo has been run once, the fingerprints for the terms in the demo are cached locally - however the demo still requires an internet connection.

* For more information on Cortical.io API usage you can have a look at their online [API interactive website](http://api.cortical.io); and their [Java SDK here...](https://github.com/cortical-io)
* For more information on NuPIC and Numenta's HTM technology, go [here...](http://numenta.org) 
* For more information about [HTM.java...](https://github.com/numenta/htm.java)
