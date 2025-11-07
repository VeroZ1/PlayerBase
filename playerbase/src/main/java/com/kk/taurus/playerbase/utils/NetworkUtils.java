/*
 * Copyright 2017 jiajunhui<junhui_jia@163.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.kk.taurus.playerbase.utils;

import static com.kk.taurus.playerbase.config.PConst.NETWORK_STATE_2G;
import static com.kk.taurus.playerbase.config.PConst.NETWORK_STATE_3G;
import static com.kk.taurus.playerbase.config.PConst.NETWORK_STATE_4G;
import static com.kk.taurus.playerbase.config.PConst.NETWORK_STATE_5G;
import static com.kk.taurus.playerbase.config.PConst.NETWORK_STATE_CONNECTING;
import static com.kk.taurus.playerbase.config.PConst.NETWORK_STATE_MOBILE_UNKNOWN;
import static com.kk.taurus.playerbase.config.PConst.NETWORK_STATE_NONE;
import static com.kk.taurus.playerbase.config.PConst.NETWORK_STATE_WIFI;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresPermission;

/**
 * Created by Taurus on 2018/5/27.
 */
public class NetworkUtils {

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private static NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return null;
        return cm.getActiveNetworkInfo();
    }

    /**
     * get current network connected type
     *
     * @param context context
     * @return int
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public static int getNetworkState(Context context) {
        NetworkInfo networkInfo = getActiveNetworkInfo(context);
        if (networkInfo != null) {
            if (networkInfo.isAvailable()) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    return NETWORK_STATE_WIFI;
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    switch (networkInfo.getSubtype()) {
                        case TelephonyManager.NETWORK_TYPE_GSM:
                        case TelephonyManager.NETWORK_TYPE_GPRS:
                        case TelephonyManager.NETWORK_TYPE_CDMA:
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                        case TelephonyManager.NETWORK_TYPE_IDEN:
                            return NETWORK_STATE_2G;

                        case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        case TelephonyManager.NETWORK_TYPE_UMTS:
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        case TelephonyManager.NETWORK_TYPE_EHRPD:
                        case TelephonyManager.NETWORK_TYPE_HSPAP:
                            return NETWORK_STATE_3G;

                        case TelephonyManager.NETWORK_TYPE_IWLAN:
                        case TelephonyManager.NETWORK_TYPE_LTE:
                            return NETWORK_STATE_4G;

                        case TelephonyManager.NETWORK_TYPE_NR:
                            return NETWORK_STATE_5G;
                        default:
                            String subtypeName = networkInfo.getSubtypeName();
                            if (subtypeName.equalsIgnoreCase("TD-SCDMA")
                                    || subtypeName.equalsIgnoreCase("WCDMA")
                                    || subtypeName.equalsIgnoreCase("CDMA2000")) {
                                return NETWORK_STATE_3G;
                            } else {
                                return NETWORK_STATE_MOBILE_UNKNOWN;
                            }
                    }
                } else {
                    return NETWORK_STATE_NONE;
                }
            } else {
                NetworkInfo.State networkInfoState = networkInfo.getState();
                if (networkInfoState == NetworkInfo.State.CONNECTING) {
                    return NETWORK_STATE_CONNECTING;
                }
                if (!networkInfo.isAvailable()) {
                    return NETWORK_STATE_NONE;
                }
            }
        }
        return NETWORK_STATE_NONE;
    }

    public static boolean isMobile(int networkState) {
        return networkState > NETWORK_STATE_WIFI;
    }

    /**
     * whether or not network connect.
     *
     * @param context context
     * @return true/false
     */
    public static boolean isNetConnected(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            @SuppressLint("MissingPermission")
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * is wifi ?
     *
     * @param context context
     * @return true/false
     */
    public static synchronized boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            @SuppressLint("MissingPermission")
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                int networkInfoType = networkInfo.getType();
                if (networkInfoType == ConnectivityManager.TYPE_WIFI || networkInfoType == ConnectivityManager.TYPE_ETHERNET) {
                    return networkInfo.isConnected();
                }
            }
        }
        return false;
    }

}
