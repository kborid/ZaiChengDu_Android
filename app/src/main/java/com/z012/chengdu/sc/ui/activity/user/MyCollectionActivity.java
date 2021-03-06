package com.z012.chengdu.sc.ui.activity.user;

import android.content.DialogInterface;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.prj.sdk.net.bean.ResponseData;
import com.prj.sdk.net.data.DataCallback;
import com.prj.sdk.net.data.DataLoader;
import com.prj.sdk.util.ToastUtil;
import com.z012.chengdu.sc.R;
import com.z012.chengdu.sc.SessionContext;
import com.z012.chengdu.sc.constants.NetURL;
import com.z012.chengdu.sc.net.request.RequestBeanBuilder;
import com.z012.chengdu.sc.net.entity.AppListBean;
import com.z012.chengdu.sc.ui.BaseActivity;
import com.z012.chengdu.sc.ui.adapter.ColumnAdapter;

import java.net.ConnectException;
import java.util.ArrayList;

/**
 * 我的收藏
 *
 * @author LiaoBo
 */
public class MyCollectionActivity extends BaseActivity implements DataCallback, DialogInterface.OnCancelListener {
    private ListView mListView;
    private ColumnAdapter mAdapter;
    private ArrayList<AppListBean> mBean = new ArrayList<AppListBean>();

    @Override
    protected int getLayoutResId() {
        return R.layout.ui_my_collection;
    }

    @Override
    public void initParams() {
        super.initParams();
        tv_center_title.setText("我的收藏");
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new ColumnAdapter(this, mBean);
        mListView.setAdapter(mAdapter);
        mAdapter.isCollection(true);
        loadData();
    }

    /**
     * 加载数据
     *
     * @return
     */
    private void loadData() {
        RequestBeanBuilder builder = RequestBeanBuilder.create(true);

        builder.addBody("areaId", SessionContext.getAreaInfo(1));

        ResponseData data = builder.syncRequest(builder);
        data.path = NetURL.GET_SC_LIST;
        data.flag = 1;

        if (!isProgressShowing())
            showProgressDialog("正在加载，请稍候...", true);
        requestID = DataLoader.getInstance().loadData(this, data);

    }

    @Override
    public void initListeners() {
        super.initListeners();
    }

    @Override
    public void preExecute(ResponseData request) {

    }

    @Override
    public void notifyMessage(ResponseData request, ResponseData response) throws Exception {
        removeProgressDialog();

        JSONArray mJson = JSON.parseArray(response.body.toString());
        if (mJson == null || mJson.size() == 0) {
            ToastUtil.show("暂无收藏", 0);
            return;
        } else {
            ArrayList<String> data = new ArrayList<String>();
            for (int i = 0; i < mJson.size(); i++) {
                data.add(mJson.get(i).toString());
            }

            // 查询对应的应用
            for (AppListBean appItem : SessionContext.getAllAppList()) {
                for (String string : data) {
//					if (appItem.id.equals(string)) {
//						mBean.add(appItem);
//					}
                }
            }
            mAdapter.notifyDataSetChanged();
            data.clear();
            data = null;
        }

    }

    @Override
    public void notifyError(ResponseData request, ResponseData response, Exception e) {
        removeProgressDialog();
        String message;
        if (e != null && e instanceof ConnectException) {
            message = getString(R.string.dialog_tip_net_error);
        } else {
            // message = getString(R.string.dialog_tip_null_error);
            message = response != null && response.data != null ? response.data.toString() : getString(R.string.dialog_tip_null_error);
        }
        ToastUtil.show(message, Toast.LENGTH_LONG);

    }

    @Override
    public void onCancel(DialogInterface dialog) {
        DataLoader.getInstance().clear(requestID);
        removeProgressDialog();
    }

}
