package com.gallantrealm.mysynth;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import android.opengl.GLSurfaceView;

/**
 * Choose a 24 bit deep configuration of 8,8,8 or 5,6,5 if that's not available.
 */
public class MyWorldConfigChooser implements GLSurfaceView.EGLConfigChooser {

	public MyWorldConfigChooser() {
	}

	@Override
	public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
		System.out.println(">chooseConfig");
		mValue = new int[1];

		int[] configSpec = { //
				EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */, //
				EGL10.EGL_CONFIG_CAVEAT, EGL10.EGL_NONE, //
				EGL10.EGL_TRANSPARENT_TYPE, EGL10.EGL_NONE, //
				EGL10.EGL_DEPTH_SIZE, 24, //
				EGL10.EGL_NONE //
		};
		if (!egl.eglChooseConfig(display, configSpec, null, 0, mValue)) {
			throw new IllegalArgumentException("eglChooseConfig failed");
		}
		int numConfigs = mValue[0];
		EGLConfig[] configs = new EGLConfig[numConfigs];
		if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs, mValue)) {
			throw new IllegalArgumentException("data eglChooseConfig failed");
		}

		System.out.println(" chooseConfig - choosing from " + configs.length + " configs:");
		int bestScore = -1;
		int index = -1;
		for (int i = 0; i < configs.length; ++i) {
			int redsize = findConfigAttrib(egl, display, configs[i], EGL10.EGL_RED_SIZE, 0);
			int greensize = findConfigAttrib(egl, display, configs[i], EGL10.EGL_GREEN_SIZE, 0);
			int bluesize = findConfigAttrib(egl, display, configs[i], EGL10.EGL_BLUE_SIZE, 0);
			int alphasize = findConfigAttrib(egl, display, configs[i], EGL10.EGL_ALPHA_SIZE, 0);
			int depthsize = findConfigAttrib(egl, display, configs[i], EGL10.EGL_DEPTH_SIZE, 0);
			int stencilsize = findConfigAttrib(egl, display, configs[i], EGL10.EGL_STENCIL_SIZE, 0);
			int samples = findConfigAttrib(egl, display, configs[i], EGL10.EGL_SAMPLES, 0);
			int luminance = findConfigAttrib(egl, display, configs[i], EGL10.EGL_LUMINANCE_SIZE, 0);
			int score = 0;
			if (samples == 0) {
				score += 32;
			}
			if (depthsize == 24) {
				score += 16;
			} else if (depthsize == 16) {
				score += 8;
			}
			if (redsize == 8) {
				score += 4;
			}
			if (alphasize == 0) {
				score += 2;
			}
			if (stencilsize == 0) {
				score += 1;
			}
			System.out.println("   #" + i + ":  rgba=" + redsize + "" + greensize + "" + bluesize + "" + alphasize + " depth=" + depthsize + " stencil=" + stencilsize + " samples=" + samples + " luminance=" + luminance + " score=" + score);
			if (score > bestScore) {
				index = i;
				bestScore = score;
			}
		}

		// Choose the best configuration.
		if (index == -1) {
			System.out.println(" chooseConfig - Did not find sane config, using first");
			index = 0;
		}
		System.out.println("  chooseConfig - Using config #" + index);
		EGLConfig config = configs[index];
		System.out.println("<chooseConfig");
		return config;
	}

	private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {
		if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
			return mValue[0];
		}
		return defaultValue;
	}

	private int[] mValue;
}
