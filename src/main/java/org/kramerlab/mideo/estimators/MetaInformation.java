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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.RandomVariable;

/**
 * @author Michael Geilke
 */
public class MetaInformation {

    private InstancesHeader header;
    private List<RandomVariable> targetVars;
    private List<RandomVariable> condVars;
    
    /**
     * 
     */
    public MetaInformation(InstancesHeader header,
			   List<RandomVariable> targetVars,
			   List<RandomVariable> condVars) {

	this.header = header;
	this.targetVars = targetVars;
	this.condVars = condVars;
    }

    public InstancesHeader getHeader() {
	return header;
    }

    public List<RandomVariable> getTargetVariables() {
	return targetVars;
    }

    public List<RandomVariable> getConditionedVariables() {
	return condVars;
    }
}
