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
package pxb.android.arsc;

import pxb.android.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 
 * Read the resources.arsc inside an Android apk.
 * 
 * Usage:
 * 
 * <pre>
 * byte[] oldArscFile= ... ; //
 * List&lt;Pkg&gt; pkgs = new ArscParser(oldArscFile).parse(); // read the file
 * modify(pkgs); // do what you want here
 * byte[] newArscFile = new ArscWriter(pkgs).toByteArray(); // build a new file
 * </pre>
 * 
 * The format of arsc is described here (gingerbread)
 * <ul>
 * <li>frameworks/base/libs/utils/ResourceTypes.cpp</li>
 * <li>frameworks/base/include/utils/ResourceTypes.h</li>
 * </ul>
 * and the cmd line <code>aapt d resources abc.apk</code> is also good for debug (available in android sdk)
 * 
 * <p>
 * Thanks to the the following projects
 * <ul>
 * <li>android4me https://code.google.com/p/android4me/</li>
 * <li>Apktool https://code.google.com/p/android-apktool</li>
 * <li>Android http://source.android.com/</li>
 * </ul>
 * 
 * @author bob
 * 
 */
public class ArscParser implements ResConst {

    private static final boolean DEBUG = false;

    private static void D(String fmt, Object... args) {
        if (DEBUG) {
            System.out.println(String.format(fmt, args));
        }
    }

    public static void E(String fmt, Object... args) {
        System.err.println(String.format(fmt, args));
    }
    /**
     * If set, this resource has been declared public, so libraries are allowed to reference it.
     */
    static final int ENGRY_FLAG_PUBLIC = 0x0002;

    /**
     * If set, this is a complex entry, holding a set of name/value mappings. It is followed by an array of ResTable_map
     * structures.
     */
    final static short ENTRY_FLAG_COMPLEX = 0x0001;

    private ByteBuffer in;
    private StringBlock keyNamesX;
    private Pkg pkg;
    private List<Pkg> pkgs = new ArrayList<Pkg>();
    private StringBlock strings;
    private StringBlock typeNamesX;

    public ArscParser(ByteBuffer b) {
        this.in = b.order(ByteOrder.LITTLE_ENDIAN);
    }
    public ArscParser(byte[] b) {
        this(ByteBuffer.wrap(b));
    }

    public List<Pkg> parse() throws IOException {
        in.position(0);
        /**
         * Header for a resource table.  Its data contains a series of
         * additional chunks:
         *   * A ResStringPool_header containing all table values.  This string pool
         *     contains all of the string values in the entire resource table (not
         *     the names of entries or type identifiers however).
         *   * One or more ResTable_package chunks.
         *
         * Specific entries within a resource table can be uniquely identified
         * with a single integer as defined by the ResTable_ref structure.
         */
        // struct ResTable_header {
        //   ResChunk_header header;
        //   uint32_t packageCount
        // }
        ResChunk_header header = new ResChunk_header(in);
        if (header.type != RES_TABLE_TYPE) {
            throw new RuntimeException();
        }
        int packageCount = in.getInt();

        int savePoint = header.location + header.headSize;

        // make sure ResStringPool loaded before ResTable_package
        in.position(savePoint);
        while (in.hasRemaining()) {
            ResChunk_header chunk = new ResChunk_header(in);
            if (chunk.type == RES_STRING_POOL_TYPE) {
                StringBlock sb = new StringBlock();
                sb.read(in, chunk);
                strings = sb;
                break;
            }
            in.position(chunk.location + chunk.size);
        }
        in.position(savePoint);
        for (int pkgIdx = 0; pkgIdx < packageCount;) {
            ResChunk_header chunk = new ResChunk_header(in);
            switch (chunk.type) {
                case RES_STRING_POOL_TYPE:
                    ;// values already read
                    break;
                case RES_TABLE_PACKAGE_TYPE:
                    readPackage(in, chunk);
                    pkgIdx++;
                    break;
                default:
                    E("WARN: type 0x%04x not allowed, only ResTable_package(0x%04x) and ResStringPool_header(0x%04x) allowed", chunk.type, RES_TABLE_PACKAGE_TYPE, RES_STRING_POOL_TYPE);
                    break;
            }
            in.position(chunk.location + chunk.size);
        }
        return pkgs;
    }
    private void readEntry(Config config, ResSpec spec) {
        D("[%08x]read ResTable_entry", in.position());
        int size = in.getShort();
        D("ResTable_entry %d", size);

        int flags = in.getShort(); // ENTRY_FLAG_PUBLIC
        int keyStr = in.getInt();
        spec.updateName(keyNamesX.get(keyStr));

        ResEntry resEntry = new ResEntry();
        resEntry.isPublic = 0 != (flags & ENGRY_FLAG_PUBLIC);

        if (0 != (flags & ENTRY_FLAG_COMPLEX)) {

            int parent = in.getInt();
            int count = in.getInt();
            BagValue bag = new BagValue(parent);
            for (int i = 0; i < count; i++) {
                Map.Entry<Integer, Res_value> entry = new AbstractMap.SimpleEntry(in.getInt(), readValue());
                bag.map.add(entry);
            }
            resEntry.value = bag;
        } else {
            resEntry.value = readValue();
        }
        config.resources.put(spec, resEntry);
    }

