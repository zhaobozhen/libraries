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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;

import static pxb.android.ResChunk_header.writeChunkHeader;
import static pxb.android.ResConst.RES_STRING_POOL_TYPE;
import static pxb.android.ResConst.RES_XML_RESOURCE_MAP_TYPE;

@SuppressWarnings("serial")
public class StringBlockWriter {
    public int wItemCount = -1;
    private byte[] styleData;
    private byte[] stringData;
    private Map<StyleItem, StyleItem> styleItemMap = new HashMap<StyleItem, StyleItem>();
    private static final StyleSpan[] noSpan = new StyleSpan[0];
    private Map<String, StringItem> wUniq = new HashMap<String, StringItem>();
    private Map<String, StringItem> wResUniq = new HashMap<String, StringItem>();
    private Map<StringItem, StringItem> wStyleUniq = new HashMap<StringItem, StringItem>();
    private List<StringItem> wJoinItems = null;
    private StyleItem key = new StyleItem();
    private boolean useUTF8 = true;
    private int wStyleCount = 0;

    static void paddingTo4x(ByteArrayOutputStream bos) {
        int stringSize = bos.size();

        if (stringSize % 4 != 0) {
            int padding = 4 - stringSize % 4;
            for (int i = 0; i < padding; i++) {
                bos.write(0);
            }
        }
    }

    static void writeLeInt(OutputStream os, int i) throws IOException {
        os.write(i);
        os.write(i >> 8);
        os.write(i >> 16);
        os.write(i >> 24);
    }

    public StringBlockWriter() {
    }

    public void of(List<String> strs) {
        wJoinItems = new ArrayList<StringItem>(strs.size());
        for (String str : strs) {
            wJoinItems.add(new StringItem(str == null ? "?" : str));
        }
    }

    public StringItem add(String key) {
        if (key == null) {
            throw new RuntimeException();
        }
        StringItem v = wUniq.get(key);
        if (v == null) {
            v = new StringItem(key);
            wUniq.put(key, v);
            return v;
        }
        return v;
    }

    /**
     * with head and padding
     * 
     * @return
     */
    public int getStringPoolSectionSize() {
        int size = 8 + 5 * 4 + wJoinItems.size() * 4 + stringData.length;
        if (wStyleCount > 0) {
            size += wStyleCount * 4 + styleData.length;
        }
        return size;
    }

    public void setUseUTF8(boolean useUTF8) {
        this.useUTF8 = useUTF8;
    }

