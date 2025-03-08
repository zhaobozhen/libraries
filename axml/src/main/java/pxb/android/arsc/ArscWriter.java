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
import pxb.android.axml.Util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import static pxb.android.ResChunk_header.writeChunkHeader;

/**
 * Write pkgs to an arsc file
 * 
 * @see ArscParser
 * @author bob
 * 
 */
public class ArscWriter implements ResConst {
    private static class PkgCtx {
        Map<String, StringItem> keyNames = new HashMap<String, StringItem>();
        StringBlockWriter keyNames0 = new StringBlockWriter();
        public int keyStringOff;
        int offset;
        Pkg pkg;
        int pkgSize;
        List<String> typeNames = new ArrayList<String>();

        StringBlockWriter typeNames0 = new StringBlockWriter();
        int typeStringOff;

        public void addKeyName(String name) {
            if (keyNames.containsKey(name)) {
                return;
            }
            StringItem stringItem = keyNames0.add(name);
            keyNames.put(name, stringItem);
        }

        public void addTypeName(int id, String name) {
            while (typeNames.size() <= id) {
                typeNames.add(null);
            }
            typeNames.set(id, name);
        }
    }

    private static void D(String fmt, Object... args) {

    }

    private List<PkgCtx> ctxs = new ArrayList<PkgCtx>(5);
    private List<Pkg> pkgs;
    private StringBlockWriter strTable0 = new StringBlockWriter();
    boolean useUTF8 = true;
    public ArscWriter(List<Pkg> pkgs) {
        this.pkgs = pkgs;
    }
    public ArscWriter(Pkg pkg) {
        this(Arrays.asList(pkg));
    }

    public static void main(String... args) throws IOException {
        if (args.length < 2) {
            System.err.println("asrc-write-test in.arsc out.arsc");
            return;
        }
        byte[] data = Util.readFile(new File(args[0]));
        List<Pkg> pkgs = new ArscParser(data).parse();
        // ArscDumper.dump(pkgs);
        byte[] data2 = new ArscWriter(pkgs).toByteArray();
        // ArscDumper.dump(new ArscParser(data2).parse());
        Util.writeFile(data2, new File(args[1]));
    }

    private void addString(String str) {
        strTable0.add(str);
    }

    private int count() {

        int size = 0;

        size += 8 + 4;// chunk, pkgcount
        size += strTable0.getStringPoolSectionSize();

        for (PkgCtx ctx : ctxs) {
            ctx.offset = size;
            int pkgSize = 0;
            pkgSize += 8 + 4 + 256;// chunk,pid+name
            pkgSize += 4 * 4;

            ctx.typeStringOff = pkgSize;
            pkgSize += ctx.typeNames0.getStringPoolSectionSize();

            ctx.keyStringOff = pkgSize;
            pkgSize += ctx.keyNames0.getStringPoolSectionSize();

            int librarySize = ctx.pkg.libraries.size();
            if (librarySize > 0) {
                pkgSize += 12 + (4 + 256) * librarySize;
            }
            for (Type type : ctx.pkg.types.values()) {
                type.wPosition = size + pkgSize;
                pkgSize += 8 + 4 + 4 + 4 * type.specs.length; // trunk,id,entryCount,
                                                              // configs

                for (Config config : type.configs) {
                    config.wPosition = pkgSize + size;
                    int configBasePostion = pkgSize;
                    pkgSize += 8 + 4 + 4 + 4; // trunk,id,entryCount,entriesStart
                    int size0 = config.id.length;
                    if(size0 < 36){
                        size0 = 36;
                    }
                    if (size0 % 4 != 0) {
                        size0 += 4 - size0 % 4;
                    }
                    pkgSize += size0;// config

                    pkgSize += 4 * type.specs.length;// offset
                    config.wEntryStart = pkgSize - configBasePostion;
                    int entryBase = pkgSize;
                    for (ResEntry e : config.resources.values()) {
                        e.wOffset = pkgSize - entryBase;
                        pkgSize += 8;// size,flag,keyString
                        if (e.value instanceof BagValue) {
                            BagValue big = (BagValue) e.value;
                            pkgSize += 8 + big.map.size() * 12;
                        } else {
                            pkgSize += 8;
                        }
                    }
                    config.wChunkSize = pkgSize - configBasePostion;
                }
            }
            ctx.pkgSize = pkgSize;
            size += pkgSize;
        }

        return size;
    }

