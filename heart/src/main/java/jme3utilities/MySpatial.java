/*
 Copyright (c) 2013-2019, Stephen Gold
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
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.effect.ParticleEmitter;
import com.jme3.font.BitmapText;
import com.jme3.light.Light;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
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
     * Count all controls of the specified type in the specified subtree of a
     * scene graph. Note: recursive!
     *
     * @param <T> subclass of Control
     * @param subtree the subtree to analyze (may be null, unaffected)
     * @param controlType the subclass of Control to search for
     * @return the count (&ge;0)
     */
    public static <T extends Control> int countControls(Spatial subtree,
            Class<T> controlType) {
        int result = 0;

        if (subtree != null) {
            int numControls = subtree.getNumControls();
            for (int controlI = 0; controlI < numControls; ++controlI) {
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
     * @param subtree the subtree to analyze (may be null)
     * @return the estimated count (&ge;0)
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
     * @param <T> subclass of Spatial
     * @param subtree the subtree to analyze (may be null, unaffected)
     * @param spatialType the subclass of Spatial to search for
     * @return the count (&ge;0)
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
     * @param subtree the subtree to analyze (may be null, unaffected)
     * @return the count (&ge;0)
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
     * Count all uses of the specified Material in the specified subtree of a
     * scene graph. Note: recursive!
     *
     * @param subtree the subtree to analyze (may be null, unaffected)
     * @param material (unaffected)
     * @return the use count (&ge;0)
     */
    public static int countUses(Spatial subtree, Material material) {
        int count = 0;

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Material mat = geometry.getMaterial();
            if (mat == material) {
                ++count;
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                count += countUses(child, material);
            }
        }

        return count;
    }

    /**
     * Count how many mesh vertices are contained in the specified subtree of a
     * scene graph. Note: recursive!
     *
     * @param subtree the subtree to analyze (may be null, unaffected)
     * @return the number of vertices (&ge;0)
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
     * Generate a single-character description of a Spatial.
     *
     * @param spatial the Spatial to describe (unaffected, may be null)
     * @return a mnemonic character
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
        } else if (spatial.getClass().getSimpleName().equals("TerrainQuad")) {
            return 'q';
        } else if (spatial instanceof Geometry) {
            return 'g';
        } else if (spatial instanceof Node) {
            return 'n';
        }
        return '?';
    }

    /**
     * Find an animated geometry in the specified subtree of a scene graph.
     * Note: recursive!
     *
     * @param subtree the subtree to search (not null, unaffected)
     * @return a pre-existing Geometry, or null if none
     */
    public static Geometry findAnimatedGeometry(Spatial subtree) {
        Geometry result = null;
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (MyMesh.isAnimated(mesh)) {
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
     * Find the first child of the specified Node that's assignable from the
     * specified class.
     *
     * @param <T> subtype of {@link com.jme3.scene.Spatial}
     * @param node the Node to search (not null)
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
     * Find the first child of the specified Node with the specified name. The
     * search is case-sensitive and looks only at the node's immediate children,
     * not all its descendents.
     *
     * @param node the Node to search (not null)
     * @param childName the name to search for (not null)
     * @return a pre-existing Spatial, or null if none found
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
     * Find a Spatial controlled by the specified S-G control in the specified
     * subtree of a scene graph. Note: recursive!
     *
     * @param sgc which scene-graph control (not null, unaffected)
     * @param subtree the subtree to search (not null, unaffected)
     * @return a pre-existing Spatial, or null if none found
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
     * Find the index of the specified scene-graph control in the specified
     * Spatial.
     *
     * @param spatial the Spatial to search (not null, unaffected)
     * @param sgc the Control to search for (not null, unaffected)
     * @return the index (&ge;0) or -1 if not found
     */
    public static int findIndex(Spatial spatial, Control sgc) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(sgc, "control");

        int numControls = spatial.getNumControls();
        int result = -1;
        for (int controlIndex = 0; controlIndex < numControls; ++controlIndex) {
            Control control = spatial.getControl(controlIndex);
            if (control == sgc) {
                result = controlIndex;
                break;
            }
        }

        return result;
    }

    /**
     * Find the first local light of the specified Spatial that is assignable
     * from the specified class.
     *
     * @param <T> subtype of {@link com.jme3.light.Light}
     * @param spatial the Spatial to search (not null)
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
     * Find the first local light of the specified Spatial with the specified
     * name.
     *
     * @param spatial the Spatial to search (not null, unaffected)
     * @param lightName the name to search for (not null)
     * @return a pre-existing Light, or null if none found
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
     * Find the minimum and maximum coordinates of a Geometry.
     *
     * @param geometry the Geometry to measure (not null)
     * @param useWorld true &rarr; use world coordinates, false &rarr; use mesh
     * coordinates
     * @return a new array consisting of array[0]: the lowest coordinate for
     * each axis and array[1]: the highest coordinate for each axis
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
        for (int vertexIndex = 0; vertexIndex < numVertices; ++vertexIndex) {
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
     * Find the minimum and maximum coordinates in a subtree of a scene graph.
     * Note: recursive!
     *
     * @param subtree subtree to measure (not null)
     * @return a new array consisting of array[0]: the lowest coordinate for
     * each world axis and array[1]: the highest coordinate for each world axis
     * (not null)
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
     * Find the named Spatial in the specified subtree of a scene graph. Note:
     * recursive!
     *
     * @param subtree the subtree to search in (not null, unaffected)
     * @param name the name to search for (not null)
     * @return a pre-existing Spatial, or null if none
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
     * Find a named M-P override in the specified Spatial.
     *
     * @param spatial the Spatial to search in (not null)
     * @param parameterName the name of the M-P override (not null, not empty)
     * @return a pre-existing instance, or null if not found
     */
    public static MatParamOverride findOverride(Spatial spatial,
            String parameterName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonEmpty(parameterName, "parameterName");

        MatParamOverride result = null;
        Collection<MatParamOverride> list = spatial.getLocalMatParamOverrides();
        for (MatParamOverride override : list) {
            String name = override.getName();
            if (parameterName.equals(name)) {
                result = override;
                break;
            }
        }

        return result;
    }

    /**
     * Test whether the specified Spatial has the specified Light in its local
     * list.
     *
     * @param spatial the Spatial to analyze (not null, unaffected)
     * @param light the Light to search for (not null, unaffected)
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
     * Construct the inverse of a spatial's world orientation, the Quaternion
     * that undoes all its rotations.
     *
     * @param spatial the Spatial to analyze (not null, unaffected)
     * @return new instance
     * @throws IllegalArgumentException if the spatial's world orientation is
     * not invertible
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
     * Test whether a Spatial is a Geometry with ignoreTransform=true.
     *
     * @param spatial the Spatial to test (may be null, unaffected)
     * @return true if the Spatial ignores transforms, otherwise false
     */
    public static boolean isIgnoringTransforms(Spatial spatial) {
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
     * Test whether a Spatial is an orphan or root (has no parent node).
     *
     * @param spatial the Spatial to test (not null, unaffected)
     * @return true if the Spatial is an orphan, otherwise false
     */
    public static boolean isOrphan(Spatial spatial) {
        Node parent = spatial.getParent();
        return parent == null;
    }

    /**
     * Enumerate all animated meshes in the specified subtree of a scene graph.
     * Note: recursive!
     *
     * @param subtree the subtree to analyze (may be null, aliases created)
     * @param storeResult storage for results (added to if not null)
     * @return an expanded List (either storeResult or a new List)
     */
    public static List<Mesh> listAnimatedMeshes(Spatial subtree,
            List<Mesh> storeResult) {
        if (storeResult == null) {
            storeResult = new ArrayList<>(10);
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (MyMesh.isAnimated(mesh) && !storeResult.contains(mesh)) {
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
     * @param <T> subclass of Control
     * @param subtree the subtree to analyze (not null, aliases created)
     * @param controlType the subclass of Control to search for
     * @param storeResult storage for results (added to if not null)
     * @return an expanded List (either storeResult or a new List)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Control> List<T> listControls(Spatial subtree,
            Class<T> controlType, List<T> storeResult) {
        if (storeResult == null) {
            storeResult = new ArrayList<>(4);
        }

        int numControls = subtree.getNumControls();
        for (int controlIndex = 0; controlIndex < numControls; ++controlIndex) {
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
     * Enumerate all materials in the specified subtree of a scene graph. Note:
     * recursive!
     *
     * @param subtree the subtree to analyze (may be null, aliases created)
     * @param storeResult storage for results (added to if not null)
     * @return an expanded List (either storeResult or a new List)
     * @see MyMesh#listMeshes(com.jme3.scene.Spatial, java.util.List)
     */
    public static List<Material> listMaterials(Spatial subtree,
            List<Material> storeResult) {
        if (storeResult == null) {
            storeResult = new ArrayList<>(10);
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Material material = geometry.getMaterial();
            if (!storeResult.contains(material)) {
                storeResult.add(material);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listMaterials(child, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Enumerate all geometries using the specified Material in the specified
     * subtree of a scene graph. Note: recursive!
     *
     * @param subtree the subtree to analyze (may be null, aliases created)
     * @param material the Material to search for (may be null, unaffected)
     * @param addResult storage for results (added to if not null)
     * @return an expanded List (either storeResult or a new List)
     */
    public static List<Geometry> listMaterialUsers(Spatial subtree,
            Material material, List<Geometry> addResult) {
        List<Geometry> result
                = (addResult == null) ? new ArrayList<Geometry>(50) : addResult;

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            if (geometry.getMaterial() == material) {
                result.add(geometry);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listMaterialUsers(child, material, result);
            }
        }

        return result;
    }

    /**
     * Enumerate all geometries using the specified Mesh in the specified
     * subtree of a scene graph. Note: recursive!
     *
     * @param subtree (may be null, aliases created)
     * @param mesh the Mesh to search for (may be null, unaffected)
     * @param addResult storage for results (added to if not null)
     * @return an expanded List (either storeResult or a new List)
     */
    public static List<Geometry> listMeshUsers(Spatial subtree, Mesh mesh,
            List<Geometry> addResult) {
        List<Geometry> result
                = (addResult == null) ? new ArrayList<Geometry>(50) : addResult;

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            if (geometry.getMesh() == mesh) {
                result.add(geometry);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listMeshUsers(child, mesh, result);
            }
        }

        return result;
    }

    /**
     * Enumerate all spatials in the specified subtree of a scene graph.
     *
     * @see com.jme3.scene.Node#descendantMatches(java.lang.Class)
     *
     * @param subtree (not null, aliases created)
     * @return a new List (not null, not empty)
     */
    public static List<Spatial> listSpatials(Spatial subtree) {
        Validate.nonNull(subtree, "subtree");
        List<Spatial> result = listSpatials(subtree, Spatial.class, null);
        return result;
    }

    /**
     * Enumerate all spatials of the specified type in the specified subtree of
     * a scene graph. Note: recursive!
     *
     * @see com.jme3.scene.Node#descendantMatches(java.lang.Class)
     *
     * @param <T> subclass of Spatial
     * @param subtree (not null, aliases created)
     * @param spatialType the subclass of Spatial to search for
     * @param addResult storage for results (added to if not null)
     * @return an expanded List (either storeResult or a new List)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Spatial> List<T> listSpatials(Spatial subtree,
            Class<T> spatialType, List<T> addResult) {
        Validate.nonNull(subtree, "subtree");
        List<T> result = (addResult == null) ? new ArrayList<T>(50) : addResult;

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
     * Remove all controls from the specified subtree of a scene graph. Note:
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
     * @throws IllegalArgumentException if the Spatial is a Geometry with
     * ignoreTransform=true
     */
    public static void setWorldLocation(Spatial spatial,
            Vector3f worldLocation) {
        Validate.finite(worldLocation, "world location");
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
     * @throws IllegalArgumentException if the Spatial is a Geometry with
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
     * @throws IllegalArgumentException if the Spatial is a Geometry with
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
     * @throws IllegalArgumentException if the Spatial is a Geometry with
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

    /**
     * Calculate the world scale factor of a uniformly scaled spatial.
     *
     * @param spatial the Spatial to analyze (not null, unaffected)
     * @return the scale factor
     * @throws IllegalArgumentException if the Spatial is scaled non-uniformly
     */
    public static float uniformScale(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        float result;
        if (isIgnoringTransforms(spatial)) {
            result = 1f;
        } else {
            Vector3f worldScale = spatial.getWorldScale();
            if (!MyVector3f.isScaleUniform(worldScale)) {
                throw new IllegalArgumentException("non-uniform scaling");
            }
            result = worldScale.y;
        }

        return result;
    }

    /**
     * Apply new materials to all animated meshes in the specified subtree in
     * order to visualize their bone weights.
     *
     * @param subtree the subtree to modify (may be null)
     * @param boneIndexToColor map bone indices to colors (not null, unaffected)
     * @param assetManager (not null)
     */
    public static void visualizeBoneWeights(Spatial subtree,
            ColorRGBA[] boneIndexToColor, AssetManager assetManager) {
        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            for (Spatial spatial : node.getChildren()) {
                visualizeBoneWeights(spatial, boneIndexToColor, assetManager);
            }
        } else if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (MyMesh.isAnimated(mesh)) {
                Material material = MyMesh.boneWeightMaterial(mesh,
                        boneIndexToColor, assetManager);
                geometry.setMaterial(material);
            }
        }
    }

    /**
     * Calculate the world location of a spatial's center.
     *
     * @param spatial spatial to locate (not null)
     * @param storeResult storage for the result (modified if not null)
     * @return the location vector (in world coordinates, either storeResult or
     * a new instance)
     */
    public static Vector3f worldLocation(Spatial spatial,
            Vector3f storeResult) {
        Validate.nonNull(spatial, "spatial");
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        if (isIgnoringTransforms(spatial)) {
            result.zero();
        } else {
            Vector3f location = spatial.getWorldTranslation();
            result.set(location);
        }

        return result;
    }

    /**
     * Calculate the world orientation of a spatial.
     *
     * @param spatial the Spatial to analyze (not null)
     * @param storeResult storage for the result (modified if not null)
     * @return the orientation (in world coordinates, either storeResult or a
     * new instance)
     */
    public static Quaternion worldOrientation(Spatial spatial,
            Quaternion storeResult) {
        Validate.nonNull(spatial, "spatial");
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        if (isIgnoringTransforms(spatial)) {
            result.loadIdentity();
        } else {
            Quaternion orientation = spatial.getWorldRotation();
            result.set(orientation);
        }

        return result;
    }

    /**
     * Calculate the world scale of a spatial.
     *
     * @param spatial the Spatial to analyze (not null)
     * @param storeResult storage for the result (modified if not null)
     * @return the scale vector (in world coordinates, either storeResult or a
     * new instance)
     */
    public static Vector3f worldScale(Spatial spatial, Vector3f storeResult) {
        Validate.nonNull(spatial, "spatial");
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        if (isIgnoringTransforms(spatial)) {
            result.set(1f, 1f, 1f);
        } else {
            Vector3f scale = spatial.getWorldScale();
            result.set(scale);
        }

        return result;
    }

    /**
     * Calculate the world transform of a Spatial.
     *
     * @param spatial the Spatial to analyze (not null)
     * @param storeResult storage for the result (modified if not null)
     * @return the Transform (in world coordinates, either storeResult or a new
     * instance)
     */
    public static Transform worldTransform(Spatial spatial,
            Transform storeResult) {
        Validate.nonNull(spatial, "spatial");
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        if (isIgnoringTransforms(spatial)) {
            result.loadIdentity();
        } else {
            Transform transform = spatial.getWorldTransform();
            result.set(transform);
        }

        return result;
    }
}
