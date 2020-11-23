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
package jme3utilities.ui;

import com.jme3.input.controls.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.SignalTracker;

/**
 * A SignalTracker to handle actions that start with "signal ".
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Signals
        extends SignalTracker
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger2
            = Logger.getLogger(Signals.class.getName());
    // *************************************************************************
    // ActionListener methods

    /**
     * Process a signal action.
     *
     * @param actionString textual description of the action (not null)
     * @param isOngoing true if the action is ongoing, otherwise false
     * @param unused time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean isOngoing, float unused) {
        logger.log(Level.INFO, "action = {0}", MyString.quote(actionString));
        /*
         * Parse the action string into words.
         */
        String[] words = actionString.split("\\s+");
        if (words.length > 3) {
            throw new IllegalArgumentException(
                    "Signal name cannot contain spaces."); // TODO relax this
        }
        assert "signal".equals(words[0]);

        String name = words[1];
        int sourceIndex = Integer.parseInt(words[2]);
        setActive(name, sourceIndex, isOngoing);
    }
}
