package com.AtomCAS.utilities;

import java.util.Arrays;
import java.util.SplittableRandom;

public class MultiplyThresholdBenchmark {
    private static volatile long sink;

    private static final int SAMPLES = 7;
    private static final long SAMPLE_TIME_NS = 50_000_000L; // 50 ms/sample
    private static final long WARMUP_TIME_NS = 100_000_000L;

    private static long[] randomMagnitude(int limbs, SplittableRandom rng) {
        long[] x = new long[limbs];

        for (int i = 0; i < limbs; i++)
            x[i] = rng.nextLong();

        // Make sure the requested limb length is actually used.
        if (x[limbs - 1] == 0)
            x[limbs - 1] = 1;

        return x;
    }

    private static void consume(long[] x) {
        sink ^= x[0];
        sink ^= x[x.length - 1];
        sink ^= x.length;
    }

    private static double benchmark(long[] a, long[] b, int kt, int t3t) {
        // Warmup this particular path.
        long warmupStart = System.nanoTime();

        while (System.nanoTime() - warmupStart < WARMUP_TIME_NS) {
            consume(LargeInteger.multiplyMagnitudesWithThresholds(
                    a, b, kt, t3t
            ));
        }

        double[] samples = new double[SAMPLES];

        for (int s = 0; s < SAMPLES; s++) {
            long start = System.nanoTime();
            long elapsed;
            long operations = 0;

            // Checking nanoTime once per 16 multiplies keeps timer overhead low.
            do {
                for (int i = 0; i < 16; i++) {
                    consume(LargeInteger.multiplyMagnitudesWithThresholds(
                            a, b, kt, t3t
                    ));
                }

                operations += 16;
                elapsed = System.nanoTime() - start;

            } while (elapsed < SAMPLE_TIME_NS);

            samples[s] = elapsed / (double) operations;
        }

        Arrays.sort(samples);
        return samples[SAMPLES / 2];
    }

    static void main(String[] args) {
        int[] sizes = {
                116, 118, 120, 122, 124, 126, 128, 130
        };

        SplittableRandom rng = new SplittableRandom(123456789);
        for (int size : sizes) {
            long[] a = randomMagnitude(size, rng);
            long[] b = randomMagnitude(size, rng);

            System.out.println("size " + size);

            double toom = benchmark(a, b, 28, size);
            double kara = benchmark(a, b, 28, size + 1);

            System.out.println(
                    toom + "     " +
                            kara + "     ratio " +
                            toom / kara
            );
        }
    }
}
