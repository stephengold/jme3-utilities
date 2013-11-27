// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
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

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * Callback interface for notifying controls when they physically overlap with
 * an unrelated PhysicsRigidBody.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public interface OverlapListener {

    /**
     * Report an overlap with an unrelated rigid body.
     *
     * @param overlappingBody the overlapping rigid body (not null)
     * @param overlappingSpatial the spatial of the overlapping rigid body (not
     * null)
     * @param localPoint the location of the overlap (rotated and translated to
     * the this control's object, but at world scale, not null)
     */
    public void onOverlap(
            PhysicsRigidBody overlappingBody,
            Spatial overlappingSpatial,
            Vector3f localPoint);
}