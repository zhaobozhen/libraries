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

import java.io.File;

public class FixManifestAdapter extends AxmlVisitor {

    private static String Android_NS = "http://schemas.android.com/apk/res/android";

    static boolean isEmpty(String e) {
        return e != null && e.length() == 0;
    }

    static boolean isNullOrEmpty(String e) {
        return e == null || e.length() == 0;
    }

    public static void main(String... args) throws Exception {
        if (args.length < 2) {
            System.err.println("FixManifestAdapter in out");
            return;
        }

        byte[] xml = Util.readFile(new File(args[0]));

        AxmlReader rd = new AxmlReader(xml);
        AxmlWriter wr = new AxmlWriter();
        rd.accept(new FixManifestAdapter(wr));

        byte[] modified = wr.toByteArray();
        Util.writeFile(modified, new File(args[1]));
    }

    public FixManifestAdapter() {
    }

    public FixManifestAdapter(NodeVisitor av) {
        super(av);
    }

    @Override
    public void attr(String ns, String name, int resourceId, String raw, Res_value obj) {
        if (isEmpty(ns)) {
            ns = null;
        }
        if (resourceId == -1) {
            super.attr(ns, name, resourceId, raw, obj);
            return;
        }
        if (((resourceId >> 16) & 0xFFFF) != 0x0101) {
            // not an attr
            // clean up the resourceId
            System.err.printf("clean up none-attr resourceId %08x\n", resourceId);
            super.attr(ns, name, -1, raw, obj);
            return;
        }
        if ((resourceId & 0xFFFF) > R.AttrNames.NAMES.length) {
            // too huge for now api22, 2015-04-10
            // clean up the resourceId
            System.err.printf("clean up too-huge-attr resourceId %08x\n", resourceId);
            super.attr(null, name, -1, raw, obj);
            return;
        }

        String suggestName = R.AttrNames.NAMES[resourceId & 0xFFFF];
        if (suggestName.length() == 0) { // reserved id
            System.err.printf("found reversed resourceId %08x\n", resourceId);
        } else {
            ns = Android_NS;
            name = suggestName;
        }

        super.attr(ns, name, resourceId, raw, obj);
    }

    @Override
    public NodeVisitor child(String ns, String name) {
        if (isEmpty(ns)) {
            ns = null;
        }
        NodeVisitor nv = super.child(ns, name);
        if (nv != null) {
            nv = new FixManifestAdapter(nv);
        }
        return nv;
    }

    @Override
    public void ns(String prefix, String uri, int ln) {
        if (isNullOrEmpty(uri)) {
            return;
        }
        if (isEmpty(prefix)) {
            prefix = null;
        }
        if (Android_NS.equals(uri)) {
            prefix = "android";
        }
        super.ns(prefix, uri, ln);
    }

}
