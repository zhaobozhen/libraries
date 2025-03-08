/*
 * Copyright (c) 2009-2013 Panxiaobo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pxb.android;

import java.util.List;

public class Res_value {

    // The 'data' is either 0 or 1, specifying this resource is either
    // undefined or empty, respectively.
    public static final int TYPE_NULL = 0x00,
    // The 'data' holds a ResTable_ref, a reference to another resource
    // table entry.
    TYPE_REFERENCE = 0x01,
    // The 'data' holds an attribute resource identifier.
    TYPE_ATTRIBUTE = 0x02,
    // The 'data' holds an index into the containing resource table's
    // global value string pool.
    TYPE_STRING = 0x03,
    // The 'data' holds a single-precision floating point number.
    TYPE_FLOAT = 0x04,
    // The 'data' holds a complex number encoding a dimension value,
    // such as "100in".
    TYPE_DIMENSION = 0x05,
    // The 'data' holds a complex number encoding a fraction of a
    // container.
    TYPE_FRACTION = 0x06,
    // The 'data' holds a dynamic ResTable_ref, which needs to be
    // resolved before it can be used like a TYPE_REFERENCE.
    TYPE_DYNAMIC_REFERENCE = 0x07,

    // Beginning of integer flavors...
    TYPE_FIRST_INT = 0x10,

    // The 'data' is a raw integer value of the form n..n.
    TYPE_INT_DEC = 0x10,
    // The 'data' is a raw integer value of the form 0xn..n.
    TYPE_INT_HEX = 0x11,
    // The 'data' is either 0 or 1, for input "false" or "true" respectively.
    TYPE_INT_BOOLEAN = 0x12,

    // Beginning of color integer flavors...
    TYPE_FIRST_COLOR_INT = 0x1c,

    // The 'data' is a raw integer value of the form #aarrggbb.
    TYPE_INT_COLOR_ARGB8 = 0x1c,
    // The 'data' is a raw integer value of the form #rrggbb.
    TYPE_INT_COLOR_RGB8 = 0x1d,
    // The 'data' is a raw integer value of the form #argb.
    TYPE_INT_COLOR_ARGB4 = 0x1e,
    // The 'data' is a raw integer value of the form #rgb.
    TYPE_INT_COLOR_RGB4 = 0x1f,

    // ...end of integer flavors.
    TYPE_LAST_COLOR_INT = 0x1f,

    // ...end of integer flavors.
    TYPE_LAST_INT = 0x1f;


    public int data;
    public String raw;
    public List<StyleSpan> styles;
    public int type;

    public Res_value() {
    }

    public Res_value(int type, int data, String raw, List<StyleSpan> styles) {
        super();
        this.type = type;
        this.data = data;
        this.raw = raw;
        this.styles = styles;
    }

    public static Res_value newStringValue(String str) {
        return new Res_value(TYPE_STRING, -1, str, null);
    }

    String toXml() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            for (StyleSpan style : styles) {
                if (style.start == i) {
                    sb.append("<").append(style.name).append(">");
                }
            }
            sb.append(raw.charAt(i));
            for (StyleSpan style : styles) {
                if (style.end == i) {
                    sb.append("</").append(style.name).append(">");
                }
            }
        }
        return sb.toString();
    }

    public String toString() {
        switch (type) {
            case TYPE_STRING:
                return styles != null ? toXml() : raw;
            case TYPE_NULL:
                return data == 0 ? "[undefined]" : "[empty]";
            case TYPE_ATTRIBUTE:
                return String.format("?0x%08x", data);
            case TYPE_INT_BOOLEAN:
                return String.valueOf(data != 0);
            case TYPE_REFERENCE:
                return String.format("@0x%08x", data);
            case TYPE_FLOAT:
                return String.valueOf(Float.intBitsToFloat(data));
            case TYPE_INT_COLOR_ARGB8:
                return String.format("#%08x", data);
            case TYPE_INT_COLOR_RGB8:
                return String.format("#%06x", data & 0x00FFFFFF);
            case TYPE_INT_COLOR_ARGB4:
                return String.format("#%04x", data & 0x0000FFFF);
            case TYPE_INT_COLOR_RGB4:
                return String.format("#%03x", data & 0x00000FFF);
            default:
                if (type >= TYPE_FIRST_INT && type <= TYPE_LAST_INT) {
                    return String.valueOf(data);
                }
                return String.format("{t=0x%02x d=0x%08x}", type, data);
        }
    }

    public static Res_value newTrue() {
        return new Res_value(TYPE_INT_BOOLEAN, -1, null, null);
    }

    public static Res_value newFalse() {
        return new Res_value(TYPE_INT_BOOLEAN, 0, null, null);
    }
    public static Res_value newDecInt(int i) {
        return new Res_value(TYPE_INT_DEC, i, null, null);
    }
    public static Res_value newHexInt(int i) {
        return new Res_value(TYPE_INT_HEX, i, null, null);
    }
    public static Res_value newReference(int i) {
        return new Res_value(TYPE_REFERENCE, i, null, null);
    }
    public static Res_value newFloat(float i) {
        return new Res_value(TYPE_FLOAT, Float.floatToIntBits(i), null, null);
    }
    public static Res_value newUnDefined() {
        return new Res_value(TYPE_NULL, 0, null, null);
    }
    public static Res_value newEmpty() {
        return new Res_value(TYPE_NULL, 1, null, null);
    }

    public static Res_value newColorARGB8(int a, int r, int g, int b) {
        int value = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                ((b & 0xFF) << 0);
        return new Res_value(TYPE_INT_COLOR_ARGB8, value, null, null);
    }

    public static Res_value newColorRGB8(int r, int g, int b) {
        int value = ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                ((b & 0xFF) << 0);
        return new Res_value(TYPE_INT_COLOR_RGB8, value, null, null);
    }

    public static Res_value newColorARGB4(int a, int r, int g, int b) {
        int value = ((a & 0xF) << 12) |
                ((r & 0xF) << 8) |
                ((g & 0xF) << 4) |
                ((b & 0xF) << 0);
        return new Res_value(TYPE_INT_COLOR_ARGB4, value, null, null);
    }

    public static Res_value newColorRGB4(int r, int g, int b) {
        int value = ((r & 0xF) << 8) |
                ((g & 0xF) << 4) |
                ((b & 0xF) << 0);
        return new Res_value(TYPE_INT_COLOR_RGB4, value, null, null);
    }


}
