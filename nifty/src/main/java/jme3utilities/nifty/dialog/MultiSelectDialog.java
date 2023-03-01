/*
 Copyright (c) 2021-2023, Stephen Gold
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
package jme3utilities.nifty.dialog;

import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Controller for a multi-select dialog box.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MultiSelectDialog<ItemType> implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MultiSelectDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * items to select from, in the displayed order
     */
    final private List<ItemType> allItems;
    /**
     * description of the commit action (not null, not empty, should fit the
     * button)
     */
    final private String commitDescription;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller with the specified list of items.
     *
     * @param description the commit-button text (not null, not empty)
     * @param itemList the items to select from, in the displayed order (not
     * null, not empty, unaffected)
     */
    public MultiSelectDialog(String description, List<ItemType> itemList) {
        Validate.nonEmpty(description, "description");
        Validate.nonEmpty(itemList, "item list");

        this.commitDescription = description;
        this.allItems = new ArrayList<>(itemList);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Determine the commit-button text
     *
     * @return the test (not null, not empty)
     */
    final public String commitDescription() {
        assert commitDescription != null;
        assert !commitDescription.isEmpty();
        return commitDescription;
    }

    /**
     * Count how many items can be selected.
     *
     * @return the count (&ge;0)
     */
    final public int countItems() {
        int result = allItems.size();
        return result;
    }

    /**
     * Access the indexed item.
     *
     * @param index the index among selectable items (&ge;0)
     * @return the pre-existing item
     */
    final public ItemType getItem(int index) {
        ItemType result = allItems.get(index);
        return result;
    }

    /**
     * Enumerate all the descriptions of all items that can be selected by this
     * dialog.
     *
     * @return a new list
     */
    final public List<String> listItemDescriptions() {
        int numItems = countItems();
        List<String> result = new ArrayList<>(numItems);
        for (ItemType item : allItems) {
            String description = item.toString();
            result.add(description);
        }

        return result;
    }

    /**
     * Enumerate all the items that can be selected by this dialog.
     *
     * @return a new unmodifiable list of pre-existing instances
     */
    final public List<ItemType> listItems() {
        List<ItemType> result = Collections.unmodifiableList(allItems);
        return result;
    }

    /**
     * Parse the specified commit suffix to obtain a BitSet.
     *
     * @param commitSuffix the input text (not null, not empty)
     * @return a new BitSet, or null for a syntax error
     */
    public BitSet parseBitSet(String commitSuffix) {
        Validate.nonEmpty(commitSuffix, "commit suffix");

        int size = countItems();
        BitSet result = new BitSet(size);

        String[] selectedDigits = commitSuffix.split(",");
        int numSelected = selectedDigits.length;
        for (String digits : selectedDigits) {
            int index = Integer.parseInt(digits);
            result.set(index);
        }

        return result;
    }

    /**
     * Parse the specified commit suffix to obtain an array of descriptions.
     *
     * @param commitSuffix the input text (not null, not empty)
     * @return a new array, or null for a syntax error
     */
    public String[] parseDescriptionArray(String commitSuffix) {
        Validate.nonEmpty(commitSuffix, "commit suffix");

        String[] selectedDigits = commitSuffix.split(",");
        int numSelected = selectedDigits.length;
        String[] result = new String[numSelected];

        for (int i = 0; i < numSelected; ++i) {
            String digits = selectedDigits[i];
            int index = Integer.parseInt(digits);
            ItemType item = allItems.get(index);
            String description = item.toString();
            result[i] = description;
        }

        return result;
    }

    /**
     * Parse the specified commit suffix to obtain a list of items.
     *
     * @param commitSuffix the input text (not null, not empty)
     * @return a new list in arbitrary order, or null for a syntax error
     */
    public List<ItemType> parseItemList(String commitSuffix) {
        Validate.nonEmpty(commitSuffix, "commit suffix");

        String[] selectedDigits = commitSuffix.split(",");
        int numSelected = selectedDigits.length;
        List<ItemType> result = new ArrayList<>(numSelected);

        for (String digits : selectedDigits) {
            int index = Integer.parseInt(digits);
            ItemType item = getItem(index);
            result.add(item);
        }

        return result;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Determine the feedback message for the specified list of indices. Meant
     * to be overridden.
     *
     * @param indexList the indices of all selected items (not null, unaffected)
     * @return the message (not null)
     */
    protected String feedback(List<Integer> indexList) {
        String result = "";
        return result;
    }
    // *************************************************************************
    // DialogController methods

    /**
     * Test whether "commit" actions are allowed for the current selection.
     *
     * @param dialogElement (not null)
     * @return true if allowed, otherwise false
     */
    @Override
    public boolean allowCommit(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");

        List<Integer> indices = getSelectedIndices(dialogElement);
        String feedback = feedback(indices);
        boolean allow = feedback.isEmpty();

        return allow;
    }

    /**
     * Construct the action-string suffix for a commit.
     *
     * @param dialogElement (not null)
     * @return the commit suffix (not null)
     */
    @Override
    public String commitSuffix(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");

        List<Integer> indices = getSelectedIndices(dialogElement);
        String result = MyString.join(",", indices);

        return result;
    }

    /**
     * Update this dialog box prior to rendering. (Invoked once per frame.)
     *
     * @param dialogElement (not null)
     * @param ignored time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(Element dialogElement, float ignored) {
        Validate.nonNull(dialogElement, "dialog element");

        List<Integer> indices = getSelectedIndices(dialogElement);
        String feedbackMessage = feedback(indices);

        String commitLabel;
        if (feedbackMessage.isEmpty()) {
            commitLabel = commitDescription;
        } else {
            commitLabel = "";
        }

        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);
        commitButton.setText(commitLabel);

        boolean makeButtonVisible = !commitLabel.isEmpty();
        commitButton.getElement().setVisible(makeButtonVisible);

        Element feedbackElement = dialogElement.findElementById("#feedback");
        TextRenderer renderer = feedbackElement.getRenderer(TextRenderer.class);
        renderer.setText(feedbackMessage);

        int numSelected = indices.size();
        int numItems = countItems();
        String numSelectedMessage
                = String.format("Selected %d of %d.", numSelected, numItems);
        Element numSelectedElement
                = dialogElement.findElementById("#numSelected");
        renderer = numSelectedElement.getRenderer(TextRenderer.class);
        renderer.setText(numSelectedMessage);
    }
    // *************************************************************************
    // private methods

    /**
     * Enumerate the selected indices.
     *
     * @param dialogElement (not null)
     * @return a new list of indices
     */
    private static List<Integer> getSelectedIndices(Element dialogElement) {
        assert dialogElement != null;

        ListBox listBox = dialogElement.findNiftyControl("#box", ListBox.class);
        @SuppressWarnings("unchecked")
        List<Integer> result = listBox.getSelectedIndices();

        return result;
    }
}
