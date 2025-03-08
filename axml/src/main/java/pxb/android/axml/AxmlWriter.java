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

import pxb.android.Res_value;
import pxb.android.StringBlock;
import pxb.android.StringBlockWriter;
import pxb.android.StringItem;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import static pxb.android.ResChunk_header.writeChunkHeader;
import static pxb.android.axml.AxmlParser.*;

/**
 * a class to write android axml
 * 
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 */
public class AxmlWriter extends AxmlVisitor {

    public static void main(String... args) throws IOException {
        AxmlReader r = new AxmlReader(Util.readFile(new File(args[0])));
        AxmlWriter w = new AxmlWriter();
        r.accept(new DumpAdapter(w));
        Util.writeFile(w.toByteArray(), new File(args[1]));
    }

    static final Comparator<Attr> ATTR_CMP = new Comparator<Attr>() {

        @Override
        public int compare(Attr a, Attr b) {
            int x = a.name.resourceId - b.name.resourceId;
            if (x == 0) {
                x = a.name.data.compareTo(b.name.data);
                if (x == 0) {
                    boolean aNsIsnull = a.ns == null;
                    boolean bNsIsnull = b.ns == null;
                    if (aNsIsnull) {
                        if (bNsIsnull) {
                            x = 0;
                        } else {
                            x = -1;
                        }
                    } else {
                        if (bNsIsnull) {
                            x = 1;
                        } else {
                            x = a.ns.data.compareTo(b.ns.data);
                        }
                    }

                }
            }
            return x;
        }
    };

    static class Attr {

        public int index;
        public StringItem name;
        public StringItem ns;
        public Res_value value;
        public StringItem value_raw;
        public StringItem raw;

        public Attr(StringItem ns, StringItem name) {
            super();
            this.ns = ns;
            this.name = name;
        }
    }

    class NodeImpl extends NodeVisitor {
        private Set<Attr> attrs = new TreeSet<Attr>(ATTR_CMP);
        private List<NodeImpl> children = new ArrayList<NodeImpl>();
        private int line;
        private StringItem name;
        private StringItem ns;
        private StringItem text;
        private Res_value styled;
        private StringItem styledText;
        private int textLineNumber;
        Attr id;
        Attr style;
        Attr clz;
        private int attributeStart = 20;
        private int attributeSize = 20;
        private int headerSize = 16;

        public NodeImpl(StringItem ns, StringItem name) {
            super(null);
            this.ns = ns;
            this.name = name;
        }

        @Override
        public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
            if (name == null) {
                throw new RuntimeException("name can't be null");
            }

            Attr a = new Attr(addNs(ns), stringItems.add(name, resourceId));
            a.value = value;

            if (value instanceof AttrRes_value) {
                AttrRes_value valueWrapper = (AttrRes_value) value;

                switch (valueWrapper.attrValueType) {
                case AttrRes_value.CLASS:
                    clz = a;
                    break;
                case AttrRes_value.ID:
                    id = a;
                    break;
                case AttrRes_value.STYLE:
                    style = a;
                    break;
                }
            }
            if (raw != null) {
                a.raw = stringItems.add(raw);
            }
            if (value!=null && value.raw != null) {
                a.value_raw = stringItems.add(value.raw, value.styles);
            }

            attrs.add(a);
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            if (name == null) {
                throw new RuntimeException("name can't be null");
            }
            NodeImpl child = new NodeImpl(addNs(ns), stringItems.add(name));
            this.children.add(child);
            return child;
        }

        @Override
        public void end() {
        }

        @Override
        public void line(int ln) {
            this.line = ln;
        }

        public int prepare(AxmlWriter axmlWriter) {
            int attrIndex = 0;
            for (Attr attr : attrs) {
                attr.index = attrIndex++;
            }

            int size = headerSize; // head size

            size += 20;

            attributeStart = size - headerSize;
            size += attrs.size() * attributeSize;// attribute

            // start tag
            for (NodeImpl child : children) {
                size += child.prepare(axmlWriter);
            }
            if (text != null) {
                size += 28;
            }
            size += 24; //  for end tag
            return size;
        }

        @Override
        public void text(int ln, String value, Res_value styled) {
            this.text = stringItems.add(value);
            if (styled != null && styled.raw != null) {
                this.styledText = stringItems.add(styled.raw, styled.styles);
            }
            this.styled = styled;
            this.textLineNumber = ln;
        }

