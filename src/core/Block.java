package core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A single block: index, timestamp, data, previous hash, nonce, difficulty
 * and its own hash. The difficulty it was mined at is part of the hash, so
 * every block can be checked against its own Proof of Work target.
 */
public class Block {
    private final int index;
    private final String timestamp;
    private String data;
    private String previousHash;
    private volatile int nonce;
    private volatile int difficulty;
    private volatile String hash;
    private long miningTimeMs;

    public Block(int index, String data, String previousHash) {
        this.index = index;
        this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.data = data;
        this.previousHash = previousHash;
        this.hash = calculateHash();
    }

    /** SHA-256 over all block contents. Changing anything changes the hash. */
    public String calculateHash() {
        String input = index + timestamp + data + previousHash + nonce + difficulty;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Proof of Work: search for a nonce so the hash starts with N zeros.
     * Also used to RE-mine a block after it has been changed.
     */
    public void mineBlock(int difficulty) {
        this.difficulty = difficulty;
        this.nonce = 0;
        this.hash = calculateHash();
        String target = "0".repeat(difficulty);
        long start = System.currentTimeMillis();
        while (!hash.startsWith(target)) {
            nonce++;
            hash = calculateHash();
        }
        miningTimeMs = System.currentTimeMillis() - start;
    }

    // ---------- getters ----------
    public int getIndex() { return index; }
    public String getTimestamp() { return timestamp; }
    public String getData() { return data; }
    public String getPreviousHash() { return previousHash; }
    public int getNonce() { return nonce; }
    public int getDifficulty() { return difficulty; }
    public String getHash() { return hash; }
    public long getMiningTimeMs() { return miningTimeMs; }

    // ---------- used when repairing a chain ----------
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    // ---------- attack simulations (for the demo only) ----------

    /** Change the data but leave the stored hash alone. */
    public void tamperWithData(String newData) { this.data = newData; }

    /** Overwrite the stored hash with a made-up value. */
    public void tamperWithHash(String fakeHash) { this.hash = fakeHash; }

    /** Point this block at a different "previous" block. */
    public void tamperWithPreviousHash(String fakeRef) { this.previousHash = fakeRef; }

    /** Change the data AND recompute the hash, but skip the mining work. */
    public void tamperWithDataAndRehash(String newData) {
        this.data = newData;
        this.hash = calculateHash();
    }
}
