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