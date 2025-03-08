package pxb.android.axml;

import pxb.android.ResConst;
import pxb.android.Res_value;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AxmlTool {
    public static void main(final String... args) throws Exception {
        final Map<String, String> ps = new HashMap<String, String>();
        final List<String> remain = new ArrayList<String>();
        int i = 0;
        while (i < args.length) {
            final String arg = args[i++];
            if (arg.startsWith("--")) {
                ps.put(arg, "true");
            } else if (arg.startsWith("-")) {
                ps.put(arg, args[i++]);
            } else {
                remain.add(arg);
            }
        }
        System.out.println("ps: " + ps);
        System.out.println("remains: " + remain);
        if (remain.size() < 2) {
            System.err.println("EnableDebugger [--enable-debug] [-app-name <class>] [-package-name <package>]  in out");
            return;
        }
        final byte[] xml = Util.readFile(new File(remain.get(0)));
        final AxmlReader rd = new AxmlReader(xml);
        Axml axml = new Axml();
        rd.accept(axml);

        Axml.Node manifest = axml.findFirst("manifest");
        Axml.Node application = manifest.findFirst("application");

        String pkg = ps.get("-package-name");
        if (pkg != null) {
            manifest.replace(null, "package", ResConst.NO_RESOURCE_ID, pkg, Res_value.newStringValue(pkg));
        }
        if (ps.containsKey("--enable-debug")) {
            application.replace("http://schemas.android.com/apk/res/android",
                    "debuggable", R.attr.debuggable, null, Res_value.newTrue());
        }
        String s = ps.get("-app-name");
        if (s != null) {
            application.replace("http://schemas.android.com/apk/res/android",
                    "name", 16842755, s, Res_value.newStringValue(s));
        }

        final AxmlWriter wr = new AxmlWriter();

        axml.accept(wr);
        final byte[] modified = wr.toByteArray();
        Util.writeFile(modified, new File(remain.get(1)));
    }
}
