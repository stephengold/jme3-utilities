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
package com.jme3.bullet.animation;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.bullet.control.AbstractPhysicsControl;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Methods to configure a DynamicAnimControl and access its configuration.
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
abstract public class ConfigDynamicAnimControl extends AbstractPhysicsControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(ConfigDynamicAnimControl.class.getName());
    /**
     * name for the ragdoll's torso, must not be used for any bone
     */
    final public static String torsoName = "";
    // *************************************************************************
    // fields

    /**
     * mass of the torso (default=1)
     */
    private float torsoMass = 1f;
    /**
     * map attachment bone names to masses for createSpatialData()
     */
    private Map<String, Float> attachMassMap = new HashMap<>(5);
    /**
     * map linked bone names to masses for createSpatialData()
     */
    private Map<String, Float> massMap = new HashMap<>(50);
    /**
     * map linked bone names to ranges of motion for createSpatialData()
     */
    private Map<String, RangeOfMotion> jointMap = new HashMap<>(50);
    /**
     * map attachment bone names to models for createSpatialData()
     */
    private Map<String, Spatial> attachModelMap = new HashMap<>(5);
    /**
     * gravitational acceleration vector for ragdolls (default is 9.8 in the -Y
     * direction, approximating Earth-normal in MKS units)
     */
    private Vector3f gravityVector = new Vector3f(0f, -9.8f, 0f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control without any attachments or linked bones
     * (torso only).
     */
    public ConfigDynamicAnimControl() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Configure the specified model as an attachment.
     *
     * @param boneName the name of the associated bone (not null, not empty)
     * @param mass the desired mass of the model (&gt;0)
     * @param model the model to attach (not null, orphan, alias created)
     */
    public void attach(String boneName, float mass, Spatial model) {
        Validate.nonEmpty(boneName, "bone name");
        Validate.positive(mass, "mass");
        RagUtils.validate(model);
        assert MySpatial.isOrphan(model);
        if (getSpatial() != null) {
            throw new IllegalStateException(
                    "Cannot attach a model while added to a spatial.");
        }
        if (hasAttachmentLink(boneName)) {
            logger2.log(Level.WARNING, "Bone {0} already had an attachment.",
                    MyString.quote(boneName));
        }

        attachModelMap.put(boneName, model);
        attachMassMap.put(boneName, mass);
    }

    /**
     * Read the mass of the attachment associated with the named bone.
     *
     * @param boneName the name of the associated bone (not null, not empty)
     * @return the mass (&gt;0)
     */
    public float attachmentMass(String boneName) {
        float mass;

        if (attachMassMap.containsKey(boneName)) {
            mass = attachMassMap.get(boneName);
        } else {
            String msg = "No attachment link for " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }

        assert mass > 0f : mass;
        return mass;
    }

    /**
     * Count the attachments.
     *
     * @return count (&ge;0)
     */
    public int countAttachments() {
        int count = attachMassMap.size();

        assert count == attachModelMap.size();
        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the linked bones.
     *
     * @return count (&ge;0)
     */
    public int countLinkedBones() {
        int count = massMap.size();

        assert count == jointMap.size();
        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the links.
     *
     * @return count (&ge;0)
     */
    public int countLinks() {
        int result = countLinkedBones() + countAttachments() + 1;
        return result;
    }

    /**
     * Cancel the attachment associated with the named bone.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the bone (not null, not empty)
     */
    public void detach(String boneName) {
        if (!hasBoneLink(boneName)) {
            String msg = "No attachment associated with "
                    + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        if (getSpatial() != null) {
            throw new IllegalStateException(
                    "Cannot cancel an attachment while added to a spatial.");
        }

        jointMap.remove(boneName);
        massMap.remove(boneName);
    }

    /**
     * Access the model attached to the named bone.
     *
     * @param boneName the name of the associated bone (not null, not empty)
     * @return the orphan spatial (not null)
     */
    public Spatial getAttachmentModel(String boneName) {
        Spatial model;

        if (attachModelMap.containsKey(boneName)) {
            model = attachModelMap.get(boneName);
        } else {
            String msg = "No attachment link for " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }

        assert model != null;
        assert MySpatial.isOrphan(model);
        return model;
    }

    /**
     * Access the nominal range of motion for the joint connecting the named
     * linked bone to its parent in the hierarchy.
     *
     * @param boneName the name of the linked bone (not null, not empty)
     * @return the pre-existing instance (not null)
     */
    public RangeOfMotion getJointLimits(String boneName) {
        if (!hasBoneLink(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        RangeOfMotion result = jointMap.get(boneName);

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
     * Test whether an AttachmentLink exists for the named bone.
     *
     * @param boneName the name of the associated bone (may be null)
     * @return true if found, otherwise false
     */
    public boolean hasAttachmentLink(String boneName) {
        boolean result;
        if (boneName == null) {
            result = false;
        } else {
            result = attachMassMap.containsKey(boneName);
        }

        return result;
    }

    /**
     * Test whether a BoneLink exists for the named bone.
     *
     * @param boneName the name of the bone (may be null)
     * @return true if found, otherwise false
     */
    public boolean hasBoneLink(String boneName) {
        boolean result;
        if (boneName == null) {
            result = false;
        } else {
            result = massMap.containsKey(boneName);
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
     * @param rom the desired range of motion (not null)
     * @see #setJointLimits(java.lang.String,
     * com.jme3.bullet.animation.RangeOfMotion)
     */
    public void link(String boneName, float mass, RangeOfMotion rom) {
        Validate.nonEmpty(boneName, "bone name");
        Validate.positive(mass, "mass");
        Validate.nonNull(rom, "range of motion");
        if (getSpatial() != null) {
            throw new IllegalStateException(
                    "Cannot link a bone while added to a spatial.");
        }
        if (hasBoneLink(boneName)) {
            logger2.log(Level.WARNING, "Bone {0} is already linked.",
                    MyString.quote(boneName));
        }

        jointMap.put(boneName, rom);
        massMap.put(boneName, mass);
    }

    /**
     * Enumerate all bones with attachment links.
     *
     * @return a new array of bone names (not null, may be empty)
     */
    public String[] listAttachmentBoneNames() {
        int size = countAttachments();
        String[] result = new String[size];
        Collection<String> names = attachMassMap.keySet();
        names.toArray(result);

        return result;
    }

    /**
     * Enumerate all bones with bone links.
     *
     * @return a new array of bone names (not null, may be empty)
     */
    public String[] listLinkedBoneNames() {
        int size = countLinkedBones();
        String[] result = new String[size];
        Collection<String> names = massMap.keySet();
        names.toArray(result);

        return result;
    }

    /**
     * Read the mass of the named bone/torso.
     *
     * @param boneName the name of the bone/torso (not null)
     * @return the mass (&gt;0)
     */
    public float mass(String boneName) {
        float mass;

        if (torsoName.equals(boneName)) {
            mass = torsoMass;
        } else if (hasBoneLink(boneName)) {
            mass = massMap.get(boneName);
        } else {
            String msg = "No bone/torso named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }

        assert mass > 0f : mass;
        return mass;
    }

    /**
     * Alter the mass of the attachment associated with the named bone.
     *
     * @param boneName the name of the associated bone (not null, not empty)
     * @param mass the desired mass (&gt;0)
     */
    public void setAttachmentMass(String boneName, float mass) {
        Validate.positive(mass, "mass");

        if (attachMassMap.containsKey(boneName)) {
            attachMassMap.put(boneName, mass);
        } else {
            String msg = "No attachment link for " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
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
     * Alter the range of motion of the joint connecting the named BoneLink to
     * its parent in the link hierarchy.
     *
     * @param boneName the name of the BoneLink (not null, not empty)
     * @param rom the desired range of motion (not null)
     */
    public void setJointLimits(String boneName, RangeOfMotion rom) {
        Validate.nonNull(rom, "range of motion");
        if (!hasBoneLink(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }

        jointMap.put(boneName, rom);
    }

    /**
     * Alter the mass of the named bone/torso.
     *
     * @param boneName the name of the bone, or torsoName (not null)
     * @param mass the desired mass (&gt;0)
     */
    public void setMass(String boneName, float mass) {
        Validate.positive(mass, "mass");

        if (torsoName.equals(boneName)) {
            torsoMass = mass;
        } else if (hasBoneLink(boneName)) {
            massMap.put(boneName, mass);
        } else {
            String msg = "No bone/torso named " + MyString.quote(boneName);
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
        for (float mass : attachMassMap.values()) {
            totalMass += mass;
        }

        assert totalMass > 0f : totalMass;
        return totalMass;
    }

    /**
     * Unlink the AttachmentLink of the named bone.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the associated bone (not null, not empty)
     */
    public void unlinkAttachment(String boneName) {
        if (!hasAttachmentLink(boneName)) {
            String msg = "No attachment bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        if (getSpatial() != null) {
            throw new IllegalStateException(
                    "Cannot unlink an attachment while added to a spatial.");
        }

        attachMassMap.remove(boneName);
        attachModelMap.remove(boneName);
    }

    /**
     * Unlink the BoneLink of the named bone.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the bone to unlink (not null, not empty)
     */
    public void unlinkBone(String boneName) {
        if (!hasBoneLink(boneName)) {
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

        attachMassMap = cloner.clone(attachMassMap);
        massMap = cloner.clone(massMap);
        jointMap = cloner.clone(jointMap);
        attachModelMap = cloner.clone(attachModelMap);
        gravityVector = cloner.clone(gravityVector);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public ConfigDynamicAnimControl jmeClone() {
        try {
            ConfigDynamicAnimControl clone
                    = (ConfigDynamicAnimControl) super.clone();
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

        jointMap.clear();
        massMap.clear();
        String[] linkedBoneNames = ic.readStringArray("linkedBoneNames", null);
        Savable[] linkedBoneJoints
                = ic.readSavableArray("linkedBoneJoints", null);
        float[] linkedBoneMasses = ic.readFloatArray("linkedBoneMasses", null);
        for (int i = 0; i < linkedBoneNames.length; i++) {
            String boneName = linkedBoneNames[i];
            RangeOfMotion rom = (RangeOfMotion) linkedBoneJoints[i];
            jointMap.put(boneName, rom);
            massMap.put(boneName, linkedBoneMasses[i]);
        }

        attachModelMap.clear();
        attachMassMap.clear();
        String[] attachBoneNames = ic.readStringArray("attachBoneNames", null);
        Savable[] attachModels
                = ic.readSavableArray("attachModels", null);
        float[] attachMasses = ic.readFloatArray("attachMasses", null);
        for (int i = 0; i < attachBoneNames.length; i++) {
            String boneName = attachBoneNames[i];
            Spatial model = (Spatial) attachModels[i];
            attachModelMap.put(boneName, model);
            attachMassMap.put(boneName, attachMasses[i]);
        }

        torsoMass = ic.readFloat("rootMass", 1f);
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
        RangeOfMotion[] roms = new RangeOfMotion[count];
        float[] linkedBoneMasses = new float[count];
        int i = 0;
        for (Map.Entry<String, Float> entry : massMap.entrySet()) {
            linkedBoneNames[i] = entry.getKey();
            roms[i] = jointMap.get(entry.getKey());
            linkedBoneMasses[i] = entry.getValue();
            ++i;
        }
        oc.write(linkedBoneNames, "linkedBoneNames", null);
        oc.write(roms, "linkedBoneJoints", null);
        oc.write(linkedBoneMasses, "linkedBoneMasses", null);

        count = countAttachments();
        String[] attachBoneNames = new String[count];
        Spatial[] attachModels = new Spatial[count];
        float[] attachMasses = new float[count];
        i = 0;
        for (Map.Entry<String, Float> entry : attachMassMap.entrySet()) {
            attachBoneNames[i] = entry.getKey();
            attachModels[i] = attachModelMap.get(entry.getKey());
            attachMasses[i] = entry.getValue();
            ++i;
        }
        oc.write(attachBoneNames, "attachBoneNames", null);
        oc.write(attachModels, "attachModels", null);
        oc.write(attachMasses, "attachMasses", null);

        oc.write(torsoMass, "rootMass", 1f);
        oc.write(gravityVector, "gravity", new Vector3f(0f, -9.8f, 0f));
    }
    // *************************************************************************
    // protected methods

    /**
     * Add unlinked descendants of the specified bone to the specified
     * collection. Note: recursive.
     *
     * @param startBone the starting bone (not null, unaffected)
     * @param addResult the collection of bone names to append to (not null,
     * modified)
     */
    protected void addUnlinkedDescendants(Bone startBone,
            Collection<Bone> addResult) {
        for (Bone childBone : startBone.getChildren()) {
            String childName = childBone.getName();
            if (!hasBoneLink(childName)) {
                addResult.add(childBone);
                addUnlinkedDescendants(childBone, addResult);
            }
        }
    }

    /**
     * Find the manager of the specified bone.
     *
     * @param bone the bone (not null, unaffected)
     * @return a bone/torso name (not null)
     */
    protected String findManager(Bone bone) {
        Validate.nonNull(bone, "bone");

        String managerName;
        while (true) {
            String boneName = bone.getName();
            if (hasBoneLink(boneName)) {
                managerName = boneName;
                break;
            }
            bone = bone.getParent();
            if (bone == null) {
                managerName = torsoName;
                break;
            }
        }

        assert managerName != null;
        return managerName;
    }

    /**
     * Create a map from bone indices to the names of the bones that manage
     * them.
     *
     * @param skeleton (not null, unaffected)
     * @return a new array of bone/torso names (not null)
     */
    protected String[] managerMap(Skeleton skeleton) {
        int numBones = skeleton.getBoneCount();
        String[] managerMap = new String[numBones];
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            managerMap[boneIndex] = findManager(bone);
        }

        return managerMap;
    }
}
