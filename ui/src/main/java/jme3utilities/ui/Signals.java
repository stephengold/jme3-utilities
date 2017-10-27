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
package jme3utilities.ui;

import com.jme3.input.controls.ActionListener;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Track the active/inactive status of named command signals. A signal may
 * originate from multiple sources such as buttons or hotkeys. A signal is
 * active as long as any of its sources is active.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Signals implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Signals.class.getName());
    // *************************************************************************
    // fields

    /**
     * map signal names to statuses
     */
    final private Map<String, TreeSet<Integer>> statusMap = new TreeMap<>();
    // *************************************************************************
    // new methods exposed

    /**
     * Add a new signal with all of its sources inactive. If the signal name is
     * already in use, this has no effect.
     *
     * @param name name of the command signal (not null)
     */
    public void add(String name) {
        Validate.nonNull(name, "signal name");

        TreeSet<Integer> status = statusMap.get(name);
        if (status == null) {
            status = new TreeSet<>();
            statusMap.put(name, status);
        }
    }

    /**
     * Test whether a signal exists.
     *
     * @param name signal's name (not null)
     * @return true if a signal with that name exists
     */
    public boolean exists(String name) {
        Validate.nonNull(name, "signal name");

        TreeSet<Integer> status = statusMap.get(name);
        return status != null;
    }

    /**
     * Test whether the named signal is active.
     *
     * @param name signal's name (not null)
     * @return true if any of the signal's sources is active; false if all of
     * the signal's sources are inactive
     */
    public boolean test(String name) {
        Validate.nonNull(name, "signal name");

        TreeSet<Integer> status = statusMap.get(name);
        if (status == null) {
            logger.log(Level.WARNING,
                    "Testing a signal which has not yet been added: {0}.",
                    MyString.quote(name));
            status = new TreeSet<>();
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
     * @param actionString textual description of the action (not null)
     * @param isOngoing true if the action is ongoing, otherwise false
     * @param unused time per frame (in seconds)
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
     * Update whether a named signal source is active.
     *
     * @param name signal's name (not null)
     * @param sourceIndex index of the signal source (key or button) which is
     * being updated
     * @param newState true if the source is active; false is the source is
     * inactive
     */
    private void update(String name, int sourceIndex, boolean newState) {
        assert name != null;

        TreeSet<Integer> status = statusMap.get(name);
        if (status == null) {
            logger.log(Level.WARNING, "Unknown signal: {0}",
                    MyString.quote(name));
            return;
        }
        logger.log(Level.INFO, "name = {0}, newState = {1}", new Object[]{
            MyString.quote(name), newState
        });

        if (newState) {
            status.add(sourceIndex);
        } else {
            status.remove(sourceIndex);
        }
    }
}
