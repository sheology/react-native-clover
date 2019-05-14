package com.infuse.cloversdkreactnativebridge;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.util.CustomerMode;
import com.clover.sdk.v1.Intents;
import com.clover.sdk.v1.ServiceConnector;
import com.clover.sdk.v1.merchant.MerchantConnector;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.infuse.cloversdkreactnativebridge.payments.ExternalSecurePaymentTask;
import com.infuse.cloversdkreactnativebridge.payments.ReactPaymentConnector;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

class RNCloverBridgeModule extends ReactContextBaseJavaModule implements ServiceConnector.OnServiceConnectedListener {

    private static final String TAG = "RNCloverBridge";
    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQUEST_ACCOUNT = 2;
    private ReactContext mContext;

    private Account account;
    private MerchantConnector merchantConnector;
    private ReactPaymentConnector mPaymentConnector;

    private Promise accountPromise;
    private Promise paymentPromise;

    public RNCloverBridgeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext;
        reactContext.addActivityEventListener(mActivityEventListener);

        startAccountChooser();
        mPaymentConnector = new ReactPaymentConnector(mContext, account);
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        WritableMap cardEntryMethods = Arguments.createMap();
        cardEntryMethods.putInt("ICC_CONTACT", Intents.CARD_ENTRY_METHOD_ICC_CONTACT);
        cardEntryMethods.putInt("MAG_STRIPE", Intents.CARD_ENTRY_METHOD_MAG_STRIPE);
        cardEntryMethods.putInt("MANUAL", Intents.CARD_ENTRY_METHOD_MANUAL);
        cardEntryMethods.putInt("NFC_CONTACTLESS", Intents.CARD_ENTRY_METHOD_NFC_CONTACTLESS);
        cardEntryMethods.putInt("VAULTED_CARD", Intents.CARD_ENTRY_METHOD_VAULTED_CARD);

        constants.put("CARD_ENTRY_METHOD", cardEntryMethods);

        constants.put("isFlex", isFlex());
        constants.put("isMini", isMini());
        constants.put("getSpaVersion", getSpaVersion());

