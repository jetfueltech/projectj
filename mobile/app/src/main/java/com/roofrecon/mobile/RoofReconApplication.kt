package com.roofrecon.mobile

import android.app.Application
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import android.util.Log

/**
 * Initializes the DJI Mobile SDK v5 once for the whole process.
 *
 * The SDK requires the `com.dji.sdk.API_KEY` manifest meta-data (set via the
 * `djiAppKey` Gradle property at build time) to register against DJI's
 * licensing service the first time the app is launched online.
 */
class RoofReconApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SDKManager.getInstance().init(this, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                Log.i(TAG, "DJI SDK registered")
            }
            override fun onRegisterFailure(error: IDJIError?) {
                Log.e(TAG, "DJI SDK register failed: $error")
            }
            override fun onProductConnect(productId: Int) {
                Log.i(TAG, "Aircraft connected: $productId")
            }
            override fun onProductDisconnect(productId: Int) {
                Log.i(TAG, "Aircraft disconnected: $productId")
            }
            override fun onProductChanged(productId: Int) {}
            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {}
            override fun onDatabaseDownloadProgress(current: Long, total: Long) {}
        })
    }

    companion object {
        private const val TAG = "RoofReconApp"
    }
}
