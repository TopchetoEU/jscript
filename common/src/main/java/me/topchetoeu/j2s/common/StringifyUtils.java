package me.topchetoeu.j2s.common;

import java.math.BigDecimal;

public class StringifyUtils {
	public static String quoteString(String raw) {
		var res = new StringBuilder("\"");
		var alphabet = "0123456789ABCDEF".toCharArray();

		for (var c : raw.toCharArray()) {
			if (c < 32 || c >= 127) {
				res
					.append("\\u")
					.append(alphabet[(c >> 12) & 0xF])
					.append(alphabet[(c >> 8) & 0xF])
					.append(alphabet[(c >> 4) & 0xF])
					.append(alphabet[(c >> 0) & 0xF]);
			}
			else if (c == '\\')
				res.append("\\\\");
			else if (c == '"')
				res.append("\\\"");
			else res.append(c);
		}

		return res.append('"').toString();
	}

	public static String quoteNumber(Double num) {
		if (num == Double.NEGATIVE_INFINITY) return "-Infinity";
		if (num == Double.POSITIVE_INFINITY) return "Infinity";
		if (Double.isNaN(num)) return "NaN";
		return BigDecimal.valueOf(num).stripTrailingZeros().toPlainString();
	}

	private static double power(double a, long b) {
		if (b == 0) return 1;
		if (b == 1) return a;
		if (b < 0) return 1 / power(a, -b);

		if ((b & 1) == 0) return power(a * a, b / 2);
		else return a * power(a * a, b / 2);
	}

	public static Double unqoteNumber(String src) {
		var i = 0;

		double whole = 0;
		double fract = 0;
		long exponent = 0;
		boolean parsedAny = false;
		boolean negative = false;

		if (src.charAt(i) == '-') {
			negative = true;
			i++;
		}

		while (i < src.length()) {
			var c = src.charAt(i);
			if (c < '0' || c > '9') break;

			parsedAny = true;
			whole *= 10;
			whole += src.charAt(i++) - '0';
		}

		if (i < src.length() && src.charAt(i) == '.') {
			parsedAny = true;
			i++;

			while (i < src.length()) {
				var c = src.charAt(i);
				if (c < '0' || c > '9') break;

				parsedAny = true;
				fract += src.charAt(i++) - '0';
				fract /= 10;
			}
		}

		if (i < src.length() && (src.charAt(i) == 'e' || src.charAt(i) == 'E')) {
			i++;
			parsedAny = true;
			boolean expNegative = false;
			boolean parsedE = false;

			if (i < src.length()) {
				if (src.charAt(i) == '-') {
					expNegative = true;
					i++;
				}
				else if (src.charAt(i) == '+') {
					i++;
				}
			}


			while (i < src.length()) {
				var c = src.charAt(i);
				if (c < '0' || c > '9') break;

				parsedE = true;
				exponent *= 10;

				if (expNegative) exponent -= src.charAt(i) - '0';
				else exponent += src.charAt(i) - '0';
			}

			if (!parsedE) return Double.NaN;
		}

		if (i != src.length()) return Double.NaN;

		if (!parsedAny) {
			if (negative) return Double.NaN;
			return 0.;
		}
		else if (negative) return -(whole + fract) * power(10, exponent);
		else return (whole + fract) * power(10, exponent);
	}
}

