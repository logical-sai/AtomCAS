package com.AtomCAS.utilities;
public class LargeMap<K, V>{
    private final LargeList<K> keys;
    private final LargeList<V> values;
    public LargeMap(){
        keys = new LargeList<>();
        values = new LargeList<>();
    }
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (!keys.intSized()) {
            for (LargeInteger i = LargeInteger.ZERO; i.lessThan(size()); i = i.add(1)) {
                sb.append(keys.get(i)).append(" -> ").append(values.get(i));
                if (i.doesntEqual(keys.size().subtract(1))) sb.append(", ");
            }
        }else{
            for(int k = 0; k < keys.intSize(); k++){
                sb.append(keys.get(k)).append(" -> ").append(values.get(k));
                if(k != keys.intSize()-1) sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
    public int hashCode(){return 0;}
    public LargeList<K> keys(){return keys;}
    public LargeList<V> values(){return values;}
    public K keyAtIndex(LargeInteger index){return keys.get(index);}
    public LargeInteger size(){return keys.size();}
    private void checkInvariant() {
        if (!keys.size().equals(values.size()))
            throw new IllegalStateException("LargeMap corrupted: key/value size mismatch");
    }
    public void put(K key, V value) {
        if (key == null || value == null)
            throw new IllegalArgumentException("Null keys/values not allowed");
        LargeInteger index = indexOfKey(key);
        if(index.lessThan(LargeInteger.ZERO)){
            keys.add(key);
            values.add(value);
        }else
            values.set(index, value);
        checkInvariant();
    }
    public void replace(LargeInteger index, K key, V value){
        if (key == null || value == null)
            throw new IllegalArgumentException("Null keys/values not allowed");
        keys.set(index, key);
        values.set(index, value);
        checkInvariant();
    }
    public void remove(K key) {
        LargeInteger index = indexOfKey(key);
        if (index.equals(LargeInteger.NEGATIVE_ONE)) return;
        keys.remove(index);
        values.remove(index);
        checkInvariant();
    }
    public boolean containsKey(K key) {return indexOfKey(key).doesntEqual(LargeInteger.NEGATIVE_ONE);}
    public LargeInteger indexOfKey(K key) {
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(keys.size()); i = i.add(1))
            if (keys.get(i).equals(key))
                return i;
        return LargeInteger.NEGATIVE_ONE;
    }
    public boolean isEmpty() {return keys.size().equals(LargeInteger.ZERO);}
    public V get(K key) {
        LargeInteger index = indexOfKey(key);
        if (index.equals(LargeInteger.NEGATIVE_ONE))
            throw new IllegalArgumentException("Key not found");
        return values.get(index);
    }
    public V getOrDefault(K key, V defaultValue) {
        LargeInteger index = indexOfKey(key);
        if (index.equals(LargeInteger.NEGATIVE_ONE))
            return defaultValue;
        return values.get(index);
    }
    public void swap(K key1, K key2){
        keys.swap(indexOfKey(key1), indexOfKey(key2));
        values.swap(indexOfKey(key2), indexOfKey(key1));
    }
}
