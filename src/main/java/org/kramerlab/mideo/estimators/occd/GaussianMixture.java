/**
 * MiDEO: a framework to perform data mining on probabilistic condensed 
 * representations
 * Copyright (C) 2015 Michael Geilke
 *
 * This file is part of MiDEO.
 * 
 * MiDEO is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 * 
 * MiDEO is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 */
package org.kramerlab.mideo.estimators.occd;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * GaussianMixture compresses a set of kernels to fewer kernels. First,
 * the kernels are clustered, and then each cluster is compressed into a
 * single kernel. The clustering part is inspired by MStream, an online
 * clustering method that is designed for evolving data streams with
 * mixed attributes. It has been proposed in the paper "Clustering over
 * Evolving Data Streams with Mixed Attributes" by Renxia Wan and Lixin
 * Wang. The actual compression is a method proposed by Jacob Goldberger
 * and Sam Roweis and has been published in the paper "Hierarchical
 * Clustering of a Mixture Model".
 *
 * @author Michael Geilke
 */
public class GaussianMixture implements Serializable {

    private Logger logger = LogManager.getLogger();

    private Map<Integer, MicroCluster> microClusters;

    /**
     * To cluster kernels, we employ MStream and map the kernels into a
     * metric space. This metric space is partitioned into regions with
     * a granularity defined by partitionGranularity.
     */
    private double partitionGranularity;

    /**
     * This is the total number of kernels that have been added. Please
     * notice that we do not take compressions into consideration
     * here. Hence, if 20,000 kernels have been added and 5,000 kernels
     * have been compressed into one kernel, {@code
     * totalNumberOfKernels} would still be 20,000.
     */
    private long totalNumberOfKernels;

    /**
     * This is the current number of kernels, where compressions are
     * taken into account. Hence, if 20,000 kernels have been added and
     * 5,000 kernels have been compressed into one kernel, {@code
     * numberOfKernels} would be 15,001.
     */
    private int numberOfKernels;

    /**
     * To limit the growth of the model size, we compress the kernels
     * regularly. If {@code maxKernels} number of kernels are reached,
     * we initiate a compression.
     */
    private int maxNumberOfKernels;

    /**
     * If sigma has not been set yet, we use a default value that has
     * been estimated from a few instances from a stream prefix.
     */
    private double defaultSigma;

    /**
     * @param maxNumberOfKernels if maxNumberOfKernels many kernels are
     * currently stored, a compression is initiated.
     * @param defaultSigma one parameter of the kernel is the bandwidth
     * sigma. If sigma has not been set yet, we use a default value that
     * has been estimated from a few instances from a stream prefix.
     */
    public GaussianMixture(int maxNumberOfKernels, double defaultSigma) {
	this.maxNumberOfKernels = maxNumberOfKernels;
        this.defaultSigma = defaultSigma;
        this.microClusters = new HashMap<>();
	this.partitionGranularity = 1000;
        this.numberOfKernels = 0;
        this.totalNumberOfKernels = 0;
    }

    /**
     * @param sigma is the bandwidth parameter of the kernel. If sigma
     * has not been set yet, we use a default value that has been
     * estimated from a few instances from a stream prefix.
     */
    public void setDefaultSigma(double sigma) {
        this.defaultSigma = sigma;
    }

    /**
     *
     */
    public int determineNumberOfKernels() {
        int numberOfKernels = 0;
        for (Integer k : microClusters.keySet()) {
            MicroCluster microCluster = microClusters.get(k);
            numberOfKernels += microCluster.size();
        }
        return numberOfKernels;
    }

