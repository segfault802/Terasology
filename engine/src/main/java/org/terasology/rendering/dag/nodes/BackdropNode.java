/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag.nodes;

import org.terasology.assets.ResourceUrn;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.registry.In;
import org.terasology.rendering.backdrop.BackdropRenderer;
import org.terasology.rendering.cameras.Camera;
import org.terasology.rendering.dag.WireframeCapableNode;
import static org.terasology.rendering.opengl.DefaultDynamicFBOs.READ_ONLY_GBUFFER;
import org.terasology.rendering.opengl.FBO;
import org.terasology.rendering.opengl.FBOConfig;
import static org.terasology.rendering.opengl.ScalingFactors.FULL_SCALE;
import org.terasology.rendering.opengl.fbms.DisplayResolutionDependentFBOs;
import org.terasology.rendering.world.WorldRenderer;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.terasology.rendering.opengl.OpenGLUtils.bindDisplay;

/**
 * TODO: Diagram of this node
 */
public class BackdropNode extends WireframeCapableNode {
    public static final ResourceUrn REFRACTIVE_REFLECTIVE = new ResourceUrn("engine:sceneReflectiveRefractive");


    @In
    private BackdropRenderer backdropRenderer;

    @In
    private WorldRenderer worldRenderer;

    @In
    private DisplayResolutionDependentFBOs displayResolutionDependentFBOs;

    private Camera playerCamera;
    private FBO sceneReflectiveRefractive;

    @Override
    public void initialise() {
        super.initialise();
        playerCamera = worldRenderer.getActiveCamera();
        requiresFBO(new FBOConfig(REFRACTIVE_REFLECTIVE, FULL_SCALE, FBO.Type.HDR).useNormalBuffer(), displayResolutionDependentFBOs);
    }

    @Override
    public void process() {
        PerformanceMonitor.startActivity("rendering/backdrop");

        initialClearing();

        READ_ONLY_GBUFFER.bind();
        READ_ONLY_GBUFFER.setRenderBufferMask(true, true, true);

        playerCamera.lookThroughNormalized();
        /**
         * Sets the state to render the Backdrop. At this stage the backdrop is the SkySphere
         * plus the SkyBands passes.
         *
         * The backdrop is the only rendering that has three state-changing methods.
         * This is due to the SkySphere requiring a state and the SkyBands requiring a slightly
         * different one.
         */
        READ_ONLY_GBUFFER.setRenderBufferMask(true, false, false);
        backdropRenderer.render(playerCamera);

        PerformanceMonitor.endActivity();
    }

    /**
     * Initial clearing of a couple of important Frame Buffers. Then binds back the Display.
     */
    // It's unclear why these buffers need to be cleared while all the others don't...
    private void initialClearing() {
        sceneReflectiveRefractive = displayResolutionDependentFBOs.get(REFRACTIVE_REFLECTIVE);
        READ_ONLY_GBUFFER.bind();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        sceneReflectiveRefractive.bind();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        bindDisplay();
    }
}
