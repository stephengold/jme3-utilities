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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A namespace used to generate unique text strings for names.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class NameGenerator implements Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * Separator character. Prefix strings must not contain this character.
     */
    final private static char separator = '.';
    /**
     * Starting value for sequence numbers.
     */
    final private static int firstSequenceNumber = 1;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(NameGenerator.class.getName());
    // *************************************************************************
    // fields

    /**
     * Track which sequence numbers remain available for the prefix strings seen
     * so far.
     */
    final private Map<String, Integer> nextSequenceNumbers = new TreeMap<>();
    // *************************************************************************
    // new methods exposed

    /**
     * Given a name, recreate its prefix.
     *
     * @param name (not null)
     * @return prefix (or null if the name was not generated here)
     */
    public static String getPrefix(String name) {
        int separatorIndex = name.indexOf(separator);
        if (separatorIndex < 0) {
            /*
             * The name was not generated here.
             */
            return null;
        }
        String prefix = name.substring(0, separatorIndex);
        return prefix;
    }

    /**
     * Test whether a name could plausibly have been generated from a specified
     * prefix.
     *
     * @param name name to test (not null)
     * @param prefix (not null)
     * @return true if the name is plausible, otherwise false
     */
    public static boolean isFrom(String name, String prefix) {
        int separatorIndex = name.indexOf(separator);
        if (separatorIndex == -1) {
            return false;
        }
        String pre = name.substring(0, separatorIndex);
        if (!pre.equals(prefix)) {
            return false;
        }
        int length = name.length();
        String post = name.substring(separatorIndex + 1, length);
        boolean result = post.matches("^\\d+$");

        return result;
    }

    /**
     * Restart name generation from scratch, forgetting about any previously
     * used sequence numbers.
     */
    public void reset() {
        // Re-initialize unique ID mapping.
        nextSequenceNumbers.clear();
    }

    /**
     * Generate a new unique name consisting of a prefix string followed by the
     * separator followed by a decimal sequence number.
     *
     * @param prefix the prefix string (not null, doesn't contain separator)
     * @return unique String which begins with the specified prefix
     */
    public String unique(String prefix) {
        int separatorIndex = prefix.indexOf(separator);
        if (separatorIndex >= 0) {
            logger.log(Level.SEVERE, "prefix={0}", MyString.quote(prefix));
            String message = String.format(
                    "prefix must not contain the separator '%c'", separator);
            throw new IllegalArgumentException(message);
        }

        Integer sequenceNumber = nextSequenceNumbers.get(prefix);
        if (sequenceNumber == null) {
            sequenceNumber = firstSequenceNumber;
        }
        nextSequenceNumbers.put(prefix, sequenceNumber + 1);

        String sequenceString = String.valueOf(sequenceNumber);
        String name = prefix + separator + sequenceString;

        return name;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this name generator, for example when loading from a J3O
     * file.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
        InputCapsule capsule = importer.getCapsule(this);
        String[] keys = capsule.readStringArray("keys", null);
        int[] values = capsule.readIntArray("values", null);
        int size = keys.length;
        assert values.length == size : size;

        reset();
        for (int i = 0; i < size; ++i) {
            String key = keys[i];
            int value = values[i];
            nextSequenceNumbers.put(key, value);
        }
    }

    /**
     * Serialize this name generator, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        int size = nextSequenceNumbers.size();
        String[] keys = new String[size];
        int[] values = new int[size];
        int i = 0;
        for (Map.Entry<String, Integer> entry
                : nextSequenceNumbers.entrySet()) {
            keys[i] = entry.getKey();
            values[i] = entry.getValue();
            ++i;
        }

        OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(keys, "keys", null);
        capsule.write(values, "values", null);
    }
    // *************************************************************************
    // Object methods

    /**
     * Represent this name generator as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(200);

        builder.append("{ ");
        for (Map.Entry<String, Integer> entry
                : nextSequenceNumbers.entrySet()) {
            String piece = String.format("%s:%s ",
                    entry.getKey(), entry.getValue());
            builder.append(piece);
        }
        builder.append("}");
        String result = builder.toString();

        return result;
    }
}
