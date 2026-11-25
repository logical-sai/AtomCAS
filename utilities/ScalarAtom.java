package com.AtomCAS.utilities;
/**
 * Represents a scalar mathematical value within the AtomCAS
 * computational system.
 *
 * <p>
 * This abstract class defines the common interface for scalar
 * objects, including integers, rational numbers, irrational
 * algebraic numbers, and other supported real or complex values.
 * It provides operations for arithmetic, exponentiation,
 * comparison, sign determination, and numerical classification.
 * </p>
 *
 * <h3>Mathematical Operations</h3>
 * <p>
 * ScalarAtom provides the following categories of operations:
 * </p>
 * <ul>
 *   <li>
 *     <b>Arithmetic:</b> Addition, subtraction, multiplication,
 *     division, reciprocals, and exponentiation.
 *   </li>
 *   <li>
 *     <b>Comparison:</b> Numerical ordering for real scalars
 *     and magnitude comparison independently of direction.
 *   </li>
 *   <li>
 *     <b>Rounding:</b> Floor and ceiling operations that
 *     return exact integers.
 *   </li>
 *   <li>
 *     <b>Classification:</b> Determination of whether a scalar
 *     is rational, real, non-real complex, or algebraic.
 *   </li>
 *   <li>
 *     <b>Complex-number operations:</b> Absolute value,
 *     signum, and extraction of real and imaginary components.
 *   </li>
 * </ul>
 *
 * <h3>Implementation</h3>
 * <p>
 * Subclasses implement the mathematical operations appropriate
 * to their representations. Common operations are defined here
 * in terms of more fundamental operations; for example,
 * subtraction is implemented as addition of the negated
 * operand, and division as multiplication by its reciprocal.
 * </p>
 *
 * <p>
 * Mathematical operations may return a different subclass of
 * ScalarAtom when the resulting value requires a different
 * representation. An operation need not preserve the concrete
 * type of its operands.
 * </p>
 *
 * <h3>Comparison Semantics</h3>
 * <p>
 * Numerical ordering is meaningful for real scalars. Non-real
 * complex scalars have no natural total ordering, so subclasses
 * must define how unsupported comparisons are handled.
 * </p>
 *
 * @see Atom
 */
