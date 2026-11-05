package com.rohan.standby;

/** Persisted face order ("3,0,1,..."). Always yields a permutation of 0..n-1 even if the saved text is stale or corrupt. */
final class FaceOrder {
    static int[] parse(String saved, int n) {
        boolean[] seen = new boolean[n]; int[] out = new int[n]; int k = 0;
        if (saved != null) for (String part : saved.split(",")) {
            try { int v = Integer.parseInt(part.trim()); if (v >= 0 && v < n && !seen[v]) { seen[v] = true; out[k++] = v; } } catch (NumberFormatException ignored) {}
        }
        for (int v = 0; v < n; v++) if (!seen[v]) out[k++] = v; // faces added by an update go to the end
        return out;
    }
    static String format(int[] order) { StringBuilder sb = new StringBuilder(); for (int i = 0; i < order.length; i++) { if (i > 0) sb.append(','); sb.append(order[i]); } return sb.toString(); }
    /** Moves the item at {@code index} by {@code dir} (-1 up, +1 down); returns a new array, unchanged at the ends. */
    static int[] move(int[] order, int index, int dir) {
        int to = index + dir; int[] out = order.clone(); if (index < 0 || index >= out.length || to < 0 || to >= out.length) return out;
        int t = out[index]; out[index] = out[to]; out[to] = t; return out;
    }
}
