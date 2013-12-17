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
package jme3utilities.ui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.MenuItemActivatedEvent;
import de.lessvoid.nifty.elements.Element;
import java.util.logging.Logger;
import org.bushe.swing.event.EventTopicSubscriber;

/**
 * Event subscriber for a simple Nifty popup menu or submenu whose items are
 * Strings.
 * <p>
 * When a menu item gets activated, invoke SimpleScreenController.perform() with
 * the item appended to the menu's action prefix, then close the menu.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class PopupMenu
        implements EventTopicSubscriber<MenuItemActivatedEvent<String>> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(PopupMenu.class.getName());
    // *************************************************************************
    // fields
    /**
     * the parent popup menu which opened this submenu, or null if not a
     * submenu: set by constructor
     */
    final private PopupMenu parent;
    /**
     * which screen controls this popup menu: set by constructor
     */
    final private SimpleScreenController controller;
    /**
     * prefix for the menu's action strings: set by constructor
     */
    final private String actionPrefix;
    /**
     * Nifty id of the popup: set by constructor
     */
    final private String popupId;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a submenu for a particular screen controller, action prefix,
     * and parent menu.
     *
     * @param controller which screen will own this menu (not null)
     * @param popupId Nifty id of the popup (not null)
     * @param actionPrefix prefix for action strings (not null)
     * @param parent the parent popup menu which opened this submenu (or null if
     * not a submenu)
     */
    PopupMenu(SimpleScreenController controller, String popupId,
            String actionPrefix, PopupMenu parent) {
        assert controller != null;
        assert actionPrefix != null;

        this.controller = controller;
        this.actionPrefix = actionPrefix;
        this.popupId = popupId;
        this.parent = parent;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Close this menu.
     */
    void close() {
        Nifty nifty = controller.getNifty();
        nifty.closePopup(popupId);
    }

    /**
     * Access the parent: the popup menu which opened this submenu.
     *
     * @return the pre-existing instance, or null if no parent
     */
    PopupMenu getParent() {
        return parent;
    }

    /**
     * Enable or disable this menu.
     *
     * @param newState true to enable, false to disable
     */
    void setEnabled(boolean newState) {
        Nifty nifty = controller.getNifty();
        Element element = nifty.findPopupByName(popupId);
        if (newState) {
            element.enable();
        } else {
            element.disable();
        }
    }
    // *************************************************************************
    // EventTopicSubscriber methods

    /**
     * Callback to deal with the activation of an item in this menu.
     *
     * @param controlId Nifty id of this menu's control (not null)
     * @param event details, such as which item got activated (not null)
     */
    @Override
    public void onEvent(String controlId,
            MenuItemActivatedEvent<String> event) {
        assert controlId != null;

        String itemName = event.getItem();
        /*
         * Generate the action string for the item by appending the item's
         * name to the menu's action prefix.
         */
        String actionString = actionPrefix + itemName;
        /*
         * Perform the action specified by the action string.
         */
        SimpleScreenController.perform(actionString);
        /*
         * If this menu is still active, close it and all of its ancestors.
         */
        controller.closePopup(this);
    }
}