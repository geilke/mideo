/**
 * MiDEO: a framework to perform data mining on probabilistic condensed 
 * representations
 * Copyright (C) 2014 Michael Geilke
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
 */
package org.kramerlab.mideo.core.discretization;

import java.util.Random;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.IntStream;

import weka.core.Utils;
import weka.core.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PartitionIncremental uses a more fine-grained discretization to
 * perform equal-width and equal-frequency discretization in an online
 * fashion.
 *
 * <p>The algorithm is based on the paper "Discretization from Data
 * Streams: Applications to Histograms and Data Mining" by Joao Gama and
 * Carlos Pinto. The main idea is to keep a more fine-grained
 * discretization of the continuous variable, which has many more bins
 * than the final one and each bin contains no more than a certain
 * number of instances. When a bin contains more instances, the bin is
 * split into two bins. The final discretization is constructed on
 * demand by combining the more fine-grained bins.</p>
 *
 * <p>In the final discretization, {@literal b0} is negative infinity
 * and {@literal bk} is positive infinity. {@literal b1} is smallest
 * value observed and {@literal b{k-1}} is the largest value
 * observed. Hence, by definition, the first and the last bin are for
 * outliers.
 *
 * @author Michael Geilke
 */
public class PartitionIncremental implements OnlineDiscretization {

    private static final String WRONG_INDEX_b = "For the index, " + 
        "the following has to hold: 0 <= index < getNumberOfBins.";
    private static final String WRONG_k = "the number of bins has to be " +
        "larger than 2.";
    
    private Logger logger;
    private Random random;

    // parameters
    private DiscretizationType discretizationType;
    private List<Double> buffer; // buffer for values
    private int recomputationThreshold; // maximal buffer size
    private int splitThreshold; // maximal number of values per Layer1 bin

    // the number of bins
    private int k; 
    // contains the currents borders of Layer1
    private Double[] b1; 
    // bin counts for Layer1
    private Long[] n1;
    // contains the current borders b0 to bk (belonging to Layer2)
    private Double[] b2; 
    // bin counts for Layer2
    private Long[] n2; 
    // total number of counts
    private long observationCounter;
    
    /**
     * @param k final number of bins for the discretization
     * @param type the type of discretization
     * @param min an initial value for the minimal value of the variable
     * @param max an initial value for the maximal value of the variable
     */
    public PartitionIncremental(int k, DiscretizationType type, double min,
                                double max) throws IllegalArgumentException {
        init(k, type, min, max);        
    }

    /**
     * @param k final number of bins for the discretization
     * @param type the type of discretization
     * @param observations a non-empty list of observations
     */
    public PartitionIncremental(int k, DiscretizationType type, 
            List<Double> observations) throws IllegalArgumentException {

        if (observations == null) {
            throw new IllegalArgumentException("Observations required!");
        }

        // Determine minimal and maximal observations. Since we only have
        // a small sample, we correct min and max slightly.
        double minValue = observations.stream().min(Double::compare).get();
        double maxValue = observations.stream().max(Double::compare).get();
        double range = maxValue - minValue;
        minValue -= (0.1 * range == 0) ? 0.1 : 0.1 * range;
        maxValue += (0.1 * range == 0) ? 0.1 : 0.1 * range;

        // initialize
        init(k, type, minValue, maxValue);
        observations.forEach(obs -> addObservation(obs));
    }

    /* Sets default values and initializes Layer1 and Layer2. */
    private void init(int k, DiscretizationType type, double min, 
                      double max) throws IllegalArgumentException {

        this.logger = LogManager.getLogger();
        this.random = new Random();

        if (k <= 2) {
            throw new IllegalArgumentException(WRONG_k);
        }

        // initialize parameters
        this.buffer = new ArrayList<>();
        this.discretizationType = type;
        this.observationCounter = 0;
        this.k = k;
        setRecomputationThreshold(50);
        setSplitThreshold(100);

        // set borders for Layer2
        this.b2 = new Double[k + 1];
        this.b2[0] = Double.NEGATIVE_INFINITY;
        this.b2[1] = min;
        this.b2[k] = Double.POSITIVE_INFINITY;
        double step = (max - min) / k;
        for (int i = 2; i < k; i++) {
            b2[i] = b2[1] + (i * step);
        }

        // initialize counts for Layer2 discretization
        n2 = new Long[k];
        for (int i = 0; i < n2.length; i++) {
            n2[i] = 0L;
        }

        // Layer1 is a discretization into many more bins than the final
        // discretization. It is initialized with an equal-width
        // strategy.
        this.n1 = new Long[k * k];
        for (int i = 0; i < n1.length; i++) {
            n1[i] = 0L;
        }
        this.b1 = new Double[n1.length + 1];
        step = (max - min) / n1.length;
        this.b1[0] = Double.NEGATIVE_INFINITY;
        this.b1[1] = min;
        this.b1[n1.length] = Double.POSITIVE_INFINITY;
        for (int i = 2; i < n1.length; i++) {
            b1[i] = b1[1] + (i * step);
        }
    }

