package com.AtomCAS.utilities;
public class Base{
    LargeList<Long> intPart;
    LargeList<Long> terminatingPart;
    LargeList<Long> repeatingPart;
    boolean sign;
    long base;
    public Base(LargeFraction N, long B){
        sign = N.lessThan(LargeFraction.ZERO);
        if(B <= 1) throw new IllegalArgumentException("Error: Invalid base");
        base = B;
        LargeInteger bigBase = LargeInteger.valueOf(B);
        LargeInteger copy = N.abs().intPart();
        //integer part
        intPart = new LargeList<>();
        while(copy.greaterThan(LargeInteger.ZERO)){
            intPart.add(copy.mod(B));
            copy = copy.longDivision(bigBase);
        }
        intPart.reverse();
        LargeFraction copy2 = N.abs().fracPart();
        if(copy2.equals(LargeFraction.ZERO)){
            terminatingPart = new LargeList<>();
            repeatingPart = new LargeList<>();
            return;
        }
        //terminating part
        long terminatingDigits = 0;
        while (copy2.denominator.gcd(bigBase).doesntEqual(LargeInteger.ONE)) {
            copy2 = copy2.multiply(B);
            terminatingDigits++;
        }
        terminatingPart = new LargeList<>();
        copy = copy2.intPart();
        for (long i = 0; i < terminatingDigits; i++) {
            terminatingPart.add(copy.mod(B));
            copy = copy.longDivision(bigBase);
        }
        terminatingPart.reverse();
        //repeating part
        copy2 = copy2.fracPart();
        repeatingPart = new LargeList<>();
        if(copy2.equals(LargeFraction.ZERO)) return;
        LargeInteger start = copy2.numerator;
        LargeInteger R = start;
        do{
            LargeInteger[] DR = R.multiply(B).quotientAndRemainder(copy2.denominator);
            repeatingPart.add(DR[0].mod(B));
            R = DR[1];
        }while(R.doesntEqual(start));
    }
    private String digitToString(long digit) {
        if (digit < 0 || digit >= base)
            throw new IllegalArgumentException("Invalid digit " + digit + " for base " + base);
        if (base <= 62)
            return Character.toString(chars[Math.toIntExact(digit)]);
        return Long.toString(digit);
    }
    private static final char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F', 'G', 'H',  'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    public String toString(){
        StringBuilder sb = new StringBuilder();
        if (sign) sb.append('-');
        //integer part
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(intPart.size()); i = i.add(1)) {
            sb.append(digitToString(intPart.get(i)));
            if (base > 62 && i.lessThan(intPart.size().subtract(1)))
                sb.append(',');
        }
        if(intPart.size().equals(LargeInteger.ZERO)) sb.append('0');
        //terminating part
        if(terminatingPart.size().greaterThan(LargeInteger.ZERO)
                || repeatingPart.size().greaterThan(LargeInteger.ZERO)) sb.append(".");
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(terminatingPart.size()); i= i.add(1)){
            if (base > 62 && i.greaterThan(LargeInteger.ZERO))
                sb.append(",");
            sb.append(digitToString(terminatingPart.get(i)));
        }
        //repeating part
        if (repeatingPart.size().greaterThan(LargeInteger.ZERO)) {
            sb.append("{");
            LargeInteger shown = repeatingPart.size().lesserThanOrEqualTo(LargeInteger.THOUSAND)
                    ? repeatingPart.size() : LargeInteger.THOUSAND;
            for (LargeInteger i = LargeInteger.ZERO; i.lessThan(shown); i = i.add(1)) {
                if (base > 62 && i.greaterThan(LargeInteger.ZERO))
                    sb.append(",");
                sb.append(digitToString(repeatingPart.get(i)));
            }
            if (repeatingPart.size().greaterThan(LargeInteger.THOUSAND))
                sb.append("...(period ").append(repeatingPart.size()).append(")");
            sb.append("}");
        }
        if(base != 10) sb.append(":").append(base);
        return sb.toString();
    }
    public Base convertToBase(long b){
        if(b <= 1) throw new IllegalArgumentException("Error: Invalid base");
        LargeFraction result = LargeFraction.ZERO;
        //integer part
        for (long digit : intPart)
            result = result.multiply(base).add(digit);
        // terminating fractional part
        LargeInteger termValue = LargeInteger.ZERO;
        for (long digit : terminatingPart)
            termValue = termValue.multiply(base).add(digit);
        LargeInteger basePowT = LargeInteger.ONE;
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(terminatingPart.size()); i = i.add(1))
            basePowT = basePowT.multiply(base);
        result = (LargeFraction) result.add(new LargeFraction(termValue, basePowT));
        // repeating fractional part
        if (repeatingPart.size().greaterThan(LargeInteger.ZERO)) {
            LargeInteger repeatValue = LargeInteger.ZERO;
            for (long digit : repeatingPart)
                repeatValue = repeatValue.multiply(base).add(digit);
            LargeInteger basePowR = LargeInteger.ONE;
            for (LargeInteger i = LargeInteger.ZERO; i.lessThan(repeatingPart.size()); i = i.add(1))
                basePowR = basePowR.multiply(base);
            LargeInteger repeatDenominator = (LargeInteger) basePowT.multiply(basePowR.subtract(1));
            result = (LargeFraction) result.add(new LargeFraction(repeatValue, repeatDenominator));
        }
        if (sign)
            result = result.negate();
        return new Base(result, b);
    }
}