    private List<PkgCtx> prepare() throws IOException {
        for (Pkg pkg : pkgs) {
            PkgCtx ctx = new PkgCtx();
            ctx.pkg = pkg;
            ctxs.add(ctx);

            for (Type type : pkg.types.values()) {
                ctx.addTypeName(type.id - 1, type.name);
                for (ResSpec spec : type.specs) {
                    if (spec.name == null) {
                        ctx.addKeyName("?");
                    } else {
                        ctx.addKeyName(spec.name);
                    }
                }
                for (Config config : type.configs) {
                    for (ResEntry e : config.resources.values()) {
                        Object object = e.value;
                        if (object instanceof BagValue) {
                            travelBagValue((BagValue) object);
                        } else {
                            travelValue((Res_value) object);
                        }
                    }
                }
            }
            ctx.keyNames0.setUseUTF8(useUTF8);
            ctx.keyNames0.prepare();
            ctx.typeNames0.of(ctx.typeNames);
            ctx.typeNames0.setUseUTF8(useUTF8);
            ctx.typeNames0.prepare();
        }
        strTable0.setUseUTF8(useUTF8);
        strTable0.prepare();
        return ctxs;
    }

    public void useUTF8String() {
        useUTF8 = true;
    }

    public void useUTF16String() {
        useUTF8 = false;
    }

    public byte[] toByteArray() throws IOException {
        prepare();
        int size = count();
        ByteBuffer out = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        write(out, size);
        return out.array();
    }

    private void travelBagValue(BagValue bag) {
        for (Map.Entry<Integer, Res_value> e : bag.map) {
            travelValue(e.getValue());
        }
    }

    private void travelValue(Res_value v) {
        if (v.raw != null) {
            if (v.styles != null && v.styles.size() > 0) { // has style
                addStringWithStyle(v.raw, v.styles);
            } else {
                addString(v.raw);
            }
        }
    }

    private void addStringWithStyle(String raw, List<StyleSpan> styles) {
        for (StyleSpan styleSpan : styles) {
            addString(styleSpan.name);
        }
        strTable0.add(raw, styles);

    }

