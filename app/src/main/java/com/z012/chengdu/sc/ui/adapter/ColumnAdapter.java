package com.z012.chengdu.sc.ui.adapter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.prj.sdk.app.AppContext;
import com.prj.sdk.db.SQLiteTemplate;
import com.prj.sdk.db.SQLiteTemplate.RowMapper;
import com.prj.sdk.net.bean.ResponseData;
import com.prj.sdk.net.data.DataCallback;
import com.prj.sdk.net.data.DataLoader;
import com.prj.sdk.net.down.DownCallback;
import com.prj.sdk.net.down.DownLoaderTask;
import com.prj.sdk.util.NetworkUtil;
import com.prj.sdk.util.SharedPreferenceUtil;
import com.prj.sdk.util.ToastUtil;
import com.prj.sdk.util.Utils;
import com.prj.sdk.zip.ZipExtractorCallback;
import com.prj.sdk.zip.ZipExtractorTask;
import com.z012.chengdu.sc.R;
import com.z012.chengdu.sc.SessionContext;
import com.z012.chengdu.sc.constants.AppConst;
import com.z012.chengdu.sc.constants.NetURL;
import com.z012.chengdu.sc.entity.WebInfoEntity;
import com.z012.chengdu.sc.net.entity.AppListBean;
import com.z012.chengdu.sc.net.entity.DownInfoBean;
import com.z012.chengdu.sc.net.request.RequestBeanBuilder;
import com.z012.chengdu.sc.ui.activity.HtmlActivity;
import com.z012.chengdu.sc.ui.dialog.CustomDialog;
import com.z012.chengdu.sc.ui.dialog.CustomDialog.onCallBackListener;

import java.io.File;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 栏目列表适配器
 *
 * @author LiaoBo
 */
public class ColumnAdapter extends BaseAdapter implements DownCallback, ZipExtractorCallback, DataCallback {
    private Context mContext;
    private LayoutInflater inflater;
    private List<AppListBean> mBeans;

    private ZipExtractorTask zipTask;
    private DownLoaderTask downTask;
    private String entrance;            // html程序入口
    private SQLiteTemplate mSQLiteTemplate;
    private String internalver;
    private String appver;
    private ProgressDialog mProgressDialog;
    private int mPosition;
    private String mID, mName;
    private boolean isCollection, isSearchHistory;

    public ColumnAdapter(Context context, List<AppListBean> Beans) {
        this.mBeans = Beans;
        this.mContext = context;
        this.inflater = LayoutInflater.from(mContext);
        this.mSQLiteTemplate = SQLiteTemplate.getInstance(AppContext.mDBManager);
    }

    public int getCount() {
        return mBeans.size();
    }

    // 列表项
    public Object getItem(int position) {
        return mBeans.get(position);
    }

    // 列表id
    public long getItemId(int position) {
        return position;
    }

    /**
     * 是否是收藏页面
     */
    public void isCollection(boolean bool) {
        isCollection = bool;
    }

    public void isSearchHistory(boolean bool) {
        isSearchHistory = bool;
    }

    public final class ViewHolder {
        private TextView tv_name, tv_circle;
        private ImageView circleImageView;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final AppListBean temp = mBeans.get(position);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.lv_column_item, null);
            holder = new ViewHolder();
            holder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            holder.circleImageView = (ImageView) convertView.findViewById(R.id.circleImageView);
            holder.tv_circle = (TextView) convertView.findViewById(R.id.tv_circle);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        int p = position + 1;
        int resId;
        if (p % 5 == 0) {
            resId = R.drawable.circle_darkgreen_bg;
        } else if (p % 4 == 0) {
            resId = R.drawable.circle_yellow_bg;
        } else if (p % 3 == 0) {
            resId = R.drawable.circle_origen_bg;
        } else if (p % 2 == 0) {
            resId = R.drawable.circle_green_bg;
        } else {
            resId = R.drawable.circle_blue_bg;
        }
        holder.tv_circle.setBackgroundResource(resId);

        String title = temp.appname;
        if (isSearchHistory) {
            if (title != null && title.length() > 0) {
                holder.tv_name.setText(title);
                holder.tv_circle.setText(String.valueOf(title.charAt(0)));
            }
            // setHeadImg(holder.img_icon, NetURL.API_LINK + temp.imgurls, (temp.imgurls + position));
        } else {
            if (title != null && title.length() > 0) {
                holder.tv_name.setText(title);
                holder.tv_circle.setText(String.valueOf(title.charAt(0)));
            }
            // setHeadImg(holder.img_icon, NetURL.API_LINK + temp.imgurls1, (temp.imgurls1 + position));

        }

