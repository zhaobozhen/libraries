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

public abstract class NodeVisitor {
    protected NodeVisitor nv;

    public NodeVisitor() {
        super();
    }

    public NodeVisitor(NodeVisitor nv) {
        super();
        this.nv = nv;
    }

    /**
     * add attribute to the node
     * 
     * @param ns
     * @param name
     * @param resourceId
     * @param value
     *            Res_value or AttrRes_value
     */
    public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
        if (nv != null) {
            nv.attr(ns, name, resourceId, raw, value);
        }
    }

    /**
     * create a child node
     * 
     * @param ns
     * @param name
     * @return
     */
    public NodeVisitor child(String ns, String name) {
        if (nv != null) {
            return nv.child(ns, name);
        }
        return null;
    }

    /**
     * end the visit
     */
    public void end() {
        if (nv != null) {
            nv.end();
        }
    }

    /**
     * line number in the .xml
     * 
     * @param ln
     */
    public void line(int ln) {
        if (nv != null) {
            nv.line(ln);
        }
    }

    /**
     * the node text
     * 
     * @param value
     */
    public void text(int lineNumber, String value, Res_value styled) {
        if (nv != null) {
            nv.text(lineNumber, value, styled);
        }
    }
}