        return constants;
    }

    @ReactMethod
    public void print(final String imagePath, final Promise promise) {
        PrinterWrapper printerWrapper = new PrinterWrapper(promise);
        Activity currentActivity = getCurrentActivity();
        printerWrapper.print(currentActivity, account, imagePath);
    }

    @ReactMethod
    public void enableCustomerMode() { CustomerMode.enable(getCurrentActivity()); }

    @ReactMethod
    public void disableCustomerMode() {
        CustomerMode.disable(getCurrentActivity(), false);
    }

    @ReactMethod
    public void getMerchant(final Promise promise) {
        startAccountChooser();
        connect();
        if (merchantConnector != null) {
            merchantConnector.getMerchant(new MerchantCallbackTask(promise));
        } else {
            Log.d(TAG, "No merchantConnector");
            promise.resolve(null);
        }
    }

    private String getSpaVersion() {
        try {
            PackageManager pManager = mContext.getPackageManager();
            PackageInfo pInfo = pManager.getPackageInfo("com.clover.payment.executor.secure", 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "N/A";
        }
    }

    private boolean isFlex() {
        return getWidthInDP() == 360;
    }

    private boolean isMini() {
        return getWidthInDP() >= 960;
    }

    private int getWidthInDP() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        return Math.round(displayMetrics.widthPixels / displayMetrics.density);
    }

    @ReactMethod
    public void getSSID(Promise promise) {
        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ssid = wifiManager.getConnectionInfo().getSSID();
        promise.resolve(ssid);
    }

    @ReactMethod
    public void getConnectionStrength(Promise promise) {
        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int rssi = wifiManager.getConnectionInfo().getRssi();
        promise.resolve(rssi);
    }

    @ReactMethod
    @TargetApi(27)
    public void getIPAddress(Promise promise) {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getApplicationContext().getSystemService(Service.CONNECTIVITY_SERVICE);
        List addresses = connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork()).getLinkAddresses();
        String ipAddress = addresses.get(1).toString();
        promise.resolve(ipAddress);
    }

    @ReactMethod
    public static void getEthernetMacAddress(Promise promise) {
        try {
            List<NetworkInterface> allNetworkInterfaces = Collections.list(NetworkInterface
                    .getNetworkInterfaces());
            for (NetworkInterface nif : allNetworkInterfaces) {
                if (!nif.getName().equalsIgnoreCase("eth0"))
                    continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    promise.reject("404", "Not Found");
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                promise.resolve(res1.toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            promise.reject("404", "Interface Error");
        }
    }

    @ReactMethod
    public void startExternalSecurePayment(ReadableMap options, Promise promise) {
        paymentPromise = promise;
        if (options.hasKey("amount") && options.hasKey("externalService")) {
            new ExternalSecurePaymentTask(getCurrentActivity(), options).execute();
        } else {
            Log.e(TAG, "Missing amount or externalService");
            promise.reject("Error", "Missing amount or externalService");
        }
    }

    @ReactMethod
    public void startPayment(ReadableMap options, Promise promise) {
        mPaymentConnector.sale(options, promise);
    }

    @ReactMethod
    public void startManualRefund(ReadableMap options, Promise promise) {
        mPaymentConnector.manualRefund(options, promise);
    }

    @ReactMethod
    public void startPaymentRefund(ReadableMap options, Promise promise) {
        mPaymentConnector.refundPayment(options, promise);
    }

    @ReactMethod
    public void cancelSPA(Promise promise) {
        Intent intent = new Intent("com.clover.remote.terminal.securepay.action.V1_BREAK");
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            currentActivity.sendBroadcast(intent);
        }
    }

    @ReactMethod
    @TargetApi(27)
    public void startAccountChooserIfNeeded(Promise promise) {
        startAccountChooser();
        if (account != null) {
            WritableMap map = Arguments.createMap();
            map.putBoolean("success", true);
            promise.resolve(map);
        } else {
            accountPromise = promise;
            Intent accountIntent = AccountManager.newChooseAccountIntent(
                    null,
                    null,
                    new String[]{CloverAccount.CLOVER_ACCOUNT_TYPE},
                    null,
                    null,
                    null,
                    null);
            getCurrentActivity().startActivityForResult(accountIntent, REQUEST_ACCOUNT);
        }
    }

    @ReactMethod
    public void enableSound() {
        AudioManager audioManager;
        audioManager = (AudioManager) mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0);
        } else {
            audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        }
    }

    @ReactMethod
    public void disableSound() {
        AudioManager audioManager;
        audioManager = (AudioManager) mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
        } else {
            audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }
    }

    @ReactMethod
    public void isPlugged(Promise promise) {
        Intent intent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int extra = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        Boolean plugged = extra == BatteryManager.BATTERY_PLUGGED_AC || extra == BatteryManager.BATTERY_PLUGGED_USB || extra == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        promise.resolve(plugged);
    }

    private void startAccountChooser() {
        account = CloverAccount.getAccount(mContext);
    }

    private void connect() {
        disconnect();
        if (account != null) {
            merchantConnector = new MerchantConnector(mContext, account, this);
            merchantConnector.connect();
        }
    }

    private void disconnect() {
        if (merchantConnector != null) {
            merchantConnector.disconnect();
            merchantConnector = null;
        }
    }

    @Override
    public void onServiceConnected(ServiceConnector connector) {
        Log.i(TAG, "service connected: " + connector);
    }

    @Override
    public void onServiceDisconnected(ServiceConnector connector) {
        Log.i(TAG, "service disconnected: " + connector);
    }

    private final ActivityEventListener mActivityEventListener = new ActivityEventListener() {
        @Deprecated
        public void onActivityResult(int requestCode, int resultCode, Intent data) { }

        public void onActivityResult(Activity activity,  int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_ACCOUNT) {
                WritableMap map = Arguments.createMap();
                map.putBoolean("success", resultCode == RESULT_OK);
                accountPromise.resolve(map);
            } else if (requestCode == ExternalSecurePaymentTask.SECURE_PAY_REQUEST) {
                WritableMap map = Arguments.createMap();
                map.putBoolean("success", resultCode == RESULT_OK);
                paymentPromise.resolve(map);
            }
        }

        public void onNewIntent(Intent intent) { }
    };
}
