/**
 * MiDEO: a framework to perform data mining on probabilistic condensed 
 * representations
 * Copyright (C) 2014 Michael Geilke
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
package org.kramerlab.mideo.core.discretization;

import java.util.Random;
import org.junit.Before;  
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
 
/**
 * @author Michael Geilke
 */
public class PartitionIncrementalTest {
 
    private final int n = 2000;
    private final int k = 10;
    private PartitionIncremental ewDisc;
    private PartitionIncremental efDisc;

    @Before
    public void setUp() {
        this.ewDisc = new PartitionIncremental(k, 
            DiscretizationType.EQUAL_WIDTH, 0, 1);
        this.efDisc = new PartitionIncremental(k, 
            DiscretizationType.EQUAL_FREQUENCY, 0, 1);

        // add 100 random numbers between 0 and 1
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            double obs = random.nextDouble();
            ewDisc.addObservation(obs);
            efDisc.addObservation(obs);
        }
    }

    @After
    public void tearDown() {
        ewDisc = null;
        efDisc = null;
    }

    /**
     * Checks whether setting the equal-width discretization works.
     */
    @Test
    public void testDiscretizationType01() {
        DiscretizationType type = ewDisc.getDiscretizationType();
        assertTrue(type == DiscretizationType.EQUAL_WIDTH);
    }

    /**
     * Checks whether setting the equal-frequency discretization works.
     */
    @Test
    public void testDiscretizationType02() {
        DiscretizationType type = efDisc.getDiscretizationType();
        assertTrue(type == DiscretizationType.EQUAL_FREQUENCY);
    }

    /**
     * Requests border {@literal b{-1}}, which does not exists.
     */
    @Test(expected=IndexOutOfBoundsException.class)
    public void testBorders01() {
        ewDisc.getBorder(-1);
    }

    /**
     * Requests border {@literal b{k+1}}, which does not exists.
     */
    @Test(expected=IndexOutOfBoundsException.class)
    public void testBorders02() {
        ewDisc.getBorder(k + 1);
    }
    
    /**
     * Checks whether b0 is negative infinity.
     */
    @Test
    public void testBorders03() {
        assertTrue(ewDisc.getBorder(0) == Double.NEGATIVE_INFINITY);
    }

    /**
     * Checks whether bk is positive infinity.
     */
    @Test
    public void testBorders04() {
        assertTrue(ewDisc.getBorder(k) == Double.POSITIVE_INFINITY);
    }

    /**
     * Checks whether the bin [b0; b{1}) has zero observations. It
     * should be zero, since we did not add any outliers.
     */
    @Test
    public void testBinCount01() {
        assertEquals(0, ewDisc.getBinCount(0), 0);
    }

    /**
     * Checks whether the bin [b{k-1} b{k}) has zero observations. It
     * should be zero, since we did not add any outliers.
     */
    @Test
    public void testBinCount02() {
        assertEquals(0, ewDisc.getBinCount(k - 1), 0);
    }

    /**
     * Checks whether the bins [b{1} b{2}), ..., [b{k-2} b{k-1}) have
     * the expected number of observerations of the equal-width
     * discretization (+- 30 percent).
     */
    @Test
    public void testBinCount03() {
        double expected = (double) n / (k - 2);
        for (int i = 1; i < k - 1; i++) {
            assertEquals(expected, ewDisc.getBinCount(i), 0.3 * expected);
        }
    }

    /**
     * Checks whether the bins [b{1} b{2}), ..., [b{k-2} b{k-1}) have
     * the expected number of observerations of the equal-frequency
     * discretization (+- 30 percent).
     */
    @Test
    public void testBinCount04() {
        double expected = (double) n / (k - 2);
        for (int i = 1; i < k - 1; i++) {
            assertEquals(expected, efDisc.getBinCount(i), 0.3 * expected);
        }
    }

    /**
     * Checks whether the observation 0.0 is in the correct bin.
     */
    @Test
    public void testApply01() {
        // The expected width for an equal-width discretization is 1 /
        // (k - 2), since there first and last bin are for
        // outliers. Hence, Math.ceil(expected with) is the expected
        // bin.
        double obs = 0.0;
        double expected = Math.ceil(obs / ((double) 1 / (k - 2)));
        assertEquals(expected, ewDisc.apply(obs), 0);
    }

    /**
     * Checks whether the observation 0.150 is in the correct bin.
     */
    @Test
    public void testApply02() {
        // The expected width for an equal-width discretization is 1 /
        // (k - 2), since there first and last bin are for
        // outliers. Hence, Math.ceil(expected with) is the expected
        // bin.
        double obs = 0.150;
        double expected = 0.0;
        if (obs % ((double) 1 / (k - 2)) == 0) {
            expected = obs / ((double) 1 / (k - 2)) + 1;
        } else {
            expected = Math.ceil(obs / ((double) 1 / (k - 2)));
        }
        assertEquals(expected, ewDisc.apply(obs), 0);
    }

    /**
     * Checks whether the observation 1.0 is in the correct bin.
     */
    @Test
    public void testApply03() {
        double obs = 1.0;    // an observation from the last bin
        double expected = 9; // last bin
        assertEquals(expected, ewDisc.apply(obs), 0);
    }

    /**
     * Adds a count of 10 to bin k/2 and checks whether it has been
     * added correctly to the equal-width discretization.
     */
    @Test
    public void testAddCountToBin01() {
        // For a proper comparison, observations from the buffer have to
        // be processed. Otherwise, they will be processed when invoking
        // addCountsToBin().
        ewDisc.processBuffer();

        int bin = k / 2;
        int count = 10;
        long current = ewDisc.getBinCount(bin);
        ewDisc.addCountToBin(bin, count);
        assertEquals(current + count, ewDisc.getBinCount(bin), 2);
    }
}
