package com.newandbie.fake.pic2word;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Question {
	// 关卡数
	private int level;
	// 正确答案
	private String answer;
	// 错误答案
	private String mistake;
	// 分类
	private String cate;
	// 通关奖励
	private int coin;
	// 自定义的图片路径
	private String picPath;

	public MyDbHelper helper;

	private Question() {
		throw new RuntimeException("调个毛线");
	}

	private Question(Context context) {
		helper = new MyDbHelper(context);
	}

	private void setLevel(int level) {
		this.level = level;
	}

	private void setAnswer(String answer) {
		this.answer = answer;
	}

	private void setMistake(String mistake) {
		this.mistake = mistake;
	}

	private void setCate(String cate) {
		this.cate = cate;
	}

	private void setCoin(int coin) {
		this.coin = coin;
	}

	public void setPicPath(String picPath) {
		this.picPath = picPath;
	}

	public static Question getQuestion(Context context, int level)
			throws IllegalArgumentException {
		Question q = new Question(context);
		// 这里应该根据level从数据库里提取答案
		SQLiteDatabase rdb = q.helper.getReadableDatabase();
		Cursor cursor = rdb.query(MyDbHelper.TABLE, new String[] {
				MyDbHelper.LEVEL, MyDbHelper.ANSWER, MyDbHelper.MISTAKE,
				MyDbHelper.COIN, MyDbHelper.CATE, MyDbHelper.PIC },
				MyDbHelper.LEVEL + "=?",
				new String[] { String.valueOf(level) }, null, null, null);
		if (cursor != null && cursor.moveToNext()) {
			final String answer = q.getAnswerFromDb(cursor);
			final String mistake = q.getMistakeFromDb(cursor);
			final String cate = q.getCateFromDb(cursor);
			final int coin = q.getCoinFromDb(cursor);
			final String picPath = q.getPicPathFromDb(cursor);
			// 设置属性
			q.setLevel(level);
			q.setAnswer(answer);
			q.setMistake(mistake);
			q.setCate(cate);
			q.setCoin(coin);
			q.setPicPath(picPath);

			cursor.close();
		} else {
			throw new IllegalArgumentException("level=" + level + ",不存在");
		}
		rdb.close();

		return q;
	}

	private String getPicPathFromDb(Cursor cursor) {
		final int picIndex = cursor.getColumnIndex(MyDbHelper.PIC);
		return cursor.getString(picIndex);
	}

	private String getAnswerFromDb(Cursor cursor) {
		final int answerIndex = cursor.getColumnIndex(MyDbHelper.ANSWER);
		return cursor.getString(answerIndex);
	}

	private String getMistakeFromDb(Cursor cursor) {
		final int mistakeIndex = cursor.getColumnIndex(MyDbHelper.MISTAKE);
		// 这里也应该从数据库提取迷惑性的答案文字
		return cursor.getString(mistakeIndex);
	}

	private int getCoinFromDb(Cursor cursor) {
		final int coinIndex = cursor.getColumnIndex(MyDbHelper.COIN);
		return cursor.getInt(coinIndex);
	}

	private String getCateFromDb(Cursor cursor) {
		final int cateIndex = cursor.getColumnIndex(MyDbHelper.CATE);
		return cursor.getString(cateIndex);
	}

	public int getLevel() {
		return level;
	}

	public String getAnswer() {
		return answer;
	}

	public String getMistake() {
		return mistake;
	}

	public String getCate() {
		return cate;
	}

	public int getCoin() {
		return coin;
	}

	public String getPicPath() {
		return picPath;
	}

}
