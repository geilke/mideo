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
 */
package org.kramerlab.mideo.inference;

import java.util.List;

import com.yahoo.labs.samoa.instances.Instance;

import org.kramerlab.mideo.estimators.DensityEstimator;

/**
 * A condensed representation extends a density estimator by inference
 * capabilities. The inference information is provided by an internal
 * inference information object, which can be retrieved by {@link
 * #getInferenceInformation} and set by {@link
 * #setInferenceInformation}. All operations are performed with respect
 * to this information.
 *
 * @author Michael Geilke
 */
public interface CondensedRepresentation extends DensityEstimator {

    /**
     * @return the inference object that keeps track of the evidence and
     * the variables that are marginalized out
     */
    InferenceInformation getInferenceInformation();

    /**
     * @param info the inference object that keeps track of the evidence
     * and the variables that are marginalized out
     */
    void setInferenceInformation(InferenceInformation info);

    /**
     * Computes the density value of the given instance with respect to
     * specified inference information.
     * @return the density value {@code inst} with respect to specified
     * inference information
     */
    @Override
    double getDensityValue(Instance inst);

    /**
     * Draws an instance with respect to specified inference
     * information.
     * @return an instance drawn with respect to specified inference
     * information
     */
    Instance drawInstance();
}
