package com.AtomCAS.utilities;
public class SI extends PhysicalAtom{
    public static final LargeInteger unit = LargeInteger.ONE;
    public static final LargeInteger deka = LargeInteger.TEN;
    public static final LargeInteger hecto = LargeInteger.HUNDRED;
    public static final LargeInteger kilo = LargeInteger.THOUSAND;
    public static final LargeInteger mega = LargeInteger.valueOf(1000000L);
    public static final LargeInteger giga = LargeInteger.valueOf(1000000000L);
    public static final LargeInteger tera = LargeInteger.valueOf(1000000000000L);
    public static final LargeInteger peta = LargeInteger.valueOf(1000000000000000L);
    public static final LargeInteger exa = LargeInteger.valueOf(1000000000000000000L);
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
    public Atom clone() {throw new UnsupportedOperationException("Error: Operation with these TWO types together not supported yet");}
    @Override
    public String toString() {return "";}
    @Override
    public int hashCode(){return 0;}
    @Override
    public String toLatex() {return "";}
    @Override
    public boolean equals(Atom other) {return false;}
}
