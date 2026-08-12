package com.AtomCAS.utilities;
public final class AtomHash {
    private AtomHash() {}
    public static int rational(LargeInteger numerator, LargeInteger denominator) {
        if (denominator.equals(LargeInteger.ONE))
            return integer(numerator);
        int hash = 17;
        hash = 31 * hash + integer(numerator);
        hash = 31 * hash + integer(denominator);
        return hash;
    }
    public static int integer(LargeInteger value) {return 0;} // Hash normalized sign and limbs
    public static int combine(int seed, int value) {return 31 * seed + value;}
}
