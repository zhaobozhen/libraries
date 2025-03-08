package pxb.android.arsc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ConfigDetail {

    public final static byte SDK_BASE = 1;
    public final static byte SDK_BASE_1_1 = 2;
    public final static byte SDK_CUPCAKE = 3;
    public final static byte SDK_DONUT = 4;
    public final static byte SDK_ECLAIR = 5;
    public final static byte SDK_ECLAIR_0_1 = 6;
    public final static byte SDK_ECLAIR_MR1 = 7;
    public final static byte SDK_FROYO = 8;
    public final static byte SDK_GINGERBREAD = 9;
    public final static byte SDK_GINGERBREAD_MR1 = 10;
    public final static byte SDK_HONEYCOMB = 11;
    public final static byte SDK_HONEYCOMB_MR1 = 12;
    public final static byte SDK_HONEYCOMB_MR2 = 13;
    public final static byte SDK_ICE_CREAM_SANDWICH = 14;
    public final static byte SDK_ICE_CREAM_SANDWICH_MR1 = 15;
    public final static byte SDK_JELLY_BEAN = 16;
    public final static byte SDK_JELLY_BEAN_MR1 = 17;
    public final static byte SDK_JELLY_BEAN_MR2 = 18;
    public final static byte SDK_KITKAT = 19;
    public final static byte SDK_LOLLIPOP = 21;
    public int mcc;
    public int mnc;
    public String language;
    public String country;
    public ORIENTATION orientation;
    public TOUCHSCREEN touchscreen;
    public DENSITY density;
    public KEYBOARD keyboard;
    public NAVIGATION navigation;
    public int screenWidth; // 0 for any
    public int screenHeight; // 0 for any
    public int sdkVersion;
    public SCREENSIZE screensize;
    public SCREENLONG screenlong;
    public LAYOUTDIR layoutdir;
    public UI_MODE_TYPE uiModeType;
    public UI_MODE_NIGHT uiModeNight;
    public int smallestScreenWidthDp;
    public int screenWidthDp;
    public int screenHeightDp;
    public byte[] extra;
    public KEYSHIDDEN keyshidden;
    public NAVHIDDEN navhidden;
    public ConfigDetail(byte[] data) {
        init(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
    }

    public ConfigDetail() {
        language = "any";
        country = "any";
        orientation = ORIENTATION.ANY;
        touchscreen = TOUCHSCREEN.ANY;
        density = DENSITY.DEFAULT;
        keyboard = KEYBOARD.ANY;
        navigation = NAVIGATION.ANY;
        keyshidden = KEYSHIDDEN.ANY;
        navhidden = NAVHIDDEN.ANY;
        screensize = SCREENSIZE.ANY;
        screenlong = SCREENLONG.ANY;
        layoutdir = LAYOUTDIR.ANY;
        uiModeType = UI_MODE_TYPE.ANY;
        uiModeNight = UI_MODE_NIGHT.ANY;
    }
    public byte[] toId() {
        int size = 36; //default size;
        if (extra != null) {
            size += extra.length;
        }
        ByteBuffer out = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

        out.putInt(size).putShort((short) mcc).putShort((short) mnc);
        if (this.language.equals("any")) {
            out.put((byte) 0).put((byte) 0);
        } else {
            out.put((byte) this.language.charAt(0)).put((byte) this.language.charAt(1));
        }
        if (this.country.equals("any")) {
            out.put((byte) 0).put((byte) 0);
        } else {
            out.put((byte) this.country.charAt(0)).put((byte) this.country.charAt(1));
        }
        out.put((byte) orientation.ordinal()).put((byte) touchscreen.ordinal()).putShort((short) density.value);

        out.put((byte) keyboard.ordinal()).put((byte) navigation.ordinal())
                .put((byte) (keyshidden.ordinal() & (navhidden.ordinal() << 2))).put((byte) 0);

        out.putShort((short) screenWidth).putShort((short) screenHeight);

        out.putShort((short) sdkVersion).putShort((short) 0);

        out.put((byte) (screensize.ordinal() & (screenlong.ordinal() << 4) & (layoutdir.ordinal() << 6)));
        out.put((byte) (uiModeType.ordinal() & (uiModeNight.ordinal() << 4)));
        out.putShort((short) smallestScreenWidthDp);

        out.putShort((short) screenWidthDp).putShort((short) screenHeightDp);
        if (extra != null) {
            out.put(extra);
        }
        return out.array();
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        if (mcc != 0) {
            ret.append("-mcc").append(String.format("%03d", mcc));
        }
        if (mnc != 0) {
            ret.append("-mnc");
            if (mnc > 0 && mnc < 10) {
                ret.append(String.format("%02d", mnc));
            } else {
                ret.append(String.format("%03d", mnc));
            }
        }
        if (!language.equals("any")) {
            ret.append('-').append(language);
        }
        if (!country.equals("any")) {
            ret.append("-r").append(country);
        }
        switch (layoutdir) {
            case RTL:
                ret.append("-ldrtl");
                break;
            case LTR:
                ret.append("-ldltr");
                break;
        }
        if (smallestScreenWidthDp != 0) {
            ret.append("-sw").append(smallestScreenWidthDp).append("dp");
        }
        if (screenWidthDp != 0) {
            ret.append("-w").append(screenWidthDp).append("dp");
        }
        if (screenHeightDp != 0) {
            ret.append("-h").append(screenHeightDp).append("dp");
        }
        switch (screensize) {
            case SMALL:
                ret.append("-small");
                break;
            case NORMAL:
                ret.append("-normal");
                break;
            case LARGE:
                ret.append("-large");
                break;
            case XLARGE:
                ret.append("-xlarge");
                break;
        }
        switch (screenlong) {
            case YES:
                ret.append("-long");
                break;
            case NO:
                ret.append("-notlong");
                break;
        }
        switch (orientation) {
            case PORT:
                ret.append("-port");
                break;
            case LAND:
                ret.append("-land");
                break;
            case SQUARE:
                ret.append("-square");
                break;
        }
        switch (uiModeType) {
            case CAR:
                ret.append("-car");
                break;
            case DESK:
                ret.append("-desk");
                break;
            case TELEVISION:
                ret.append("-television");
                break;
            case SMALLUI:
                ret.append("-smallui");
                break;
            case MEDIUMUI:
                ret.append("-mediumui");
                break;
            case LARGEUI:
                ret.append("-largeui");
                break;
            case HUGEUI:
                ret.append("-hugeui");
                break;
            case APPLIANCE:
                ret.append("-appliance");
                break;
            case WATCH:
                ret.append("-watch");
                break;
        }
        switch (uiModeNight) {
            case YES:
                ret.append("-night");
                break;
            case NO:
                ret.append("-notnight");
                break;
        }
        switch (density) {
            case DEFAULT:
                break;
            case LOW:
                ret.append("-ldpi");
                break;
            case MEDIUM:
                ret.append("-mdpi");
                break;
            case HIGH:
                ret.append("-hdpi");
                break;
            case TV:
                ret.append("-tvdpi");
                break;
            case XHIGH:
                ret.append("-xhdpi");
                break;
            case XXHIGH:
                ret.append("-xxhdpi");
                break;
            case XXXHIGH:
                ret.append("-xxxhdpi");
                break;
            case ANY:
                ret.append("-anydpi");
                break;
            case NONE:
                ret.append("-nodpi");
                break;
            default:
                ret.append('-').append(density).append("dpi");
        }
        switch (touchscreen) {
            case NOTOUCH:
                ret.append("-notouch");
                break;
            case STYLUS:
                ret.append("-stylus");
                break;
            case FINGER:
                ret.append("-finger");
                break;
        }
        switch (keyshidden) {
            case NO:
                ret.append("-keysexposed");
                break;
            case YES:
                ret.append("-keyshidden");
                break;
            case SOFT:
                ret.append("-keyssoft");
                break;
        }
        switch (keyboard) {
            case NOKEYS:
                ret.append("-nokeys");
                break;
            case QWERTY:
                ret.append("-qwerty");
                break;
            case _12KEY:
                ret.append("-12key");
                break;
        }
        switch (navhidden) {
            case NO:
                ret.append("-navexposed");
                break;
            case YES:
                ret.append("-navhidden");
                break;
        }
        switch (navigation) {
            case NONAV:
                ret.append("-nonav");
                break;
            case DPAD:
                ret.append("-dpad");
                break;
            case TRACKBALL:
                ret.append("-trackball");
                break;
            case WHEEL:
                ret.append("-wheel");
                break;
        }
        if (screenWidth != 0 && screenHeight != 0) {
            if (screenWidth > screenHeight) {
                ret.append(String.format("-%dx%d", screenWidth, screenHeight));
            } else {
                ret.append(String.format("-%dx%d", screenHeight, screenWidth));
            }
        }
        if (sdkVersion > getNaturalSdkVersionRequirement()) {
            ret.append("-v").append(sdkVersion);
        }

        return ret.toString();
    }

    private short getNaturalSdkVersionRequirement() {
        if (density == DENSITY.ANY) {
            return SDK_LOLLIPOP;
        }
        if (smallestScreenWidthDp != 0 || screenWidthDp != 0 || screenHeightDp != 0) {
            return SDK_HONEYCOMB_MR2;
        }
        if (uiModeNight != UI_MODE_NIGHT.ANY || uiModeType != UI_MODE_TYPE.ANY) {
            return SDK_FROYO;
        }
        if ((screensize != SCREENSIZE.ANY || screenlong != SCREENLONG.ANY) || density != DENSITY.DEFAULT) {
            return SDK_DONUT;
        }
        return 0;
    }

    private void init(ByteBuffer in) {
        int size = in.getInt();
        if (size < 28) {
            throw new RuntimeException();
        }
        mcc = 0xFFFF & in.getShort();
        mnc = 0xFFFF & in.getShort();

        char[] language = new char[]{(char) in.get(), (char) in.get()};
        if (language[0] == 0 && language[1] == 0) {
            this.language = "any";
        } else {
            this.language = new String(language);
        }
        char[] country = new char[]{(char) in.get(), (char) in.get()};
        if (country[0] == 0 && country[1] == 0) {
            this.country = "any";
        } else {
            this.country = new String(country);
        }

        orientation = ORIENTATION.values()[0xFF & in.get()];
        touchscreen = TOUCHSCREEN.values()[0xFF & in.get()];
        density = DENSITY.from(0xFFFF & in.getShort());

        keyboard = KEYBOARD.values()[0xFF & in.get()];
        navigation = NAVIGATION.values()[0xFF & in.get()];
        byte inputFlags = in.get();
        byte inputPad0 = in.get();
        keyshidden = KEYSHIDDEN.values()[inputFlags & 0x0003];
        navhidden = NAVHIDDEN.values()[(inputFlags >> 2) & 0x0003];

        screenWidth = 0xFFFF & in.getShort();
        screenHeight = 0xFFFF & in.getShort();

        sdkVersion = 0xFFFF & in.getShort();
        in.getShort(); //minorVersion

        int screenLayout;
        int uiMode;

        screenLayout = 0xFF & in.get();
        uiMode = 0xFF & in.get();
        smallestScreenWidthDp = 0xFFFF & in.getShort();
        screensize = SCREENSIZE.values()[screenLayout & 0x000f];
        screenlong = SCREENLONG.values()[(screenLayout >> 4) & 0x0003];
        layoutdir = LAYOUTDIR.values()[(screenLayout >> 6) & 0x000C];
        uiModeType = UI_MODE_TYPE.values()[uiMode & 0x0F];
        uiModeNight = UI_MODE_NIGHT.values()[(uiMode >> 4) & 0x03];

        screenWidthDp = in.getShort();
        screenHeightDp = in.getShort();

        if (in.position() < size) {
            extra = new byte[size - in.position()];
            in.get(extra);
        }
    }

    public enum ORIENTATION {
        ANY, PORT, LAND, SQUARE;
    }

    public enum TOUCHSCREEN {
        ANY, NOTOUCH, STYLUS, FINGER;
    }

    public enum DENSITY {
        DEFAULT(0), LOW(120), MEDIUM(160), TV(213),
        HIGH(240), XHIGH(320), XXHIGH(480), XXXHIGH(640), ANY(0xFFFe), NONE(0xFFFF);
        int value;

        DENSITY(int v) {
            this.value = v;
        }

        static DENSITY from(int v) {
            switch (v) {
                case 0:
                    return DEFAULT;
                case 120:
                    return LOW;
                case 160:
                    return MEDIUM;
                case 213:
                    return TV;
                case 240:
                    return HIGH;
                case 320:
                    return XHIGH;
                case 480:
                    return XXHIGH;
                case 640:
                    return XXXHIGH;
                case 0xFFFe:
                    return ANY;
                case 0xFFFF:
                    return NONE;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    public enum KEYBOARD {
        ANY, NOKEYS, QWERTY, _12KEY;
    }

    public enum NAVIGATION {
        ANY, NONAV, DPAD, TRACKBALL, WHEEL;

    }

    public enum KEYSHIDDEN {
        ANY, NO, YES, SOFT;
    }

    public enum NAVHIDDEN {
        ANY, NO, YES
    }

    public enum SCREENSIZE {
        ANY, SMALL, NORMAL, LARGE, XLARGE;
    }

    public enum SCREENLONG {
        ANY, NO, YES;
    }

    public enum LAYOUTDIR {
        ANY, LTR, RTL;
    }

    public enum UI_MODE_TYPE {
        ANY, NORMAL, DESK, CAR, TELEVISION, APPLIANCE, WATCH,
        T07, T08, T09, T0a, T0b,
        // miui
        SMALLUI, MEDIUMUI, LARGEUI, HUGEUI
        // end - miui
        ;
    }

    public enum UI_MODE_NIGHT {
        ANY, NO, YES;
    }
}
