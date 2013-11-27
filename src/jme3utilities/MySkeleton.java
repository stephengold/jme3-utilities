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

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for manipulating unit skeletons. Aside from test cases, all
 * methods should be public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MySkeleton
        extends SimpleApplication {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MySkeleton.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MySkeleton() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access a named bone in a unit.
     *
     * @param spatial which unit (not null)
     * @param boneName which bone to measure (not null)
     * @return the pre-existing instance (or null if not found)
     */
    public static Bone getBone(Spatial spatial, String boneName) {
        assert spatial != null;
        assert boneName != null;

        Skeleton skeleton = getSkeleton(spatial);
        if (skeleton == null) {
            return null;
        }
        Bone bone = skeleton.getBone(boneName);
        return bone;
    }

    /**
     * Access one bone angle in a unit's pose.
     *
     * @param spatial which unit (not null)
     * @param boneName which bone to measure (not null)
     * @param axis which axis to measure (0 = x, 1 = y, 2 = z)
     * @return the rotation angle (in radians) or zero for unknown bone
     */
    public static float getBoneAngle(Spatial spatial, String boneName,
            int axis) {
        assert spatial != null;
        assert boneName != null;
        assert axis >= 0 : axis;
        assert axis <= 2 : axis;

        Bone bone = getBone(spatial, boneName);
        if (bone == null) {
            return 0f;
        }
        Quaternion orientation = bone.getLocalRotation();
        float[] angles = orientation.toAngles(null);
        float angle = angles[axis];
        return angle;
    }

    /**
     * Access the world coordinates of a bone.
     *
     * @param spatial which unit (not null)
     * @param boneName which bone to measure (not null)
     * @return a new vector
     */
    public static Vector3f getBoneLocation(Spatial spatial, String boneName) {
        assert spatial != null;
        assert boneName != null;

        SkeletonControl skeletonControl =
                spatial.getControl(SkeletonControl.class);
        Node boneAttach = skeletonControl.getAttachmentsNode(boneName);
        Vector3f location = boneAttach.getWorldTranslation().clone();
        return location;
    }

    /**
     * Access a unit's skeleton.
     *
     * @param spatial which unit (not null)
     * @return the pre-existing instance (or null if not found)
     */
    public static Skeleton getSkeleton(Spatial spatial) {
        assert spatial != null;

        SkeletonControl control = spatial.getControl(SkeletonControl.class);
        if (control == null) {
            return null;
        }
        Skeleton skeleton = control.getSkeleton();
        return skeleton;
    }

    /**
     * List the bones in the skeleton of a particular unit.
     *
     * @param spatial which unit (or null)
     * @return a new sorted set
     */
    public static Collection<String> listBones(Spatial spatial) {
        /*
         * Collect all bone names.
         */
        Collection<String> names = new TreeSet<>();
        if (spatial != null) {
            Skeleton skeleton = getSkeleton(spatial);
            if (skeleton != null) {
                int boneCount = skeleton.getBoneCount();
                for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
                    Bone bone = skeleton.getBone(boneIndex);
                    String name = bone.getName();
                    names.add(name);
                }
            }
        }
        return names;
    }

    /**
     * Alter one bone rotation angle in a unit's bind pose.
     *
     * @param spatial which unit (not null)
     * @param boneName which bone to adjust (not null)
     * @param axis which axis to adjust (0 = x, 1 = y, 2 = z)
     * @param newAngle new rotation angle (in radians)
     */
    public static void setBoneAngle(Spatial spatial, String boneName, int axis,
            float newAngle) {
        assert spatial != null;
        assert boneName != null;
        assert axis >= 0 : axis;
        assert axis <= 2 : axis;

        Bone bone = getBone(spatial, boneName);
        if (bone == null) {
            return;
        }
        Misc.setAngle(bone, axis, newAngle);
        getSkeleton(spatial).updateWorldVectors();
    }
    // *************************************************************************
    // test cases

    /**
     * Test cases for the MySkeleton class.
     *
     * @param ignored
     */
    public static void main(String[] ignored) {
        Misc.setLoggingLevels(Level.SEVERE);
        MySkeleton application = new MySkeleton();
        application.setShowSettings(false);
        application.start();
    }

    @Override
    public void simpleInitApp() {
        logger.setLevel(Level.INFO);
        System.out.print("Test results for the MySkeleton class:\n");

        String modelPath = "Models/Oto/Oto.mesh.xml";
        Node node = (Node) assetManager.loadModel(modelPath);
        rootNode.attachChild(node);

        String bone = "uparm.right";
        for (int axis = 0; axis < 3; axis++) {
            float angle = 0.2f + 0.1f * axis;
            System.out.printf("angle = %s%n", angle);
            setBoneAngle(node, bone, axis, angle);
            float angle2 = getBoneAngle(node, bone, axis);
            System.out.printf("angle2 = %s%n", angle2);
        }

        stop();
    }
}