package com.my.mwble.util;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.my.mwble.util.DigitalTrans.patchHexString;


public class NumUtil {
    /*
     * 将时间转换为时间戳
     */
    public static String dateToStamp(String s) throws ParseException {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime();
        res = String.valueOf(ts);
        return res;
    }

    /**
     * 合并byte数组
     */
    public static byte[] unitByteArray(byte[] byte1, byte[] byte2) {
        byte[] unitByte = new byte[byte1.length + byte2.length];
        System.arraycopy(byte1, 0, unitByte, 0, byte1.length);
        System.arraycopy(byte2, 0, unitByte, byte1.length, byte2.length);
        return unitByte;
    }

    /*
     * 16进制字符串转字节数组
     */
    public static byte[] hexString2Bytes(String hex) {

        if ((hex == null) || (hex.equals(""))) {
            return null;
        } else if (hex.length() % 2 != 0) {
            return null;
        } else {
            hex = hex.toUpperCase();
            int len = hex.length() / 2;
            byte[] b = new byte[len];
            char[] hc = hex.toCharArray();
            for (int i = 0; i < len; i++) {
                int p = 2 * i;
                b[i] = (byte) (charToByte(hc[p]) << 4 | charToByte(hc[p + 1]));
            }
            return b;
        }

    }

    /*
     * 字符转换为字节
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }


    /**
     * 获取当前日期是星期几<br>
     *
     * @param dt
     * @return 当前日期是星期几
     */
    public static String getWeekOfDate(Date dt) {
        String[] weekDays = {"07", "01", "02", "03", "04", "05", "06"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0)
            w = 0;
        return weekDays[w];
    }

    /**
     * 字符串转换成十六进制值
     *
     * @param bin String 我们看到的要转换成十六进制的字符串
     * @return
     */
    public static String bin2hex(String bin) {
        char[] digital = "0123456789ABCDEF".toCharArray();
        StringBuffer sb = new StringBuffer("");
        byte[] bs = bin.getBytes();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(digital[bit]);
            bit = bs[i] & 0x0f;
            sb.append(digital[bit]);
        }
        return sb.toString();
    }

    /**
     * 把16进制字符串转化为byte数组
     *
     * @param hexString
     * @return
     */
    public static byte[] toByteArray(String hexString) {

        if (TextUtils.isEmpty(hexString))
            throw new IllegalArgumentException("this hexString must not be empty");
        hexString = hexString.toLowerCase();

        final byte[] byteArray = new byte[hexString.length() / 2];

        int k = 0;

        for (int i = 0; i < byteArray.length; i++) {//因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先

            byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);

            byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);

            byteArray[i] = (byte) (high << 4 | low);

            k += 2;
        }
        return byteArray;
    }


    /**
     * 十六进制字符串转十进制
     *
     * @param hex 十六进制字符串
     * @return 十进制数值
     */
    public static int hexStringToAlgorism(String hex) {
        hex = hex.toUpperCase();
        int max = hex.length();
        int result = 0;
        for (int i = max; i > 0; i--) {
            char c = hex.charAt(i - 1);
            int algorism = 0;
            if (c >= '0' && c <= '9') {
                algorism = c - '0';
            } else {
                algorism = c - 55;
            }
            result += Math.pow(16, max - i) * algorism;
        }
        return result;
    }

