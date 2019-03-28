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
package jme3utilities.math.test;

import java.util.logging.Logger;
import jme3utilities.MyString;
import org.junit.Test;

/**
 * Test the MyString class.
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
    /**
     *
     */
    final private static String[] stringCases = new String[]{
        "\"he\"",
        "",
        "-0",
        "-900",
        "-0.000",
        "54.32100",
        "0.1234",
        "hello",
        "he\\\\no\tgoodbye\n",
        "1.0e10",
        "1.0000e-20"
    };
    // *************************************************************************
    // new methods exposed

    @Test
    public void testMyString() {
        assert MyString.quote(stringCases[0]).equals("\"\\\"he\\\"\"");
        assert MyString.quote(stringCases[1]).equals("\"\"");
        assert MyString.quote(stringCases[2]).equals("\"-0\"");
        assert MyString.quote(stringCases[8]).equals("\"he\\\\\\\\no\\tgoodbye\\n\"");

        assert MyString.trimFloat(stringCases[0]).equals(stringCases[0]);
        assert MyString.trimFloat(stringCases[1]).equals(stringCases[1]);
        assert MyString.trimFloat(stringCases[2]).equals("0");
        assert MyString.trimFloat(stringCases[3]).equals("-900");
        assert MyString.trimFloat(stringCases[4]).equals("0");
        assert MyString.trimFloat(stringCases[5]).equals("54.321");
        assert MyString.trimFloat(stringCases[6]).equals(stringCases[6]);
        assert MyString.trimFloat(stringCases[7]).equals(stringCases[7]);
        assert MyString.trimFloat(stringCases[8]).equals(stringCases[8]);
        assert MyString.trimFloat(stringCases[9]).equals("1e10");
        assert MyString.trimFloat(stringCases[10]).equals("1e-20");

        for (String s : stringCases) {
            String e = MyString.escape(s);
            String ue = MyString.unEscape(e);
            assert (s.equals(ue));
        }

        String[] a1 = {"a", "fistful", "of", "bytes"};
        String j1 = MyString.join(a1);
        assert j1.equals("a fistful of bytes");

        String[] a2 = {};
        String j2 = MyString.join(a2);
        assert j2.equals("");
    }
}
