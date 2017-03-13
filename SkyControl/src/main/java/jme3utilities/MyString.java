/*
 Copyright (c) 2013-2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Utility methods for Strings.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyString {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MyString.class.getName());
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
     * Test two strings for lexicographic order.
     *
     * @param a string that should precede (not null)
     * @param b string that should follow (not null)
     * @return true if a precedes or equals b, false otherwise
     */
    public static boolean areLexOrdered(String a, String b) {
        Validate.nonNull(b, "string");

        boolean result = a.compareTo(b) <= 0;
        return result;
    }

    /**
     * Escape all tab, newline, and backslash characters in a string.
     *
     * @param unescaped the input string (not null)
     * @return escaped output string
     * @see #unEscape(String)
     */
    public static String escape(String unescaped) {
        StringBuilder result = new StringBuilder(50);
        for (char ch : unescaped.toCharArray()) {
            switch (ch) {
                case '\n':
                    result.append("\\n");
                    break;
                case '\t':
                    result.append("\\t");
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
     * Parse a line of two fields from a string.
     *
     * @param input the input string
     * @return either an array of three Strings, each containing a substring of
     * the input, or else null:<ul>
     * <li>result[0] is the portion before the 1st '\t'.
     * <li>result[1] is the portion after the '\t' but before the 1st '\n'.
     * <li>result[2] is the remainder of the string.
     * </ul>
     */
    public static String[] getLine(String input) {
        String[] results = new String[3];
        results[0] = "";
        results[1] = "";
        results[2] = "";
        boolean foundNewline = false;
        boolean foundTab = false;
        for (char ch : input.toCharArray()) {
            if (foundNewline) {
                results[2] += ch;
            } else if (foundTab) {
                if (ch == '\n') {
                    foundNewline = true;
                } else {
                    results[1] += ch;
                }
            } else if (ch == '\t') {
                foundTab = true;
            } else {
                results[0] += ch;
            }
        }

        return results;
    }

    /**
     * Join an array of strings using spaces, ignoring any nulls.
     *
     * @param array of strings to join (not null)
     * @return joined string
     */
    public static String join(String[] array) {
        Validate.nonNull(array, "array");

        StringBuilder result = new StringBuilder(20);
        for (String element : array) {
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
     * Enclose text in quotation marks.
     *
     * @param text the text to enclose (not null)
     * @return quoted string
     */
    public static String quote(String text) {
        Validate.nonNull(text, "text");

        return String.format("\"%s\"", text);
    }

    /**
     * Convert a collection of strings into an array. This is more convenient
     * than Collection.toArray() because the elements of the resulting array
     * will all be strings.
     *
     * @param collection to convert (not null)
     * @return new array containing the same strings in the same order
     */
    public static String[] toArray(Collection<String> collection) {
        int itemCount = collection.size();
        String[] result = new String[itemCount];

        int nextIndex = 0;
        for (String string : collection) {
            result[nextIndex] = string;
            nextIndex++;
        }

        return result;
    }

    /**
     * Trim any trailing zeroes and one trailing decimal point from a string
     * representation of a float. Also remove sign from zero.
     *
     * @param input the input string (not null)
     * @return trimmed string
     */
    public static String trimFloat(String input) {
        String result;
        if (input.contains(".")) {
            int end = input.length();
            char[] chars = input.toCharArray();
            while (end >= 1 && chars[end - 1] == '0') {
                end--;
            }
            if (end >= 1 && chars[end - 1] == '.') {
                end--;
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
     * Undo character escapes added by escape().
     *
     * @param escaped the input string
     * @return unescaped output string
     * @see #escape(String)
     */
    public static String unEscape(String escaped) {
        StringBuilder result = new StringBuilder(50);
        boolean inEscape = false;
        for (char ch : escaped.toCharArray()) {
            if (inEscape) {
                if (ch == '\\') {
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
