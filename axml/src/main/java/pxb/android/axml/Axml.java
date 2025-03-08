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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Axml extends AxmlVisitor {
    public static Axml parse(ByteBuffer b) throws IOException {
        Axml axml = new Axml();
        AxmlReader r = new AxmlReader(b);
        r.accept(axml);
        return axml;
    }

    public static Axml parse(byte[] b) throws IOException {
        return parse(ByteBuffer.wrap(b));
    }

    public byte[] toByteArray() throws IOException {
        AxmlWriter w = new AxmlWriter();
        this.accept(w);
        return w.toByteArray();
    }

    public static class Node extends NodeVisitor {
        public static class Attr {
            public String ns, name;
            public int resourceId;
            public Res_value value;
            public String raw;

            public void accept(NodeVisitor nodeVisitor) {
                nodeVisitor.attr(ns, name, resourceId, raw, value);
            }
        }

        public static class Text {
            public int ln;
            public String text;
            public Res_value styled;

            public void accept(NodeVisitor nodeVisitor) {
                nodeVisitor.text(ln, text, styled);
            }
        }

        public List<Attr> attrs = new ArrayList<Attr>();
        public List<Node> children = new ArrayList<Node>();
        public Integer ln;
        public String ns, name;
        public Text text;

        public void accept(NodeVisitor nodeVisitor) {
            NodeVisitor nodeVisitor2 = nodeVisitor.child(ns, name);
            acceptB(nodeVisitor2);
            nodeVisitor2.end();
        }

        public void acceptB(NodeVisitor nodeVisitor) {
            if (text != null) {
                text.accept(nodeVisitor);
            }
            for (Attr a : attrs) {
                a.accept(nodeVisitor);
            }
            if (ln != null) {
                nodeVisitor.line(ln);
            }
            for (Node c : children) {
                c.accept(nodeVisitor);
            }
        }

        @Override
        public void attr(String ns, String name, int resourceId, String raw, Res_value obj) {
            Attr attr = new Attr();
            attr.name = name;
            attr.ns = ns;
            attr.resourceId = resourceId;
            attr.value = obj;
            attr.raw = raw;
            attrs.add(attr);
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            Node node = new Node();
            node.name = name;
            node.ns = ns;
            children.add(node);
            return node;
        }

        @Override
        public void line(int ln) {
            this.ln = ln;
        }

        @Override
        public void text(int lineNumber, String value, Res_value styled) {
            Text text = new Text();
            text.ln = lineNumber;
            text.text = value;
            text.styled = styled;
            this.text = text;
        }

        public Attr findFirstAttr(int resourceId) {
            for (Node.Attr attr : this.attrs) {
                if (attr.resourceId == resourceId) {
                    return attr;
                }
            }
            return null;
        }

        public Attr findFirstAttr(String attrName) {
            for (Node.Attr attr : this.attrs) {
                if (attr.name.equals(attrName) && attr.ns == null) {
                    return attr;
                }
            }
            return null;
        }

        public Node findFirst(final String nodeName) {
            for (Node node : this.children) {
                if (node.name.equals(nodeName)) {
                    return node;
                }
            }
            return null;
        }

        public void replace(String ns, String name, int resourceId, String raw, Res_value v) {
            Axml.Node.Attr attr = this.findFirstAttr(resourceId);
            if (attr != null) {
                attr.ns = ns;
                attr.name = name;
                attr.value = v;
                attr.raw = raw;
            } else {
                this.attr(ns, name, resourceId, raw, v);
            }
        }
    }

    public static class Ns {
        public int ln;
        public String prefix, uri;

        public void accept(AxmlVisitor visitor) {
            visitor.ns(prefix, uri, ln);
        }
    }

    public List<Node> firsts = new ArrayList<Node>();
    public List<Ns> nses = new ArrayList<Ns>();

    public void accept(final AxmlVisitor visitor) {
        for (Ns ns : nses) {
            ns.accept(visitor);
        }
        for (Node first : firsts) {
            first.accept(visitor);
        }
    }

    @Override
    public NodeVisitor child(String ns, String name) {
        Node node = new Node();
        node.name = name;
        node.ns = ns;
        firsts.add(node);
        return node;
    }

    @Override
    public void ns(String prefix, String uri, int ln) {
        Ns ns = new Ns();
        ns.prefix = prefix;
        ns.uri = uri;
        ns.ln = ln;
        nses.add(ns);
    }

    public Node findFirst(final String nodeName) {
        for (Node node : this.firsts) {
            if (node.name.equals(nodeName)) {
                return node;
            }
        }
        return null;
    }


}
