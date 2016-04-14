/*
 * DoubleVector.java
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package org.kramerlab.mideo.classifiers.core;

import moa.AbstractMOAObject;
import moa.core.StringUtils;

/**
 * Vector of double numbers with some utilities.
 *
 * In particular DoubleVector supports Laplace correction, which is set
 * to 0 by default. The Laplace correction is a non-negative integer
 * that introduces a correction value for unobserved values. If a value
 * has not been observed so far, the corresponding count would be
 * 0. With the Laplace correction, however, the corresponding count is
 * initialized with a user-defined value. Please notice that the
 * correction is only introduced for initializing and expanding the
 * DoubleVector. Other methods such as {@link #sumOfAbsoluteValues}
 * still only consider all values up to the maximal observed value. Also
 * notice that we implemented the laplace correction as a global
 * parameter (static getter and setter method) to avoid extensive
 * changes in other MOA classes.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Michael Geilke
 */
public class DoubleVector extends AbstractMOAObject {

    private static final long serialVersionUID = 1L;
    private static double laplaceCorrection = 0;
    
    protected double[] array;

    public DoubleVector() {
        this.array = new double[0];
    }

    /**
     * @param toCopy the double array that is supposed to be copied
     */
    public DoubleVector(double[] toCopy) {
        this.array = new double[toCopy.length];
        System.arraycopy(toCopy, 0, this.array, 0, toCopy.length);
        for (int i = 0; i < toCopy.length; i++) {
            array[i] = DoubleVector.getLaplaceCorrection();
        }
    }

    /**
     * @param toCopy the DoubleVector that is supposed to be copied
     */
    public DoubleVector(DoubleVector toCopy) {
        this(toCopy.getArrayRef());
    }

    public int numValues() {
        return this.array.length;
    }

    public void setValue(int i, double v) {
        if (i >= this.array.length) {
            setArrayLength(i + 1);
        }
        this.array[i] = v;
    }

    public void addToValue(int i, double v) {
        if (i >= this.array.length) {
            setArrayLength(i + 1);
        }
        this.array[i] += v;
    }

    public void addValues(DoubleVector toAdd) {
        addValues(toAdd.getArrayRef());
    }

    public void addValues(double[] toAdd) {
        if (toAdd.length > this.array.length) {
            setArrayLength(toAdd.length);
        }
        for (int i = 0; i < toAdd.length; i++) {
            this.array[i] += toAdd[i];
        }
    }

    public void subtractValues(DoubleVector toSubtract) {
        subtractValues(toSubtract.getArrayRef());
    }

    public void subtractValues(double[] toSubtract) {
        if (toSubtract.length > this.array.length) {
            setArrayLength(toSubtract.length);
        }
        for (int i = 0; i < toSubtract.length; i++) {
            this.array[i] -= toSubtract[i];
        }
    }

    public void addToValues(double toAdd) {
        for (int i = 0; i < this.array.length; i++) {
            this.array[i] = this.array[i] + toAdd;
        }
    }

    public void scaleValues(double multiplier) {
        for (int i = 0; i < this.array.length; i++) {
            this.array[i] = this.array[i] * multiplier;
        }
    }

    /**
     * Returns the count for the value at index {@code i}. Notice that
     * {@code DoubleVector.getLaplaceCorrection} is returned for
     * unobserved values.
     *
     * @param i index of element in DoubleVector
     * @return the value at index {@code i}
     */
    public double getValue(int i) {
        if ((i >= 0) && (i < this.array.length)) {
            return this.array[i];
        } else {
            return DoubleVector.getLaplaceCorrection();
        }
    }

    public double sumOfValues() {
        double sum = 0.0;
        for (double element : this.array) {
            sum += element;
        }
        return sum;
    }
    
    public double sumOfAbsoluteValues() {
        double sum = 0.0;
        for (double element : this.array) {
            sum += (element > 0.0) ? element : -element;
        }
        return sum;
    }

    public int maxIndex() {
        int max = -1;
        for (int i = 0; i < this.array.length; i++) {
            if ((max < 0) || (this.array[i] > this.array[max])) {
                max = i;
            }
        }
        return max;
    }

    public void normalize() {
        scaleValues(1.0 / sumOfAbsoluteValues());
    }

    public int numNonZeroEntries() {
        int count = 0;
        for (double element : this.array) {
            if (element != DoubleVector.getLaplaceCorrection()) {
                count++;
            }
        }
        return count;
    }

    public double minWeight() {
        if (this.array.length > 0) {
            double min = this.array[0];
            for (int i = 1; i < this.array.length; i++) {
                if (this.array[i] < min) {
                    min = this.array[i];
                }
            }
            return min;
        }
        return 0.0;
    }

    public double[] getArrayCopy() {
        double[] aCopy = new double[this.array.length];
        System.arraycopy(this.array, 0, aCopy, 0, this.array.length);
        return aCopy;
    }

    public double[] getArrayRef() {
        return this.array;
    }

    protected void setArrayLength(int l) {
        double[] newArray = new double[l];
        int numToCopy = this.array.length;
        if (numToCopy > l) {
            numToCopy = l;
        }
        System.arraycopy(this.array, 0, newArray, 0, numToCopy);
        if (l > this.array.length) {
            for (int i = this.array.length; i < l; i++) {
                newArray[i] = DoubleVector.getLaplaceCorrection();
            }
        }
        this.array = newArray;
    }

    public void getSingleLineDescription(StringBuilder out) {
        getSingleLineDescription(out, numValues());
    }

    public void getSingleLineDescription(StringBuilder out, int numValues) {
        out.append("{");
        for (int i = 0; i < numValues; i++) {
            if (i > 0) {
                out.append("|");
            }
            out.append(StringUtils.doubleToString(getValue(i), 3));
        }
        out.append("}");
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        getSingleLineDescription(sb);
    }

    /**
     * The initial count for unobserved values. Please notice that this
     * value will be used by all DoubleVector instances.
     * @param laplaceCorrection the initial count for unobserved values
     */
    public static void setLaplaceCorrection(double laplaceCorrection) {
        DoubleVector.laplaceCorrection = laplaceCorrection;
    }

    /**
     * @return the initial count for unobserved values
     */
    public static double getLaplaceCorrection() {
        return DoubleVector.laplaceCorrection;
    }
}