    /**
     * Adds {@code kernel} to the Gaussian mixture model and initiates a
     * kernel compression if necessary.
     * @param kernel a kernel that is supposed to be added to the
     * Gaussian mixture model.
     */
    public void add(Kernel kernel) {
        // Map the kernel to a position in the metric space and store
        // the kernel into the corresponding micro-cluster. Kernels that
        // have the same position in this space are assumed to belong to
        // the same micro-cluster.
        int position = 0;
	if (kernel.getMean() > partitionGranularity) {
	    position = (int) (kernel.getMean() / partitionGranularity);
	} else {
	    position = (int) (kernel.getMean() * partitionGranularity);
	}
        if (!microClusters.containsKey(position)) {
            MicroCluster microCluster = new MicroCluster();
            microClusters.put(position, microCluster);
        }
        microClusters.get(position).add(kernel);
        numberOfKernels++;
	totalNumberOfKernels++;

        // If the number of kernels exceeded the maximal number of
        // permitted kernels, we initiate a compression.
        try {
            if (numberOfKernels > maxNumberOfKernels) {
                stage1Compression();
                stage2Compression();
            }
        } catch (IllegalArgumentException ex) {
            logger.error(ex.toString());
            System.exit(1);
        }
    }
    
    /**
     * Stage 1 compression: Compresses the kernels in each micro-cluster
     * to a single kernel.  
     */
     private void stage1Compression() throws IllegalArgumentException {
        Map<Integer, MicroCluster> newMicroClusters = new HashMap<>();

        // Compress micro clusters in microClusters.
        for (Integer k : microClusters.keySet()) {
            MicroCluster microCluster = microClusters.get(k);
            // We can assume that microCluster contains at least one
            // element (we know that because we manage the objects).
            int numBins = microCluster.get(0).getWeight().getNumberOfBins();

            MicroCluster newMicroCluster;
            if (microCluster.size() > 1) {
                newMicroCluster = microCluster.compress(defaultSigma, numBins);
            } else {
                newMicroCluster = new MicroCluster();
                newMicroCluster.add(microCluster.get(0)); // cannot be empty
            }
            newMicroClusters.put(k, newMicroCluster);
        }
        microClusters = newMicroClusters;

        // Determines the current number of kernels.
        int numberOfKernels = 0;
        for (MicroCluster microCluster : microClusters.values()) {
            numberOfKernels += microCluster.size();
        }
        this.numberOfKernels = numberOfKernels;
    }

    /**
     * Stage 2: If the number of kernel is above maxNumberOfKernels / 2,
     * we try to compress kernels from adjacent microClusters. Notice
     * that we aim for less than (maxNumberOfKernels / 2) kernels
     * instead of maxNumberOfKernels. This way, we want to reduce the
     * number of times the compression is initiated.
     */
    private void stage2Compression() {
        int l = 2;

        // We increase the partitionGranularity and compress the kernels
        // in the resulting micro clusters.
        while (numberOfKernels > (maxNumberOfKernels / 2)) {
            Map<Integer, MicroCluster> newMicroClusters = new HashMap<>();
            for (Integer k : microClusters.keySet()) {
                MicroCluster microCluster = microClusters.get(k);
                int position = (int) (k / (partitionGranularity * l));
                if (!newMicroClusters.containsKey(position)) {
                    MicroCluster newMicroCluster = new MicroCluster();
                    newMicroClusters.put(position, newMicroCluster);
                }
                for (Kernel kernel : microCluster) {
                    newMicroClusters.get(position).add(kernel);
                }
            }
            this.microClusters = newMicroClusters;
            stage1Compression();
            l++;
        }

        // If stage2 compression has been used, re-hash micro-clusters using
        // partitionGranularity.
        if (l > 2) {
            Map<Integer, MicroCluster> newMicroClusters = new HashMap<>();
            for (Integer k : microClusters.keySet()) {
                int position = (int) (k / (partitionGranularity * l));
                newMicroClusters.put(position, microClusters.get(k));
            }
            this.microClusters = newMicroClusters;
        }
    }

    /**
     * Computes {@literal f_{kernel} (y | X) = \frac{1}{n} \sum_{i=0}^n
     * w(y_i | X) \cdot N(y; y_i, sigma_kernel^2)} where {@literal
     * sigma_kernel = \frac{sigma_x}{n^{\frac{1}{4}}}}.
     * @param y target variable
     * @param w weights of the bins of the target variable
     */
    public double evaluate(double y, double[] w) 
            throws IllegalArgumentException {
        double p = 0.0;
        for (MicroCluster microCluster : microClusters.values()) {
            for (Kernel kernel : microCluster) {
		kernel.setVariance(defaultSigma); // TODO always possible?
                double value = kernel.evaluate(y, w);
                p += kernel.evaluate(y, w);
            }
        }

        return p / totalNumberOfKernels;
    }
    
