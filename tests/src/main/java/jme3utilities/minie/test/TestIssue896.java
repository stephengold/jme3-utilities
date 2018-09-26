/*
 Copyright (c) 2018, Stephen Gold
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
package jme3utilities.minie.test;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.io.File;
import java.io.IOException;

/**
 * Test case for JME issue #896: PhysicsCharacter writes incomplete gravity and
 * upAxis for Native Bullet.
 * <p>
 * If successful, the loaded and saved vectors will be equal.
 */
public class TestIssue896 extends SimpleApplication {
    // *************************************************************************
    // new methods exposed

    public static void main(String[] args) {
        TestIssue896 app = new TestIssue896();
        app.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    @Override
    public void simpleInitApp() {
        CollisionShape box = new BoxCollisionShape(new Vector3f(1f, 1f, 1f));
        CharacterControl savedControl = new CharacterControl(box, 0.1f);
        Vector3f up = new Vector3f(1f, 1f, 1f).normalizeLocal();
        savedControl.setUp(up);
        Vector3f gravity = up.multLocal(-32f);
        savedControl.setGravity(gravity);
        rootNode.addControl(savedControl);

        BinaryExporter exporter = BinaryExporter.getInstance();
        String filePath;
        try {
            File writeFile = File.createTempFile("TestIssue896", "j3o");
            writeFile.deleteOnExit();
            exporter.save(rootNode, writeFile);
            filePath = writeFile.getCanonicalPath();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        BinaryImporter importer = BinaryImporter.getInstance();
        Node loadedNode;
        try {
            File readFile = new File(filePath);
            loadedNode = (Node) importer.load(readFile);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        CharacterControl loadedControl
                = loadedNode.getControl(CharacterControl.class);

        Vector3f savedGravity = new Vector3f();
        savedControl.getGravity(savedGravity);
        Vector3f loadedGravity = new Vector3f();
        loadedControl.getGravity(loadedGravity);
        System.out.printf("gravity  saved=%s  loaded=%s%n", savedGravity,
                loadedGravity);

        stop();
    }
}
