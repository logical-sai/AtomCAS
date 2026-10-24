package com.AtomCAS.utilities;
import java.util.ArrayList;
public class Parser{
    private static final long BASE = 1000000000L;
    private static final int CHUNK_SIZE = 9;
    public static ScalarAtom parse(String str) {
        str = str.replaceAll("\\s+", "");
        str = stripOuterParentheses(str);
        if (str.isEmpty()) throw new IllegalArgumentException("Error: Invalid input");
        if(str.matches("-?\\d+"))
            return parseLargeInteger(str);
        if(str.matches("-?(\\d+)?\\.(\\d+)?(\\{\\d+})?"))
            return parseLargeFraction(str);
        if(str.matches("-?\\d+(\\.\\d+)+"))
            throw new IllegalArgumentException("Error: Invalid input");
        str = insertImplicitMultiplication(str);
        return parseAddSubtract(str);
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
                throw new IllegalArgumentException("Error: Mismatched parentheses");
        }
        return depth == 0;
    }
    private static ScalarAtom parseAddSubtract(String str){
        if(str.indexOf("-") == 0)
            return LargeInteger.zero.subtract(parse(str.substring(1)));
        int index = findMainOperator(str, "+", "-");
        if (index != -1) {
            ScalarAtom left = parse(str.substring(0, index));
            ScalarAtom right = parse(str.substring(index + 1));
            if (str.charAt(index) == '+') return left.add(right);
            return left.subtract(right);
        }
        return parseMod(str);
    }
    private static ScalarAtom parseMod(String str){
        int index = findMainModOperator(str);
        if (index != -1) {
            ScalarAtom divisor = parse(str.substring(0, index));
            ScalarAtom dividend = parse(str.substring(index+3));
            return divisor.mod(dividend);
        }
        return parseMultiplyDivide(str);
    }
    private static ScalarAtom parseMultiplyDivide(String str) {
        int index = findMainOperator(str, "*", "/");
        if (index != -1) {
            ScalarAtom left = parse(str.substring(0, index));
            ScalarAtom right = parse(str.substring(index + 1));
            if (str.charAt(index) == '*') return left.multiply(right);
            return left.divide(right);
        }
        return parsePercent(str);
    }
    private static ScalarAtom parsePercent(String str){
        int index = findMainOperator(str, "%", "‰");
        if (index != -1){
            String left = str.substring(0, index);
            String right = str.substring(index + 1);
            if(left.isEmpty()) return LargeInteger.zero;
            if (str.charAt(index) == '%'){
                if(right.isEmpty()) return parse(left).percent();
                return parse(left).percent().multiply(parse(right));
            }else if (str.charAt(index) == '‰'){
                if(right.isEmpty()) return parse(left).permille();
                return parse(left).permille().multiply(parse(right));
            }
        }
        return parseFactorial(str);
    }
    private static ScalarAtom parseFactorial(String str){
        int index = findMainOperator(str, "!");
        if(index == 0) throw new IllegalArgumentException("Error: Invalid input");
        if (index != -1){
            String left = str.substring(0, index);
            if(left.matches("-?\\d+")) return parse(parseLargeInteger(left).factorial().toString() + str.substring(index+1));
            throw new IllegalArgumentException("Error: Factorial currently only defined for positive integers");
        }
        return parseSquareRoot(str);
    }
    private static ScalarAtom parseSquareRoot(String str){
        int index = findMainSqrtOperator(str);
        if (index != -1){
            if(str.length() == 1) throw new IllegalArgumentException("Error: Invalid input");
            String left = str.substring(0, index);
            ScalarAtom leftAtom;
            if(left.isEmpty()) leftAtom = LargeInteger.one;
            else leftAtom = parse(left);
            StringBuilder right = new StringBuilder();
            int index2 = index+1;
            int depth = 0;
            if(str.charAt(index2) == '('){
                depth++;
                while(index2 < str.length() && depth != 0){
                    right.append(str.charAt(index2));
                    if(str.charAt(index2) == '(') depth++;
                    else if(str.charAt(index2) == ')') depth--;
                    index2++;
                }
            }else{
                while(index2 < str.length() && (str.substring(index2, index2+1).matches("\\d") || str.charAt(index2) == '.' || str.charAt(index2) == '{' || str.charAt(index2) == '}' || str.charAt(index2) == '-' || str.charAt(index2) == '√' || str.charAt(index2) == '^')){
                    right.append(str.charAt(index2));
                    index2++;
                }
            }
            ScalarAtom rightAtom = parse(right.toString());
            if(rightAtom.isRational()){
                if(rightAtom instanceof LargeInteger || rightAtom instanceof LargeFraction)
                    rightAtom = new LargePowerMonomial((LargeFraction) rightAtom.divide(LargeInteger.one), LargeFraction.half).add(LargeInteger.zero);
                else if(rightAtom instanceof LargePowerMonomial)
                    rightAtom = ((LargePowerMonomial)rightAtom).power(LargeFraction.half).add(LargeInteger.zero);
                else if(rightAtom instanceof RadicalTerm)
                    rightAtom = new LargePowerMonomial(((RadicalTerm) rightAtom).rationalPart, LargeFraction.half).add(LargeInteger.zero);
            }else rightAtom = rightAtom.power(LargeFraction.half);
            if(index2 != str.length()){
                ScalarAtom righter = parse(str.substring(index2+1));
                return leftAtom.multiply(rightAtom).add(righter);
            }
            return leftAtom.multiply(rightAtom).add(LargeInteger.zero);
        }
        return parsePower(str);
    }
    private static ScalarAtom parsePower(String str) {
        int index = findMainPowerOperator(str);
        if(index == -1) return parse(str);
        ScalarAtom base = parse(str.substring(0, index));
        ScalarAtom exp = parse(str.substring(index + 1));
        if(exp.isRational()){
            switch (exp) {
                case LargeInteger _ -> {
                    LargeInteger expTemp = (LargeInteger) exp.magnitude();
                    ScalarAtom basTemp = (ScalarAtom) base.copy();
                    ScalarAtom res = LargeInteger.one;
                    while (expTemp.greaterThan(LargeInteger.zero)) {
                        if (expTemp.isOdd()){
                            res = res.multiply(basTemp);
                            if(expTemp.equals(LargeInteger.one)) break;
                        }
                        basTemp = basTemp.square();
                        expTemp = expTemp.longDivision(LargeInteger.two);
                    }
                    if (exp.lessThan(LargeInteger.zero)) return res.reciprocal();
                    return res;
                }case LargeFraction largeFraction -> {
                    if (base instanceof LargeInteger || base instanceof LargeFraction)
                        return new LargePowerMonomial((LargeFraction) base.divide(LargeInteger.one), largeFraction).add(LargeInteger.zero);
                    return new LargePowerNode(base, largeFraction, LargeFraction.zero).add(LargeInteger.zero);
                }case RadicalTerm radicalTerm -> {
                    if (base instanceof LargeInteger || base instanceof LargeFraction)
                        return new LargePowerMonomial((LargeFraction) base.divide(LargeInteger.one), radicalTerm.rationalPart).add(LargeInteger.zero);
                    return new LargePowerNode(base, radicalTerm.rationalPart, LargeFraction.zero).add(LargeInteger.zero);
                }case RadicalTermSum rT -> {
                    if (base instanceof LargeInteger || base instanceof LargeFraction)
                        return new LargePowerMonomial((LargeFraction) base.divide(LargeInteger.one), rT.constant).add(LargeInteger.zero);
                    return new LargePowerNode(base, rT.constant, LargeFraction.zero).add(LargeInteger.zero);
                }case LargePowerNode largePowerNode -> {
                    if (base instanceof LargeInteger || base instanceof LargeFraction){
                        if(base.lessThan(LargeInteger.zero))
                            return new LargePowerMonomial((LargeFraction) LargeInteger.one.divide(base), (LargeFraction) largePowerNode.base).add(LargeInteger.zero);
                        return new LargePowerMonomial((LargeFraction) base.divide(LargeInteger.one), (LargeFraction) largePowerNode.base).add(LargeInteger.zero);
                    }
                    if(base.lessThan(LargeInteger.zero))
                        return new LargePowerNode(base, (LargeFraction) largePowerNode.base, LargeFraction.zero).reciprocal().add(LargeInteger.zero);
                    return new LargePowerNode(base, (LargeFraction) largePowerNode.base, LargeFraction.zero).add(LargeInteger.zero);
                }
                default -> throw new IllegalArgumentException("Error: Power must be rational");
            }
        }
        throw new IllegalArgumentException("Error: Power must be rational");
    }
    private static LargeInteger parseLargeInteger(String str) {
        int sign = 1;
        if(str.isEmpty()) return LargeInteger.zero;
        else if (str.charAt(0) == '-') {
            sign = -1;
            str = str.substring(1);
        }
        // eliminate leading zeros
        while (str.length() > 1 && str.charAt(0) == '0')
            str = str.substring(1);
        ArrayList<Long> chunks = new ArrayList<>();
        for (int j = str.length(); j > 0; j -= CHUNK_SIZE) {
            int start = Math.max(0, j - CHUNK_SIZE);
            chunks.addFirst(Long.parseLong(str.substring(start, j)));
        }
        long[] arr = new long[chunks.size()];
        for(int i = 0; i < arr.length; i++) arr[i] = chunks.get(i);
        return new LargeInteger(arr, sign);
    }
    private static LargeFraction parseLargeFraction(String str){
        LargeInteger num;
        LargeInteger dem = new LargeInteger(1);
        if (str.contains(".")){
            if (str.contains("..")) throw new IllegalArgumentException("Error: Invalid input");
            if (str.contains("{") || str.contains("}")) {
                if (!str.contains("}")) str += "}";
                if (!str.contains("{")) str = str.replace(".", ".{");
                if (str.indexOf("{") != str.lastIndexOf("{") || str.indexOf("}") != str.lastIndexOf("}"))
                    throw new IllegalArgumentException("Error: Invalid input");
                String nonRepeat = str.substring(str.indexOf(".") + 1, str.indexOf("{"));
                String repeat = str.substring(str.indexOf("{") + 1, str.indexOf("}"));
                String whole = str.substring(0, str.indexOf("."));
                if(repeat.isEmpty()) repeat += 0;
                if (whole.isEmpty()) whole += 0;
                dem = (LargeInteger) LargeInteger.pow10(repeat.length()).subtract(LargeInteger.one);
                dem = (LargeInteger) dem.multiply(LargeInteger.pow10(nonRepeat.length()));
                num = (LargeInteger) parseLargeInteger(nonRepeat + repeat).subtract(parseLargeInteger(nonRepeat));
                if(whole.charAt(0) == '-') num = num.multiply(-1);
                num = (LargeInteger) num.add(parseLargeInteger(whole).multiply(dem));
            } else if (str.contains("}") && !str.contains("{"))
                throw new IllegalArgumentException("Error: Invalid Input");
            else{
                while(str.endsWith("0"))
                    str = str.substring(0, str.length() - 1);
                String nonRepeat = str.substring(str.indexOf(".") + 1);
                String whole = str.substring(0, str.indexOf("."));
                dem = LargeInteger.pow10(nonRepeat.length());
                if (nonRepeat.isEmpty()) nonRepeat += 0;
                if(whole.startsWith("-")) nonRepeat = "-" + nonRepeat;
                if (whole.isEmpty()) whole += 0;
                num = (LargeInteger) parseLargeInteger(nonRepeat).add(parseLargeInteger(whole).multiply(dem));
            }
        }else{
            if(str.contains("{") || str.contains("}"))
                throw new IllegalArgumentException("Error: Invalid input");
            num = (LargeInteger) Parser.parse(str);
            return new LargeFraction(num, dem);
        }
        if (dem.sign().equalsTo(LargeInteger.zero))
            throw new ArithmeticException("Error: Cannot divide by 0");
        if (dem.sign().equalsTo(LargeInteger.negativeOne)) {
            num = (LargeInteger) num.negate();
            dem = (LargeInteger) dem.negate();
        }
        LargeInteger g = num.gcd(dem);
        num = num.longDivision(g);
        dem = dem.longDivision(g);
        return new LargeFraction(num, dem);
    }
    private static int lastBinaryMinus(String str) {
        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) == '-') {
                if (i == 0) break; // unary negative at front
                char prev = str.charAt(i - 1);
                // If the previous char is an operator, then this '-' is unary.
                if (prev == '+' || prev == '-' || prev == '*' || prev == '/')
                    continue;
                return i;
            }
        }
        return -1;
    }
    private static boolean isUnaryMinus(String str, int index) {
        if (str.charAt(index) != '-') return false;
        char prev = str.charAt(index - 1);
        return prev == '+' || prev == '-' || prev == '*' || prev == '/' || prev == '^' || prev == '√';
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
                else if (depth == 0) {
                    if (c.equals("^")) return i;
                }
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
                if (c.equals(")"))
                    depth++;
                else if (c.equals("("))
                    depth--;
                else if (depth == 0) {
                    if (c.equals("mod")) return i;
                }
            }
        }
        if (depth != 0)
            throw new IllegalArgumentException("Mismatched parentheses");
        return -1;
    }
}
