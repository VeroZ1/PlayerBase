package com.kk.taurus.aliyunplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;
import com.aliyun.player.IPlayer;
import com.aliyun.player.bean.ErrorInfo;
import com.aliyun.player.bean.InfoBean;
import com.aliyun.player.source.UrlSource;
import com.kk.taurus.playerbase.config.AppContextAttach;
import com.kk.taurus.playerbase.config.PlayerConfig;
import com.kk.taurus.playerbase.config.PlayerLibrary;
import com.kk.taurus.playerbase.entity.DataSource;
import com.kk.taurus.playerbase.entity.DecoderPlan;
import com.kk.taurus.playerbase.event.BundlePool;
import com.kk.taurus.playerbase.event.EventKey;
import com.kk.taurus.playerbase.event.OnErrorEventListener;
import com.kk.taurus.playerbase.event.OnPlayerEventListener;
import com.kk.taurus.playerbase.log.PLog;
import com.kk.taurus.playerbase.player.BaseInternalPlayer;

import java.util.HashMap;

/**
 * Created by VeroZ
 */

public class AliyunPlayer extends BaseInternalPlayer {
    private final String TAG = "AliyunPlayer";

    public static final int PLAN_ID = 300;

    private final Context mAppContext;
    private AliPlayer mMediaPlayer;

    private long mCurrentPosition = 0;
    private int mPlayerState = IPlayer.idle;

    private int mTargetState = Integer.MAX_VALUE;

    private int startSeekPos;

    public static void init(Context context) {
        PlayerConfig.addDecoderPlan(new DecoderPlan(
                PLAN_ID,
                AliyunPlayer.class.getName(),
                "aliyunplayer"));
        PlayerConfig.setDefaultPlanId(PLAN_ID);
        PlayerLibrary.init(context);
    }

    public AliyunPlayer() {
        // init player
        mAppContext = AppContextAttach.getApplicationContext();
        mMediaPlayer = AliPlayerFactory.createAliPlayer(mAppContext);
    }

    @Override
    public void setDataSource(DataSource data) {
        if (data != null) {
            openVideo(data);
        }
    }

