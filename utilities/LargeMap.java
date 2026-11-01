package com.AtomCAS.utilities;
public final class LargeMap<K, V> implements Cloneable{
    private final LargeList<K> keys;
    private final LargeList<V> values;
    public LargeMap(){
        keys = new LargeList<>();
        values = new LargeList<>();
    }
    private LargeMap(LargeList<K> keys, LargeList<V> values) {
        this.keys = keys;
        this.values = values;
    }
    public LargeMap<K, V> clone() {
        try {
            @SuppressWarnings("unchecked")
            LargeMap<K, V> clone = (LargeMap<K, V>) super.clone();
            return new LargeMap<>(clone.keys.clone(), clone.values.clone());
        } catch (CloneNotSupportedException e) {throw new AssertionError(e);}
    }
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (keys.intSized())
            for(int k = 0; k < keys.intSize(); k++){
                sb.append(keys.get(k)).append(" -> ").append(values.get(k));
                if(k != keys.intSize()-1) sb.append(", ");
            }
        else for (LargeInteger i = LargeInteger.ZERO; i.lessThan(size()); i = i.add(1)) {
                sb.append(keys.get(i)).append(" -> ").append(values.get(i));
                if (i.doesntEqual(keys.size().subtract(1))) sb.append(", ");
            }
        sb.append("}");
        return sb.toString();
    }
    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("LargeMap<K,V> hashCode not implemented yet");
//        int h = 0;
//        for (K key : keys) {
//            int kh = key.hashCode();
//            int vh = get(key).hashCode();
//            int entryHash = CASHash.finish(CASHash.mix(ENTRY_TAG, kh) ^ vh);
//            h += entryHash;
//        }
//        return CASHash.finish(h);
    }
    public LargeList<K> keys() {return keys;}
    public LargeList<V> values() {return values;}
    K keyAtIndex(LargeInteger index) {return keys.get(index);}
    public LargeInteger size() {return keys.size();}
    private void checkInvariant() {
        if (!keys.size().equals(values.size()))
            throw new IllegalStateException("LargeMap corrupted: key/value size mismatch");
    }
    public boolean put(K key, V value) {
        if (key == null || value == null)
            throw new IllegalArgumentException("Null keys/values not allowed");
        if(containsKey(key)) return false;
        keys.add(key);
        values.add(value);
        checkInvariant();
        return true;
    }
    public V remove(K key) {
        LargeInteger index = indexOfKey(key);
        if (index.isNegative()) throw new IllegalArgumentException("Key not in map");
        keys.remove(index);
        V val = values.remove(index);
        checkInvariant();
        return val;
    }
    public boolean containsKey(K key) {return !indexOfKey(key).isNegative();}
    public LargeInteger indexOfKey(K key) {
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(keys.size()); i = i.add(1))
            if (keys.get(i).equals(key))
                return i;
        return LargeInteger.NEGATIVE_ONE;
    }
    public boolean isEmpty() {return keys.isEmpty();}
    public V get(K key) {
        LargeInteger index = indexOfKey(key);
        if (index.isNegative())
            throw new IllegalArgumentException("Key not found");
        return values.get(index);
    }
    public V getOrDefault(K key, V defaultValue) {
        LargeInteger index = indexOfKey(key);
        if (index.isNegative())
            return defaultValue;
        return values.get(index);
    }
    @SuppressWarnings("unused")
    public void swap(K key1, K key2){
        keys.swap(indexOfKey(key1), indexOfKey(key2));
        values.swap(indexOfKey(key2), indexOfKey(key1));
    }
    public void swapIndices(LargeInteger i, LargeInteger j) {
        keys.swap(i, j);
        values.swap(i, j);
    }
    public void set(K key, V value) {
        if (key == null || value == null)
            throw new IllegalArgumentException("Null keys/values not allowed");
        LargeInteger index = indexOfKey(key);
        if (index.isNegative()) put(key, value);
        else values.set(index, value);
    }
}
