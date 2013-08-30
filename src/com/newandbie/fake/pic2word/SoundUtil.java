package com.newandbie.fake.pic2word;

import java.io.IOException;
import java.util.HashMap;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;

/**
 * 播放声音的单元
 * 
 * @author ray
 * 
 */
public class SoundUtil {

	private SoundUtil() {
		// '池'都应该是单例
	}

	private static SoundPool soundPool;
	/** 最大声道数 */
	private static final int MAX_STREAMS = 2;
	private static HashMap<String, Integer> sounds;
	private static LoadListener loadListener;

	public static void play(Context context, String fileName) {
		ensureSoundPool();
		// 如果这个声音已经被缓存了
		if (sounds.containsKey(fileName)) {
			final int id = sounds.get(fileName);
			// 播放指定id的声音，左右声道的音量都是最大
			// 优先级是最低，不循环，不变速
			soundPool.play(id, 1.0f, 1.0f, 0, 0, 1.0f);
		} else {
			// 声音没有被缓存
			AssetFileDescriptor afd = null;
			try {
				afd = context.getAssets().openFd(fileName);
				// 参数 ‘1’ 没有意义
				final int id = soundPool.load(afd, 1);
				sounds.put(fileName, id);
				soundPool.setOnLoadCompleteListener(loadListener);
			} catch (IOException e) {
				e.printStackTrace();
				// 通常是文件不存在
			} finally {
				if (afd != null) {
					try {
						afd.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private static class LoadListener implements OnLoadCompleteListener {

		@Override
		public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
			soundPool.play(sampleId, 1.0f, 1.0f, 0, 0, 1.0f);
		}

	}

	private static void ensureSoundPool() {
		if (loadListener == null) {
			loadListener = new LoadListener();
		}
		if (soundPool == null) {
			// 创建SoundPool对象
			// 同时发声的数量
			// 声音类型
			// 总是0
			soundPool = new SoundPool(MAX_STREAMS,
					AudioManager.STREAM_NOTIFICATION, 0);
		}
		if (sounds == null) {
			sounds = new HashMap<String, Integer>();
		}
	}

	/**
	 * <pre>
	 * Java没有析构函数，所以这样的资源清理方法一般都放在finally中, 
	 * 但是SoundUtil对象里的声音池，生命周期很长，不容易控制，
	 * 所以需要写清文档，让调用者在适当的时机调用该方法
	 * </pre>
	 */
	public static void release() {
		if (soundPool != null) {
			soundPool.release();
			soundPool = null;
		}
		if (sounds != null) {
			sounds.clear();
			sounds = null;
		}
	}
}