    public class MicroCluster extends ArrayList<Kernel> {

        /**
         * Compresses the kernel in the micro cluster to a micro cluster
         * with only one component. It is an implementation of a method
         * proposed by Goldberger and Roweis, who presented an approach
         * that allowed to compress a Gaussian mixture model with k
         * d-dimensional components to a l components. For now, we only
         * consider a Gaussian mixture with one-dimensional components,
         * which are compressed to one component.
         *
         * Jacob Goldberger and Sam Roweis. "Hierarchical Clustering of a
         * Mixture Model".
         *
         * @param defaultSigma the default bandwidth of the compression
         * kernel, in case some kernels do not have a bandwidth yet.
         * @param numBins number of bins used to discretize the
         * continuous variable
         * @return a micro cluster containing the compression kernel,
         * which is the kernel compression of this micro cluster.
         */
        public MicroCluster compress(double defaultSigma, int numBins) 
                throws IllegalArgumentException {

            // Given is a Gaussian mixture model of the form 
            // f(y) = \sum_{i=1}^k \alpha_i N(y; \mu_i, \sigma_i).
            // We compress f(y) to
            // f^\prime(y) = \sum_{j=1}^l \beta_j N(y;\mu^\prime_j,
            // \sigma^\prime_j).  
            // As noted above, $l$ is in our case $1$.

            // beta_j = \sum_{i=1}^k \alpha_i
            KernelFactor beta = new KernelFactor(numBins);
            for (Kernel kernel : this) {
                beta.add(kernel.getWeight());
            }

            // \mu^\prime_j = \frac{1}{\beta_j} \sum_{i=1}^k \alpha_i
            // \mu_i
            //
            // \alpha_i contains variables that are not known when
            // compressing kernels. Therefore, we set these variables to
            // 1, thereby assuming that each original kernel has the
            // same weight. If a kernel resulted from a compression, the
            // weight of this compressed kernel is the number of kernels
            // from which the compression has been created.
            double muPrime = 0.0;
            double normalizingFactor = 0.0;
            for (Kernel kernel : this) {
                double mu_i = kernel.getMean();
                double alpha_i = determineAlphaI(kernel.getWeight());
                muPrime += alpha_i * mu_i;
                normalizingFactor += alpha_i;
            }
            muPrime /= normalizingFactor;

            // \sigma^\prime_j = \frac{1}{\beta_j} \sum_{i=1}^k \alpha_i
            // \cdot (\sigma_i + (\mu_i - muPrime_j)^2)
            double sigmaPrime = 0.0;
            for (Kernel kernel : this) {
                double mu_i = kernel.getMean();
                double alpha_i = determineAlphaI(kernel.getWeight());
                double sigma_i;
                if (kernel.getVariance() == null) {
                    sigma_i = defaultSigma;
                } else {
                    sigma_i = kernel.getVariance();
                }
                sigmaPrime += alpha_i * (sigma_i + Math.pow(mu_i-muPrime, 2));
            }
            sigmaPrime /= normalizingFactor;
            sigmaPrime = Math.sqrt(sigmaPrime);

            // Create compressed Gaussian mixture model.
            Kernel kernel = new Kernel(muPrime);
            kernel.setWeight(beta);
            kernel.setVariance(sigmaPrime);
            MicroCluster compressed = new MicroCluster();
            compressed.add(kernel);
	
            return compressed;
        }

        private double determineAlphaI(KernelFactor factor) {
            double alpha_i = 0.0;
            for (int j = 0; j < factor.getNumberOfBins(); j++) {
                alpha_i += factor.getMultiplier(j);
            }
            return alpha_i;
        }
    }
}
