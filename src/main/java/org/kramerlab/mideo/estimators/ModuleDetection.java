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
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.MutualInformation;

/**
 * ModuleDetection determines the normalized mutual information
 * between modules and creates groups of variables based on that.
 *
 * @author Michael Geilke
 */
public class ModuleDetection {
        
    private static Logger logger = LogManager.getLogger();

    // thresholds at which a two variables are considered dependent on each
    // other, in terms of the normalized mutual information   
    public static Double DEPENDENCY_THRESHOLD = 0.25;

    private InstancesHeader originalHeader;
    private List<RandomVariable> targetVars;
    private List<RandomVariable> conditionedVars;

    private String name;
    private List<MutualInformation> NMIs;
    private List<Set<RandomVariable> > varGroups;
    private Set<RandomVariable> independentVars;
    private List<MetaInformation> metaInformation;
	
    public ModuleDetection() {
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void init(InstancesHeader header,
		     List<RandomVariable> targetVars, 
                     List<RandomVariable> condVars) {
	this.originalHeader = header;
        this.targetVars = targetVars;
        this.conditionedVars = condVars;
	
        // We basically want to create a matrix providing the NMI for
        // all variable pairs. But since the NMI is symetric, we just
        // compute the upper half of the matrix without the diagonal.
	this.NMIs = new ArrayList<>();
        for (RandomVariable rv1 : targetVars) {
            boolean start = false;
            for (RandomVariable rv2 : targetVars) {
                if (start) {
                    MutualInformation nmi = new MutualInformation(rv1, rv2);
                    NMIs.add(nmi);
                }
                if (rv1.equals(rv2)) {
                    start = true;
                }
            }
        }

	this.varGroups = new ArrayList<>();
	this.independentVars = new HashSet<>();
	this.metaInformation = new ArrayList<>();
    }

    public void prepareModules() {
	groupVariables();
	createMetaInformation();
    }

    private void groupVariables() {
	// determine variable dependencies and groups them accordingly
	this.varGroups = new ArrayList<>();
	boolean newDepsFound = true;
	while (newDepsFound) {
	    newDepsFound = false;
	    for (MutualInformation NMI : NMIs) {
		double nmi = NMI.getNormalizedMutualInformation();
		if (nmi >= DEPENDENCY_THRESHOLD) {
		    boolean isVarKnown = false;
		    for (Set<RandomVariable> varGroup : varGroups) {
			int size = varGroup.size();
			if (varGroup.contains(NMI.getVariable1())
			    || varGroup.contains(NMI.getVariable2())) {
			    isVarKnown = true;
			    varGroup.add(NMI.getVariable1());
			    varGroup.add(NMI.getVariable2());
			}
			if (varGroup.size() > size) {
			    newDepsFound = true;
			}
		    }
		    if (!isVarKnown) {
			Set<RandomVariable> varGroup = new HashSet<>();
			varGroup.add(NMI.getVariable1());
			varGroup.add(NMI.getVariable2());
			varGroups.add(varGroup);
			newDepsFound = true;
		    }
		}
	    }
	}
	// variables not having any dependencies
	this.independentVars = new HashSet<>();
	for (RandomVariable rv : targetVars) {
            boolean contained = false;
	    for (Set<RandomVariable> varGroup : varGroups) {
		if (varGroup.contains(rv)) {
                    contained = true;
		}
	    }
            if (!contained) {
                independentVars.add(rv);
            }
	}
        // if all variables are independent, we get one big module
        if (varGroups.size() == 0) {
            varGroups.add(independentVars);
            independentVars = new HashSet<>();
        }

        
    }

    private void createMetaInformation() {
	for (Set<RandomVariable> varGroup : varGroups) {
	    // random variables
	    List<RandomVariable> mTarget = new ArrayList<>(varGroup);
	    List<RandomVariable> mCond = new ArrayList<>(independentVars);
	    // instances header
	    String relationName = originalHeader.getRelationName();
	    List<Attribute> atts = new ArrayList<>();
	    for (RandomVariable rv : mTarget) {
		atts.add(rv.getAttribute());
	    }
	    for (RandomVariable rv : mCond) {
		atts.add(rv.getAttribute());
	    }
	    Instances ds = new Instances(relationName, atts, 0);
	    InstancesHeader header = new InstancesHeader(ds);
	    header.setClassIndex(header.numAttributes() - 1);
	    MetaInformation meta = new MetaInformation(header, mTarget, mCond);
	    metaInformation.add(meta);
	}   
    }
    
    public int getNumberOfModules() {
	return varGroups.size();
    }

    public Set<RandomVariable> getModule(int index) {
	return varGroups.get(index);
    }

    public Set<RandomVariable> getIndependentVariables() {
	return independentVars;
    }

    public MetaInformation getMetaInformation(int index) {
	return metaInformation.get(index);
    }
    
    public void update(Instance inst) {
        // cache instance values for faster access
        Map<String, Double> vals = new HashMap<>();
        for (int i = 0; i < inst.numAttributes(); i++) {
            String varName = inst.attribute(i).name();
            Double val = inst.value(i);
            vals.put(varName, val);
        }
        
        // update NMIs for all variable pairs
        for (MutualInformation nmi : NMIs) {
            double v1 = vals.get(nmi.getVariable1().getName());
            double v2 = vals.get(nmi.getVariable2().getName());
            nmi.update(v1, v2);
        }
    }

    public void printMatrix() {

        // we contruct a full matrix from NMIs
        String matrix = "";
        int start = 1;
        int index = 0;
        for (MutualInformation nmi : NMIs) {
            // when we reached the next line, we have to go the correct
            // starting position
            if (index == 0) {
                for (int i = 0; i < start; i++) {
                    matrix += " 0.0";
                }
            }
            matrix += " "; 
            matrix += Double.toString(nmi.getNormalizedMutualInformation());
            index++;
            // if we reach the end of the line, we have to start a new
            // one
            if (index + start >= targetVars.size()) {
                start++;
                matrix += "\n";
                index = 0;
            }
        }
        for (int i = 0; i < start; i++) {
            matrix += " 0.0";
        }
        matrix += "\n";

        String s = "";
        s += "set terminal postscript eps enhanced color font \", 18\"\n";
        s += "set size 1.0, 1.0\n";
        s += "set palette defined (0 '#6095c9', 1 '#ffffff', 2 '#cd665f')\n";
        s += "unset key\n";
        s += "set xtics (\"v_1\" 0, \"v_2\" 1, \"v_3\" 2, \"v_4\" 3, \"v_5\" 4, \"v_6\" 5, \"v_7\" 6, \"v_8\" 7, \"v_9\" 8, \"v_{10}\" 9, \"v_{11}\" 10, \"v_{12}\" 11, \"v_{13}\" 12, \"v_{14}\" 13, \"v_{15}\" 14, \"v_{16}\" 15, \"v_{17}\" 16, \"v_{18}\" 17)\n";
        s += "set ytics (\"v_1\" 0, \"v_2\" 1, \"v_3\" 2, \"v_4\" 3, \"v_5\" 4, \"v_6\" 5, \"v_7\" 6, \"v_8\" 7, \"v_9\" 8, \"v_{10}\" 9, \"v_{11}\" 10, \"v_{12}\" 11, \"v_{13}\" 12, \"v_{14}\" 13, \"v_{15}\" 14, \"v_{16}\" 15, \"v_{17}\" 16, \"v_{18}\" 17)\n";
        s += "set cbrange [0:0.75]\n";
        s += "set view map\n";
        s += "set output \"VAR_NAME.eps\"\n";
        s += "plot '-' matrix with image\n";
        s += "VAR_DATA\n";
        s += "EOF\n";
        s = s.replace("VAR_NAME", name);
        s = s.replace("VAR_DATA", matrix);
        System.out.println(s);
    }
}
