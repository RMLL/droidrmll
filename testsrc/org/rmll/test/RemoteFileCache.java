package org.rmll.test;

import java.io.IOException;
import java.net.MalformedURLException;

import org.rmll.util.FileUtil;

import android.test.AndroidTestCase;

public class RemoteFileCache extends AndroidTestCase {
	public void testRemoteFetch(){
		try {
			FileUtil.fetchCached("http://rmll.info/2010/map/room/aw1105/small");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			fail("IOException during remote fetch");
		}
	}
}
