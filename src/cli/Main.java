package cli;
import core.Block;
import core.Blockchain;

import java.util.Scanner;

public class Main {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        Blockchain blockchain = new Blockchain(3);
        System.out.println("Simple Blockchain - Proof of Work (difficulty = "
                + blockchain.getDifficulty() + ")");

        while (true) {
            System.out.println("\n========== MENU ==========");
            System.out.println("1. Add a new block (mine)");
            System.out.println("2. Display the blockchain");
            System.out.println("3. Validate the blockchain");
            System.out.println("4. Simulate an attack (tamper with a block)");
            System.out.println("5. Re-mine blocks (repair the chain)");
            System.out.println("6. Change mining difficulty");
            System.out.println("0. Exit");
            System.out.print("Choice: ");
            if (!sc.hasNextLine()) return;

            switch (sc.nextLine().trim()) {
                case "1" -> addBlock(blockchain);
                case "2" -> blockchain.display();
                case "3" -> System.out.println(blockchain.isValid());
                case "4" -> attackMenu(blockchain);
                case "5" -> remine(blockchain);
                case "6" -> changeDifficulty(blockchain);
                case "0" -> { System.out.println("Bye!"); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void addBlock(Blockchain bc) {
        System.out.print("Enter block data (e.g. Alice pays Bob 5): ");
        String data = sc.nextLine();
        System.out.println("Mining at difficulty " + bc.getDifficulty() + "...");
        Block b = bc.addBlock(data);
        System.out.println("Block #" + b.getIndex() + " mined in " + b.getMiningTimeMs()
                + " ms (nonce = " + b.getNonce() + ")");
        System.out.println("Hash: " + b.getHash());
    }

    private static void attackMenu(Blockchain bc) {
        int i = readIndex(bc, "Block index to attack: ");
        if (i < 0) return;
        Block b = bc.getBlock(i);

        System.out.println("\nAttack type:");
        System.out.println("  a) Change the data only");
        System.out.println("  b) Replace the stored hash with a fake one");
        System.out.println("  c) Break the previous-hash link");
        System.out.println("  d) Change the data and recompute the hash (no mining)");
        System.out.print("Choice: ");
        String type = sc.nextLine().trim().toLowerCase();

        switch (type) {
            case "a" -> { System.out.print("New data: "); b.tamperWithData(sc.nextLine()); }
            case "b" -> b.tamperWithHash("f".repeat(64));
            case "c" -> b.tamperWithPreviousHash("a".repeat(64));
            case "d" -> { System.out.print("New data: "); b.tamperWithDataAndRehash(sc.nextLine()); }
            default -> { System.out.println("Unknown attack."); return; }
        }
        System.out.println("Block #" + i + " attacked. Use option 3 or 2 to see the effect.");
    }

    private static void remine(Blockchain bc) {
        int from = readIndex(bc, "Re-mine starting from block index: ");
        if (from < 0) return;
        System.out.println("Re-mining block #" + from + " and all " + (bc.size() - 1 - from)
                + " block(s) after it...");
        long ms = bc.remineFrom(from);
        System.out.println("Done in " + ms + " ms. Chain status: " + bc.isValid());
        System.out.println("(Every block after the attacked one had to be re-mined too, so the cost grows with chain length and difficulty.)");
    }

    private static void changeDifficulty(Blockchain bc) {
        System.out.print("New difficulty (1-6): ");
        try {
            int d = Integer.parseInt(sc.nextLine().trim());
            if (d < 1 || d > 6) throw new NumberFormatException();
            bc.setDifficulty(d);
            System.out.println("Difficulty set to " + d
                    + ". New blocks use it; old blocks keep their own difficulty.");
        } catch (NumberFormatException e) {
            System.out.println("Please enter a number between 1 and 6.");
        }
    }

    private static int readIndex(Blockchain bc, String prompt) {
        System.out.print(prompt);
        try {
            int i = Integer.parseInt(sc.nextLine().trim());
            if (i < 0 || i >= bc.size()) throw new NumberFormatException();
            return i;
        } catch (NumberFormatException e) {
            System.out.println("Invalid block index (0 to " + (bc.size() - 1) + ").");
            return -1;
        }
    }
}