        convertView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent mIntent = new Intent(mContext, HtmlActivity.class);
                WebInfoEntity webInfoEntity;
                if (isSearchHistory) {
                    webInfoEntity = new WebInfoEntity(temp.id, temp.appname, temp.appurls);
                    saveHistory(temp.appname);
                } else {
                    webInfoEntity = new WebInfoEntity(temp.id, temp.name, temp.linkurls);
                }
                mIntent.putExtra("webEntity", webInfoEntity);
                mContext.startActivity(mIntent);
            }
        });

        return convertView;
    }

    public void setHeadImg(ImageView view, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        if (!url.startsWith("http")) {
            url = NetURL.API_LINK + url;
        }
        Glide.with(mContext).load(url).placeholder(R.drawable.round_loading).crossFade().into(view);
    }

    /**
     * 下载或解压处理或进入应用综合判断处理
     */
    public void doDeal(String appid, String url) {
        try {
            if (Utils.isFolderDir("resource/" + appid)) {// 存在跳转
                StringBuilder sb = new StringBuilder();
                sb.append(Utils.getFolderDir("resource")).append(appid);
                sb.append(File.separator).append(getFileName(sb.toString()));
                sb.append(entrance);
                sb.insert(0, "file://");
                Intent intent = new Intent(mContext, HtmlActivity.class);// HtmlActivity WebActivity
                intent.putExtra("webEntity", new WebInfoEntity(sb.toString()));
                mContext.startActivity(intent);
            } else if (Utils.isFolderDir("zip/" + appid)) {// 有下载就解压
                if (zipTask == null || zipTask.getStatus() != AsyncTask.Status.RUNNING) {
                    zipTask = new ZipExtractorTask(mContext, Utils.getFolderDir("zip") + appid, Utils.getFolderDir("resource") + appid, true, this);
                    zipTask.execute();
                }
            } else {// 下载
                downFile(url, appid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void down(String url, String local, int down_status, String filename) {
        if (down_status == 1) {
            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put("id", filename);
                contentValues.put("internalver", internalver);
                contentValues.put("appver", appver);
                if (mSQLiteTemplate.isExistsByField("down_log", "id", filename)) {// 记录数据

                    mSQLiteTemplate.updateById("down_log", filename, contentValues);
                } else {
                    mSQLiteTemplate.insert("down_log", contentValues);
                }

                if (zipTask == null || zipTask.getStatus() != AsyncTask.Status.RUNNING) {
                    zipTask = new ZipExtractorTask(mContext, local, Utils.getFolderDir("resource") + filename, true, this);
                    zipTask.execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (down_status == 0) {
            // ToastUtil.show("下载任务已取消", 0);
            // TODO 删除操作
        } else {
            ToastUtil.show("下载失败，已取消", 0);
        }
    }

    @Override
    public void unZip(String inPath, String outPath, int status) {
        if (status == 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("file://").append(outPath).append(File.separator).append(getFileName(outPath)).append(entrance);
            Intent intent = new Intent(mContext, HtmlActivity.class);
            intent.putExtra("webEntity", new WebInfoEntity(sb.toString()));
            mContext.startActivity(intent);
        } else if (status == 0) {
            // ToastUtil.show("解压任务已取消", 0);
        } else {
            ToastUtil.show("解压失败，请重试", 0);
        }
    }

    /**
     * 获取当前文件目录子目录文件名
     *
     * @param file
     */
    public String getFileName(String file) {
        File tempFile = new File(file);
        if (tempFile.isDirectory()) // 是不是目录
            return tempFile.list()[0];// 返回该目录下所有文件及文件夹数组中的0个元素的目录名
        return "";
    }

    /**
     * 判断升级处理
     */
    public void doJudgeUpgrade(String appver, String appid, String url) {
        try {

            DownInfoBean temp = mSQLiteTemplate.queryForObject(new RowMapper<DownInfoBean>() {
                @Override
                public DownInfoBean mapRow(Cursor cursor, int index) {
                    DownInfoBean temp = new DownInfoBean();
                    // temp.id=cursor.getString(cursor.getColumnIndex("id"));
                    temp.internalver = cursor.getString(cursor.getColumnIndex("internalver"));
                    temp.appver = cursor.getString(cursor.getColumnIndex("appver"));
                    return temp;
                }
            }, "select * from down_log where id= ? ", new String[]{appid});
            if (temp == null) {// 初次下载
                // doDeal(appid, url);
                downFile(url, appid);
            } else {
                if (appver.compareTo(temp.appver) > 0) {// 有更新，重新下载
                    downFile(url, appid);
                } else {
                    doDeal(appid, url);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载文件
     */
    private void downFile(final String url, final String appid) {
        if (downTask == null || downTask.getStatus() != AsyncTask.Status.RUNNING) {
            if (NetworkUtil.isNetworkAvailable()) {
                if (NetworkUtil.isWifi()) {
                    downTask = new DownLoaderTask(mContext, url, appid, true, this);
                    downTask.execute();
                } else {
                    downTask = new DownLoaderTask(mContext, url, appid, true, this);
                    CustomDialog mTip = new CustomDialog(mContext);
                    mTip.setBtnText("取消", "确定");
                    mTip.show(mContext.getResources().getText(R.string.dialog_tip).toString());
                    mTip.setCanceledOnTouchOutside(false);
                    mTip.setListeners(new onCallBackListener() {

                        @Override
                        public void rightBtn(CustomDialog dialog) {
                            dialog.dismiss();
                            downTask.execute();
                        }

                        @Override
                        public void leftBtn(CustomDialog dialog) {
                            dialog.dismiss();
                            downTask.cancel(true);
                        }
                    });
                }
            } else {
                ToastUtil.show("暂无网络...", 0);
            }
        }
    }

    /**
     * 收藏处理
     *
     * @param appid
     * @param type  0：查看是否收藏；1：收藏；2：取消收藏
     */
    private void Collection(String appid, int type, int flag) {

        RequestBeanBuilder builder = RequestBeanBuilder.create(true);
        builder.addBody("areaId", SessionContext.getAreaInfo(1));
        builder.addBody("appId", appid);

        ResponseData data = builder.syncRequest(builder);
        if (type == 0) {
            data.path = NetURL.IS_SC;
            showProgressDialog("正在加载，请稍候...", false);
        } else if (type == 1) {
            data.path = NetURL.ADD_SC;
            showProgressDialog("提交中，请稍候...", false);
        } else {
            data.path = NetURL.REMOVE_SC;
            showProgressDialog("提交中，请稍候...", false);
        }
        data.flag = flag;

        DataLoader.getInstance().loadData(this, data);
    }

    @Override
    public void preExecute(ResponseData request) {

    }

    @Override
    public void notifyMessage(ResponseData request, ResponseData response) throws Exception {
        removeProgressDialog();
        if (request.flag == 1) {
            if (response.body instanceof Boolean) {
                if ((Boolean) response.body) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("确定取消收藏“").append(mName).append("”?");
                    // 显示取消收藏
                    showAlertDialog(sb.toString(), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            if (SessionContext.isLogin()) {
                                Collection(mID, 2, 3);
                            } else {
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(AppConst.ACTION_UNLOGIN));
                            }
                        }

                    }, null);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("确定收藏“").append(mName).append("”?");
                    // 显示收藏
                    showAlertDialog(sb.toString(), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            if (SessionContext.isLogin()) {
                                Collection(mID, 1, 2);
                            } else {
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(AppConst.ACTION_UNLOGIN));
                            }
                        }

                    }, null);
                }
            }
        } else if (request.flag == 2) {
            ToastUtil.show("收藏成功", 0);
        } else {
            ToastUtil.show("已经取消收藏", 0);
            if (isCollection) {
                mBeans.remove(mPosition);
                this.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void notifyError(ResponseData request, ResponseData response, Exception e) {
        removeProgressDialog();
        String message;
        if (e != null && e instanceof ConnectException) {
            message = mContext.getString(R.string.dialog_tip_net_error);
        } else {
            // message = getString(R.string.dialog_tip_null_error);
            message = response != null && response.data != null ? response.data.toString() : mContext.getString(R.string.dialog_tip_null_error);
        }
        ToastUtil.show(message, Toast.LENGTH_LONG);

    }

    public final void removeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * 显示loading对话框
     */
    public final void showProgressDialog(String tip, boolean cancelable) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext);
        }
        mProgressDialog.setMessage(tip);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(cancelable);
        mProgressDialog.show();
    }

    private void showAlertDialog(String tip, DialogInterface.OnClickListener positive, DialogInterface.OnClickListener negative) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setInverseBackgroundForced(true);
        builder.setMessage(tip);
        builder.setTitle(mContext.getString(R.string.alert_title));
        builder.setPositiveButton(mContext.getString(R.string.alert_ok), positive);
        builder.setNegativeButton(mContext.getString(R.string.alert_cancel), negative);
        builder.create().show();
    }

    /**
     * 保存历史字段
     *
     * @param str
     */
    public void saveHistory(String str) {
        // 获取搜索框信息
        String oldString = SharedPreferenceUtil.getInstance().getString("history", "", false);
        StringBuilder data = new StringBuilder();
        String[] history_arr = oldString.split(",");
        List<String> list = new ArrayList<String>(Arrays.asList(history_arr));
        // 如果有就去掉以前的str
        if (list.contains(str)) {
            list.remove(str);
        }
        list.add(str);// 重新添加
        Collections.reverse(list);// 反序
        if (list.size() > 10) {// 保留最新10条数据
            list = list.subList(0, 10);
        }
        for (String ss : list) {
            data.append(ss).append(",");
        }
        SharedPreferenceUtil.getInstance().setString("history", data.toString(), false);
    }
}
