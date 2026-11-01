package com.AtomCAS.utilities;

/**
 * The abstract root class of all mathematical objects represented
 * within the AtomCAS computational system.
 *
 * <p>
 * An Atom represents a mathematical object with a defined value,
 * structure, or symbolic meaning. Concrete subclasses determine
 * which mathematical operations are supported and how their
 * objects are represented internally.
 * </p>
 *
 * <h3>Core Functionality</h3>
 * <p>
 * Every Atom provides the following fundamental operations:
 * </p>
 * <ul>
 *   <li>
 *     <b>Cloning:</b> Produces another Atom representing the
 *     same mathematical object.
 *   </li>
 *   <li>
 *     <b>String representation:</b> Converts the object into
 *     a human-readable mathematical expression.
 *   </li>
 *   <li>
 *     <b>LaTeX representation:</b> Produces a string suitable
 *     for mathematical typesetting.
 *   </li>
 *   <li>
 *     <b>Equality:</b> Determines whether two Atom objects
 *     represent equivalent mathematical values or structures,
 *     according to their concrete implementations.
 *   </li>
 *   <li>
 *     <b>Hashing:</b> Computes a hash code using the shared
 *     {@link CASHash} utility.
 *   </li>
 * </ul>
 *
 * <h3>Equality and Hashing</h3>
 * <p>
 * Equality is defined through {@link #equals(Atom)}, which
 * concrete subclasses must implement. The inherited
 * {@link #equals(Object)} method is finalized to ensure that
 * all comparisons through the standard Java Object interface
 * are delegated consistently to the Atom-specific method.
 * </p>
 *
 * <p>
 * Subclass implementations must preserve the Java equality
 * contract, including reflexivity, symmetry, transitivity,
 * and consistency. Numerically equivalent objects belonging
 * to different Atom subclasses should compare equally when
 * cross-type numerical equality is supported.
 * </p>
 *
 * <p>
 * Objects that compare equally must produce identical hash codes.
 * The shared hashing mechanism provided by {@link CASHash}
 * must therefore remain consistent with all subclass equality
 * implementations.
 * </p>
 *
 * <h3>Subclass Responsibilities</h3>
 * <p>
 * Concrete subclasses must implement cloning, string conversion,
 * LaTeX conversion, and Atom-specific equality. Additional
 * mathematical operations are defined by more specialized
 * subclasses, such as {@link ScalarAtom}.
 * </p>
 *
 * @see ScalarAtom
 * @see TensorAtom
 * @see CASHash
 */
public abstract class Atom implements Cloneable {

    /**
     * Returns a copy of this mathematical object.
     *
     * <p>
     * The returned Atom must represent the same mathematical
     * value or structure as the original. Concrete subclasses
     * determine how the copy is constructed and whether
     * internal objects are duplicated or safely shared.
     * </p>
     *
     * @return an Atom representing the same mathematical
     *         object as this instance
     * @apiNote It is usually unnecessary to call this method, since Atoms (except MiscellaneousAtoms) are immutable.
     */
    @Override
    public abstract Atom clone();

    /**
     * Returns a human-readable string representation
     * of this mathematical object.
     *
     * <p>
     * The representation is determined by the concrete
     * subclass and may include mathematical operators,
     * parentheses, coefficients, exponents, or other
     * notation appropriate to the object.
     * </p>
     *
     * @return a string representation of this Atom
     */
    @Override
    public abstract String toString();

    /**
     * Returns a LaTeX representation of this mathematical
     * object for use in mathematical typesetting.
     *
     * <p>
     * The returned string may contain LaTeX commands for
     * fractions, exponents, radicals, matrices, or other
     * mathematical structures depending on the subclass.
     * </p>
     *
     * @return the LaTeX representation of this Atom
     */
    public abstract String toLatex();

    /**
     * Determines whether another object is equal to this Atom.
     *
     * <p>
     * This method first checks whether the supplied object
     * is an instance of {@code Atom}. If it is, the comparison
     * is delegated to {@link #equals(Atom)}.
     * Otherwise, the method returns {@code false}.
     * </p>
     *
     * <p>
     * This method is final to prevent subclasses from
     * independently redefining standard Object equality.
     * Concrete subclasses must instead implement
     * {@code equals(Atom)}.
     * </p>
     *
     * @param other the object to compare with this Atom
     * @return {@code true} if {@code other} is an Atom
     *         considered equal to this instance;
     *         {@code false} otherwise
     */
    @Override
    public final boolean equals(Object other) {return other instanceof Atom A && equals(A);}

    /**
     * Determines whether this Atom is mathematically or
     * structurally equivalent to another Atom.
     *
     * <p>
     * The interpretation of equality depends on the concrete
     * subclass. Implementations may compare numerical values,
     * symbolic expressions, or internal mathematical structures.
     * </p>
     *
     * <p>
     * Implementations must remain consistent with
     * {@link #hashCode()} and satisfy the Java equality
     * contract, including symmetry when comparisons involve
     * different Atom subclasses.
     * </p>
     *
     * @param other the Atom to compare against
     * @return {@code true} if the two Atoms are considered
     *         equivalent; {@code false} otherwise
     */
    public abstract boolean equals(Atom other);

    /**
     * Determines whether another object is unequal to this Atom.
     *
     * <p>
     * This method returns the logical negation of
     * {@link #equals(Object)}.
     * </p>
     *
     * @param other the object to compare against
     * @return {@code true} if the objects are not equal;
     *         {@code false} otherwise
     */
    public final boolean doesntEqual(Object other) {return !equals(other);}

    /**
     * Computes the hash code of this mathematical object.
     *
     * <p>
     * Hash generation is delegated to {@link CASHash},
     * providing a common hashing mechanism for Atom objects.
     * </p>
     *
     * <p>
     * The hashing implementation must respect mathematical
     * equality: if two Atoms compare equally, their hash codes
     * must also be equal, even when the Atoms belong to
     * different concrete subclasses.
     * </p>
     *
     * @return the hash code of this Atom
     */
    @Override
    public int hashCode() {return CASHash.hash(this);}
}
