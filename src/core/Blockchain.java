package core;

import java.util.ArrayList;
import java.util.List;

/** The chain: a linked list of blocks secured by hashes and Proof of Work. */
public class Blockchain {
    private final List<Block> chain = new ArrayList<>();
    private int difficulty;

    public Blockchain(int difficulty) {
        this.difficulty = difficulty;
        Block genesis = new Block(0, "Genesis Block", "0");
        genesis.mineBlock(difficulty);
        chain.add(genesis);
    }

    public Block getLatestBlock() { return chain.get(chain.size() - 1); }

    /** Create (but do not mine) a block linked to the latest block. */
    public Block prepareBlock(String data) {
        return new Block(chain.size(), data, getLatestBlock().getHash());
    }

    /** Append a block that has already been mined. */
    public void appendBlock(Block block) {
        chain.add(block);
    }

    /** Create a block linked to the latest one, mine it, then append it. */
    public Block addBlock(String data) {
        Block newBlock = prepareBlock(data);
        newBlock.mineBlock(difficulty);
        appendBlock(newBlock);
        return newBlock;
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /** Returns null if block i is fine, otherwise the reason it is not. */
    public String checkBlock(int i) {
        Block current = chain.get(i);

        if (!current.getHash().equals(current.calculateHash())) {
            return "stored hash does not match the block's contents (data was altered).";
        }
        if (!current.getHash().startsWith("0".repeat(current.getDifficulty()))) {
            return "hash lacks the " + current.getDifficulty()
                    + " leading zeros - Proof of Work is missing.";
        }
        if (i > 0 && !current.getPreviousHash().equals(chain.get(i - 1).getHash())) {
            return "previous-hash link does not match block #" + (i - 1) + "'s hash.";
        }
        return null;
    }

    /** Validate the whole chain, stopping at the first broken block. */
    public ValidationResult isValid() {
        for (int i = 0; i < chain.size(); i++) {
            String problem = checkBlock(i);
            if (problem != null) return ValidationResult.fail(i, problem);
        }
        return ValidationResult.ok();
    }

    // ------------------------------------------------------------------
    // Repair: the real cost of changing history
    // ------------------------------------------------------------------

    /**
     * Re-mine block 'from' and EVERY block after it, fixing each link.
     * Returns the total time spent, which grows with the number of blocks
     * and the difficulty.
     */
    public long remineFrom(int from) {
        long total = 0;
        for (int i = from; i < chain.size(); i++) {
            Block b = chain.get(i);
            if (i > 0) b.setPreviousHash(chain.get(i - 1).getHash());
            b.mineBlock(difficulty);
            total += b.getMiningTimeMs();
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Display
    // ------------------------------------------------------------------

    /** Print the chain as connected boxes, with a status line per block. */
    public void display() {
        final int w = 62;
        String line = "+" + "-".repeat(w) + "+";
        for (int i = 0; i < chain.size(); i++) {
            Block b = chain.get(i);
            String problem = checkBlock(i);

            System.out.println(line);
            row(w, "BLOCK #" + b.getIndex() + (i == 0 ? "  (GENESIS)" : ""));
            System.out.println("|" + "-".repeat(w) + "|");
            row(w, "Timestamp  : " + b.getTimestamp());
            row(w, "Data       : " + cut(b.getData(), w - 15));
            row(w, "Difficulty : " + b.getDifficulty());
            row(w, "Nonce      : " + b.getNonce());
            row(w, "Prev Hash  : " + shortHash(b.getPreviousHash()));
            row(w, "Hash       : " + shortHash(b.getHash()));
            System.out.println("|" + "-".repeat(w) + "|");
            row(w, problem == null ? "Status     : VALID"
                                   : "Status     : INVALID");
            if (problem != null) row(w, "  -> " + cut(problem, w - 8));
            System.out.println(line);

            if (i < chain.size() - 1) {
                System.out.println("                              |");
                System.out.println("                              | (prev hash)");
                System.out.println("                              v");
            }
        }
    }

    private void row(int w, String text) {
        System.out.printf("| %-" + (w - 2) + "s |%n", text);
    }

    private String shortHash(String h) {
        return h.length() > 40 ? h.substring(0, 20) + "..." + h.substring(h.length() - 16) : h;
    }

    private String cut(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public int size() { return chain.size(); }
    public Block getBlock(int i) { return chain.get(i); }
}
