/*
 Copyright (c) 2013, Stephen Gold
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

import com.jme3.input.controls.ActionListener;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Track the active/inactive status of named command signals. A signal may
 * originate from multiple sources such as buttons or hotkeys. A signal is
 * active as long as any of its sources is active.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Signals
        implements ActionListener {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Signals.class.getName());
    // *************************************************************************
    // fields
    /**
     * map signal names to statuses
     */
    final private Map<String, Indices> statusMap = new TreeMap<>();
    // *************************************************************************
    // new methods exposed

    /**
     * Add a new signal with all of its sources inactive. If the signal name is
     * already in use, this has no effect.
     *
     * @param name the name of the command signal (not null)
     */
    public void add(String name) {
        assert name != null;

        Indices status = statusMap.get(name);
        if (status == null) {
            status = new Indices();
            statusMap.put(name, status);
        }
    }

    /**
     * Test whether a signal exists.
     *
     * @param name the signal's name (not null)
     * @return true if a signal with that name exists
     */
    public boolean exists(String name) {
        assert name != null;

        Indices status = statusMap.get(name);
        return status != null;
    }

    /**
     * Test whether a particular signal is active.
     *
     * @param name the signal's name (not null)
     * @return true if any of the signal's sources is active; false if all of
     * the signal's sources are inactive
     */
    public boolean test(String name) {
        assert name != null;

        Indices status = statusMap.get(name);
        if (status == null) {
            logger.log(Level.WARNING,
                    "Testing a signal which has not yet been added: {0}.",
                    MyString.quote(name));
            status = new Indices();
            statusMap.put(name, status);
        }
        boolean result = !status.isEmpty();
        return result;
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process a signal action.
     *
     * @param actionString
     * @param isOngoing
     * @param unused
     */
    @Override
    public void onAction(String actionString, boolean isOngoing, float unused) {
        logger.log(Level.INFO, "action = {0}", MyString.quote(actionString));
        /*
         * Parse the action string into words.
         */
        String[] words = actionString.split("\\s+");
        assert words.length == 3;
        assert "signal".equals(words[0]);
        String name = words[1];
        int sourceIndex = Integer.parseInt(words[2]);
        update(name, sourceIndex, isOngoing);
    }
    // *************************************************************************
    // private methods

    /**
     * Update whether a particular signal source is active.
     *
     * @param name the signal's name
     * @param sourceIndex specifies which signal source (key or button) is being
     * updated
     * @param newState true if the source is active; false is the source is
     * inactive
     */
    private void update(String name, int sourceIndex, boolean newState) {
        Indices status = statusMap.get(name);
        if (status == null) {
            logger.log(Level.WARNING, "Unknown signal: {0}",
                    MyString.quote(name));
            return;
        }
        logger.log(Level.INFO, "name = {0}, newState = {1}", new Object[]{
            MyString.quote(name), newState
        });

        status.addRemove(sourceIndex, newState);
    }
}