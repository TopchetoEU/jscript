package me.topchetoeu.jscript.common.parsing;

import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.compilation.values.constants.NumberNode;

public class Parsing {
	public static boolean isDigit(Character c) {
		return c != null && c >= '0' && c <= '9';
	}
	public static boolean isAny(char c, String alphabet) {
		return alphabet.contains(Character.toString(c));
	}

	public static int fromHex(char c) {
		if (c >= 'A' && c <= 'F') return c - 'A' + 10;
		if (c >= 'a' && c <= 'f') return c - 'a' + 10;
		if (c >= '0' && c <= '9') return c - '0';
		return -1;
	}

	public static int skipEmpty(Source src, int i) {
		return skipEmpty(src, i, true);
	}

	public static int skipEmpty(Source src, int i, boolean noComments) {
		int n = 0;

		if (i == 0 && src.is(0, "#!")) {
			while (!src.is(n, '\n')) n++;
			n++;
		}

		var isSingle = false;
		var isMulti = false;

		while (i + n < src.size()) {
			if (isSingle) {
				if (src.is(i + n, '\n')) {
					n++;
					isSingle = false;
				}
				else n++;
			}
			else if (isMulti) {
				if (src.is(i + n, "*/")) {
					n += 2;
					isMulti = false;
				}
				else n++;
			}
			else if (src.is(i + n, "//")) {
				n += 2;
				isSingle = true;
			}
			else if (src.is(i + n, "/*")) {
				n += 2;
				isMulti = true;
			}
			else if (src.is(i + n, Character::isWhitespace)) {
				n++;
			}
			else break;
		}

		return n;
	}

	public static ParseRes<Character> parseChar(Source src, int i) {
		int n = 0;

		if (src.is(i + n, '\\')) {
			n++;
			char c = src.at(i + n++);

			if (c == 'b') return ParseRes.res('\b', n);
			else if (c == 't') return ParseRes.res('\t', n);
			else if (c == 'n') return ParseRes.res('\n', n);
			else if (c == 'f') return ParseRes.res('\f', n);
			else if (c == 'r') return ParseRes.res('\r', n);
			else if (c == '0') {
				if (src.is(i + n, Parsing::isDigit)) return ParseRes.error(src.loc(i), "Octal escape sequences are not allowed");
				else return ParseRes.res('\0', n);
			}
			else if (c >= '1' && c <= '9') return ParseRes.error(src.loc(i), "Octal escape sequences are not allowed");
			else if (c == 'x') {
				var newC = 0;

				for (var j = 0; j < 2; j++) {
					if (i + n >= src.size()) return ParseRes.error(src.loc(i), "Invalid hexadecimal escape sequence");

					int val = fromHex(src.at(i + n));
					if (val == -1) throw new SyntaxException(src.loc(i + n), "Invalid hexadecimal escape sequence");
					n++;

					newC = (newC << 4) | val;
				}

				return ParseRes.res((char)newC, n);
			}
			else if (c == 'u') {
				var newC = 0;

				for (var j = 0; j < 4; j++) {
					if (i + n >= src.size()) return ParseRes.error(src.loc(i), "Invalid Unicode escape sequence");

					int val = fromHex(src.at(i + n));
					if (val == -1) throw new SyntaxException(src.loc(i + n), "Invalid Unicode escape sequence");
					n++;

					newC = (newC << 4) | val;
				}

				return ParseRes.res((char)newC, n);
			}
			else if (c == '\n') return ParseRes.res(null, n);
			else n--;
		}

		return ParseRes.res(src.at(i + n), n + 1);
	}

