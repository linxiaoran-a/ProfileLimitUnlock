package com.linxiaoran.profilelimitunlock;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    static final String PKG = "com.linxiaoran.profilelimitunlock";
    static final String PREFS = "profile_limit";

    static final String KEY_PROFILE_LIMIT = "max_profiles_per_parent";
    static final String KEY_MAX_USERS = "max_users";
    static final String KEY_MAX_RUNNING = "max_running_users";

    static final int DEFAULT_PROFILE = 99;
    static final int DEFAULT_MAX_USERS = 99;
    static final int DEFAULT_MAX_RUNNING = 99;

    static final int MIN_VALUE = 1;
    static final int MAX_VALUE = 999;

    private static final String UMS = "com.android.server.pm.UserManagerService";

    private static XSharedPreferences prefs;

    private static int read(String key, int def) {
        try {
            if (prefs == null) {
                prefs = new XSharedPreferences(PKG, PREFS);
            }
            prefs.reload();
            return prefs.getInt(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"android".equals(lpparam.packageName)) {
            return;
        }
        ClassLoader cl = lpparam.classLoader;

        // 1. MANAGED profile per-parent limit
        try {
            Class<?> userTypeDetails = XposedHelpers.findClass("com.android.server.pm.UserTypeDetails", cl);
            XposedHelpers.findAndHookMethod(UMS, cl, "getMaxUsersOfTypePerParent",
                    userTypeDetails, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(read(KEY_PROFILE_LIMIT, DEFAULT_PROFILE));
                        }
                    });
            XposedBridge.log("ProfileLimitUnlock: hooked getMaxUsersOfTypePerParent");
        } catch (Throwable t) {
            XposedBridge.log("ProfileLimitUnlock: hook getMaxUsersOfTypePerParent failed: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(UMS, cl, "canAddMoreProfilesToUser",
                    String.class, int.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (read(KEY_PROFILE_LIMIT, DEFAULT_PROFILE) > 1) {
                                param.setResult(true);
                            }
                        }
                    });
            XposedBridge.log("ProfileLimitUnlock: hooked canAddMoreProfilesToUser");
        } catch (Throwable t) {
            XposedBridge.log("ProfileLimitUnlock: hook canAddMoreProfilesToUser failed: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(UMS, cl, "getRemainingCreatableProfileCount",
                    String.class, int.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (read(KEY_PROFILE_LIMIT, DEFAULT_PROFILE) > 1) {
                                param.setResult(read(KEY_PROFILE_LIMIT, DEFAULT_PROFILE));
                            }
                        }
                    });
            XposedBridge.log("ProfileLimitUnlock: hooked getRemainingCreatableProfileCount");
        } catch (Throwable t) {
            XposedBridge.log("ProfileLimitUnlock: hook getRemainingCreatableProfileCount failed: " + t);
        }

        // 2. Global max users (config_multiuserMaximumUsers)
        try {
            XposedHelpers.findAndHookMethod(UMS, cl, "getMaxUsers",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(read(KEY_MAX_USERS, DEFAULT_MAX_USERS));
                        }
                    });
            XposedBridge.log("ProfileLimitUnlock: hooked getMaxUsers");
        } catch (Throwable t) {
            XposedBridge.log("ProfileLimitUnlock: hook getMaxUsers failed: " + t);
        }

        // 3. Max running users (config_multiuserMaxRunningUsers)
        try {
            XposedHelpers.findAndHookMethod(UMS, cl, "getMaxRunningUsers",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(read(KEY_MAX_RUNNING, DEFAULT_MAX_RUNNING));
                        }
                    });
            XposedBridge.log("ProfileLimitUnlock: hooked getMaxRunningUsers");
        } catch (Throwable t) {
            XposedBridge.log("ProfileLimitUnlock: hook getMaxRunningUsers failed: " + t);
        }
    }
}
