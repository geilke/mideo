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
package org.kramerlab.mideo.inference;

import org.kramerlab.mideo.core.RandomVariable;

/**
 * Evidence is information about a random variable. It can be very
 * specific as in the case of hard evidence ({@link HardEvidence}) or
 * more vague as in the case of ({@link SoftEvidence}).
 *
 * @author Michael Geilke
 */
public abstract class Evidence {

    protected RandomVariable variable;

    /**
     * @param rv the random variable for which the evidence is given
     */
    public void setVariable(RandomVariable rv) {
        this.variable = rv;
    }

    /**
     * @return the random variable for which the evidence is given
     */
    public RandomVariable getVariable() {
        return variable;
    }
}

