/*
 * Copyright (c) 2009-2018 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.PhysicsSpace.BroadphaseType;
import com.jme3.bullet.debug.BulletDebugAppState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * An app state to manage a single Bullet physics space.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public class BulletAppState implements AppState, PhysicsTickListener {

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BulletAppState.class.getName());
    /**
     * true if-and-only-if the physics simulation is running (started but not
     * yet stopped)
     */
    private boolean initialized = false;
    /**
     * app-state manager that manages this state
     */
    private AppStateManager stateManager;
    /**
     * executor service for delayed physics tasks
     */
    private ScheduledThreadPoolExecutor executor;
    /**
     * physics space managed by this state
     */
    private PhysicsSpace pSpace;
    /**
     * threading mode to use (not null)
     */
    private ThreadingType threadingType = ThreadingType.SEQUENTIAL;
    /**
     * broadphase collision-detection algorithm to use (not null)
     */
    private BroadphaseType broadphaseType = BroadphaseType.DBVT;
    /**
     * minimum coordinate values for the physics space when using AXIS_SWEEP
     * broadphase algorithms
     */
    final private Vector3f worldMin = new Vector3f(-10000f, -10000f, -10000f);
    /**
     * maximum coordinate values for the physics space when using AXIS_SWEEP
     * broadphase algorithms
     */
    final private Vector3f worldMax = new Vector3f(10000f, 10000f, 10000f);
    /**
     * simulation speed multiplier (default=1, paused=0)
     */
    private float speed = 1f;
    /**
     * true if-and-only-if this state is enabled
     */
    private boolean active = true;
    /**
     * true if-and-only-if debug visualization is enabled
     */
    private boolean debugEnabled = false;
    /**
     * true if-and-only-if this state has been attached at least once
     */
    private boolean isAttached = false;
    /**
     * app state to manage the debug visualization
     */
    private BulletDebugAppState debugAppState;
    /**
     * time-per-frame from the most recent update() (in seconds)
     */
    private float tpf;
    private Future physicsFuture;
    /**
     * view ports in which to render the debug visualization
     */
    private ViewPort[] debugViewPorts = null;

    /**
     * Create an app state to manage a new PhysicsSpace with DBVT collision
     * detection.
     * <p>
     * Use getStateManager().addState(bulletAppState) to start physics.
     */
    public BulletAppState() {
    }

    /**
     * Create an app state to manage a new PhysicsSpace.
     * <p>
     * Use getStateManager().addState(bulletAppState) to start physics.
     *
     * @param broadphaseType which broadphase collision-detection algorithm to
     * use (not null)
     */
    public BulletAppState(BroadphaseType broadphaseType) {
        this(new Vector3f(-10000f, -10000f, -10000f),
                new Vector3f(10000f, 10000f, 10000f),
                broadphaseType);
    }

    /**
     * Create an app state to manage a new PhysicsSpace with AXIS_SWEEP_3
     * collision detection.
     * <p>
     * Use getStateManager().addState(bulletAppState) to start physics.
     *
     * @param worldMin the desired minimum coordinates values (not null,
     * unaffected)
     * @param worldMax the desired minimum coordinates values (not null,
     * unaffected)
     */
    public BulletAppState(Vector3f worldMin, Vector3f worldMax) {
        this(worldMin, worldMax, BroadphaseType.AXIS_SWEEP_3);
    }

    /**
     * Create an app state to manage a new PhysicsSpace.
     * <p>
     * Use getStateManager().addState(bulletAppState) to enable physics.
     *
     * @param worldMin the desired minimum coordinates values (not null,
     * unaffected)
     * @param worldMax the desired minimum coordinates values (not null,
     * unaffected)
     * @param broadphaseType which broadphase collision-detection algorithm to
     * use (not null)
     */
    public BulletAppState(Vector3f worldMin, Vector3f worldMax,
            BroadphaseType broadphaseType) {
        Validate.nonNull(worldMin, "world min");
        Validate.nonNull(worldMax, "world max");
        Validate.nonNull(broadphaseType, "broadphase type");

        this.worldMin.set(worldMin);
        this.worldMax.set(worldMax);
        this.broadphaseType = broadphaseType;
    }

    private boolean startPhysicsOnExecutor() {
        if (executor != null) {
            executor.shutdown();
        }
        executor = new ScheduledThreadPoolExecutor(1);
        final BulletAppState app = this;
        Callable<Boolean> call = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                detachedPhysicsLastUpdate = System.currentTimeMillis();
                pSpace = new PhysicsSpace(worldMin, worldMax, broadphaseType);
                pSpace.addTickListener(app);
                return true;
            }
        };
        try {
            return executor.submit(call).get();
        } catch (InterruptedException | ExecutionException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    private Callable<Boolean> parallelPhysicsUpdate = new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
            pSpace.update(tpf * getSpeed());
            return true;
        }
    };

    private long detachedPhysicsLastUpdate = 0;
    private Callable<Boolean> detachedPhysicsUpdate = new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
            pSpace.update(getPhysicsSpace().getAccuracy() * getSpeed());
            pSpace.distributeEvents();
            long update = System.currentTimeMillis() - detachedPhysicsLastUpdate;
            detachedPhysicsLastUpdate = System.currentTimeMillis();
            executor.schedule(detachedPhysicsUpdate,
                    Math.round(getPhysicsSpace().getAccuracy() * 1000000.0f) - (update * 1000),
                    TimeUnit.MICROSECONDS);
            return true;
        }
    };

    /**
     * Access the PhysicsSpace managed by this app state. Normally there is none
     * until the state is attached.
     *
     * @return the pre-existing instance, or null if no simulation running
     */
    public PhysicsSpace getPhysicsSpace() {
        return pSpace;
    }

    /**
     * The physics system is started automatically on attaching. If you want to
     * start it before for some reason, you can use this method.
     */
    public void startPhysics() {
        if (initialized) {
            return;
        }
        //start physics thread(pool)
        if (threadingType == ThreadingType.PARALLEL) {
            startPhysicsOnExecutor();
        } else {
            pSpace = new PhysicsSpace(worldMin, worldMax, broadphaseType);
        }
        pSpace.addTickListener(this);
        initialized = true;
    }

    /**
     * The physics system is stopped automatically during the cleanup that takes
     * place after it is detached.
     */
    private void stopPhysics() {
        if (!initialized) {
            return;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        pSpace.removeTickListener(this);
        pSpace.destroy();
        initialized = false;
    }

    /**
     * Initialize this state prior to its 1st update. Should be invoked only by
     * a subclass or by the AppStateManager.
     *
     * @param stateManager the manager for this state (not null)
     * @param app the application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.stateManager = stateManager;
        if (debugViewPorts == null) {
            debugViewPorts = new ViewPort[1];
            debugViewPorts[0] = app.getViewPort();
        }
        startPhysics();
    }

    /**
     * Test whether the physics simulation is running (started but not yet
     * stopped).
     *
     * @return true if running, otherwise false
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Enable or disable this state.
     *
     * @param enabled true &rarr; enable, false &rarr; disable
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.active = enabled;
    }

    /**
     * Test whether this state is enabled.
     *
     * @return true if enabled, otherwise false
     */
    @Override
    public boolean isEnabled() {
        return active;
    }

    /**
     * Alter whether debug visualization is enabled.
     *
     * @param debugEnabled true &rarr; enable, false &rarr; disable
     */
    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    /**
     * Alter which view ports will render the debug visualization.
     *
     * @param viewPorts (not null, alias created)
     */
    public void setDebugViewPorts(ViewPort[] viewPorts) {
        Validate.nonNull(viewPorts, "view ports");

        debugViewPorts = viewPorts;
        if (debugEnabled) {
            if (debugAppState != null) {
                stateManager.detach(debugAppState);
            }
            startDebug();
        }
    }

    /**
     * Test whether debug visualization is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void stateAttached(AppStateManager stateManager) {
        if (!initialized) {
            startPhysics();
        }
        if (threadingType == ThreadingType.PARALLEL) {
            PhysicsSpace.setLocalThreadPhysicsSpace(pSpace);
        }
        if (debugEnabled) {
            startDebug();
        }
        isAttached = true;
    }

    @Override
    public void stateDetached(AppStateManager stateManager) {
    }

    @Override
    public void update(float tpf) {
        if (debugEnabled && debugAppState == null && pSpace != null) {
            startDebug();
            debugAppState = new BulletDebugAppState(pSpace, debugViewPorts);
            stateManager.attach(debugAppState);
        } else if (!debugEnabled && debugAppState != null) {
            stateManager.detach(debugAppState);
            debugAppState = null;
        }
        if (!active) {
            return;
        }
        pSpace.distributeEvents();
        this.tpf = tpf;
    }

    @Override
    public void render(RenderManager rm) {
        if (!active) {
            return;
        }
        if (threadingType == ThreadingType.PARALLEL) {
            physicsFuture = executor.submit(parallelPhysicsUpdate);
        } else if (threadingType == ThreadingType.SEQUENTIAL) {
            pSpace.update(active ? tpf * speed : 0);
        }
    }

    @Override
    public void postRender() {
        if (physicsFuture != null) {
            try {
                physicsFuture.get();
                physicsFuture = null;
            } catch (InterruptedException | ExecutionException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void cleanup() {
        if (debugAppState != null) {
            stateManager.detach(debugAppState);
            debugAppState = null;
        }
        stopPhysics();
    }

    /**
     * Read which type of threading this app state uses.
     *
     * @return the threadingType
     */
    public ThreadingType getThreadingType() {
        return threadingType;
    }

    /**
     * Alter which type of threading this app state uses. Not allowed after
     * attaching the app state.
     *
     * @param threadingType the desired type (not null)
     */
    public void setThreadingType(ThreadingType threadingType) {
        assert !isAttached;
        this.threadingType = threadingType;
    }

    /**
     * Read which broadphase collision-detection algorithm this app state uses.
     *
     * @return enum value (not null)
     */
    public BroadphaseType getBroadphaseType() {
        return broadphaseType;
    }

    /**
     * Alter the broadphase type. Not allowed after attaching the app state.
     *
     * @param broadphaseType an enum value (not null)
     */
    public void setBroadphaseType(BroadphaseType broadphaseType) {
        Validate.nonNull(broadphaseType, "broadphase type");
        assert !isAttached;

        this.broadphaseType = broadphaseType;
    }

    /**
     * Alter the coordinate range. Not allowed after attaching the app state.
     *
     * @param worldMin the desired minimum coordinate values when using
     * AXIS_SWEEP broadphase algorithms (not null, unaffected)
     */
    public void setWorldMin(Vector3f worldMin) {
        Validate.nonNull(worldMin, "world min");
        assert !isAttached;

        this.worldMin.set(worldMin);
    }

    /**
     * Alter the coordinate range. Not allowed after attaching the app state.
     *
     * @param worldMax the desired maximum coordinate values when using
     * AXIS_SWEEP broadphase algorithms (not null, unaffected)
     */
    public void setWorldMax(Vector3f worldMax) {
        Validate.nonNull(worldMin, "world max");
        assert !isAttached;

        this.worldMax.set(worldMax);
    }

    /**
     * Read the simulation speed.
     *
     * @return speed (&ge;0, default=1)
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Alter the simulation speed.
     *
     * @param speed (&ge;0, default=1)
     */
    public void setSpeed(float speed) {
        Validate.nonNegative(speed, "speed");
        this.speed = speed;
    }

    /**
     * Create the debug app state and attach it.
     */
    private void startDebug() {
        assert debugEnabled;
        debugAppState = new BulletDebugAppState(pSpace, debugViewPorts);
        stateManager.attach(debugAppState);
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback invoked before the physics is stepped. A good time to
     * clear/apply forces.
     *
     * @param space the space that is about to be stepped (not null)
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
    }

    /**
     * Callback invoked after the physics has been stepped, use to check for
     * forces etc.
     *
     * @param space the space that was just stepped (not null)
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
    }

    /**
     * Enumerate threading modes.
     */
    public enum ThreadingType {
        /**
         * Default mode: user update, physics update, and rendering happen
         * sequentially. (single threaded)
         */
        SEQUENTIAL,
        /**
         * Parallel threaded mode: physics update and rendering are executed in
         * parallel, update order is maintained.
         *
         * In this mode, multiple BulletAppStates will execute in parallel.
         */
        PARALLEL,
    }
}
