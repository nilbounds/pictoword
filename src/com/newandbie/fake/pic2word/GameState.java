package com.newandbie.fake.pic2word;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;

/**
 * 游戏状态
 * 
 * @author ray
 * 
 */
public class GameState {
	/** 存档文件名 */
	private static final String SAVE_FILE = "game.state";
	/** 当前关卡数 */
	private int level;
	/** 当前金币数 */
	private int coin;

	public int getLevel() {
		return level;
	}

	public int getCoin() {
		return coin;
	}

	public void addCoin(int coin) {
		this.coin += coin;
	}

	public void nextLevel() {
		level++;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	private GameState() {

	}

	/**
	 * 存档
	 */
	public void saveState(Context context) {
		File saveFile = new File(context.getFilesDir(), SAVE_FILE);
		if (!saveFile.getParentFile().isDirectory()) {
			saveFile.getParentFile().mkdirs();
		}
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new FileOutputStream(saveFile, false));
			// 顺序很重要
			out.writeInt(level);
			out.writeInt(coin);
			out.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 读档
	 * 
	 * @return
	 */
	public static GameState loadState(Context context) {
		File saveFile = new File(context.getFilesDir(), SAVE_FILE);
		DataInputStream in = null;
		try {
			in = new DataInputStream(new FileInputStream(saveFile));
			GameState gameState = new GameState();
			gameState.level = in.readInt();
			gameState.coin = in.readInt();
			return gameState;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return defaultState();
	}

	private static GameState defaultState() {
		GameState gameState = new GameState();
		gameState.level = 1;
		gameState.coin = 0;
		return gameState;
	}
}
