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
import java.util.ArrayList;
import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.Copyable;
import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.estimators.DensityEstimator;
import org.kramerlab.mideo.estimators.EstimatorType;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;

/**
 * In simple words, a module is an set of random variables that are
 * independent from the remaining random variables. Let {@literal X1,
 * ..., Xn} be random variables, let {@literal {Y1, ..., Yk} \subseteq
 * {X1, ..., Xn}}, and let {@literal f} be a density. We say that
 * {@literal {Y1, ..., Yk}} is a module if {@literal f(X1, ..., Xn) =
 * f1(Y1, ..., Yk) \cdot f2(R1, ..., Rl)}, {@literal {Y1, ..., Yk} \cup
 * {Z1, ..., Zl} = {X1, ..., Xn}}, and {@literal {Y1, ..., Yk} \cap {R1,
 * ..., Rl} = {}}. For further details regarding the definition, we
 * refer you to the paper by Geilke et al.:
 *
 * <p>Michael Geilke, Andreas Karwath, and Stefan Kramer. <br> A
 * Probabilistic Condensed Representation of Data for Stream
 * Mining. <br> In: Proceedings of the DSAA 2014, pages 297-303, IEEE
 * 2014.</p>
 *
 * {@code Module} extends this definition by allowing conditional
 * dependencies between the modules, which has already been suggested in
 * the paper mentioned above. It is associated with a density {@literal
 * fm} that we estimate by a density estimator. Moreover, it has target
 * variables, {@literal {Y1, ..., Yk}}, and variables on which the
 * density is conditioned {@literal {Z1, ..., Zm}}.
 *
 * Please notice that a module has to initialized ({@link #init}) before
 * setting a density estimator ({@link #setDensityEstimator}).
 * 
 * @author Michael Geilke
 */
public class Module implements Copyable<Module> {

    private List<RandomVariable> targetVars;
    private List<RandomVariable> conditionedVars;
    private DensityEstimator estimator;
    private boolean initialized;

    /**
     * Creates an empty module.
     */
    public Module() {
        this.initialized = false;
    }

    /**
     * Initializes the module with {@code targetVars} and {@code
     * condVars}.
     * @param header description of the attributes
     * @param targetVars {@literal Y1, ..., Yk}
     * @param condVars {@literal Z1, ..., Zm}
     * @throws UnsupportedConfiguration is thrown if {@code header} is
     * not compatible with {@code targetVars} and / or {@code
     * condVars}. Please check {@link EstimatorType#matchesRequirements}
     * for details.
     */
    public void init(InstancesHeader header, List<RandomVariable> targetVars, 
                     List<RandomVariable> condVars)
        throws UnsupportedConfiguration {
        EstimatorType.matchesRequirements(header, targetVars, condVars);
        this.targetVars = targetVars;
        this.conditionedVars = condVars;
        this.initialized = true;
    }
    
    /**
     * The random variables that are estimated.
     * @return {@literal Y1, ..., Yk}
     */
    public List<RandomVariable> getTargetVariables() {
        return targetVars;
    }

    /**
     * The random variables on which the target variables are
     * conditioned.
     * @return {@literal Z1, ..., Zm}
     */
    public List<RandomVariable> getConditionedVariables() {
        return conditionedVars;
    }

    /**
     * Specifies the density estimator. Please notice that {@link #init}
     * has to be invoked before setting a density estimator.
     *
     * @param est an estimator for the density of {@literal fm}
     * @throws UnsupportedConfiguration if module has not been
     * initialized yet or the variables of the estimator do not match
     * the variables specified during the initialization of the module
     */
    public void setDensityEstimator(DensityEstimator est) 
            throws UnsupportedConfiguration {

        // check whether the variables match
        List<RandomVariable> estTV = est.getTargetVariables();
        List<RandomVariable> estCV = est.getConditionedVariables();
        List<RandomVariable> tv = getTargetVariables();
        List<RandomVariable> cv = getConditionedVariables();
        boolean variablesMatch = (estTV.size() == tv.size());
        variablesMatch &= (estCV.size() == cv.size());
        for (RandomVariable rv : estTV) {
            variablesMatch &= tv.contains(rv);
        }
        for (RandomVariable rv : estCV) {
            variablesMatch &= cv.contains(rv);
        }

        if (initialized && variablesMatch) {
            this.estimator = est;
        } else {
            String msg = "Variables do not match.";
            throw new UnsupportedConfiguration(msg);
        }
    }
    
    /**
     * The estimator for the density associated with this module.
     * @return {@literal fm}
     */
    public DensityEstimator getDensityEstimator() {
        return estimator;
    }    
}
