/*
 Copyright (c) 2017-2021, Stephen Gold
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
package jme3utilities.evo;

import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * An immutable fitness score value for optimization, composed of (possibly
 * multiple) double-precision sub-scores.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreDoubles implements Comparable<ScoreDoubles> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ScoreDoubles.class.getName());
    // *************************************************************************
    // fields

    /**
     * array of sub-score values (not null, length &gt;0, all elements numbers,
     * initialized by constructors)
     *
     */
    final private double[] subscores;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a score from a single sub-score value.
     *
     * @param subscore sub-score value (number)
     */
    public ScoreDoubles(double subscore) {
        Validate.number(subscore, "sub-score");

        subscores = new double[1];
        subscores[0] = subscore;
    }

    /**
     * Instantiate a score from a pair of sub-score values.
     *
     * @param subscore0 first sub-score value (number)
     * @param subscore1 2nd sub-score value (number)
     */
    public ScoreDoubles(double subscore0, double subscore1) {
        Validate.number(subscore0, "first sub-score");
        Validate.number(subscore1, "2nd sub-score");

        subscores = new double[2];
        subscores[0] = subscore0;
        subscores[1] = subscore1;
    }

    /**
     * Instantiate a score from an array of sub-score values.
     *
     * @param subscores input array (not null, length &gt;0, all elements
     * numbers, unaffected)
     */
    public ScoreDoubles(double[] subscores) {
        Validate.nonNull(subscores, "sub-scores");
        int num = subscores.length;
        Validate.positive(num, "number of sub-scores");

        this.subscores = new double[num];
        for (int index = 0; index < num; index++) {
            double sub = subscores[index];
            Validate.number(sub, "sub-score");
            this.subscores[index] = sub;
        }
        System.arraycopy(subscores, 0, this.subscores, 0, num);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the specified sub-score.
     *
     * @param index which sub-score (&ge;0, &lt;numSubscores)
     * @return sub-score value (number)
     */
    public double getSubscore(int index) {
        Validate.inRange(index, "index", 0, subscores.length - 1);
        double result = subscores[index];
        return result;
    }

    /**
     * Compare two scores and return the higher one. If they are equivalent,
     * return the first score. Null is treated as the lowest possible score.
     *
     * @param score1 first score input (may be null)
     * @param score2 2nd score input (may be null)
     * @return one of the inputs, or null if both inputs are null
     */
    public static ScoreDoubles max(ScoreDoubles score1, ScoreDoubles score2) {
        if (score1 == null) {
            return score2;
        } else if (score2 == null) {
            return score1;
        }

        int num1 = score1.numSubscores();
        int num2 = score2.numSubscores();
        int minNum = Math.min(num1, num2);
        for (int subIndex = 0; subIndex < minNum; subIndex++) {
            double sub1 = score1.getSubscore(subIndex);
            double sub2 = score2.getSubscore(subIndex);
            if (sub1 > sub2) {
                return score1;
            } else if (sub2 > sub1) {
                return score2;
            }
        }

        return score1;
    }

    /**
     * Read the number of sub-scores in this score.
     *
     * @return count (&gt;0)
     */
    public int numSubscores() {
        int result = subscores.length;
        assert result > 0 : result;
        return result;
    }
    // *************************************************************************
    // Comparable methods

    /**
     * Compare with another score. Null is treated as the lowest possible score.
     *
     * @param otherScore (may be null)
     * @return 0 if the scores are equivalent, +1 if the other score is lower,
     * -1 if the other score is higher
     */
    @Override
    public int compareTo(ScoreDoubles otherScore) {
        if (otherScore == null) {
            return +1;
        }

        int ns = subscores.length;
        int nsOther = otherScore.numSubscores();
        int minNum = Math.min(ns, nsOther);
        for (int subIndex = 0; subIndex < minNum; subIndex++) {
            double sub = subscores[subIndex];
            double subOther = otherScore.getSubscore(subIndex);
            if (sub > subOther) {
                return +1;
            } else if (subOther > sub) {
                return -1;
            }
        }
        return 0;
    }
    // *************************************************************************
    // Object methods

    /**
     * Test for equivalence with another object.
     *
     * @param otherObject (unaffected, may be null)
     * @return true if the objects are equivalent, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;

        } else if (otherObject == null) {
            return false;

        } else if (otherObject instanceof ScoreDoubles) {
            ScoreDoubles otherScore = (ScoreDoubles) otherObject;
            int num = subscores.length;
            int numOther = otherScore.numSubscores();
            int minNum = Math.min(num, numOther);
            for (int subIndex = 0; subIndex < minNum; subIndex++) {
                double sub = subscores[subIndex];
                double subOther = otherScore.getSubscore(subIndex);
                if (sub != subOther) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Generate the hash code for this score.
     *
     * @return value for use in hashing
     */
    @Override
    public int hashCode() {
        long longCode = 13;
        for (double sub : subscores) {
            long bits = Double.doubleToLongBits(sub);
            longCode = 17 * longCode + bits;
        }
        int result = (int) (longCode ^ (longCode >> 32));

        return result;
    }

    /**
     * Represent this score as a string of text.
     *
     * @return descriptive string of text (not null, not empty)
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(64);
        buffer.append("[");
        for (int subIndex = 0; subIndex < subscores.length; subIndex++) {
            if (subIndex != 0) {
                buffer.append(", ");
            }
            double sub = subscores[subIndex];
            buffer.append(sub);
        }
        buffer.append("]");
        String result = buffer.toString();

        return result;
    }
}
