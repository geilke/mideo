# MiDEO
MiDEO is a framework to perform data mining on probabilistic condensed representations. It builds on online density estimates that estimate the joint density of data streams.

In the next couple of weeks, we will provide some infrastructure for future data mining algorithms and implement the algorithms proposed in

Michael Geilke, Andreas Karwath, Eibe Frank, and Stefan Kramer. *Online Estimation of Discrete Densities*. In: Proceedings of the 13th IEEE International Conference on Data Mining (ICDM 2013), pp. 191-200, IEEE 2013. [doi:10.1109/ICDM.2013.91](http://dx.doi.org/10.1109/ICDM.2013.91).

## Build
The following command builds a jar file without including any dependencies (e.g., MOA) and makes it available in the folder `target`:
```
mvn clean install
```
If you want to have a standalone jar file, you can run the following commands:
```
mvn package
mvn assembly:single
```
Subsequently, the jar file is available in the folder `target` and has the naming scheme ```mideo-*-jar-with-dependencies.jar```.

## Run
Either, you run MiDEO directly from Maven
```
mvn exec:java -Dexec.mainClass="org.kramerlab.mideo.evaluation.JobCenter" -Dexec.args="-f examples/bn.eval -startIndex 1 -endIndex 1"
```
or you use the standalone version of the jar file (which has been renamed to ```mideo.jar``` in this case):
```
java -Xmx10000M -cp mideo.jar org.kramerlab.mideo.evaluation.JobCenter -f examples/bn.eval -startIndex 1 -endIndex 1
```

## Cite
If you use MiDEO, please cite the following paper:

Michael Geilke, Andreas Karwath, Eibe Frank, and Stefan Kramer. *Online Estimation of Discrete Densities*. In: Proceedings of the 13th IEEE International Conference on Data Mining (ICDM 2013), pp. 191-200, IEEE 2013. [doi:10.1109/ICDM.2013.91](http://dx.doi.org/10.1109/ICDM.2013.91).

```
@inproceedings{geilke-2013a,
  author    = {Michael Geilke and
               Eibe Frank and
               Andreas Karwath and
               Stefan Kramer},
  title     = {Online Estimation of Discrete Densities},
  booktitle = {Proceedings of the 13th {IEEE} International Conference
               on Data Mining},
  pages     = {191--200},
  year      = 2013,
  publisher = {IEEE},
  url       = {http://dx.doi.org/10.1109/ICDM.2013.91},
  doi       = {10.1109/ICDM.2013.91},
  timestamp = {Fri, 02 Jan 2015 13:41:10 +0100},
  biburl    = {http://dblp.uni-trier.de/rec/bib/conf/icdm/GeilkeFKK13},
  bibsource = {dblp computer science bibliography, http://dblp.org}
}
```
