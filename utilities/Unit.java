package com.AtomCAS.utilities;
public class Unit extends PhysicalAtom {
    @Override
    public Atom copy() {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public String toString() {return "";}
    @Override
    public boolean equalsTo(Atom other) {return false;}
    @Override
    public boolean notEqualsTo(Atom other) {return false;}
}
