/*
 Copyright (c) 2017-2019, Stephen Gold
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
package jme3utilities.nifty;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A menu builder for jme3-utilities-nifty.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PopupMenuBuilder {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PopupMenuBuilder.class.getName());
    // *************************************************************************
    // fields

    /**
     * map menu items to icon asset paths
     */
    final protected Map<String, String> itemMap = new HashMap<>(40);
    // *************************************************************************
    // new methods exposed

    /**
     * Add an item without an icon.
     *
     * @param item (not null, not empty)
     */
    public void add(String item) {
        Validate.nonEmpty(item, "item");
        assert !itemMap.containsKey(item) : item;

        itemMap.put(item, null);
    }

    /**
     * Add an item with the specified icon.
     *
     * @param item (not null, not empty)
     * @param iconAssetPath path to the icon's image asset (may be null)
     */
    public void add(String item, String iconAssetPath) {
        Validate.nonEmpty(item, "item");
        assert !itemMap.containsKey(item) : item;

        itemMap.put(item, iconAssetPath);
    }

    /**
     * Add multiple items without icons.
     *
     * @param items (not null, unaffected)
     */
    public void addAll(Iterable<String> items) {
        Validate.nonNull(items, "items");

        for (String item : items) {
            add(item);
        }
    }

    /**
     * Copy the icon asset paths to a new array.
     *
     * @return a new array
     */
    public String[] copyIconAssetPaths() {
        int numIcons = itemMap.size();
        String[] result = new String[numIcons];
        int i = 0;
        for (String icontAssetPath : itemMap.values()) {
            result[i] = icontAssetPath;
            ++i;
        }

        return result;
    }

    /**
     * Copy the items to a new array.
     *
     * @return a new array
     */
    public String[] copyItems() {
        int numItems = itemMap.size();
        String[] result = new String[numItems];
        int i = 0;
        for (String item : itemMap.keySet()) {
            result[i] = item;
            ++i;
        }

        return result;
    }

    /**
     * Test whether the menu contains the specified item.
     *
     * @param item (not null, not empty)
     * @return true if found, otherwise false
     */
    public boolean hasItem(String item) {
        Validate.nonEmpty(item, "item");
        boolean result = itemMap.containsKey(item);
        return result;
    }

    /**
     * Test whether the menu is empty.
     *
     * @return true if empty, otherwise false
     */
    public boolean isEmpty() {
        boolean result = itemMap.isEmpty();
        return result;
    }

    /**
     * Remove everything from the menu and start over.
     */
    public void reset() {
        itemMap.clear();
    }
}
