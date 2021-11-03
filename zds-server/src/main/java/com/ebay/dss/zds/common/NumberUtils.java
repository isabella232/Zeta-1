package com.ebay.dss.zds.common;

import com.google.common.collect.ImmutableMap;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberUtils {

    public static final DecimalFormat _df=new DecimalFormat("#.##");

    public static double afterDecimalPoint(double number,int pos){
        if(pos == 2) return Double.parseDouble(_df.format(number));
        else{
            DecimalFormat df=new DecimalFormat("#."+repeatBy("#",pos));
            return Double.parseDouble(df.format(number));
        }
    }

    private static String repeatBy(String str,int repeat){
        StringBuffer sb=new StringBuffer();
        for(int i=1;i<=repeat;i++) sb.append(str);
        return sb.toString();
    }

    public static final Long unit = 1024L;
    public static final Long b = unit;
    public static final Long kb = b * unit;
    public static final Long mb = kb * unit;
    public static final Long gb = mb * unit;
    public static final Long tb = gb * unit;
    public static final Long pb = tb * unit;

    private static final ImmutableMap<String, ByteUnit> byteSuffixes =
            ImmutableMap.<String, ByteUnit>builder()
                    .put("b", ByteUnit.BYTE)
                    .put("k", ByteUnit.KiB)
                    .put("kb", ByteUnit.KiB)
                    .put("m", ByteUnit.MiB)
                    .put("mb", ByteUnit.MiB)
                    .put("g", ByteUnit.GiB)
                    .put("gb", ByteUnit.GiB)
                    .put("t", ByteUnit.TiB)
                    .put("tb", ByteUnit.TiB)
                    .put("p", ByteUnit.PiB)
                    .put("pb", ByteUnit.PiB)
                    .build();

    public static long byteStringAs(String str, ByteUnit unit) {
        String lower = str.toLowerCase().trim();

        try {
            Matcher m = Pattern.compile("([0-9]+)([a-z]+)?").matcher(lower);
            Matcher fractionMatcher = Pattern.compile("([0-9]+\\.[0-9]+)([a-z]+)?").matcher(lower);

            if (m.matches()) {
                long val = Long.parseLong(m.group(1));
                String suffix = m.group(2);

                // Check for invalid suffixes
                if (suffix != null && !byteSuffixes.containsKey(suffix)) {
                    throw new NumberFormatException("Invalid suffix: \"" + suffix + "\"");
                }

                // If suffix is valid use that, otherwise none was provided and use the default passed
                return unit.convertFrom(val, suffix != null ? byteSuffixes.get(suffix) : unit);
            } else if (fractionMatcher.matches()) {
                throw new NumberFormatException("Fractional values are not supported. Input was: "
                        + fractionMatcher.group(1));
            } else {
                throw new NumberFormatException("Failed to parse byte string: " + str);
            }

        } catch (NumberFormatException e) {
            String byteError = "Size must be specified as bytes (b), " +
                    "kibibytes (k), mebibytes (m), gibibytes (g), tebibytes (t), or pebibytes(p). " +
                    "E.g. 50b, 100k, or 250m.";

            throw new NumberFormatException(byteError + "\n" + e.getMessage());
        }
    }

    public static String formatBytes(Long bytes) {
        String unit;
        if (bytes < b) {
            unit = "b";
        } else if (bytes < kb) {
            unit = "kb";
        } else if (bytes < mb) {
            unit = "mb";
        } else if (bytes < gb) {
            unit = "gb";
        } else if (bytes < tb) {
            unit = "tb";
        } else if (bytes < pb) {
            unit = "pb";
        } else {
            unit = "b";
        }

        return byteStringAs(bytes.toString() + "b", byteSuffixes.get(unit)) + unit;
    }
}
