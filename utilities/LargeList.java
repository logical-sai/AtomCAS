package com.AtomCAS.utilities;
import java.util.*;
public class LargeList<T> implements Iterable<T>, Cloneable {
    private static final int CHUNK_SIZE = 1000;

    private LargeInteger size = LargeInteger.ZERO;
    private int sizeInt = 0; // -1 means size overflows int

    private int height = 0; // 0 means root is a leaf Object[1000]
    private Object[] root = new Object[CHUNK_SIZE];

    public LargeList() {}
    public LargeList(int initialSize) {this(LargeInteger.valueOf(initialSize));}
    public LargeList(LargeInteger initialSize) {
        if (initialSize == null || initialSize.isNegative())
            throw new IllegalArgumentException("Initial size cannot be null or negative");
        // Grow the tree to accommodate the final index.
        if (initialSize.greaterThan(LargeInteger.ZERO))
            ensureCapacity(initialSize.subtract(1));
        size = initialSize;
        sizeInt = initialSize.greaterThan(LargeInteger.valueOf(Integer.MAX_VALUE)) ? -1 : initialSize.intValue();
    }
    private static Object[] cloneTree(Object[] src, int height) {
        Object[] dst = src.clone();
        if (height > 0) {
            for (int i = 0; i < dst.length; i++) {
                if (dst[i] != null)
                    dst[i] = cloneTree((Object[]) dst[i], height - 1);
            }
        }

        return dst;
    }
    private LargeList(int sizeInt, int height, LargeInteger size, Object[] root) {
        this.sizeInt = sizeInt;
        this.height = height;
        this.size = size;
        this.root = root;
    }
    public LargeList<T> clone() {
        try {
            @SuppressWarnings("unchecked")
            LargeList<T> clone = (LargeList<T>) super.clone();
            clone.sizeInt = this.sizeInt;
            clone.height = this.height;
            clone.size = this.size.clone();
            clone.root = cloneTree(this.root, this.height);
            return clone;
        } catch (CloneNotSupportedException e) {
            //Generally should not happen
            throw new AssertionError(e);
        }
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(sizeInt == 0) return "{}";
        sb.append("{");
        if(sizeInt > 100 || sizeInt < 0){
            for(int i = 0; i < 100; i++){
                sb.append(get(i));
                if(i < 99) sb.append(", ");
            }
            sb.append(" +").append(size.subtract(100)).append(" more");
            return sb.append("}").toString();
        }
        for(int i = 0; i < sizeInt; i++){
            sb.append(get(i));
            if(i < sizeInt-1) sb.append(", ");
        }
        return sb.append("}").toString();
    }
    @Override
    public int hashCode() {
        int h = 1;
        for (T x : this)
            h = CASHash.mix(h, x == null ? 0 : x.hashCode());
        return CASHash.finish(h);
    }
    public void add(T value) {
        if (intSized())
            setRaw(sizeInt, value);
        else setRaw(size, value);
        size = size.add(1);
        if(intSized()) {
            if(++sizeInt == Integer.MIN_VALUE)
                sizeInt = -1;
        }
    }
    public void add(int index, T value) {
        add(null);
        if (intSized()) {
            for (int i = sizeInt - 1; i > index; i--)
                set(i, get(i - 1));
            set(0, value);
            return;
        }
        for (LargeInteger i = size.subtract(1); i.greaterThan(LargeInteger.valueOf(index)); i = i.subtract(1))
            setRaw(i, get(i.subtract(1)));
        setRaw(LargeInteger.ZERO, value);
    }
    public void addFirst(T value) {
        add(null);
        if (intSized()) {
            for (int i = sizeInt - 1; i > 0; i--)
                set(i, get(i - 1));
            set(0, value);
            return;
        }
        for (LargeInteger i = size.subtract(1); i.greaterThan(LargeInteger.ZERO); i = i.subtract(1))
            set(i, get(i.subtract(1)));
        set(LargeInteger.ZERO, value);
    }
    public void set(LargeInteger index, T value){
        if (index.isNegative() || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + size + ")");
        setRaw(index, value);
    }
    public void set(int index, T value){
        if(index < 0 || index >= sizeInt)
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + size + ")");
        setRaw(index, value);
    }
    private void setRaw(LargeInteger index, T value){
        ensureCapacity(index);
        int[] digits = base1000Digits(index);
        Object[] node = root;
        for (int level = 0; level < height; level++) {
            int slot = digits[level];
            if (node[slot] == null)
                node[slot] = new Object[CHUNK_SIZE];
            node = (Object[]) node[slot];
        }
        node[digits[height]] = value;
    }
    private void setRaw(int index, T value) {
        ensureCapacity(LargeInteger.valueOf(index));
        int[] digits = base1000Digits(index);
        Object[] node = root;
        int leadingZeros = height + 1 - digits.length;
        for (int level = 0; level < height; level++) {
            int slot = level < leadingZeros ? 0 : digits[level - leadingZeros];
            if (node[slot] == null)
                node[slot] = new Object[CHUNK_SIZE];
            node = (Object[]) node[slot];
        }
        int leafSlot = height < leadingZeros ? 0 : digits[height - leadingZeros];
        node[leafSlot] = value;
    }

    private Object[] leafForDigits(int[] digits) {
        Object[] node = root;
        int leadingZeros = height + 1 - digits.length;
        for (int level = 0; level < height; level++) {
            int slot = level < leadingZeros ? 0 : digits[level - leadingZeros];
            if (node[slot] == null)
                node[slot] = new Object[CHUNK_SIZE];
            node = (Object[]) node[slot];
        }
        return node;
    }
    private Object[] leafAt(int index) {return leafForDigits(base1000Digits(index));}
    private Object[] leafAt(LargeInteger index) {
        int[] digits = new int[height + 1];
        LargeInteger n = index;
        for (int i = height; i >= 0 && !n.isZero(); i--) {
            LargeInteger[] qr =
                    n.quotientAndRemainder(LargeInteger.THOUSAND);
            digits[i] = qr[1].intValue();
            n = qr[0];
        }

        if (!n.isZero())
            throw new IndexOutOfBoundsException("Index exceeds tree capacity");

        return leafForDigits(digits);
    }

    public LargeInteger size() {return size;}
    public boolean intSized() {return sizeInt != -1;}
    public int intSize() {
        if (intSized())
            return sizeInt;
        throw new ArithmeticException("List too large to fit in int");
    }

    private void ensureCapacity(LargeInteger index) {
        LargeInteger capacity = LargeInteger.THOUSAND.power(height + 1).numerator;
        while (index.greaterThanOrEqualTo(capacity)) {
            Object[] newRoot = new Object[CHUNK_SIZE];
            newRoot[0] = root;
            root = newRoot;
            height++;
            capacity = capacity.multiply(1000);
        }
    }
    @SuppressWarnings("unchecked")
    public T get(LargeInteger index) {
        if (index.isNegative() || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + size + ")");
        if(intSized()) return get(Integer.parseInt(index.toString()));
        int[] path = base1000Digits(index);
        Object[] node = root;
        for (int level = 0; level < height; level++) {
            int slot = path[level];
            if (node[slot] == null)
                return null;
            node = (Object[]) node[slot];
        }
        return (T) node[path[height]];
    }
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if(!intSized()) return get(LargeInteger.valueOf(index));
        if (index < 0 || index >= sizeInt)
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + sizeInt + ")");
        int[] digits = base1000Digits(index);
        Object[] node = root;
        int leadingZeros = height + 1 - digits.length;
        for (int level = 0; level < height; level++) {
            int slot = level < leadingZeros ? 0 : digits[level - leadingZeros];
            if (node[slot] == null)
                return null;
            node = (Object[]) node[slot];
        }
        int leafSlot = height < leadingZeros ? 0 : digits[height - leadingZeros];
        return (T) node[leafSlot];
    }
    public void swap(LargeInteger index1, LargeInteger index2) {
        T temp = get(index1);
        set(index1, get(index2));
        set(index2, temp);
    }
    public void swap(int index1, int index2) {
        T temp = get(index1);
        set(index1, get(index2));
        set(index2, temp);
    }
    public T getFirst() {return this.get(0);}
    public T getLast() {return intSized() ? get(sizeInt-1) : get(size.subtract(1));}
    public void addAll(LargeList<T> other) {for(T I: other) add(I);}
    T remove(LargeInteger index) {
        if(index.isNegative() || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + size + ")");
        T val = get(index);
        for(LargeInteger i = index; i.lessThan(size.subtract(1)); i = i.add(1))
            set(i, this.get(i.add(1)));
        set(size.subtract(1), null);
        size = size.subtract(1);
        if(intSized())
            sizeInt--;
        else {
            if (size.equals(LargeInteger.valueOf(Integer.MAX_VALUE)))
                sizeInt = Integer.MAX_VALUE;
        }
        return val;
    }
    public void remove(int index) {
        if(index < 0 || index >= sizeInt)
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + sizeInt + ")");
        if(intSized()) {
            for (int i = index; i < this.sizeInt - 1; i++)
                this.set(i, this.get(i + 1));
            set(this.sizeInt - 1, null);
        }else{
            for(LargeInteger i = LargeInteger.valueOf(index); i.lessThan(size.subtract(1)); i = i.add(1))
                this.set(i, this.get(i.add(1)));
            this.set(size.subtract(1), null);
        }
        size = size.subtract(1);
        if(intSized())
            sizeInt--;
        else
        if(size.equals(LargeInteger.valueOf(2147483647)))
            sizeInt = 2147483647;
    }
    public void removeFirst(){remove(0);}
    public void removeLast(){remove(size.subtract(1));}
    public void clear(){
        size = LargeInteger.ZERO;
        sizeInt = 0;
        height = 0;
        root = new Object[CHUNK_SIZE];
    }
    public boolean isEmpty(){return sizeInt==0;}
    private int[] base1000Digits(LargeInteger index) {
        if (index.isZero())
            return new int[]{0};
        int[] temp = new int[height + 1];
        LargeInteger copy = index;
        int count = 0;
        long base = 1000;
        while (copy.greaterThan(LargeInteger.ZERO)) {
            temp[count++] = (int) copy.mod(base);
            copy = copy.divide(LargeInteger.THOUSAND);
        }
        int[] result = new int[count];
        for (int i = 0; i < count; i++)
            result[i] = temp[count - 1 - i];
        return result;
    }
    private static int[] base1000Digits(int index) {
        if (index == 0)
            return new int[]{0};
        int len = 0;
        int temp = index;
        while (temp > 0) {
            len++;
            temp /= CHUNK_SIZE;
        }
        int[] digits = new int[len];
        for (int i = len - 1; i >= 0; i--) {
            digits[i] = index % CHUNK_SIZE;
            index /= CHUNK_SIZE;
        }
        return digits;
    }
    public boolean equals(LargeList<T> other){
        if (this.intSized() && other.intSized()) {
            if (this.sizeInt != other.sizeInt) return false;
            for (int i = 0; i < sizeInt; i++)
                if (!this.get(i).equals(other.get(i)))
                    return false;
            return true;
        }
        if(other.size.doesntEqual(size))
            return false;
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(size); i = i.add(1))
            if(!this.get(i).equals(other.get(i)))
                return false;
        return true;
    }
    public ArrayList<T> toArrayList(){
        if(!intSized()) return null;
        ArrayList<T> list = new ArrayList<>();
        for(T item: this)
            list.add(item);
        return list;
    }
    public Object[] toArray() {
        if (!intSized()) return null;
        return Arrays.copyOf(root, sizeInt);
    }
    public void reverse(){
        if(intSized())
            for(int i = 0; i < this.sizeInt/2; i++)
                swap(i, this.sizeInt-i-1);
        else
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(size.shiftRight(1)); i = i.add(1))
                swap(i, size.subtract(i));
    }

    @Override
    public Iterator<T> iterator() {
        if (intSized()) {
            return new Iterator<>() {
                private int nextIndex = 0;
                private int lastReturned = -1;
                private Object[] leaf;
                private int offset = 0;
                @Override
                public boolean hasNext() {return nextIndex < sizeInt;}
                @Override
                @SuppressWarnings("unchecked")
                public T next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    if (leaf == null || offset == CHUNK_SIZE) {
                        leaf = leafAt(nextIndex);
                        offset = nextIndex % CHUNK_SIZE;
                    }
                    lastReturned = nextIndex++;
                    return (T) leaf[offset++];
                }
                @Override
                public void remove() {
                    if (lastReturned < 0)
                        throw new IllegalStateException();
                    LargeList.this.remove(lastReturned);
                    nextIndex = lastReturned;
                    lastReturned = -1;
                    // Reposition after elements shift.
                    leaf = null;
                    offset = 0;
                }
            };
        }
        return new Iterator<>() {
            private static final int BLOCK_SIZE = CHUNK_SIZE;
            private LargeInteger blockStart = LargeInteger.ZERO;
            private Object[] leaf;
            private int offset;
            private int limit;
            private boolean hasMore;
            private LargeInteger lastReturnedBlock;
            private int lastReturnedOffset;
            private boolean canRemove = false;
            {
                loadAt(LargeInteger.ZERO);
            }
            private void loadBlock(LargeInteger start, int startOffset) {
                blockStart = start;
                leaf = leafAt(start);
                offset = startOffset;
                LargeInteger remaining = size.subtract(start);
                limit = remaining.lessThan(LargeInteger.THOUSAND) ? remaining.intValue() : BLOCK_SIZE;
                hasMore = true;
            }
            private void loadAt(LargeInteger position) {
                if (!position.lessThan(size)) {
                    leaf = null;
                    hasMore = false;
                    return;
                }
                LargeInteger[] qr = position.quotientAndRemainder(LargeInteger.THOUSAND);
                LargeInteger start = qr[0].multiply(1000);
                loadBlock(start, qr[1].intValue());
            }
            private void advanceBlock() {
                LargeInteger nextBlock = blockStart.add(1000);
                if (nextBlock.lessThan(size))
                    loadBlock(nextBlock, 0);
                else {
                    leaf = null;
                    hasMore = false;
                }
            }
            @Override
            public boolean hasNext() {return hasMore;}
            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                if (!hasMore)
                    throw new NoSuchElementException();

                lastReturnedBlock = blockStart;
                lastReturnedOffset = offset;
                canRemove = true;
                T value = (T) leaf[offset++];
                if (offset == limit)
                    advanceBlock();
                return value;
            }
            @Override
            public void remove() {
                if (!canRemove)
                    throw new IllegalStateException();
                LargeInteger removedIndex = lastReturnedBlock.add(LargeInteger.valueOf(lastReturnedOffset));
                LargeList.this.remove(removedIndex);
                canRemove = false;
                // Continue from the element shifted into
                // the removed position.
                loadAt(removedIndex);
            }
        };
    }

}
