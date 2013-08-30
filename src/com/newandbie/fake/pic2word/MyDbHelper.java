package com.newandbie.fake.pic2word;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

public class MyDbHelper extends SQLiteOpenHelper {
	private static final String DB_NAME = "question";
	public static final String TABLE = "question";
	public static final String _ID = "_ID";
	public static final String LEVEL = "level";
	public static final String ANSWER = "answer";
	public static final String MISTAKE = "mistake";
	public static final String COIN = "coin";
	public static final String CATE = "cate";
	public static final String PIC = "pic";
	private Context context;

	public MyDbHelper(Context context) {
		super(context, DB_NAME, null, 1);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		if (!isDbExsited()) {
			createDb(db, "question.sql");
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(context);
			pref.edit().putBoolean("FIRST", true).commit();
			Log.d("数据库", "创建成功");
		} else {
			Log.d("数据库", "已存在");
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	/**
	 * 判断数据库文件是否存在
	 * 
	 * @return
	 */
	private boolean isDbExsited() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return pref.getBoolean("FIRST", false);
	}

	private void createDb(SQLiteDatabase db, String sqlFileName) {
		InputStream in = null;
		try {
			in = context.getAssets().open(sqlFileName);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = br.readLine()) != null) {
				db.execSQL(line);
			}
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

	}

}
