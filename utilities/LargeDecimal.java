package com.AtomCAS.utilities;
public class LargeDecimal extends ScalarAtom{
    LargeInteger mantissa;
    LargeInteger scale;
    public LargeDecimal(LargeInteger mantissa, LargeInteger scale){
        this.mantissa = mantissa;
        this.scale = scale;
    }
    @Override
    public Atom copy() {return new LargeDecimal(mantissa, scale);}
    @Override
    public String toString() {
        if (mantissa.equalsTo(LargeInteger.zero))
            return "0";
        boolean negative = mantissa.sign().equalsTo(LargeInteger.negativeOne);
        String digits = mantissa.magnitude().toString();
        StringBuilder sb = new StringBuilder();
        if (negative) sb.append("-");
        // scale == 0: ordinary integer
        if (scale.equalsTo(LargeInteger.zero)) {
            sb.append(digits);
            return sb.toString();
        }
        // scale < 0: append zeros to the right
        if (scale.lessThan(LargeInteger.zero)) {
            sb.append(digits);
            LargeInteger zeros = scale.negate();
            for (LargeInteger i = LargeInteger.zero; i.lessThan(zeros); i = i.add(1))
                sb.append("0");
            return sb.toString();
        }

        // scale > 0: decimal point goes scale digits from the right
        LargeInteger digitCount = new LargeInteger(digits.length());

        if (scale.lessThan(digitCount)) {
            int pointIndex = Integer.parseInt(digitCount.subtract(scale).toString());
            sb.append(digits, 0, pointIndex);
            sb.append(".");
            sb.append(digits.substring(pointIndex));
        } else {
            sb.append("0.");
            LargeInteger leadingZeros = (LargeInteger) scale.subtract(digitCount);
            for (LargeInteger i = LargeInteger.zero; i.lessThan(leadingZeros); i = i.add(1))
                sb.append("0");
            sb.append(digits);
        }

        return sb.toString();
    }

    @Override
    public boolean equalsTo(Atom other) {
        return false;
    }

    @Override
    public boolean notEqualsTo(Atom other) {
        return false;
    }
    @Override
    public LargeInteger floor() {
        return null;
    }
    @Override
    public LargeInteger ceil() {
        return null;
    }

    @Override
    public long compareMags(ScalarAtom other) {
        return 0;
    }

    @Override
    public long compareTo(ScalarAtom other) {
        return 0;
    }

    @Override
    public ScalarAtom negate() {
        return null;
    }

    @Override
    public ScalarAtom magnitude() {
        return null;
    }

    @Override
    public boolean isRational() {
        return false;
    }

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
    public boolean isNotReal() {
        return false;
    }

    @Override
    public boolean isAlgebraic() {
        return false;
    }

    @Override
    public ScalarAtom realPart() {
        return null;
    }

    @Override
    public ScalarAtom imagPart() {
        return null;
    }

    @Override
    public ScalarAtom add(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom reciprocal() {
        return null;
    }

    @Override
    public ScalarAtom square() {
        return null;
    }

    @Override
    public ScalarAtom cube() {
        return null;
    }

    @Override
    public ScalarAtom power(LargeFraction f) {
        return null;
    }

    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {
        return this;
    }
}
