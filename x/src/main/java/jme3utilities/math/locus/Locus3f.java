/*
 Copyright (c) 2017-2019, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.math.locus;

import com.jme3.math.Vector3f;
import jme3utilities.math.spline.Spline3f;

/**
 * Represents a region (set of locations) in 3-D space.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public interface Locus3f {
    /**
     * Test whether this region can be merged with another.
     *
     * @param otherLocus (not null, unaffected)
     * @return true if can merge, otherwise false
     */
    boolean canMerge(Locus3f otherLocus);

    /**
     * Calculate the centroid of this region. The centroid need not be contained
     * in the region, but it should be relatively near all locations that are.
     *
     * @return a new coordinate vector
     */
    Vector3f centroid();

    /**
     * Test whether this region contains the specified location.
     *
     * @param location coordinates of test location (not null, unaffected)
     * @return true if location is in region, false otherwise
     */
    boolean contains(Vector3f location);

    /**
     * Test whether this region contains the specified segment.
     *
     * @param startLocation coordinates of start of test segment (not null,
     * unaffected)
     * @param endLocation coordinates of end of test segment (not null,
     * unaffected)
     * @return true if test segment is entirely contained in region, false
     * otherwise
     */
    boolean contains(Vector3f startLocation, Vector3f endLocation);

    /**
     * Find the location in this region nearest to the specified location.
     *
     * @param location coordinates of the input (not null, unaffected)
     * @return a new vector, or null it none found
     */
    Vector3f findLocation(Vector3f location);

    /**
     * Merge this region with another.
     *
     * @param otherLocus (not null, unaffected)
     * @return a new region representing the union of the two regions
     */
    Locus3f merge(Locus3f otherLocus);

    /**
     * Calculate a representative location (or rep) for this region. The rep
     * must be contained in the region.
     *
     * @return a new coordinate vector, or null if none found
     */
    Vector3f rep();

    /**
     * Score a location based on how well it "fits" with this region.
     *
     * @param location coordinates of the input (not null, unaffected)
     * @return score value (more positive &rarr; better)
     */
    double score(Vector3f location);

    /**
     * Find a path between 2 locations in this region without leaving the
     * region. Short paths are preferred over long ones.
     *
     * @param startLocation coordinates (contained in region, unaffected)
     * @param goalLocation coordinates (contained in region, unaffected)
     * @param maxPoints maximum number of control points to use (&ge;2)
     * @return a new path spline, or null if none found
     */
    Spline3f shortestPath(Vector3f startLocation, Vector3f goalLocation,
            int maxPoints);

    /**
     * Calculate the distance from the specified starting point to the first
     * point of support (if any) directly below it in this region.
     *
     * @param location coordinates of starting point (not null, unaffected)
     * @param cosineTolerance cosine of maximum slope for support (&gt;0, &lt;1)
     * @return the minimum distance (&ge;0) or {@link Float#POSITIVE_INFINITY}
     * if no support
     */
    float supportDistance(Vector3f location, float cosineTolerance);
}
