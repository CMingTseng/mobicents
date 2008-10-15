package org.jdiameter.client.impl.parser;

/*
 * Copyright (c) 2006 jDiameter.
 * https://jdiameter.dev.java.net/
 *
 * License: GPL v3
 *
 * e-mail: erick.svenson@yahoo.com
 *
 */

import org.jdiameter.api.AvpDataException;
import org.jdiameter.client.api.parser.DecodeException;
import org.jdiameter.client.api.parser.IElementParser;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Date;

public class ElementParser implements IElementParser {

    private static final long SECOND_SHIFT = 2208988800L;
    
    private static final int INT_INET4 = 1;
    private static final int INT_INET6 = 2;
    
    private static final int INT32_SIZE = 4;
    private static final int INT64_SIZE = 8;
    private static final int FLOAT32_SIZE = 4;
    private static final int FLOAT64_SIZE = 8;

    public int bytesToInt(byte[] rawData) throws AvpDataException {
      return prepareBuffer(rawData, INT32_SIZE).getInt();
    }

    public long bytesToLong(byte[] rawData) throws AvpDataException {
      return prepareBuffer(rawData, INT64_SIZE).getLong();
    }

    public float bytesToFloat(byte[] rawData) throws AvpDataException {
       return prepareBuffer(rawData, INT32_SIZE).getFloat();
    }

    public double bytesToDouble(byte[] rawData) throws AvpDataException {
         return prepareBuffer(rawData, FLOAT64_SIZE).getDouble();
    }

    protected ByteBuffer prepareBuffer(byte [] bytes, int len)  {
        if (bytes.length != len)
            throw new IllegalArgumentException("Incorrect data length");
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    public String bytesToOctetString(byte[] rawData) throws AvpDataException {
        try {
            char[] ca = new String(rawData, "iso-8859-1").toCharArray();
            StringBuffer rc = new StringBuffer(ca.length);
            for (char c:ca)
                if (c != (char)0x0) rc.append(c);
            return rc.toString();
        } catch (UnsupportedEncodingException e) {
            throw new AvpDataException("Invalid data type", e);
        }
    }

    public String bytesToUtf8String(byte[] rawData) throws AvpDataException {
        try {
            char[] ca = new String(rawData, "utf8").toCharArray();
            StringBuffer rc = new StringBuffer(ca.length);
            for (char c:ca)
                if (c != (char)0x0) rc.append(c);
            return rc.toString();
        } catch (Exception e) {
             throw new AvpDataException("Invalid data type", e);
        }
    }

    public Date bytesToDate(byte[] rawData) throws AvpDataException {
        try {
            byte[] tmp = new byte[8];
            System.arraycopy(rawData, 0 , tmp, 4, 4);
            return new Date( ((bytesToLong(tmp) - SECOND_SHIFT) * 1000L));
        } catch (Exception e) {
            throw new AvpDataException(e);
        }
    }

    public InetAddress bytesToAddress(byte[] rawData) throws AvpDataException {
        InetAddress inetAddress;
        byte[] address;
        try {
            if (rawData[INT_INET4] == INT_INET4) {
                address = new byte[4];
                System.arraycopy(rawData, 2, address, 0, address.length);
                inetAddress = Inet4Address.getByAddress(address);
            } else {
                address = new byte[16];
                System.arraycopy(rawData, 2, address, 0, address.length);
                inetAddress = Inet6Address.getByAddress(address);
            }
        } catch (Exception e) {
            throw new AvpDataException(e);
        }
        return inetAddress;
    }

    public byte [] int32ToBytes(int value){
        byte [] bytes = new byte[INT32_SIZE];
        ByteBuffer buffer = ByteBuffer.allocate(INT32_SIZE);
        buffer.putInt(value);
        buffer.flip();
        buffer.get(bytes);
        return bytes;
    }

    public byte [] intU32ToBytes(long value){
        byte [] bytes = new byte[INT32_SIZE];
        ByteBuffer buffer = ByteBuffer.allocate(INT64_SIZE);
        buffer.putLong(value);
        buffer.flip();
        buffer.get(bytes);
        buffer.get(bytes);
        return bytes;
    }

    public byte [] int64ToBytes(long value){
        byte [] bytes = new byte[INT64_SIZE];
        ByteBuffer buffer = ByteBuffer.allocate(INT64_SIZE);
        buffer.putLong(value);
        buffer.flip();
        buffer.get(bytes);
        return bytes;
    }

    public byte [] float32ToBytes(float value){
        byte [] bytes = new byte[FLOAT32_SIZE];
        ByteBuffer buffer = ByteBuffer.allocate(FLOAT32_SIZE);
        buffer.putFloat(value);
        buffer.flip();
        buffer.get(bytes);
        return bytes;
    }

    public byte [] float64ToBytes(double value){
        byte [] bytes = new byte[FLOAT64_SIZE];
        ByteBuffer buffer = ByteBuffer.allocate(FLOAT64_SIZE);
        buffer.putDouble(value);
        buffer.flip();
        buffer.get(bytes);
        return bytes;
    }

    public byte[] octetStringToBytes(String value) throws DecodeException{
        try {
            return value.getBytes("iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new DecodeException(e);
        }
    }

    public byte[] utf8StringToBytes(String value) throws DecodeException {
        try {
            return value.getBytes("utf8");
        } catch (Exception e) {
            throw new DecodeException(e);
        }
    }

    public byte[] addressToBytes(InetAddress address) {
        byte byteAddrOrig[] = address.getAddress();

        byte[] data = new byte[byteAddrOrig.length + 2];

        int addrType = address instanceof Inet4Address ? INT_INET4 : INT_INET6;
        data[0] = (byte) ((addrType >> 8) & 0xFF);
        data[INT_INET4] = (byte) ((addrType >> 0) & 0xFF);

        System.arraycopy(byteAddrOrig, 0, data, 2, byteAddrOrig.length);
        return data;
    }

    public byte[] dateToBytes(Date date) {
        byte[] data = new byte[4];
        System.arraycopy(int64ToBytes( (date.getTime()/1000L) + SECOND_SHIFT  ), 4, data, 0, 4);
        return data;
    }

    public <T> T bytesToObject(java.lang.Class<?> iface, byte[] rawdata) throws AvpDataException {
        return null;
    }

    public byte [] objectToBytes(Object data) throws DecodeException {
        return null;
    }
}
