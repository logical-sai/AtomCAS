package com.AtomCAS.utilities;
import java.security.SecureRandom;
import java.util.ArrayList;
public class LargeInteger extends ScalarAtom {
    //instance variables
    private final long[] number;
    public static final LargeInteger negativeOne = new LargeInteger(-1L);
    public static final LargeInteger zero = new LargeInteger(0L);
    public static final LargeInteger one = new LargeInteger(1L);
    public static final LargeInteger two = new LargeInteger(2L);
    public static final LargeInteger three = new LargeInteger(3L);
    public static final LargeInteger four = new LargeInteger(4L);
    public static final LargeInteger five = new LargeInteger(5L);
    public static final LargeInteger six = new LargeInteger(6L);
    public static final LargeInteger seven  = new LargeInteger(7L);
    public static final LargeInteger eight = new LargeInteger(8L);
    public static final LargeInteger nine = new LargeInteger(9L);
    public static final LargeInteger ten = new LargeInteger(10L);
    public static final LargeInteger eleven = new LargeInteger(11L);
    public static final LargeInteger thirteen = new LargeInteger(13L);
    public static final LargeInteger seventeen = new LargeInteger(17L);
    public static final LargeInteger nineteen = new LargeInteger(19L);
    public static final LargeInteger twentyThree = new LargeInteger(23L);
    public static final LargeInteger twentyNine = new LargeInteger(29L);
    public static final LargeInteger thirtyOne = new LargeInteger(31L);
    public static final LargeInteger thirtySeven = new LargeInteger(37L);
    public static final LargeInteger fortyOne = new LargeInteger(41L);
    public static final LargeInteger fortyThree = new LargeInteger(43L);
    public static final LargeInteger fortySeven = new LargeInteger(47L);
    public static final LargeInteger fiftyThree = new LargeInteger(53L);
    public static final LargeInteger fiftyNine = new LargeInteger(59L);
    public static final LargeInteger sixtyOne = new LargeInteger(61L);
    public static final LargeInteger sixtySeven = new LargeInteger(67L);
    public static final LargeInteger seventyOne = new LargeInteger(71L);
    public static final LargeInteger seventyThree = new LargeInteger(73L);
    public static final LargeInteger seventyNine = new LargeInteger(79L);
    public static final LargeInteger eightyThree = new LargeInteger(83L);
    public static final LargeInteger eightyNine = new LargeInteger(89L);
    public static final LargeInteger ninetySeven = new LargeInteger(97L);
    public static final LargeInteger hundred = new LargeInteger(100L);
    public static final LargeInteger hundredOne = new LargeInteger(101L);
    public static final LargeInteger hundredThree = new LargeInteger(103L);
    public static final LargeInteger hundredSeven  = new LargeInteger(107L);
    public static final LargeInteger hundredNine = new LargeInteger(109L);
    public static final LargeInteger hundredThirteen = new LargeInteger(113L);
    public static final LargeInteger hundredTwentySeven = new LargeInteger(127L);
    public static final LargeInteger hundredThirtyOne = new LargeInteger(131L);
    public static final LargeInteger hundredThirtySeven = new LargeInteger(137L);
    public static final LargeInteger hundredThirtyNine = new LargeInteger(139L);
    public static final LargeInteger hundredFortyNine = new LargeInteger(149L);
    public static final LargeInteger hundredFiftyOne = new LargeInteger(151L);
    public static final LargeInteger hundredFiftySeven = new LargeInteger(157L);
    public static final LargeInteger thousand = new LargeInteger(1000L);
    private static final long BASE = 1000000000L;
    LargeInteger[] smallPrimes = {two, three, five, seven, eleven, thirteen, seventeen, nineteen, twentyThree, twentyNine, thirtyOne, thirtySeven,
            fortyOne, fortyThree, fortySeven, fiftyThree, fiftyNine, sixtyOne, sixtySeven, seventyOne, seventyThree, seventyNine, eightyThree, eightyNine,
            ninetySeven, hundredOne, hundredThree, hundredSeven, hundredNine, hundredThirteen, hundredTwentySeven, hundredThirtyOne, hundredThirtySeven,
            hundredThirtyNine, hundredFortyNine, hundredFiftyOne, hundredFiftySeven};
    private static final SecureRandom RNG = new SecureRandom();
    private final boolean sign;
    //these constructors serve as various ways to build the number from structure of the inputs.
    public LargeInteger(long num){
        long numCopy = Math.abs(num);
        if(num == 0){
            number = new long[]{0L};
            sign = false;
            return;
        }
        number = new long[(int)(Math.log(numCopy)/Math.log(BASE))+1];
        for(int i = number.length-1; i >= 0; i--){
            number[i] = numCopy%BASE;
            numCopy = numCopy/BASE;
        }
        sign = num < 0;
    }
    private static long[] trimLeadingZeros(long[] arr) {
        int start = 0;
        while (start < arr.length - 1 && arr[start] == 0)
            start++;
        long[] result = new long[arr.length - start];
        for(int i = start; i < arr.length; i++) result[i-start] = Math.abs(arr[i]);
        return result;
    }
    LargeInteger(long[] digits, long sign){
        this.number = trimLeadingZeros(digits.clone());
        this.sign = sign < 0;
    }
    @Override
    public LargeInteger copy(){return new LargeInteger(number, Long.parseLong(sign().toString()));}
    //returns a String version that can be printed.
    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        if(sign) str.append("-");
        for(int i = 0; i < number.length; i++){
            if(i > 0){
                if(number[i] < 100000000L)
                    str.append(0);
                if(number[i] < 10000000L)
                    str.append(0);
                if(number[i] < 1000000L)
                    str.append(0);
                if(number[i] < 100000L)
                    str.append(0);
                if(number[i] < 10000L)
                    str.append(0);
                if(number[i] < 1000L)
                    str.append(0);
                if(number[i] < 100L)
                    str.append(0);
                if(number[i] < 10L)
                    str.append(0);
            }
            str.append(number[i]);
        }
        return str.toString();
    }
    //evaluates mathematical equality of two numbers.
    @Override
    public boolean equalsTo(Atom other){
        if(!(other instanceof ScalarAtom)) return false;
        return this.compareTo((ScalarAtom) other) == 0L;
    }
    //evaluates mathematical inequality of two numbers.
    @Override
    public boolean notEqualsTo(Atom other){
        if(!(other instanceof ScalarAtom)) return true;
        return this.compareTo((ScalarAtom) other) != 0L;
    }
    //returns the sign of an integer.
    public LargeInteger sign(){
        if(number.length == 1 && number[0] == 0L) return zero;
        return sign ? negativeOne : one;
    }
    public LargeInteger negate(){return new LargeInteger(this.number, -1*Long.parseLong(this.sign().toString()));}
    //returns a new integer with the same list as the old one.
    @Override
    public LargeInteger magnitude(){
        long[] result = new long[number.length];
        for(int i = 0; i < number.length; i++) result[i] = Math.abs(number[i]);
        return new LargeInteger(result, 1);
    }
    @Override
    public LargeInteger floor(){return this;}
    @Override
    public LargeInteger ceil(){return this;}
    //this serves as a way to optimize modulo for large integer powers.
    public LargeInteger powMod(LargeInteger exp, LargeInteger rem){
        if(exp.sign().equalsTo(negativeOne))
            throw new ArithmeticException("Error: PowMod not supported for fractions");
        LargeInteger result = one;
        LargeInteger base = (LargeInteger) this.copy().mod(rem);
        LargeInteger expTemp = exp.magnitude();
        while(expTemp.notEqualsTo(zero)){
            if(expTemp.isOdd()){
                result = (LargeInteger) result.multiply(base).mod(rem);
                if(expTemp.equalsTo(one)) break;
            }
            base = (LargeInteger) base.square().mod(rem);
            expTemp = expTemp.longDivision(two);
        }
        return result;
    }
    //generates a random integer within a given range.
    public static LargeInteger random(LargeInteger bottom, LargeInteger top){
        //generates a random number within a certain range, including both bounds in the range
        if (bottom.greaterThan(top)) throw new IllegalArgumentException("Error: Invalid range");
        LargeInteger range = (LargeInteger) top.subtract(bottom);
        long[] res = new long[range.number.length];
        for(int i = 0; i < range.number.length; i++){
            if(i == 0)
                res[i] = RNG.nextLong(range.number[i]);
            else res[i] = RNG.nextLong(BASE);
        }
        return (LargeInteger) new LargeInteger(res, 1).add(bottom);
    }
    //uses Miller-Rabin to determine if an integer is prime.
    public boolean isProbablyNotPrime(int rounds){
        if(this.lessThan(LargeInteger.two))
            return true;
        if(this.isEven()) return !this.equalsTo(two);
        for(int i = 1; i < smallPrimes.length; i++){
            if(this.mod(smallPrimes[i]).equalsTo(LargeInteger.zero))
                return !this.equalsTo(smallPrimes[i]);
        }
        LargeInteger nMinusOne = (LargeInteger) this.subtract(one);
        LargeInteger nMinusTwo = (LargeInteger) this.subtract(two);
        LargeInteger multiplier = nMinusOne.magnitude();
        int twoPower = 0;
        while(multiplier.isEven()){
            twoPower++;
            multiplier = multiplier.longDivision(two);
        }
        for(int i = 0; i < rounds; i++){
            LargeInteger witness = random(two, nMinusTwo);
            if(!isWitness(witness, multiplier, twoPower, nMinusOne))
                return true;
        }
        return false;
    }
    //private helper for isPrime
    private boolean isWitness(LargeInteger witness, LargeInteger multiplier, int twoPower, LargeInteger nMinusOne){
        LargeInteger x = witness.powMod(multiplier, this);
        if (x.equalsTo(one) || x.equalsTo(nMinusOne)) return true;
        for (int r = 1; r < twoPower; r++) {
            x = (LargeInteger) x.multiply(x).mod(this);
            if (x.equalsTo(nMinusOne)) return true;
            if (x.equalsTo(one)) return false;
        }
        return false;
    }
    //helper that finds a nontrivial factor of the input.
    private LargeInteger brentRhos(){
        if (this.isEven()) return two;
        LargeInteger y = random(one, (LargeInteger) this.subtract(one));
        LargeInteger c = random(one, (LargeInteger) this.subtract(one));
        LargeInteger m = new LargeInteger(128);
        LargeInteger g = one;
        LargeInteger r = one;
        LargeInteger q;
        LargeInteger x = zero;
        LargeInteger ys = zero;
        while (g.equalsTo(one)) {
            x = y.copy();
            for (LargeInteger j = zero; j.lessThan(r); j = (LargeInteger) j.add(one))
                y = (LargeInteger) y.square().add(c).mod(this);
            LargeInteger k = zero;
            while (k.lessThan(r) && g.equalsTo(one)) {
                ys = y.copy();
                q = one;
                LargeInteger limit = m;
                if (r.subtract(k).lessThan(m)) limit = (LargeInteger) r.subtract(k);
                for (LargeInteger j = zero; j.lessThan(limit); j = (LargeInteger) j.add(one)) {
                    y = (LargeInteger) y.square().add(c).mod(this);
                    q = (LargeInteger) q.multiply(x.subtract(y).magnitude()).mod(this);
                }
                g = q.gcd(this);
                k = (LargeInteger) k.add(m);
            }
            r = (LargeInteger) r.multiply(two);
        }
        if (g.equalsTo(this)) {
            do{
                ys = (LargeInteger) ys.square().add(c).mod(this);
                g = ((LargeInteger) x.subtract(ys)).magnitude().gcd(this);
            }while (g.equalsTo(one));
        }
        return g;
    }
    //factors a number using trial division for small primes and batched rhos for the rest. despite the implications, it is not strict prime factorization. why? well, 0 and 1 do not have prime factorizations. however, a total function is basically required in programming.
    public LargeList<PrimeExponent> factor(){
        LargeList<PrimeExponent> p = new LargeList<>();
        if(this.equalsTo(zero)){
            p.add(new PrimeExponent(zero, (LargeFraction) one.divide(one)));
            return p;
        }else if(this.equalsTo(one)){
            p.add(new PrimeExponent(one, (LargeFraction) one.divide(one)));
            return p;
        }else if(this.sign().equalsTo(negativeOne)) p.add(new PrimeExponent(negativeOne, (LargeFraction) one.divide(one)));
        LargeInteger copy = this.magnitude();
        for(LargeInteger s: smallPrimes){
            LargeInteger exp = zero;
            while(copy.mod(s).equalsTo(zero)){
                exp = (LargeInteger) exp.add(one);
                copy = copy.longDivision(s);
            }
            if(exp.notEqualsTo(zero)) p.add(new PrimeExponent(s, (LargeFraction) exp.divide(one)));
        }
        if(copy.equalsTo(one)) return p;
        //batched rhos
        while(copy.greaterThan(one) && copy.isProbablyNotPrime(60)){
            LargeInteger P = copy.brentRhos();
            if(P.isProbablyNotPrime(80)){
                LargeList<PrimeExponent> list = P.factor();
                for(PrimeExponent pr: list) p.add(new PrimeExponent(pr.base, pr.exponent));
            }else p.add(new PrimeExponent(P, LargeFraction.one));
            copy = copy.longDivision(P);
        }
        if(copy.greaterThan(one)) p.add(new PrimeExponent(copy, LargeFraction.one));
        PrimeExponent.sortBases(p);
        for(LargeInteger i = one; i.lessThan(p.size()); i = i.add(1)){
            if(p.get(i).base.equalsTo(p.get((LargeInteger) i.subtract(one)).base)){
                p.set(i.subtract(1), new PrimeExponent(p.get(i.subtract(1)).base, (LargeFraction) p.get(i.subtract(1)).exponent.add(p.get(i).exponent)));
                p.remove(i);
                i= (LargeInteger) i.subtract(one);
            }
        }
        return p;
    }
    public LargeList<LargeInteger> divisorsProper(){
        LargeList<LargeInteger> result = new LargeList<>();
        if(sign) result.add(negativeOne);
        LargeInteger bound = this.magnitude().longSqrt(twentyThree).floor();
        for (LargeInteger d = one; d.lesserThanOrEqualTo(bound); d = d.add(1))
            if (this.mod(d).equalsTo(zero))
                result.add(d);
        LargeInteger oldSize = result.size().copy();
        for(LargeInteger i = oldSize.subtract(1); i.greaterThan(zero); i = i.subtract(1)){
            if(this.longDivision(result.get(i)).notEqualsTo(result.get(i)))
                result.add(this.longDivision(result.get(i)));
        }
        return result;
    }
    public boolean isPerfect(){
        if(this.lesserThanOrEqualTo(one)) return false;
        LargeInteger result = zero;
        for(LargeInteger I: divisorsProper()) result = (LargeInteger) result.add(I);
        return result.equalsTo(this);
    }
    //the inverse of the factor method.
    public static LargeInteger buildFromFactors(LargeList<PrimeExponent> pr){
        LargeInteger result = one;
        for(PrimeExponent p: pr){
            if(p.exponent.denominator.notEqualsTo(one)) throw new ArithmeticException("Error: Result is not rational");
            if(p.exponent.sign().equalsTo(LargeInteger.negativeOne)) throw new ArithmeticException("Error: Result is not an integer");
            LargeInteger expTemp = p.exponent.numerator.magnitude();
            LargeInteger basTemp = p.base.copy();
            while(expTemp.notEqualsTo(zero)){
                if(expTemp.isOdd()){
                    result = (LargeInteger) result.multiply(basTemp);
                    if(expTemp.equalsTo(one)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(two);
            }
        }
        return result;
    }
    @Override
    public long compareMags(ScalarAtom other) {
        if(other instanceof LargeInteger){
            if(number.length > ((LargeInteger) other).number.length) return 1L;
            if(number.length < ((LargeInteger) other).number.length) return -1L;
            for(int i = 0; i < number.length; i++){
                if(number[i] > ((LargeInteger) other).number[i]) return 1L;
                if(number[i] < ((LargeInteger) other).number[i]) return -1L;
            }
            return 0L;
        }else if(other instanceof LargeFraction)
            return this.multiply(((LargeFraction) other).denominator).compareMags(((LargeFraction) other).numerator);
        else if(other instanceof LargePowerMonomial){
            if(other.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
            LargeFraction f = this.power(((LargePowerMonomial) other).exponent.denominator);
            LargeFraction o = ((LargePowerMonomial) other).power((LargeFraction) ((LargePowerMonomial) other).exponent.denominator.divide(one)).base;
            return f.compareMags(o);
        }else if(other instanceof RadicalTerm){
            if(other.isNotReal()){
                RadicalTermSum R = (RadicalTermSum) other.multiply(((RadicalTerm) other).conjugate());
                return this.square().compareMags(R);
            }
            if(this.equalsTo(((RadicalTerm) other).rationalPart) && ((RadicalTerm) other).coefficient.equalsTo(LargeInteger.zero)) return 0L;
            LargeFraction f = (LargeFraction) this.subtract(((RadicalTerm) other).rationalPart).divide(((RadicalTerm) other).coefficient);
            f = f.power(((RadicalTerm) other).exponent.exponent.denominator);
            LargeFraction o = ((RadicalTerm) other).exponent.base;
            return f.compareMags(o);
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    //compares two numbers. for integers and exponents, it uses special techniques.
    //integer comparison uses compareMags, which does most of the hard work.
    @Override
    public long compareTo(ScalarAtom l) {
        if(l instanceof LargeInteger){
            if(this.compareMags(l) > 0L){
                if(this.sign) return -1L;
                return 1L;
            }else if(this.compareMags(l) < 0L){
                if(((LargeInteger) l).sign) return 1L;
                return -1L;
            }
            if(this.sign && !((LargeInteger) l).sign) return -1L;
            else if(!this.sign && ((LargeInteger) l).sign) return 1L;
            return 0L;
        }else if(l instanceof LargeFraction)
            return this.multiply(((LargeFraction) l).denominator).compareTo(((LargeFraction) l).numerator);
        else if(l instanceof LargePowerMonomial){
            if(l.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
            LargeFraction f = this.power(((LargePowerMonomial) l).exponent.denominator);
            LargeFraction o = ((LargePowerMonomial) l).power((LargeFraction) ((LargePowerMonomial) l).exponent.denominator.divide(one)).base;
            long res = f.compareMags(o);
            if(res == 1){
                if(this.sign) return -1L;
                return 1L;
            }else if(res == -1L){
                if(l.sign().equalsTo(negativeOne)) return 1L;
                return -1L;
            }
            return res;
        }else if(l instanceof RadicalTerm){
            if(l.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
            long B = this.compareMags(l);
            if(B == 1L){
                if(this.sign) return -1L;
                return 1L;
            }else if(B == -1L){
                if(l.sign().equalsTo(negativeOne)) return 1L;
                return -1L;
            }
            if(this.sign && l.sign().equalsTo(one)) return -1L;
            else if(!this.sign && l.sign().equalsTo(negativeOne)) return 1L;
            return 0L;
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public boolean greaterThan(ScalarAtom other){return this.compareTo(other) > 0L;}
    public boolean lessThan(ScalarAtom other) {return this.compareTo(other) < 0L;}
    public boolean greaterThanOrEqualTo(ScalarAtom other){return this.compareTo(other) >= 0L;}
    public boolean lesserThanOrEqualTo(ScalarAtom other){return this.compareTo(other) <= 0L;}
    @Override
    public boolean isRational() {return true;}
    @Override
    public boolean isReal() {return true;}
    @Override
    public boolean isNotReal() {return false;}
    public LargeInteger gcd(LargeInteger other){
        if(other.equalsTo(LargeInteger.zero)) return this.magnitude();
        if(this.equalsTo(LargeInteger.zero)) return other.magnitude();
        LargeInteger a = this.magnitude();
        LargeInteger b = other.magnitude();
        while(b.notEqualsTo(LargeInteger.zero)){
            LargeInteger r = (LargeInteger) a.mod(b);
            a = b.copy();
            b = r.copy();
        }
        return a;
    }
    public LargeInteger gcd(LargeInteger... other){
        LargeInteger copy = this.magnitude();
        for(LargeInteger I: other)
            copy = copy.gcd(I);
        return copy;
    }
    public LargeInteger lcm(LargeInteger other){return ((LargeInteger)this.multiply(other)).longDivision(this.gcd(other));}
    public LargeInteger lcm(LargeInteger... other){
        LargeInteger copy = this.magnitude();
        for(LargeInteger I: other)
            copy = copy.lcm(I);
        return copy;
    }
    public LargeInteger triangular(){return ((LargeInteger) this.add(one)).nCr(two);}
    public LargeInteger tetrahedral(){return ((LargeInteger)this.add(two)).nCr(three);}
    public static LargeInteger figurate(LargeInteger l, LargeInteger k){return ((LargeInteger)k.add(l).subtract(one)).nCr(k);}
    private static long[] addMagnitudes(long[] a, long[] b) {
        int len = Math.max(a.length, b.length);
        long[] result = new long[len + 1];

        int ia = a.length - 1;
        int ib = b.length - 1;
        int ir = result.length - 1;

        long carry = 0;

        while (ir >= 0) {
            long x = ia >= 0 ? a[ia--] : 0;
            long y = ib >= 0 ? b[ib--] : 0;

            long sum = x + y + carry;
            result[ir--] = sum % BASE;
            carry = sum / BASE;
        }
        return trimLeadingZeros(result);
    }
    private static long[] subtractMagnitudes(long[] a, long[] b) {
        long[] result = new long[a.length];
        int ia = a.length - 1;
        int ib = b.length - 1;
        int ir = result.length - 1;
        long borrow = 0;
        while (ir >= 0) {
            long x = a[ia--];
            long y = ib >= 0 ? b[ib--] : 0;

            long diff = x - y + borrow;
            result[ir--] = Math.floorMod(diff, BASE);
            borrow = Math.floorDiv(diff, BASE);
        }
        return trimLeadingZeros(result);
    }
    //This method serves to add two numbers. More details in the method
    @Override
    public ScalarAtom add(ScalarAtom other) {
        //This one adds two integers via base-10^9 chunking. Like every other method, it assumes both are normalized to the best they can be. Runs in time complexity O(n), n being max number of chunks
        if (other instanceof LargeInteger o) {
            if (this.sign == o.sign) {
                long[] result = addMagnitudes(this.number, o.number);
                return new LargeInteger(result, this.sign ? -1 : 1);
            }
            long cmp = this.compareMags(o);
            if (cmp == 0) return LargeInteger.zero;
            if (cmp > 0) {
                long[] result = subtractMagnitudes(this.number, o.number);
                return new LargeInteger(result, this.sign ? -1 : 1);
            }
            long[] result = subtractMagnitudes(o.number, this.number);
            return new LargeInteger(result, o.sign ? -1 : 1);
        } else if(other instanceof LargeFraction) return this.divide(one).add(other);
        else if(other instanceof LargePowerMonomial) return new RadicalTerm(new LargeFraction(this, one), LargeFraction.one, (LargePowerMonomial) other);
        else if(other instanceof RadicalTerm) return new RadicalTerm((LargeFraction) this.add(((RadicalTerm) other).rationalPart), ((RadicalTerm) other).coefficient, ((RadicalTerm) other).exponent);
        else if(other instanceof RadicalTermSum)
            return new RadicalTermSum(((RadicalTermSum) other).sum, (LargeFraction) ((RadicalTermSum) other).constant.add(this));
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public ScalarAtom add(ScalarAtom... atoms){
        ScalarAtom result = this;
        for(ScalarAtom atom: atoms) result = result.add(atom);
        return result;
    }
    public LargeInteger add(long other){return (LargeInteger) this.add(new LargeInteger(other));}
    @Override
    public ScalarAtom subtract(ScalarAtom other){return this.add(other.negate());}
    public ScalarAtom subtract(ScalarAtom... atoms){
        ScalarAtom result = this;
        for(ScalarAtom atom: atoms) result = result.subtract(atom);
        return result;
    }
    public LargeInteger subtract(long other){return (LargeInteger) this.add(new LargeInteger(other).multiply(negativeOne));}
    private LargeInteger shift(int n){
        if(this.sign().equalsTo(zero)) return this;
        long[] num = new long[number.length + n];
        System.arraycopy(number, 0, num, 0, number.length);
        return new LargeInteger(num, sign ? -1 : 1);
    }
    private static long[] slice(long[] a, int from, int to) {
        int len = Math.max(0, to - from);
        if(len == 0) return new long[]{0};
        long[] out = new long[len];
        System.arraycopy(a, from, out, 0, len);
        return trimLeadingZeros(out);
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if(other instanceof LargeInteger I){
            if(this.number.length == 1 && I.number.length == 1){
                long re = this.number[0] * I.number[0];
                if(sign ^ I.sign) re = re*-1;
                return new LargeInteger(re);
            }
            int n = Math.max(this.number.length, I.number.length) / 2;
            long[] a = this.number;
            long[] b = I.number;
            int aSplit = Math.max(0, a.length - n);
            int bSplit = Math.max(0, b.length - n);
            LargeInteger high1 = new LargeInteger(slice(a, 0, aSplit), 1);
            LargeInteger low1  = new LargeInteger(slice(a, aSplit, a.length), 1);
            LargeInteger high2 = new LargeInteger(slice(b, 0, bSplit), 1);
            LargeInteger low2  = new LargeInteger(slice(b, bSplit, b.length), 1);
            LargeInteger z0 = (LargeInteger) low1.multiply(low2);
            LargeInteger z2 = (LargeInteger) high1.multiply(high2);
            LargeInteger z1 = (LargeInteger) high1.add(low1).multiply(high2.add(low2));
            LargeInteger middle = (LargeInteger) z1.subtract(z2).subtract(z0);
            LargeInteger result = (LargeInteger) z2.shift(2 * n).add(middle.shift(n)).add(z0);
            long signum = 1;
            if(sign ^ I.sign) signum = -1;
            return new LargeInteger(result.number, signum);
        }else if(other instanceof LargeFraction)
            return new LargeFraction((LargeInteger) this.multiply(((LargeFraction) other).numerator), ((LargeFraction) other).denominator);
        else if(other instanceof LargePowerMonomial)
            return new LargePowerMonomial(new LargeFraction(this, one), LargeFraction.one).multiply(other);
        else if(other instanceof RadicalTerm)
            return new RadicalTerm((LargeFraction) this.multiply(((RadicalTerm) other).rationalPart), (LargeFraction) this.multiply(((RadicalTerm) other).coefficient), ((RadicalTerm) other).exponent);
        else if(other instanceof RadicalTermSum R){
            LargeList<RadicalTerm> rt = new LargeList<>();
            for(LargePowerMonomial L: R.sum)
                rt.add(new RadicalTerm(LargeFraction.zero, (LargeFraction) this.multiply(R.sum.get(L)), L));
            return new RadicalTermSum(rt, (LargeFraction) this.multiply(R.constant));
        }
        return other.multiply(this);
    }
    public ScalarAtom multiply(ScalarAtom... atoms){
        ScalarAtom result = this;
        for(ScalarAtom atom: atoms) result = result.multiply(atom);
        return result;
    }
    public LargeInteger multiply(long greg){return (LargeInteger) this.multiply(new LargeInteger(greg));}
    @Override
    public LargeInteger square(){
        if (number.length < 10) {
            LargeInteger result = zero;
            for (int i = 0; i < this.number.length; i++)
                for (int j = i; j < this.number.length; j++) {
                    LargeInteger term = one;
                    term = term.shift(2 * this.number.length - i - j - 2);
                    term = term.multiply(this.number[i] * this.number[j]);
                    if (i != j) term = term.multiply(2);
                    result = (LargeInteger) result.add(term);
                }
            return result;
        }
        return (LargeInteger) this.multiply(this);
    }
    @Override
    public LargeInteger cube(){return (LargeInteger) this.square().multiply(this);}
    public static LargeInteger productRange(LargeInteger left, LargeInteger right) {
        if (left.equalsTo(right)) return left;
        if (right.subtract(left).equalsTo(one)) return (LargeInteger) left.multiply(right);
        LargeInteger mid = ((LargeInteger)left.add(right)).longDivision(two);
        return (LargeInteger) productRange(left, mid).multiply(productRange((LargeInteger) mid.add(one), right));
    }
    public LargeInteger factorial(){
        if(sign) throw new ArithmeticException("Error: Factorial undefined for negative integers");
        if(equalsTo(zero) || equalsTo(one)) return one;
        return productRange(one, this);
    }
    public LargeInteger longDivision(LargeInteger other){
        if(other.equalsTo(zero)) throw new ArithmeticException("Error: Cannot divide by 0");
        else if(other.equalsTo(one)) return this;
        else if(this.compareMags(other) < 0L) return LargeInteger.zero;
        else if(this.compareMags(other) == 0L) return (LargeInteger) this.sign().multiply(other.sign());
        //knuth-style division
        LargeInteger copy = this.magnitude();
        LargeInteger copy2 = other.magnitude();
        if(copy2.number.length == 1){
            long carry = 0;
            ArrayList<Long> quotient = new ArrayList<>();
            for (long chunk : copy.number) {
                long current = carry * BASE + chunk;
                long qChunk = current/copy2.number[0];
                carry = current % copy2.number[0];
                quotient.add(qChunk);
            }
            while(quotient.getFirst() == 0L && quotient.size() > 1) quotient.removeFirst();
            long[] res = new long[quotient.size()];
            for(int i = 0; i < res.length; i++) res[i] = quotient.get(i);
            long signum = 1;
            if(this.sign ^ other.sign) signum = -1;
            return new LargeInteger(res, signum);
        }
        long factor = BASE/(copy2.number[0]+1);
        copy = copy.multiply(factor);
        copy2 = copy2.multiply(factor);
        long[] remainder = new long[]{0};
        for(int i = 0; i < copy2.number.length; i++){
            long[] cr = new long[remainder.length + 1];
            System.arraycopy(remainder, 0, cr, 0, remainder.length);
            cr[cr.length-1] = copy.number[i];
            remainder = cr;
        }
        ArrayList<Long> quotient = new ArrayList<>();
        for(int i = 0; i <= copy.number.length - copy2.number.length; i++){
            //pad zeros
            while(remainder.length < copy2.number.length+1){
                long[] shift = new long[remainder.length+1];
                System.arraycopy(remainder, 0, shift, 1, shift.length - 1);
                shift[0] = 0;
                remainder = shift;
            }
            //estimate
            long numerator = remainder[0] * BASE + remainder[1];
            long qHat = numerator/copy2.number[0];
            long rHat = numerator%copy2.number[0];
            if (qHat >= BASE) {
                qHat = 999999999L;
                rHat = numerator - qHat * copy2.number[0];
            }
            while(qHat * copy2.number[1] > rHat * BASE + remainder[2]){
                qHat--;
                rHat += copy2.number[0];
                if (rHat >= BASE)
                    break;
            }
            LargeInteger rt = ((LargeInteger) new LargeInteger(remainder, 1).subtract(copy2.multiply(qHat)));
            if (rt.sign) {
                qHat--;
                rt = (LargeInteger) rt.add(copy2);
            }
            quotient.add(qHat);
            int nextIndex = i + copy2.number.length;
            if (nextIndex < copy.number.length) rt = rt.shift(1).add(copy.number[nextIndex]);
            remainder = rt.number;
        }
        while(quotient.getFirst() == 0L && quotient.size() > 1) quotient.removeFirst();
        long[] res = new long[quotient.size()];
        for(int i = 0; i < res.length; i++) res[i] = quotient.get(i);
        long signum = 1;
        if(this.sign ^ other.sign) signum = -1;
        return new LargeInteger(res, signum);
    }
    public LargeInteger floorDivision(LargeInteger other){
        LargeInteger result = this.longDivision(other);
        boolean shouldBeNegative = this.sign() != other.sign();
        if(other.multiply(result).notEqualsTo(this) && shouldBeNegative) result = (LargeInteger) result.subtract(one);
        return result;
    }
    @Override
    public ScalarAtom divide(ScalarAtom other) {
        if(other instanceof LargeInteger) return new LargeFraction(this, (LargeInteger) other);
        else if(other instanceof LargeFraction) return new LargeFraction((LargeInteger) ((LargeFraction) other).denominator.multiply(this), ((LargeFraction) other).numerator);
        else if(other instanceof LargePowerMonomial) return new RadicalTerm(LargeFraction.zero, new LargeFraction(this, one), (LargePowerMonomial) other.reciprocal());
        return other.reciprocal().multiply(this);
    }
    public LargeInteger totient(){
        LargeList<PrimeExponent> lk = this.factor();
        LargeFraction result = LargeFraction.one;
        for(PrimeExponent p: lk)
            result = (LargeFraction) result.multiply(LargeFraction.one.subtract(p.base.reciprocal()));
        result = (LargeFraction) result.multiply(this);
        return result.numerator;
    }
    @Override
    public LargeFraction reciprocal(){return (LargeFraction) one.divide(this);}
    public LargeInteger remainder(LargeInteger other){return (LargeInteger) this.subtract(this.longDivision(other).multiply(other));}
    @Override
    public ScalarAtom mod(ScalarAtom other){
        if(other instanceof LargeInteger I){
            if(other.equalsTo(zero)) throw new ArithmeticException("Error: Cannot divide by 0");
            if(I.number.length == 1){
                long rem = 0L;
                long absM = I.number[0];
                for (long chunk : number)
                    rem = (rem * BASE + chunk) % absM;
                if (this.sign && rem != 0L)
                    rem = absM - rem;
                if (I.sign && rem != 0L)
                    rem = rem - absM;
                return new LargeInteger(rem);
            }
            return this.subtract(this.floorDivision(I).multiply(other));
        }
        return this.subtract(this.divide(other).floor().multiply(other));
    }
    public LargeInteger mod2(){
        if(number[number.length-1] % 2L == 1L) return one;
        return zero;
    }
    public boolean isOdd(){return this.mod2().equalsTo(one);}
    public boolean isEven(){return this.mod2().equalsTo(zero);}
    @Override
    public LargeFraction percent(){return new LargeFraction(this, hundred);}
    @Override
    public LargeFraction permille(){return new LargeFraction(this, thousand);}
    public LargeInteger nCr(LargeInteger l) {
        if (this.lessThan(l)) return zero;
        else if(l.equalsTo(zero)) return one;
        return productRange((LargeInteger) this.subtract(l).add(one), this).longDivision(l.factorial());
    }
    public LargeFraction nPr(LargeInteger l){
        LargeInteger temp = (LargeInteger) this.subtract(l);
        if(temp.sign) return new LargeFraction(zero, one);
        return (LargeFraction) factorial().divide(temp.factorial());
    }
    public LargeFraction power(LargeInteger l){
        if(l.sign().equalsTo(zero) && this.sign().equalsTo(zero)) throw new ArithmeticException("Error: Indeterminate form: 0^0");
        LargeFraction result = LargeFraction.one;
        LargeInteger expTemp = l.magnitude();
        LargeInteger basTemp = this.copy();
        while(expTemp.notEqualsTo(LargeInteger.zero)){
            if(expTemp.isOdd()){
                result = (LargeFraction) result.multiply(basTemp);
                if(expTemp.equals(one)) break;
            }
            basTemp = basTemp.square();
            expTemp = expTemp.longDivision(two);
        }
        if(l.sign) return result.reciprocal();
        return result;
    }
    @Override
    public ScalarAtom power(LargeFraction f) {return new LargePowerMonomial(new LargeFraction(this, one), f);}
    public static LargeInteger pow10(int n) {
        if (n < 0) throw new ArithmeticException("Error: Negative exponent yields a non-integer");
        long[] chunks = new long[n/9 + 1];
        chunks[0] = 1;
        for(int i = 0; i < chunks.length; i++){
            if(i == 0){
                for(int j = 0; j < n%9; j++) chunks[i] *= 10;
            }else chunks[i] = 0;
        }
        return new LargeInteger(chunks, 1);
    }
    public LargeFraction longSqrt(LargeInteger digits) {
        if (this.sign)
            throw new ArithmeticException("Error: Newton's method does not work for negative arguments");
        if (this.equalsTo(zero))
            return LargeFraction.zero;
        LargeFraction N = new LargeFraction(this, one);
        LargeFraction x = N.copy();
        LargeFraction tolerance = ten.power(digits.negate());
        while (x.square().subtract(N).magnitude().greaterThan(x.multiply(LargeInteger.two).multiply(tolerance)))
            x = (LargeFraction) x.add(N.divide(x)).divide(LargeInteger.two);
        return x;
    }
}