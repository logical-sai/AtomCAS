package com.AtomCAS.utilities;
public class Parser{
    private Parser() {
        throw new UnsupportedOperationException(
                "N O  P A R S E R  I N S T A N C E S  F O R  Y O U,  Y O U  D I R T Y  T H I E F");
    }
    public static ScalarAtom parse(String str) {
        return CASTrace.call("Parser.parse", str, () -> {
            String S = str;
            S = S.replaceAll("\\s+", "");
            S = stripOuterParentheses(S);
            if (S.isEmpty())
                throw new IllegalArgumentException("Invalid input");
            if (S.matches("-?\\d+")) {
                CASTrace.step("Recognized integer literal", S);
                return new LargeInteger(S);
            }
            if (S.matches("-?(\\d+)?\\.(\\d+)?(\\{\\d+})?")) {
                CASTrace.step("Recognized decimal/repeating fraction literal", S);
                return parseLargeFraction(S);
            }
            String withImplicitMultiplication = insertImplicitMultiplication(S);
            if (!withImplicitMultiplication.equals(S))
                CASTrace.value("Inserted implicit multiplication", S, withImplicitMultiplication);
            return parseAddSubtract(withImplicitMultiplication);
        });
    }
    private static String insertImplicitMultiplication(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (i > 0) {
                char prev = str.charAt(i - 1);
                if((prev == ')' || prev == ']' || prev == '}') && str.substring(i, i+1).matches("[0-9(]")) result.append("*");
            }
            result.append(c);
            if(i < str.length() - 1) {
                char next = str.charAt(i + 1);
                if((next == '(' || next == '[' || next == '√') && (str.substring(i, i+1).matches("\\d"))) result.append("*");
            }
        }
        return result.toString();
    }
    private static String stripOuterParentheses(String str) {
        while (str.startsWith("(") && str.endsWith(")") && wrapsWholeExpression(str))
            str = str.substring(1, str.length() - 1);
        return str;
    }
    private static boolean wrapsWholeExpression(String str) {
        int depth = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (depth == 0 && i < str.length() - 1)
                return false;
            if (depth < 0)
                throw new IllegalArgumentException("Mismatched parentheses");
        }
        return depth == 0;
    }
    private static ScalarAtom parseAddSubtract(String str) {
        return CASTrace.call("Parser.parseAddSubtract", str, () -> {
            int index = findMainOperator(str, "+", "-");
            if (index == -1) return parseMod(str);
            String leftText = str.substring(0, index);
            String rightText = str.substring(index + 1);
            char op = str.charAt(index);
            CASTrace.step("Found main operator", String.valueOf(op));
            if(leftText.isEmpty()) return parse(rightText).negate();
            ScalarAtom left = parse(leftText);
            ScalarAtom right = parse(rightText);
            if (op == '+') return left.add(right);
            return left.subtract(right);
        });
    }
    private static ScalarAtom parseMod(String str){
        return CASTrace.call("Parser.parseMod", str, () -> {
            int index = findMainModOperator(str);
            if (index == -1) return parseMultiplyDivide(str);
            String leftText = str.substring(0, index);
            String rightText = str.substring(index + 1);
            char op = str.charAt(index);
            CASTrace.step("Found main operator", String.valueOf(op));
            ScalarAtom left = parse(leftText);
            ScalarAtom right = parse(rightText);
            return left.mod(right);
        });
    }
    private static ScalarAtom parseMultiplyDivide(String str) {
        return CASTrace.call("Parser.parseMultiplyDivide", str, () -> {
            int index = findMainOperator(str, "*", "/");
            if (index == -1) return parsePercent(str);
            String leftText = str.substring(0, index);
            String rightText = str.substring(index + 1);
            char op = str.charAt(index);
            CASTrace.step("Found main operator", String.valueOf(op));
            ScalarAtom left = parse(leftText);
            ScalarAtom right = parse(rightText);
            if(op == '*')
                return left.multiply(right);
            return left.divide(right);
        });
    }

    private static ScalarAtom parsePercent(String str){
        return CASTrace.call("Parser.parsePercent", str, ()->{
            int index = findMainOperator(str, "%", "‰");
            if (index == -1) return parseSquareRoot(str);
            String left = str.substring(0, index);
            String right = str.substring(index + 1);
            if (left.isEmpty()) return LargeInteger.ZERO;
            if (str.charAt(index) == '%') {
                if (right.isEmpty()) return parse(left).percent();
                return parse(left).percent().multiply(parse(right));
            }
            if (right.isEmpty()) return parse(left).permille();
            return parse(left).permille().multiply(parse(right));
        });
    }
    private static ScalarAtom parseFactorial(String str){
        return CASTrace.call("Parser.parseFactorial", str, ()->{
            int index = findMainOperator(str, "!");
            if (index == -1) throw new IllegalArgumentException("Invalid input");
            String left = str.substring(0, index);
            String right = str.substring(index + 1);
            ScalarAtom S;
            if (left.matches("-?\\d+"))
                S = ((LargeInteger) parse(left)).factorial();
            else throw new IllegalArgumentException("Factorial currently only defined for positive integers");
            if (right.isEmpty()) return S;
            return S.multiply(parse(right));
        });
    }
    private static ScalarAtom parseSquareRoot(String str){
        return CASTrace.call("Parser.parseSquareRoot", str, ()->{
            int index = findMainSqrtOperator(str);
            if (index == -1) return parsePower(str);
            if (str.length() == 1) throw new IllegalArgumentException("Invalid input");
            String left = str.substring(0, index);
            ScalarAtom leftAtom;
            if (left.isEmpty()) leftAtom = LargeInteger.ONE;
            else leftAtom = parse(left);
            StringBuilder right = new StringBuilder();
            int index2 = index + 1;
            int depth = 0;
            if (str.charAt(index2) == '(') {
                depth++;
                while (index2 < str.length() && depth != 0) {
                    right.append(str.charAt(index2));
                    if (str.charAt(index2) == '(') depth++;
                    else if (str.charAt(index2) == ')') depth--;
                    index2++;
                }
            } else {
                while (index2 < str.length() && (str.substring(index2, index2 + 1).matches("\\d") || str.charAt(index2) == '.' || str.charAt(index2) == '{' || str.charAt(index2) == '}' || str.charAt(index2) == '-' || str.charAt(index2) == '√' || str.charAt(index2) == '^')) {
                    right.append(str.charAt(index2));
                    index2++;
                }
            }
            ScalarAtom rightAtom = parse(right.toString());
            if (rightAtom.isRational()) {
                switch (rightAtom) {
                    case LargeInteger largeInteger ->
                            rightAtom = new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, new LargePowerMonomial(new LargeFraction(largeInteger, LargeInteger.ONE), LargeFraction.HALF));
                    case LargeFraction largeFraction ->
                            rightAtom = new LargePowerMonomial(largeFraction, LargeFraction.HALF).add(LargeInteger.ZERO);
                    case LargePowerMonomial L -> rightAtom = L.power(LargeFraction.HALF).add(LargeInteger.ZERO);
                    case RadicalTerm RT ->
                            rightAtom = new LargePowerMonomial(RT.rationalPart, LargeFraction.HALF).add(LargeInteger.ZERO);
                    case RadicalTermSum RTS ->
                            rightAtom = new LargePowerMonomial(RTS.constant, LargeFraction.HALF).add(LargeInteger.ZERO);
                    default -> {}
                }
            }else rightAtom = rightAtom.power(LargeFraction.HALF);
            if (index2 != str.length()) {
                ScalarAtom righter = parse(str.substring(index2 + 1));
                return leftAtom.multiply(rightAtom).add(righter);
            }
            return leftAtom.multiply(rightAtom);
        });
    }
    private static ScalarAtom parsePower(String str) {
        return CASTrace.call("Parser.parsePower", str, () -> {
            int index = findMainPowerOperator(str);
            if (index == -1) return parseFactorial(str);
            ScalarAtom base = parse(str.substring(0, index));
            ScalarAtom exp = parse(str.substring(index + 1));
            exp = parse(exp.toString());
            if (exp.equals(LargeInteger.ZERO) && base.equals(LargeInteger.ZERO))
                throw new ArithmeticException("Indeterminate form: 0^0");
            switch (exp) {
                case LargeInteger _ -> {
                    if (base.equals(LargeInteger.TWO))
                        return LargeInteger.powerOfTwo(Long.parseLong(exp.toString()));
                    else if (exp.equals(LargeInteger.ZERO)) return LargeInteger.ONE;
                    else if (base.equals(LargeInteger.ZERO)) return LargeInteger.ZERO;
                    else if (base.equals(LargeInteger.ONE)) return LargeInteger.ONE;
                    else if (base.equals(LargeInteger.NEGATIVE_ONE))
                        return ((LargeInteger) exp).isOdd() ? LargeInteger.NEGATIVE_ONE : LargeInteger.ONE;
                    LargeInteger expTemp = (LargeInteger) exp.abs();
                    ScalarAtom basTemp = (ScalarAtom) base.clone();
                    ScalarAtom res = LargeInteger.ONE;
                    while (expTemp.greaterThan(LargeInteger.ZERO)) {
                        if (expTemp.isOdd()) {
                            res = res.multiply(basTemp);
                            if (expTemp.equals(LargeInteger.ONE)) break;
                        }
                        basTemp = basTemp.square();
                        expTemp = expTemp.divide(LargeInteger.TWO);
                    }
                    if (exp.lessThan(LargeInteger.ZERO)) return res.reciprocal();
                    return res;
                }
                case LargeFraction largeFraction -> {
                    if (base instanceof LargeInteger || base instanceof LargeFraction)
                        return new LargePowerMonomial((LargeFraction) base.divide(LargeInteger.ONE), largeFraction).add(LargeInteger.ZERO);
                    return new LargePowerNode(base, largeFraction, LargeFraction.ONE);
                }
                default -> throw new IllegalArgumentException("Power must be rational");
            }
        });
    }
    private static LargeFraction parseLargeFraction(String str){
        LargeInteger num;
        LargeInteger dem = LargeInteger.ONE;
        if (str.contains(".")){
            if (str.contains("..")) throw new IllegalArgumentException("Invalid input");
            if (str.contains("{") || str.contains("}")) {
                if (!str.contains("}")) str += "}";
                if (!str.contains("{")) str = str.replace(".", ".{");
                if (str.indexOf("{") != str.lastIndexOf("{") || str.indexOf("}") != str.lastIndexOf("}"))
                    throw new IllegalArgumentException("Invalid input");
                String nonRepeat = str.substring(str.indexOf(".") + 1, str.indexOf("{"));
                String repeat = str.substring(str.indexOf("{") + 1, str.indexOf("}"));
                String whole = str.substring(0, str.indexOf("."));
                if(repeat.isEmpty()) repeat += 0;
                if (whole.isEmpty()) whole += 0;
                int expTemp = repeat.length();
                LargeInteger basTemp = LargeInteger.TEN;
                while(true){
                    if(expTemp % 2 == 1){
                        dem = dem.multiply(basTemp);
                        if(expTemp == 1) break;
                    }
                    basTemp = basTemp.square();
                    expTemp /= 2;
                }
                dem = dem.subtract(1);
                expTemp = nonRepeat.length();
                basTemp = LargeInteger.TEN;
                while(expTemp != 0) {
                    if ((expTemp & 1) == 1){
                        dem = dem.multiply(basTemp);
                        if(expTemp == 1) break;
                    }
                    basTemp = basTemp.square();
                    expTemp >>>= 1;
                }
                if(nonRepeat.isEmpty()) nonRepeat += 0;
                num = new LargeInteger(nonRepeat + repeat).subtract(new LargeInteger(nonRepeat));
                if(whole.charAt(0) == '-') num = num.multiply(-1);
                num = num.add(new LargeInteger(whole).multiply(dem));
            } else if (str.contains("}") && !str.contains("{"))
                throw new IllegalArgumentException("Invalid Input");
            else{
                while(str.endsWith("0"))
                    str = str.substring(0, str.length() - 1);
                String nonRepeat = str.substring(str.indexOf(".") + 1);
                String whole = str.substring(0, str.indexOf("."));
                int expTemp = nonRepeat.length();
                LargeInteger basTemp = LargeInteger.TEN;
                while(expTemp != 0) {
                    if ((expTemp & 1) == 1){
                        dem = dem.multiply(basTemp);
                        if(expTemp == 1) break;
                    }
                    basTemp = basTemp.square();
                    expTemp >>>= 1;
                }
                if (nonRepeat.isEmpty()) nonRepeat += 0;
                if(whole.startsWith("-")) nonRepeat = "-" + nonRepeat;
                if (whole.isEmpty()) whole += 0;
                num = new LargeInteger(nonRepeat).add(new LargeInteger(whole).multiply(dem));
            }
        }else{
            if(str.contains("{") || str.contains("}"))
                throw new IllegalArgumentException("Invalid input");
            num = (LargeInteger) Parser.parse(str);
            return new LargeFraction(num, dem);
        }
        return new LargeFraction(num, dem);
    }
    private static boolean isUnaryMinus(String str, int index) {
        if (str.charAt(index) != '-') return false;
        if(index == 0) return false;
        char prev = str.charAt(index - 1);
        return prev == '+' || prev == '-' || prev == '*' || prev == '/' || prev == '^' || prev == 'd' || prev == '√';
    }
    private static int findMainOperator(String str, String... ops) {
        int depth = 0;
        for (int i = str.length(); i > 0; i--) {
            String c = str.substring(i - 1, i);
            if (c.equals(")"))
                depth++;
            else if (c.equals("("))
                depth--;
            else if (depth == 0) {
                for (String op : ops) {
                    if (c.equals(op) && !isUnaryMinus(str, i-1))
                        return i - 1;
                }
            }
        }
        if (depth != 0)
            throw new IllegalArgumentException("Mismatched parentheses");
        return -1;
    }
    private static int findMainSqrtOperator(String str){
        int depth = 0;
        for(int i = 0; i < str.length(); i++) {
            for (int j = i+1; j <= str.length(); j++){
                String c = str.substring(i, j);
                if (c.equals(")"))
                    depth++;
                else if (c.equals("("))
                    depth--;
                else if (depth == 0) {
                    if (c.equals("√")) return i;
                }
            }
        }
        if (depth != 0)
            throw new IllegalArgumentException("Mismatched parentheses");
        return -1;
    }
    private static int findMainPowerOperator(String str) {
        int depth = 0;
        for(int i = 0; i < str.length(); i++) {
            for (int j = i+1; j <= str.length(); j++){
                String c = str.substring(i, j);
                if (c.equals(")"))
                    depth++;
                else if (c.equals("("))
                    depth--;
                else if (depth == 0)
                    if (c.equals("^")) return i;
            }
        }
        if (depth != 0)
            throw new IllegalArgumentException("Mismatched parentheses");
        return -1;
    }
    private static int findMainModOperator(String str) {
        int depth = 0;
        for(int i = 0; i < str.length(); i++) {
            for (int j = i+3; j <= str.length(); j++){
                String c = str.substring(i, j);
                if (c.equals(")")) depth++;
                else if (c.equals("(")) depth--;
                else if (depth == 0)
                    if (c.equals("mod")) return i;
            }
        }
        if (depth != 0)
            throw new IllegalArgumentException("Mismatched parentheses");
        return -1;
    }
}