    @Override
    public DiscretizationType getDiscretizationType() {
        return discretizationType;
    }

    /**
     * Specifies the number of observations before a recomputation takes
     * place. Until then, the instances are stored in a buffer.
     *
     * @param threshold number of observations until the borders are
     *     recomputed
     */
    public void setRecomputationThreshold(int threshold) {
        this.recomputationThreshold = threshold;
    }

    /**
     * Specifies the number of values that a bin of the more
     * fine-grained discretization may contain. If this number is
     * exceeded, the bin is split into two.
     * 
     * @param threshold the number of values that a bin of the more
     *     fine-grained discretization may contain
     */
    public void setSplitThreshold(int threshold) {
        this.splitThreshold = threshold;
    }

    /**
     * Adds the observation and updates the discretization.
     *
     * @param obs new observation for the continuous variable
     */
    public void addObservation(double obs) {
        buffer.add(obs);
        if (buffer.size() >= recomputationThreshold) {
            processBuffer();
        }
    }

    /**
     * Processes the observation that are currently in the buffer and
     * clears the buffer.
     */
    public void processBuffer() {
        for (Double o : buffer) {
            updateLayer1(o);
        }
        updateLayer2();
        buffer.clear();
    }

    /* Check the pseudo code for more explanations: It is available in
     * the paper "Discretization from Data Streams: Applications to
     * Histograms and Data Mining" by Joao Gama and Carlos Pinto.
     */
    private void updateLayer1(double x) {
        logger.info("updateLayer1 with {}", () -> Double.toString(x));

        // update lower and upper border if necessary and find position
        // for the bin of x
        double step = (b1[b1.length - 2] - b1[1]) / n1.length;
        int bin;
        if (x < b1[1]) {
            // smallest observation so far 
            // (step is as a correction)
            b1[1] = x - step;
            bin = 1;
        } else if (x > b1[b1.length - 2]) {
            // largest observation so far
            // (step is as a correction)
            b1[b1.length - 2] = x + step;
            bin = b1.length - 2;
        } else {
            // an observation in between
            bin = 1 + (int) ((x - b1[1]) / step);
            bin = Math.min(bin, b1.length - 2);
        }
        while (x < b1[bin - 1]) {
            bin--;
        }
        while (x > b1[bin]) {
            bin++;
        }

        // update statistics
        n1[bin - 1]++;
        logger.info(x);
        logger.info("Selected bin: " + Integer.toString(bin - 1));
        logger.info("Bin count: " + Long.toString(n1[bin - 1]));
        observationCounter++;

        // split the bin if necessary
        if ((1 + n1[bin - 1]) > splitThreshold) {
            // perform split on b1 and n1
            Long[] n1Prime = new Long[n1.length + 1];
            Double[] b1Prime = new Double[b1.length + 1];
            long count = n1[bin - 1] / 2;
            n1[bin - 1] = n1[bin - 1] - count;
            performSplit(b1, b1Prime, bin);
            if (bin == 1) {
                // first bin
                b1Prime[1] = b1[1] - step;
                performSplit(n1, n1Prime, 0);
                n1Prime[0] = count;
            } else if (bin == n1.length - 1) {
                // last bin
                b1Prime[bin] = b1[bin - 1] + step;
                performSplit(n1, n1Prime, n1Prime.length - 1);
                n1Prime[n1Prime.length - 1] = count;
            } else {
                // bin in between
                double newB = (b1[bin-1] + b1[bin]) / 2;
                b1Prime[bin] = newB;
                performSplit(n1, n1Prime, bin - 1);
                n1Prime[bin - 1] = count;
            }
            n1 = n1Prime;
            b1 = b1Prime;
        }
    }