    /**
     * A collection of resource data types within a package.  Followed by
     * one or more ResTable_type and ResTable_typeSpec structures containing the
     * entry values for each resource type.
     */
    private void readPackage(ByteBuffer in, ResChunk_header package_header) throws IOException {
        // If this is a base package, its ID.  Package IDs start
        // at 1 (corresponding to the value of the package bits in a
        // resource identifier).  0 means this is not a base package.
        //  uint32_t id;
        int pid = in.getInt() % 0xFF;

        // Actual name of this package, \0-terminated.
        // char16_t name[128];
        String name = readFixedU16String(in);

        pkg = new Pkg(pid, name);
        pkgs.add(pkg);
        // Offset to a ResStringPool_header defining the resource
        // type symbol table.  If zero, this package is inheriting from
        // another base package (overriding specific values in it).
        // uint32_t typeStrings;
        int typeStrings = in.getInt();

        // Last index into typeStrings that is for public use by others.
        // uint32_t lastPublicType;
        int lastPublicType = in.getInt();

        // Offset to a ResStringPool_header defining the resource
        // key symbol table.  If zero, this package is inheriting from
        // another base package (overriding specific values in it).
        // uint32_t keyStrings;
        int keyStrings = in.getInt();

        // Last index into keyStrings that is for public use by others.
        // uint32_t lastPublicKey;
        int lastPublicKey = in.getInt();

        // 1. search for strings
        {
            if(typeStrings==0){
                E("typeStrings is 0");
            }else {
                in.position(package_header.location+typeStrings);
                ResChunk_header chunk = new ResChunk_header(in);
                if (chunk.type != RES_STRING_POOL_TYPE) {
                    throw new RuntimeException();
                }
                StringBlock sb = new StringBlock();
                sb.read(in, chunk);
                typeNamesX = sb;
            }
        }
        {
            if (keyStrings == 0) {
                E("keyStrings is 0");
            } else {
                in.position(package_header.location + keyStrings);
                ResChunk_header chunk = new ResChunk_header(in);
                if (chunk.type != RES_STRING_POOL_TYPE) {
                    throw new RuntimeException();
                }
                StringBlock sb = new StringBlock();
                sb.read(in, chunk);
                keyNamesX = sb;
            }
        }


        final int end = package_header.location + package_header.size;

        // 2. search for type specs
        in.position(package_header.location + package_header.headSize);
        while (in.position() < end) {
            ResChunk_header chunk = new ResChunk_header(in);
            switch (chunk.type) {
                case RES_TABLE_TYPE_SPEC_TYPE: {

                    /**
                     * A specification of the resources defined by a particular type.
                     *
                     * There should be one of these chunks for each resource type.
                     *
                     * This structure is followed by an array of integers providing the set of
                     * configuration change flags (ResTable_config::CONFIG_*) that have multiple
                     * resources for that configuration.  In addition, the high bit is set if that
                     * resource has been made public.
                     *
                     * // Additional flag indicating an entry is public.
                     * SPEC_PUBLIC = 0x40000000
                     */

                    D("[%08x]read spec", in.position() - 8);

                    // The type identifier this chunk is holding.  Type IDs start
                    // at 1 (corresponding to the value of the type bits in a
                    // resource identifier).  0 is invalid.
                    // uint8_t id;
                    int tid = in.get() & 0xFF;
                    in.get(); // res0
                    in.getShort();// res1

                    // Number of uint32_t entry configuration masks that follow.
                    // uint32_t entryCount;
                    int entryCount = in.getInt();

                    in.position(chunk.location + chunk.headSize);
                    Type t = pkg.ensureType(tid, typeNamesX.get(tid - 1), entryCount);
                    for (int i = 0; i < entryCount; i++) {
                        t.getSpec(i).flags = in.getInt();
                    }
                }
                break;
                default:
                    break;
            }
            in.position(chunk.location + chunk.size);
        }

        // 3. search for configs
        in.position(package_header.location + package_header.headSize);
        while (in.position() < end) {
            ResChunk_header chunk = new ResChunk_header(in);
            switch (chunk.type) {
            case RES_TABLE_TYPE_TYPE: {
                /**
                 * A collection of resource entries for a particular resource data
                 * type. Followed by an array of uint32_t defining the resource
                 * values, corresponding to the array of type strings in the
                 * ResTable_package::typeStrings string block. Each of these hold an
                 * index from entriesStart; a value of NO_ENTRY means that entry is
                 * not defined.
                 *
                 * There may be multiple of these chunks for a particular resource type,
                 * supply different configuration variations for the resource values of
                 * that type.
                 *
                 * It would be nice to have an additional ordered index of entries, so
                 * we can do a binary search if trying to find a resource by string name.
                 */
                D("[%08x]read config", in.position() - 8);


                // The type identifier this chunk is holding.  Type IDs start
                // at 1 (corresponding to the value of the type bits in a
                // resource identifier).  0 is invalid.
                // uint8_t id;
                int tid = in.get() & 0xFF;
                in.get(); // res0
                in.getShort();// res1

                // Number of uint32_t entry indices that follow.
                // uint32_t entryCount;
                int entryCount = in.getInt();
                Type t = pkg.ensureType(tid, typeNamesX.get(tid - 1), entryCount);

                // Offset from header where ResTable_entry data starts.
                // uint32_t entriesStart;
                int entriesStart = in.getInt();

                D("[%08x]read config id", in.position());

                int p = in.position();
                int size = in.getInt();
                // readConfigFlags();
                byte[] data = new byte[size];
                in.position(p);
                in.get(data);
                Config config = new Config(data);

                in.position(chunk.location + chunk.headSize);

                D("[%08x]read config entry offset", in.position());

                int[] entrys = new int[entryCount];
                for (int i = 0; i < entryCount; i++) {
                    entrys[i] = in.getInt();
                }
                D("[%08x]read config entrys", in.position());
                for (int i = 0; i < entrys.length; i++) {
                    if (entrys[i] != -1) {
                        try {
                            extracted(in, chunk, t, entriesStart, config, entrys, i);
                        } catch (Exception ignore) {
                            E("fail parse config entry %d", i);
                        }
                    }
                }

                t.addConfig(config);
            }
                break;
            case RES_TABLE_LIBRARY_TYPE:
                int libraryCount = in.getInt();
                in.position(chunk.location + chunk.headSize);
                for (int i = 0; i < libraryCount; i++) {
/**
 * A shared library package-id to package name entry.
                    struct ResTable_lib_entry
                    {
                        // The package-id this shared library was assigned at build time.
                        // We use a uint32 to keep the structure aligned on a uint32 boundary.
                        uint32_t packageId;

                        // The package name of the shared library. \0 terminated.
                        char16_t packageName[128];
                    };
 */
                    int packageId = in.getInt();
                    String packageName = readFixedU16String(in);
                    this.pkg.libraries.put(packageName, packageId);
                }
                break;
            default:
                break;
            }
            in.position(chunk.location + chunk.size);
        }
    }

    private void extracted(ByteBuffer in, ResChunk_header chunk, Type t, int entriesStart, Config config, int[] entrys, int i) {
        in.position(chunk.location + entriesStart + entrys[i]);
        ResSpec spec = t.getSpec(i);
        readEntry(config, spec);
    }

    private String readFixedU16String(ByteBuffer in) {
        String name;
        int nextPisition = in.position() + 128 * 2;
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 128; i++) {
            int s = in.getShort();
            if (s == 0) {
                break;
            } else {
                sb.append((char) s);
            }
        }
        name = sb.toString();
        in.position(nextPisition);
        return name;
    }

    private Object readValue() {
        int pos = in.position();
        int size1 = in.getShort();// 8
        int zero = in.get();// 0
        int type = in.get() & 0xFF; // TypedValue.*
        int data = in.getInt();
        String raw = null;
        List<StyleSpan> xstyles = null;
        if (type == Res_value.TYPE_STRING) {
            raw = strings.get(data);
            xstyles = strings.getStyles(data);

        }
        in.position(pos + size1);
        return new Res_value(type, data, raw, xstyles);
    }
}
