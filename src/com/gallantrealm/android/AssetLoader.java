package com.gallantrealm.android;

import java.io.IOException;
import java.io.InputStream;
import com.gallantrealm.mysynth.ClientModel;
import android.app.Activity;

public final class AssetLoader {
	
	public static InputStream loadAsset(String assetName) throws IOException {
		Activity context = ClientModel.getClientModel().getContext();
		return context.getAssets().open(assetName);
	}
	
	public static long getAssetLength(String assetName) throws IOException {
		Activity context = ClientModel.getClientModel().getContext();
		return context.getAssets().openFd(assetName).getLength();
	}
}
