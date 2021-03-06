package com.z012.chengdu.sc.ui.activity.weather;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baidu.location.BDLocation;
import com.common.widget.custom.MyListViewWidget;
import com.z012.chengdu.sc.R;
import com.z012.chengdu.sc.helper.LocationManagerBD;
import com.z012.chengdu.sc.helper.LocationManagerBD.LocationCallback;
import com.z012.chengdu.sc.helper.PinyinComparator;
import com.z012.chengdu.sc.net.entity.WeatherCityInfo;
import com.z012.chengdu.sc.ui.BaseActivity;
import com.z012.chengdu.sc.ui.adapter.WeatherCityAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeatherSelectActivity extends BaseActivity {
    private TextView tv_location;
    private EditText et_search;
    private MyListViewWidget listview;
    private WeatherCityAdapter adapter;

    private List<WeatherCityInfo> weatherCityList;
    private List<WeatherCityInfo> list = new ArrayList<WeatherCityInfo>();
    private boolean isSuccess = false;
    private String cityStr;
    private String distStr;

    /**
     * 根据拼音来排列ListView里面的数据类
     */
    private PinyinComparator pinyinComparator;

    @Override
    protected int getLayoutResId() {
        return R.layout.ui_cityselect_act;
    }

    @Override
    public void initParams() {
        super.initParams();
        tv_center_title.setText("选择城市");
        pinyinComparator = new PinyinComparator();
        listview = (MyListViewWidget) findViewById(R.id.listview);
        tv_location = (TextView) findViewById(R.id.tv_location);
        et_search = (EditText) findViewById(R.id.et_search);
        tv_location.setText("正在定位...");
        initJsonData();
        // 根据a-z进行排序源数据
        Collections.sort(weatherCityList, pinyinComparator);
        list.clear();
        list.addAll(weatherCityList);
        adapter = new WeatherCityAdapter(this, list);
        listview.setAdapter(adapter);
    }

    @Override
    public void initListeners() {
        super.initListeners();
        listview.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent data = new Intent();
                data.putExtra("city", list.get(position));
                setResult(RESULT_OK, data);
                finish();
            }
        });

        tv_location.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isSuccess) {
                    int index = 0;
                    for (int i = 0; i < weatherCityList.size(); i++) {
                        if (distStr.contains(weatherCityList.get(i).name)) {
                            index = i;
                            break;
                        }
                    }
                    Intent data = new Intent();
                    data.putExtra("city", weatherCityList.get(index));
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });

        et_search.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String tmp = s.toString();
                list.clear();
                for (int i = 0; i < weatherCityList.size(); i++) {
                    if (weatherCityList.get(i).name.contains(tmp)
                        /*|| weatherCityList.get(i).py.contains(tmp)*/) {
                        list.add(weatherCityList.get(i));
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void initJsonData() {
        try {
            InputStreamReader inputReader = new InputStreamReader(
                    getResources().getAssets().open("weatherCity.json"));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            StringBuffer result = new StringBuffer();
            while ((line = bufReader.readLine()) != null) {
                result.append(line);
            }
            inputReader.close();
            bufReader.close();
            JSONObject json = JSON.parseObject(result.toString());
            weatherCityList = JSON.parseArray(
                    json.getString("city").toString(), WeatherCityInfo.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isSuccess) {
            return;
        }
        LocationManagerBD.getIns().startLocation(new LocationCallback() {

            @Override
            public void onLocationInfo(BDLocation locationInfo) {
                if (locationInfo.getLocType() == BDLocation.TypeNetWorkLocation) {
                    cityStr = locationInfo.getCity();
                    distStr = locationInfo.getDistrict();
                    tv_location.setText(cityStr + distStr);
                    isSuccess = true;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocationManagerBD.getIns().stopLocation();
    }
}
