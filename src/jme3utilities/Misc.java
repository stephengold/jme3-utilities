/*
 Copyright (c) 2013-2014, Stephen Gold
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

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.texture.Texture;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Miscellaneous utility methods. Aside from test cases, all methods here should
 * be public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Misc {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Misc.class.getName());
    /**
     * asset path to the Unshaded material definition
     */
    final public static String unshadedMaterialAssetPath =
            "Common/MatDefs/Misc/Unshaded.j3md";
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Misc() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Create an unshaded material.
     *
     * @param assetManager (not null)
     * @return a new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager) {
        if (assetManager == null) {
            throw new NullPointerException("asset manager should not be null");
        }

        Material material = new Material(assetManager,
                unshadedMaterialAssetPath);
        return material;
    }

    /**
     * Create an unshaded material from a texture asset path.
     *
     * @param assetManager (not null)
     * @param assetPath to the texture asset (not null)
     * @return a new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager,
            String assetPath) {
        if (assetManager == null) {
            throw new NullPointerException("asset manager should not be null");
        }
        if (assetPath == null) {
            throw new NullPointerException("path should not be null");
        }

        Texture texture = loadTexture(assetManager, assetPath);
        Material material = createUnshadedMaterial(assetManager, texture);

        return material;
    }

    /**
     * Create an unshaded material from a texture.
     *
     * @param assetManager (not null)
     * @param texture (not null)
     * @return a new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager,
            Texture texture) {
        if (assetManager == null) {
            throw new NullPointerException("asset manager should not be null");
        }
        if (texture == null) {
            throw new NullPointerException("texture should not be null");
        }

        Material material = createUnshadedMaterial(assetManager);
        material.setTexture("ColorMap", texture);

        return material;
    }

    /**
     * Generate the filesystem path to a file in the user's home directory.
     *
     * @param fileName (not null)
     * @return the generated path
     */
    public static String getUserPath(String fileName) {
        if (fileName == null) {
            throw new NullPointerException("file name should not be null");
        }

        String homePath = System.getProperty("user.home");
        String result = String.format("%s/%s", homePath, fileName);
        return result;
    }

    /**
     * Read the verbose version string for this package.
     *
     * @return the package name, branch, and revision of this file
     */
    public static String getVersion() {//
        return "jme3-utilities skybeta $Rev$";
    }

    /**
     * Read the terse version string for this package.
     *
     * @return the branch and revision of this file
     */
    public static String getVersionShort() {
        String verbose = getVersion();
        String[] words = verbose.split("\\s+");
        String result = String.format("%s %s", words[1], words[3]);

        return result;
    }

    /**
     * Load a non-flipped texture asset in edge-clamp mode.
     *
     * @param assetManager (not null)
     * @param assetPath to the texture asset (not null)
     * @return the texture which was loaded (not null)
     */
    public static Texture loadTexture(AssetManager assetManager,
            String assetPath) {
        if (assetPath == null) {
            throw new NullPointerException("path should not be null");
        }

        boolean flipY = false;
        TextureKey key = new TextureKey(assetPath, flipY);
        Texture texture = assetManager.loadTexture(key);
        // edge-clamp mode is the default

        assert texture != null;
        return texture;
    }

    /**
     * Alter the logging levels of all known loggers.
     *
     * @param newLevel (not null)
     */
    public static void setLoggingLevels(Level newLevel) {
        if (newLevel == null) {
            throw new NullPointerException("level should not be null");
        }

        Logger.getLogger("").setLevel(newLevel);
    }

    /**
     * Convert a collection of strings into an array. This is more convenient
     * than Collection.toArray() because the elements of the resulting array
     * will all be strings.
     *
     * @param collection to convert (not null)
     * @return new array containing the same strings in the same order
     */
    public static String[] toArray(Collection<String> collection) {
        int itemCount = collection.size();
        String[] result = new String[itemCount];

        int nextIndex = 0;
        for (String string : collection) {
            result[nextIndex] = string;
            nextIndex++;
        }
        return result;
    }

    /**
     * Write an image to a PNG file, attempting to overwrite any pre-existing
     * file.
     *
     * @param filePath path to the output file (not null)
     * @param image to be written (not null)
     */
    public static void writeMap(String filePath, RenderedImage image)
            throws IOException {
        if (filePath == null) {
            throw new NullPointerException("path should not be null");
        }
        if (image == null) {
            throw new NullPointerException("image should not be null");
        }

        File textureFile = new File(filePath);
        try {
            ImageIO.write(image, "png", textureFile);
            logger.log(Level.INFO, "wrote texture to {0}",
                    MyString.quote(filePath));

        } catch (IOException exception) {
            logger.log(Level.SEVERE, "write to {0} failed",
                    MyString.quote(filePath));
            boolean success = textureFile.delete();
            if (success) {
                logger.log(Level.INFO, "deleted file {0}",
                        MyString.quote(filePath));
            } else {
                logger.log(Level.SEVERE, "delete of {0} failed",
                        MyString.quote(filePath));
            }
            throw exception;
        }
    }
}