	public static ParseRes<String> parseIdentifier(Source src, int i) {
		var n = skipEmpty(src, i);
		var res = new StringBuilder();
		var first = true;

		while (true) {
			if (i + n > src.size()) break;
			char c = src.at(i + n, '\0');

			if (first && Parsing.isDigit(c)) break;
			if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') break;
			res.append(c);
			n++;
			first = false;
		}

		if (res.length() <= 0) return ParseRes.failed();
		else return ParseRes.res(res.toString(), n);
	}
	public static ParseRes<String> parseIdentifier(Source src, int i, String test) {
		var n = skipEmpty(src, i);
		var res = new StringBuilder();
		var first = true;

		while (true) {
			if (i + n > src.size()) break;
			char c = src.at(i + n, '\0');

			if (first && Parsing.isDigit(c)) break;
			if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') break;
			res.append(c);
			n++;
			first = false;
		}

		if (res.length() <= 0) return ParseRes.failed();
		else if (test == null || res.toString().equals(test)) return ParseRes.res(res.toString(), n);
		else return ParseRes.failed();
	}
	public static boolean isIdentifier(Source src, int i, String test) {
		return parseIdentifier(src, i, test).isSuccess();
	}

	public static ParseRes<String> parseOperator(Source src, int i, String op) {
		var n = skipEmpty(src, i);

		if (src.is(i + n, op)) return ParseRes.res(op, n + op.length());
		else return ParseRes.failed();
	}

	private static ParseRes<Double> parseHex(Source src, int i) {
		int n = 0;
		double res = 0;

		while (true) {
			int digit = Parsing.fromHex(src.at(i + n, '\0'));
			if (digit < 0) {
				if (n <= 0) return ParseRes.failed();
				else return ParseRes.res(res, n);
			}
			n++;

			res *= 16;
			res += digit;
		}
	}
	private static ParseRes<Double> parseOct(Source src, int i) {
		int n = 0;
		double res = 0;

		while (true) {
			int digit = src.at(i + n, '\0') - '0';
			if (digit < 0 || digit > 9) break;
			if (digit > 7) return ParseRes.error(src.loc(i + n), "Digits in octal literals must be from 0 to 7, encountered " + digit);

			if (digit < 0) {
				if (n <= 0) return ParseRes.failed();
				else return ParseRes.res(res, n);
			}
			n++;

			res *= 8;
			res += digit;
		}

		return ParseRes.res(res, n);
	}

