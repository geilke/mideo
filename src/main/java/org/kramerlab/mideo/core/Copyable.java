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
package org.kramerlab.mideo.core;

import java.io.Serializable;

import moa.core.SerializeUtils;

/**
 * Classes implementing {@code Copyable} support deep copies. The copy
 * is performed by serializing the object and casting it to the required
 * type. Hence, any class implementing {@code Copyable} also supports
 * serialization.
 *
 * @author Michael Geilke
 */
public interface Copyable<T> extends Serializable {

    /**
     * @return makes a deep copy and casts the copy to type {@code T}.
     */
    default T makeCopy() {
        try {            
            return (T) SerializeUtils.copyObject(this);
        } catch (Exception ex) {
            throw new RuntimeException("Could not copy " 
                                       + this.getClass(), ex);
        }
    }
}
