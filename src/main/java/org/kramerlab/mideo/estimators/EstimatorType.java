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
package org.kramerlab.mideo.estimators;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.Utils;
import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.DiscreteRandomVariable;
import org.kramerlab.mideo.core.ContinuousRandomVariable;
import org.kramerlab.mideo.data.streams.Stream;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;

/**
 * Allows to distinguish different types of conditional density
 * estimators. The variables on which the estimator is conditioned can
 * be of different types, i.e., discrete and / or continuous random
 * variables.
 *
 * @author Michael Geilke
 */
public enum EstimatorType {
    /**
     * A density estimator with a single discrete target variable and
     * several variables on which it is conditioned.
     */
    DISC_X1_I_Y1___Yl,

    /**
     * A density estimator with a single continuous target variable and
     * several variables on which it is conditioned.
     */
    CONT_X1_I_Y1___Yl,

    /**
     * A joint density estimator with several target variables and
     * several variables on which it is conditioned. The target
     * variables can be of different types, i.e., discrete and / or
     * continuous random variables.
     */
    X1___Xk_I_Y1___Yl;

    
    /** 
     * A density estimator estimates a density {@literal f(X1, ..., Xk |
     * Y1, ..., Yl)}, where {@literal {X1, ..., Xk} \cap {Y1, ..., Yl} =
     * \emptyset}. To avoid unnecessary overhead, we require that
     * {@literal {X1, ..., Xk} \cup {Y1, ..., Yl} = header}. Otherwise,
     * we are at the risk of performing too much preprocessing in the
     * density estimator that could also be done at a higher level
     * (e.g., as part of an ensemble density estimator).
     *
     * @param header description of the attributes
     * @param targetVars {@literal X1, ..., Xk}
     * @param condVars {@literal Y1, ..., Yl}     
     * @return true iff all requirements are fulfilled
     * @throws UnsupportedConfiguration is thrown if any of the
     *     arguments does not match the requirements.
     */
    public static boolean matchesRequirements(InstancesHeader header, 
                                              List<RandomVariable> targetVars,
                                              List<RandomVariable> condVars)
        throws UnsupportedConfiguration {

        // check whether targetVariables and conditionedVariables
        // contain variables only once
        Set<Attribute> targetAtts = new HashSet<>();
        for (RandomVariable rv : targetVars) {
            targetAtts.add(rv.getAttribute());
        }
        if (targetAtts.size() != targetVars.size()) {
            String msg = "target variable may only occur once.";
            throw new UnsupportedConfiguration(msg); 
        }
        Set<Attribute> condAtts = new HashSet<>();
        for (RandomVariable rv : condVars) {
            condAtts.add(rv.getAttribute());
        }
        if (condAtts.size() != condVars.size()) {
            String msg = "conditioned variable may only occur once.";
            throw new UnsupportedConfiguration(msg); 
        }
        
        // check whether targetVariables + conditionedVariables = header
        Set<Attribute> atts = new HashSet<>(targetAtts);
        atts.addAll(condAtts);
        boolean contained = true;
        for (int i = 0; i < header.numAttributes(); i++) {
            contained &= atts.contains(header.attribute(i));
        }
        if (!contained || !(header.numAttributes() == atts.size())) {
            String msg = "targetVariables + conditionedVariables must have";
            msg += " exactly the same attributes as header";
            throw new UnsupportedConfiguration(msg);
        }
        
        // check whether targetVariables and conditionedVariables are
        // disjoint (notice that we exploit earlier checks)
        if (!(targetAtts.size() + condAtts.size() == atts.size())) {
            String msg = "targetVariables and conditionedVariables have to be";
            msg += " disjoint";
            throw new UnsupportedConfiguration(msg);
        }

        return true;
    }

    /** 
     * Checks whether the arguments match the estimator type. False is
     * returned if any of the arguments do not match of capabilities of
     * the density estimators. For example, if the density estimator can
     * only handle a single discrete random target variable, {@code
     * targetVariables} should neither contain a continuous random
     * variable nor should it contain more than one element.
     *
     * Please notice that we assume that the general requirements have
     * been tested already with {@link #matchesRequirements}.
     *
     * @param header description of the attributes
     * @param targetVars {@literal X1, ..., Xk}
     * @param conditionedVars {@literal Y1, ..., Yl}
     * @return true iff arguments match the estimator type
     */
    public boolean matchesType(InstancesHeader header, 
                               List<RandomVariable> targetVars, 
                               List<RandomVariable> conditionedVars) {

        EstimatorType discOne = EstimatorType.DISC_X1_I_Y1___Yl;
        EstimatorType contOne = EstimatorType.CONT_X1_I_Y1___Yl;
        EstimatorType joint = EstimatorType.X1___Xk_I_Y1___Yl;

        boolean oneTarget = (targetVars.size() == 1);
        if ((this == discOne || this == contOne) && !oneTarget) {
            return false;
        }

        if (this == discOne) {
            return (targetVars.get(0) instanceof DiscreteRandomVariable);
        } else if (this == contOne) {
            return (targetVars.get(0) instanceof ContinuousRandomVariable);
        } else {
            return true;
        }
    }
}
