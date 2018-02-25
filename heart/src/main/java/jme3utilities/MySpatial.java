/*
 Copyright (c) 2013-2018, Stephen Gold
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
package jme3utilities;

import com.jme3.app.StatsView;
import com.jme3.audio.AudioNode;
import com.jme3.effect.ParticleEmitter;
import com.jme3.font.BitmapText;
import com.jme3.light.Light;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.AssetLinkNode;
import com.jme3.scene.BatchNode;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.GeometryGroupNode;
import com.jme3.scene.LightNode;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.SimpleBatchNode;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.scene.debug.SkeletonDebugger;
import com.jme3.scene.instancing.InstancedGeometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.ui.Picture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.math.MyVector3f;

/**
 * Utility methods for manipulating scene graphs, nodes, and geometries.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MySpatial {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MySpatial.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MySpatial() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Re-parent a spatial, keeping its world scale unchanged.
     *
     * NOTE: This method may yield incorrect results in the presence of
     * non-uniform scaling.
     *
     * @param newParent (not null)
     * @param child spatial to re-parent (not null, not orphan)
     * @throws IllegalArgumentException if the child is a geometry with
     * ignoreTransform=true
     */
    public static void adopt(Node newParent, Spatial child) {
        if (isIgnoringTransforms(child)) {
            throw new IllegalArgumentException("transform ignored");
        }
        Validate.nonNull(newParent, "new parent");
        Node oldParent = child.getParent();
        if (oldParent == null) {
            throw new NullPointerException("child should not be an orphan");
        }

        if (newParent != oldParent) {
            float scaleFactor
                    = oldParent.getWorldScale().x / newParent.getWorldScale().x;
            Vector3f localScale = child.getLocalScale();
            localScale.multLocal(scaleFactor);
            child.setLocalScale(localScale);

            newParent.attachChild(child);
        }

        assert child.getParent() == newParent : newParent;
    }

    /**
     * Count all controls of the specified type in the specified subtree of a
     * scene graph. Note: recursive!
     *
     * @param <T> superclass of Control
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param controlType superclass of Control to search for
     * @return number of scene-graph controls (&ge;0)
     */
    public static <T extends Control> int countControls(Spatial subtree,
            Class<T> controlType) {
        int result = 0;

        if (subtree != null) {
            int numControls = subtree.getNumControls();
            for (int controlI = 0; controlI < numControls; controlI++) {
                Control control = subtree.getControl(controlI);
                if (controlType.isAssignableFrom(control.getClass())) {
                    ++result;
                }
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countControls(child, controlType);
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Estimate the number of bones in the specified subtree by reading its mesh
     * index buffers.
     *
     * @param subtree (may be null)
     * @return estimated number (&ge;0)
     */
    public static int countMeshBones(Spatial subtree) {
        int result = 0;
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            result = MyMesh.countBones(mesh);
        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                int childBones = countMeshBones(child);
                if (childBones > result) {
                    result = childBones;
                }
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count all spatials of the specified type in the specified subtree of a
     * scene graph. Note: recursive!
     *
     * @param <T> superclass of Spatial
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param spatialType superclass of Spatial to search for
     * @return number of spatials (&ge;0)
     */
    public static <T extends Spatial> int countSpatials(Spatial subtree,
            Class<T> spatialType) {
        int result = 0;

        if (subtree != null
                && spatialType.isAssignableFrom(subtree.getClass())) {
            ++result;
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countSpatials(child, spatialType);
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count all user data in the specified subtree of a scene graph. Note:
     * recursive!
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @return number of user data (&ge;0)
     */
    public static int countUserData(Spatial subtree) {
        int result = 0;
        if (subtree != null) {
            Collection<String> keys = subtree.getUserDataKeys();
            result += keys.size();
        }
        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countUserData(child);
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many mesh vertices are contained in the specified subtree of a
     * scene graph. Note: recursive!
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @return number of vertices (&ge;0)
     */
    public static int countVertices(Spatial subtree) {
        int result = 0;
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            result = mesh.getVertexCount();

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            Collection<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countVertices(child);
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Generate a single-character description of a spatial.
     *
     * @param spatial spatial to describe (unaffected)
     * @return mnemonic character
     */
    public static char describeType(Spatial spatial) {
        if (spatial instanceof AssetLinkNode) {
            return 'A';
        } else if (spatial instanceof AudioNode) {
            return 'a';
        } else if (spatial instanceof BatchNode) {
            return 'b';
        } else if (spatial instanceof BitmapText) {
            return 't';
        } else if (spatial instanceof CameraNode) {
            return 'c';
        } else if (spatial instanceof GeometryGroupNode) {
            return 'G';
        } else if (spatial instanceof InstancedGeometry) {
            return 'i';
        } else if (spatial instanceof InstancedNode) {
            return 'N';
        } else if (spatial instanceof LightNode) {
            return 'L';
        } else if (spatial instanceof ParticleEmitter) {
            return 'e';
        } else if (spatial instanceof Picture) {
            return 'p';
        } else if (spatial instanceof SimpleBatchNode) {
            return 'B';
        } else if (spatial instanceof SkeletonDebugger) {
            return 's';
        } else if (spatial instanceof StatsView) {
            return 'S';
        } else if (spatial instanceof Geometry) {
            return 'g';
        } else if (spatial instanceof Node) {
            return 'n';
        }
        return '?';
    }

    /**
     * Find an animated geometry in the specified subtree of the scene graph.
     * Note: recursive!
     *
     * @param subtree where to search (not null, unaffected)
     * @return a pre-existing instance, or null if none
     */
    public static Geometry findAnimatedGeometry(Spatial subtree) {
        Geometry result = null;
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (mesh.isAnimated()) {
                result = geometry;
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result = findAnimatedGeometry(child);
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Find a node's 1st child that is assignable from the specified class.
     *
     * @param <T> subtype of {@link com.jme3.scene.Spatial}
     * @param node node to search (not null)
     * @param spatialType type of Spatial to search for (not null)
     * @return a pre-existing instance, or null if none found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Spatial> T findChild(Node node,
            Class<T> spatialType) {
        for (Spatial child : node.getChildren()) {
            if (spatialType.isAssignableFrom(child.getClass())) {
                return (T) child;
            }
        }

        return null;
    }

    /**
     * Find the 1st child of the specified node with the specified name. The
     * search is case-sensitive and looks only at the node's immediate children,
     * not all its descendents.
     *
     * @param node node to search (not null)
     * @param childName name to search for (not null)
     * @return a pre-existing instance, or null if none found
     *
     * @see com.jme3.scene.Node#getChild(java.lang.String)
     */
    public static Spatial findChild(Node node, String childName) {
        Validate.nonNull(childName, "child name");

        for (Spatial child : node.getChildren()) {
            String name = child.getName();
            if (childName.equals(name)) {
                return child;
            }
        }

        return null;
    }

    /**
     * Find a spatial controlled by the specified S-G control in the specified
     * subtree of the scene graph. Note: recursive!
     *
     * @param sgc which scene-graph control (not null, unaffected)
     * @param subtree which subtree (not null, unaffected)
     * @return the pre-existing controlled spatial, or null if none found
     */
    public static Spatial findControlledSpatial(Control sgc, Spatial subtree) {
        Validate.nonNull(sgc, "control");
        Validate.nonNull(subtree, "subtree");

        Spatial result = null;
        if (sgc instanceof AbstractControl) {
            AbstractControl abstractControl = (AbstractControl) sgc;
            result = abstractControl.getSpatial();
        }
        if (result == null) {
            int sgcIndex = MyControl.findIndex(sgc, subtree);
            if (sgcIndex != -1) {
                result = subtree;
            } else if (subtree instanceof Node) {
                Node node = (Node) subtree;
                List<Spatial> children = node.getChildren();
                for (Spatial child : children) {
                    result = findControlledSpatial(sgc, child);
                    if (result != null) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Find a spatial's 1st local light that is assignable from the specified
     * class.
     *
     * @param <T> subtype of {@link com.jme3.light.Light}
     * @param spatial spatial to search (not null)
     * @param lightClass type of Light to search for (not null)
     * @return a pre-existing instance, or null if none found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Light> T findLight(Spatial spatial,
            Class<T> lightClass) {
        Validate.nonNull(lightClass, "light class");

        for (Light light : spatial.getLocalLightList()) {
            if (lightClass.isAssignableFrom(light.getClass())) {
                return (T) light;
            }
        }

        return null;
    }

    /**
     * Find the 1st local light with the specified name.
     *
     * @param spatial spatial to search (not null, unaffected)
     * @param lightName name to search for (not null)
     * @return a pre-existing instance, or null if none found
     */
    public static Light findLight(Spatial spatial, String lightName) {
        Validate.nonNull(lightName, "light name");

        for (Light light : spatial.getLocalLightList()) {
            String name = light.getName();
            if (lightName.equals(name)) {
                return light;
            }
        }

        return null;
    }

    /**
     * Find the minimum and maximum coordinates of a geometry.
     *
     * @param geometry mesh geometry to measure (not null)
     * @param useWorld true &rarr; use world coordinates, false &rarr; use mesh
     * coordinates
     * @return array consisting of array[0]: the lowest coordinate for each axis
     * and array[1]: the highest coordinate for each axis
     */
    public static Vector3f[] findMinMaxCoords(Geometry geometry,
            boolean useWorld) {
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3f[] result = new Vector3f[]{min, max};

        Vector3f meshLocation = new Vector3f();
        Vector3f location = new Vector3f();

        Mesh mesh = geometry.getMesh();
        int numVertices = mesh.getVertexCount();
        for (int vertexIndex = 0; vertexIndex < numVertices; vertexIndex++) {
            MyMesh.vertexVector3f(mesh, VertexBuffer.Type.Position, vertexIndex,
                    meshLocation);

            if (useWorld && !geometry.isIgnoreTransform()) {
                geometry.localToWorld(meshLocation, location);
            } else {
                location.set(meshLocation);
            }

            MyVector3f.accumulateMinima(min, location);
            MyVector3f.accumulateMaxima(max, location);
        }

        return result;
    }

    /**
     * Find the minimum and maximum coordinates in a subtree of the scene graph.
     * Note: recursive!
     *
     * @param subtree subtree to measure (not null)
     * @return array consisting of array[0]: the lowest coordinate for each axis
     * and array[1]: the highest coordinate for each axis
     */
    public static Vector3f[] findMinMaxCoords(Spatial subtree) {
        Vector3f[] result;
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            result = findMinMaxCoords(geometry, true);

        } else if (subtree instanceof Node) {
            Vector3f maxima = new Vector3f(Float.NEGATIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
            Vector3f minima = new Vector3f(Float.POSITIVE_INFINITY,
                    Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
            result = new Vector3f[]{minima, maxima};
            Node node = (Node) subtree;
            for (Spatial child : node.getChildren()) {
                Vector3f[] childMm = findMinMaxCoords(child);
                MyVector3f.accumulateMinima(minima, childMm[0]);
                MyVector3f.accumulateMaxima(maxima, childMm[1]);
            }

        } else {
            throw new IllegalArgumentException(
                    "spatial should be a geometry or a node");
        }

        return result;
    }

    /**
     * Find a named spatial in the specified subtree of the scene graph. Note:
     * recursive!
     *
     * @param subtree where to search (not null, unaffected)
     * @param name (not null)
     * @return a pre-existing instance, or null if none
     */
    public static Spatial findNamed(Spatial subtree, String name) {
        Spatial result = null;
        String spatialName = subtree.getName();
        if (name.equals(spatialName)) {
            result = subtree;
        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result = findNamed(child, name);
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Calculate the world scale factor of a uniformly scaled spatial.
     *
     * @param spatial spatial to measure (not null, unaffected)
     * @return scale factor
     * @throws IllegalArgumentException if the spatial is scaled non-uniformly
     */
    public static float getUniformScale(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        float result;
        if (isIgnoringTransforms(spatial)) {
            result = 1f;
        } else {
            Vector3f worldScale = spatial.getWorldScale();
            if (worldScale.x != worldScale.y || worldScale.y != worldScale.z) {
                throw new IllegalArgumentException("non-uniform scaling");
            }
            result = worldScale.y;
        }

        return result;
    }

    /**
     * Calculate the world location of a spatial's center.
     *
     * @param spatial spatial to locate (not null, unaffected)
     * @return new vector
     */
    public static Vector3f getWorldLocation(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");
        /*
         * Access the rigid body, if any.
         */
        Vector3f location;
        if (isIgnoringTransforms(spatial)) {
            location = new Vector3f();
        } else {
            location = spatial.getWorldTranslation().clone();
        }

        return location;
    }

    /**
     * Calculate the world orientation of a spatial.
     *
     * @param spatial spatial to orient (not null, unaffected)
     * @return new vector
     */
    public static Quaternion getWorldOrientation(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");
        /*
         * Access the rigid body, if any.
         */
        Quaternion orientation;
        if (isIgnoringTransforms(spatial)) {
            orientation = new Quaternion();
        } else {
            orientation = spatial.getWorldRotation().clone();
        }

        return orientation;
    }

    /**
     * Calculate the world elevation of a perfectly horizontal surface.
     *
     * @param geometry surface to measure (not null)
     * @return elevation of the surface (in world coordinates)
     */
    public static float getYLevel(Geometry geometry) {
        Validate.nonNull(geometry, "geometry");

        Vector3f minMax[] = findMinMaxCoords(geometry, true);
        float result = minMax[0].y;

        if (minMax[0].y != minMax[1].y) {
            throw new IllegalArgumentException("not perfectly horizontal");
        }

        return result;
    }

    /**
     * Test whether a spatial has a specific light in its local list.
     *
     * @param spatial spatial to search (not null, unaffected)
     * @param light light to search for (not null, unaffected)
     * @return true if found, false if not found
     */
    public static boolean hasLight(Spatial spatial, Light light) {
        Validate.nonNull(light, "light");

        for (Light l : spatial.getLocalLightList()) {
            if (l == light) {
                return true;
            }
        }

        return false;
    }

    /**
     * Construct the inverse of a spatial's world orientation, the quaternion
     * that undoes all its rotations.
     *
     * @param spatial spatial to analyze (not null, unaffected)
     * @return new instance
     * @throws IllegalArgumentException if the spatial's world orientation is
     * not invertible
     *
     */
    public static Quaternion inverseOrientation(Spatial spatial) {
        Quaternion forward = spatial.getWorldRotation();
        Quaternion result = forward.inverse();
        if (result == null) {
            throw new IllegalArgumentException("orientation not invertible");
        }

        return result;
    }

    /**
     * Test whether a spatial is a geometry with ignoreTransform=true.
     *
     * @param spatial spatial to test (not null, unaffected)
     * @return true if the spatial ignores transforms, otherwise false
     */
    public static boolean isIgnoringTransforms(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        boolean result = false;
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            if (geometry.isIgnoreTransform()) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Test whether a spatial is an orphan (has no parent node).
     *
     * @param spatial spatial to test (not null, unaffected)
     * @return true if the spatial is an orphan, otherwise false
     */
    public static boolean isOrphan(Spatial spatial) {
        Node parent = spatial.getParent();
        return parent == null;
    }

    /**
     * Enumerate all animated meshes in the specified subtree of a scene graph.
     * Note: recursive!
     *
     * @param subtree (not null, unaffected)
     * @param storeResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    public static List<Mesh> listAnimatedMeshes(Spatial subtree,
            List<Mesh> storeResult) {
        Validate.nonNull(subtree, "subtree");
        if (storeResult == null) {
            storeResult = new ArrayList<>(10);
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (mesh.isAnimated()) {
                storeResult.add(mesh);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listAnimatedMeshes(child, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Enumerate all controls of the specified type in the specified subtree of
     * a scene graph. Note: recursive!
     *
     * @param <T> superclass of Control
     * @param subtree (not null)
     * @param controlType superclass of Control to search for
     * @param storeResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Control> List<T> listControls(Spatial subtree,
            Class<T> controlType, List<T> storeResult) {
        Validate.nonNull(subtree, "subtree");
        if (storeResult == null) {
            storeResult = new ArrayList<>(4);
        }

        int numControls = subtree.getNumControls();
        for (int controlIndex = 0; controlIndex < numControls; controlIndex++) {
            T control = (T) subtree.getControl(controlIndex);
            if (controlType.isAssignableFrom(control.getClass())
                    && !storeResult.contains(control)) {
                storeResult.add(control);
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listControls(child, controlType, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Enumerate all spatials of the specified type in the specified subtree of
     * a scene graph. Note: recursive!
     *
     * @param <T> superclass of Spatial
     * @param subtree (not null, aliases created)
     * @param spatialType superclass of Spatial to search for
     * @param addResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Spatial> List<T> listSpatials(Spatial subtree,
            Class<T> spatialType, List<T> addResult) {
        Validate.nonNull(subtree, "subtree");
        List<T> result = (addResult == null) ? new ArrayList<>(50) : addResult;

        if (spatialType.isAssignableFrom(subtree.getClass())) {
            T spatial = (T) subtree;
            if (!result.contains(spatial)) {
                result.add(spatial);
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listSpatials(child, spatialType, result);
            }
        }

        return result;
    }

    /**
     * Clear all cached collision data from the specified subtree of the scene
     * graph and force a bound refresh. Note: recursive!
     *
     * @param subtree where to search (may be null)
     */
    public static void prepareForCollide(Spatial subtree) {
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            geometry.updateModelBound();
            Mesh mesh = geometry.getMesh();
            mesh.clearCollisionData();

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                prepareForCollide(child);
            }
        }
    }

    /**
     * Remove all controls from the specified subtree of the scene graph. Note:
     * recursive!
     *
     * @param subtree (not null)
     */
    public static void removeAllControls(Spatial subtree) {
        while (subtree.getNumControls() > 0) {
            Control control = subtree.getControl(0);
            subtree.removeControl(control);
        }
        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                removeAllControls(child);
            }
        }
    }

    /**
     * Alter the world location of a spatial's center.
     *
     * @param spatial spatial to relocate (not null)
     * @param worldLocation desired world location (not null, unaffected)
     * @throws IllegalArgumentException if the spatial is a geometry with
     * ignoreTransform=true
     */
    public static void setWorldLocation(Spatial spatial,
            Vector3f worldLocation) {
        Validate.nonNull(worldLocation, "world location");
        if (isIgnoringTransforms(spatial)) {
            throw new IllegalArgumentException("transform ignored");
        }

        Node parent = spatial.getParent();
        if (parent == null) {
            spatial.setLocalTranslation(worldLocation);
        } else {
            Vector3f translation = parent.worldToLocal(worldLocation, null);
            spatial.setLocalTranslation(translation);
        }
    }

    /**
     * Alter the world orientation of a spatial.
     *
     * @param spatial spatial to reorient (not null)
     * @param worldOrientation desired world orientation (not null, unaffected)
     * @throws IllegalArgumentException if the spatial is a geometry with
     * ignoreTransform=true OR the parent's world orientation is not invertible
     */
    public static void setWorldOrientation(Spatial spatial,
            Quaternion worldOrientation) {
        Validate.nonNull(worldOrientation, "world orientation");
        if (isIgnoringTransforms(spatial)) {
            throw new IllegalArgumentException("transform ignored");
        }

        Spatial parent = spatial.getParent();
        if (parent == null) {
            spatial.setLocalRotation(worldOrientation);
        } else {
            Quaternion rotation = inverseOrientation(parent);
            rotation.multLocal(worldOrientation);
            spatial.setLocalRotation(rotation);
        }
    }

    /**
     * Alter the (uniform) world scaling of a spatial.
     *
     * @param spatial spatial to rescale (not null)
     * @param worldScale desired world scale (&gt;0)
     * @throws IllegalArgumentException if the spatial is a geometry with
     * ignoreTransform=true OR the spatial's parent has a zero in its world
     * scale
     */
    public static void setWorldScale(Spatial spatial, float worldScale) {
        Validate.positive(worldScale, "world scale");
        if (isIgnoringTransforms(spatial)) {
            throw new IllegalArgumentException("transform ignored");
        }

        Spatial parent = spatial.getParent();
        if (parent == null) {
            spatial.setLocalScale(worldScale);
        } else {
            Vector3f parentScale = parent.getWorldScale();
            if (parentScale.x == 0f
                    || parentScale.y == 0f
                    || parentScale.z == 0f) {
                throw new IllegalArgumentException();
            }
            Vector3f scale = new Vector3f(worldScale, worldScale, worldScale);
            scale.divideLocal(parentScale);
            spatial.setLocalScale(scale);
        }
    }

    /**
     * Alter the world transform of a spatial.
     *
     * @param spatial spatial to alter (not null)
     * @param worldTransform desired world transform (not null, unaffected)
     * @throws IllegalArgumentException if the spatial is a geometry with
     * ignoreTransform=true OR the spatial's parent has a zero in its world
     * scale OR the parent's world orientation is not invertible
     */
    public static void setWorldTransform(Spatial spatial,
            Transform worldTransform) {
        Validate.nonNull(worldTransform, "world transform");
        if (isIgnoringTransforms(spatial)) {
            throw new IllegalArgumentException("transform ignored");
        }

        Node parent = spatial.getParent();
        if (parent == null) {
            spatial.setLocalTransform(worldTransform);
        } else {
            Transform transform = worldTransform.clone();
            Vector3f translation = transform.getTranslation();
            Quaternion rotation = transform.getRotation();
            Vector3f scale = transform.getScale();

            Transform parentTransform = parent.getWorldTransform();
            Vector3f parentTranslation = parentTransform.getTranslation();
            Quaternion parentRotation = parentTransform.getRotation();
            Vector3f parentScale = parentTransform.getScale();
            if (parentScale.x == 0f || parentScale.y == 0f
                    || parentScale.z == 0f) {
                throw new IllegalArgumentException("zero in scale");
            }
            /*
             * Undo the operations of Transform.combineWithParent()
             */
            Quaternion parentInvRotation = parentRotation.inverse();
            if (parentInvRotation == null) {
                throw new IllegalArgumentException("rotation not invertible");
            }
            scale.divideLocal(parentScale);
            parentInvRotation.mult(rotation, rotation);
            translation.subtractLocal(parentTranslation);
            parentInvRotation.multLocal(translation);
            translation.divideLocal(parentScale);

            spatial.setLocalTransform(transform);
        }
    }

    /**
     * Test whether the specified subtree of a scene graph contains any of the
     * collected spatials.
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param collection spatials to find (not null, unaffected)
     * @return true if one of the collected spatials was found, otherwise false
     */
    public static boolean subtreeContainsAny(Spatial subtree,
            Collection<Spatial> collection) {
        boolean result;
        if (subtree == null) {
            result = false;
        } else if (collection.isEmpty()) {
            result = false;
        } else if (collection.contains(subtree)) {
            result = true;
        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            result = false;
            for (Spatial spatial : collection) {
                result = spatial.hasAncestor(node);
                if (result) {
                    break;
                }
            }
        } else {
            result = false;
        }

        return result;
    }
}
