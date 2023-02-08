package com.kk.taurus.txplayer;

import android.content.Context;
import android.util.Log;

import com.tencent.rtmp.TXLiveBase;
import com.tencent.rtmp.TXLiveBaseListener;

public class TxLicenseUtil {

    private static final String TAG = "TxLicenseUtil";

    public static void initLicence(Context context, String licenceUrl, String licenceKey) {
        try {
            TXLiveBase.getInstance().setLicence(context, licenceUrl, licenceKey);
            TXLiveBase.setListener(new TXLiveBaseListener() {
                @Override
                public void onLicenceLoaded(int result, String reason) {
                    Log.i(TAG, "onLicenceLoaded: result:" + result + ", reason:" + reason);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "initLicenseFailed: exception: " + e);
        }
    }
}
