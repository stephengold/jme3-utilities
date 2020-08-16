/*
 Copyright (c) 2017-2020, Stephen Gold
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

import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.asset.plugins.ZipLocator;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Manage the registered locators in the AssetManager of an ActionApplication.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Locators {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Locators.class.getName());
    // *************************************************************************
    // fields

    /**
     * the application's asset manager
     */
    private static AssetManager manager = null;
    /**
     * list of locator types (first element is tried first, parallel with
     * {@link #rootPaths})
     */
    final private List<Class<? extends AssetLocator>> locatorTypes
            = new ArrayList<>(6);
    /**
     * stack of configurations for save/restore (the last element is the current
     * configuration)
     */
    final private static List<Locators> stack = new ArrayList<>(3);
    /**
     * list of locator root paths (first element is tried first, parallel with
     * {@link #locatorTypes})
     */
    final private List<String> rootPaths = new ArrayList<>(6);
    // *************************************************************************
    // new methods exposed

    /**
     * Dump the entire stack of locators to the specified PrintStream.
     *
     * @param printStream the output stream (not null)
     */
    public static void dumpAll(PrintStream printStream) {
        Validate.nonNull(printStream, "print stream");

        printStream.printf("%nCurrent locators:%n");
        int lastIndex = stack.size() - 1;
        dump(printStream, lastIndex);

        for (int index = lastIndex - 1; index >= 0; --index) {
            String aside = "";
            if (index == 0) {
                aside = " (oldest)";
            }
            printStream.printf("%nSaved locators[%d]%s:%n", index, aside);
            dump(printStream, index);
        }

        printStream.println();
    }

    /**
     * Access the asset manager.
     *
     * @return the pre-existing instance (not null)
     */
    public static AssetManager getAssetManager() {
        assert manager != null;
        return manager;
    }

    /**
     * Determine the root path of the sole locator in the current configuration.
     *
     * @return root path, or "" if there isn't exactly one locator or if the
     * locator isn't a FileLocator/ZipLocator
     */
    public static String getRootPath() {
        int lastIndex = stack.size() - 1;
        Locators current = stack.get(lastIndex);
        String result = current.configurationGetRootPath();

        assert result != null;
        return result;
    }

    /**
     * Register (add) the specified locator to the current configuration.
     *
     * @param spec URL specification (null specifies the default locators)
     */
    public static void register(String spec) {
        int lastIndex = stack.size() - 1;
        Locators current = stack.get(lastIndex);
        current.configurationRegister(spec);
    }

    /**
     * Register (add) a locator of the specified type to the current
     * configuration.
     *
     * @param rootPath (not null, not empty)
     * @param locatorType type of locator
     */
    public static void register(String rootPath,
            Class<? extends AssetLocator> locatorType) {
        Validate.nonEmpty(rootPath, "root path");

        int lastIndex = stack.size() - 1;
        Locators current = stack.get(lastIndex);
        current.configurationRegister(rootPath, locatorType);
    }

    /**
     * Register (add) the specified locators to the current configuration, in
     * the specified order.
     *
     * @param specList a list of URL specifications (not null, unaffected)
     */
    public static void register(List<String> specList) {
        Validate.nonNull(specList, "spec list");

        for (String specifier : specList) {
            register(specifier);
        }
    }

    /**
     * Register (add) the default locator(s) to the current configuration: the
     * "Written Assets" folder (if one exists) followed by the classpath.
     */
    public static void registerDefault() {
        int lastIndex = stack.size() - 1;
        Locators current = stack.get(lastIndex);
        current.configurationRegisterDefault();
    }

    /**
     * Register (add) a file locator or zip locator to the current
     * configuration.
     *
     * @param rootPath absolute filesystem path to the directory/folder/JAR/ZIP
     * (not null, not empty)
     */
    public static void registerFilesystem(String rootPath) {
        Validate.nonEmpty(rootPath, "root path");

        int lastIndex = stack.size() - 1;
        Locators current = stack.get(lastIndex);
        current.configurationRegisterFilesystem(rootPath);
    }

    /**
     * Restore an old configuration.
     */
    public static void restore() {
        int lastIndex = stack.size() - 1;
        assert lastIndex >= 1 : lastIndex;
        Locators oldTop = stack.get(lastIndex);
        oldTop.justUnregisterAll();

        stack.remove(lastIndex); // pop

        Locators newTop = stack.get(lastIndex - 1);
        newTop.justRegisterAll();
    }

    /**
     * Save a copy of the current configuration for later restoration.
     */
    public static void save() {
        int lastIndex = stack.size() - 1;
        Locators old = stack.get(lastIndex);

        Locators add = new Locators();
        add.locatorTypes.addAll(old.locatorTypes);
        add.rootPaths.addAll(old.rootPaths);

        stack.add(add); // push
    }

    /**
     * Specify the asset manager and reset the save/restore stack.
     *
     * @param assetManager an empty manager to use (not null, alias created)
     */
    static void setAssetManager(AssetManager assetManager) {
        Validate.nonNull(assetManager, "asset manager");

        manager = assetManager;
        stack.clear();
        Locators newConfig = new Locators();
        stack.add(newConfig);
    }

    /**
     * Unregister all locators from the current configuration.
     */
    public static void unregisterAll() {
        int lastIndex = stack.size() - 1;
        Locators current = stack.get(lastIndex);
        current.configurationUnregisterAll();
    }

    /**
     * Set the current configuration to use only the default locator(s). Any
     * other locators get unregistered.
     */
    public static void useDefault() {
        unregisterAll();
        registerDefault();
    }

    /**
     * Set the current configuration to use only the specified file locator or
     * zip locator for assets. Any other locators get unregistered.
     *
     * @param rootPath absolute filesystem path to the directory/folder/JAR/ZIP
     * (not null, not empty)
     */
    public static void useFilesystem(String rootPath) {
        Validate.nonEmpty(rootPath, "root path");

        unregisterAll();
        registerFilesystem(rootPath);
    }
    // *************************************************************************
    // private methods

    /**
     * Find a locator of the specified type with the specified root path.
     *
     * @param rootPath (not null, not empty)
     * @param locatorType type of locator (not null)
     * @return index, or -1 if not found
     */
    private int configurationFind(String rootPath,
            Class<? extends AssetLocator> locatorType) {
        Validate.nonEmpty(rootPath, "root path");

        int result = -1;
        for (int index = 0; index < rootPaths.size(); index++) {
            if (locatorTypes.get(index).equals(locatorType)
                    && rootPaths.get(index).equals(rootPath)) {
                result = index;
                break;
            }
        }

        return result;
    }

    /**
     * Determine the root path of the sole locator in this configuration.
     *
     * @return root path, or "" if there isn't exactly one locator or if the
     * locator isn't a FileLocator/ZipLocator
     */
    private String configurationGetRootPath() {
        String result = "";
        if (locatorTypes.size() == 1) {
            String name = locatorTypes.get(0).toString();
            switch (name) {
                case "class com.jme3.asset.plugins.FileLocator":
                case "class com.jme3.asset.plugins.ZipLocator":
                    result = rootPaths.get(0);
            }
        }

        assert result != null;
        return result;
    }

    /**
     * Register (add) the specified locator(s) to this configuration.
     *
     * @param spec URL specification, or null for the default locators
     */
    private void configurationRegister(String spec) {
        if (spec == null) {
            configurationRegisterDefault();

        } else if (spec.startsWith("file:///")) {
            String rootPath = MyString.remainder(spec, "file:///");
            configurationRegisterFilesystem(rootPath);

        } else if (spec.endsWith(".jar") || spec.endsWith(".zip")) {
            configurationRegister(spec, HttpZipLocator.class);

        } else {
            configurationRegister(spec, UrlLocator.class);
        }
    }

    /**
     * Register (add) a locator of the specified type to this configuration, if
     * it isn't already registered.
     *
     * @param rootPath (not null, not empty)
     * @param locatorType type of locator
     */
    private void configurationRegister(String rootPath,
            Class<? extends AssetLocator> locatorType) {
        Validate.nonEmpty(rootPath, "root path");

        int index = configurationFind(rootPath, locatorType);
        if (index == -1) {
            manager.registerLocator(rootPath, locatorType);
            locatorTypes.add(locatorType);
            rootPaths.add(rootPath);
        }
    }

    /**
     * Register (add) the default locator(s) to this configuration, namely: the
     * "Written Assets" folder (if one exists) followed by the classpath.
     */
    private void configurationRegisterDefault() {
        String wadPath = ActionApplication.writtenAssetPath();
        File wadFile = new File(wadPath);
        if (wadFile.isDirectory()) {
            configurationRegister(wadPath, FileLocator.class);
        }

        configurationRegister("/", ClasspathLocator.class);
    }

    /**
     * Register (add) a file locator or zip locator to this configuration.
     *
     * @param rootPath absolute filesystem path to the directory/folder/JAR/ZIP
     * (not null, not empty)
     */
    private void configurationRegisterFilesystem(String rootPath) {
        Validate.nonEmpty(rootPath, "root path");

        File root = new File(rootPath);
        if (root.isDirectory()) {
            configurationRegister(rootPath, FileLocator.class);
        } else if (rootPath.endsWith(".jar") || rootPath.endsWith(".zip")) {
            configurationRegister(rootPath, ZipLocator.class);
        } else if (!root.exists()) {
            logger.log(Level.WARNING, "{0} does not exist.",
                    MyString.quote(rootPath));
        } else {
            logger.log(Level.WARNING, "{0} is not a directory/folder/JAR/ZIP.",
                    MyString.quote(rootPath));
        }
    }

    /**
     * Unregister all locators from this configuration.
     */
    private void configurationUnregisterAll() {
        justUnregisterAll();
        locatorTypes.clear();
        rootPaths.clear();
    }

    /**
     * Dump the indexed locator to the specified PrintStream.
     *
     * @param printStream the output stream (not null)
     * @param index (&ge;0)
     */
    private static void dump(PrintStream printStream, int index) {
        Locators locators = stack.get(index);

        int numPaths = locators.rootPaths.size();
        assert locators.locatorTypes.size() == numPaths : numPaths;

        if (numPaths == 0) {
            printStream.println("  none!");
        }

        for (int pathIndex = 0; pathIndex < numPaths; ++pathIndex) {
            Class locatorType = locators.locatorTypes.get(pathIndex);
            String typeName = locatorType.getSimpleName();
            if (typeName.endsWith("Locator")) {
                typeName = MyString.removeSuffix(typeName, "Locator");
            }

            String rootPath = locators.rootPaths.get(pathIndex);
            rootPath = MyString.quote(rootPath);

            printStream.printf("  %s rootPath=%s%n", typeName, rootPath);
        }
    }

    /**
     * Register (add) all locators in this configuration with the asset manager,
     * without updating this configuration.
     */
    private void justRegisterAll() {
        int numLocators = locatorTypes.size();
        assert rootPaths.size() == numLocators : numLocators;
        for (int i = 0; i < numLocators; i++) {
            Class<? extends AssetLocator> locatorType = locatorTypes.get(i);
            String rootPath = rootPaths.get(i);
            manager.registerLocator(rootPath, locatorType);
        }
    }

    /**
     * Unregister all locators in this configuration from the asset manager,
     * without updating this configuration.
     */
    private void justUnregisterAll() {
        int numLocators = locatorTypes.size();
        assert rootPaths.size() == numLocators : numLocators;
        for (int i = 0; i < numLocators; i++) {
            Class<? extends AssetLocator> locatorType = locatorTypes.get(i);
            String rootPath = rootPaths.get(i);
            manager.unregisterLocator(rootPath, locatorType);
        }
    }
}
