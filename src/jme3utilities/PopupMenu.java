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

import com.google.common.base.Joiner;
import de.lessvoid.nifty.controls.MenuItemActivatedEvent;
import java.util.logging.Logger;
import org.bushe.swing.event.EventTopicSubscriber;

/**
 * Event subscriber for a simple Nifty popup menu.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class PopupMenu
        implements EventTopicSubscriber<MenuItemActivatedEvent<String>> {
    // *************************************************************************
    // constants

    /**
     * joiner for constructing action strings
     */
    final private static Joiner actionJoiner = Joiner.on(" ");
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(PopupMenu.class.getName());
    // *************************************************************************
    // fields
    /**
     * which screen controls this popup menu: set by constructor
     */
    final private SimpleScreenController controller;
    /**
     * prefix words for the menu's action strings: set by constructor
     */
    final private String[] actionPrefixWords;
    // *************************************************************************
    // constructors

    /**
     * Instantiate for a particular screen controller, action prefix, and
     * element.
     *
     * @param controller which screen owns the popup menu (not null)
     * @param actionPrefixWords prefix words for action strings (unaffected, not
     * null)
     * @param elementId id of the popup element (not null)
     */
    PopupMenu(SimpleScreenController controller, String[] actionPrefixWords) {
        assert controller != null;
        assert actionPrefixWords != null;

        this.controller = controller;
        this.actionPrefixWords = actionPrefixWords.clone();
    }
    // *************************************************************************
    // EventTopicSubscriber methods

    /**
     * Handle an event from the Nifty GUI.
     *
     * @param ignored
     * @param event (not null)
     */
    @Override
    public void onEvent(String ignored, MenuItemActivatedEvent<String> event) {
        String itemName = event.getItem();
        /*
         * Generate the action string for the item by appending the item's
         * name to the menu's action prefix.
         */
        String actionString;
        int wordCount = actionPrefixWords.length;
        if (wordCount > 0) {
            String actionPrefix = actionJoiner.join(actionPrefixWords);
            actionString = actionJoiner.join(actionPrefix, itemName);
        } else {
            actionString = itemName;
        }
        /*
         * Perform the action and then close the popup.
         */
        SimpleScreenController.perform(actionString);
        controller.closeActivePopup();
    }
}