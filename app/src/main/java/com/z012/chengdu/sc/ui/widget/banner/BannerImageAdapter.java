package com.z012.chengdu.sc.ui.widget.banner;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.z012.chengdu.sc.R;
import com.z012.chengdu.sc.constants.NetURL;
import com.z012.chengdu.sc.entity.WebInfoEntity;
import com.z012.chengdu.sc.helper.ForbidFastClickHelper;
import com.z012.chengdu.sc.net.entity.BannerListBean;
import com.z012.chengdu.sc.ui.activity.HtmlActivity;

import java.util.List;

public class BannerImageAdapter extends PagerAdapter {

    private Context context;
    private List<BannerListBean.BannerItemBean> list;

    public BannerImageAdapter(Context context, List<BannerListBean.BannerItemBean> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        if (list.size() > 1) {
            return Integer.MAX_VALUE;
        }
        return list.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return (view == object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        position %= list.size();
        if (position < 0) {
            position = list.size() + position;
        }

        final BannerListBean.BannerItemBean bean = list.get(position);
        ImageView view = new ImageView(context);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.setScaleType(ImageView.ScaleType.FIT_XY);

        String imgurl = bean.getImgurls();
        if (!TextUtils.isEmpty(imgurl)) {
            if (!bean.getImgurls().startsWith("http")) {
                imgurl = NetURL.API_LINK + imgurl;
            }
            Glide.with(context)
                    .load(imgurl)
                    .placeholder(R.drawable.iv_banner_simple)
                    .crossFade()
                    .override(750, 480)
                    .into(view);
        }

        final String url = bean.getLinkurls();
        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (ForbidFastClickHelper.isForbidFastClick()) {
                    return;
                }

                if (!TextUtils.isEmpty(url) && !url.contains("Weather.getWeatherInfo.do")) {
                    Intent intent = new Intent(context, HtmlActivity.class);
                    intent.putExtra("webEntity", new WebInfoEntity(bean.getBnname(), url));
                    context.startActivity(intent);
                }
            }
        });

        //如果View已经在之前添加到了一个父组件，则必须先remove，否则会抛出IllegalStateException。
        ViewParent vp = view.getParent();
        if (vp != null) {
            ViewGroup parent = (ViewGroup) vp;
            parent.removeView(view);
        }
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
    }
}
