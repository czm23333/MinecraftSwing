package io.github.czm23333.minecraftswing;

public class Pair<T, U> {
    public T first;
    public U second;

    private Pair() {}

    public static <T, U> Pair<T, U> of(T first, U second) {
        Pair<T, U> pair = new Pair<>();
        pair.first = first;
        pair.second = second;
        return pair;
    }
}
