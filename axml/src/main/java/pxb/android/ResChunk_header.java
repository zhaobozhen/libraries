package pxb.android;

import pxb.android.arsc.ArscParser;

import java.nio.ByteBuffer;

/**
 * Header that appears at the front of every data chunk in a resource.
 */
public class ResChunk_header {
    /**
     * Size of the chunk header (in bytes).  Adding this value to
     * the address of the chunk allows you to find its associated data
     * (if any).
     * uint16_t headerSize;
     */
    public final int headSize;
    public final int location;
    /**
     * Total size of this chunk (in bytes).  This is the chunkSize plus
     * the size of any data associated with the chunk.  Adding this value
     * to the chunk allows you to completely skip its contents (including
     * any child chunks).  If this value is the same as chunkSize, there is
     * no data associated with the chunk.
     * uint32_t size;
     */
    public final int size;
    /**
     * Type identifier for this chunk.  The meaning of this value depends
     * on the containing chunk.
     * uint16_t type;
     */
    public final int type;

    public ResChunk_header(ByteBuffer in) {
        location = in.position();
        type = in.getShort() & 0xFFFF;
        headSize = in.getShort() & 0xFFFF;
        size = in.getInt();
        // ArscParser.D("[%08x]type: %04x, headsize: %04x, size:%08x", location, type, headSize, size);
    }

    public static void writeChunkHeader(ByteBuffer out, int type, int headerSize, int size) {
        out.putShort((short) type).putShort((short) headerSize).putInt(size);
    }
}