public abstract class ScalarAtom extends Atom
        implements Comparable<ScalarAtom> {

    /**
     * Returns a copy of this scalar with the same mathematical value.
     *
     * @return a ScalarAtom representing the same value as this object
     * @apiNote It is usually unnecessary to call this method, since Atoms (except MiscellaneousAtoms) are immutable.
     */
    @Override
    public abstract ScalarAtom clone();

    /**
     * Returns the greatest integer less than or equal to this scalar.
     *
     * <p>
     * Mathematically, this is the floor function:
     * </p>
     * <pre>
     * floor(x) = max {n in Z : n &lt;= x}
     * </pre>
     * @return the exact floor of this scalar
     */
    public abstract LargeInteger floor();

    /**
     * Returns the smallest integer greater than or equal to this scalar.
     *
     * <p>
     * Mathematically, this is the ceiling function:
     * </p>
     * <pre>
     * ceil(x) = min {n in Z : n &gt;= x}
     * </pre>
     *
     * @return the exact ceiling of this scalar
     */
    public abstract LargeInteger ceil();

    /**
     * Compares the absolute values of two scalar atoms,
     * ignoring their signs or directions in the complex plane.
     *
     * <p>
     * This method compares {@code abs(this)} against
     * {@code abs(other)}, allowing magnitudes to be ordered
     * even when the original scalar values are non-real.
     * </p>
     *
     * @param other the scalar whose magnitude is compared
     *              against this scalar's magnitude
     * @return a negative integer, zero, or a positive integer
     *         if this scalar's magnitude is respectively
     *         smaller than, equal to, or greater than the
     *         magnitude of {@code other}
     */
    int compareMagnitudes(ScalarAtom other) {
        return abs().compareTo(other.abs());
    }

    /**
     * Compares this scalar numerically with another scalar.
     *
     * <p>
     * Real numbers are compared according to their natural
     * numerical ordering. Non-real complex numbers do not
     * possess a natural total ordering.
     * </p>
     *
     * @param other the scalar to compare against
     * @return a negative integer if this scalar is smaller,
     *         zero if equal, or a positive integer if greater
     */
    @Override
    public abstract int compareTo(ScalarAtom other);

    /**
     * Determines whether this scalar is strictly greater
     * than another scalar.
     *
     * @param other the scalar to compare against
     * @return true if this scalar is greater than {@code other}
     */
    public final boolean greaterThan(ScalarAtom other) {
        return compareTo(other) > 0;
    }

    /**
     * Determines whether this scalar is strictly less
     * than another scalar.
     *
     * @param other the scalar to compare against
     * @return true if this scalar is less than {@code other}
     */
    public final boolean lessThan(ScalarAtom other) {
        return compareTo(other) < 0;
    }

    /**
     * Determines whether this scalar is greater than or
     * equal to another scalar.
     *
     * @param other the scalar to compare against
     * @return true if this scalar is greater than or equal
     *         to {@code other}
     */
    public final boolean greaterThanOrEqualTo(ScalarAtom other) {
        return compareTo(other) >= 0;
    }

    /**
     * Determines whether this scalar is less than or
     * equal to another scalar.
     *
     * @param other the scalar to compare against
     * @return true if this scalar is less than or equal
     *         to {@code other}
     */
    public final boolean lesserThanOrEqualTo(ScalarAtom other) {
        return compareTo(other) <= 0;
    }

    /**
     * Returns the additive inverse of this scalar.
     *
     * @return a scalar representing {@code -this}
     */
    public abstract ScalarAtom negate();

    /**
     * Returns the signum or unit-magnitude direction
     * of this scalar.
     *
     * <p>
     * For a nonzero scalar z, the signum is defined as:
     * </p>
     * <pre>
     * sign(z) = z / |z|
     * </pre>
     *
     * <p>
     * For real numbers, the result is -1 or 1 depending
     * on the sign. For non-real complex numbers, the result
     * is a complex number of magnitude one with the same
     * direction as the original value.
     * </p>
     *
     * <p>
     * Zero is handled separately, returning zero rather
     * than attempting the undefined expression 0/0.
     * </p>
     *
     * @return the signum of this scalar, or zero if
     *         this scalar is zero
     */
    public ScalarAtom sign() {
        return isZero() ? LargeInteger.ZERO : divide(abs());
    }

    /**
     * Determines whether this scalar represents zero.
     *
     * @return true if this scalar is numerically equal to zero
     */
    public boolean isZero() {
        return equals(LargeInteger.ZERO);
    }

    /**
     * Determines whether this scalar represents one.
     *
     * @return true if this scalar is numerically equal to one
     */
    public boolean isOne() {
        return equals(LargeInteger.ONE);
    }

    /**
     * Determines whether this scalar is strictly negative.
     *
     * <p>
     * This operation relies on numerical ordering and is
     * therefore intended for real scalars.
     * </p>
     *
     * @return true if this scalar is less than zero
     */
    public boolean isNegative() {
        return lessThan(LargeInteger.ZERO);
    }

    /**
     * Returns the absolute value or complex modulus
     * of this scalar.
     *
     * <p>
     * For real numbers, this is the nonnegative absolute
     * value. For a complex number z = a + bi, its modulus is:
     * </p>
     * <pre>
     * |z| = sqrt(a^2 + b^2)
     * </pre>
     *
     * @return a nonnegative real scalar representing
     *         the magnitude of this scalar
     */
    public abstract ScalarAtom abs();

    /**
     * Determines whether this scalar is a rational number.
     *
     * @return true if this scalar can be expressed as
     *         a quotient of two integers with a nonzero
     *         denominator
     */
    public abstract boolean isRational();

    /**
     * Determines whether this scalar is a real number.
     *
     * @return true if this scalar belongs to the real numbers
     */
    public abstract boolean isReal();

    /**
     * Determines whether this scalar is non-real complex.
     *
     * <p>
     * In AtomCAS, this method uses "complex" to mean
     * specifically non-real, rather than the full mathematical
     * set of complex numbers, which includes real numbers.
     * </p>
     *
     * @return true if this scalar is not real
     */
    public final boolean isComplex() {
        return !isReal();
    }

    /**
     * Determines whether this scalar is algebraic.
     *
     * <p>
     * A complex number is algebraic if it is a root of
     * some nonzero polynomial with rational coefficients.
     * </p>
     *
     * @return true if this scalar is algebraic
     */
    public abstract boolean isAlgebraic();

    /**
     * Returns the real component of this scalar.
     *
     * <p>
     * For z = A + B*i, the real component is A.
     * </p>
     *
     * @return the real component of this scalar
     */
    public abstract ScalarAtom realPart();

    /**
     * Returns the imaginary component of this scalar.
     *
     * <p>
     * For z = a + bi, the imaginary component is b,
     * without the imaginary unit i.
     * </p>
     *
     * @return the imaginary coefficient of this scalar
     */
    public ScalarAtom imagPart() {
        return subtract(realPart()).divide(LargePowerMonomial.I);
    }

    /**
     * Adds another scalar to this scalar.
     *
     * @param other the scalar to add
     * @return the exact sum {@code this + other}
     */
    public abstract ScalarAtom add(ScalarAtom other);

    /**
     * Subtracts another scalar from this scalar.
     *
     * <p>
     * Implemented as addition of the additive inverse:
     * </p>
     * <pre>
     * a - b = a + (-b)
     * </pre>
     *
     * @param other the scalar to subtract
     * @return the difference {@code this - other}
     */
    public ScalarAtom subtract(ScalarAtom other) {
        return add(other.negate());
    }

    /**
     * Multiplies this scalar by another scalar.
     *
     * @param other the scalar multiplier
     * @return the product {@code this * other}
     */
    public abstract ScalarAtom multiply(ScalarAtom other);

    /**
     * Divides this scalar by another scalar.
     *
     * <p>
     * Implemented as multiplication by the reciprocal:
     * </p>
     * <pre>
     * a / b = a * (1/b)
     * </pre>
     *
     * @param other the nonzero scalar divisor
     * @return the quotient {@code this / other}
     */
    public ScalarAtom divide(ScalarAtom other) {
        return multiply(other.reciprocal());
    }

    /**
     * Returns the multiplicative inverse of this scalar.
     *
     * @return the scalar {@code 1 / this}
     */
    public abstract ScalarAtom reciprocal();

    /**
     * Returns the remainder of division by another scalar,
     * using floor-based modulo arithmetic.
     *
     * <p>
     * The result is calculated according to:
     * </p>
     * <pre>
     * a mod b = a - b * floor(a / b)
     * </pre>
     *
     * <p>
     * For real operands and a nonzero divisor, the remainder
     * has the same sign as the divisor, or is zero.
     * This definition also supports non-integral real operands
     * when the necessary arithmetic operations are implemented.
     * </p>
     *
     * @param other the nonzero scalar divisor
     * @return the floor-based modulo remainder
     */
    public ScalarAtom mod(ScalarAtom other) {
        return subtract(divide(other).floor().multiply(other));
    }

    /**
     * Returns the square of this scalar.
     *
     * @return {@code this} raised to the second power
     */
    public ScalarAtom square() {
        return multiply(this);
    }

    /**
     * Returns the cube of this scalar.
     *
     * @return {@code this} raised to the third power
     */
    public ScalarAtom cube() {
        return multiply(square());
    }

    /**
     * Converts this scalar to its percentage value by
     * multiplying it by 1/100.
     *
     * @return {@code this / 100}
     */
    public ScalarAtom percent() {
        return multiply(SI.centi);
    }

    /**
     * Converts this scalar to its per mille value by
     * multiplying it by 1/1000.
     *
     * @return {@code this / 1000}
     */
    public ScalarAtom permille() {
        return multiply(SI.milli);
    }

    /**
     * Raises this scalar to a rational exponent.
     *
     * <p>
     * The exponent is represented exactly as a
     * {@link LargeFraction}. The result may require a
     * different scalar representation, including an
     * algebraic or complex representation.
     * </p>
     *
     * @param f the rational exponent
     * @return the result of raising this scalar to
     *         the power {@code f}
     */
    public abstract ScalarAtom power(LargeFraction f);

    /**
     * Placeholder for decimal approximation of a scalar.
     *
     * <p>
     * Decimal approximation is not yet implemented by this
     * default method. It currently returns {@code null},
     * regardless of the requested precision.
     * </p>
     *
     * @param precision the requested approximation precision,
     *                  currently unused
     * @return {@code null} in the default implementation
     */
    public LargeDecimal approximateToDecimal(LargeInteger precision) {
        return this instanceof LargeDecimal D ? D : null;
    }
    /**
     * Returns a fractional polynomial that has a root being {@code this}.
     */
    public LargePolynomial annihilatingPolynomial() {
        throw new UnsupportedOperationException("Do not use the default implementation");
    }
}

