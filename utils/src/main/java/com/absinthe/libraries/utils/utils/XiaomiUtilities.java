package com.absinthe.libraries.utils.utils;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.Method;

public class XiaomiUtilities {

  public static final String TAG = "XiaomiUtilities";

  // custom permissions
  public static final int OP_WIFI_CHANGE = 10001;
  public static final int OP_BLUETOOTH_CHANGE = 10002;
  public static final int OP_DATA_CONNECT_CHANGE = 10003;
  public static final int OP_SEND_MMS = 10004;
  public static final int OP_READ_MMS = 10005;
  public static final int OP_WRITE_MMS = 10006;
  public static final int OP_BOOT_COMPLETED = 10007;
  public static final int OP_AUTO_START = 10008;
  public static final int OP_NFC_CHANGE = 10009;
  public static final int OP_DELETE_SMS = 10010;
  public static final int OP_DELETE_MMS = 10011;
  public static final int OP_DELETE_CONTACTS = 10012;
  public static final int OP_DELETE_CALL_LOG = 10013;
  public static final int OP_EXACT_ALARM = 10014;
  public static final int OP_ACCESS_XIAOMI_ACCOUNT = 10015;
  public static final int OP_NFC = 10016;
  public static final int OP_INSTALL_SHORTCUT = 10017;
  public static final int OP_READ_NOTIFICATION_SMS = 10018;
  public static final int OP_GET_TASKS = 10019;
  public static final int OP_SHOW_WHEN_LOCKED = 10020;
  public static final int OP_BACKGROUND_START_ACTIVITY = 10021;
  public static final int OP_GET_INSTALLED_APPS = 10022;
  public static final int OP_SERVICE_FOREGROUND = 10023;
  public static final int OP_GET_ANONYMOUS_ID = 10024;
  public static final int OP_GET_UDEVICE_ID = 10025;
  public static final int OP_SHOW_DEAMON_NOTIFICATION = 10026;
  public static final int OP_BACKGROUND_LOCATION = 10027;
  public static final int OP_READ_SMS_REAL = 10028;
  public static final int OP_READ_CONTACTS_REAL = 10029;
  public static final int OP_READ_CALENDAR_REAL = 10030;
  public static final int OP_READ_CALL_LOG_REAL = 10031;
  public static final int OP_READ_PHONE_STATE_REAL = 10032;
  public static final int OP_ACCESS_GALLERY = 10034;
  public static final int OP_ACCESS_SOCIALITY = 10035;

  public static boolean isMIUI() {
    return !TextUtils.isEmpty(Utility.INSTANCE.getSystemProperty("ro.miui.ui.version.name"));
  }

  @SuppressWarnings("JavaReflectionMemberAccess")
  @TargetApi(19)
  public static boolean isCustomPermissionGranted(int permission) {
    try {
      AppOpsManager mgr = (AppOpsManager) Utility.INSTANCE.getAppContext().getSystemService(Context.APP_OPS_SERVICE);
      Method m = AppOpsManager.class.getMethod("checkOpNoThrow", int.class, int.class, String.class);
      int result = (int) m.invoke(mgr, permission, android.os.Process.myUid(), Utility.INSTANCE.getAppContext().getPackageName());
      return result == AppOpsManager.MODE_ALLOWED;
    } catch (Exception x) {
      Log.d(TAG, "isCustomPermissionGranted: " + x);
    }
    return true;
  }

  public static int getMIUIMajorVersion() {
    String prop = Utility.INSTANCE.getSystemProperty("ro.miui.ui.version.name");
    if (prop != null) {
      try {
        return Integer.parseInt(prop.replace("V", ""));
      } catch (NumberFormatException ignore) {
      }
    }
    return -1;
  }

  public static Intent getPermissionManagerIntent() {
    Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
    intent.putExtra("extra_package_uid", android.os.Process.myUid());
    intent.putExtra("extra_pkgname", Utility.INSTANCE.getAppContext().getPackageName());
    return intent;
  }

  private static Class<?> clazz;

  static {
    try {
      clazz = Class.forName("miui.os.Build");
    } catch (ClassNotFoundException e) {
      clazz = null;
    }
  }

  private boolean isMiuiStableBuild() {
    if (clazz == null) return false;
    try {
      Field stableField = clazz.getField("IS_STABLE_VERSION");
      Boolean isStable = (Boolean) stableField.get(null);
      return isStable != null && isStable;
    } catch (NoSuchFieldException | ClassCastException | IllegalAccessException e) {
      return false;
    }
  }

  private boolean isMiuiAlphaBuild() {
    if (clazz == null) return false;
    try {
      Field alphaField = clazz.getField("IS_ALPHA_BUILD");
      Boolean isAlpha = (Boolean) alphaField.get(null);
      return isAlpha != null && isAlpha;
    } catch (NoSuchFieldException | ClassCastException | IllegalAccessException e) {
      return false;
    }
  }

  private boolean isMiuiDevelopBuild() {
    return !isMiuiStableBuild() && !isMiuiAlphaBuild();
  }
}