    private void write(ByteBuffer out, int size) throws IOException {
        writeChunkHeader(out, RES_TABLE_TYPE, 0x000c, size);
        out.putInt(ctxs.size());

        strTable0.writeStringPoolSection(out);

        for (PkgCtx pctx : ctxs) {
            if (out.position() != pctx.offset) {
                throw new RuntimeException();
            }
            final int basePosition = out.position();
            writeChunkHeader(out, RES_TABLE_PACKAGE_TYPE, 0x011c, pctx.pkgSize);
            out.putInt(pctx.pkg.id);
            {
                int p = out.position();
                out.put(pctx.pkg.name.getBytes("UTF-16LE"));
                out.position(p + 256);
            }

            out.putInt(pctx.typeStringOff);
            out.putInt(pctx.typeNames0.wItemCount);

            out.putInt(pctx.keyStringOff);
            out.putInt(pctx.keyNames0.wItemCount);

            {
                if (out.position() - basePosition != pctx.typeStringOff) {
                    throw new RuntimeException();
                }
                pctx.typeNames0.writeStringPoolSection(out);
            }

            {
                if (out.position() - basePosition != pctx.keyStringOff) {
                    throw new RuntimeException();
                }
                pctx.keyNames0.writeStringPoolSection(out);
            }

            int librarySize = pctx.pkg.libraries.size();
            if (librarySize > 0) {
                writeChunkHeader(out, RES_TABLE_LIBRARY_TYPE, 12, 12 + (4 + 256) * librarySize);
                out.putInt(librarySize);
                for (Map.Entry<String, Integer> e : pctx.pkg.libraries.entrySet()){
                    out.putInt(e.getValue());
                    int p = out.position();
                    out.put(e.getKey().getBytes("UTF-16LE"));
                    out.position(p + 256);
                }
            }
            for (Type t : pctx.pkg.types.values()) {
                D("[%08x]write spec", out.position(), t.name);
                if (t.wPosition != out.position()) {
                    throw new RuntimeException();
                }

                writeChunkHeader(out, RES_TABLE_TYPE_SPEC_TYPE, 0x0010, 4 * 4 + 4 * t.specs.length);

                out.putInt(t.id);
                out.putInt(t.specs.length);
                for (ResSpec spec : t.specs) {
                    out.putInt(spec.flags);
                }

                for (Config config : t.configs) {
                    D("[%08x]write config", out.position());
                    int typeConfigPosition = out.position();
                    if (config.wPosition != typeConfigPosition) {
                        throw new RuntimeException();
                    }

                   int  headSize = 8 + 4 + 4 + 4; // trunk,id,entryCount,entriesStart
                    int size0 = config.id.length;
                    if(size0 < 36){
                        size0 = 36;
                    }
                    if (size0 % 4 != 0) {
                        size0 += 4 - size0 % 4;
                    }

                    headSize += size0;// config

                    writeChunkHeader(out, RES_TABLE_TYPE_TYPE, headSize, config.wChunkSize);

                    out.putInt(t.id);
                    out.putInt(t.specs.length);
                    out.putInt(config.wEntryStart);

                    D("[%08x]write config ids", out.position());

                    if (config.id.length < 36) {
                        out.putInt(size0);
                        out.put(config.id, 4, config.id.length - 4);
                    } else {
                        out.put(config.id);
                    }

                    if (size0 > config.id.length) {
                        out.put(new byte[size0 - config.id.length]);
                    }

                    D("[%08x]write config entry offsets", out.position());
                    for (int i = 0; i < t.specs.length; i++) {
                        ResEntry entry = config.resources.get(t.specs[i]);
                        if (entry == null) {
                            out.putInt(-1);
                        } else {
                            out.putInt(entry.wOffset);
                        }
                    }

                    if (out.position() - typeConfigPosition != config.wEntryStart) {
                        throw new RuntimeException();
                    }
                    D("[%08x]write config entrys", out.position());
                    for (Map.Entry<ResSpec, ResEntry> e : config.resources.entrySet()) {
                        ResEntry resEntry = e.getValue();
                        ResSpec spec = e.getKey();
                        D("[%08x]ResTable_entry", out.position());
                        boolean isBag = resEntry.value instanceof BagValue;
                        out.putShort((short) (isBag ? 16 : 8));
                        int flag = 0;
                        if (isBag) { // add complex flag
                            flag |= ArscParser.ENTRY_FLAG_COMPLEX;
                        }
                        if (resEntry.isPublic) {
                            flag |= ArscParser.ENGRY_FLAG_PUBLIC;
                        }
                        out.putShort((short) flag);
                        out.putInt(pctx.keyNames.get(spec.name == null ? "?" : spec.name).index);
                        if (isBag) {
                            BagValue bag = (BagValue) resEntry.value;
                            out.putInt(bag.parent);
                            out.putInt(bag.map.size());
                            for (Map.Entry<Integer, Res_value> entry : bag.map) {
                                out.putInt(entry.getKey());
                                writeValue(entry.getValue(), out);
                            }
                        } else {
                            writeValue((Res_value) resEntry.value, out);
                        }
                    }
                }
            }
        }
    }

    private void writeValue(Res_value value, ByteBuffer out) {
        out.putShort((short) 8);
        out.put((byte) 0);
        out.put((byte) value.type);
        if (value.type == Res_value.TYPE_STRING) {
            if (value.styles != null) {
                out.putInt(strTable0.add(value.raw, value.styles).index);
            } else {
                out.putInt(strTable0.add(value.raw).index);
            }
        } else {
            out.putInt(value.data);
        }
    }

}
