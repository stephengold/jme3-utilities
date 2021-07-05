/*
 Copyright (c) 2020-2021, Stephen Gold
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
package jme3utilities.ui;

import com.jme3.input.InputManager;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Represent a Hotkey combined with positive and/or negative signals. Immutable.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Combo {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Combo.class.getName());
    // *************************************************************************
    // fields

    /**
     * for each signal tested: true&rarr;required, false&rarr;prohibited
     */
    final private boolean[] positiveFlags;
    /**
     * hotkey that triggers this Combo
     */
    final private Hotkey hotkey;
    /**
     * names all signals tested (in lexicographic order, no duplicates)
     */
    final private String[] signalNames;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a Combo with the specified Hotkey and a single signal.
     *
     * @param hotkey the Hotkey that will trigger the Combo (not null)
     * @param signalName the name of the signal to be tested (not null, not
     * empty)
     * @param positiveFlag true&rarr;signal required, false&rarr;signal
     * prohibited
     */
    public Combo(Hotkey hotkey, String signalName, boolean positiveFlag) {
        Validate.nonNull(hotkey, "trigger");
        Validate.nonEmpty(signalName, "signal name");

        this.hotkey = hotkey;

        signalNames = new String[1];
        signalNames[0] = signalName;

        positiveFlags = new boolean[1];
        positiveFlags[0] = positiveFlag;
    }

    /**
     * Instantiate a Combo with the specified Hotkey, signal names, and positive
     * flags.
     *
     * @param hotkey the Hotkey that will trigger this Combo (not null)
     * @param names the names of all signals to be tested (not null, not empty,
     * no duplicates)
     * @param flags for each signal tested: true&rarr;required,
     * false&rarr;prohibited (not null)
     */
    public Combo(Hotkey hotkey, String[] names, boolean[] flags) {
        Validate.nonNull(hotkey, "hotkey");

        int numSignals = names.length;
        int flagsLength = flags.length;
        Validate.require(numSignals == flagsLength, "equal-length arrays");

        Set<String> set = new TreeSet<>();
        for (String signalName : names) {
            set.add(signalName);
        }
        Validate.require(numSignals == set.size(), "distinct signal names");

        this.hotkey = hotkey;

        signalNames = new String[numSignals];
        set.toArray(signalNames);

        positiveFlags = new boolean[numSignals];
        for (int sortedI = 0; sortedI < numSignals; ++sortedI) {
            String name = signalNames[sortedI];
            int originalI = findIndex(names, name);
            assert originalI >= 0 : originalI;
            assert names[originalI].equals(name) : name;

            positiveFlags[sortedI] = flags[originalI];
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Count the signals.
     *
     * @return the number of signals (&ge;0)
     */
    int countSignals() {
        int result = signalNames.length;
        return result;
    }

    /**
     * Determine the polarity of the indexed signal.
     *
     * @param signalIndex which signal (&ge;0, &lt;numSignals)
     * @return true if the signal is required, false if it is excluded
     */
    boolean isPositive(int signalIndex) {
        Validate.inRange(signalIndex, "signal index", 0,
                positiveFlags.length - 1);
        boolean result = positiveFlags[signalIndex];

        return result;
    }

    /**
     * Map this Combo in an InputManager.
     *
     * @param inputManager the application's InputManager (not null)
     */
    void map(InputManager inputManager) {
        assert inputManager != null;

        String actionString = String.format("combo %d", hotkey.code());
        hotkey.map(actionString, inputManager);
    }

    /**
     * Determine the name of the indexed signal.
     *
     * @param signalIndex which signal (&ge;0, &lt;numSignals)
     * @return the name
     */
    String signalName(int signalIndex) {
        Validate.inRange(signalIndex, "signal index", 0,
                signalNames.length - 1);
        String result = signalNames[signalIndex];
        return result;
    }

    /**
     * Test all signals relevant to this Combo.
     *
     * @param signalTracker the signal tracker to test against (not null)
     * @return true if all required signals are active and no prohibited signals
     * are active
     */
    boolean testAll(Signals signalTracker) {
        assert signalTracker != null;

        boolean result = true;
        int numSignals = signalNames.length;
        for (int signalIndex = 0; signalIndex < numSignals; ++signalIndex) {
            String name = signalNames[signalIndex];
            boolean value = signalTracker.test(name);

            boolean positiveFlag = positiveFlags[signalIndex];
            if (value != positiveFlag) {
                result = false;
                break;
            }
        }

        return result;
    }

    /**
     * Determine the universal code of the Hotkey that triggers this Combo.
     *
     * @return a universal code (&ge;0)
     */
    int triggerCode() {
        int code = hotkey.code();
        return code;
    }

    /**
     * Unmap this Combo in an InputManager.
     *
     * @param inputManager the application's InputManager (not null)
     */
    void unmap(InputManager inputManager) {
        assert inputManager != null;

        String actionString = String.format("combo %d", hotkey.code());
        hotkey.unmap(actionString, inputManager);
    }
    // *************************************************************************
    // Object methods

    /**
     * Test for exact equivalence with another Object.
     *
     * @param otherObject the object to compare to (may be null, unaffected)
     * @return true if the objects are equivalent, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        boolean result;
        if (otherObject == this) {
            result = true;

        } else if (otherObject != null
                && otherObject.getClass() == getClass()) {
            Combo other = (Combo) otherObject;
            result = hotkey.equals(other.hotkey)
                    && Arrays.deepEquals(signalNames, other.signalNames)
                    && Arrays.equals(positiveFlags, other.positiveFlags);

        } else {
            result = false;
        }

        return result;
    }

    /**
     * Generate the hash code for this instance.
     *
     * @return the value to use for hashing
     */
    @Override
    public int hashCode() {
        int hash = 31;
        hash = 79 * hash + Objects.hashCode(hotkey);
        hash = 79 * hash + Arrays.deepHashCode(signalNames);
        hash = 79 * hash + Arrays.hashCode(positiveFlags);

        return hash;
    }

    /**
     * Represent this instance as a String.
     *
     * @return a descriptive string of text (not null, not empty)
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(40);

        int numSignals = signalNames.length;
        for (int signalIndex = 0; signalIndex < numSignals; ++signalIndex) {
            String name = signalNames[signalIndex];
            result.append(name);

            boolean positiveFlag = positiveFlags[signalIndex];
            if (positiveFlag) {
                result.append('+');
            } else {
                result.append('-');
            }
        }
        result.append(hotkey.name());

        return result.toString();
    }
    // *************************************************************************
    // private methods

    /**
     * Find the specified String value in an array, using a linear search. The
     * array need not be sorted. TODO use Heart library
     *
     * @param array the array to search (not null, unaffected)
     * @param value the value to find (not null)
     * @return the index of the first match (&ge;0) or -1 if not found
     */
    private static int findIndex(String[] array, String value) {
        assert array != null;
        assert value != null;

        for (int i = 0; i < array.length; ++i) {
            if (value.equals(array[i])) {
                return i;
            }
        }

        return -1;
    }
}
