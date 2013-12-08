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
package jme3utilities;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A simple loader for XML assets.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class XmlLoader
        implements AssetLoader {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(XmlLoader.class.getName());
    // *************************************************************************
    // fields
    /**
     * DOM parser for XML
     */
    private static DocumentBuilder parser = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Parse an open input stream.
     *
     * @param stream which stream (not null, open, rewound)
     * @param description (not null)
     * @return a new DOM document
     */
    public static Document parse(InputStream stream, String description) {
        if (parser == null) {
            initialize();
        }
        assert stream != null;
        assert description != null;
        assert parser != null;

        Document document;
        try {
            document = parser.parse(stream);
        } catch (SAXException exception) {
            logger.log(Level.SEVERE, "SAX exception while parsing {0}",
                    description);
            return null;
        } catch (IOException exception) {
            logger.log(Level.SEVERE,
                    "Input exception while parsing {0}", description);
            return null;
        }

        return document;
    }
    // *************************************************************************
    // AssetLoader methods

    /**
     * Load an XML asset.
     *
     * @param assetInfo (not null)
     * @return a new DOM document
     */
    @Override
    public Object load(AssetInfo assetInfo)
            throws IOException {
        /*
         * Open the asset stream.
         */
        InputStream stream = assetInfo.openStream();
        /*
         * Parse the stream's data.
         */
        Document document = parse(stream, "an XML asset");
        return document;
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the loader.
     */
    private static void initialize() {
        /*
         * Create the DOM parser.
         */
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder tmpParser;
        try {
            tmpParser = factory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            logger.log(Level.SEVERE, "Failed to create XML parser.");
            assert false;
            tmpParser = null;
        }
        parser = tmpParser;
    }
}