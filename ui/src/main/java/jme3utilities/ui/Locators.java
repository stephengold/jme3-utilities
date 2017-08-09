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
 * Utility class to manage the registered asset locators of an
 * ActionApplication's asset manager.
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
     * the asset manager
     */
    private static AssetManager manager = null;
    /**
     * list of locator types
     */
    final private static List<Class<? extends AssetLocator>> locatorTypes = new ArrayList<>(6);
    /**
     * list of locator root paths
     */
    final private static List<String> rootPaths = new ArrayList<>(6);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Locators() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the filesystem root path of the asset folder.
     *
     * @return filesystem path, or "" if there isn't exactly 1 locator or if the
     * locator isn't a FileLocator
     */
    public static String getAssetFolder() {
        String result = "";
        if (locatorTypes.size() == 1) {
            String name = locatorTypes.get(0).toString();
            if (name.equals("class com.jme3.asset.plugins.FileLocator")) {
                result = rootPaths.get(0);
            }
        }

        return result;
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
     * Register (add) a locator of the specified type.
     *
     * @param rootPath (not null, not empty)
     * @param locatorType type of locator
     */
    public static void register(String rootPath,
            Class<? extends AssetLocator> locatorType) {
        Validate.nonEmpty(rootPath, "root path");

        manager.registerLocator(rootPath, locatorType);
        locatorTypes.add(locatorType);
        rootPaths.add(rootPath);
    }

    /**
     * Register (add) the specified locators in the specified order.
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
     * Register (add) the default locator(s): the "Written Assets" folder (if
     * one exists) followed by the classpath.
     */
    public static void registerDefault() {
        String wadPath = ActionApplication.getWrittenAssetDirPath();
        File wadFile = new File(wadPath);
        if (wadFile.isDirectory()) {
            register(wadPath, FileLocator.class);
        }

        register("/", ClasspathLocator.class);
    }

    /**
     * Register (add) a file locator or zip locator.
     *
     * @param rootPath absolute filesystem path to the directory/folder/JAR/ZIP
     * (not null, not empty)
     */
    public static void registerFilesystem(String rootPath) {
        Validate.nonEmpty(rootPath, "root path");

        File root = new File(rootPath);
        if (root.isDirectory()) {
            register(rootPath, FileLocator.class);
        } else if (rootPath.endsWith(".jar") || rootPath.endsWith(".zip")) {
            register(rootPath, ZipLocator.class);
        } else if (!root.exists()) {
            logger.log(Level.WARNING, "{0} does not exist.",
                    MyString.quote(rootPath));
        } else {
            logger.log(Level.WARNING, "{0} is not a directory/folder/JAR/ZIP.",
                    MyString.quote(rootPath));
        }
    }

    /**
     * Specify the asset manager.
     *
     * @param assetManager an empty manager to use (not null, alias created)
     */
    public static void setAssetManager(AssetManager assetManager) {
        Validate.nonNull(assetManager, "asset manager");
        manager = assetManager;
        locatorTypes.clear();
        rootPaths.clear();
    }

    /**
     * Unregister all locators.
     */
    public static void unregisterAll() {
        int numLocators = locatorTypes.size();
        assert rootPaths.size() == numLocators : numLocators;
        for (int i = 0; i < numLocators; i++) {
            Class<? extends AssetLocator> locatorType = locatorTypes.get(i);
            String rootPath = rootPaths.get(i);
            manager.unregisterLocator(rootPath, locatorType);
        }
        locatorTypes.clear();
        rootPaths.clear();
    }

    /**
     * Use only the default locators for assets. Any other locators get
     * unregistered.
     */
    public static void useDefault() {
        unregisterAll();
        registerDefault();
    }

    /**
     * Use only the specified file locator or zip locator for assets. Any other
     * locators get unregistered.
     *
     * @param rootPath absolute filesystem path to the directory/folder/JAR/ZIP
     * (not null, not empty)
     */
    public static void useFilesystem(String rootPath) {
        Validate.nonEmpty(rootPath, "root path");

        unregisterAll();
        registerFilesystem(rootPath);
    }
}
