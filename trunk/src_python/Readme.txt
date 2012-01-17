All the settings, parameters are in "conf.py"

I haven't tried PyMetis yet, so PyMetis is just what I downloaded.


community.py is the algorithm for graph partition.(you don't need to run this)

sim.py is the file to keep the similarity measurements. You could add here and change in conf.py to decide what to use (I tried to add cosine similarity, but result is too bad.)

netsimile.py is the file for graph similarity measure. You don't need to run this.

ReID.py is the original work by Tina. Which is just pair-wisely compare node in target graph and all the nodes in reference graph.

/data is the directory which contains all the data. You could re-generated all the features using ReID and import different graphs in there. (To keep it simple, let make the names of graphs like Target_Reference as prefix. i.e. For sms and wire data let's make it sms_wire as the prefix, which means sms is the target graph, and wire is the reference graph)

utility.py is the common functions the programs need to use. you don't need to run this. (called by ReID.py and community_matcher.py)


