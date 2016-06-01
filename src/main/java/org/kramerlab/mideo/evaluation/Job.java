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
package org.kramerlab.mideo.evaluation;

import java.io.IOException;

import org.kramerlab.mideo.core.Configurable;

/**
 * A job is an abstract computation usually involving a data stream, an
 * algorithm, and a performance measure. Which objects are involved and
 * which parameters to use is described by the job description. The
 * individual steps of the computation are defined by the method {@code
 * run}.
 *
 * The main purpose of a job is to evaluate algorithms of the MiDEO
 * framework. It is usually initialized and run by the {@link JobCenter}.
 *
 * @author Michael Geilke
 */
public abstract class Job implements Configurable {

    private JobDescription desc;

    /**
     * @param desc a description of the involved objects and parameters
     */
    public void setJobDescription(JobDescription desc) {
        this.desc = desc;
    }

    /**
     * @return provides a description of the involved objects and
     * parameters
     */
    public JobDescription getJobDescription() {
        return desc;
    }

    /**
     * Performs the computation using the objects defined by {@code
     * getJobDescription}.
     */
    abstract void run();

    /**
     * If an output file is defined in {@code getJobDescription}, the
     * output of the job is written to this file.
     * @throws IOException if the output cannot be written to the
     * specified file or if no file has been specified.
     */
    abstract void writeOutput() throws IOException;
}
