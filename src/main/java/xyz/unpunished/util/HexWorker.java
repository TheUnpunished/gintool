package xyz.unpunished.util;

import java.nio.ByteBuffer;


public class HexWorker {
    
    public static int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }
    
    public static String addZerosToNumber(int number){
        switch (Integer.toString(number).length()){
            case 1:{
                return "00" + number;
            }
            case 2:{
                return "0" + number;
            }
            case 3:{
                return Integer.toString(number);
            }
            default:{
                return "000";
            }
        }
    }
    
}
