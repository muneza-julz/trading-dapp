package ui;

import core.*;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

/**
 * Swing front-end for the blockchain: mine blocks, watch them link up,
 * attack a block, and see exactly where the chain breaks.
 */
public class BlockchainGUI extends JFrame {

    private Blockchain chain = new Blockchain(3);
    private final ChainView view = new ChainView(chain);
    private final JScrollPane chainScroll = new JScrollPane(view);

    private final JTextField dataField = new JTextField(26);
    private final JTextField editField = new JTextField(24);
    private final JButton mineBtn = new JButton("Mine Block");
    private final JButton validateBtn = new JButton("Validate Chain");
    private final JButton newChainBtn = new JButton("New Chain");
    private final JSpinner diffSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));

    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel miningLabel = new JLabel(" ");
    private final JLabel selectedLabel = new JLabel("No block selected - click a card above");
    private final JButton dataBtn, rehashBtn, hashBtn, linkBtn, remineBtn;
    private final JTextArea log = new JTextArea();

    private boolean busy = false;
    private int selected = -1;

    public BlockchainGUI() {
        super("Blockchain Demo - Proof of Work");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 4));

        // ---------- top: controls + status ----------
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controls.add(new JLabel("Block data:"));
        controls.add(dataField);
        controls.add(mineBtn);
        controls.add(Box.createHorizontalStrut(14));
        controls.add(new JLabel("Difficulty:"));
        diffSpinner.setPreferredSize(new Dimension(56, 26));
        controls.add(diffSpinner);
        controls.add(Box.createHorizontalStrut(14));
        controls.add(validateBtn);
        controls.add(newChainBtn);

        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 14f));
        miningLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel status = new JPanel(new GridLayout(2, 1));
        status.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 12));
        status.add(statusLabel);
        status.add(miningLabel);

        JPanel top = new JPanel(new BorderLayout());
        top.add(controls, BorderLayout.NORTH);
        top.add(status, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // ---------- centre: the chain ----------
        chainScroll.setBorder(BorderFactory.createTitledBorder("The blockchain (click a block to select it)"));
        chainScroll.getHorizontalScrollBar().setUnitIncrement(30);
        add(chainScroll, BorderLayout.CENTER);

        // ---------- bottom: attack panel + log ----------
        dataBtn = attackButton("1. Change data only",
                "Edits the data but leaves the stored hash alone -> hash no longer matches.",
                () -> attack("changed the data of Block #%d (stored hash left untouched)",
                        b -> b.tamperWithData(editField.getText())));
        rehashBtn = attackButton("2. Change data + recompute hash",
                "Hash matches the new data, but nobody did the mining work -> Proof of Work fails.",
                () -> attack("changed the data of Block #%d and recomputed its hash (no mining)",
                        b -> b.tamperWithDataAndRehash(editField.getText())));
        hashBtn = attackButton("3. Replace hash with a fake",
                "Overwrites the stored hash. This block AND the link to the next block break.",
                () -> attack("replaced the hash of Block #%d with a fake value",
                        b -> b.tamperWithHash("f".repeat(64))));
        linkBtn = attackButton("4. Break previous-hash link",
                "Points the block at a different 'previous' block.",
                () -> attack("changed the previous-hash link of Block #%d",
                        b -> b.tamperWithPreviousHash("a".repeat(64))));
        remineBtn = attackButton("Re-mine from selected block",
                "Repair: re-mine this block and EVERY block after it. Cost grows with length and difficulty.",
                this::remine);
        remineBtn.setForeground(new Color(30, 110, 60));

        JPanel attack = new JPanel(new GridBagLayout());
        attack.setBorder(BorderFactory.createTitledBorder("Attack simulator"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 6, 2, 6); c.weightx = 1;
        selectedLabel.setFont(selectedLabel.getFont().deriveFont(Font.BOLD));
        attack.add(selectedLabel, c);
        c.gridy = 1; attack.add(new JLabel("New data for the attack:"), c);
        c.gridy = 2; attack.add(editField, c);
        c.gridwidth = 1; c.gridy = 3; attack.add(dataBtn, c);
        c.gridx = 1; attack.add(rehashBtn, c);
        c.gridx = 0; c.gridy = 4; attack.add(hashBtn, c);
        c.gridx = 1; attack.add(linkBtn, c);
        c.gridx = 0; c.gridy = 5; c.gridwidth = 2; attack.add(remineBtn, c);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setBorder(BorderFactory.createTitledBorder("Activity log"));

        JPanel bottom = new JPanel(new GridLayout(1, 2, 6, 0));
        bottom.add(attack);
        bottom.add(logScroll);
        bottom.setPreferredSize(new Dimension(100, 225));
        add(bottom, BorderLayout.SOUTH);

        // ---------- behaviour ----------
        mineBtn.addActionListener(e -> mine());
        dataField.addActionListener(e -> mine());
        validateBtn.addActionListener(e -> {
            log("Validate: " + chain.isValid());
            refresh();
        });
        newChainBtn.addActionListener(e -> newChain());
        diffSpinner.addChangeListener(e -> {
            int d = (int) diffSpinner.getValue();
            chain.setDifficulty(d);
            log("Difficulty set to " + d + ": new blocks need a hash starting with "
                    + "0".repeat(d) + " (about 16^" + d + " = "
                    + NumberFormat.getIntegerInstance().format((long) Math.pow(16, d)) + " attempts).");
        });
        view.setOnSelect(this::onSelect);

        log("Welcome! 1) Type some data and press Mine Block.  2) Add a few blocks.");
        log("3) Click a block and try an attack.  4) Watch the chain turn red.  5) Re-mine to repair it.");
        refresh();
        updateButtons();

        setSize(1180, 720);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void mine() {
        if (busy) return;
        String data = dataField.getText().trim();
        if (data.isEmpty()) data = "Transaction #" + chain.size();
        final int d = chain.getDifficulty();
        final Block candidate = chain.prepareBlock(data);

        setBusy(true);
        view.setMining(candidate);
        scrollToEnd();
        log("Mining Block #" + candidate.getIndex() + " at difficulty " + d + "...");

        Timer live = new Timer(40, e -> miningLabel.setText(String.format(
                "Mining Block #%d | nonce %,d | hash %s.. | target %s...",
                candidate.getIndex(), candidate.getNonce(),
                candidate.getHash().substring(0, 20), "0".repeat(d))));
        live.start();

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                candidate.mineBlock(d);
                return null;
            }
            @Override protected void done() {
                live.stop();
                chain.appendBlock(candidate);
                view.setMining(null);
                miningLabel.setText(" ");
                dataField.setText("");
                log(String.format("Mined Block #%d in %,d ms after %,d attempts. Hash: %s..",
                        candidate.getIndex(), candidate.getMiningTimeMs(), candidate.getNonce() + 1,
                        candidate.getHash().substring(0, 20)));
                setBusy(false);
                refresh();
                scrollToEnd();
                dataField.requestFocusInWindow();
            }
        }.execute();
    }

    private void attack(String message, java.util.function.Consumer<Block> action) {
        if (selected < 0 || busy) return;
        action.accept(chain.getBlock(selected));
        log("ATTACK: " + String.format(message, selected));
        log("   -> " + chain.isValid());
        refresh();
    }

    private void remine() {
        if (selected < 0 || busy) return;
        final int from = selected;
        setBusy(true);
        miningLabel.setText("Re-mining Block #" + from + " and everything after it...");
        log("Re-mining " + (chain.size() - from) + " block(s) starting at Block #" + from + "...");
        new SwingWorker<Long, Void>() {
            @Override protected Long doInBackground() { return chain.remineFrom(from); }
            @Override protected void done() {
                long ms = 0;
                try { ms = get(); } catch (Exception ignored) { }
                miningLabel.setText(" ");
                log(String.format("Done in %,d ms. %s", ms, chain.isValid()));
                setBusy(false);
                refresh();
            }
        }.execute();
    }

    private void newChain() {
        if (busy) return;
        chain = new Blockchain((int) diffSpinner.getValue());
        view.setChain(chain);
        selected = -1;
        selectedLabel.setText("No block selected - click a card above");
        log("Started a new chain with a fresh genesis block.");
        refresh();
        updateButtons();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void onSelect(int i) {
        selected = i;
        selectedLabel.setText("Selected: Block #" + i);
        editField.setText(chain.getBlock(i).getData());
        updateButtons();
    }

    private void refresh() {
        view.refresh();
        ValidationResult r = chain.isValid();
        statusLabel.setText("Chain status: " + r);
        statusLabel.setForeground(r.isValid() ? new Color(30, 130, 70) : new Color(190, 50, 40));
    }

    private void setBusy(boolean b) {
        busy = b;
        updateButtons();
    }

    private void updateButtons() {
        boolean attackReady = selected >= 0 && !busy;
        for (JButton btn : new JButton[]{dataBtn, rehashBtn, hashBtn, linkBtn, remineBtn}) {
            btn.setEnabled(attackReady);
        }
        editField.setEnabled(attackReady);
        mineBtn.setEnabled(!busy);
        validateBtn.setEnabled(!busy);
        newChainBtn.setEnabled(!busy);
        diffSpinner.setEnabled(!busy);
        dataField.setEnabled(!busy);
    }

    private void scrollToEnd() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = chainScroll.getHorizontalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private JButton attackButton(String text, String tip, Runnable action) {
        JButton b = new JButton(text);
        b.setToolTipText(tip);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void log(String msg) {
        log.append(msg + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new BlockchainGUI().setVisible(true));
    }
}
