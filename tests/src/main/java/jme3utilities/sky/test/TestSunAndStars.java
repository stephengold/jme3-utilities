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
package jme3utilities.sky.test;

import com.jme3.math.FastMath;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.sky.SunAndStars;

/**
 * Test cases for the SunAndStars class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestSunAndStars {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestSunAndStars.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * A console application to test this class.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        logger.setLevel(Level.INFO);
        System.out.print("Test results for class SunAndStars:\n\n");

        SunAndStars test = new SunAndStars();
        System.out.printf("Default value:  %s%n", test.toString());

        test.setSolarLongitude(Calendar.DECEMBER, 31);
        System.out.printf(" on December 31st:  %s%n", test.toString());

        test.setSolarLongitude(Calendar.JANUARY, 1);
        System.out.printf(" on January 1st:  %s%n", test.toString());

        test.setSolarLongitude(Calendar.FEBRUARY, 29);
        System.out.printf(" on February 29th:  %s%n", test.toString());

        test.setSolarLongitude(Calendar.MARCH, 1);
        System.out.printf(" on March 1st:  %s%n", test.toString());

        SunAndStars copy;
        try {
            copy = test.clone();
        } catch (CloneNotSupportedException exception) {
            System.out.printf("Clone not supported!%n");
            return;
        }
        System.out.printf("Clone of last value:  %s%n", copy.toString());

        test.setSolarLongitude(Calendar.MARCH, 20);
        System.out.printf(" on March 20th:  %s%n", test.toString());

        test.setObserverLatitude(FastMath.HALF_PI);
        System.out.printf(" at the North Pole:  %s%n", test.toString());

        test.setObserverLatitude(-FastMath.HALF_PI);
        System.out.printf(" at the South Pole:  %s%n", test.toString());

        test.setObserverLatitude(0f);
        System.out.printf(" at the Equator:  %s%n", test.toString());

        test.setHour(23f + (59f + 59f / 60f) / 60f);
        System.out.printf(" at 23:59:59 LST:  %s%n", test.toString());
    }
}
