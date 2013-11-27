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