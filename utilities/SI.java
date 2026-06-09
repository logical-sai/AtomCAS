package com.AtomCAS.utilities;
public class SI extends PhysicalAtom{
    public static final LargeInteger unit = LargeInteger.one;
    public static final LargeInteger deka = LargeInteger.ten;
    public static final LargeInteger hecto = LargeInteger.hundred;
    public static final LargeInteger kilo = LargeInteger.thousand;
    public static final LargeInteger mega = new LargeInteger(1000000L);
    public static final LargeInteger giga = new LargeInteger(1000000000L);
    public static final LargeInteger tera = new LargeInteger(1000000000000L);
    public static final LargeInteger peta = new LargeInteger(1000000000000000L);
    public static final LargeInteger exa = new LargeInteger(1000000000000000000L);
    public static final LargeInteger zetta = (LargeInteger) Parser.parse("10^21");
    public static final LargeInteger yotta = (LargeInteger) Parser.parse("10^24");
    public static final LargeInteger ronna = (LargeInteger) Parser.parse("10^27");
    public static final LargeInteger quetta = (LargeInteger) Parser.parse("10^30");
    public static final LargeFraction deci = deka.reciprocal();
    public static final LargeFraction centi = hecto.reciprocal();
    public static final LargeFraction milli = kilo.reciprocal();
    public static final LargeFraction micro = mega.reciprocal();
    public static final LargeFraction nano = giga.reciprocal();
    public static final LargeFraction pico = tera.reciprocal();
    public static final LargeFraction femto = peta.reciprocal();
    public static final LargeFraction atto = exa.reciprocal();
    public static final LargeFraction zepto = zetta.reciprocal();
    public static final LargeFraction yocto = yotta.reciprocal();
    public static final LargeFraction ronto = ronna.reciprocal();
    public static final LargeFraction quecto = quetta.reciprocal();
    @Override
    public Atom copy() {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public String toString() {return "";}
    @Override
    public boolean equalsTo(Atom other) {return false;}
    @Override
    public boolean notEqualsTo(Atom other) {return false;}
}