    public void prepare() throws IOException {
        if (wJoinItems == null) {
            wJoinItems = new ArrayList<StringItem>(wResUniq.size() + wUniq.size() + wStyleUniq.size());
        }
        wJoinItems.addAll(wResUniq.values());
        wJoinItems.addAll(wStyleUniq.values());
        wJoinItems.addAll(wUniq.values());
        wItemCount = wUniq.size();
        if (useUTF8) {
            for (StringItem s : wJoinItems) {
                if (s.data.length() > 0x7FFF) {
                    useUTF8 = false;
                }
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = 0;
        int offset = 0;
        baos.reset();
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (StringItem item : wJoinItems) {
            item.index = i++;
            String stringData = item.data;
            Integer of = map.get(stringData);
            if (of != null) {
                item.dataOffset = of;
            } else {
                item.dataOffset = offset;
                map.put(stringData, offset);
                if (useUTF8) {
                    int length = stringData.length();
                    byte[] data = Mutf8.encode(stringData);
                    int u8lenght = data.length;

                    if (length > 0x7F) {
                        offset++;
                        baos.write((length >> 8) | 0x80);
                    }
                    baos.write(length);

                    if (u8lenght > 0x7F) {
                        offset++;
                        baos.write((u8lenght >> 8) | 0x80);
                    }
                    baos.write(u8lenght);
                    baos.write(data);
                    baos.write(0);
                    offset += 3 + u8lenght;
                } else {
                    int length = stringData.length();
                    byte[] data = stringData.getBytes("UTF-16LE");
                    if (length > 0x7FFF) {
                        int x = (length >> 16) | 0x8000;
                        baos.write(x);
                        baos.write(x >> 8);
                        offset += 2;
                    }
                    baos.write(length);
                    baos.write(length >> 8);
                    baos.write(data);
                    baos.write(0);
                    baos.write(0);
                    offset += 4 + data.length;
                }
            }
        }
        paddingTo4x(baos);

        stringData = baos.toByteArray();
        if (wStyleUniq.size() > 0) {
            if (wResUniq.size() > 0) {
                StyleItem it = new StyleItem();
                it.styleSpans = noSpan;
                styleItemMap.put(it, it);
            }
            for (StringItem item : wStyleUniq.values()) {
                StyleItem it = new StyleItem();
                it.styleSpans = item.styleSpans;
                styleItemMap.put(it, it);
            }
            ByteArrayOutputStream styleOs = new ByteArrayOutputStream();
            for (StyleItem si : styleItemMap.values()) {
                si.offset = styleOs.size();
                for (StyleSpan styleSpan : si.styleSpans) {
                    writeLeInt(styleOs, wUniq.get(styleSpan.name).index);
                    writeLeInt(styleOs, styleSpan.start);
                    writeLeInt(styleOs, styleSpan.end);
                }
                writeLeInt(styleOs, -1);
            }
            writeLeInt(styleOs, -1);
            writeLeInt(styleOs, -1);

            paddingTo4x(styleOs);
            styleData = styleOs.toByteArray();
            wStyleCount = wStyleUniq.size() + wResUniq.size();
        } else {
            wStyleCount = 0;
        }
    }

    private void write(ByteBuffer out) throws IOException {
        out.putInt(wJoinItems.size());

        out.putInt(wStyleCount);// style count
        out.putInt(useUTF8 ? StringBlock.UTF8_FLAG : 0);
        int stringDataOffst = 7 * 4 + (wJoinItems.size() + wStyleCount) * 4;
        out.putInt(stringDataOffst);
        out.putInt(wStyleCount > 0 ? stringDataOffst + stringData.length : 0); // style data offset
        for (StringItem item : wJoinItems) {
            out.putInt(item.dataOffset);
        }

        if (wStyleCount > 0) {
            if (wResUniq.size() > 0) {
                key.styleSpans = noSpan;
                StyleItem e = this.styleItemMap.get(key);
                for (int i = 0; i < wResUniq.size(); i++) {
                    out.putInt(e.offset);
                }
            }
            for (StringItem stringItem : wStyleUniq.values()) {
                key.styleSpans = stringItem.styleSpans;
                StyleItem e = this.styleItemMap.get(key);
                out.putInt(e.offset);
            }
        }

        out.put(stringData);
        if (wStyleCount > 0) {
            out.put(styleData);
        }
    }

    public StringItem add(String name, int resourceId) {
        if (name == null) {
            throw new RuntimeException();
        }
        if (resourceId == ResConst.NO_RESOURCE_ID) {
            return add(name);
        }
        String key = name + "_" + resourceId;
        StringItem it = wResUniq.get(key);
        if (it == null) {
            it = new StringItem(name, resourceId);
            wResUniq.put(key, it);
            return it;
        } else {
            return it;
        }
    }

    public StringItem add(String name, List<StyleSpan> styleSpans) {
        if (name == null) {
            throw new RuntimeException();
        }
        if (styleSpans == null || styleSpans.size() == 0) {
            return add(name);
        }
        StringItem key = new StringItem(name, styleSpans.toArray(new StyleSpan[styleSpans.size()]));
        StringItem value = wStyleUniq.get(key);
        if (value == null) {
            value = key;
            wStyleUniq.put(key, value);
            for (StyleSpan styleSpan : styleSpans) {
                add(styleSpan.name);
            }
        }
        return value;
    }

    public void writeStringPoolSection(ByteBuffer out) throws IOException {
        writeChunkHeader(out, RES_STRING_POOL_TYPE, 0x001C, getStringPoolSectionSize());
        write(out);
    }

    public void writeXmlResourceTableSection(ByteBuffer out) {
        writeChunkHeader(out, RES_XML_RESOURCE_MAP_TYPE, 0x0008, getXmlResourceTableSectionSize());
        for (StringItem item : wResUniq.values()) {
            out.putInt(item.resourceId);
        }
    }

    public int getXmlResourceTableSectionSize() {
        return 8 + wResUniq.size() * 4;
    }

    static class StyleItem {
        int offset;
        StyleSpan[] styleSpans;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            StyleItem styleItem = (StyleItem) o;

            if (!Arrays.equals(styleSpans, styleItem.styleSpans))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(styleSpans);
        }
    }
}
