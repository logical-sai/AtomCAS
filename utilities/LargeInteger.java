package com.AtomCAS.utilities;
import java.security.SecureRandom;
import java.util.ArrayList;
public class LargeInteger extends ScalarAtom {
    //instance variables
    private final long[] number;
    private final boolean sign;
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
    private static final SecureRandom RNG = new SecureRandom();
    private static final LargeInteger TRIAL_LIMIT_PRIME_TEST = new LargeInteger(100_000);
    private static final LargeInteger TRIAL_LIMIT_FACTOR = new LargeInteger(1_000_000);
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
    public LargeInteger(String s){
        ScalarAtom I = Parser.parse(s);
        if(!(I instanceof LargeInteger)) throw new IllegalArgumentException("Error: Result not an integer");
        number = ((LargeInteger) I).number;
        sign = ((LargeInteger) I).sign;
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
        ArrayList<Long> D = new ArrayList<>();
        long part = 0;
        for(int i = digits.length-1; i >= 0; i--){
            part += Math.abs(digits[i]);
            D.addFirst(part % BASE);
            part = part / BASE;
        }
        while(part > 0){
            D.addFirst(part % BASE);
            part = part / BASE;
        }
        while(D.size() > 1 && D.getFirst() == 0)
            D.removeFirst();
        long[] chars = new long[D.size()];
        for(int i = 0; i < chars.length; i++) chars[i] = D.get(i);
        this.number = chars;
        if(number.length == 1 && number[0] == 0) this.sign = false;
        else this.sign = sign < 0;
    }
    @Override
    public LargeInteger copy(){return new LargeInteger(number, sign ? -1 : 1);}
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
    public LargeInteger negate(){return new LargeInteger(this.number, sign ? 1 : -1);}
    //returns a new integer with the same list as the old one.
    @Override
    public LargeInteger magnitude(){return new LargeInteger(this.number, 1L);}
    @Override
    public LargeInteger floor(){return this;}
    @Override
    public LargeInteger ceil(){return this;}
    //this serves as a way to optimize modulo for large integer powers.
    public LargeInteger powMod(LargeInteger exp, LargeInteger rem){
        if(exp.sign)
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
    public static LargeInteger randomChunks(int chunks){
        long[] res = new long[chunks];
        for(int i = 0; i < chunks; i++)
            res[i] = RNG.nextLong(BASE);
        return new LargeInteger(res, RNG.nextLong(2));
    }
    //uses Miller-Rabin to determine if an integer is prime.
    public boolean isProbablyNotPrime(){
        if(this.lessThan(LargeInteger.two))
            return true;
        LargeInteger copy = this.magnitude();
        if(copy.isEven())
            return copy.notEqualsTo(two);
        if(copy.mod(three).equalsTo(zero))
            return copy.notEqualsTo(three);
        for(LargeInteger i = six; i.lessThan(TRIAL_LIMIT_PRIME_TEST); i=i.add(6)){
            if(copy.mod(i.subtract(1)).equalsTo(zero))
                return copy.notEqualsTo(i.subtract(1));
            if(copy.mod(i.add(1)).equalsTo(zero))
                return copy.notEqualsTo(i.add(1));
        }
        LargeInteger nMinusOne = (LargeInteger) this.subtract(one);
        LargeInteger nMinusTwo = (LargeInteger) this.subtract(two);
        LargeInteger multiplier = nMinusOne.magnitude();
        long twoPower = 0;
        while(multiplier.isEven()){
            twoPower++;
            multiplier = multiplier.longDivision(two);
        }
        for(int i = 0; i < 120; i++){
            LargeInteger witness = random(two, nMinusTwo);
            if(!isWitness(witness, multiplier, twoPower, nMinusOne))
                return true;
        }
        return false;
    }
    //private helper for isPrime
    private boolean isWitness(LargeInteger witness, LargeInteger multiplier, long twoPower, LargeInteger nMinusOne){
        LargeInteger x = witness.powMod(multiplier, this);
        if (x.equalsTo(one) || x.equalsTo(nMinusOne)) return true;
        for (long r = 1; r < twoPower; r++) {
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
        long start = System.nanoTime();
        while(System.nanoTime()-start < 60_000_000_000L && g.equalsTo(one)){
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
    /**
     * Factors a number into multiplicative components. Usually this means prime factors. USUALLY.
     * In strict mathematics, 1 has an empty factorization, 0 has no prime factorization, and negatives are handled by a separate sign.
     * However, programming prefers total functions over philosophical purity.
     * To account for this, 1 returns {1 -> 1}, 0 returns {0 -> 1}, and -1 is treated as a sign-factor.
     * Also, if the number decides to become a cryptography final boss, the remaining chunk may be treated as one atomic factor.
     */
    public LargeMap<LargeInteger, LargeFraction> factor(){
        LargeMap<LargeInteger, LargeFraction> p = new LargeMap<>();
        if(this.equalsTo(zero)){
            p.put(zero, LargeFraction.one);
            return p;
        }else if(this.equalsTo(one)){
            p.put(one, LargeFraction.one);
            return p;
        }else if(this.sign)
            p.put(negativeOne, LargeFraction.one);
        LargeInteger copy = this.magnitude();
        if(copy.mod(two).equalsTo(zero)){
            LargeFraction exp = LargeFraction.zero;
            while(copy.mod(two).equalsTo(zero)){
                exp = (LargeFraction) exp.add(one);
                copy = copy.longDivision(two);
            }
            p.put(two, exp);
        }
        if(copy.mod(three).equalsTo(zero)){
            LargeFraction exp = LargeFraction.zero;
            while(copy.mod(three).equalsTo(zero)){
                exp = (LargeFraction) exp.add(one);
                copy = copy.longDivision(three);
            }
            p.put(three, exp);
        }
        for(LargeInteger i = six; i.lessThan(TRIAL_LIMIT_FACTOR); i=i.add(6)){
            if(copy.mod(i.subtract(1)).equalsTo(zero)){
                LargeFraction exp = LargeFraction.zero;
                while(copy.mod(i.subtract(1)).equalsTo(zero)){
                    exp = (LargeFraction) exp.add(one);
                    copy = copy.longDivision(i.subtract(1));
                }
                p.put(i.subtract(1), exp);
            }
            if(copy.mod(i.add(1)).equalsTo(zero)){
                LargeFraction exp = LargeFraction.zero;
                while(copy.mod(i.add(1)).equalsTo(zero)){
                    exp = (LargeFraction) exp.add(one);
                    copy = copy.longDivision(i.add(1));
                }
                p.put(i.add(1), exp);
            }
            if(copy.equalsTo(one)) return p;
        }
        //batched rhos
        while(copy.greaterThan(one) && copy.isProbablyNotPrime()){
            LargeInteger P = copy.brentRhos();
            if(P.equalsTo(one)){
                System.out.println("Number " + copy + " too difficult to factor; treated as atomic");
                p.put(copy, LargeFraction.one);
                return p;
            }else if(P.isProbablyNotPrime()){
                LargeMap<LargeInteger, LargeFraction> list = P.factor();
                for(LargeInteger I: list){
                    if(p.containsKey(I))
                        p.put(I, (LargeFraction) p.get(I).add(list.get(I)));
                    else p.put(I, list.get(I));
                }
            }else{
                if(p.containsKey(P))
                    p.put(P, (LargeFraction) p.get(P).add(one));
                else p.put(P, LargeFraction.one);
            }
            copy = copy.longDivision(P);
        }
        if(copy.greaterThan(one)){
            if(p.containsKey(copy))
                p.put(copy, (LargeFraction) p.get(copy).add(one));
            else p.put(copy, LargeFraction.one);
        }
        for(LargeInteger i = zero; i.lessThan(p.size()); i=i.add(1)){
            for(LargeInteger j = i.add(1); j.lessThan(p.size()); j=j.add(1))
                if(p.keyAtIndex(i).greaterThan(p.keyAtIndex(j)))
                    p.swap(p.keyAtIndex(i), p.keyAtIndex(j));
        }
        return p;
    }
    public LargeList<LargeInteger> divisorsProper() {
        LargeList<LargeInteger> result = new LargeList<>();
        if (this.equalsTo(zero) || this.equalsTo(one))
            return result;
        LargeMap<LargeInteger, LargeFraction> f = this.magnitude().factor();
        result.add(LargeInteger.one);
        for (LargeInteger p : f) {
            if (p.equalsTo(LargeInteger.negativeOne)) continue;
            LargeInteger exp = f.get(p).numerator;
            LargeInteger currentSize = result.size();
            LargeInteger pPower = LargeInteger.one;
            for (LargeInteger e = LargeInteger.one; e.lesserThanOrEqualTo(exp); e = e.add(1)) {
                pPower = (LargeInteger) pPower.multiply(p);
                for (LargeInteger i = LargeInteger.zero; i.lessThan(currentSize); i = i.add(1))
                    result.add((LargeInteger) result.get(i).multiply(pPower));
            }
        }
        result.removeLast();
        return result;
    }
    public boolean isPerfect(){
        if(this.lesserThanOrEqualTo(one)) return false;
        LargeInteger result = zero;
        for(LargeInteger I: divisorsProper()) result = (LargeInteger) result.add(I);
        return result.equalsTo(this);
    }
    //the inverse of the factor method.
    public static LargeInteger buildFromFactors(LargeMap<LargeInteger, LargeFraction> pr){
        LargeInteger result = one;
        for(LargeInteger p: pr){
            if(pr.get(p).denominator.notEqualsTo(one)) throw new ArithmeticException("Error: Result is not rational");
            if(pr.get(p).lessThan(zero)) throw new ArithmeticException("Error: Result is not an integer");
            LargeInteger expTemp = pr.get(p).numerator.magnitude();
            LargeInteger basTemp = p.copy();
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
    private static long compareArrs(long[] arr1, long[] arr2){
        if(arr1.length > arr2.length) return 1;
        if(arr1.length < arr2.length) return -1;
        for(int i = 0; i < arr1.length; i++){
            if(arr1[i] > arr2[i]) return 1;
            if(arr1[i] < arr2[i]) return -1;
        }
        return 0;
    }
    @Override
    public long compareMags(ScalarAtom other) {
        if(other instanceof LargeInteger I)
            return compareArrs(this.number, I.number);
        else if(other instanceof LargeFraction)
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
                if(l.lessThan(zero)) return 1L;
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
                if(l.lessThan(zero)) return 1L;
                return -1L;
            }
            if(this.sign && l.greaterThan(zero)) return -1L;
            else if(!this.sign && l.lessThan(zero)) return 1L;
            return 0L;
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public boolean isRational() {return true;}
    @Override
    public boolean isReal() {return true;}
    @Override
    public boolean isNotReal() {return false;}
    @Override
    public boolean isAlgebraic() {return true;}
    public LargeInteger gcd(LargeInteger other){
        LargeInteger a = this.magnitude();
        LargeInteger b = other.magnitude();
        while(b.notEqualsTo(zero)){
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
    private static long[] subtractMagnitudes(long[] a, long[] b){
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
    @Override
    public ScalarAtom add(ScalarAtom other) {
        //This one adds two integers via base-10^9 chunking. Like every other method, it assumes both are normalized to the best they can be. Runs in time complexity O(n), n being max number of chunks
        if (other instanceof LargeInteger o) {
            if (this.sign == o.sign)
                return new LargeInteger(addMagnitudes(this.number, o.number), this.sign ? -1 : 1);
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
    public LargeInteger subtract(long other){return this.add(other*-1);}
    private static long[] shift(long[] N, int n){
        long[] num = new long[N.length + n];
        System.arraycopy(N, 0, num, 0, N.length);
        return num;
    }
    private static long[] slice(long[] a, int from, int to) {
        int len = Math.max(0, to - from);
        if(len == 0) return new long[]{0};
        else if(to > a.length){
            if(from > a.length)
                return new long[]{0};
            long[] out = new long[a.length - from];
            if (a.length - from >= 0)
                System.arraycopy(a, from, out, 0, a.length - from);
            return out;
        }
        long[] out = new long[len];
        if (to - from >= 0) System.arraycopy(a, from, out, 0, to - from);
        return trimLeadingZeros(out);
    }
    private static long[] part0Low(long[] x, int n) {
        int end = x.length;
        int start = Math.max(0, end - n);
        return slice(x, start, end);
    }

    private static long[] part1Mid(long[] x, int n) {
        int end = Math.max(0, x.length - n);
        int start = Math.max(0, end - n);
        if (start >= end) return new long[]{0};
        return slice(x, start, end);
    }

    private static long[] part2High(long[] x, int n) {
        int end = Math.max(0, x.length - 2 * n);
        if (end == 0) return new long[]{0};
        return slice(x, 0, end);
    }
    private static long[] multiplyMagnitudes(long[] a, long[] b, int m, int n) {
        if(a.length == 1 && b.length == 1){
            //direct long multiply
            return new long[]{a[0]*b[0]};
        }else if(a.length <= m && b.length <= m){
            //grade-school multiply
            long[] result = new long[a.length + b.length];
            for (int i = a.length - 1; i >= 0; i--) {
                long carry = 0;
                for (int j = b.length - 1; j >= 0; j--) {
                    int pos = i + j + 1;
                    long cur = result[pos] + a[i] * b[j] + carry;
                    result[pos] = cur % BASE;
                    carry = cur / BASE;
                }
                int pos = i;
                while (carry > 0) {
                    long cur = result[pos] + carry;
                    result[pos] = cur % BASE;
                    carry = cur / BASE;
                    pos--;
                }
            }
            return result;
        }else if(a.length <= n && b.length <= n){
            //Karatsuba multiply
            int R = Math.max(a.length, b.length) / 2;
            int aSplit = Math.max(0, a.length - R);
            int bSplit = Math.max(0, b.length - R);
            long[] high1 = slice(a, 0, aSplit);
            long[] low1 = slice(a, aSplit, a.length);
            long[] high2 = slice(b, 0, bSplit);
            long[] low2 = slice(b, bSplit, b.length);
            long[] z0 = multiplyMagnitudes(low1, low2, m, n);
            long[] z2 = multiplyMagnitudes(high1, high2, m, n);
            long[] z1 = multiplyMagnitudes(addMagnitudes(high1, low1), addMagnitudes(high2, low2), m, n);
            long[] middle = subtractMagnitudes(z1, addMagnitudes(z2, z0));
            return addMagnitudes(addMagnitudes(shift(z2, 2 * R), shift(middle, R)), z0);
        }
        //Toom-Cook 3 multiply
        int len = Math.max(a.length, b.length);
        int R = (len + 2) / 3;
        //represent this as a2*B^2 + a1*B + a0
        LargeInteger a0 = new LargeInteger(part0Low(a, R), 1);
        LargeInteger a1 = new LargeInteger(part1Mid(a, R), 1);
        LargeInteger a2 = new LargeInteger(part2High(a, R), 1);
        //represent other as b2*B^2 + b1*B + b0
        LargeInteger b0 = new LargeInteger(part0Low(b, R), 1);
        LargeInteger b1 = new LargeInteger(part1Mid(b, R), 1);
        LargeInteger b2 = new LargeInteger(part2High(b, R), 1);

        LargeInteger p0 = a0.copy();
        LargeInteger p1 = (LargeInteger) a0.add(a1).add(a2);
        LargeInteger pm1 = (LargeInteger) a0.subtract(a1).add(a2);
        LargeInteger p2 = (LargeInteger) a0.add(a1.multiply(2)).add(a2.multiply(4));
        LargeInteger pinf = a2.copy();

        LargeInteger q0 = b0.copy();
        LargeInteger q1 = (LargeInteger) b0.add(b1).add(b2);
        LargeInteger qm1 = (LargeInteger) b0.subtract(b1).add(b2);
        LargeInteger q2 = (LargeInteger) b0.add(b1.multiply(2)).add(b2.multiply(4));
        LargeInteger qinf = b2.copy();

        LargeInteger v0 = (LargeInteger) p0.multiply(q0);
        LargeInteger v1 = (LargeInteger) p1.multiply(q1);
        LargeInteger vm1 = (LargeInteger) pm1.multiply(qm1);
        LargeInteger v2 = (LargeInteger) p2.multiply(q2);
        LargeInteger vinf = (LargeInteger) pinf.multiply(qinf);
        LargeInteger c0 = v0.copy();
        LargeInteger c4 = vinf.copy();

        LargeInteger c3 = ((LargeInteger)v2.subtract(v1)).longDivision(new LargeInteger(3));
        LargeInteger c1 = ((LargeInteger) v1.subtract(vm1)).longDivision(new LargeInteger(2));
        LargeInteger c2 = (LargeInteger) vm1.subtract(v0);

        c3 = (LargeInteger) ((LargeInteger) c2.subtract(c3)).longDivision(new LargeInteger(2)).add(c4.multiply(2));
        c2 = (LargeInteger) c2.add(c1).subtract(c4);
        c1 = (LargeInteger) c1.subtract(c3);
        LargeInteger result = (LargeInteger) new LargeInteger(shift(c4.number, 4 * R), 1).add(new LargeInteger(shift(c3.number, 3 * R), 1)).add(new LargeInteger(shift(c2.number, 2 * R), 1)).add(new LargeInteger(shift(c1.number, R), 1)).add(c0);
        return result.number;
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if(other instanceof LargeInteger I)
            return new LargeInteger(multiplyMagnitudes(number, I.number, 768, 8192), this.sign ^ I.sign ? -1 : 1);
        else if(other instanceof LargeFraction)
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
        }else if(other instanceof LargePowerNode L)
            return new LargePowerNode(L.base.multiply(this.power(L.exponent.denominator)), L.exponent, L.signFactor);
        return other.multiply(this);
    }
    public ScalarAtom multiply(ScalarAtom... atoms){
        ScalarAtom result = this;
        for(ScalarAtom atom: atoms) result = result.multiply(atom);
        return result;
    }
    public LargeInteger multiply(long greg){return (LargeInteger) this.multiply(new LargeInteger(greg));}
    private static void normalizeFrom(long[] result, int index) {
        for (int k = index; k > 0 && result[k] >= BASE; k--) {
            result[k - 1] += result[k] / BASE;
            result[k] %= BASE;
        }
    }
    public static long[] squareArray(long[] a, int m, int n) {
        if(a.length <= m){
            int R = a.length;
            long[] result = new long[2 * R];
            // Off-diagonal terms: 2*a[i]*a[j]
            for (int i = R - 1; i >= 0; i--) {
                long carry = 0;
                for (int j = i - 1; j >= 0; j--) {
                    int pos = i + j + 1;
                    long product = 2L * a[i] * a[j] + result[pos] + carry;
                    result[pos] = product % BASE;
                    carry = product / BASE;
                }
                result[i] += carry;
                normalizeFrom(result, i);
            }
            // Diagonal terms: a[i]^2
            for (int i = R - 1; i >= 0; i--) {
                int pos = 2 * i + 1;
                long product = a[i] * a[i] + result[pos];
                result[pos] = product % BASE;
                result[pos - 1] += product / BASE;
                normalizeFrom(result, pos - 1);
            }
            return trimLeadingZeros(result);
        }else if(a.length <= n){
            int R = a.length / 2;
            long[] high = slice(a, 0, a.length - R);
            long[] low  = slice(a, a.length - R, a.length);
            long[] z2 = squareArray(high, m, n);
            long[] z0 = squareArray(low, m, n);
            long[] sum = addMagnitudes(high, low);
            long[] z1 = subtractMagnitudes(subtractMagnitudes(squareArray(sum, m, n), z2), z0);
            return trimLeadingZeros(addMagnitudes(addMagnitudes(shift(z2, 2 * R), shift(z1, R)), z0));
        }
        return multiplyMagnitudes(a, a, m, n);
    }
    @Override
    public LargeInteger square(){
        return new LargeInteger(squareArray(number, 256, 16000), 1);
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
        else if(this.compareMags(other) == 0L) return new LargeInteger(this.sign ^ other.sign ? -1 : 1);
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
            if (nextIndex < copy.number.length) rt = new LargeInteger(shift(rt.number, 1), rt.sign ? -1: 1).add(copy.number[nextIndex]);
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
        boolean shouldBeNegative = this.sign != other.sign;
        if(other.multiply(result).notEqualsTo(this) && shouldBeNegative) result = (LargeInteger) result.subtract(one);
        return result;
    }
    private static long[] divSmall(long[] a, long d) {
        long[] result = new long[a.length];
        long rem = 0;
        for (int i = 0; i < a.length; i++) {
            long cur = rem * BASE + a[i];
            result[i] = cur / d;
            rem = cur % d;
        }
        if (rem != 0)
            throw new ArithmeticException("Non-exact small division");
        return trimLeadingZeros(result);
    }
    public LargeInteger totient(){
        LargeMap<LargeInteger, LargeFraction> lk = this.factor();
        LargeFraction result = LargeFraction.one;
        for(LargeInteger I: lk)
            result = (LargeFraction) result.multiply(LargeFraction.one.subtract(I.reciprocal()));
        result = (LargeFraction) result.multiply(this);
        return result.numerator;
    }
    @Override
    public LargeFraction reciprocal(){return new LargeFraction(one, this);}
    public LargeInteger remainder(LargeInteger other){return (LargeInteger) this.subtract(this.longDivision(other).multiply(other));}
    public ScalarAtom mod(ScalarAtom other){
        if(other instanceof LargeInteger I)
            return this.subtract(this.floorDivision(I).multiply(I));
        return this.subtract(this.divide(other).floor().multiply(other));
    }
    public LargeInteger mod2(){
        if(number[number.length-1] % 2L == 1L) return one;
        return zero;
    }
    public boolean isOdd(){return this.mod2().equalsTo(one);}
    public boolean isEven(){return this.mod2().equalsTo(zero);}
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
        if(l.equalsTo(zero) && this.equalsTo(zero)) throw new ArithmeticException("Error: Indeterminate form: 0^0");
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
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {
        return null;
    }

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