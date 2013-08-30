package com.newandbie.fake.pic2word.editor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.newandbie.fake.pic2word.MyDbHelper;
import com.newandbie.fake.pic2word.R;

public class EditorActivity extends Activity implements OnClickListener {
	private ImageView imgPic;
	private TextView textCamera;
	private TextView textMediaStore;
	private EditText editAnswer;
	private EditText editMistake;
	private TextView textCreate;
	private File customFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_editor);

		imgPic = (ImageView) findViewById(R.id.img_pic);
		textCamera = (TextView) findViewById(R.id.text_camera);
		textCamera.setOnClickListener(this);
		textMediaStore = (TextView) findViewById(R.id.text_mediastore);
		textMediaStore.setOnClickListener(this);
		editAnswer = (EditText) findViewById(R.id.edit_answer);
		editMistake = (EditText) findViewById(R.id.edit_mistake);
		textCreate = (TextView) findViewById(R.id.text_create);
		textCreate.setOnClickListener(this);

		// 开始动态调整布局
		final int SCREEN_WIDTH = getResources().getDisplayMetrics().widthPixels;

		android.widget.LinearLayout.LayoutParams llp = (android.widget.LinearLayout.LayoutParams) imgPic
				.getLayoutParams();
		llp.width = llp.height = (int) (SCREEN_WIDTH * 0.6f);
		imgPic.setLayoutParams(llp);
	}

	private final int REQ_CAMERA = 1;
	private final int REQ_MEDIASTORE = 2;
	private final int REQ_CROP = 3;
	private final String TEMP_PIC = "temp";

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.text_camera:
			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			// 为了避免相机bug，指定拍照后的图片保存地址
			// 这个地址必须是所有程序都能访问的
			File saveFile = new File(
					Environment
							.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
					TEMP_PIC);
			if (!saveFile.getParentFile().isDirectory()) {
				saveFile.getParentFile().mkdirs();
			}
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(saveFile));
			startActivityForResult(cameraIntent, REQ_CAMERA);
			break;
		case R.id.text_mediastore:
			Intent msIntent = new Intent(Intent.ACTION_PICK);
			msIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(msIntent, REQ_MEDIASTORE);
			break;
		case R.id.text_create:
			final String answer = editAnswer.getText().toString().trim();
			if (TextUtils.isEmpty(answer)) {
				editAnswer.setError("正确答案不能为空");
				return;
			}
			if (answer.length() > 8) {
				Toast.makeText(getApplicationContext(), "正确答案的字数不能超过8",
						Toast.LENGTH_SHORT).show();
				return;
			}
			final String mistake = editMistake.getText().toString().trim();
			if (TextUtils.isEmpty(mistake)) {
				editMistake.setError("迷惑性答案不能为空");
				return;
			} else if (mistake.length() < 24 - answer.length()) {
				editMistake.setError("迷惑性答案字数不足");
				return;
			}
			for (int i = 0; i < answer.length(); i++) {
				if (mistake.contains(answer.substring(i, i + 1))) {
					// 检查正确答案里的字有没有出现在迷惑答案中
					Toast.makeText(getApplicationContext(), "正确答案和迷惑答案的文字有重复",
							Toast.LENGTH_SHORT).show();
					return;
				}
			}
			int level = createLevel(answer,
					mistake.substring(0, 24 - answer.length()),
					customFile != null ? customFile.getAbsolutePath() : "");
			Toast.makeText(getApplicationContext(), "自定义关卡：" + level + " 创建成功",
					Toast.LENGTH_SHORT).show();
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQ_CAMERA:
				if (!crop(Uri
						.fromFile(new File(
								Environment
										.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
								TEMP_PIC)))) {
					// 不支持裁剪，那么就对图片进行采样处理
					imgPic.setImageBitmap(samplePictureIfNeeded(new File(
							Environment
									.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
							TEMP_PIC)));
				}
				break;
			case REQ_MEDIASTORE:
				if (!crop(data.getData())) {
					// 不支持裁剪，那么就对图片进行采样处理
					// 先拿到图片路径
					Cursor cursor = MediaStore.Images.Media
							.query(getContentResolver(),
									data.getData(),
									new String[] { MediaStore.Images.ImageColumns.DATA });
					if (cursor != null && cursor.moveToNext()) {
						final int pathIndex = cursor
								.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
						String path = cursor.getString(pathIndex);
						cursor.close();
						imgPic.setImageBitmap(samplePictureIfNeeded(new File(
								path)));
					}
				}
				break;
			case REQ_CROP:
				Bundle extras = data.getExtras();
				if (extras != null) {
					Bitmap bmp = extras.getParcelable("data");
					imgPic.setImageBitmap(bmp);
					saveCustom(bmp);
				}
				File f = new File(
						Environment
								.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
						TEMP_PIC);
				if (f.exists()) {
					f.delete();
				}
				break;
			}
		}
	}

	/**
	 * 对图片文件进行采样，如果需要采样，采样后会删除原图
	 * 
	 * @param picFile
	 * @return 返回采样后的图片；或者因为各种失败，返回null
	 */
	private Bitmap samplePictureIfNeeded(File picFile) {
		if (picFile != null && picFile.isFile() && picFile.canRead()) {
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			// 第一次解码，只得到图片大小
			BitmapFactory.decodeFile(picFile.getAbsolutePath(), opts);
			// 屏幕宽度
			final int SCREEN_WIDTH = getResources().getDisplayMetrics().widthPixels;
			if (opts.outWidth > SCREEN_WIDTH) {
				opts.inSampleSize = opts.outWidth / SCREEN_WIDTH;
			}
			// 第二次解码
			opts.inJustDecodeBounds = false;
			Bitmap bm = BitmapFactory.decodeFile(picFile.getAbsolutePath(),
					opts);
			if (bm != null) {
				picFile.delete();
				saveCustom(bm);
				return bm;
			}
		}
		return null;
	}

	/**
	 * 保存自定义关卡的图片
	 * 
	 * @param bm
	 */
	private void saveCustom(Bitmap bm) {
		FileOutputStream out = null;
		customFile = new File(getFilesDir(), "custom"
				+ System.currentTimeMillis());
		try {
			bm.compress(CompressFormat.PNG, 100, new FileOutputStream(
					customFile));
		} catch (FileNotFoundException e) {
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

	private boolean crop(Uri u) {
		// 照相完成，先对图片进行剪裁处理
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setType("image/*");
		// 用包管理器查询有没有Activity能够支持这条意图
		List<ResolveInfo> list = getPackageManager().queryIntentActivities(
				intent, 0);
		if (list.size() > 0) {
			// bug
			if (list.size() == 1) {
				// 在某些机型上，相机的裁剪Activity可能是不公开的
				// 所以要用显示意图
				intent.setComponent(new ComponentName(
						list.get(0).activityInfo.packageName,
						list.get(0).activityInfo.name));
			}
			// 对裁剪的设置
			intent.setData(u);
			final int OUT_SIZE = (int) (getResources().getDisplayMetrics().widthPixels * 0.6f);
			intent.putExtra("outputX", OUT_SIZE);
			intent.putExtra("outputY", OUT_SIZE);
			intent.putExtra("aspectX", 1);
			intent.putExtra("aspectY", 1);
			intent.putExtra("scale", true);
			intent.putExtra("return-data", true);
			startActivityForResult(intent, REQ_CROP);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 创建自定义关卡
	 * 
	 * @param answer
	 * @param mistake
	 * @return
	 */
	private int createLevel(String answer, String mistake, String picPath) {
		int level = 1;
		MyDbHelper helper = new MyDbHelper(getApplicationContext());
		SQLiteDatabase wdb = helper.getWritableDatabase();
		// 先从数据库中读最后一行的level
		Cursor cursor = wdb.query(MyDbHelper.TABLE, new String[] {
				"MAX(" + MyDbHelper._ID + ")", MyDbHelper.LEVEL }, null, null,
				null, null, null);
		if (cursor != null && cursor.moveToNext()) {
			final int levelIndex = cursor.getColumnIndex(MyDbHelper.LEVEL);
			level = cursor.getInt(levelIndex);
			ContentValues values = new ContentValues();
			values.put(MyDbHelper.LEVEL, ++level);
			values.put(MyDbHelper.ANSWER, answer);
			values.put(MyDbHelper.MISTAKE, mistake);
			values.put(MyDbHelper.COIN, 3);
			values.put(MyDbHelper.CATE, "自定义");
			values.put(MyDbHelper.PIC, picPath);
			wdb.insert(MyDbHelper.TABLE, null, values);
			cursor.close();
		}
		wdb.close();
		return level;
	}
}
