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
package pxb.android.axml;

import pxb.android.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * a class to read android axml
 * 
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 */
public class AxmlParser implements ResConst {

    public static final int END_FILE = 7;
    public static final int END_NS = 5;
    public static final int END_TAG = 3;
    public static final int START_FILE = 1;
    public static final int START_NS = 4;
    public static final int START_TAG = 2;
    public static final int TEXT = 6;
    // private int attrName[];
    // private int attrNs[];
    // private int attrResId[];
    // private int attrType[];
    // private Object attrValue[];

    private int attributeCount;

    private ByteBuffer attrs;

    private int classAttribute;
    private int fileSize = -1;
    private int idAttribute;
    private ByteBuffer in;
    private int lineNumber;
    private int nameIdx;
    private int nsIdx;
    private int commentIdx;
    private int prefixIdx;

    StringBlock stringBlock = new StringBlock();

    private int styleAttribute;

    private int textIdx;
    private int attributeSize;
    private int typedTextPos;

    public AxmlParser(byte[] data) {
        this(ByteBuffer.wrap(data));
    }

    public AxmlParser(ByteBuffer in) {
        super();
        this.in = in.order(ByteOrder.LITTLE_ENDIAN);
    }

    public String getComment() {
        return commentIdx < 0 ? null : stringBlock.get(commentIdx);
    }

    public int getAttrCount() {
        return attributeCount;
    }

    public int getAttributeCount() {
        return attributeCount;
    }

    public String getAttrName(int i) {
        int idx = attrs.getInt(i * attributeSize + 1 * 4);
        return stringBlock.get(idx);

    }


    public String getAttrNs(int i) {
        int idx = attrs.getInt(i * attributeSize + 0 * 4);
        return idx >= 0 ? stringBlock.get(idx) : null;
    }

    String getAttrRawString(int i) {
        int idx = attrs.getInt(i * attributeSize + 2 * 4);
        if (idx >= 0) {
            return stringBlock.get(idx);
        }
        return null;
    }

    public int getAttrResId(int i) {
        if (stringBlock.resourceIds != null) {
            int idx = attrs.getInt(i * attributeSize + 1 * 4);
            if (idx >= 0 && idx < stringBlock.resourceIds.length) {
                return stringBlock.resourceIds[idx];
            }
        }
        return NO_RESOURCE_ID;
    }

    public Res_value getAttrResValue(int i) {
        int pos = i * attributeSize + 4 * 3;
        int size1 = attrs.getShort(pos);// 8
        int zero = attrs.get(pos + 2);// 0
        int type = attrs.get(pos + 3) & 0xFF; // TypedValue.*
        int data = attrs.getInt(pos + 4);
        String raw = null;
        List<StyleSpan> xstyles = null;
        if (type == Res_value.TYPE_STRING && data >= 0) {
            raw = stringBlock.get(data);
            xstyles = stringBlock.getStyles(data);
        }
        if (i == idAttribute) {
            return new AttrRes_value(AttrRes_value.ID, type, data, raw, xstyles);
        } else if (i == styleAttribute) {
            return new AttrRes_value(AttrRes_value.STYLE, type, data, raw, xstyles);
        } else if (i == classAttribute) {
            return new AttrRes_value(AttrRes_value.CLASS, type, data, raw, xstyles);
        }
        return new Res_value(type, data, raw, xstyles);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getName() {
        return stringBlock.get(nameIdx);
    }

    public String getNamespacePrefix() {
        return stringBlock.get(prefixIdx);
    }

    public String getNamespaceUri() {
        return nsIdx >= 0 ? stringBlock.get(nsIdx) : null;
    }

    public String getText() {
        return stringBlock.get(textIdx);
    }
    public Res_value getTypedText(){
        int pos = typedTextPos;
        int size1 = in.getShort(pos);// 8
        int zero = in.get(pos + 2);// 0
        int type = in.get(pos + 3) & 0xFF; // TypedValue.*
        int data = in.getInt(pos + 4);
        String raw = null;
        List<StyleSpan> xstyles = null;
        if (type == Res_value.TYPE_STRING) {
            raw = stringBlock.get(data);
            xstyles = stringBlock.getStyles(data);
        }
        return new Res_value(type, data, raw, xstyles);
    }

    public int next() throws IOException {
        if (fileSize < 0) {
            int start = in.position();
            ResChunk_header treeHeader = new ResChunk_header(in);
            if (treeHeader.type != RES_XML_TYPE) {
                throw new RuntimeException();
            }
            fileSize = start + treeHeader.size;
            int savePoint = start + treeHeader.headSize;
            in.position(savePoint);
            for (int p = in.position(); p < fileSize; p = in.position()) {
                ResChunk_header header = new ResChunk_header(in);
                switch (header.type) {
                    case RES_STRING_POOL_TYPE:
                        stringBlock.read(in, header);
                        break;
                    case RES_XML_RESOURCE_MAP_TYPE:
                        stringBlock.readResourceIdTable(in, header);
                        break;
                }
                in.position(p + header.size);
            }
            in.position(savePoint);

            return START_FILE;
        }
        int event = -1;
        for (int p = in.position(); p < fileSize; p = in.position()) {
            ResChunk_header header = new ResChunk_header(in);

            switch (header.type) {
                case RES_XML_START_ELEMENT_TYPE:
                case RES_XML_END_ELEMENT_TYPE:
                case RES_XML_START_NAMESPACE_TYPE:
                case RES_XML_END_NAMESPACE_TYPE:
                case RES_XML_CDATA_TYPE:
                    lineNumber = in.getInt();
                    commentIdx = in.getInt();
                    break;
                default:
            }

            in.position(p + header.headSize);
            switch (header.type) {
            case RES_XML_START_ELEMENT_TYPE: {
                int baseP = in.position();
                nsIdx = in.getInt();
                nameIdx = in.getInt();
                int attributeStart = in.getShort() & 0xFFFF;
                attributeSize = in.getShort() & 0xFFFF;
                attributeCount = in.getShort() & 0xFFFF;
                idAttribute = (in.getShort() & 0xFFFF) - 1;
                classAttribute = (in.getShort() & 0xFFFF) - 1;
                styleAttribute = (in.getShort() & 0xFFFF) - 1;

                in.position(baseP + attributeStart);
                attrs = in.slice().order(ByteOrder.LITTLE_ENDIAN);

                event = START_TAG;
            }
                break;
            case RES_XML_END_ELEMENT_TYPE: {
                event = END_TAG;
            }
                break;
            case RES_XML_START_NAMESPACE_TYPE:
                prefixIdx = in.getInt();
                nsIdx = in.getInt();
                event = START_NS;
                break;
            case RES_XML_END_NAMESPACE_TYPE:
                event = END_NS;
                break;
            case RES_XML_CDATA_TYPE:
                textIdx = in.getInt();
                typedTextPos = in.position();
                event = TEXT;
                break;
            default:
            case RES_STRING_POOL_TYPE:
            case RES_XML_RESOURCE_MAP_TYPE:
                in.position(p + header.size);
                continue;
            }
            in.position(p + header.size);
            return event;
        }
        return END_FILE;
    }
}
