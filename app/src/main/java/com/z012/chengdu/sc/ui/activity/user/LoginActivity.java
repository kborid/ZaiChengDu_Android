package com.z012.chengdu.sc.ui.activity.user;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.prj.sdk.util.SystemUtil;
import com.prj.sdk.util.ToastUtil;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.umeng.analytics.MobclickAgent;
import com.umeng.socialize.bean.HandlerRequestCode;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.bean.SocializeEntity;
import com.umeng.socialize.bean.StatusCode;
import com.umeng.socialize.controller.UMServiceFactory;
import com.umeng.socialize.controller.UMSocialService;
import com.umeng.socialize.controller.listener.SocializeListeners.SocializeClientListener;
import com.umeng.socialize.controller.listener.SocializeListeners.UMAuthListener;
import com.umeng.socialize.controller.listener.SocializeListeners.UMDataListener;
import com.umeng.socialize.exception.SocializeException;
import com.umeng.socialize.sso.SinaSsoHandler;
import com.umeng.socialize.sso.UMQQSsoHandler;
import com.umeng.socialize.sso.UMSsoHandler;
import com.umeng.socialize.weixin.controller.UMWXHandler;
import com.z012.chengdu.sc.R;
import com.z012.chengdu.sc.SessionContext;
import com.z012.chengdu.sc.constants.AppConst;
import com.z012.chengdu.sc.constants.NetURL;
import com.prj.sdk.algo.SHA1;
import com.z012.chengdu.sc.net.entity.CertUserAuth;
import com.z012.chengdu.sc.net.entity.UserInfo;
import com.z012.chengdu.sc.net.request.RequestBeanBuilder;
import com.z012.chengdu.sc.ui.BaseActivity;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import cn.jpush.android.api.JPushInterface;

/**
 * 登录
 *
 * @author LiaoBo
 */
