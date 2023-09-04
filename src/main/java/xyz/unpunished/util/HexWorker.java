package xyz.unpunished.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class HexWorker {
    
    public static void skip(InputStream stream, int howMuch) throws IOException{
        stream.read(new byte[howMuch]);
    }
    
    public static int readInt32Val(ByteOrder order, byte[] buf){
        return ByteBuffer.wrap(buf).order(order).getInt();
    }
    
}