    /* Inserts element at the specified position.
     *
     * @param src is the original array
     * @param dest is an empty array that has src.length + 1 as length
     * @param position is the position in the array at which the element
     * will be inserted.
     */
    private void performSplit(Object src, Object dest, int position) {

        Object[] srcConverted = (Object[]) src;

        if (position == 0) {
            System.arraycopy(src, 0, dest, 1, srcConverted.length);
        } else if (position == srcConverted.length) {
            System.arraycopy(src, 0, dest, 0, srcConverted.length);
        } else {
            System.arraycopy(src, 0, dest, 0, position);
            System.arraycopy(src, position, dest, position + 1, 
                             srcConverted.length - position);
        }
    }

    /* Updates Layer2 based on Layer1. It traverses Layer1 and merges
     * the bins to obtain a equal-frequency or equal-width
     * discretization with the desired number of bins, which is k.
     */
    private void updateLayer2() {
        logger.info("updateLayer2");

        if (discretizationType == DiscretizationType.EQUAL_FREQUENCY) {
            // bin 1 to k - 1
            b2[0] = b1[0];
            int bin = 1;
            long currCount = 0;
            long prevCount = 0;
            double observationsPerBin = observationCounter / (double) k;
            for (int i = 1; i < b1.length - 1; i++) {
                currCount += n1[i - 1];
                if (bin < k && currCount >= bin * observationsPerBin) {
                    n2[bin - 1] = currCount - prevCount;
                    b2[bin] = b1[i];
                    prevCount = currCount;
                    bin++;
                }
            }
            // the remaining values go into bin k
            b2[k] = b1[b1.length - 1];
            n2[k - 1] = currCount - prevCount;

        } else if (discretizationType == DiscretizationType.EQUAL_WIDTH) {
            // b2_1 and b2_{k-1}
            b2[1] = b1[1];
            b2[b2.length - 2] = b1[b1.length - 2];
            // remaining borders and n2
            int bin = 1;
            long currCount = 0;
            long prevCount = 0;
            double step = (b2[b2.length - 2] - b2[1]) / (k - 1);
            for (int i = 1; i < b2.length - 1; i++) {
                b2[i] = b2[1] + (i - 1) * step;
                while (bin < b1.length - 1 && b1[bin] < b2[i]) {
                    currCount += n1[bin - 1];
                    bin++;
                }
                n2[i - 1] = currCount - prevCount;
                prevCount = currCount;
            }
        }
    }

    @Override
    public int getNumberOfBins() {
        return k;
    }

    @Override
    public double getBorder(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index > getNumberOfBins()) {
            throw new IndexOutOfBoundsException(WRONG_INDEX_b);
        }
        return b2[index];
    }

    @Override
    public long getBinCount(int index) throws IndexOutOfBoundsException {
        return n2[index];
    }

    /**
     * {@inheritDoc} It simulates observations from the interval
     * {@literal [bi; b{i+1})} by drawing them uniformly at random.
     */
    @Override
    public void addCountToBin(int index, long count)
	throws IllegalArgumentException {
        double diff = (b2[index + 1] - b2[index]);
        double offset = b2[index];
        for (int i = 0; i < count; i++) {
            double r = random.nextDouble() * diff + offset;
            addObservation(r);
        }
        processBuffer();
    }

    @Override
    public double[] getBinDistribution() {
        double[] dist = new double[getNumberOfBins()];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = getBinCount(i) + 1;  // + 1 for laplace correction
        }
        Utils.normalize(dist);
        return dist;
    }

    @Override
    public double drawRandomValueFromBin(int index) 
        throws IllegalArgumentException {
        if (index < 0 || index > getNumberOfBins()) {
            throw new IllegalArgumentException(WRONG_INDEX_b);
        }
        // convert borders to actual double values if necessary
        double l = getBorder(index);
        double u = getBorder(index + 1);
        if (l == Double.NEGATIVE_INFINITY) {
            l = Double.MIN_VALUE;
        }
        if (u == Double.POSITIVE_INFINITY) {
            u = Double.MAX_VALUE;
        }
        // draw random value
        return random.nextDouble() * (u - l) + l;
    }
}