    private void openVideo(DataSource dataSource) {
        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = AliPlayerFactory.createAliPlayer(mAppContext);
            } else {
                stop();
                reset();
                resetListener();
            }
            mTargetState = Integer.MAX_VALUE;
            // REMOVED: mAudioSession
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnRenderingStartListener(mRenderingStartListener);
            mMediaPlayer.setOnStateChangedListener(mStateChangedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
            updateStatus(STATE_INITIALIZED);

            if (dataSource.getTimedTextSource() != null) {
                PLog.e(TAG, "aliyunplayer not support timed text !");
            }

            String data = dataSource.getData();
            Uri uri = dataSource.getUri();
            String assetsPath = dataSource.getAssetsPath();
            HashMap<String, String> headers = dataSource.getExtra();
            int rawId = dataSource.getRawId();
            UrlSource urlSource = new UrlSource();
            if (data != null) {
                urlSource.setUri(data);
                if (headers == null)
                    mMediaPlayer.setDataSource(urlSource);
                else {
                    mMediaPlayer.setDataSource(urlSource/*, headers*/);
                }
            } else if (uri != null) {
                urlSource.setUri(uri.getEncodedPath());
                if (headers == null)
                    mMediaPlayer.setDataSource(urlSource);
                else {
                    mMediaPlayer.setDataSource(urlSource/*, headers*/);
                }
            } else if (!TextUtils.isEmpty(assetsPath)) {
                Log.e(TAG, "aliplayer not support assets play, you can use raw play.");
            } else if (rawId > 0) {
                Uri rawUri = DataSource.buildRawPath(mAppContext.getPackageName(), rawId);
                urlSource.setUri(rawUri.getEncodedPath());
                mMediaPlayer.setDataSource(urlSource);
            }

            //mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepare();

            //set looping indicator for AliPlayer
            mMediaPlayer.setLoop(isLooping());

            Bundle bundle = BundlePool.obtain();
            bundle.putSerializable(EventKey.SERIALIZABLE_DATA, dataSource);
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET, bundle);
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus(STATE_ERROR);
            mTargetState = STATE_ERROR;
            submitErrorEvent(OnErrorEventListener.ERROR_EVENT_IO, null);
        }
    }

    private boolean available() {
        return mMediaPlayer != null;
    }

    @Override
    public void start() {
        if (available() &&
                (getState() == STATE_PREPARED
                        || getState() == STATE_PAUSED
                        || getState() == STATE_PLAYBACK_COMPLETE)) {
            mMediaPlayer.start();
            updateStatus(STATE_STARTED);
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_START, null);
        }
        mTargetState = STATE_STARTED;
        PLog.d(TAG, "start...");
    }

    @Override
    public void start(int msc) {
        if (getState() == STATE_PREPARED && msc > 0) {
            start();
            mMediaPlayer.seekTo(msc);
        } else {
            if (msc > 0) {
                startSeekPos = msc;
            }
            if (available()) {
                start();
            }
        }
    }

    @Override
    public void pause() {
        try {
            int state = getState();
            if (available()
                    && state != STATE_END
                    && state != STATE_ERROR
                    && state != STATE_IDLE
                    && state != STATE_INITIALIZED
                    && state != STATE_PAUSED
                    && state != STATE_STOPPED) {
                mMediaPlayer.pause();
                updateStatus(STATE_PAUSED);
                submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_PAUSE, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public void resume() {
        try {
            if (available() && getState() == STATE_PAUSED) {
                mMediaPlayer.start();
                updateStatus(STATE_STARTED);
                submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_RESUME, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mTargetState = STATE_STARTED;
    }

    @Override
    public void seekTo(int msc) {
        if (available() &&
                (getState() == STATE_PREPARED
                        || getState() == STATE_STARTED
                        || getState() == STATE_PAUSED
                        || getState() == STATE_PLAYBACK_COMPLETE)) {
            mMediaPlayer.seekTo(msc);
            Bundle bundle = BundlePool.obtain();
            bundle.putInt(EventKey.INT_DATA, msc);
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_TO, bundle);
        }
    }

    @Override
    public void stop() {
        if (available() &&
                (getState() == STATE_PREPARED
                        || getState() == STATE_STARTED
                        || getState() == STATE_PAUSED
                        || getState() == STATE_PLAYBACK_COMPLETE)) {
            mMediaPlayer.stop();
            updateStatus(STATE_STOPPED);
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_STOP, null);
        }
        mTargetState = STATE_STOPPED;
        mCurrentPosition = 0;
    }

    @Override
    public void reset() {
        if (available()) {
            mMediaPlayer.reset();
            updateStatus(STATE_IDLE);
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_RESET, null);
        }
        mTargetState = STATE_IDLE;
        mCurrentPosition = 0;
        mPlayerState = IPlayer.idle;
    }

    @Override
    public boolean isPlaying() {
        if (available() && getState() != STATE_ERROR) {
            return mPlayerState == IPlayer.started;
        }
        return false;
    }

    @Override
    public int getCurrentPosition() {
        if (available() && (getState() == STATE_PREPARED
                || getState() == STATE_STARTED
                || getState() == STATE_PAUSED
                || getState() == STATE_PLAYBACK_COMPLETE)) {
            return (int) mCurrentPosition;
        }
        return 0;
    }

    @Override
    public int getDuration() {
        if (available()
                && getState() != STATE_ERROR
                && getState() != STATE_INITIALIZED
                && getState() != STATE_IDLE) {
            return (int) mMediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public int getVideoWidth() {
        if (available()) {
            return mMediaPlayer.getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getVideoHeight() {
        if (available()) {
            return mMediaPlayer.getVideoHeight();
        }
        return 0;
    }

    @Override
    public void destroy() {
        if (available()) {
            updateStatus(STATE_END);
            resetListener();
            mMediaPlayer.release();
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_DESTROY, null);
        }
        mCurrentPosition = 0;
        mPlayerState = IPlayer.idle;
    }

    @Override
    public void setDisplay(SurfaceHolder surfaceHolder) {
        try {
            if (available()) {
                mMediaPlayer.setDisplay(surfaceHolder);
                submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_HOLDER_UPDATE, null);
            }
        } catch (Exception e) {
            Bundle bundle = BundlePool.obtain();
            bundle.putString("errorMessage", e.getMessage());
            bundle.putString("causeMessage", e.getCause() != null ? e.getCause().getMessage() : "");
            submitErrorEvent(OnErrorEventListener.ERROR_EVENT_RENDER, bundle);
        }
    }

    @Override
    public void setSurface(Surface surface) {
        try {
            if (available()) {
                mMediaPlayer.setSurface(surface);
                submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_UPDATE, null);
            }
        } catch (Exception e) {
            Bundle bundle = BundlePool.obtain();
            bundle.putString("errorMessage", e.getMessage());
            bundle.putString("causeMessage", e.getCause() != null ? e.getCause().getMessage() : "");
            submitErrorEvent(OnErrorEventListener.ERROR_EVENT_RENDER, bundle);
        }
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        if (available()) {
            mMediaPlayer.setVolume(leftVolume);
        }
    }

    @Override
    public void setSpeed(float speed) {
        if (available()) {
            mMediaPlayer.setSpeed(speed);
        }
    }

    @Override
    public void setLooping(boolean looping) {
        super.setLooping(looping);
        mMediaPlayer.setLoop(looping);
    }

    @Override
    public int getAudioSessionId() {
        /*if(available()){
            return mMediaPlayer.getAudioSessionId();
        }*/
        return 0;
    }

    private void resetListener() {
        if (mMediaPlayer == null)
            return;
        mMediaPlayer.setOnPreparedListener(null);
        mMediaPlayer.setOnRenderingStartListener(null);
        mMediaPlayer.setOnStateChangedListener(null);
        mMediaPlayer.setOnVideoSizeChangedListener(null);
        mMediaPlayer.setOnCompletionListener(null);
        mMediaPlayer.setOnErrorListener(null);
        mMediaPlayer.setOnInfoListener(null);
        mMediaPlayer.setOnSeekCompleteListener(null);
    }

    IPlayer.OnPreparedListener mPreparedListener = new IPlayer.OnPreparedListener() {
        @Override
        public void onPrepared() {
            PLog.d(TAG, "onPrepared...");
            updateStatus(STATE_PREPARED);

            mVideoWidth = mMediaPlayer.getVideoWidth();
            mVideoHeight = mMediaPlayer.getVideoHeight();

            Bundle bundle = BundlePool.obtain();
            bundle.putInt(EventKey.INT_ARG1, mVideoWidth);
            bundle.putInt(EventKey.INT_ARG2, mVideoHeight);

            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_PREPARED, bundle);

            int seekToPosition = startSeekPos;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition > 0 && mMediaPlayer.getDuration() > 0) {
                mMediaPlayer.seekTo(seekToPosition);
                startSeekPos = 0;
            }

            // We don't know the video size yet, but should start anyway.
            // The video size might be reported to us later.
            PLog.d(TAG, "mTargetState = " + mTargetState);
            if (mTargetState == STATE_STARTED) {
                start();
            } else if (mTargetState == STATE_PAUSED) {
                pause();
            } else if (mTargetState == STATE_STOPPED
                    || mTargetState == STATE_IDLE) {
                reset();
            }
        }
    };

    IPlayer.OnRenderingStartListener mRenderingStartListener = new IPlayer.OnRenderingStartListener() {
        @Override
        public void onRenderingStart() {
            PLog.d(TAG, "onRenderingStart...");
            startSeekPos = 0;
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START, null);
        }
    };

    IPlayer.OnStateChangedListener mStateChangedListener = new IPlayer.OnStateChangedListener() {
        @Override
        public void onStateChanged(int newState) {
            PLog.d(TAG, "onStateChanged...");
            mPlayerState = newState;
            //submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_STATUS_CHANGE, null);
        }
    };

    private int mVideoWidth;
    private int mVideoHeight;
    IPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new IPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(int width, int height) {
                    mVideoWidth = mMediaPlayer.getVideoWidth();
                    mVideoHeight = mMediaPlayer.getVideoHeight();
                    Bundle bundle = BundlePool.obtain();
                    bundle.putInt(EventKey.INT_ARG1, mVideoWidth);
                    bundle.putInt(EventKey.INT_ARG2, mVideoHeight);
                    submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE, bundle);
                }
            };

    private IPlayer.OnCompletionListener mCompletionListener = new IPlayer.OnCompletionListener() {
        @Override
        public void onCompletion() {
            updateStatus(STATE_PLAYBACK_COMPLETE);
            mTargetState = STATE_PLAYBACK_COMPLETE;
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE, null);
            if (!isLooping()) {
                stop();
            }
        }
    };

    private IPlayer.OnInfoListener mInfoListener =
            new IPlayer.OnInfoListener() {
                @Override
                public void onInfo(InfoBean infoBean) {
                    switch (infoBean.getCode()) {
                        case Unknown:
                            PLog.d(TAG, "Unknown:");
                            break;
                        case LoopingStart:
                            PLog.d(TAG, "LoopingStart:");
                            break;
                        case BufferedPosition:
                            PLog.d(TAG, "BufferedPosition:");
                            long mVideoBufferedPosition = infoBean.getExtraValue();
                            submitBufferingUpdate((int) mVideoBufferedPosition, null);
                            break;
                        case CurrentPosition:
                            PLog.d(TAG, "CurrentPosition:");
                            mCurrentPosition = infoBean.getExtraValue();
                            //submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END, null);
                            break;
                        case AutoPlayStart:
                            PLog.d(TAG, "AutoPlayStart:");
                            break;
                        case CurrentDownloadSpeed:
                            PLog.d(TAG, "CurrentDownloadSpeed:");
                            break;
                        case SubtitleSelectError:
                            PLog.d(TAG, "SubtitleSelectError:");
                            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_SUBTITLE_TIMED_OUT, null);
                            break;
                    }
                }
            };

    private IPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new IPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete() {
            PLog.d(TAG, "onSeekComplete...");
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE, null);
        }
    };

    private IPlayer.OnErrorListener mErrorListener =
            new IPlayer.OnErrorListener() {
                @Override
                public void onError(ErrorInfo errorInfo) {
                    PLog.d(TAG, "Error: " + errorInfo.getCode());
                    updateStatus(STATE_ERROR);
                    mTargetState = STATE_ERROR;

                    switch (errorInfo.getCode()) {
                        case ERROR_SERVER_NO_RESPONSE:
//                            release(true);
                            break;
                    }

                    /* If an error handler has been supplied, use it and finish. */
                    Bundle bundle = BundlePool.obtain();
                    submitErrorEvent(OnErrorEventListener.ERROR_EVENT_COMMON, bundle);
                }
            };

}
