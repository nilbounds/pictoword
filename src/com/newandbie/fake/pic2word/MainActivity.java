package com.newandbie.fake.pic2word;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener,
		OnItemClickListener {
	private GridView gridAnswer;
	private GridView gridSelected;
	private ImageView imgPic;
	private ImageView imgAddAnswer;
	private ImageView imgRemoveAnswer;
	private ImageView imgShare;
	private TextView textCate;
	private ImageView imgBack;
	private TextView textCoin;
	private TextView textLevel;
	private AnswerAdapter answerAdapter;
	private SelectedAdapter selectedAdapter;

	// 不绑定监听器的控件
	private RelativeLayout titleLayout;
	private LinearLayout contentLayout;
	private RelativeLayout picLayout;
	private ImageView imgDivider;

	// 游戏逻辑相关
	private static final long RANDOM_SEED = 0x314159265758l;
	private Random rnd = new Random(RANDOM_SEED);
	private Question currentQuestion;
	private GameState gameState;
	/** 答案不正确? */
	private boolean isAnswerIncorrect;
	/** 去掉一个错误答案所需的金币 */
	private final int REMOVE_COIN = 30;
	/** 添加一个正确答案所需的金币 */
	private final int ADD_COIN = 90;

	// 产品中要去掉的
	/** 添加金币的提示 */
	private Toast addCoinToast;
	/** 使用上限的提示 */
	private Toast useLimitToast;
	/** 金币不足的提示 */
	private Toast coinLeakToast;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		gridAnswer = (GridView) findViewById(R.id.grid_answer);
		gridAnswer.setSoundEffectsEnabled(false);
		gridSelected = (GridView) findViewById(R.id.grid_selected);
		gridSelected.setSoundEffectsEnabled(false);
		imgPic = (ImageView) findViewById(R.id.img_pic);
		imgAddAnswer = (ImageView) findViewById(R.id.img_add_answer);
		imgAddAnswer.setOnClickListener(this);
		imgRemoveAnswer = (ImageView) findViewById(R.id.img_remove_answer);
		imgRemoveAnswer.setOnClickListener(this);
		imgShare = (ImageView) findViewById(R.id.img_share);
		imgShare.setOnClickListener(this);
		imgBack = (ImageView) findViewById(R.id.img_back);
		imgBack.setOnClickListener(this);
		textCoin = (TextView) findViewById(R.id.text_coin);
		textCoin.setOnClickListener(this);
		textLevel = (TextView) findViewById(R.id.text_level);

		textCate = (TextView) findViewById(R.id.text_cate);

		// 拿到屏幕的宽度
		final int SCREEN_WIDTH = getResources().getDisplayMetrics().widthPixels;

		// 修改GridView的高度
		// 表格布局中每个item间的间距
		final int SPACING = 1;// 单位 px
		int gridCellWidth = -1;// 表格里每一个单元的宽度（高度）
		RelativeLayout.LayoutParams lp = (LayoutParams) gridAnswer
				.getLayoutParams();
		gridCellWidth = (int) (SCREEN_WIDTH / 8.0f) - SPACING;
		final int GRID_ANSWER_HEIGHT = lp.height = gridCellWidth * 3 + SPACING
				* 2;
		gridAnswer.setLayoutParams(lp);
		// 不要显示选择时的背景高亮色
		gridAnswer.setSelector(new ColorDrawable(0x00000000));
		gridAnswer.setHorizontalSpacing(SPACING);
		gridAnswer.setVerticalSpacing(SPACING);

		LinearLayout.LayoutParams llp = (android.widget.LinearLayout.LayoutParams) gridSelected
				.getLayoutParams();
		llp.height = gridCellWidth;
		gridSelected.setLayoutParams(llp);
		gridSelected.setSelector(new ColorDrawable(0x00000000));

		// 修改ImageView的宽高
		// 中间的图片
		lp = (LayoutParams) imgPic.getLayoutParams();
		final int PIC_WIDTH = lp.width = lp.height = (int) (SCREEN_WIDTH * 0.6f);
		imgPic.setLayoutParams(lp);

		// 分类飘带
		final int orgCateWidth = 310;
		final int orgCateHeight = 58;
		lp = (LayoutParams) textCate.getLayoutParams();
		lp.width = (int) (PIC_WIDTH * 0.8f);
		final int CATE_HEIGHT = lp.height = (int) ((float) lp.width
				/ orgCateWidth * orgCateHeight);
		textCate.setLayoutParams(lp);

		// 根据飘带的高度，设置pic的上边距
		lp = (LayoutParams) imgPic.getLayoutParams();
		lp.topMargin = CATE_HEIGHT / 2;
		imgPic.setLayoutParams(lp);

		// 左侧的两个提示按钮
		final int orgWidth = 92;
		final int orgHeight = 123;
		int btnHintWidth = -1;
		int btnHintHeight = -1;

		btnHintWidth = (int) ((SCREEN_WIDTH - PIC_WIDTH) / 2.0f * 0.8f);
		btnHintHeight = (int) ((float) btnHintWidth / orgWidth * orgHeight);

		// 去掉一个错误答案
		llp = (android.widget.LinearLayout.LayoutParams) imgRemoveAnswer
				.getLayoutParams();
		llp.width = btnHintWidth;
		llp.height = btnHintHeight;
		llp.leftMargin = (int) ((SCREEN_WIDTH - PIC_WIDTH) / 2 * 0.2f / 2);
		imgRemoveAnswer.setLayoutParams(llp);

		// 提示一个正确答案
		llp = (android.widget.LinearLayout.LayoutParams) imgAddAnswer
				.getLayoutParams();
		llp.width = btnHintWidth;
		llp.height = btnHintHeight;
		llp.leftMargin = (int) ((SCREEN_WIDTH - PIC_WIDTH) / 2 * 0.2f / 2);
		imgAddAnswer.setLayoutParams(llp);

		// 分享按钮
		final int orgShareWidth = 92;
		final int orgShareHeight = 96;
		lp = (LayoutParams) imgShare.getLayoutParams();
		lp.width = btnHintWidth;
		lp.height = (int) ((float) lp.width / orgShareWidth * orgShareHeight);
		lp.rightMargin = (int) ((SCREEN_WIDTH - PIC_WIDTH) / 2 * 0.2f / 2);
		imgShare.setLayoutParams(lp);

		// 到目前为止，所有的控件已经摆好位置，但是中间内容区还没有设置内边距
		final int SCREEN_HEIGHT = getResources().getDisplayMetrics().heightPixels;
		titleLayout = (RelativeLayout) findViewById(R.id.title_layout);
		final int TITLE_HEIGHT = titleLayout.getLayoutParams().height;
		// 中间内容区占用的空间是
		final int CONTENT_HEIGHT = SCREEN_HEIGHT - TITLE_HEIGHT
				- GRID_ANSWER_HEIGHT;
		contentLayout = (LinearLayout) findViewById(R.id.content_layout);
		Log.d("内容区高度", "h=" + CONTENT_HEIGHT);
		// 开始计算内容区中每一个视图的高度
		int sumHeight = 0;
		// 内容区中有三个纵向视图，由下而上按照复杂程度排列
		// 1.ImageView 分割线
		imgDivider = (ImageView) contentLayout.findViewById(R.id.img_divider);
		llp = (android.widget.LinearLayout.LayoutParams) imgDivider
				.getLayoutParams();
		llp.width = (int) (SCREEN_WIDTH * 0.8f);
		imgDivider.setLayoutParams(llp);
		sumHeight += imgDivider.getDrawable().getIntrinsicHeight();
		Log.d("1/3", "h=" + sumHeight);
		// 2.GridView 已选择的答案
		sumHeight += gridCellWidth;
		Log.d("2/3", "h=" + sumHeight);
		// 3.RelativeLayout 这个最复杂，但是只要知道imgPic和textCate的高度就能计算
		sumHeight += PIC_WIDTH + CATE_HEIGHT / 2;
		Log.d("3/3", "h=" + sumHeight);

		if (sumHeight < CONTENT_HEIGHT) {
			// 说明还有空间可以用来设置内容区的内边距
			// ---------------
			// 边距=1个
			// RelativeLayout
			// 边距=1个
			// GridView
			// 边距=1个
			// ImageView
			// 边距=2个
			// ---------------
			// 均分的话，需要5个边距
			int margin = (CONTENT_HEIGHT - sumHeight) / 5;
			if (margin > 1) {
				// 开始由上而下设置外边距
				picLayout = (RelativeLayout) contentLayout
						.findViewById(R.id.pic_layout);
				llp = (android.widget.LinearLayout.LayoutParams) picLayout
						.getLayoutParams();
				llp.topMargin = margin;
				picLayout.setLayoutParams(llp);

				llp = (android.widget.LinearLayout.LayoutParams) gridSelected
						.getLayoutParams();
				llp.topMargin = margin;
				gridSelected.setLayoutParams(llp);

				llp = (android.widget.LinearLayout.LayoutParams) imgDivider
						.getLayoutParams();
				llp.topMargin = margin;
				// 这个下边距就不要设置了
				// llp.bottomMargin = margin * 2;
				imgDivider.setLayoutParams(llp);
			}
		}

		// 初始化答案区GridView的适配器
		answerAdapter = new AnswerAdapter(gridCellWidth);
		gridAnswer.setAdapter(answerAdapter);
		gridAnswer.setOnItemClickListener(this);

		selectedAdapter = new SelectedAdapter(gridCellWidth);
		gridSelected.setAdapter(selectedAdapter);
		gridSelected.setOnItemClickListener(this);

		// 初始化游戏
		gameState = GameState.loadState(getApplicationContext());
		initLevel(gameState.getLevel());

		// 初始化动画资源
		initAnim();
	}

	@Override
	protected void onDestroy() {
		// 保存游戏
		gameState.saveState(getApplicationContext());
		// 清理声音池
		SoundUtil.release();
		super.onDestroy();
	}

	public class AnswerState {
		/** 被用户选择了？ */
		public boolean isSelected;
		/** 答案的文字 */
		public String text;
		/** 被去掉一个错误答案功能设置 */
		public boolean isIncorrect;
		/** 被添加一个正确答案功能设置 */
		public boolean isCorrect;
		/** 是正确答案中的第几个字 */
		public int correctIndex;
		/** 选择的位置 */
		public int selectedIndex;

		@Override
		public String toString() {
			return "AnswerState [text=" + text + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((text == null) ? 0 : text.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AnswerState other = (AnswerState) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (text == null) {
				if (other.text != null)
					return false;
			} else if (!text.equals(other.text))
				return false;
			return true;
		}

		private MainActivity getOuterType() {
			return MainActivity.this;
		}

	}

	private class AnswerAdapter extends BaseAdapter {
		private int cellWidth;
		private List<AnswerState> answers = new ArrayList<AnswerState>();
		// 去掉的迷惑答案的字数
		private int removedCount = 0;
		// 添加的正确答案的字数
		private int addedCount = 0;

		/**
		 * 去掉一个错误答案
		 */
		public void removeIncorrectAnswer() {
			final String answer = currentQuestion.getAnswer();
			if (removedCount + answer.length() * 2 == 24) {
				if (useLimitToast != null) {
					useLimitToast.cancel();
					useLimitToast = null;
				}
				useLimitToast = Toast.makeText(getApplicationContext(),
						"已经到达使用上限", Toast.LENGTH_SHORT);
				useLimitToast.show();
			} else {
				gameState.addCoin(-REMOVE_COIN);
				setCoin(gameState.getCoin());

				final String mistake = currentQuestion.getMistake();
				HashSet<AnswerState> temp = new HashSet<AnswerState>(24);
				// 先把所有没去掉的错误答案放到一个Set里
				for (AnswerState as : answers) {
					if (!as.isIncorrect && mistake.contains(as.text)) {
						temp.add(as);
					}
				}
				// 再把第一个拿出来去掉
				for (AnswerState as : temp) {
					as.isIncorrect = true;
					removedCount++;
					break;
				}
				temp.clear();
				temp = null;
			}
		}

		/**
		 * 添加一个正确答案
		 */
		public void addCorrectAnswer() {
			final String answer = currentQuestion.getAnswer();
			// 最后一个字就不提示了
			if (addedCount >= answer.length() - 1) {
				if (useLimitToast != null) {
					useLimitToast.cancel();
					useLimitToast = null;
				}
				useLimitToast = Toast.makeText(getApplicationContext(),
						"已经到达使用上限", Toast.LENGTH_SHORT);
				useLimitToast.show();
			} else {
				gameState.addCoin(-ADD_COIN);
				setCoin(gameState.getCoin());
				HashSet<AnswerState> temp = new HashSet<AnswerState>(
						answer.length());
				for (AnswerState as : answers) {
					if (!as.isCorrect && answer.contains(as.text)) {
						temp.add(as);
					}
				}

				for (AnswerState as : temp) {
					as.isCorrect = true;
					if (as.isSelected) {
						// 如果已经在上面，但是位置不对，那么应该把这个字符拿下来
						selectedAdapter.unselect(as.selectedIndex);
					}
					if (selectedAdapter.getItem(as.correctIndex) != null) {
						// 如果正确字符应该出现的位置上已经有了字符
						selectedAdapter.unselect(as.correctIndex);
					}
					as.isSelected = true;
					addedCount++;
					selectedAdapter.select(as.correctIndex, as);
					break;
				}
				temp.clear();
				temp = null;
			}
		}

		public AnswerAdapter(int cellWidth) {
			this.cellWidth = cellWidth;
		}

		public void addAnswer(AnswerState as) {
			answers.add(as);
		}

		public void clear() {
			answers.clear();
			removedCount = 0;
			addedCount = 0;
		}

		@Override
		public int getCount() {
			return 24;
		}

		@Override
		public AnswerState getItem(int position) {
			if (position < 0 || position > 24 || position > answers.size() - 1) {
				return null;
			}
			return answers.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv = null;
			if (convertView == null) {
				tv = new TextView(getApplicationContext());
				AbsListView.LayoutParams lp = new android.widget.AbsListView.LayoutParams(
						cellWidth, cellWidth);
				tv.setLayoutParams(lp);
				tv.setBackgroundResource(R.drawable.button_characters);
				tv.setGravity(Gravity.CENTER);
				tv.setTextColor(Color.BLACK);
				tv.setTextSize(cellWidth * 0.45f);
				// 不启用系统默认音效
				tv.setSoundEffectsEnabled(false);
			} else {
				tv = (TextView) convertView;
			}

			AnswerState as = getItem(position);
			if (as != null) {
				tv.setText(as.text);
				tv.setVisibility(as.isSelected || as.isIncorrect
						|| as.isCorrect ? View.INVISIBLE : View.VISIBLE);
				tv.setTag(as);
			}

			return tv;
		}
	}

	private class SelectedAdapter extends BaseAdapter {
		private SparseArray<AnswerState> selected = new SparseArray<AnswerState>();
		private int cellWidth;
		private String answer;

		public SelectedAdapter(int cellWidth) {
			this.cellWidth = cellWidth;
		}

		@Override
		public int getCount() {
			return answer == null ? 0 : answer.length();
		}

		public int getCellWidth() {
			return cellWidth;
		}

		public void select(AnswerState as) {
			for (int i = 0; i < getCount(); i++) {
				if (selected.get(i) == null) {
					select(i, as);
					break;
				}
			}
		}

		public void select(int position, AnswerState as) {
			selected.put(position, as);
			as.selectedIndex = position;
		}

		public void clear() {
			selected.clear();
		}

		public void unselect(int position) {
			getItem(position).isSelected = false;
			selected.delete(position);
		}

		public int getSelectedCount() {
			return selected.size();
		}

		public void setAnswer(String answer) {
			this.answer = answer;
		}

		public String getSelectedAnswer() {
			StringBuilder sb = new StringBuilder(getCount());
			for (int i = 0; i < selected.size(); i++) {
				if (selected.get(i) != null) {
					sb.append(selected.get(i).text);
				}
			}
			return sb.toString();
		}

		@Override
		public AnswerState getItem(int position) {
			return selected.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv = null;
			if (convertView == null) {
				tv = new TextView(getApplicationContext());
				android.widget.AbsListView.LayoutParams lp = new android.widget.AbsListView.LayoutParams(
						cellWidth, cellWidth);
				tv.setLayoutParams(lp);
				tv.setTextSize(cellWidth * 0.5f);
				tv.setGravity(Gravity.CENTER);
				tv.setSoundEffectsEnabled(false);
			} else {
				tv = (TextView) convertView;
			}
			tv.setTextColor(Color.WHITE);
			AnswerState as = getItem(position);
			if (as != null) {
				tv.setText(as.text);
				if (as.isCorrect) {
					tv.setBackgroundResource(0);
				} else {
					tv.setBackgroundResource(R.drawable.blank);
				}
				if (isAnswerIncorrect && !as.isCorrect) {
					tv.setTextColor(Color.RED);
				}
			} else {
				tv.setText("");
				tv.setBackgroundResource(R.drawable.blank);
			}
			return tv;
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.img_remove_answer:
			if (gameState.getCoin() >= REMOVE_COIN) {
				answerAdapter.removeIncorrectAnswer();
				answerAdapter.notifyDataSetChanged();
			} else {
				if (coinLeakToast != null) {
					coinLeakToast.cancel();
					coinLeakToast = null;
				}
				coinLeakToast = Toast.makeText(getApplicationContext(),
						"您的金币数量不足", Toast.LENGTH_SHORT);
				coinLeakToast.show();
			}
			break;
		case R.id.img_add_answer:
			if (gameState.getCoin() >= ADD_COIN) {

				answerAdapter.addCorrectAnswer();
				answerAdapter.notifyDataSetChanged();
				selectedAdapter.notifyDataSetChanged();
				checkAnswer();
			} else {
				if (coinLeakToast != null) {
					coinLeakToast.cancel();
					coinLeakToast = null;
				}
				coinLeakToast = Toast.makeText(getApplicationContext(),
						"您的金币数量不足", Toast.LENGTH_SHORT);
				coinLeakToast.show();
			}
			break;
		case R.id.text_coin:
			playSound("coin.wav");
			if (addCoinToast != null) {
				addCoinToast.cancel();
				addCoinToast = null;
			}
			addCoinToast = Toast.makeText(getApplicationContext(),
					"开发者的福利，需要理由吗？", Toast.LENGTH_SHORT);
			addCoinToast.show();
			gameState.addCoin(1000);
			setCoin(gameState.getCoin());
			break;
		}
	}

	/**
	 * 设置分类文字
	 * 
	 * @param cate
	 */
	private void setCate(CharSequence cate) {
		if (textCate != null) {
			textCate.setText(cate);
		}
	}

	/**
	 * 设置金币数量
	 * 
	 * @param coin
	 */
	private void setCoin(int coin) {
		if (textCoin != null) {
			textCoin.setText(String.valueOf(coin));
		}
	}

	/**
	 * 设置关卡数
	 * 
	 * @param level
	 */
	private void setLevel(int level) {
		if (textLevel != null) {
			textLevel.setText(String.valueOf(level));
		}
	}

	/**
	 * 设置答案适配器
	 * 
	 * @param question
	 */
	private void setAnswerAdapter(Question question) {
		if (answerAdapter != null) {
			answerAdapter.clear();
			// 先混合答案和错误答案
			// String allStr = mixAnswer(question.getAnswer(),
			// question.getMistake());
			List<AnswerState> answer = new LinkedList<AnswerState>();
			final String answerStr = question.getAnswer();
			for (int i = 0; i < answerStr.length(); i++) {
				AnswerState as = new AnswerState();
				as.isSelected = false;
				as.correctIndex = i;
				as.text = answerStr.substring(i, i + 1);
				answer.add(as);
			}
			List<AnswerState> mistake = new LinkedList<AnswerState>();
			final String mistakeStr = question.getMistake();
			for (int i = 0; i < mistakeStr.length(); i++) {
				AnswerState as = new AnswerState();
				as.isSelected = false;
				as.text = mistakeStr.substring(i, i + 1);
				mistake.add(as);
			}

			List<AnswerState> allAns = mixAnswer(answer, mistake);
			answer.clear();
			answer = null;
			mistake.clear();
			mistake = null;

			for (AnswerState as : allAns) {
				answerAdapter.addAnswer(as);
			}
			answerAdapter.notifyDataSetChanged();
		}
	}

	/**
	 * 混合正确答案和错误答案
	 * 
	 * @param answer
	 * @param mistake
	 * @return
	 */

	private List<AnswerState> mixAnswer(List<AnswerState> answer,
			List<AnswerState> mistake) {
		// 先把两个列表连在一起
		answer.addAll(mistake);
		List<AnswerState> temp = new ArrayList<AnswerState>();
		int index = -1;
		while (answer.size() > 0) {
			index = rnd.nextInt(answer.size());
			temp.add(answer.get(index));
			answer.remove(index);
		}
		return temp;
	}

	/**
	 * 用来格式化题目图片的文件名
	 */
	private static DecimalFormat df = new DecimalFormat("__00000.png");
	/** 用来引用当前题目的图片 */
	private Bitmap currentBitmap = null;

	/**
	 * 设置题目图片
	 * 
	 * @param level
	 */
	private void setQuestionImage(int level) {
		if (imgPic != null) {
			imgPic.setImageBitmap(null);
			if (currentBitmap != null) {
				currentBitmap.recycle();
				currentBitmap = null;
			}
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inPurgeable = true;
			opts.inInputShareable = true;
			try {
				currentBitmap = BitmapFactory.decodeStream(
						getAssets().open("image/" + df.format(level-1)), null,
						opts);
				imgPic.setImageBitmap(currentBitmap);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void setQuestionImageByPath(String path) {
		if (imgPic != null) {
			imgPic.setImageBitmap(null);
			if (currentBitmap != null) {
				currentBitmap.recycle();
				currentBitmap = null;
			}
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inPurgeable = true;
			opts.inInputShareable = true;
			currentBitmap = BitmapFactory.decodeFile(path, opts);
			imgPic.setImageBitmap(currentBitmap);
		}
	}

	/**
	 * 设置已选择答案适配器
	 * 
	 * @param answer
	 */
	private void setSelectedAdapter(String answer) {
		if (selectedAdapter != null) {
			selectedAdapter.clear();
			selectedAdapter.setAnswer(answer);
			selectedAdapter.notifyDataSetChanged();
		}
		if (gridSelected != null) {
			LinearLayout.LayoutParams llp = (android.widget.LinearLayout.LayoutParams) gridSelected
					.getLayoutParams();
			llp.width = selectedAdapter.getCellWidth() * answer.length();
			gridSelected.setLayoutParams(llp);
			gridSelected.setNumColumns(answer.length());
		}

	}

	/**
	 * 根据关卡数，设置当前游戏界面
	 * 
	 * @param level
	 */
	public void initLevel(int level) {
		try {
			currentQuestion = Question.getQuestion(getApplicationContext(),
					level);
			setCoin(gameState.getCoin());
			setLevel(level);
			setAnswerAdapter(currentQuestion);
			setSelectedAdapter(currentQuestion.getAnswer());
			setCate(currentQuestion.getCate());
			if (TextUtils.isEmpty(currentQuestion.getPicPath())) {
				setQuestionImage(level);
			} else {
				setQuestionImageByPath(currentQuestion.getPicPath());
			}
		} catch (IllegalArgumentException e) {
			Toast.makeText(getApplicationContext(), "关卡不存在", Toast.LENGTH_SHORT)
					.show();
			// 回到上一个可用关
			initLevel(--level);
			gameState.setLevel(level);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		switch (parent.getId()) {
		case R.id.grid_answer:
			// 播放音效
			playSound("enter.wav");
			// 播放动画
			buttonScaleListener.setView(view);
			view.startAnimation(buttonScale);
			break;
		case R.id.grid_selected:
			AnswerState sas = (AnswerState) selectedAdapter.getItem(position);
			if (sas != null) {
				if (sas.isCorrect) {
					return;
				}
				isAnswerIncorrect = false;
				playSound("cancel.wav");
				selectedAdapter.unselect(position);
				answerAdapter.notifyDataSetChanged();
				selectedAdapter.notifyDataSetChanged();
			}
			break;
		}
	}

	/** 按钮缩放动画 */
	private Animation buttonScale;

	/**
	 * 初始化动画资源
	 */
	private void initAnim() {
		buttonScale = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.button_scale);

		// buttonScale = new ScaleAnimation(0.85f, 1.15f, 0.85f, 1.15f,
		// ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
		// ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
		// buttonScale.setDuration(50);

		buttonScale
				.setAnimationListener(buttonScaleListener = new ButtonScaleListener());
	}

	private ButtonScaleListener buttonScaleListener;

	private class ButtonScaleListener implements AnimationListener {
		// 执行动画的视图
		private View v;

		public void setView(View v) {
			this.v = v;
		}

		@Override
		public void onAnimationStart(Animation animation) {

		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (v != null) {
				onAnswerClicked(v);
				v = null;
			}
		}

		@Override
		public void onAnimationRepeat(Animation animation) {

		}

	}

	/**
	 * 当答案按钮被点击时
	 * 
	 * @param view
	 */
	private void onAnswerClicked(View view) {
		AnswerState as = (AnswerState) view.getTag();
		if (as != null
				&& selectedAdapter.getSelectedCount() < currentQuestion
						.getAnswer().length()) {
			as.isSelected = true;
			selectedAdapter.select(as);

			if (selectedAdapter.getSelectedCount() == currentQuestion
					.getAnswer().length()) {
				isAnswerIncorrect = !checkAnswer();
			}
			selectedAdapter.notifyDataSetChanged();
			answerAdapter.notifyDataSetChanged();
		}
	}

	private boolean checkAnswer() {
		boolean flag = currentQuestion.getAnswer().equals(
				selectedAdapter.getSelectedAnswer());
		if (flag) {
			// TODO 判断正误
			// 是应该进入下一关，还是整个游戏都通关?
			gameState.nextLevel();
			gameState.addCoin(currentQuestion.getCoin());
			playSound("coin.wav");
			initLevel(gameState.getLevel());
		}
		return flag;
	}

	/** 声音文件路径的前缀 */
	private static final String SOUND_PREFIX = "sound/";

	/**
	 * 播放音效
	 * 
	 * @param soundName
	 */
	private void playSound(String soundName) {
		SoundUtil.play(getApplicationContext(), SOUND_PREFIX + soundName);
	}
}
