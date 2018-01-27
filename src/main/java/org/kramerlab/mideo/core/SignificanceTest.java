/**
 * MiDEO: a framework to perform data mining on probabilistic condensed 
 * representations
 * Copyright (C) 2016 Michael Geilke
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
package org.kramerlab.mideo.core;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.yahoo.labs.samoa.instances.Instance;

import org.kramerlab.mideo.estimators.DensityEstimator;

/**
 * This class provides statistical tests that are based on a signifance
 * level.
 */
public class SignificanceTest {

    /**
     * Performs the Wilcoxon rank-sum test on sample1 and sample2. It is
     * assumed that sample1 and sample2 are some measurements of object1
     * and object2, respectively. It is further assumed, that the
     * measurements are i.i.d. observations and that |sample1| =
     * |sample2|.
     * 
     * @param sample1 sample of object1
     * @param sample2 sample of object2
     * @param alpha significance level
     * @return true if sample1 and sample2 are comming from the same
     *     distribution, and false otherwise
     */
    public static boolean wilcoxonRankSumTest(List<Double> sample1,
        				      List<Double> sample2,
        				      double alpha) {
	
        // Rank samples
        Double[] s1 = new Double[sample1.size()];
        sample1.toArray(s1);
        Arrays.sort(s1);
        Double[] s2 = new Double[sample2.size()];
        sample2.toArray(s2);
        Arrays.sort(s2);

        List<Pair<Integer, Double>> R = new ArrayList<Pair<Integer,Double>>();
        int index1 = 0;
        int index2 = 0;
        while (index1 < s1.length || index2 < s2.length) {
            if (index1 >= s1.length) {
        	R.add(new Pair(2, s2[index2]));
        	index2++;
            } else if (index2 >= s2.length) {
        	R.add(new Pair(1, s1[index1]));
        	index1++;
            } else if (s1[index1] <= s2[index2]) {
        	R.add(new Pair(1, s1[index1]));
        	index1++;
            } else {
        	R.add(new Pair(2, s2[index2]));
        	index2++;
            }
        }

        // Resolve ties
        List<Pair<Integer, Double>> ties; // stores indices plus average
                                          // ranks
        ties = new ArrayList<Pair<Integer, Double>>();
        int start = 0;
        int end = 0;
        for (int i = 0; i < R.size(); i++) {
            if (i > 0) {
        	if (start != end &&
        	    R.get(i - 1).getSecond() != R.get(i).getSecond()) {
        	    // if a sequence of equal values ends
		    
        	    // compute average rank
        	    double avg = 0.0;
        	    for (int j = start; j <= end; j++) {
        		avg += j;
        	    }
        	    avg /= end - start + 1;
        	    for (int j = start; j <= end; j++) {
        		ties.add(new Pair(j, avg));
        	    }
        	    start = end;

        	} else if (R.get(i - 1).getSecond() == R.get(i).getSecond()) { 
        	    // if values are equal

        	    if (start != end) {
        		end++;
        	    } else {
        		start = i - 1;
        		end = i;
        	    }
        	}
            }
        }

        // compute r1
        double[] r1 = new double[s1.length];
        int index = 0;
        for (int j = 0; j < R.size(); j++) {
            if (R.get(j).getFirst() == 1) {
        	double rank = j;
        	// look for average rank in ties and correct rank if
        	// necessary
        	for (int k = 0; k < ties.size(); k++) {
        	    if (ties.get(k).getFirst() == j) {
        		rank = ties.get(k).getSecond();
        	    }
        	}
        	r1[index] = rank;
        	index++;
            }
        }
	
        // perform test
        int n1 = sample1.size();
        int n2 = sample2.size();
        int n = n1 + n2;
        int R1 = 0;
        for (int i = 0; i < r1.length; i++) {
            R1 += r1[i];
        }
        double mu = ((double)n1 * (n1 + n2 + 1)) / 2;
        double sigma = Math.sqrt(((double)n1 * n2 * (n1 + n2 + 1)) / 12);
        double z = Math.abs(R1 - mu) / sigma;
        return 1 - integrateGaussian(z, 0, 1) > alpha;
    }
    
    /*
     * Evaluates the normal density at {@code x}.
     * @param x x value
     * @param mu the mean
     * @param sigma standard deviation
     * @return N(x; mu, sigma)
     */
    private static double gaussian(double x, double mu, double sigma) {
	double multiplier = (1 / (sigma * Math.sqrt(2 * Math.PI)));
	double exp = -0.5 * Math.pow(x - mu, 2) / Math.pow(sigma, 2);
	return multiplier * Math.exp(exp);
    }

    /*
     * Approximates the integral of gaussian(x; mu, sigma) over
     * (-\infty; x].
     * @param x x value
     * @param mu the mean
     * @param sigma standard deviation
     * @return integral of gaussian(x; mu, sigma) over (-\infty; x]
     */
    private static double integrateGaussian(double x, double mu, double sigma) {

	double xStart = -100 - mu - 3 * sigma;  // heuristic
	double xEnd = x;
	double stepSize = 0.1;

	double integral = 0.0;
	for (double i = xStart; i < xEnd; i = i + stepSize) {
	    double p = gaussian(i, mu, sigma);
	    integral += p * stepSize;
	}
	
	return integral;
    }

    /**
     * Checks whether {@code sample1} and {@code sample2} are coming
     * from the same distribution.
     *
     * @param sample1 first sample
     * @param sample2 second sample
     * @param f the density estimator with which the density values are
     * computed
     * @param alpha signifiance level for the underlying Wilcoxon
     * rank-sum test
     * @return true iff the Wilcoxon rank-sum test with significance
     * level {@code alpha} returns true for {@code sample1} and {@code
     * sample2}
     */
    public static boolean equalDensities(List<Instance> sample1, 
        				 List<Instance> sample2,
        				 DensityEstimator f, 
        				 double alpha) throws Exception {

        if (sample1.size() != sample2.size()) {
            throw new Exception("Sample size does not match!");
        }

        // compute log-likelihoods
        ArrayList<Double> logs1 = new ArrayList<Double>();
        for (Instance inst : sample1) {
            logs1.add(Math.log(f.getDensityValue(inst)));
        }

        ArrayList<Double> logs2 = new ArrayList<Double>();
        for (Instance inst : sample2) {
            logs2.add(Math.log(f.getDensityValue(inst)));
        }

        // perform wilcoxon rank sum test
        return SignificanceTest.wilcoxonRankSumTest(logs1, logs2, alpha);
    }

    public static void main(String[] args) throws Exception {

        // System.out.println(SignificanceTest.gaussian(0, 0, 1));
        // System.out.println(SignificanceTest.integrateGaussian(0, 0, 1));
    	// if (args.length == 2) {
    	//     String f1 = args[0];
    	//     String f2 = args[1];

    	//     ArrayList<Double> s1 = new ArrayList<>();
    	//     BufferedReader br = new BufferedReader(new FileReader(f1));
    	//     String line = null;
    	//     while ((line = br.readLine()) != null) {
    	// 	s1.add(Double.parseDouble(line));
    	//     }
    	//     br.close();
	    
    	//     ArrayList<Double> s2 = new ArrayList<>();
    	//     br = new BufferedReader(new FileReader(f2));
    	//     while ((line = br.readLine()) != null) {
    	// 	s2.add(Double.parseDouble(line));
    	//     }
    	//     br.close();

    	//     boolean eql = SignificanceTest.wilcoxonRankSumTest(s1, s2, 0.05);
    	//     System.out.println(eql);
    	// }
    }
}
