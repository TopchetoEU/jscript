package me.topchetoeu.jscript.mapping;

import java.util.ArrayList;
import java.util.List;

public class VLQ {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private static long[] toArray(List<Long> list) {
        var arr = new long[list.size()];
        for (var i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    public static String encode(long... arr) {
        var raw = new StringBuilder();

        for (var data : arr) {
            var b = data < 0 ? 1 : 0;
            data = Math.abs(data);
            b |= (int)(data & 0b1111) << 1;
            data >>= 4;
            b |=  data > 0 ? 0x20 : 0;;
            raw.append(ALPHABET.charAt(b));

            while (data > 0) {
                b = (int)(data & 0b11111);
                data >>= 5;
                b |= data > 0 ? 0x20 : 0;
                raw.append(ALPHABET.charAt(b));
            }
        }

        return raw.toString();
    }
    public static long[] decode(String val) {
        if (val.length() == 0) return new long[0];

        var list = new ArrayList<Long>();

        for (var i = 0; i < val.length();) {
            var sign = 1;
            var curr = ALPHABET.indexOf(val.charAt(i++));
            var cont = (curr & 0x20) == 0x20;
            if ((curr & 1) == 1) sign = -1;
            long res = (curr & 0b11110) >> 1;
            var n = 4;

            for (; i < val.length() && cont;) {
                curr = ALPHABET.indexOf(val.charAt(i++));
                cont = (curr & 0x20) == 0x20;
                res |= (curr & 0b11111) << n;
                n += 5;
                if (!cont) break;
            }

            list.add(res * sign);
        }

        return toArray(list);
    }

    public static String encodeMapping(long[][][] arr) {
        var res = new StringBuilder();
        var semicolon = false;
        
        for (var line : arr) {
            var comma = false;

            if (semicolon) res.append(";");
            semicolon = true;

            for (var el : line) {
                if (comma) res.append(",");
                comma = true;
                res.append(encode(el));
            }
        }

        return res.toString();
    }
    public static long[][][] decodeMapping(String val) {
        var lines = new ArrayList<long[][]>();

        for (var line : val.split(";", -1)) {
            var elements = new ArrayList<long[]>();
            for (var el : line.split(",", -1)) {
                elements.add(decode(el));
            }
            lines.add(elements.toArray(long[][]::new));
        }

        return lines.toArray(long[][][]::new);
    }
}
