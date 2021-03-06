package com.z012.chengdu.sc.ui.activity.user;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.prj.sdk.app.AppContext;
import com.prj.sdk.net.bean.ResponseData;
import com.prj.sdk.net.data.DataCallback;
import com.prj.sdk.net.data.DataLoader;
import com.prj.sdk.util.DateUtil;
import com.prj.sdk.util.LogUtil;
import com.prj.sdk.util.SharedPreferenceUtil;
import com.prj.sdk.util.StringUtil;
import com.prj.sdk.util.ToastUtil;
import com.prj.sdk.util.Utils;
import com.umeng.analytics.MobclickAgent;
import com.z012.chengdu.sc.R;
import com.z012.chengdu.sc.SessionContext;
import com.z012.chengdu.sc.constants.AppConst;
import com.z012.chengdu.sc.constants.NetURL;
import com.prj.sdk.algo.SHA1;
import com.z012.chengdu.sc.entity.WebInfoEntity;
import com.z012.chengdu.sc.net.request.RequestBeanBuilder;
import com.z012.chengdu.sc.net.entity.UserInfo;
import com.z012.chengdu.sc.ui.BaseActivity;
import com.z012.chengdu.sc.ui.activity.WebViewActivity;

import java.net.ConnectException;
import java.util.HashMap;

import cn.jpush.android.api.JPushInterface;

/**
 * 注册
 *
 * @author LiaoBo
 */
