/**
 * Copyright 2013 Alex Yanchenko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.droidparts.net.cache;

import static org.droidparts.contract.Constants.BUFFER_SIZE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.droidparts.util.AppUtils;
import org.droidparts.util.L;
import org.droidparts.util.crypto.HashCalc;
import org.droidparts.util.io.IOUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

public class BitmapDiskCache implements BitmapCache {

	private static final String DEFAULT_DIR = "img";

	// .png is painfully slow
	private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
	private static final int DEFAULT_COMPRESS_QUALITY = 80;

	private static BitmapDiskCache instance;

	public static BitmapDiskCache getDefaultInstance(Context ctx) {
		if (instance == null) {
			File cacheDir = new AppUtils(ctx).getExternalCacheDir();
			if (cacheDir != null) {
				instance = new BitmapDiskCache(new File(cacheDir, DEFAULT_DIR),
						DEFAULT_COMPRESS_FORMAT, DEFAULT_COMPRESS_QUALITY);
			} else {
				L.w("External cache dir null. Lacking 'android.permission.WRITE_EXTERNAL_STORAGE' permission?");
			}
		}
		return instance;
	}

	private final File cacheDir;
	private final CompressFormat format;
	private final int quality;

	public BitmapDiskCache(File cacheDir, CompressFormat format,
			int quality) {
		this.cacheDir = cacheDir;
		this.format = format;
		this.quality = quality;
		cacheDir.mkdirs();
	}

	@Override
	public boolean put(String key, Bitmap bm) {
		File file = getCachedFile(key);
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(file),
					BUFFER_SIZE);
			bm.compress(format, quality, bos);
			return true;
		} catch (Exception e) {
			L.w(e);
			return false;
		} finally {
			IOUtils.silentlyClose(bos);
		}
	}

	@Override
	public Bitmap get(String key) {
		Bitmap bm = null;
		File file = getCachedFile(key);
		if (file.exists()) {
			BufferedInputStream bis = null;
			try {
				bis = new BufferedInputStream(new FileInputStream(file),
						BUFFER_SIZE);
				bm = BitmapFactory.decodeStream(bis);
				// only after successful restore
				file.setLastModified(System.currentTimeMillis());
			} catch (Exception e) {
				L.w(e);
			} finally {
				IOUtils.silentlyClose(bis);
			}
		}
		if (bm == null) {
			L.i("Cache miss for '%s'.", key);
		}
		return bm;
	}

	public void purgeFilesAccessedBefore(long timestamp) {
		for (File f : IOUtils.getFileList(cacheDir)) {
			if (f.lastModified() < timestamp) {
				f.delete();
			}
		}
	}

	private File getCachedFile(String key) {
		return new File(cacheDir, HashCalc.getMD5(key));
	}

}