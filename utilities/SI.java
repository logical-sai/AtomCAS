package com.AtomCAS.utilities;
public class SI extends PhysicalAtom{
    public static final LargeFraction quecto = (LargeFraction) Parser.parse("1/1000000000000000000000000000000");
    public static final LargeFraction ronto = (LargeFraction) Parser.parse("1/1000000000000000000000000000");
    public static final LargeFraction yocto = (LargeFraction) Parser.parse("1/1000000000000000000000000");
    public static final LargeFraction zepto = (LargeFraction) Parser.parse("1/1000000000000000000000");
    public static final LargeFraction atto = (LargeFraction) Parser.parse("1/1000000000000000000");
    public static final LargeFraction femto = (LargeFraction) Parser.parse("1/1000000000000000");
    public static final LargeFraction pico = (LargeFraction) Parser.parse("1/1000000000000");
    public static final LargeFraction nano = (LargeFraction) Parser.parse("1/1000000000");
    public static final LargeFraction micro = (LargeFraction) Parser.parse("1/1000000");
    static final LargeFraction milli = (LargeFraction) Parser.parse("1/1000");
    static final LargeFraction centi = (LargeFraction) Parser.parse("1/100");
    static final LargeFraction deci = (LargeFraction) Parser.parse("1/10");
    static final LargeFraction unit = LargeFraction.one;
    static final LargeFraction deka = (LargeFraction) Parser.parse("10");
    static final LargeFraction hecto = (LargeFraction) Parser.parse("100");
    static final LargeFraction kilo = (LargeFraction) Parser.parse("1000");
    static final LargeFraction mega = (LargeFraction) Parser.parse("1000000");
    static final LargeFraction giga = (LargeFraction) Parser.parse("1000000000");
    static final LargeFraction tera = (LargeFraction) Parser.parse("1000000000000");
    static final LargeFraction peta = (LargeFraction) Parser.parse("1000000000000000");
    static final LargeFraction exa = (LargeFraction) Parser.parse("1000000000000000000");
    static final LargeFraction zetta = (LargeFraction) Parser.parse("1000000000000000000000");
    static final LargeFraction yotta = (LargeFraction) Parser.parse("1000000000000000000000000");
    static final LargeFraction ronna = (LargeFraction) Parser.parse("1000000000000000000000000000");
    static final LargeFraction quetta = (LargeFraction) Parser.parse("1000000000000000000000000000000");
    @Override
    public Atom copy() {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public String toString() {return "";}
    @Override
    public boolean equalsTo(Atom other) {return false;}
    @Override
    public boolean notEqualsTo(Atom other) {return false;}
}
