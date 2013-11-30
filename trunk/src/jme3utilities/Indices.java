// (c) Copyright 2012, 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities;

import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * A set of (integer-valued) indices. Used (for instance) to track command
 * signals from multiple sources.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Indices
        extends TreeSet<Integer> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Indices.class.getName());
    /**
     * version for serialization
     */
    final private static long serialVersionUID = 1L;
    // *************************************************************************
    // new methods exposed

    /**
     * Adjust the inclusion status of a particular index.
     *
     * @param index which index
     * @param addFlag if true, make sure the index is included in the set; if
     * false, make sure the index is excluded
     */
    public void addRemove(int index, boolean addFlag) {
        if (addFlag) {
            add(index);
        } else {
            remove(index);
        }
    }
}