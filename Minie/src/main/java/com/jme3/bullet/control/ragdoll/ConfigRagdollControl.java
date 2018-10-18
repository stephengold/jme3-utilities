/*
 * Copyright (c) 2018 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.control.ragdoll;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.control.*;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Methods to configure a ragdoll control and access its configuration and also
 * some protected utility methods.
 *
 * @author Stephen Gold sgold@sonic.net
 */
abstract public class ConfigRagdollControl extends AbstractPhysicsControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(ConfigRagdollControl.class.getName());
    /**
     * magic bone name to refer to the ragdoll's torso TODO rename torsoLinkName
     */
    final public static String torsoFakeBoneName = "";
    // *************************************************************************
    // fields

    /**
     * map linked bone names to masses for createSpatialData()
     */
    private Map<String, Float> massMap = new HashMap<>(32);
    /**
     * map linked bone names to joint presets for createSpatialData()
     */
    private Map<String, JointPreset> jointMap = new HashMap<>(32);
    /**
     * mass of the torso (default=15)
     */
    private float torsoMass = 15f;
    /**
     * gravitational acceleration for ragdoll (default is 9.8 in the -Y
     * direction, corresponding to Earth-normal in MKS units)
     */
    private Vector3f gravityVector = new Vector3f(0f, -9.8f, 0f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control without any linked bones (torso only).
     */
    public ConfigRagdollControl() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Count the linked bones.
     *
     * @return count (&ge;0)
     */
    public int countLinkedBones() {
        int result = massMap.size();
        return result;
    }

    /**
     * Access the limits of the joint connecting the named linked bone to its
     * parent in the hierarchy.
     *
     * @param boneName the name of the linked bone (not null, not empty)
     * @return the pre-existing instance (not null)
     */
    public JointPreset getJointLimits(String boneName) {
        if (!isBoneLinkName(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        JointPreset result = jointMap.get(boneName);

        assert result != null;
        return result;
    }

    /**
     * Copy this control's gravitational acceleration for Ragdoll mode.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return an acceleration vector (in physics-space coordinates, either
     * storeResult or a new vector, not null)
     */
    public Vector3f gravity(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        result.set(gravityVector);
        return result;
    }

    /**
     * Test whether the named bone link exists.
     *
     * @param name (may be null)
     * @return true if found, otherwise false
     */
    public boolean isBoneLinkName(String name) {
        boolean result;
        if (name == null) {
            result = false;
        } else {
            result = massMap.containsKey(name);
        }

        return result;
    }

    /**
     * Test whether the named link exists.
     *
     * @param name (may be null)
     * @return true if found, otherwise false
     */
    public boolean isLinkName(String name) {
        boolean result;
        if (name == null) {
            result = false;
        } else if (name.equals(torsoFakeBoneName)) {
            result = true;
        } else {
            result = massMap.containsKey(name);
        }

        return result;
    }

    /**
     * Link the named bone using the specified mass and range of motion.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the bone to link (not null, not empty)
     * @param mass the desired mass of the bone (&gt;0)
     * @param jointPreset the desired range of motion (not null)
     * @see #setJointLimits(java.lang.String,
     * com.jme3.bullet.control.ragdoll.JointPreset)
     */
    public void link(String boneName, float mass, JointPreset jointPreset) {
        Validate.nonEmpty(boneName, "bone name");
        Validate.positive(mass, "mass");
        Validate.nonNull(jointPreset, "joint preset");
        if (getSpatial() != null) {
            throw new IllegalStateException(
                    "Cannot link a bone while added to a spatial.");
        }

        jointMap.put(boneName, jointPreset);
        massMap.put(boneName, mass);
    }

    /**
     * Enumerate all linked bones in this control.
     *
     * @return an unmodifiable collection of names
     */
    public Collection<String> linkedBoneNames() {
        assert massMap.size() == jointMap.size();
        Collection<String> names = massMap.keySet();
        Collection<String> result = Collections.unmodifiableCollection(names);
        assert result.size() == massMap.size();

        return result;
    }

    /**
     * Read the mass of the named link.
     *
     * @param boneName the name of the link (not null)
     * @return the mass (&gt;0)
     */
    public float mass(String boneName) {
        float mass;

        if (torsoFakeBoneName.equals(boneName)) {
            mass = torsoMass;
        } else if (isBoneLinkName(boneName)) {
            mass = massMap.get(boneName);
        } else {
            String msg = "No link named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }

        assert mass > 0f : mass;
        return mass;
    }

    /**
     * Alter this control's gravitational acceleration for Ragdoll mode.
     *
     * @param gravity the desired acceleration vector (in physics-space
     * coordinates, not null, unaffected, default=0,-9.8,0)
     */
    public void setGravity(Vector3f gravity) {
        Validate.nonNull(gravity, "gravity");
        gravityVector.set(gravity);
    }

    /**
     * Alter the limits of the joint connecting the named linked bone to its
     * parent in the linked-bone hierarchy.
     *
     * @param boneName the name of the linked bone (not null, not empty)
     * @param preset the desired range of motion (not null)
     */
    public void setJointLimits(String boneName, JointPreset preset) {
        Validate.nonNull(preset, "preset");
        if (!isBoneLinkName(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }

        jointMap.put(boneName, preset);
    }

    /**
     * Alter the mass of the named link.
     *
     * @param boneName the name of the link (not null)
     * @param mass the desired mass (&gt;0)
     */
    public void setMass(String boneName, float mass) {
        Validate.positive(mass, "mass");

        if (torsoFakeBoneName.equals(boneName)) {
            torsoMass = mass;
        } else if (isBoneLinkName(boneName)) {
            massMap.put(boneName, mass);
        } else {
            String msg = "No link named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Calculate the ragdoll's total mass.
     *
     * @return the total amount (&gt;0)
     */
    public float totalMass() {
        float totalMass = torsoMass;
        for (float mass : massMap.values()) {
            totalMass += mass;
        }

        assert totalMass > 0f : totalMass;
        return totalMass;
    }

    /**
     * Unlink the named bone.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the linked bone to unlink (not null, not
     * empty)
     */
    public void unlink(String boneName) {
        if (!isBoneLinkName(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        if (getSpatial() != null) {
            throw new IllegalStateException(
                    "Cannot unlink a bone while added to a spatial.");
        }

        jointMap.remove(boneName);
        massMap.remove(boneName);
    }
    // *************************************************************************
    // AbstractPhysicsControl methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned control into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        massMap = cloner.clone(massMap);
        jointMap = cloner.clone(jointMap);
        gravityVector = cloner.clone(gravityVector);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public ConfigRagdollControl jmeClone() {
        try {
            ConfigRagdollControl clone
                    = (ConfigRagdollControl) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * De-serialize this control, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        // TODO jointMap, massMap
        torsoMass = ic.readFloat("rootMass", 15f);
        gravityVector = (Vector3f) ic.readSavable("gravity", null);
    }

    /**
     * Render this control. Invoked once per view port per frame, provided the
     * control is added to a scene. Should be invoked only by a subclass or by
     * the RenderManager.
     *
     * @param rm the render manager (not null)
     * @param vp the view port to render (not null)
     */
    @Override
    public void render(RenderManager rm, ViewPort vp) {
    }

    /**
     * Serialize this control, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);

        int count = countLinkedBones();
        String[] linkedBoneNames = new String[count];
        float[] linkedBoneMasses = new float[count];
        JointPreset[] linkedBoneJoints = new JointPreset[count];
        int i = 0;
        for (Map.Entry<String, Float> e : massMap.entrySet()) {
            linkedBoneNames[i] = e.getKey();
            linkedBoneMasses[i] = e.getValue();
            linkedBoneJoints[i] = jointMap.get(e.getKey());
            i++;
        }
        oc.write(linkedBoneNames, "linkedBoneNames", null);
        oc.write(linkedBoneMasses, "linkedBoneMasses", null);
        oc.write(linkedBoneJoints, "linkedBoneJOints", null);

        oc.write(torsoMass, "rootMass", 15f);
        oc.write(gravityVector, "gravity", new Vector3f(0f, -9.8f, 0f));
    }
    // *************************************************************************
    // protected methods

    /**
     * Add unlinked descendants of the specified bone to the specified list.
     * Note: recursive.
     *
     * @param bone (not null, alias created)
     * @param addResult (not null, modified)
     */
    protected void addUnlinkedDescendants(Bone bone, List<Bone> addResult) {
        for (Bone childBone : bone.getChildren()) {
            String childName = childBone.getName();
            if (!isLinkName(childName)) {
                addResult.add(childBone);
                addUnlinkedDescendants(childBone, addResult);
            }
        }
    }

    /**
     * Assign each mesh vertex to a link and add its location (mesh coordinates
     * in bind pose) to that link's list. TODO move to RagUtils
     *
     * @param meshes array of animated meshes to use (not null, unaffected)
     * @param managerMap a map from bone indices to managing link names
     * @return a new map from link names to coordinates
     */
    protected static Map<String, List<Vector3f>> coordsMap(Mesh[] meshes,
            String[] managerMap) {
        float[] wArray = new float[4];
        int[] iArray = new int[4];
        Map<String, List<Vector3f>> coordsMap = new HashMap<>(32);
        for (Mesh mesh : meshes) {
            int numVertices = mesh.getVertexCount();
            for (int vertexI = 0; vertexI < numVertices; vertexI++) {
                MyMesh.vertexBoneIndices(mesh, vertexI, iArray);
                MyMesh.vertexBoneWeights(mesh, vertexI, wArray);

                Map<String, Float> weightMap
                        = weightMap(iArray, wArray, managerMap);

                float bestTotalWeight = Float.NEGATIVE_INFINITY;
                String bestLbName = null;
                for (Map.Entry<String, Float> entry : weightMap.entrySet()) {
                    float totalWeight = entry.getValue();
                    if (totalWeight >= bestTotalWeight) {
                        bestTotalWeight = totalWeight;
                        bestLbName = entry.getKey();
                    }
                }
                /*
                 * Add the bind-pose coordinates of the vertex
                 * to the linked bone's list.
                 */
                List<Vector3f> coordList;
                if (coordsMap.containsKey(bestLbName)) {
                    coordList = coordsMap.get(bestLbName);
                } else {
                    coordList = new ArrayList<>(20);
                    coordsMap.put(bestLbName, coordList);
                }
                Vector3f bindPosition = MyMesh.vertexVector3f(mesh,
                        VertexBuffer.Type.BindPosePosition, vertexI, null);
                coordList.add(bindPosition);
            }
        }

        return coordsMap;
    }

    /**
     * Create a hull collision shape, using the specified inverse transform and
     * list of vertex locations. The skeleton is assumed to be in bind pose.
     * TODO move to RagUtils
     *
     * @param transform from vertex coordinates to descaled shape coordinates
     * (not null, unaffected)
     * @param vertexLocations list of vertex locations (not null, not empty,
     * unaffected)
     * @return a new shape
     */
    protected static CollisionShape createShape(Transform transform,
            List<Vector3f> vertexLocations) {
        assert transform != null;
        assert vertexLocations != null;
        assert !vertexLocations.isEmpty();

        for (Vector3f location : vertexLocations) {
            /*
             * Transform mesh coordinates to descaled shape coordinates.
             */
            transform.transformVector(location, location);
        }

        CollisionShape boneShape = new HullCollisionShape(vertexLocations);

        return boneShape;
    }

    /**
     * Map bone indices to names of the links that manage them.
     *
     * @param skeleton (not null, unaffected)
     * @return a new array of link names
     */
    protected String[] managerMap(Skeleton skeleton) {
        int numBones = skeleton.getBoneCount();
        String[] nameArray = new String[numBones];
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            while (true) {
                String boneName = bone.getName();
                if (isBoneLinkName(boneName)) {
                    nameArray[boneIndex] = boneName;
                    break;
                }
                bone = bone.getParent();
                if (bone == null) {
                    nameArray[boneIndex] = torsoFakeBoneName;
                    break;
                }
            }
        }

        return nameArray;
    }

    /**
     * Validate a skeleton. TODO move to RagUtils
     *
     * @param skeleton the skeleton to validate (not null, unaffected)
     */
    protected static void validate(Skeleton skeleton) {
        int numBones = skeleton.getBoneCount();
        if (numBones < 0) {
            throw new IllegalArgumentException("Bone count is negative!");
        }

        Set<String> nameSet = new TreeSet<>();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            if (bone == null) {
                String msg = String.format("Bone %d in skeleton is null!",
                        boneIndex);
                throw new IllegalArgumentException(msg);
            }
            String boneName = bone.getName();
            if (boneName == null) {
                String msg = String.format("Bone %d in skeleton has null name!",
                        boneIndex);
                throw new IllegalArgumentException(msg);
            } else if (boneName.equals(torsoFakeBoneName)) {
                String msg = String.format(
                        "Bone %d in skeleton has a reserved name!",
                        boneIndex);
                throw new IllegalArgumentException(msg);
            } else if (nameSet.contains(boneName)) {
                String msg = "Duplicate bone name in skeleton: "
                        + MyString.quote(boneName);
                throw new IllegalArgumentException(msg);
            }
            nameSet.add(boneName);
        }
    }

    /**
     * Tabulate the total bone weight associated with each link. TODO move to
     * RagUtils
     *
     * @param biArray the array of bone indices (not null, unaffected)
     * @param bwArray the array of bone weights (not null, unaffected)
     * @param lbNames a map from bone indices to managing link names (not null,
     * unaffected)
     * @return a new map from linked-bone names to total weight
     */
    protected static Map<String, Float> weightMap(int[] biArray,
            float[] bwArray, String[] lbNames) {
        assert biArray.length == 4;
        assert bwArray.length == 4;

        Map<String, Float> weightMap = new HashMap<>(4);
        for (int j = 0; j < 4; j++) {
            int boneIndex = biArray[j];
            if (boneIndex != -1) {
                String lbName = lbNames[boneIndex];
                if (weightMap.containsKey(lbName)) {
                    float oldWeight = weightMap.get(lbName);
                    float newWeight = oldWeight + bwArray[j];
                    weightMap.put(lbName, newWeight);
                } else {
                    weightMap.put(lbName, bwArray[j]);
                }
            }
        }

        return weightMap;
    }
}
