package com.kk.taurus.avplayer.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kk.taurus.avplayer.R;
import com.kk.taurus.avplayer.bean.VideoBean;
import com.kk.taurus.avplayer.play.DataInter;
import com.kk.taurus.avplayer.play.ReceiverGroupManager;
import com.kk.taurus.avplayer.play.ShareAnimationPlayer;
import com.kk.taurus.avplayer.utils.ImageDisplayEngine;
import com.kk.taurus.playerbase.entity.DataSource;
import com.kk.taurus.playerbase.receiver.ReceiverGroup;

public class ShareAnimationActivityA extends AppCompatActivity implements View.OnClickListener {

    private ImageView mAlbumImage;
    private ImageView playIcon;
    private RelativeLayout mAlbumLayout;
    private FrameLayout mLayoutContainer;
    private TextView mTvTitle;

    private DataSource mData;

    private boolean toNext;
    private ReceiverGroup mReceiverGroup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_animation_a);

        mAlbumImage = findViewById(R.id.albumImage);
        playIcon = findViewById(R.id.playIcon);
        mAlbumLayout = findViewById(R.id.album_layout);
        mLayoutContainer = findViewById(R.id.layoutContainer);
        mTvTitle = findViewById(R.id.tv_title);

        mAlbumLayout.setOnClickListener(this);
        mTvTitle.setOnClickListener(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        VideoBean mVideoBean = new VideoBean(
                "不想从被子里出来",
                "http://open-image.nosdn.127.net/57baaaeaad4e4fda8bdaceafdb9d45c2.jpg",
                "https://mov.bn.netease.com/open-movie/nos/mp4/2018/01/12/SD70VQJ74_sd.mp4");

        mData = new DataSource(mVideoBean.getPath());
        mData.setTitle(mVideoBean.getDisplayName());

        ImageDisplayEngine.display(this, mAlbumImage, mVideoBean.getCover(), R.mipmap.ic_launcher);
        mTvTitle.setText(mVideoBean.getDisplayName());

        mReceiverGroup = ReceiverGroupManager.get().getReceiverGroup(this);

    }

    @Override
    public void onClick(View v) {
        ShareAnimationPlayer.get().setReceiverGroup(mReceiverGroup);
        switch (v.getId()) {
            case R.id.album_layout:
                playIcon.setVisibility(View.GONE);
                ShareAnimationPlayer.get().play(mLayoutContainer, mData);
                break;
            case R.id.tv_title:
                toNext = true;
                Intent intent = new Intent(this, ShareAnimationActivityB.class);
                intent.putExtra(ShareAnimationActivityB.KEY_DATA, mData);
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                            this, mLayoutContainer, "videoShare");
                    ActivityCompat.startActivity(this, intent, options.toBundle());
                }else{
                    startActivity(intent);
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        playIcon.setVisibility(View.VISIBLE);

        mReceiverGroup.getGroupValue().putBoolean(DataInter.Key.KEY_CONTROLLER_TOP_ENABLE, false);
        mReceiverGroup.getGroupValue().putBoolean(DataInter.Key.KEY_CONTROLLER_SCREEN_SWITCH_ENABLE, false);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!toNext){
            ShareAnimationPlayer.get().pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ShareAnimationPlayer.get().destroy();

    }
}
