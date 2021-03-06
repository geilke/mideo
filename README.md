# MiDEO
MiDEO is a framework to perform data mining on probabilistic condensed representations. It builds on online density estimates that estimate the joint density of data streams.

For further details, please refer to the paper:

Michael Geilke, Andreas Karwath, Eibe Frank and Stefan Kramer. *Online Estimation of Discrete, Continuous, and Conditional Joint Densities using Classifier Chains*.
In: Data Mining and Knowledge Discovery, Springer 2017. http://dx.doi.org/10.1007/s10618-017-0546-6.

## Modules

Please notice that module support has only been implemented for datasets with discrete variables so far. It is disabled by default and can be activated by setting the variable ```MODULES_ENABLED``` in the class ```org.kramerlab.mideo.estimators.ModuleDetection``` to ```true```. It is planned to replace this flag by an option that is supported by EVAL files (for an explanation, see below).

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
The easiest way to get in touch with MiDEO are EVAL files. They are basically a list of jobs that are supposed to be executed by the MiDEO framework, where each job specifies a data stream, a density estimator, and some evaluation. A simple example is provided by the file [bn.eval](examples/bn.eval):
```
[{
    "jobDescription": {
        "outputFile": "bn.result", 
        "jobIndex": 1, 
        "estimator": {
            "label": "edo-cc-MC", 
            "discreteBaseEstimator.leafClassifier": "MC", 
            "type": "org.kramerlab.mideo.estimators.edo.EDO", 
            "ensembleSize": 1, 
            "seed": 35315}, 
        "evaluation": {
            "measure": "LL", 
            "type": "org.kramerlab.mideo.evaluation.DensityEstimation"}, 
        "stream": {
            "label": "dataset-01", 
            "numInstances": 100000,
            "streamSource": "src/test/resources/dataset-01.arff", 
            "classIndex": -1, 
            "type": "org.kramerlab.mideo.data.streams.FileStream"}}, 
    "result": null
}]
```
In this example, the density estimator ```org.kramerlab.mideo.estimators.edo.EDO``` is used with a single classifier chain (ensembleSize) and majority class as base classifier (discreteBaseEstimator.leafClassifier). The evaluation is performed by ```org.kramerlab.mideo.evaluation.DensityEstimation```, which measure the average log-likelihood (```LL```). The density estimator is trained and evaluated on a data stream created from the ARFF file ```src/test/resources/dataset-01.arff```. It will read 100000 instances from it.

To use this EVAL file, one can run MiDEO either directly from Maven
```
mvn exec:java -Dexec.mainClass="org.kramerlab.mideo.evaluation.JobCenter" -Dexec.args="-f examples/bn.eval -startIndex 1 -endIndex 1"
```
or you use the standalone version of the jar file (which has been renamed to ```mideo.jar``` in this case):
```
java -Xmx10000M -cp mideo.jar org.kramerlab.mideo.evaluation.JobCenter -f examples/bn.eval -startIndex 1 -endIndex 1
```
### Evaluation measures

MiDEO supports two evaluation measures: ```LL``` and ```PrequentialLL```.

### Datasets with continuous variables
MiDEO support datasets with discrete and / or continuous variables. To configure the base estimator that is used for continuous variables, one can specify the parameters ```continuousBaseEstimator.numBins``` and ```continuousBaseEstimator.maxNumberOfKernels```.

```
[{
    "jobDescription": {
        "outputFile": "letter.result",
        "jobIndex": 1,
        "estimator": {
            "label": "edo-cc-MC",
            "discreteBaseEstimator.leafClassifier": "MC",
            "continuousBaseEstimator.numBins": 5,
            "continuousBaseEstimator.maxNumberOfKernels": 1000,
            "type": "org.kramerlab.mideo.estimators.edo.EDO",
            "ensembleSize": 1,
            "seed": 35316},
        "evaluation": {
            "measure": "PrequentialLL",
            "type": "org.kramerlab.mideo.evaluation.DensityEstimation"},
        "stream": {
            "label": "dataset-02",
            "numInstances": 1000,
            "streamSource": "src/test/resources/dataset-02.arff",
            "classIndex": -1,
            "type": "org.kramerlab.mideo.data.streams.FileStream"}}, 
    "result": null
}]
```

### Estimator options

In addition to the options mentioned above, one can specify how the ensemble members are weighted. Using the option ```uniformWeights``` (boolean), ```true``` selects ECC and ```false``` EWCC.

## Cite
If you use MiDEO, please cite the following paper:

Michael Geilke, Andreas Karwath, Eibe Frank and Stefan Kramer. *Online Estimation of Discrete, Continuous, and Conditional Joint Densities using Classifier Chains*.
In: Data Mining and Knowledge Discovery, Springer 2017. http://dx.doi.org/10.1007/s10618-017-0546-6.