        void write(ByteBuffer out) throws IOException {
            // start tag
            writeChunkHeader(out, RES_XML_START_ELEMENT_TYPE, headerSize, headerSize + attributeStart + attrs.size() * attributeSize);
            out.putInt(line);
            out.putInt(0xFFFFFFFF);
            out.position(out.position() + (headerSize - 16));
            // header size ends here
            out.putInt(ns != null ? this.ns.index : -1);
            out.putInt(name.index);
            out.putShort((short) (attributeStart));
            out.putShort((short) attributeSize);
            out.putShort((short) this.attrs.size());
            out.putShort((short) (id == null ? 0 : id.index + 1));
            out.putShort((short) (clz == null ? 0 : clz.index + 1));
            out.putShort((short) (style == null ? 0 : style.index + 1));

            for (Attr attr : attrs) {
                out.putInt(attr.ns == null ? -1 : attr.ns.index);
                out.putInt(attr.name.index);
                out.putInt(attr.raw != null ? attr.raw.index : -1);

                out.putShort((short) 8);
                out.put((byte) 0);
                if (attr.value == null) {
                    out.put((byte) 0);
                    out.putInt(0);
                } else {
                    out.put((byte) attr.value.type);
                    if (attr.value_raw != null) {
                        out.putInt(attr.value_raw.index);
                    } else if (attr.value.type == Res_value.TYPE_STRING) {
                        out.putInt(-1);
                    } else {
                        out.putInt(attr.value.data);
                    }
                }
                out.position(out.position() + (attributeSize - 20));
            }

            if (this.text != null) {
                writeChunkHeader(out, RES_XML_CDATA_TYPE, 0x0010, 28);
                out.putInt(textLineNumber);
                out.putInt(0xFFFFFFFF);
                out.putInt(text.index);
                if (styled != null) {
                    out.putShort((short) 8);
                    out.put((byte) 0);
                    out.put((byte) styled.type);
                    if (styledText != null) {
                        out.putInt(styledText.index);
                    } else {
                        out.putInt(styled.data);
                    }
                } else {
                    out.putInt(0x00000008);
                    out.putInt(0x00000000);
                }
            }

            // children
            for (NodeImpl child : children) {
                child.write(out);
            }

            // end tag
            writeChunkHeader(out, RES_XML_END_ELEMENT_TYPE, 0x0010, 24);
            out.putInt(-1);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns != null ? this.ns.index : -1);
            out.putInt(name.index);
        }
    }

    static class Ns {
        int ln;
        StringItem prefix;
        StringItem uri;

        public Ns(StringItem prefix, StringItem uri, int ln) {
            super();
            this.prefix = prefix;
            this.uri = uri;
            this.ln = ln;
        }
    }

    private List<NodeImpl> firsts = new ArrayList<NodeImpl>(3);

    private Map<String, Ns> nses = new HashMap<String, Ns>();

    private StringBlockWriter stringItems = new StringBlockWriter();

    @Override
    public NodeVisitor child(String ns, String name) {
        if (name == null) {
            throw new RuntimeException("name can't be null");
        }
        NodeImpl first = new NodeImpl(addNs(ns), stringItems.add(name));
        this.firsts.add(first);
        return first;
    }

    @Override
    public void end() {
    }

    @Override
    public void ns(String prefix, String uri, int ln) {
        Ns ns = nses.get(uri);
        StringItem prefixItem = prefix == null ? null : stringItems.add(prefix);
        if (ns == null) {
            nses.put(uri, new Ns(prefixItem, stringItems.add(uri), ln));
        } else {
            ns.prefix = prefixItem;
            ns.ln = ln;
        }
    }

    public void useUTF8String() {
        stringItems.setUseUTF8(true);
    }

    public void useUTF16String() {
        stringItems.setUseUTF8(false);
    }

    private int prepare() throws IOException {
        int size = 0;

        for (NodeImpl first : firsts) {
            size += first.prepare(this);
        }
        {
            int a = 0;
            for (Map.Entry<String, Ns> e : nses.entrySet()) {
                Ns ns = e.getValue();
                if (ns.prefix == null) {
                    ns.prefix = stringItems.add(String.format("axml_auto_%02d", a++));
                }
            }
        }

        size += nses.size() * 24 * 2;

        this.stringItems.prepare();
        size += stringItems.getStringPoolSectionSize();
        size += stringItems.getXmlResourceTableSectionSize();
        return size;
    }

    public byte[] toByteArray() throws IOException {

        int size = 8 + prepare();
        ByteBuffer out = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

        writeChunkHeader(out, RES_XML_TYPE, 0x0008, size);

        stringItems.writeStringPoolSection(out);
        stringItems.writeXmlResourceTableSection(out);

        Stack<Ns> stack = new Stack<Ns>();
        for (Map.Entry<String, Ns> e : this.nses.entrySet()) {
            Ns ns = e.getValue();
            stack.push(ns);
            writeChunkHeader(out, RES_XML_START_NAMESPACE_TYPE, 0x0010, 24);
            out.putInt(-1);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns.prefix.index);
            out.putInt(ns.uri.index);
        }

        for (NodeImpl first : firsts) {
            first.write(out);
        }

        while (stack.size() > 0) {
            Ns ns = stack.pop();
            writeChunkHeader(out, RES_XML_END_NAMESPACE_TYPE, 0x0010, 24);
            out.putInt(ns.ln);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns.prefix.index);
            out.putInt(ns.uri.index);
        }
        return out.array();
    }

    private StringItem addNs(String uri) {
        if (uri == null) {
            return null;
        }
        Ns ns = nses.get(uri);
        if (ns == null) {
            StringItem uriItem = stringItems.add(uri);
            nses.put(uri, new Ns(null, uriItem, 0));
            return uriItem;
        } else {
            return ns.uri;
        }
    }
}
