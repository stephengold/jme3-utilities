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

import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.asset.plugins.ZipLocator;
import java.io.File;
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
    final private static Logger logger = Logger.getLogger(
            Locators.class.getName());
    // *************************************************************************
    // fields

    /**
     * the application's asset manager
     */
    private static AssetManager manager = null;
    /**
     * list of locator types (1st element is tried first, parallel with
     * {@link #rootPaths})
     */
    final private List<Class<? extends AssetLocator>> locatorTypes = new ArrayList<>(6);
    /**
     * stack of configurations for save/restore (the last element is the current
     * configuration)
     */
    final private static List<Locators> stack = new ArrayList<>(3);
    /**
     * list of locator root paths (1st element is tried first, parallel with
     * {@link #locatorTypes})
     */
    final private List<String> rootPaths = new ArrayList<>(6);
    // *************************************************************************
    // new methods exposed

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
     * Read the root path of the sole locator in the current configuration.
     *
     * @return root path, or "" if there isn't exactly one locator or if the
     * locator isn't a FileLocator/ZipLocator
     */
    public static String getRootPath() {
        int lastIndex = stack.size() - 1;
        Locators current = stack.get(lastIndex);
        String result = current.configurationGetRootPath();

        return result;
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
     * Register (add) the specified locators to the current configuration in the
     * specified order.
     *
     * @param rootPathList a list of root paths in which a null String indicates
     * the default locators
     */
    public static void register(List<String> rootPathList) {
        for (String rootPath : rootPathList) {
            if (rootPath == null) {
                registerDefault();
            } else {
                register(rootPath, FileLocator.class);
            }
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
        int lastIndex = stack.size() - 1;
        Locators current = stack.get(lastIndex);
        current.configurationUnregisterAll();
        current.configurationRegisterDefault();
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

        int lastIndex = stack.size() - 1;
        Locators current = stack.get(lastIndex);
        current.configurationUnregisterAll();
        current.configurationRegisterFilesystem(rootPath);
    }
    // *************************************************************************
    // private methods

    /**
     * Read the root path of the sole locator in this configuration.
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

        return result;
    }

    /**
     * Register (add) a locator of the specified type to this configuration.
     *
     * @param rootPath (not null, not empty)
     * @param locatorType type of locator
     */
    private void configurationRegister(String rootPath,
            Class<? extends AssetLocator> locatorType) {
        Validate.nonEmpty(rootPath, "root path");

        manager.registerLocator(rootPath, locatorType);
        locatorTypes.add(locatorType);
        rootPaths.add(rootPath);
    }

    /**
     * Register (add) the default locator(s) to this configuration, namely: the
     * "Written Assets" folder (if one exists) followed by the classpath.
     */
    private void configurationRegisterDefault() {
        String wadPath = ActionApplication.getWrittenAssetDirPath();
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
