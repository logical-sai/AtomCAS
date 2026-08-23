package com.AtomCAS.utilities;
public class Unit extends PhysicalAtom {
    @Override
    public Atom clone() {throw new UnsupportedOperationException("Error: Operation with these TWO types together not supported yet");}
    @Override
    public String toString() {return "";}
    @Override
    public String toLatex() {return "";}
    public int hashCode(){return 0;}
    @Override
    public boolean equals(Atom other) {return false;}
}
