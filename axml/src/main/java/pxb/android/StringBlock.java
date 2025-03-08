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

import pxb.android.arsc.ArscParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
@SuppressWarnings("serial")
public class StringBlock {
    public static final int UTF8_FLAG = 0x00000100;
    public int[] resourceIds;
    private String[] strings;
    private List<StyleSpan>[] styles;

    static int u16length(ByteBuffer in) {
        int length = in.getShort() & 0xFFFF;
        if (length > 0x7FFF) {
            length = ((length & 0x7FFF) << 8) | (in.getShort() & 0xFFFF);
        }
        return length;
    }

    static int u8length(ByteBuffer in) {
        int len = in.get() & 0xFF;
        if ((len & 0x80) != 0) {
            len = ((len & 0x7F) << 8) | (in.get() & 0xFF);
        }
        return len;
    }

    public String get(int data) {
        if (data < 0 || data >= strings.length) {
            return "";
        }
        String s = strings[data];
        if (s == null) {
            return "";
        }
        return s;
    }

    public List<StyleSpan> getStyles(int data) {
        if (data < 0 || data >= styles.length) {
            return null;
        }
        return styles[data];
    }

    static class Buff extends ByteArrayOutputStream {
        public String toString(String enc) throws UnsupportedEncodingException {
            return new String(super.buf, 0, super.count, enc);
        }
    }

    @SuppressWarnings("unused")
    public void read(ByteBuffer in, ResChunk_header header) throws IOException {
        int trunkOffset = header.location;
        int stringCount = in.getInt();
        int styleCount = in.getInt();
        int flags = in.getInt();
        int stringDataOffset = in.getInt();
        int stylesDataOffset = in.getInt();
        in.position(header.location + header.headSize);
        int[] strOffsets = new int[stringCount];
        String[] strings = new String[stringCount];
        for (int i = 0; i < stringCount; i++) {
            strOffsets[i] = in.getInt();
        }
        int[] styleOffsets = new int[styleCount];
        for (int i = 0; i < styleCount; i++) {
            styleOffsets[i] = in.getInt();
        }
        int base = trunkOffset + stringDataOffset;
        Buff buff = new Buff();
        for (int i = 0; i < strOffsets.length; i++) {
            try {
                strings[i] = parseString(in, flags, base, buff, strOffsets[i]);
            } catch (Exception ignore) {
                ArscParser.E("fail parse string index %d", i);
            }
        }
        List<StyleSpan>[] styles = new List[styleCount];
        base = trunkOffset + stylesDataOffset;
        for (int i = 0; i < styleCount; i++) {
            try {
                styles[i] = parseStyleSpans(in, strings, base, styleOffsets[i]);
            } catch (Exception ignore) {
                ArscParser.E("fail parse string style span index %d", i);
            }
        }
        this.strings = strings;
        this.styles = styles;
    }

    private List<StyleSpan> parseStyleSpans(ByteBuffer in, String[] strings, int base, int styleOffsets) {
        in.position(base + styleOffsets);
        List<StyleSpan> spans = null;
        // System.out.println(String.format("%d %s", i, strings[i]));
        while (true) {
            int id = in.getInt();
            if (id == -1) {
                break;
            }
            if (spans == null) {
                spans = new ArrayList<StyleSpan>();
            }
            StyleSpan span = new StyleSpan(strings[id], in.getInt(), in.getInt());
            spans.add(span);
            // System.out.println(String.format("%d  [%s %d ~ %d]", i, span.name, span.start, span.end));
        }
        return spans;
    }

    private String parseString(ByteBuffer in, int flags, int base, Buff buff, int strOffsets) throws UnsupportedEncodingException {
        in.position(base + strOffsets);
        String s;
        buff.reset();

        if (0 != (flags & UTF8_FLAG)) {
            u8length(in); // ignored
            int length = u8length(in);
            int start = in.position();
            int blength = 0;
            while (true) {
                byte b = in.get(start + blength);
                if (b == 0) break;
                buff.write(b);
                blength++;
            }
            s = buff.toString("UTF-8"); // MUTF8 ?
            if (length != s.length()) {
                //
            }
        } else {
            int length = u16length(in);
            int start = in.position();
            for (int blength = 0; blength < length * 2; blength++) {
                byte b = in.get(start + blength);
                buff.write(b);
            }
            s = buff.toString("UTF-16LE");
            if (length != s.length()) {
                //
            }
        }
        return s;
    }

    public void readResourceIdTable(ByteBuffer in, ResChunk_header header) {
        in.position(header.location + header.headSize);
        int count = (header.size - header.headSize) / 4;
        resourceIds = new int[count];
        for (int i = 0; i < count; i++) {
            resourceIds[i] = in.getInt();
        }
    }
}
