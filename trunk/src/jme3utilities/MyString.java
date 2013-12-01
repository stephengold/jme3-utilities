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

import java.util.logging.Logger;

/**
 * Utility methods for Strings. Aside from test cases, all methods should be
 * public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MyString {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MyString.class.getName());
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
     * @param a the string which should precede (not null)
     * @param b the string which should follow (not null)
     * @return true if a precedes or equals b, false otherwise
     */
    public static boolean areLexOrdered(String a, String b) {
        assert b != null;

        boolean result = a.compareTo(b) <= 0;
        return result;
    }

    /**
     * Escape all tab, newline, and backslash characters in a string.
     *
     * @param unescaped the input string (not null)
     * @return the escaped output string
     * @see #unEscape(String)
     */
    public static String escape(String unescaped) {
        StringBuilder result = new StringBuilder();
        for (char ch : unescaped.toCharArray()) {
            if (ch == '\n') {
                result.append("\\n");
            } else if (ch == '\t') {
                result.append("\\t");
            } else if (ch == '\\') {
                result.append("\\\\");
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * Parse a line of two fields from a string.
     *
     * @param input the input string
     * @return either an array of three Strings, each containing a substring of
     * the input, or else null.
     *
     * result[0] is the portion before the first '\t'.
     *
     * result[1] is the portion after the '\t' but before the first '\n'.
     *
     * result[2] is the remainder of the string
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
     * Enclose text in quotation marks.
     *
     * @param text the text to enclose (not null)
     */
    public static String quote(String text) {
        assert text != null;

        return String.format("\"%s\"", text);
    }

    /**
     * Trim any trailing zeroes and one trailing decimal point from a string
     * representation of a float. Also remove sign from zero.
     *
     * @param input the input string (not null)
     * @return the trimmed string
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
     * @return the unescaped output string
     * @see #escape(String)
     */
    public static String unEscape(String escaped) {
        StringBuilder result = new StringBuilder();
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

    // *************************************************************************
    // test cases
    /**
     * Test cases for this class.
     *
     * @param ignored
     */
    public static void main(String[] ignored) {
        System.out.print("Test results for class MyString:\n");

        String[] stringCases = new String[]{
            "",
            "-0",
            "-900",
            "-0.000",
            "54.32100",
            "0.1234",
            "hello",
            "he\\\\no\tgoodbye\n"
        };

        for (String s : stringCases) {
            System.out.printf("s = \"%s\"%n", s);
            String t = trimFloat(s);
            System.out.printf(" trimFloat(s) = \"%s\"%n", t);
            String e = escape(s);
            System.out.printf(" escape(s) = \"%s\"%n", e);
            String ue = unEscape(e);
            System.out.printf(" unEscape(escape(s)) = \"%s\"%n", ue);
            assert (s.equals(ue));

            String[] ss = getLine(s);
            System.out.printf(" ss0 = \"%s\" ss1 = \"%s\" ss3 = \"%s\"%n",
                    ss[0], ss[1], ss[2]);

            System.out.println();
        }

        System.out.println();
    }
}