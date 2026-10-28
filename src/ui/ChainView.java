package ui;
import core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;

/**
 * Draws the blockchain as a row of cards joined by arrows.
 * Green = valid, red = invalid, orange (dashed) = block currently being mined.
 * Click a card to select it.
 */
public class ChainView extends JPanel {
    public static final int CARD_W = 240, CARD_H = 270, GAP = 80, PAD = 24;

    private static final Color VALID = new Color(39, 140, 90);
    private static final Color INVALID = new Color(192, 57, 43);
    private static final Color MINING = new Color(230, 126, 34);
    private static final Color VALID_BG = new Color(240, 250, 244);
    private static final Color INVALID_BG = new Color(253, 237, 235);
    private static final Color MINING_BG = new Color(255, 246, 232);
    private static final Color SELECT = new Color(41, 98, 255);
    private static final Color LABEL = new Color(110, 110, 110);
    private static final Color TEXT = new Color(35, 35, 35);

    private static final Font SANS_B = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private static final Font SMALL_B = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    private static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font MONO_B = new Font(Font.MONOSPACED, Font.BOLD, 12);

    private Blockchain chain;
    private Block mining;
    private int selected = -1;
    private IntConsumer onSelect = i -> { };

    public ChainView(Blockchain chain) {
        this.chain = chain;
        setBackground(new Color(247, 248, 250));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                for (int i = 0; i < ChainView.this.chain.size(); i++) {
                    int x = cardX(i);
                    if (e.getX() >= x && e.getX() <= x + CARD_W
                            && e.getY() >= PAD && e.getY() <= PAD + CARD_H) {
                        selected = i;
                        onSelect.accept(i);
                        repaint();
                        return;
                    }
                }
            }
        });
        refresh();
    }

    public void setChain(Blockchain chain) { this.chain = chain; selected = -1; mining = null; refresh(); }
    public void setMining(Block b) { this.mining = b; refresh(); }
    public void setSelected(int i) { this.selected = i; repaint(); }
    public void setOnSelect(IntConsumer c) { this.onSelect = c; }

    /** Recompute the size (the chain may have grown) and repaint. */
    public void refresh() {
        int count = chain.size() + (mining != null ? 1 : 0);
        int w = PAD * 2 + count * CARD_W + (count - 1) * GAP;
        setPreferredSize(new Dimension(w, CARD_H + 2 * PAD));
        revalidate();
        repaint();
    }

    private int cardX(int i) { return PAD + i * (CARD_W + GAP); }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int n = chain.size();
        for (int i = 0; i < n; i++) {
            drawCard(g, cardX(i), chain.getBlock(i), chain.checkBlock(i), false, i == selected);
        }
        for (int i = 0; i < n - 1; i++) {
            boolean ok = chain.getBlock(i + 1).getPreviousHash().equals(chain.getBlock(i).getHash());
            drawArrow(g, cardX(i) + CARD_W, cardX(i + 1), ok ? VALID : INVALID,
                    ok ? "linked" : "BROKEN", false);
        }
        Block m = mining;
        if (m != null) {
            drawCard(g, cardX(n), m, null, true, false);
            drawArrow(g, cardX(n - 1) + CARD_W, cardX(n), MINING, "mining", true);
        }
        g.dispose();
    }

    private void drawCard(Graphics2D g, int x, Block b, String problem, boolean isMining, boolean isSelected) {
        int y = PAD;
        Color accent = isMining ? MINING : (problem == null ? VALID : INVALID);
        Color bg = isMining ? MINING_BG : (problem == null ? VALID_BG : INVALID_BG);
        RoundRectangle2D shape = new RoundRectangle2D.Float(x, y, CARD_W, CARD_H, 14, 14);

        g.setColor(bg);
        g.fill(shape);
        Shape oldClip = g.getClip();
        g.setClip(shape);
        g.setColor(accent);
        g.fillRect(x, y, CARD_W, 32);
        g.setClip(oldClip);

        g.setColor(isSelected ? SELECT : accent);
        g.setStroke(isMining
                ? new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f)
                : new BasicStroke(isSelected ? 3.5f : 1.5f));
        g.draw(shape);

        g.setFont(SANS_B);
        g.setColor(Color.WHITE);
        g.drawString("BLOCK #" + b.getIndex() + (b.getIndex() == 0 ? "  (GENESIS)" : ""), x + 12, y + 21);

        int tx = x + 12, w = CARD_W - 24;
        field(g, "Timestamp", b.getTimestamp(), tx, y + 50, w);
        field(g, "Data", b.getData(), tx, y + 84, w);
        field(g, "Nonce", b.getNonce() + "   (difficulty " + b.getDifficulty() + ")", tx, y + 118, w);
        hashField(g, "Previous hash", b.getPreviousHash(), tx, y + 152);
        hashField(g, "Hash", b.getHash(), tx, y + 186);

        g.setFont(SANS_B);
        if (isMining) {
            g.setColor(MINING);
            g.drawString("MINING...", tx, y + 228);
        } else {
            g.setColor(accent);
            g.drawString(problem == null ? "VALID" : "INVALID", tx, y + 228);
            if (problem != null) {
                g.setFont(SMALL);
                g.setColor(TEXT);
                String[] lines = wrap(problem, g.getFontMetrics(), w, 2);
                for (int i = 0; i < lines.length; i++) g.drawString(lines[i], tx, y + 244 + i * 12);
            }
        }
    }

    private void field(Graphics2D g, String label, String value, int x, int y, int maxW) {
        g.setFont(SMALL);
        g.setColor(LABEL);
        g.drawString(label, x, y);
        g.setFont(MONO);
        g.setColor(TEXT);
        g.drawString(clip(value, g.getFontMetrics(), maxW), x, y + 15);
    }

    /** Leading zeros (the Proof of Work) are highlighted. */
    private void hashField(Graphics2D g, String label, String hash, int x, int y) {
        g.setFont(SMALL);
        g.setColor(LABEL);
        g.drawString(label, x, y);

        String shown = hash.length() > 24 ? hash.substring(0, 24) + ".." : hash;
        int zeros = 0;
        while (zeros < shown.length() && shown.charAt(zeros) == '0') zeros++;
        String zs = shown.substring(0, zeros), rest = shown.substring(zeros);

        g.setFont(MONO_B);
        g.setColor(VALID);
        g.drawString(zs, x, y + 15);
        int zw = g.getFontMetrics().stringWidth(zs);
        g.setFont(MONO);
        g.setColor(TEXT);
        g.drawString(rest, x + zw, y + 15);
    }

    private void drawArrow(Graphics2D g, int x1, int x2, Color c, String text, boolean dashed) {
        int y = PAD + CARD_H / 2;
        g.setColor(c);
        g.setStroke(dashed
                ? new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f)
                : new BasicStroke(3f));
        g.drawLine(x1 + 4, y, x2 - 10, y);
        g.fillPolygon(new int[]{x2 - 2, x2 - 12, x2 - 12}, new int[]{y, y - 6, y + 6}, 3);
        g.setFont(SMALL_B);
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, x1 + (x2 - x1 - tw) / 2, y - 8);
    }

    private static String clip(String s, FontMetrics fm, int maxW) {
        if (fm.stringWidth(s) <= maxW) return s;
        while (s.length() > 1 && fm.stringWidth(s + "..") > maxW) s = s.substring(0, s.length() - 1);
        return s + "..";
    }

    private static String[] wrap(String text, FontMetrics fm, int maxW, int maxLines) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(test) > maxW && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        if (lines.size() > maxLines) {
            lines = new java.util.ArrayList<>(lines.subList(0, maxLines));
            lines.set(maxLines - 1, clip(lines.get(maxLines - 1) + " ..", fm, maxW));
        }
        return lines.toArray(new String[0]);
    }
}