public class LoginActivity extends BaseActivity implements DataCallback,
        DialogInterface.OnCancelListener, OnCheckedChangeListener {

    @BindView(R.id.login_phone_ed)
    EditText et_phone;
    @BindView(R.id.login_pwd)
    EditText et_pwd;
    @BindView(R.id.iv_qq)
    ImageView iv_qq;
    @BindView(R.id.iv_sina)
    ImageView iv_sina;
    @BindView(R.id.iv_weixin)
    ImageView iv_weixin;

    private String phoneStr, password;
    private static onCancelLoginListener mCancelLogin;
    // 整个平台的Controller, 负责管理整个SDK的配置、操作等处理
    private UMSocialService mController = UMServiceFactory.getUMSocialService("com.umeng.login");
    private String usertoken;
    private String mPlatform; // （01-新浪微博，02-腾讯QQ，03-微信，04-支付宝）
    private String thirdpartusername, thirdpartuserheadphotourl, openid, unionid;

    @Override
    protected int getLayoutResId() {
        return R.layout.ui_login;
    }

    @Override
    public void initParams() {
        super.initParams();
        SessionContext.cleanUserInfo();
        String name = SharedPreferenceUtil.getInstance().getString(AppConst.USERNAME, "", true);
        if (StringUtil.notEmpty(name)) {
            et_phone.setText(name);// 设置默认用户名
        }
        addQQQZonePlatform();
        addWXPlatform();
        // 设置新浪SSO handler
        mController.getConfig().setSsoHandler(new SinaSsoHandler());
    }

    @OnClick(R.id.tv_left_title)
    void left() {
        if (mCancelLogin != null) {
            mCancelLogin.isCancelLogin(true);
        }
        finish();
    }

    @OnClick(R.id.btn_login)
    void btnLogin() {
        loadData();
    }

    @OnClick(R.id.tv_reigster)
    void register() {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    @OnClick(R.id.tv_forget_pwd)
    void forgetPwd() {
        startActivity(new Intent(this, ForgetPwdActivity.class));
    }

    @OnClick(R.id.cb_cancel)
    void emptyText() {
        et_phone.setText("");
    }

    @OnClick(R.id.iv_qq)
    void qqLogin() {
        login(SHARE_MEDIA.QQ);
    }

    @OnClick(R.id.iv_weixin)
    void wxLogin() {
        if (WXAPIFactory.createWXAPI(this, null).isWXAppInstalled()) {
            login(SHARE_MEDIA.WEIXIN);
        } else {
            ToastUtil.show("没有安装微信", 0);
        }
    }

    @OnClick(R.id.iv_sina)
    void sinaLogin() {
        login(SHARE_MEDIA.SINA);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionContext.isLogin()) {
            this.finish();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            // 设置为明文显示
            et_pwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        } else {
            // 设置为密文显示
            et_pwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        et_pwd.setSelection(et_pwd.getText().length());// 设置光标位置
    }

    /**
     * 注销本次登录</br>
     */
    private void logout(final SHARE_MEDIA platform) {
        mController.deleteOauth(this, platform, new SocializeClientListener() {

            @Override
            public void onStart() {

            }

            @Override
            public void onComplete(int status, SocializeEntity entity) {
                String showText = "解除" + platform.toString() + "平台授权成功";
                if (status != StatusCode.ST_CODE_SUCCESSED) {
                    showText = "解除" + platform.toString() + "平台授权失败[" + status + "]";
                }
                ToastUtil.show(showText, Toast.LENGTH_SHORT);
            }
        });
    }

    /**
     * 授权。如果授权成功，则获取用户信息</br>
     */
    private void login(final SHARE_MEDIA platform) {
        mController.doOauthVerify(this, platform, new UMAuthListener() {

            @Override
            public void onStart(SHARE_MEDIA platform) {
                ToastUtil.show("授权开始", 0);
            }

            @Override
            public void onError(SocializeException e, SHARE_MEDIA platform) {
                ToastUtil.show("授权错误", Toast.LENGTH_SHORT);
            }

            @Override
            public void onComplete(Bundle value, SHARE_MEDIA platform) {
                String uid = value.getString("uid");
                openid = uid;
                unionid = value.getString("unionid");
                usertoken = value.getString("access_token");
                if (!TextUtils.isEmpty(uid)) {
                    getUserInfo(platform);
                } else {
                    ToastUtil.show("授权失败", Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void onCancel(SHARE_MEDIA platform) {
                ToastUtil.show("授权取消", Toast.LENGTH_SHORT);
            }
        });
    }

    /**
     * 获取授权平台的用户信息</br>
     */
    private void getUserInfo(final SHARE_MEDIA platform) {
        mController.getPlatformInfo(this, platform, new UMDataListener() {

            @Override
            public void onStart() {

            }

            @Override
            public void onComplete(int status, Map<String, Object> info) {
                if (status == StatusCode.ST_CODE_SUCCESSED && info != null) {
                    try {
                        if (platform == SHARE_MEDIA.SINA) { // 新浪微博
                            mPlatform = "01";
                            thirdpartuserheadphotourl = info.get("profile_image_url").toString();
                            thirdpartusername = info.get("screen_name").toString();
                            if (StringUtil.empty(usertoken)) {// 通过web方式登录，需要从用户信息中获取access_token
                                usertoken = info.get("access_token").toString();
                            }
                        } else if (platform == SHARE_MEDIA.QQ) { // QQ
                            mPlatform = "02";
                            thirdpartuserheadphotourl = info.get("profile_image_url").toString();
                            thirdpartusername = info.get("screen_name").toString();
                        } else if (platform == SHARE_MEDIA.WEIXIN) { // 微信
                            mPlatform = "03";
                            thirdpartuserheadphotourl = info.get("headimgurl").toString();
                            thirdpartusername = info.get("nickname").toString();
                            unionid = info.get("unionid").toString();
                        }

                        // StringBuilder sb = new StringBuilder();
                        // Set<String> keys = info.keySet();
                        // for (String key : keys) {
                        // sb.append(key + "=" + info.get(key).toString() +
                        // "\r\n");
                        // }
                        checkThirdLogin();
                    } catch (Exception e) {
                        ToastUtil.show("获取用户信息失败", Toast.LENGTH_SHORT);
                    }
                } else {
                    ToastUtil.show("获取用户信息失败", Toast.LENGTH_SHORT);
                }
            }
        });
    }

    /**
     * 添加qq及qq Zone平台
     */
    private void addQQQZonePlatform() {
        String appId = getString(R.string.qq_appid);
        String appKey = getString(R.string.qq_appkey);
        // 添加QQ支持, 并且设置QQ分享内容的target url
        UMQQSsoHandler qqSsoHandler = new UMQQSsoHandler(this, appId, appKey);
        // qqSsoHandler.setTargetUrl("http://www.umeng.com");
        qqSsoHandler.addToSocialSDK();
        // 添加QZone平台
        // QZoneSsoHandler qZoneSsoHandler = new QZoneSsoHandler(this, appId,
        // appKey);
        // qZoneSsoHandler.addToSocialSDK();
        if (!mController.getConfig().getSsoHandler(HandlerRequestCode.QQ_REQUEST_CODE).isClientInstalled()) {
            iv_qq.setVisibility(View.GONE);// 没有安装QQ，隐藏QQ
        }
    }

    /**
     * 添加微信平台配置
     */
    private void addWXPlatform() {
        if (!WXAPIFactory.createWXAPI(this, null).isWXAppInstalled()) {
            iv_weixin.setVisibility(View.GONE);// 没有安装微信，隐藏微信
            return;
        }
        String appId = getString(R.string.wx_appid);
        String appSecret = getString(R.string.wx_appsecret);
        // String appId = "wx967daebe835fbeac";
        // String appSecret = "5bb696d9ccd75a38c8a0bfe0675559b3";
        // 添加微信平台
        UMWXHandler wxHandler = new UMWXHandler(this, appId, appSecret);
        wxHandler.addToSocialSDK();
    }

    /**
     * 加载数据
     */
    private void loadData() {

        phoneStr = et_phone.getText().toString().trim();
        password = et_pwd.getText().toString().trim();
        if (StringUtil.empty(phoneStr)) {
            ToastUtil.show("用户名不能为空", Toast.LENGTH_SHORT);
            return;
        }
        if (StringUtil.empty(password)) {
            ToastUtil.show("密码不能为空", Toast.LENGTH_SHORT);
            return;
        }

        RequestBeanBuilder builder = RequestBeanBuilder.create(false);
        SHA1 sha1 = new SHA1();
        String pwd = sha1.getDigestOfString(password.getBytes());

        // builder.addBody("username", phoneStr);
        // builder.addBody("password", pwd);
        builder.addBody("LOGIN", phoneStr);
        builder.addBody("USER_PWD", pwd);

        ResponseData data = builder.syncRequest(builder);
        data.path = NetURL.GET_TICKET;
        data.flag = 1;

        if (!isProgressShowing()) {
            showProgressDialog("正在登录，请稍候...", true);
        }
        requestID = DataLoader.getInstance().loadData(this, data);
    }

    /**
     * 判断是否绑定了三方帐号
     */
    private void checkThirdLogin() {
        RequestBeanBuilder builder = RequestBeanBuilder.create(false);
        builder.addBody("openid", openid);// openid值为uid
        builder.addBody("unionid", unionid);
        builder.addBody("platform", mPlatform);
        builder.addBody("ip", SystemUtil.getLocalIpAddress());
        ResponseData data = builder.syncRequest(builder);
        data.path = NetURL.BIND_CHECK;
        data.flag = 3;

        if (!isProgressShowing()) {
            showProgressDialog("正在登录，请稍候...", true);
        }
        requestID = DataLoader.getInstance().loadData(this, data);
    }

    /**
     * 获取用户信息
     *
     * @param ticket 票据
     */
    private void getUserInfo(String ticket) {
        RequestBeanBuilder builder = RequestBeanBuilder.create(true);
        ResponseData data = builder.syncRequest(builder);
        data.path = NetURL.GET_USER_INFO;
        data.flag = 2;
        requestID = DataLoader.getInstance().loadData(this, data);
    }

    private void requestCertResult() {
        LogUtil.i("dw", "requestCertResult()");
        RequestBeanBuilder b = RequestBeanBuilder.create(true);
        b.addBody("uid", SessionContext.mUser.LOCALUSER.id);
        ResponseData d = b.syncRequest(b);
        d.path = NetURL.CERT_STATUS_BY_UID;
        d.flag = 11;
        DataLoader.getInstance().loadData(this, d);
    }

    @Override
    public void preExecute(ResponseData request) {
    }

    @Override
    public void notifyMessage(ResponseData request, ResponseData response)
            throws Exception {
        if (request.flag == 1) {
            JSONObject mJson = JSON.parseObject(response.body.toString());
            String ticket = mJson.getString("accessTicket");
            // 记录登录ticket
            SharedPreferenceUtil.getInstance().setString(AppConst.ACCESS_TICKET, ticket, true);
            SessionContext.setTicket(ticket);
            getUserInfo(ticket);
        } else if (request.flag == 2) {
            removeProgressDialog();
            if (StringUtil.empty(response.body.toString()) || response.body.toString().equals("{}")) {
                ToastUtil.show("获取用户信息失败，请重试", 0);
                return;
            }

            SessionContext.mUser = JSON.parseObject(response.body.toString(), UserInfo.class);
            if (SessionContext.mUser == null || StringUtil.empty(SessionContext.mUser.USERBASIC)) {
                ToastUtil.show("获取用户信息失败，请重试", 0);
                return;
            }

            SharedPreferenceUtil.getInstance().setString(AppConst.USERNAME, phoneStr, true);// 保存用户名
            SharedPreferenceUtil.getInstance().setString(AppConst.LAST_LOGIN_DATE, DateUtil.getCurDateStr(null), false);// 保存登录时间
            if (StringUtil.notEmpty(SessionContext.mUser.USERBASIC.headphotourl)) {
                SharedPreferenceUtil.getInstance().setString(AppConst.USER_PHOTO_URL, SessionContext.mUser.USERBASIC.getHeadphotourl(), false);
            } else {
                SharedPreferenceUtil.getInstance().setString(AppConst.USER_PHOTO_URL, "", false);
            }
            SharedPreferenceUtil.getInstance().setString(AppConst.USER_INFO, response.body.toString(), true);
            ToastUtil.show("登录成功", 0);
            Intent mIntent = new Intent(AppConst.ACTION_DYNAMIC_USER_INFO);
            LocalBroadcastManager.getInstance(AppContext.mMainContext).sendBroadcast(mIntent);
            JPushInterface.setAlias(AppContext.mMainContext, SessionContext.mUser.USERAUTH.mobilenum, null);
            if (mCancelLogin != null) {
                mCancelLogin.isCancelLogin(false);
            }

            requestCertResult();
            // 添加友盟自定义事件
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("userId", SessionContext.mUser.USERBASIC.id);
            MobclickAgent.onEvent(this, "UserLoginSuccess", map);
        } else if (request.flag == 3) {// 如果绑定，直接获取用户信息，没有绑定到绑定页面
            removeProgressDialog();
            JSONObject mJson = JSON.parseObject(response.body.toString());
            int flag = mJson.getInteger("flag");
            if (flag == 1) {// flag:0-未绑定 ，1-已绑定，当flag=1时，还返回accessTicket
                String accessTicket = mJson.getString("accessTicket");
                SharedPreferenceUtil.getInstance().setString(AppConst.ACCESS_TICKET, accessTicket, true);// 保存ticket
                SessionContext.setTicket(accessTicket);
                getUserInfo(accessTicket);
            } else {
                Intent intent = new Intent(this, BindPhoneActivity.class);
                intent.putExtra("thirdpartusername", thirdpartusername);
                intent.putExtra("thirdpartuserheadphotourl", thirdpartuserheadphotourl);
                intent.putExtra("openid", openid);
                intent.putExtra("unionid", unionid);
                intent.putExtra("platform", mPlatform);
                intent.putExtra("usertoken", usertoken);
                startActivity(intent);
            }
        } else if (request.flag == 11) {
            if (null != response && response.body != null) {
                LogUtil.i("dw", response.body.toString());
                SessionContext.mCertUserAuth = JSON.parseObject(response.body.toString(), CertUserAuth.class);
            }
            this.finish();
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
        if (mCancelLogin != null) {
            mCancelLogin.isCancelLogin(true);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (mCancelLogin != null) {
                mCancelLogin.isCancelLogin(true);
            }
            this.finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * 处理取消登录回调接口
     */
    public interface onCancelLoginListener {
        /**
         * @param isCancel true:取消登录；false:登录成功
         */
        public void isCancelLogin(boolean isCancel);
    }

    /**
     * 设置登录状态监听
     *
     * @param cancelLogin
     */
    public static final void setCancelLogin(onCancelLoginListener cancelLogin) {
        mCancelLogin = cancelLogin;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /** 使用SSO授权必须添加如下代码 */
        UMSsoHandler ssoHandler = mController.getConfig().getSsoHandler(requestCode);
        if (ssoHandler != null) {
            ssoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }

}
