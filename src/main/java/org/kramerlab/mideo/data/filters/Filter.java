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
package org.kramerlab.mideo.data.filters;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

/**
 * A filter is a function transforming instances with a specific source
 * format to a target format. The source format is specified by {@link
 * #getSourceHeader} and the target format by {@link
 * #getTargetHeader}. With {@link #apply}, an instance {@code inst} is
 * mapped to from the source format to the target format.
 *
 * Typical examples are noise filters or discretization filters.
 *
 * @author Michael Geilke
 */
public interface Filter {

    /**
     * Describes the structure of the source instances. It includes the
     * attributes and the values the attributes can take.
     *
     * @return the structure of the source instances as described in
     * SAMOA
     */
    InstancesHeader getSourceHeader();

    /**
     * Describes the structure of the target instance. It includes the
     * attributes and the values the attributes can take.
     *
     * @return the structure of the target instances as described in
     * SAMOA
     */
    InstancesHeader getTargetHeader();
    
    /**
     * Transforms the given instance according to the filter.
     * @param inst the instance that should be tranformed
     */
    Instance apply(final Instance inst);
}
