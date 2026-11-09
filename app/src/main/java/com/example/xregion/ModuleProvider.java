package com.example.xregion;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

public class ModuleProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.xregion.config";
    private static final String TARGET_PACKAGE = "com.twitter.android";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle out = new Bundle();
        Context ctx = getContext();
        if (ctx == null) {
            out.putBoolean("denied", true);
            return out;
        }
        if (!isCallerAllowed(ctx)) {
            out.putBoolean("denied", true);
            return out;
        }
        out.putBoolean("denied", false);
        if ("get_config".equals(method)) {
            out.putString("iso", ConfigStore.getIso(ctx));
            out.putBoolean("fake_sim", ConfigStore.getFakeSim(ctx));
            out.putBoolean("fake_service_state", ConfigStore.getFakeServiceState(ctx));
            out.putBoolean("hit_log", ConfigStore.getHitLog(ctx));
        } else if ("log".equals(method)) {
            ConfigStore.appendLog(ctx, arg);
            out.putBoolean("ok", true);
        } else {
            out.putBoolean("ok", false);
        }
        return out;
    }

    private boolean isCallerAllowed(Context ctx) {
        int uid;
        try {
            uid = Binder.getCallingUid();
        } catch (Throwable t) {
            return false;
        }
        if (uid == Process.myUid()) {
            return true;
        }
        try {
            PackageManager pm = ctx.getPackageManager();
            String[] pkgs = pm.getPackagesForUid(uid);
            if (pkgs != null) {
                for (String p : pkgs) {
                    if (TARGET_PACKAGE.equals(p)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
