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
package org.kramerlab.mideo.core;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.DiscreteRandomVariable;
import org.kramerlab.mideo.core.ContinuousRandomVariable;

/**
 * Provides some general funtionality that is useful for various types
 * of classes.
 *
 * @author Michael Geilke
 */
public class Utils {

    /**
     * Generates random variables from the attributes described by
     * {@code header}. The ordering of the random variables follow
     * exactly the ordering of the attributes in {@code header}. Please
     * notice that only nominal and numeric attributes are
     * supported. All other attributes result in {@code null}.
     *
     * @param header an attribute discription of a data stream
     * @return a list of variables generated from the attribute in
     * {@code header}, in exactly the same order
     */
    public static List<RandomVariable> getRandomVariables(
            InstancesHeader header) {
        List<RandomVariable> vars = new ArrayList<>();
        for (int i = 0; i < header.numAttributes(); i++) {
            Attribute att = header.attribute(i);
            String name = att.name();
            RandomVariable var = null;
            if (att.isNominal()) {
                var = new DiscreteRandomVariable(name, att);
            } else if (att.isNumeric()) {
                var = new ContinuousRandomVariable(name, att);
            }
            vars.add(var);
        }
        return vars;
    }

    /**
     * Checks whether {@code vars} only has attributes from {@code
     * header}.
     *
     * @param header an attribute discription of a data stream
     * @param vars random variables 
     * @return true iff the underlying attributes of {@code vars} is a
     * subset of the attributes of {@code header}
     */
    public static boolean isCompatible(InstancesHeader header, 
                                       List<RandomVariable> vars) {
        Set<Attribute> atts = new HashSet<>();
        for (int i = 0; i < vars.size(); i++) {
            if (header.attribute(i) == vars.get(i).getAttribute()) {
                atts.add(vars.get(i).getAttribute());
            }
        }
        return atts.size() == vars.size();
    }

    /**
     * Normalizes {@code d}. It is assumed that {@code d} are
     * non-negative values.
     * @param d an array of non-negative values
     * @return dPrime with {@literal d[i] / sum_{0 <= i < d.length}
     * d[i]}
     */
    public static double[] normalize(double[] d) {
        double[] dPrime = new double[d.length];
        double sum = 0.0;
        for (int i = 0; i < d.length; i++) {
            sum += d[i];
        }
        for (int i = 0; i < dPrime.length; i++) {
            dPrime[i] = d[i] / sum;
        }
        return dPrime;
    }

    /**
     * Determines the attribute index of attribute {@code att} in {@code
     * header}. In principle, header already provides this
     * functionality, but it does not seem to work with the SAMOA
     * instances so far. Therefore, we provide this functionality as
     * util method. As soon as a bug fix is provided for SAMOA, we
     * update this method.
     *
     * @param header the header of the data stream
     * @param att the name of the attribute
     * @return the first occurrence of an attribute with name {@code att}
     * in header. If no such attribute is found, -1 is returned.
     */
    public static int determineAttributeIndex(InstancesHeader header, 
                                              String att) {
        for (int i = 0; i < header.numAttributes(); i++) {
            // This looks a little bit complicated, but the SAMOA
            // instance are not working properly right now.
            if (header.attribute(i).name().equals(att)) {
                return i;
            }
        }
        return -1;
    }

}
