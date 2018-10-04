/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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

import com.jme3.bullet.joints.SixDofJoint;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A preset for a KinematicRagdollControl.
 *
 * @author Nehon
 */
public abstract class RagdollPreset implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(RagdollPreset.class.getName());
    // *************************************************************************
    // fields

    /**
     * map bone names to joint presets
     */
    protected Map<String, JointPreset> boneMap = new HashMap<>();
    /**
     * lexicon to map bone names to lexicon entries
     */
    protected Map<String, LexiconEntry> lexicon = new HashMap<>();
    // *************************************************************************
    // new methods exposed

    /**
     * Initialize the map from bone names to joint presets.
     */
    protected abstract void initBoneMap();

    /**
     * Initialize the lexicon.
     */
    protected abstract void initLexicon();

    /**
     * Apply the preset for the named bone to the specified joint.
     *
     * @param boneName name
     * @param joint where to apply the preset (not null, modified)
     */
    public void setupJointForBone(String boneName, SixDofJoint joint) {
        if (boneMap.isEmpty()) {
            initBoneMap();
        }
        if (lexicon.isEmpty()) {
            initLexicon();
        }
        String resultName = "";
        int resultScore = 0;

        for (String key : lexicon.keySet()) {
            int score = lexicon.get(key).getScore(boneName);
            if (score > resultScore) {
                resultScore = score;
                resultName = key;
            }
        }

        JointPreset preset = boneMap.get(resultName);
        if (preset != null && resultScore >= 50) {
            logger.log(Level.FINE,
                    "Found matching joint for bone {0} : {1} with score {2}",
                    new Object[]{boneName, resultName, resultScore});
            preset.setupJoint(joint);
        } else {
            logger.log(Level.FINE, "No joint match found for bone {0}",
                    boneName);
            if (resultScore > 0) {
                logger.log(Level.FINE, "Best match found is {0} with score {1}",
                        new Object[]{resultName, resultScore});
            }
            new JointPreset().setupJoint(joint);
        }
    }

    /**
     * Create a deep copy of this preset.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if a field isn't cloneable
     */
    @Override
    public RagdollPreset clone() throws CloneNotSupportedException {
        RagdollPreset clone = (RagdollPreset) super.clone();
        clone.boneMap = new HashMap<>(boneMap);
        clone.lexicon = new HashMap<>();
        for (String boneName : lexicon.keySet()) {
            LexiconEntry le = lexicon.get(boneName);
            LexiconEntry leClone = new LexiconEntry(le);
            clone.lexicon.put(boneName, leClone);
        }

        return clone;
    }

    /**
     * One entry in a bone lexicon.
     */
    protected class LexiconEntry extends HashMap<String, Integer> {

        /**
         * Null constructor.
         */
        public LexiconEntry() {
            super();
        }

        /**
         * Copy constructor.
         *
         * @param le the entry to copy (not null)
         */
        public LexiconEntry(LexiconEntry le) {
            super(le);
        }

        /**
         * Add a synonym with the specified score.
         *
         * @param word a substring that might occur in a bone name (not null)
         * @param score larger value means more likely to correspond
         */
        public void addSynonym(String word, int score) {
            put(word.toLowerCase(), score);
        }

        /**
         * Calculate a total score for the specified bone name.
         *
         * @param name the name of a bone (not null)
         * @return total score: larger value means more likely to correspond
         */
        public int getScore(String name) {
            int score = 0;
            String searchWord = name.toLowerCase();
            for (String key : this.keySet()) {
                if (searchWord.contains(key)) {
                    score += get(key);
                }
            }

            return score;
        }
    }
}
