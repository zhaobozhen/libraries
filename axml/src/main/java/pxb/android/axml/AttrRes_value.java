package pxb.android.axml;

import pxb.android.Res_value;
import pxb.android.StyleSpan;

import java.util.List;

public class AttrRes_value extends Res_value {
    public static final int ID = 1;
    public static final int STYLE = 2;
    public static final int CLASS = 3;
    public final int attrValueType;

    public AttrRes_value(int attrValueType, int type, int data, String raw, List<StyleSpan> styles) {
        super(type, data, raw, styles);
        this.attrValueType = attrValueType;
    }

    public AttrRes_value(int attrValueType) {
        this.attrValueType = attrValueType;
    }
}
