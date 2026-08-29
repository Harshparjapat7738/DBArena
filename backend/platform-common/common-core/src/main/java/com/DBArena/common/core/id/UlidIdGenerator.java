package com.DBArena.common.core.id;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ULID (Universally Unique Lexicographically Sortable Identifier)
 * generator: 48-bit millisecond timestamp + 80 bits of randomness,
 * Crockford base32 encoded to a 26-character string. Chosen over UUIDv4
 * so that ids are roughly time-ordered, which keeps primary-key inserts
 * and cursor-based pagination cheap in both Postgres and Mongo.
 *
 * <p>Monotonic within the same millisecond on a single instance: if two
 * calls land in the same millisecond, the random component is
 * incremented rather than redrawn, so {@code next()} never goes backwards
 * on one generator instance.
 */
public final class UlidIdGenerator implements IdGenerator {

    private static final char[] CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int TIME_LEN = 10;
    private static final int RANDOM_LEN = 16;

    private final Clock clock;
    private final SecureRandom random;
    private final ReentrantLock lock = new ReentrantLock();

    private long lastTimestamp = -1L;
    private byte[] lastRandom = new byte[10];

    public UlidIdGenerator() {
        this(Clock.systemUTC(), new SecureRandom());
    }

    public UlidIdGenerator(Clock clock, SecureRandom random) {
        this.clock = clock;
        this.random = random;
    }

    @Override
    public String next() {
        lock.lock();
        try {
            long timestamp = clock.millis();
            byte[] randomBytes;
            if (timestamp == lastTimestamp) {
                randomBytes = incrementRandom(lastRandom);
            } else {
                randomBytes = new byte[10];
                random.nextBytes(randomBytes);
            }
            lastTimestamp = timestamp;
            lastRandom = randomBytes;
            return encode(timestamp, randomBytes);
        } finally {
            lock.unlock();
        }
    }

    private static byte[] incrementRandom(byte[] previous) {
        byte[] next = previous.clone();
        for (int i = next.length - 1; i >= 0; i--) {
            if ((next[i] & 0xFF) == 0xFF) {
                next[i] = 0;
            } else {
                next[i]++;
                break;
            }
        }
        return next;
    }

    private static String encode(long timestamp, byte[] randomBytes) {
        char[] out = new char[TIME_LEN + RANDOM_LEN];

        long t = timestamp;
        for (int i = TIME_LEN - 1; i >= 0; i--) {
            out[i] = CROCKFORD_ALPHABET[(int) (t & 0x1F)];
            t >>>= 5;
        }

        // 80 bits of randomness packed 5 bits at a time into 16 base32 chars.
        long hi = ((long) (randomBytes[0] & 0xFF) << 32)
                | ((long) (randomBytes[1] & 0xFF) << 24)
                | ((long) (randomBytes[2] & 0xFF) << 16)
                | ((long) (randomBytes[3] & 0xFF) << 8)
                | (randomBytes[4] & 0xFF);
        long lo = ((long) (randomBytes[5] & 0xFF) << 32)
                | ((long) (randomBytes[6] & 0xFF) << 24)
                | ((long) (randomBytes[7] & 0xFF) << 16)
                | ((long) (randomBytes[8] & 0xFF) << 8)
                | (randomBytes[9] & 0xFF);

        for (int i = 0; i < 8; i++) {
            out[TIME_LEN + 7 - i] = CROCKFORD_ALPHABET[(int) (hi & 0x1F)];
            hi >>>= 5;
        }
        for (int i = 0; i < 8; i++) {
            out[TIME_LEN + 15 - i] = CROCKFORD_ALPHABET[(int) (lo & 0x1F)];
            lo >>>= 5;
        }
        return new String(out);
    }
}
