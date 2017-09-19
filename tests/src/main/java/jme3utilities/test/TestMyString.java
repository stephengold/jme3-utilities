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
package jme3utilities.test;

import java.util.logging.Logger;
import jme3utilities.MyString;

/**
 * Test cases for the MyString class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestMyString {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestMyString.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Console application to test the MyString class.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        System.out.print("Test results for class MyString:\n\n");

        String[] stringCases = new String[]{
            "\"he\"",
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
            System.out.printf("s = {%s}%n", s);

            String q = MyString.quote(s);
            System.out.printf(" quote(s) = {%s}%n", q);

            String t = MyString.trimFloat(s);
            System.out.printf(" trimFloat(s) = {%s}%n", t);

            String e = MyString.escape(s);
            System.out.printf(" escape(s) = {%s}%n", e);

            String ue = MyString.unEscape(e);
            System.out.printf(" unEscape(escape(s)) = {%s}%n", ue);
            assert (s.equals(ue));

            System.out.println();
        }

        String[] a1 = {"a", "fistful", "of", "bytes"};
        String j1 = MyString.join(a1);
        System.out.printf(" j1={%s}%n", j1);

        String[] a2 = {};
        String j2 = MyString.join(a2);
        System.out.printf(" j2={%s}%n", j2);

        System.out.println();
    }
}
