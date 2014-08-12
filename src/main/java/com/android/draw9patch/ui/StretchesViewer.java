/*
 *
 *  Copyright (C) 2013 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.android.draw9patch.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.*;

public class StretchesViewer extends JTabbedPane {
    public static final float DEFAULT_SCALE = 2.0f;
    private static final int MARGIN = 10;

    private final Container container;
    private final ImageViewer viewer;
    private final TexturePaint texture;
    private final StretchView mExact;

    private BufferedImage image;
    private PatchInfo patchInfo;

    private StretchView horizontal;
    private StretchView vertical;
    private StretchView both;

    private Dimension size;

    private float horizontalPatchesSum;
    private float verticalPatchesSum;

    private boolean showPadding;

    StretchesViewer(Container container, ImageViewer viewer, TexturePaint texture) {
        this.container = container;
        this.viewer = viewer;
        this.texture = texture;

        image = viewer.getImage();
        patchInfo = viewer.getPatchInfo();

        viewer.addPatchUpdateListener(new ImageViewer.PatchUpdateListener() {
            @Override
            public void patchesUpdated() {
                computePatches();
                refreshExactSize();
            }
        });

        setOpaque(false);

        mExact = new StretchView();
        horizontal = new StretchView();
        vertical = new StretchView();
        both = new StretchView();

        setScale(DEFAULT_SCALE);

        JTabbedPane tab = new JTabbedPane();
        add(tab);
        tab.addTab("↕↔", both);
        tab.addTab("↕", vertical);
        tab.addTab("↔", horizontal);
        tab.addTab("Text", mExact);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(texture);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    private float mScale = 0;
    void setScale(float scale) {
        mScale = scale;
        int patchWidth = image.getWidth() - 2;
        int patchHeight = image.getHeight() - 2;

        int scaledWidth = (int) (patchWidth * scale);
        int scaledHeight = (int) (patchHeight * scale);


        mExact.scaledWidth = patchWidth;
        mExact.scaledHeight = patchHeight;
        refreshExactSize();

        horizontal.scaledWidth = scaledWidth;
        vertical.scaledHeight = scaledHeight;
        both.scaledWidth = scaledWidth;
        both.scaledHeight = scaledHeight;

        size = new Dimension(scaledWidth, scaledHeight);

        computePatches();
    }

    private void refreshExactSize() {
        if (mFontMetrics != null && mTextValue != null) {
            String text = mTextValue.replaceAll("\\\\n", "\n");
            int lines = text.split("\\n").length;
            TextRenderHelper helper = new TextRenderHelper(mFontMetrics, text, Integer.MAX_VALUE, Integer.MAX_VALUE);
            ArrayList<TextRenderHelper.LineRenderContext> lineRenderContexts = helper.prepareForRender(TextRenderHelper.Gravity.LEFT_TOP, TextRenderHelper.Gravity.LEFT_TOP);
            int maxWidth = 0;
            for (TextRenderHelper.LineRenderContext lineRenderContext : lineRenderContexts) {
                maxWidth = Math.max(maxWidth, lineRenderContext.width);
            }
            int height = lines * (mFontMetrics.getMaxAscent() + mFontMetrics.getMaxDescent());
            if (text.length() == 0) {
                mExact.scaledWidth = patchInfo.horizontalPadding.first + patchInfo.horizontalPadding.second;
                mExact.scaledHeight = patchInfo.verticalPadding.first + patchInfo.verticalPadding.second;
            } else {
                mExact.scaledWidth = maxWidth + patchInfo.horizontalPadding.first + patchInfo.horizontalPadding.second + 2;
                mExact.scaledHeight = height + patchInfo.verticalPadding.first + patchInfo.verticalPadding.second + 2;
            }

            computePatches();
        }
    }

    void computePatches() {
        image = viewer.getImage();
        patchInfo = viewer.getPatchInfo();

        boolean measuredWidth = false;
        boolean endRow = true;

        int remainderHorizontal = 0;
        int remainderVertical = 0;

        if (!patchInfo.fixed.isEmpty()) {
            int start = patchInfo.fixed.get(0).y;
            for (Rectangle rect : patchInfo.fixed) {
                if (rect.y > start) {
                    endRow = true;
                    measuredWidth = true;
                }
                if (!measuredWidth) {
                    remainderHorizontal += rect.width;
                }
                if (endRow) {
                    remainderVertical += rect.height;
                    endRow = false;
                    start = rect.y;
                }
            }
        } else {
            /* fully stretched without fixed regions (often single pixel high or wide). Since
             * width of vertical patches (and height of horizontal patches) are fixed, use them to
             * determine fixed space
             */
            for (Rectangle rect : patchInfo.verticalPatches) {
                remainderHorizontal += rect.width;
            }
            for (Rectangle rect : patchInfo.horizontalPatches) {
                remainderVertical += rect.height;
            }
        }

        horizontal.remainderHorizontal = horizontal.scaledWidth - remainderHorizontal;
        vertical.remainderHorizontal = vertical.scaledWidth - remainderHorizontal;
        both.remainderHorizontal = both.scaledWidth - remainderHorizontal;


        horizontal.remainderVertical = horizontal.scaledHeight - remainderVertical;
        vertical.remainderVertical = vertical.scaledHeight - remainderVertical;
        both.remainderVertical = both.scaledHeight - remainderVertical;

        mExact.remainderHorizontal = mExact.scaledWidth - remainderHorizontal;
        mExact.remainderVertical = mExact.scaledHeight - remainderVertical;

        horizontalPatchesSum = 0;
        if (patchInfo.horizontalPatches.size() > 0) {
            int start = -1;
            for (Rectangle rect : patchInfo.horizontalPatches) {
                if (rect.x > start) {
                    horizontalPatchesSum += rect.width;
                    start = rect.x;
                }
            }
        } else {
            int start = -1;
            for (Rectangle rect : patchInfo.patches) {
                if (rect.x > start) {
                    horizontalPatchesSum += rect.width;
                    start = rect.x;
                }
            }
        }

        verticalPatchesSum = 0;
        if (patchInfo.verticalPatches.size() > 0) {
            int start = -1;
            for (Rectangle rect : patchInfo.verticalPatches) {
                if (rect.y > start) {
                    verticalPatchesSum += rect.height;
                    start = rect.y;
                }
            }
        } else {
            int start = -1;
            for (Rectangle rect : patchInfo.patches) {
                if (rect.y > start) {
                    verticalPatchesSum += rect.height;
                    start = rect.y;
                }
            }
        }

        setSize(size);
        container.validate();
        repaint();
    }

    void setPaddingVisible(boolean visible) {
        showPadding = visible;
        repaint();
    }

    void setTextVisible(boolean visible) {
        mShowText = visible;
        repaint();
    }

    private class StretchView extends JComponent {
        private final Color PADDING_COLOR = new Color(0.37f, 0.37f, 1.0f, 0.5f);

        int scaledWidth;
        int scaledHeight;

        int remainderHorizontal;
        int remainderVertical;

        StretchView() {
            scaledWidth = image.getWidth();
            scaledHeight = image.getHeight();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int x = (getWidth() - scaledWidth) / 2;
            int y = (getHeight() - scaledHeight) / 2;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.translate(x, y);

            x = 0;
            y = 0;

            if (patchInfo.patches.isEmpty()) {
                g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
                g2.dispose();
                return;
            }

            int fixedIndex = 0;
            int horizontalIndex = 0;
            int verticalIndex = 0;
            int patchIndex = 0;

            boolean hStretch;
            boolean vStretch;

            float vWeightSum = 1.0f;
            float vRemainder = remainderVertical;

            vStretch = patchInfo.verticalStartWithPatch;
            while (y < scaledHeight - 1) {
                hStretch = patchInfo.horizontalStartWithPatch;

                int height = 0;
                float vExtra = 0.0f;

                float hWeightSum = 1.0f;
                float hRemainder = remainderHorizontal;

                while (x < scaledWidth - 1) {
                    Rectangle r;
                    if (!vStretch) {
                        if (hStretch) {
                            r = patchInfo.horizontalPatches.get(horizontalIndex++);
                            float extra = r.width / horizontalPatchesSum;
                            int width = (int) (extra * hRemainder / hWeightSum);
                            hWeightSum -= extra;
                            hRemainder -= width;
                            g.drawImage(image, x, y, x + width, y + r.height, r.x, r.y,
                                    r.x + r.width, r.y + r.height, null);
                            x += width;
                        } else {
                            r = patchInfo.fixed.get(fixedIndex++);
                            g.drawImage(image, x, y, x + r.width, y + r.height, r.x, r.y,
                                    r.x + r.width, r.y + r.height, null);
                            x += r.width;
                        }
                        height = r.height;
                    } else {
                        if (hStretch) {
                            r = patchInfo.patches.get(patchIndex++);
                            vExtra = r.height / verticalPatchesSum;
                            height = (int) (vExtra * vRemainder / vWeightSum);
                            float extra = r.width / horizontalPatchesSum;
                            int width = (int) (extra * hRemainder / hWeightSum);
                            hWeightSum -= extra;
                            hRemainder -= width;
                            g.drawImage(image, x, y, x + width, y + height, r.x, r.y,
                                    r.x + r.width, r.y + r.height, null);
                            x += width;
                        } else {
                            r = patchInfo.verticalPatches.get(verticalIndex++);
                            vExtra = r.height / verticalPatchesSum;
                            height = (int) (vExtra * vRemainder / vWeightSum);
                            g.drawImage(image, x, y, x + r.width, y + height, r.x, r.y,
                                    r.x + r.width, r.y + r.height, null);
                            x += r.width;
                        }

                    }
                    hStretch = !hStretch;
                }
                x = 0;
                y += height;
                if (vStretch) {
                    vWeightSum -= vExtra;
                    vRemainder -= height;
                }
                vStretch = !vStretch;
            }

            Rectangle r = new Rectangle(patchInfo.horizontalPadding.first,
                    patchInfo.verticalPadding.first,
                    scaledWidth - patchInfo.horizontalPadding.first
                            - patchInfo.horizontalPadding.second,
                    scaledHeight - patchInfo.verticalPadding.first
                            - patchInfo.verticalPadding.second);

            if (showPadding) {
                g.setColor(PADDING_COLOR);
                g.fillRect(r.x, r.y, r.width, r.height);
            }

            if(mShowText && mTextValue != null && mTextValue.length() > 0){
                g.setColor(Color.BLACK);
                String s = mTextValue.replaceAll("\\\\n", "\n");// abc GJK gjkl";
                g.clipRect(r.x, r.y, r.width, r.height);
                g.translate(r.x, r.y);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g.setFont(mFont);
                TextRenderHelper helper = new TextRenderHelper(g.getFontMetrics(), s, r.width, r.height);
                for (TextRenderHelper.LineRenderContext lrc : helper.prepareForRender(mHorizontal, mVertical)) {
                    g.drawString(lrc.text, lrc.x, lrc.y);
                }
            }

            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return size;
        }
    }

    private String mTextValue;
    private int mFontSize = 24;
    private Font mFont = new Font(Font.MONOSPACED, Font.PLAIN, mFontSize);
    private FontMetrics mFontMetrics;
    private boolean mShowText;
    private TextRenderHelper.Gravity mVertical = TextRenderHelper.Gravity.LEFT_TOP;
    private TextRenderHelper.Gravity mHorizontal = TextRenderHelper.Gravity.LEFT_TOP;

    public void setTextValue(String textValue) {
        mTextValue = textValue;
        setScale(mScale);
        refreshExactSize();
        repaint();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        refreshExactSize();
        mFont = font;
        mFontMetrics = new Canvas().getFontMetrics(mFont);
        repaint();
    }

    public int getFontSize() {
        return mFontSize;
    }

    public void setFontSize(int fontSize) {
        mFontSize = fontSize;
        mFont = new Font(mFont.getName(), mFont.isPlain() ? Font.PLAIN : Font.BOLD, mFontSize);
        mFontMetrics = new Canvas().getFontMetrics(mFont);
        refreshExactSize();
        repaint();
    }

    public void setTextHorizontalGravity(int value) {
        mHorizontal = TextRenderHelper.Gravity.valueOf(value);
        repaint();
    }

    public void setTextVerticalGravity(int value) {
        mVertical = TextRenderHelper.Gravity.valueOf(value);
        repaint();
    }

    private static class TextRenderHelper{
        private String mText;

        public enum Gravity {
            LEFT_TOP(0), CENTER(1), RIGHT_BOTTOM(2);
            static final Gravity[] ARRAY = new Gravity[]{LEFT_TOP, CENTER, RIGHT_BOTTOM};

            private final int value;

            private Gravity(final int newValue) {
                value = newValue;
            }

            public int getValue() { return value; }

            public static Gravity valueOf(int value) {
                return ARRAY[value];
            }
        }

        private final FontMetrics mFontMetrics;
        private int mMaxWidth;
        private int mMaxHeight;
        private final int mLineHeight;

        public TextRenderHelper(FontMetrics metrics, String text, int maxWidth, int maxHeight) {
            mFontMetrics = metrics;
            mLineHeight = mFontMetrics.getHeight();//mFontMetrics.getMaxAscent() + mFontMetrics.getMaxDescent();
            mText = text;
            mMaxWidth = maxWidth;
            mMaxHeight = maxHeight;
        }

        public ArrayList<LineRenderContext> prepareForRender(Gravity horizontal, Gravity vertical) {
            ArrayList<LineRenderContext> list = createLineContexts(horizontal);
            countVerticalPositions(list, vertical);
            return list;
        }

        private ArrayList<LineRenderContext> createLineContexts(Gravity horizontal) {
            ArrayList<LineRenderContext> result = new ArrayList<LineRenderContext>();
            int width = mFontMetrics.stringWidth(mText);
            if (width < mMaxWidth && !mText.contains("\n")) {
                //single line
                result.add(new LineRenderContext(getX(horizontal, width), mText, width));
            } else {
                char[] chars = mText.toCharArray();
                String currentLine = String.valueOf(chars[0]);
                width = mFontMetrics.stringWidth(currentLine);
                for (int i = 1; i < chars.length; i++) {
                    char c = chars[i];
                    boolean newLine = '\n' == c;
                    int newWidth = mFontMetrics.stringWidth(currentLine + c);
                    if (!newLine && newWidth < mMaxWidth) {//continue till letter will reach max width
                        currentLine += c;
                        width = newWidth;
                    } else {
                        //add new line
                        result.add(new LineRenderContext(getX(horizontal, width), currentLine, width));
                        currentLine = newLine ? "" : String.valueOf(c);
                        width = newWidth - width;
                    }

                }
                //rest
                currentLine = currentLine.trim();
                if (currentLine.length() > 0) {
                    result.add(new LineRenderContext(getX(horizontal, width), currentLine, mFontMetrics.stringWidth(currentLine)));
                }
            }
            return result;
        }

        private void countVerticalPositions(ArrayList<LineRenderContext> contexts, Gravity vertical) {
            final int len = contexts.size();
            int y = 0;
            switch (vertical) {
                case LEFT_TOP:
                    y = mFontMetrics.getMaxAscent();
                    for (LineRenderContext context1 : contexts) {
                        context1.y = y;
                        y += mLineHeight;
                    }
                    break;
                case CENTER:
                    y = ((mLineHeight + mMaxHeight) >> 1) - mFontMetrics.getMaxDescent();
                    final int offset = ((len - 1) * mLineHeight) >> 1;
                    for (LineRenderContext context : contexts) {
                        context.y = y - offset;
                        y += mLineHeight;
                    }
                    break;
                case RIGHT_BOTTOM:
                    y = mMaxHeight - mFontMetrics.getMaxDescent();
                    for (int i = len - 1; i >= 0; i--) {
                        contexts.get(i).y = y;
                        y -= mLineHeight;
                    }
                    break;
            }
        }

        public int getX(Gravity horizontal, int lineWidth){
            int x = 0;
            switch (horizontal) {
                case LEFT_TOP:      x = 0;                             break;
                case CENTER:        x = (mMaxWidth - lineWidth) >> 1;  break;
                case RIGHT_BOTTOM:  x = mMaxWidth - lineWidth;         break;
            }
            return x;
        }

        private static class LineRenderContext {
            public int x;
            public int y;
            public int width;
            public String text;

            public LineRenderContext(int x, String text, int width) {
                this(x, 0, text, width);
            }

            public LineRenderContext(int x, int y, String text, int width) {
                this.x = x;
                this.y = y;
                this.text = text;
                this.width = width;
            }
        }
    }
}