	public static ParseRes<String> parseString(Source src, int i) {
		var n = skipEmpty(src, i);
	
		char quote;
	
		if (src.is(i + n, '\'')) quote = '\'';
		else if (src.is(i + n, '"')) quote = '"';
		else return ParseRes.failed();
		n++;
	
		var res = new StringBuilder();
	
		while (true) {
			if (i + n >= src.size()) return ParseRes.error(src.loc(i + n), "Unterminated string literal");
			if (src.is(i + n, quote)) {
				n++;
				return ParseRes.res(res.toString(), n);
			}

			var charRes = parseChar(src, i + n);
			if (!charRes.isSuccess()) return charRes.chainError(src.loc(i + n), "Invalid character");
			n += charRes.n;

			if (charRes.result != null) res.append(charRes.result);
		}
	}
	public static ParseRes<Double> parseNumber(Source src, int i, boolean withMinus) {
		var n = skipEmpty(src, i);

		double whole = 0;
		double fract = 0;
		long exponent = 0;
		boolean parsedAny = false;
		boolean negative = false;

		if (withMinus && src.is(i + n, "-")) {
			negative = true;
			n++;
		}

		if (src.is(i + n, "0x") || src.is(i + n, "0X")) {
			n += 2;

			var res = parseHex(src, i + n);
			if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Incomplete hexadecimal literal");
			n += res.n;

			if (negative) return ParseRes.res(-res.result, n);
			else return ParseRes.res(res.result, n);
		}
		else if (src.is(i + n, "0o") || src.is(i + n, "0O")) {
			n += 2;

			var res = parseOct(src, i + n);
			if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Incomplete octal literal");
			n += res.n;

			if (negative) return ParseRes.res(-res.result, n);
			else return ParseRes.res(res.result, n);
		}
		else if (src.is(i + n, '0')) {
			n++;
			parsedAny = true;
			if (src.is(i + n, Parsing::isDigit)) return ParseRes.error(src.loc(i + n), "Decimals with leading zeroes are not allowed");
		}

		while (src.is(i + n, Parsing::isDigit)) {
			parsedAny = true;
			whole *= 10;
			whole += src.at(i + n++) - '0';
		}

		if (src.is(i + n, '.')) {
			parsedAny = true;
			n++;

			while (src.is(i + n, Parsing::isDigit)) {
				fract += src.at(i + n++) - '0';
				fract /= 10;
			}
		}

		if (src.is(i + n, 'e') || src.is(i + n, 'E')) {
			n++;
			parsedAny = true;
			boolean expNegative = false;
			boolean parsedE = false;

			if (src.is(i + n, '-')) {
				expNegative = true;
				n++;
			}
			else if (src.is(i + n, '+')) n++;

			while (src.is(i + n, Parsing::isDigit)) {
				parsedE = true;
				exponent *= 10;

				if (expNegative) exponent -= src.at(i + n++) - '0';
				else exponent += src.at(i + n++) - '0';
			}

			if (!parsedE) return ParseRes.error(src.loc(i + n), "Incomplete number exponent");
		}

		if (!parsedAny) {
			if (negative) return ParseRes.error(src.loc(i + n), "Expected number immediatly after minus");
			return ParseRes.failed();
		}
		else if (negative) return ParseRes.res(-(whole + fract) * NumberNode.power(10, exponent), n);
		else return ParseRes.res((whole + fract) * NumberNode.power(10, exponent), n);
	}
	public static ParseRes<Double> parseFloat(Source src, int i, boolean withMinus) {
		var n = skipEmpty(src, i);

		double whole = 0;
		double fract = 0;
		long exponent = 0;
		boolean parsedAny = false;
		boolean negative = false;

		if (withMinus && src.is(i + n, "-")) {
			negative = true;
			n++;
		}

		while (src.is(i + n, Parsing::isDigit)) {
			parsedAny = true;
			whole *= 10;
			whole += src.at(i + n++) - '0';
		}

		if (src.is(i + n, '.')) {
			parsedAny = true;
			n++;

			while (src.is(i + n, Parsing::isDigit)) {
				fract += src.at(i + n++) - '0';
				fract /= 10;
			}
		}

		if (src.is(i + n, 'e') || src.is(i + n, 'E')) {
			n++;
			parsedAny = true;
			boolean expNegative = false;
			boolean parsedE = false;

			if (src.is(i + n, '-')) {
				expNegative = true;
				n++;
			}
			else if (src.is(i + n, '+')) n++;

			while (src.is(i + n, Parsing::isDigit)) {
				parsedE = true;
				exponent *= 10;

				if (expNegative) exponent -= src.at(i + n++) - '0';
				else exponent += src.at(i + n++) - '0';
			}

			if (!parsedE) return ParseRes.error(src.loc(i + n), "Incomplete number exponent");
		}

		if (!parsedAny) {
			if (negative) return ParseRes.error(src.loc(i + n), "Expected number immediatly after minus");
			return ParseRes.failed();
		}
		else if (negative) return ParseRes.res(-(whole + fract) * NumberNode.power(10, exponent), n);
		else return ParseRes.res((whole + fract) * NumberNode.power(10, exponent), n);
	}
	public static ParseRes<Double> parseInt(Source src, int i, String alphabet, boolean withMinus) {
		var n = skipEmpty(src, i);

		double result = 0;
		boolean parsedAny = false;
		boolean negative = false;

		if (withMinus && src.is(i + n, "-")) {
			negative = true;
			n++;
		}

		if (alphabet == null && src.is(i + n, "0x") || src.is(i + n, "0X")) {
			n += 2;

			var res = parseHex(src, i);
			if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Incomplete hexadecimal literal");
			n += res.n;

			if (negative) return ParseRes.res(-res.result, n);
			else return ParseRes.res(res.result, n);
		}

		while (true) {
			var digit = alphabet.indexOf(Character.toLowerCase(src.at(i + n)));
			if (digit < 0) break;

			parsedAny = true;
			result += digit;
			result *= alphabet.length();
		}

		if (!parsedAny) {
			if (negative) return ParseRes.error(src.loc(i + n), "Expected number immediatly after minus");
			return ParseRes.failed();
		}
		else if (negative) return ParseRes.res(-result, n);
		else return ParseRes.res(-result, n);
	}
}
