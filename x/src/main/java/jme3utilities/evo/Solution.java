/*
 Copyright (c) 2017, Stephen Gold
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

import java.util.Random;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * An immutable candidate solution for evolutionary optimization, composed of a
 * species name and an array (possibly empty) of single-precision parameters.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Solution {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Solution.class.getName());
    // *************************************************************************
    // fields
    
    /**
     * array of parameter values (not null, initialized by constructor)
     */
    final private float[] parameters;
    /**
     * name of species (not null, initialized by constructor)
     */
    final private String species;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new solution.
     *
     * @param species name of species (not null)
     * @param parameters array of parameter values (not null, unaffected)
     */
    public Solution(String species, float[] parameters) {
        Validate.nonNull(species, "species");
        Validate.nonNull(parameters, "parameters");

        this.species = species;
        int numParameters = parameters.length;
        this.parameters = new float[numParameters];
        System.arraycopy(parameters, 0, this.parameters, 0, numParameters);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Linearly blend this solution with another to produce a new solution.
     *
     * @param otherSolution (not null, same species and number of parameters)
     * @param blendAmount fractional weight given to other solution (&ge;0,
     * &le;1)
     * @return a new instance
     */
    public Solution blendLinear(Solution otherSolution, double blendAmount) {
        Validate.nonNull(otherSolution, "other solution");
        Validate.fraction(blendAmount, "blend amount");
        String otherSpecies = otherSolution.getSpecies();
        if (!species.equals(otherSpecies)) {
            throw new IllegalArgumentException("species mismatch");
        }
        int otherNum = otherSolution.numParameters();
        if (parameters.length != otherNum) {
            throw new IllegalArgumentException("parameter count mismatch");
        }

        double complement = 1.0 - blendAmount;
        float[] newParameters = new float[otherNum];
        for (int iParameter = 0; iParameter < otherNum; iParameter++) {
            float parm = parameters[iParameter];
            float otherParm = otherSolution.getParameter(iParameter);
            double newParm = parm * complement + otherParm * blendAmount;
            newParameters[iParameter] = (float) newParm;
        }

        Solution result = new Solution(otherSpecies, newParameters);
        return result;
    }

    /**
     * Linearly blend this solution with two others to produce a new solution.
     *
     * @param solution1 (not null, same species and number of parameters)
     * @param blend1 fractional weight for solution1 (&ge;0, &le;1)
     * @param solution2 (not null, same species and number of parameters)
     * @param blend2 fractional weight for solution2 (&ge;0, &le;1)
     * @return a new instance
     */
    public Solution blendLinear(Solution solution1, double blend1,
            Solution solution2, double blend2) {
        Validate.nonNull(solution1, "solution1");
        Validate.nonNull(solution2, "solution2");
        Validate.fraction(blend1, "1st blend amount");
        Validate.fraction(blend2, "2nd blend amount");
        Validate.fraction(blend1 + blend2, "sum of blend amounts");
        String species1 = solution1.getSpecies();
        if (!species.equals(species1)) {
            throw new IllegalArgumentException("species mismatch");
        }
        String species2 = solution2.getSpecies();
        if (!species.equals(species2)) {
            throw new IllegalArgumentException("species mismatch");
        }
        int num = parameters.length;
        int num1 = solution1.numParameters();
        if (num != num1) {
            throw new IllegalArgumentException("parameter count mismatch");
        }
        int num2 = solution2.numParameters();
        if (num != num2) {
            throw new IllegalArgumentException("parameter count mismatch");
        }

        double complement = 1.0 - blend1 - blend2;
        float[] newParameters = new float[num1];
        for (int iParameter = 0; iParameter < num1; iParameter++) {
            float parm = parameters[iParameter];
            float parm1 = solution1.getParameter(iParameter);
            float parm2 = solution2.getParameter(iParameter);
            double newParm = parm * complement
                    + parm1 * blend1 + parm2 * blend2;
            newParameters[iParameter] = (float) newParm;
        }

        Solution result = new Solution(species1, newParameters);
        return result;
    }

    /**
     * Probabilistically combine this solution (parameter by parameter) with
     * another to produce a new solution.
     *
     * @param otherSolution (not null, same species and number of parameters)
     * @param blendProbability probability of copying from the other solution
     * (&ge;0, &le;1)
     * @param generator pseudo-random generator to use (not null)
     * @return a new instance
     */
    public Solution blendRandom(Solution otherSolution,
            double blendProbability, Random generator) {
        Validate.nonNull(otherSolution, "other solution");
        String otherSpecies = otherSolution.getSpecies();
        if (!species.equals(otherSpecies)) {
            throw new IllegalArgumentException("species mismatch");
        }
        int otherNum = otherSolution.numParameters();
        if (parameters.length != otherNum) {
            throw new IllegalArgumentException("parameter count mismatch");
        }
        Validate.fraction(blendProbability, "blend probability");
        Validate.nonNull(generator, "generator");

        float[] newParameters = new float[otherNum];
        for (int iParameter = 0; iParameter < otherNum; iParameter++) {
            float newParm;
            double random = generator.nextDouble();
            if (random < blendProbability) {
                newParm = otherSolution.getParameter(iParameter);
            } else {
                newParm = parameters[iParameter];
            }
            newParameters[iParameter] = newParm;
        }

        Solution result = new Solution(otherSpecies, newParameters);
        return result;
    }

    /**
     * Copy this solution's parameters.
     *
     * @return a new array
     */
    public float[] copyParameters() {
        int numParameters = parameters.length;
        float[] result = new float[numParameters];
        System.arraycopy(parameters, 0, result, 0, numParameters);

        return result;
    }

    /**
     * Read the specified parameter.
     *
     * @param index which parameter (&ge;0, &lt;numParameters)
     *
     * @return parameter value
     */
    public float getParameter(int index) {
        Validate.inRange(index, "index", 0, parameters.length - 1);
        float result = parameters[index];
        return result;
    }

    /**
     * Read the species.
     *
     * @return species (not null)
     */
    public String getSpecies() {
        assert species != null;
        return species;
    }

    /**
     * Read the number of parameters.
     *
     * @return count (&ge;0)
     */
    public int numParameters() {
        int result = parameters.length;
        assert result >= 0 : result;
        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Test for equivalence with another solution.
     *
     * @param otherObject (unaffected, may be null)
     * @return true if the solutions are equivalent, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;

        } else if (otherObject == null) {
            return false;

        } else if (otherObject instanceof Solution) {
            Solution otherSolution = (Solution) otherObject;
            String otherSpecies = otherSolution.getSpecies();
            if (!species.equals(otherSpecies)) {
                return false;
            }
            int otherNumParameters = otherSolution.numParameters();
            if (parameters.length != otherNumParameters) {
                return false;
            }
            for (int i = 0; i < parameters.length; i++) {
                float otherParm = otherSolution.getParameter(i);
                if (parameters[i] != otherParm) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Generate the hash code for this solution.
     *
     * @return value for use in hashing
     */
    @Override
    public int hashCode() {
        int result = 13;
        for (int i = 0; i < parameters.length; i++) {
            float ss = parameters[i];
            int bits = Float.floatToIntBits(ss);
            result = 17 * result + bits;
        }

        return result;
    }

    /**
     * Represent this solution as a string of text.
     *
     * @return descriptive string of text (not null, not empty)
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(64);
        buffer.append(species);
        buffer.append("[");
        for (int i = 0; i < parameters.length; i++) {
            double parm = parameters[i];
            buffer.append(parm);
            if (i + 1 < parameters.length) {
                buffer.append(",");
            }
        }
        buffer.append("]");
        String result = buffer.toString();

        return result;
    }
}
