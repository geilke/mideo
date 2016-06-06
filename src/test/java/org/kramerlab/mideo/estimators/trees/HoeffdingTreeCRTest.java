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
package org.kramerlab.mideo.estimators.trees;

import java.util.List;
import java.util.ArrayList;
import org.junit.Before;  
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;

import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.DiscreteRandomVariable;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;
import org.kramerlab.mideo.data.streams.FileStream;
import org.kramerlab.mideo.evaluation.measures.LL;

/**
 * @author Michael Geilke
 */
public class HoeffdingTreeCRTest {
 
    private Logger logger;
    private FileStream stream;
    private HoeffdingTreeCR estimator;

    @Before
    public void setUp() {
        logger = LogManager.getLogger();

        long numInsts = 100000;
        String ds = getClass().getResource("/dataset-01.arff").getPath();

        // intialize file stream
        try {
            this.stream = new FileStream(ds, -1, numInsts);
            this.stream.init();
        } catch (Exception ex) {
            logger.error(ex.toString());
        }

        // initialize density estimator
        try {
            List<RandomVariable> targetVars = new ArrayList<>();
            List<RandomVariable> condVars = new ArrayList<>();
            List<RandomVariable> vars = stream.getRandomVariables();
            for (int i = 0; i < vars.size(); i++) {
                if (i == vars.size() - 1) {
                    targetVars.add(vars.get(i));
                } else {
                    condVars.add(vars.get(i));
                }
            }
            this.estimator = new HoeffdingTreeCR();
            this.estimator.init(stream.getHeader(), targetVars, condVars);
        } catch (UnsupportedConfiguration ex) {
            logger.error(ex.toString());
        }
    }

    @After
    public void tearDown() {
        stream = null;
        estimator = null;
    }

    /**
     * Tests whether the density value is larger than -infinity. For
     * that purpose, it trains the estimator on the {@literal dataset-01}
     * dataset with 50,000 instances and uses the remaining instances to
     * measure the log-likelihood, which has to be strictly greater than
     * {@code DOUBLE.NEGATIVE_INFINITY}.
     */
    @Test
    public void testDensityValue() {
        LL ll = new LL(stream, estimator);
        ll.evaluate();
        String testing = "average log-likelihood > -infinity?";
        assertTrue(testing, ll.getResult() > Double.NEGATIVE_INFINITY);
    }
}
