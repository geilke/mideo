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
package org.kramerlab.mideo.evaluation.measures;

/**
 * A class implementing <code>PerformanceMeasure</code> allows to
 * evaluate the performance of an estimator based on a given stream.
 *
 * When invoking the method <code>evaluate</code>, the object should
 * have been fully initialized with a stream, an estimator, and possibly
 * other parameters. After <code>evaluate</code> has been successfully
 * invoked, the performance can be retrieved by invoking
 * <code>getResult</code>.
 *
 * @author Michael Geilke
 */
public interface PerformanceMeasure {

    /**
     * Trains the estimator with instances from the stream and performs
     * the evaluation. Which and how many instances are used depends on
     * the performance measure.
     */
    void evaluate();

    /**
     * Computes the value of the performance measure. Notice that
     * <code>evaluate</code> has to be invoked before calling
     * <code>getResult</code>.
     * @return the result of the evaluation
     */
    double getResult();
}
