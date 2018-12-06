package com.z012.chengdu.sc.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.prj.sdk.app.AppContext;
import com.z012.chengdu.sc.R;
import com.z012.chengdu.sc.constants.AppConst;
import com.z012.chengdu.sc.constants.NetURL;
import com.z012.chengdu.sc.ui.base.BaseActivity;

/**
 * 关于
 * 
 * @author LiaoBo
 */
public class AboutActivity extends BaseActivity {
	private Button btnAbout, btn_develop;
	private TextView tv_version;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_about_act);
		initViews();
		initParams();
		initListeners();

	}

	@Override
	public void initViews() {
		super.initViews();
		btnAbout = (Button) findViewById(R.id.btn_about);
		tv_version = (TextView) findViewById(R.id.tv_version);
		tv_center_title.setText("关于");
		tv_right_title.setVisibility(View.GONE);
		btn_develop = (Button) findViewById(R.id.btn_develop);
		if (AppConst.ISDEVELOP) {
			btn_develop.setVisibility(View.VISIBLE);
		} else {
			btn_develop.setVisibility(View.GONE);
		}
	}

	@Override
	public void initParams() {
		super.initParams();
		StringBuilder sb = new StringBuilder();
		sb.append(AppContext.getVersion());
		if (AppConst.ISDEVELOP) {
			sb.append(" 渠道名：").append(
					AppContext.getAppMetaData(this, "UMENG_CHANNEL"));
		}
		tv_version.setText(sb);// 设置版本
	}

	@Override
	public void initListeners() {
		super.initListeners();
		btnAbout.setOnClickListener(this);
		btn_develop.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);
		Intent mIntent = null;
		switch (v.getId()) {
		case R.id.btn_about:
			mIntent = new Intent(this, WebViewActivity.class);
			mIntent.putExtra("path", NetURL.ABOUT_URL);
			mIntent.putExtra("title", "关于我们");
			startActivity(mIntent);
			break;
		case R.id.btn_develop:
			mIntent = new Intent(this, HtmlActivity.class);
			mIntent.putExtra("ISDEVELOP", AppConst.ISDEVELOP);
			startActivity(mIntent);
			break;
		default:
			break;
		}
	}
}