public class RegisterActivity extends BaseActivity implements DataCallback,
        DialogInterface.OnCancelListener {
    private EditText et_yzm, et_phone, et_password;
    private Button btn_register, btn_getYZM;
    private CheckBox checkBox;
    private String mPhoneNum;
    private TextView tv_agreement;
    private CountDownTimer mCountDownTimer;

    @Override
    protected int getLayoutResId() {
        return R.layout.ui_register_act;
    }

    @Override
    public void initParams() {
        super.initParams();
        et_yzm = (EditText) findViewById(R.id.et_yzm);
        btn_register = (Button) findViewById(R.id.btn_register);
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        et_phone = (EditText) findViewById(R.id.et_phone);
        btn_getYZM = (Button) findViewById(R.id.btn_getYZM);
        et_password = (EditText) findViewById(R.id.et_password);
        tv_agreement = (TextView) findViewById(R.id.tv_agreement);

        setCountDownTimer(60 * 1000, 1000);
    }

    @Override
    public void initListeners() {
        super.initListeners();
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox.isChecked()) {
                    loadData();
                } else {
                    ToastUtil.show("请先阅读《注册协议》", 0);
                }
            }
        });

        btn_getYZM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhoneNum = et_phone.getText().toString().trim();
                if (StringUtil.notEmpty(mPhoneNum)) {
                    if (Utils.isMobile(mPhoneNum)) {
                        CheckPhoneNumber();
                    } else {
                        ToastUtil.show("请输入正确的手机号", 0);
                    }
                } else {
                    ToastUtil.show("请输入手机号", 0);
                }
            }
        });

        tv_agreement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, WebViewActivity.class);
                intent.putExtra("webEntity", new WebInfoEntity("注册协议", NetURL.REGISTER_URL));
                startActivity(intent);
            }
        });
    }

    /**
     * 检测手机号是否已经注册
     */
    public void CheckPhoneNumber() {

        RequestBeanBuilder builder = RequestBeanBuilder.create(false);
        builder.addBody("MOBILENUM", mPhoneNum);

        ResponseData data = builder.syncRequest(builder);
        data.path = NetURL.CHECK_PHONE;
        data.flag = 10;

        if (!isProgressShowing()) {
            showProgressDialog(getString(R.string.loading), true);
        }
        requestID = DataLoader.getInstance().loadData(this, data);
    }

    /**
     * 加载验证码
     */
    private void loadYZM() {
        // String senderId =
        // SharedPreferenceUtil.getInstance().getString(AppConst.CITY_SENDER_ID,
        // "", false);

        RequestBeanBuilder builder = RequestBeanBuilder.create(false);
        builder.addBody("BUSINESSTYPE", "40658");// 业务类型01手机绑定；02邮箱绑定；03注册绑定手机；04找回密码；05市民卡注册绑定；06市民卡实名认证
        builder.addBody("MOBILENUM", mPhoneNum);
        // builder.addBody("VALID", "10");// 有效期(分钟)
        // builder.addBody("appId", "cdportal");// 暂时固定 TODO
        // builder.addBody("senderId", senderId);

        ResponseData data = builder.syncRequest(builder);
        data.path = NetURL.GET_YZM;
        data.flag = 1;

        requestID = DataLoader.getInstance().loadData(this, data);
    }

    /**
     * 加载数据
     */
    private void loadData() {
        String MOBILENUM = et_phone.getText().toString().trim();
        String CODE = et_yzm.getText().toString().trim();
        String PASSWORD = et_password.getText().toString().trim();

        if (StringUtil.empty(MOBILENUM)) {
            ToastUtil.show("请输入手机号码", 0);
            return;
        }
        if (!Utils.isMobile(MOBILENUM)) {
            ToastUtil.show("请输入正确的手机号码", 0);
            return;
        }
        if (StringUtil.empty(CODE)) {
            ToastUtil.show("请输入验证码", 0);
            return;
        }
        if (StringUtil.empty(PASSWORD)) {
            ToastUtil.show("密码不允许为空", 0);
            return;
        }
        if (PASSWORD.length() < 6) {
            ToastUtil.show("请输入6-20个字符的密码", 0);
            return;
        }
        RequestBeanBuilder builder = RequestBeanBuilder.create(false);
        builder.addBody("MOBILENUM", MOBILENUM);
        builder.addBody("CODE", CODE);
        builder.addBody("PWDSTRENGTH", "1");
        SHA1 sha1 = new SHA1();
        builder.addBody("PASSWORD", sha1.getDigestOfString(PASSWORD.getBytes()));

        ResponseData data = builder.syncRequest(builder);
        data.path = NetURL.REGISTER;
        data.flag = 2;

        if (!isProgressShowing()) {
            showProgressDialog(getString(R.string.present), true);
        }
        requestID = DataLoader.getInstance().loadData(this, data);
    }

    private void requestTicket() {
        String phoneNum = et_phone.getText().toString().trim();
        String password = et_password.getText().toString().trim();

        RequestBeanBuilder builder = RequestBeanBuilder.create(false);
        SHA1 sha1 = new SHA1();
        String pwd = sha1.getDigestOfString(password.getBytes());

        builder.addBody("LOGIN", phoneNum);
        builder.addBody("USER_PWD", pwd);

        ResponseData data = builder.syncRequest(builder);
        data.path = NetURL.GET_TICKET;
        data.flag = 0;

        requestID = DataLoader.getInstance().loadData(this, data);
    }

    private void requestUserInfo(String ticket) {
        RequestBeanBuilder builder = RequestBeanBuilder.create(true);

        ResponseData data = builder.syncRequest(builder);
        data.path = NetURL.GET_USER_INFO;
        data.flag = 3;
        requestID = DataLoader.getInstance().loadData(this, data);
    }

    @Override
    public void preExecute(ResponseData request) {

    }

    @Override
    public void notifyMessage(ResponseData request, ResponseData response)
            throws Exception {
        if (request.flag == 10) {
            if (response.body instanceof Boolean) {
                if ((Boolean) response.body) {
                    loadYZM();
                } else {
                    ToastUtil.show("你输入的手机号已被占用", 0);
                }
            }
        } else if (request.flag == 1) {
            removeProgressDialog();
            ToastUtil.show("验证码已发送，请稍候...", Toast.LENGTH_LONG);
            btn_getYZM.setEnabled(false);
            mCountDownTimer.start();// 启动倒计时
        } else if (request.flag == 2) {
            requestTicket();
        } else if (request.flag == 0) {
            JSONObject mJson = JSON.parseObject(response.body.toString());
            String ticket = mJson.getString("accessTicket");
            // 记录登录ticket
            SharedPreferenceUtil.getInstance().setString(AppConst.ACCESS_TICKET, ticket, true);
            SessionContext.setTicket(ticket);
            System.out.println("ticket = " + ticket);
            requestUserInfo(ticket);
        } else if (request.flag == 3) {
            removeProgressDialog();
            if (StringUtil.empty(response.body.toString()) || response.body.toString().equals("{}")) {
                ToastUtil.show("获取用户信息失败，请重试", 0);
                return;
            }
            SessionContext.mUser = JSON.parseObject(response.body.toString(), UserInfo.class);
            LogUtil.d("dw", "url = " + SessionContext.mUser.USERBASIC.headphotourl);
            LogUtil.d("dw", "url = " + SessionContext.mUser.USERBASIC.getHeadphotourl());
            if (SessionContext.mUser == null || StringUtil.empty(SessionContext.mUser.USERBASIC)) {
                ToastUtil.show("获取用户信息失败，请重试", 0);
                return;
            }

            SharedPreferenceUtil.getInstance().setString(AppConst.USERNAME, et_phone.getText().toString(), true);// 保存用户名
            SharedPreferenceUtil.getInstance().setString(AppConst.LAST_LOGIN_DATE, DateUtil.getCurDateStr(null), false);// 保存登录时间
            if (StringUtil.notEmpty(SessionContext.mUser.USERBASIC.headphotourl)) {
                SharedPreferenceUtil.getInstance().setString(AppConst.USER_PHOTO_URL, SessionContext.mUser.USERBASIC.getHeadphotourl(), false);
            } else {
                SharedPreferenceUtil.getInstance().setString(AppConst.USER_PHOTO_URL, "", false);
            }
            SharedPreferenceUtil.getInstance().setString(AppConst.USER_INFO, response.body.toString(), true);
            ToastUtil.show("注册成功", 0);
            Intent mIntent = new Intent(AppConst.ACTION_DYNAMIC_USER_INFO);
            LocalBroadcastManager.getInstance(AppContext.mMainContext).sendBroadcast(mIntent);
            JPushInterface.setAlias(AppContext.mMainContext, SessionContext.mUser.USERAUTH.mobilenum, null);
            finish();
            // 添加友盟自定义事件
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("userId", SessionContext.mUser.USERBASIC.id);
            MobclickAgent.onEvent(this, "UserLoginSuccess", map);
        }

    }

    @Override
    public void notifyError(ResponseData request, ResponseData response, Exception e) {
        removeProgressDialog();

        String message;
        if (e != null && e instanceof ConnectException) {
            message = getString(R.string.dialog_tip_net_error);
        } else {
            message = response != null && response.data != null ? response.data.toString() : getString(R.string.dialog_tip_null_error);
        }
        ToastUtil.show(message, Toast.LENGTH_LONG);

    }

    @Override
    public void onCancel(DialogInterface dialog) {
        DataLoader.getInstance().clear(requestID);
        removeProgressDialog();
    }

    /**
     * 设置倒计时
     *
     * @param millisInFuture
     * @param countDownInterval
     */
    private void setCountDownTimer(long millisInFuture, long countDownInterval) {
        mCountDownTimer = new CountDownTimer(millisInFuture, countDownInterval) {

            @Override
            public void onTick(long millisUntilFinished) {
                btn_getYZM.setText((millisUntilFinished / 1000) + " s");
            }

            @Override
            public void onFinish() {
                btn_getYZM.setEnabled(true);
                btn_getYZM.setText("获取验证码");
            }
        };
    }
}
