/*
 * Copyright 2014-2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotcrab.vis.runtime.scene;

import com.artemis.BaseSystem;
import com.artemis.Manager;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.kotcrab.vis.runtime.RuntimeConfiguration;
import com.kotcrab.vis.runtime.RuntimeContext;
import com.kotcrab.vis.runtime.data.SceneData;
import com.kotcrab.vis.runtime.plugin.EntitySupport;
import com.kotcrab.vis.runtime.scene.SceneLoader.SceneParameter;
import com.kotcrab.vis.runtime.system.*;
import com.kotcrab.vis.runtime.util.ArtemisUtils;
import com.kotcrab.vis.runtime.util.EntityEngine;
import com.kotcrab.vis.runtime.util.EntityEngineConfiguration;

/**
 * Base class of VisRuntime scene system. Scene are typically constructed using {@link VisAssetManager} with {@link SceneLoader}
 * @author Kotcrab
 */
public class Scene {
	private CameraManager cameraManager;
	private EntityEngine engine;

	/** Used by framework, not indented for external use */
	public Scene (RuntimeContext context, SceneData data, SceneParameter parameter) {
		AssetManager assetsManager = context.assetsManager;
		RuntimeConfiguration runtimeConfig = context.configuration;

		ShaderProgram distanceFieldShader = null;
		if (assetsManager.isLoaded(SceneLoader.DISTANCE_FIELD_SHADER)) {
			distanceFieldShader = assetsManager.get(SceneLoader.DISTANCE_FIELD_SHADER, ShaderProgram.class);
		}

		EntityEngineConfiguration engineConfig = new EntityEngineConfiguration();

		engineConfig.setManager(cameraManager = new CameraManager(data.viewport, data.width, data.height, data.pixelsPerUnit));
		engineConfig.setSystem(new SpriteInflaterSystem(runtimeConfig, assetsManager));
		engineConfig.setSystem(new SoundInflaterSystem(runtimeConfig, assetsManager));
		engineConfig.setSystem(new MusicInflaterSystem(runtimeConfig, assetsManager));
		engineConfig.setSystem(new ParticleInflaterSystem(runtimeConfig, assetsManager, data.pixelsPerUnit));
		engineConfig.setSystem(new TextInflaterSystem(runtimeConfig, assetsManager, data.pixelsPerUnit));

		engineConfig.setManager(new VisIDManager());

		ArtemisUtils.createCommonSystems(engineConfig, context.batch, distanceFieldShader, true);
		engineConfig.setSystem(new ParticleRenderSystem(engineConfig.getSystem(RenderBatchingSystem.class), false), true);

		for (EntitySupport support : context.supports) {
			support.registerSystems(runtimeConfig, engineConfig, assetsManager);
		}

		if (parameter != null) {
			for (BaseSystem system : parameter.systems)
				engineConfig.setSystem(system);

			for (Manager manager : parameter.managers)
				engineConfig.setManager(manager);
		}

		engine = new EntityEngine(engineConfig);
	}

	/** Updates and renders entire scene. Typically called from {@link ApplicationListener#render()} */
	public void render () {
		engine.setDelta(Gdx.graphics.getDeltaTime());
		engine.process();
	}

	/** Must by called when screen was resized. Typically called from {@link ApplicationListener#resize(int, int)} */
	public void resize (int width, int height) {
		cameraManager.resize(width, height);
	}

	public EntityEngine getEntityEngine () {
		return engine;
	}
}
