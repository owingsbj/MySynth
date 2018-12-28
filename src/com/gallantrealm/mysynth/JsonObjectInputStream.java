package com.gallantrealm.mysynth;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class JsonObjectInputStream extends ObjectInputStream {

	public JsonObjectInputStream(InputStream in) throws IOException {
		super(in);
	}

}
