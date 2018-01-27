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

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;
import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.Copyable;
import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.data.streams.Stream;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;

/**
 * DensityEstimator is a conditional density estimator {@literal f(X1,
 * ..., Xk | Y1, ..., Yl)}. Dependent on the types supported by the
 * estimator, which can be accessed through {@code getSupportedTypes},
 * it can be initialized accordingly via the {@code init} method, where
 * {@literal X1, ..., Xk} are the target variables and {@literal Y1,
 * ..., Yl} are the variables on which the target variables are
 * conditioned. After initializing the estimator, it can be updated
 * through the {@code update} method.
 *
 * @author Michael Geilke
 */ 
public interface DensityEstimator extends Copyable<DensityEstimator> {

    /**
     * Initializes the density estimator as an estimator for {@literal
     * f(X1, ..., Xk | Y1, ..., Yl)}, where {@literal {X1, ..., Xk} \cap
     * {Y1, ..., Yl} = \emptyset}. To avoid unnecessary overhead, we
     * require that {@literal {X1, ..., Xk} \cup {Y1, ..., Yl} =
     * header}. Otherwise, we are at the risk of performing too much
     * preprocessing in the density estimator that could also be done at
     * a higher level (e.g., as part of an ensemble density estimator).
     *
     * Please notice that calling {@code init} after the density
     * estimator has been updated causes a reset, i.e., already trained
     * models are discarded.
     *
     * @param header description of the attributes
     * @param targetVars {@literal X1, ..., Xk}
     * @param condVars {@literal Y1, ..., Yl}
     * @throws UnsupportedConfiguration is thrown if any of the
     *     parameters does not match the capabilities of the density
     *     estimators. For example, if the density estimator can only
     *     handle a single discrete random target variable, {@code
     *     targetVariables} should neither contain a continuous random
     *     variable nor should it contain more than one element.
     */
    default void init(InstancesHeader header, List<RandomVariable> targetVars, 
                      List<RandomVariable> condVars)
        throws UnsupportedConfiguration {

        // test whether requirements for a density estimator are
        // fulfilled and whether one of the estimator types matches
        boolean m = false;
        boolean t = false;
        m = EstimatorType.matchesRequirements(header, targetVars, condVars);
        for (EstimatorType type : getSupportedTypes()) {
            t |= type.matchesType(header, targetVars, condVars);
            if (t) {
                break;
            }
        }
        if (!m || !t) {
            String msg = "does not match any estimator type";
            throw new UnsupportedConfiguration(msg);
        }
    }

    /**
     * The types of densities that can be estimated by the estimator.
     * @return the types of densities that can be estimated by the
     *     estimator
     */
    List<EstimatorType> getSupportedTypes();

    /**
     * The random variables that are estimated.
     * @return {@literal X1, ..., Xk}
     */
    List<RandomVariable> getTargetVariables();

    /**
     * The random variables on which the target variables are
     * conditioned.
     * @return {@literal Y1, ..., Yl}
     */
    List<RandomVariable> getConditionedVariables();

    /**
     * Updates the density estimator with instance {@code inst}.
     * @param inst instance on which density estimator should be trained
     */
    void update(Instance inst);

    /**
     * Returns the density value of {@code inst}, i.e., {@literal f(x1,
     * ..., xk | y1, ..., yl)}, where xi and yj are the values of {@code
     * inst}.
     * @param inst the instance of which the density value is supposed
     * to be computed. Whether an attribute belongs to an Xi or an Yj
     * depends on {@link #getTargetVariables} and {@link
     * #getConditionedVariables}.
     * @return the density value, i.e., {@literal f(x1, ..., xk | y1,
     * ..., yl)}
     */
    double getDensityValue(Instance inst);

    /**
     * Returns a sample of instances.
     * @return instance sample
     */
    default List<Instance> getSample() {
	return new ArrayList<>();
    }
    
    /**
     * To evaluate and debug density estimators, information on the
     * model is provided in JSON format. This could be something like
     * the depth of trees, the number of nodes, the number of kernels,
     * etc.
     *
     * @return information about the density estimator in JSON format
     */
    default JsonStructure getModelCharacteristics() {
        JsonObjectBuilder o = Json.createObjectBuilder();
        return o.build();
    }
}
