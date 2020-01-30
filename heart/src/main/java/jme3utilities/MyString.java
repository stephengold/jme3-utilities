/*
 Copyright (c) 2013-2020, Stephen Gold
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.math.MyVector3f;

/**
 * Utility methods for char sequences, strings, and collections of strings.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyString {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyString.class.getName());
    /**
     * pattern for matching a scientific-notation exponent
     */
    final private static Pattern sciPattern = Pattern.compile("[Ee][+-]?\\d+$");
    /**
     * names of the coordinate axes
     */
    final private static String[] axisNames = {"X", "Y", "Z"};
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyString() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find all strings in the input collection that begin with the specified
     * prefix and add them to the result.
     *
     * @param collection input collection (not null, unaffected)
     * @param prefix (not null)
     * @param addResult (added to if not null)
     * @return an expanded list (either addResult or a new instance)
     */
    public static List<String> addMatchPrefix(Collection<String> collection,
            String prefix, List<String> addResult) {
        Validate.nonNull(collection, "input collection");
        Validate.nonNull(prefix, "prefix");
        if (addResult == null) {
            int size = collection.size();
            addResult = new ArrayList<>(size);
        }

        for (String string : collection) {
            if (string.startsWith(prefix)) {
                addResult.add(string);
            }
        }

        return addResult;
    }

    /**
     * Determine the index of the named coordinate axis.
     *
     * @param axisName the name of the axis (not null, not empty)
     * @return the index of the axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     * @see #axisName(int)
     */
    public static int axisIndex(String axisName) {
        for (int axisIndex = 0; axisIndex < axisNames.length; ++axisIndex) {
            if (axisNames[axisIndex].equals(axisName)) {
                return axisIndex;
            }
        }
        String quoted = MyString.quote(axisName);
        throw new IllegalArgumentException(quoted);
    }

    /**
     * Describe a coordinate axis.
     *
     * @param axisIndex the index of the axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     * @return a textual description (not null, not empty)
     * @see #axisIndex(String)
     */
    public static String axisName(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", MyVector3f.xAxis,
                MyVector3f.zAxis);

        String axisName = axisNames[axisIndex];
        return axisName;
    }

    /**
     * De-duplicate a list of strings by appending distinguishing suffixes as
     * needed. The number of strings and their order remains unchanged.
     *
     * @param list input (not null, modified)
     * @param separator text to separate original name from suffix (not null)
     */
    public static void dedup(List<String> list, String separator) {
        Validate.nonNull(list, "list");

        for (String string : list) {
            int numInstances = 0;
            for (String s : list) {
                if (s.equals(string)) {
                    ++numInstances;
                }
            }
            if (numInstances > 1) {
                /*
                 * Append a disinguishing suffix to each duplicate.
                 */
                int numElements = list.size();
                int nextSuffix = 1;
                for (int index = 0; index < numElements; ++index) {
                    String originalName = list.get(index);
                    if (originalName.equals(string)) {
                        String withSuffix = String.format("%s%s%d",
                                originalName, separator, nextSuffix);
                        ++nextSuffix;
                        list.set(index, withSuffix);
                    }
                }
            }
        }
    }

    /**
     * Generate a textual description of a single-precision floating-point
     * value.
     *
     * @param fValue the value to describe
     * @return a description (not null, not empty)
     */
    public static String describe(float fValue) {
        String raw = String.format("%g", fValue);
        String result = MyString.trimFloat(raw);

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Generate a textual description of a single-precision floating-point value
     * using at most 3 decimal places.
     *
     * @param fValue the value to describe
     * @return a description (not null, not empty)
     */
    public static String describeFraction(float fValue) {
        String raw = String.format("%.3f", fValue);
        String result = MyString.trimFloat(raw);

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Escape all tab, quote, newline, and backslash characters in a
     * CharSequence.
     *
     * @param unescaped the input sequence (not null)
     * @return an escaped String (not null)
     * @see #unEscape(CharSequence)
     */
    public static String escape(CharSequence unescaped) {
        int length = unescaped.length();
        StringBuilder result = new StringBuilder(length + 10);
        for (int i = 0; i < length; ++i) {
            char ch = unescaped.charAt(i);
            switch (ch) {
                case '\n':
                    result.append("\\n");
                    break;

                case '\t':
                    result.append("\\t");
                    break;

                case '"':
                    result.append("\\\"");
                    break;

                case '\\':
                    result.append("\\\\");
                    break;

                default:
                    result.append(ch);
                    break;
            }
        }

        return result.toString();
    }

    /**
     * Find the longest repeated prefix in a collection of strings.
     *
     * @param collection (not null, unaffected)
     * @return prefix (not null)
     */
    public static String findLongestPrefix(Collection<String> collection) {
        CharSequence longest = "";
        int longestLength = 0;

        String[] array = toArray(collection);
        int count = array.length;
        for (int i = 0; i < count; ++i) {
            String si = array[i];
            for (int j = i + 1; j < count; ++j) {
                String sj = array[j];
                int prefixLength = sharedPrefixLength(si, sj);
                if (prefixLength > longestLength) {
                    longestLength = prefixLength;
                    longest = si.subSequence(0, prefixLength);
                }
            }
        }

        return longest.toString();
    }

    /**
     * Convert the first character of the specified String to lower case.
     *
     * @param input the input string (not null)
     * @return the converted String (not null)
     */
    public static String firstToLower(String input) {
        String result = input;
        if (!input.isEmpty()) {
            String first = input.substring(0, 1);
            first = first.toLowerCase(Locale.ROOT);
            String rest = input.substring(1);
            result = first + rest;
        }

        return result;
    }

    /**
     * Invert the specified String-to-String map.
     *
     * @param input (not null, unaffected)
     * @return a new String-to-String map
     */
    public static Map<String, String> invert(Map<String, String> input) {
        Map<String, String> result = new TreeMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String key = entry.getKey();
            if (result.containsKey(key)) {
                throw new IllegalArgumentException("Non-invertible map.");
            }
            String value = entry.getValue();
            result.put(value, key);
        }

        return result;
    }

    /**
     * Join a collection of objects into a text string using spaces for
     * separators and ignoring any empties/nulls. Note that Java 8 provides
     * {@link java.lang.String#join(java.lang.CharSequence, java.lang.Iterable)}.
     *
     * @param objects objects to join (not null, unaffected, may contain nulls)
     * @return joined string (not null)
     */
    public static String join(Iterable objects) {
        Validate.nonNull(objects, "objects");
        String result = join(" ", objects);
        return result;
    }

    /**
     * Test whether the specified List is lexicographically sorted in ascending
     * order with no duplicates.
     *
     * @param list the List to analyze (not null, unaffected)
     * @return true if sorted, otherwise false
     */
    public static boolean isSorted(List<String> list) {
        int len = list.size();
        for (int i = 0; i < len - 1; ++i) {
            if (list.get(i).compareTo(list.get(i + 1)) >= 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Join an array of objects into a text string using spaces for separators
     * and ignoring any empties/nulls.
     *
     * @param array objects to join (not null, unaffected, may contain nulls)
     * @return joined string (not null)
     */
    public static String join(Object[] array) {
        Validate.nonNull(array, "array");

        StringBuilder result = new StringBuilder(80);
        for (Object element : array) {
            if (element != null) {
                if (result.length() > 0) {
                    /*
                     * Append a space as a separator.
                     */
                    result.append(' ');
                }
                result.append(element);
            }
        }

        return result.toString();
    }

    /**
     * Join a collection of objects into a text string using the specified
     * separator, ignoring any empties/nulls. Note that Java 8 provides
     * {@link java.lang.String#join(java.lang.CharSequence, java.lang.Iterable)}.
     *
     * @param objects objects to join (not null, unaffected, may contain nulls)
     * @param separator text string (not null)
     * @return joined string (not null)
     */
    public static String join(CharSequence separator, Iterable objects) {
        Validate.nonNull(separator, "separator");
        Validate.nonNull(objects, "list");

        StringBuilder result = new StringBuilder(80);
        for (Object item : objects) {
            if (item != null) {
                if (result.length() > 0) {
                    /*
                     * Append a separator.
                     */
                    result.append(separator);
                }
                result.append(item);
            }
        }

        return result.toString();
    }

    /**
     * Filter a collection of strings, keeping only those that begin with the
     * specified prefix.
     *
     * @param collection collection to filter (not null, modified)
     * @param prefix (not null)
     */
    public static void matchPrefix(Collection<String> collection,
            String prefix) {
        Validate.nonNull(collection, "collection");
        Validate.nonNull(prefix, "prefix");

        for (String element : toArray(collection)) {
            if (!element.startsWith(prefix)) {
                collection.remove(element);
            }
        }
    }

    /**
     * Enclose the specified text in quotation marks and escape all tab, quote,
     * newline, and backslash characters.
     *
     * @param text the input text to quote
     * @return a quoted string, or "null" if the input was null
     */
    public static String quote(CharSequence text) {
        String result;
        if (text == null) {
            result = "null";
        } else {
            result = "\"" + escape(text) + "\"";
        }

        return result;
    }

    /**
     * Enclose the specified name in quotation marks and escape all tab, quote,
     * newline, and backslash characters.
     *
     * @param name the name to quote
     * @return a quoted string, or "(no name)" if the name was null
     */
    public static String quoteName(CharSequence name) {
        String result;
        if (name == null) {
            result = "(no name)";
        } else {
            result = "\"" + escape(name) + "\"";
        }

        return result;
    }

    /**
     * Reduce a collection of strings using common prefixes.
     *
     * @param collection (not null, modified)
     * @param sizeGoal (&gt;0)
     */
    public static void reduce(Collection<String> collection, int sizeGoal) {
        Validate.positive(sizeGoal, "size goal");

        while (collection.size() > sizeGoal) {
            String longestPrefix = findLongestPrefix(collection);
            if (longestPrefix.length() == 0) {
                return;
            }
            for (String string : toArray(collection)) {
                if (string.startsWith(longestPrefix)) {
                    collection.remove(string);
                }
            }
            collection.add(longestPrefix);
        }
    }

    /**
     * Extract the remainder of the specified string after removing the
     * specified prefix.
     *
     * @param input the input string (not null)
     * @param prefix the prefix string (not null)
     * @return the remainder of the input (not null)
     */
    public static String remainder(String input, String prefix) {
        Validate.nonNull(prefix, "prefix");
        if (!input.startsWith(prefix)) {
            logger.log(Level.SEVERE, "input={0}, prefix={1}", new Object[]{
                MyString.quote(input), MyString.quote(prefix)
            });
            throw new IllegalArgumentException("input must start with prefix.");
        }

        int endPosition = prefix.length();
        String result = input.substring(endPosition);

        assert result != null;
        return result;
    }

    /**
     * Extract the remainder of the specified string after removing the
     * specified suffix.
     *
     * @param input input string (not null)
     * @param suffix prefix string (not null)
     * @return the remainder of the input (not null)
     */
    public static String removeSuffix(String input, String suffix) {
        Validate.nonNull(suffix, "suffix");
        if (!input.endsWith(suffix)) {
            logger.log(Level.SEVERE, "input={0}, suffix={1}", new Object[]{
                MyString.quote(input), MyString.quote(suffix)
            });
            throw new IllegalArgumentException("input must end with suffix.");
        }

        int endPosition = input.length() - suffix.length();
        String result = input.substring(0, endPosition);

        assert result != null;
        return result;
    }

    /**
     * Generate a String consisting of a specified character sequence repeated a
     * specified number of times.
     *
     * @param sequence the sequence to use (not null)
     * @param numTimes the number of times (&ge;0)
     * @return a repetitious String
     */
    public static String repeat(CharSequence sequence, int numTimes) {
        Validate.nonNull(sequence, "sequence");
        Validate.nonNegative(numTimes, "number of times");

        int length = numTimes * sequence.length();
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < numTimes; ++index) {
            builder.append(sequence);
        }

        return builder.toString();
    }

    /**
     * Find the length of the shared prefix of 2 text sequences.
     *
     * @param s1 the first string (not null)
     * @param s2 the 2nd string (not null)
     * @return number of characters in shared prefix (&ge;0)
     */
    public static int sharedPrefixLength(CharSequence s1, CharSequence s2) {
        int length1 = s1.length();
        int length2 = s2.length();
        int maxPrefixLength = Math.min(length1, length2);

        int spLength;
        for (spLength = 0; spLength < maxPrefixLength; ++spLength) {
            char c1 = s1.charAt(spLength);
            char c2 = s2.charAt(spLength);
            if (c1 != c2) {
                break;
            }
        }

        return spLength;
    }

    /**
     * Convert a collection of strings into an array. This is more convenient
     * than Collection.toArray() because the elements of the resulting array
     * will all be strings.
     *
     * @param collection the collection to convert (not null)
     * @return a new array containing the same strings in the same order
     */
    public static String[] toArray(Collection<String> collection) {
        int count = collection.size();
        String[] array = new String[count];
        collection.toArray(array);

        return array;
    }

    /**
     * Trim any trailing zeros and one trailing decimal point from a string
     * representation of a float. Also remove sign from zero. TODO localize
     *
     * @param input the String to trim (not null)
     * @return a trimmed String (not null)
     */
    public static String trimFloat(String input) {
        String result;
        Matcher matcher = sciPattern.matcher(input);
        if (matcher.find()) {
            int suffixPos = matcher.start();
            String suffix = input.substring(suffixPos);
            String number = input.substring(0, suffixPos);
            result = trimFloat(number) + suffix;

        } else if (input.contains(".")) {
            int end = input.length();
            char[] chars = input.toCharArray();
            while (end >= 1 && chars[end - 1] == '0') {
                --end;
            }
            if (end >= 1 && chars[end - 1] == '.') {
                --end;
            }
            result = input.substring(0, end);

        } else {
            result = input;
        }

        if ("-0".equals(result)) {
            result = "0";
        }

        return result;
    }

    /**
     * Undo character escape sequences added by {@link #escape(CharSequence)}.
     *
     * @param escaped the input sequence (not null)
     * @return an unescaped string (not null)
     */
    public static String unEscape(CharSequence escaped) {
        int length = escaped.length();
        StringBuilder result = new StringBuilder(length);
        boolean inEscape = false;
        for (int i = 0; i < length; ++i) {
            char ch = escaped.charAt(i);
            if (inEscape) {
                if (ch == '\\' || ch == '"') {
                    result.append(ch);
                } else if (ch == 'n') {
                    result.append('\n');
                } else {
                    assert ch == 't';
                    result.append('\t');
                }
                inEscape = false;
            } else if (ch == '\\') {
                inEscape = true;
            } else {
                assert ch != '\t';
                assert ch != '\n';
                result.append(ch);
            }
        }
        assert !inEscape;
        return result.toString();
    }
}