//    Bitwise AND

    /**
     * 16进制 按位与
     *
     * @param key   16进制 String
     * @param value 16进制 String
     * @return 16 进制 String
     */
    public static String hexBitwiseAND(String key, String value) {
        byte[] bKey = NumUtil.hexString2Bytes(key);
        byte[] bValue = NumUtil.hexString2Bytes(value);
        byte[] bResult = new byte[bKey.length];

        for (int i = 0; i < bKey.length; i++) {
            bResult[i] = (byte) (bKey[i] & bValue[i]);
        }
        return bytesToHexString(bResult);
    }


    /**
     * 16进制 按位异或
     *
     * @param key   16进制 String
     * @param value 16进制 String
     * @return 16进制 String
     */
    public static String hexBitwiseXOR(String key, String value) {
        byte[] bKey = NumUtil.hexString2Bytes(key);
        byte[] bValue = NumUtil.hexString2Bytes(value);
        byte[] bResult = new byte[bKey.length];

        for (int i = 0; i < bKey.length; i++) {
            bResult[i] = (byte) (bKey[i] ^ bValue[i]);
        }
        return bytesToHexString(bResult);
    }

    /**
     * 字符加密
     * 按位与，按位异或
     *
     * @param key1  按位与加密串
     * @param key2  按位异或加密串
     * @param value 需要加密的值
     * @return 加密之后的Hex String
     */
    public static String getEncryption(String key1, String key2, String value) {
        String valueOne = hexBitwiseAND(key1, value);
        String valueTwo = hexBitwiseXOR(key2, valueOne);
        return valueTwo;
    }

    /**
     * 将byte数组转为16进制字符串 此方法主要目的为方便Log的显示
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
//            stringBuilder.append(hv.toUpperCase()).append(" ");
            stringBuilder.append(hv.toUpperCase());
        }
        return stringBuilder.toString();
    }

    /**
     * 获取高两位
     *
     * @param value
     * @return
     */
    public static String getHeightValue(String value) {
        return value.substring(0, 2);
    }

    /**
     * 获取低两位
     *
     * @param value
     * @return
     */
    public static String getLowValue(String value) {
        return value.substring(value.length() - 2, value.length());
    }


    /**
     * 将十进制转换为指定长度的十六进制字符串
     *
     * @param algorism  int 十进制数字
     * @param maxLength int 转换后的十六进制字符串长度
     * @return String 转换后的十六进制字符串
     */
    public static String algorismToHEXString(int algorism, int maxLength) {
        String result = "";
        result = Integer.toHexString(algorism);

        if (result.length() % 2 == 1) {
            result = "0" + result;
        }
        return patchHexString(result.toUpperCase(), maxLength);
    }

    /**
     * 10进制转16进制
     *
     * @param n
     * @return
     */
    public static String intToHex(int n) {
        //StringBuffer s = new StringBuffer();
        StringBuilder sb = new StringBuilder(8);
        String a;
        char[] b = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        while (n != 0) {
            sb = sb.append(b[n % 16]);
            n = n / 16;
        }
        a = sb.reverse().toString();
        return a;
    }

    public static String longByte2String(byte[] bytes) {
        StringBuffer sbHex = new StringBuffer();

        sbHex.append(bytesToHexString(bytes));

        StringBuffer sbByre = new StringBuffer();
        for (int i = 0; i < sbHex.length() / 2; i++) {
            String hex = sbHex.substring(i * 2, i * 2 + 2);
            sbByre.append(byreArray2String(hexString2Bytes(hex)));
        }

        return sbByre.toString();
    }

    /**
     * 值运算一字节
     *
     * @param bytes
     * @return
     */
    public static String byreArray2String(byte[] bytes) {
        String hex = bytesToHexString(bytes);

        if (hex.length() > 2) {
            return null;
        }

        int value = hexStringToAlgorism(hex);

        StringBuffer sbByte = new StringBuffer();

        while (true) {
            if (value == 0) {
                break;
            } else {
                sbByte.append(value % 2);
                value = value / 2;
            }
        }

        int bwFlag = 8 - sbByte.length();

        if (bwFlag != 0) {
            for (int i = 0; i < bwFlag; i++) {
                sbByte.append("0");
            }
        }

        return sbByte.reverse().toString();
    }

    /**
     * 01010101二进制转Hex
     * 只支持1字节=8位
     *
     * @param value
     * @return
     */
    public static String byte2Hex(StringBuffer value) {

        if (value.length() != 8) {
            return null;
        }

        boolean isFu = false; // 是否为负数

        if (value.substring(0, 1).equals("1")) { // 负数 取反
            isFu = true;
            StringBuffer sbFan = new StringBuffer();
            for (int i = 0; i < value.length(); i++) { // 取反，颠倒
                if (value.substring(i, i + 1).equals("0")) {
                    sbFan.append("1");
                } else {
                    sbFan.append("0");
                }
            }
            value = sbFan;
        }

        StringBuffer sbReverse = new StringBuffer();
        sbReverse = value.reverse();
        value = sbReverse;

        int iValue = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.substring(i, i + 1).equals("1")) {
                iValue += Math.pow(2, i);
            }
        }

        if (isFu) {
            iValue = 0 - iValue - 1; // 取反加一 (变相)
        }

        StringBuffer sbHex = new StringBuffer();
        sbHex.append(Integer.toHexString(iValue));

        if (sbHex.length() == 1) {
            StringBuffer cValue = new StringBuffer();
            sbHex.append("0");
            cValue = sbHex.reverse();
            sbHex = cValue;

        }

        LogUtil.i("hexString:" + sbHex.toString());
        String hexStringFinal = sbHex.substring(sbHex.length() - 2, sbHex.length());
        return toUpperCase(hexStringFinal);
    }

    /**
     * 小写转大写
     *
     * @param str
     * @return
     */
    public static String toUpperCase(String str) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if ('a' <= chars[i] && chars[i] <= 'z') {
                chars[i] -= 32;
            }
        }
        return String.valueOf(chars);
    }

    /**
     * 大写转小写
     *
     * @param str
     * @return
     */
    public static String toLowerCase(String str) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if ('A' <= chars[i] && chars[i] <= 'Z') {
                chars[i] += 32;
            }
        }
        return String.valueOf(chars);
    }
}
