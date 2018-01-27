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

import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.kramerlab.mideo.core.RandomVariable;

/**
 * Computes the normalized mutual information in an online fashion.
 *
 * @author Michael Geilke
 */
public class MutualInformation {

    private static Logger logger = LogManager.getLogger();
    
    private RandomVariable variable1;
    private RandomVariable variable2;

    // TODO: What about variables with an extremely large number of values?
    private int maxBufferSize = 75;
    private double initialCount = 0.25;
    
    private LinkedList<Double> variable1Buffer;
    private LinkedList<Double> variable2Buffer;

    public MutualInformation(RandomVariable var1, RandomVariable var2) {
        this.variable1 = var1;
        this.variable1Buffer = new LinkedList<>();
        this.variable2 = var2;
        this.variable2Buffer = new LinkedList<>();
    }

    public RandomVariable getVariable1() {
        return variable1;
    }

    public RandomVariable getVariable2() {
        return variable2;
    }

    public void update(double val1, double val2) {
        if (variable1Buffer.size() > maxBufferSize) {
            variable1Buffer.remove();
        }
        if (variable2Buffer.size() > maxBufferSize) {
            variable2Buffer.remove();
        }

        variable1Buffer.add(val1);
        variable2Buffer.add(val2);
    }

    public double getNormalizedMutualInformation() {
        // determine counts of variable values
        double instanceCounter = 0;
        double[] countsX = new double[variable1.getAttribute().numValues()];
        double[] countsY = new double[variable2.getAttribute().numValues()];
        double[][] countsXY = new double[countsX.length][countsY.length];

	// add a small initial count to all possible values
	for (int i = 0; i < countsX.length; i++) {
	    countsX[i] = initialCount;
	}
	for (int i = 0; i < countsY.length; i++) {
	    countsY[i] = initialCount;
	}
	for (int i = 0; i < countsXY.length; i++) {
	    for (int j = 0; j < countsXY[i].length; j++) {
		countsXY[i][j] = initialCount;
                instanceCounter += initialCount;
	    }
	}

	// counts from buffer
        long numValues = Math.min(variable1Buffer.size(), 
                                  variable2Buffer.size());
        for (int i = 0; i < numValues; i++) {
            double val1 = variable1Buffer.get(i);
            double val2 = variable2Buffer.get(i);
            countsX[(int)(val1)]++;
            countsY[(int)(val2)]++;
            countsXY[(int)(val1)][(int)(val2)]++;
            instanceCounter++;
        }

        // compute mutual information and entropies
        double I = 0.0;
        double H_X = 0.0;
        double H_Y = 0.0;
	for (int x = 0; x < countsX.length; x++) {
	    for (int y = 0; y < countsY.length; y++) {
		double p_x = (double)(countsX[x]) / instanceCounter;
                H_X -= p_x * (Math.log(p_x) / Math.log(2));
		double p_y = (double)(countsY[y]) / instanceCounter;
                H_Y -= p_y * (Math.log(p_y) / Math.log(2));
		double p_xy = (double)(countsXY[x][y]) / instanceCounter;
		if (p_xy > 0) {
		    I += p_xy * Math.log(p_xy / (p_x * p_y)) / Math.log(2);
		}
	    }
	}
	return (2 * I) / (H_X + H_Y);
    }

    public String toString() {
        String s = "";
        s += variable1.getName();
        s += variable2.getName();
        return s;
    }
}
