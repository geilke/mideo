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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.kramerlab.mideo.core.RandomVariable;

/**
 * InferenceInformation collects hard evidence, soft evidence, and
 * variables to be marginalized out for a given object.
 *
 * @author Michael Geilke
 */
public class InferenceInformation {

    protected List<InferenceInformation> observers;
    protected Map<String, List<Evidence>> evidences;
    protected Map<String, HardEvidence> tmpEvidences;
    protected Set<RandomVariable> marginalizedOutVars;

    public InferenceInformation() {
        this.observers = new ArrayList<>();
        this.evidences = new HashMap<>();
        this.tmpEvidences = new HashMap<>();
        this.marginalizedOutVars = new HashSet<>();
    }

    /**
     * Adds other inference information objects as an observer.
     * Condensed representations often consist of many different
     * components. To make sure that all components have the same
     * information, {@code InferenceInformation} provides the
     * possibility to attach observers. Whenever the inference
     * information is changed, all observers are informed automatically.
     * @param obs the observer to which changes are forwarded
     */
    public void addObserver(InferenceInformation obs) {
        observers.add(obs);
    }

    /**
     * Removes the given observer. See {@link addObserver} for further
     * information.
     * @param obs the observer that has to be removed
     */
    public void removeObserver(InferenceInformation obs) {
        observers.remove(obs);
    }

    /**
     * Adds the given evidence and forwards it to the attached
     * observers.
     * @param evidence the evidence that is supposed to be added
     */
    public void addEvidence(Evidence evidence) {
        String varName = evidence.getVariable().getName();
        List<Evidence> varEvidences = null;
        if (evidences.containsKey(varName)) {
            varEvidences = evidences.get(varName);
        } else {
            varEvidences = new ArrayList<>();
        }
        // remove old evidence if necessary
        if (varEvidences.contains(evidence)) {
            varEvidences.remove(evidence);
        }
        // add evidence and inform observers
        varEvidences.add(evidence);
        for (InferenceInformation obs : observers) {
            obs.addEvidence(evidence);
        }
    }

    public void setTemporaryEvidence(Set<HardEvidence> evidences) {
        clearTemporaryEvidence();
        for (HardEvidence evidence : evidences) {
            String varName = evidence.getVariable().getName();
            tmpEvidences.put(varName, evidence);
            for (InferenceInformation obs : observers) {
                obs.setTemporaryEvidence(evidences);
            }
        }
    }

    /**
     * @param varName the name of the random variable
     * @param true if either evidence or temporary evidence is given
     */
    public boolean hasEvidence(String varName) {
        boolean hasEvid = evidences.containsKey(varName);
        hasEvid |= tmpEvidences.containsKey(varName);
        return hasEvid;

    }

    public List<Evidence> getEvidence(String varName) {
        if (tmpEvidences.containsKey(varName)) {
            return Collections.singletonList(tmpEvidences.get(varName));
        } else if (evidences.containsKey(varName)) {
            return evidences.get(varName);
        } else {
            return null;
        }
    }

    /**
     * Removes all evidences and temporary evidences.
     */
    public void clearEvidence() {
        evidences.clear();
        clearTemporaryEvidence();
    }

    /**
     * Removes all temporary evidences.
     */
    public void clearTemporaryEvidence() {
        tmpEvidences.clear();
    }

    /**
     * @param vars the random variables to be marginalized out
     */
    public void setMarginalizedOutVariables(Set<RandomVariable> vars) {
        marginalizedOutVars = vars;
    }

    /**
     * @return the random variables that have been marginalized out
     */
    public Set<RandomVariable> getMarginalizedOutVariables() {
        return marginalizedOutVars;
    }

    /**
     * @param varName the name of the random variable
     * @return true iff the variable is a marginalized-out variable,
     * i.e., it has been set via {@link #setMarginalizedOutVariables}
     */
    public boolean isMarginalizedOutVariable(String varName) {
        for (RandomVariable rv : marginalizedOutVars) {
            if (rv.getName().equals(varName)) {
                return true;
            }
        }
        return false;
    }
}

