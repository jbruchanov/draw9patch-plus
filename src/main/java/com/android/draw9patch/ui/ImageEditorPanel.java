/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.draw9patch.ui;

import com.android.draw9patch.graphics.GraphicsUtilities;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class ImageEditorPanel extends JPanel {
    private static final String EXTENSION_9PATCH = ".9.png";

    private String name;
    private BufferedImage image;
    private boolean is9Patch;

    private ImageViewer viewer;
    private StretchesViewer stretchesViewer;
    private JLabel xLabel;
    private JLabel yLabel;

    private TexturePaint texture;
    private JSlider zoomSlider;
    //JtS
    private boolean mWheelToZoom = true;

    public ImageEditorPanel(MainFrame mainFrame, BufferedImage image, String name) {
        this.image = image;
        this.name = name;

        if (mainFrame != null) {
            setTransferHandler(new ImageTransferHandler(mainFrame));
        }

        setOpaque(false);
        setLayout(new BorderLayout());

        is9Patch = name.endsWith(EXTENSION_9PATCH);
        if (!is9Patch) {
            this.image = convertTo9Patch(image);
            this.name = name.substring(0, name.lastIndexOf('.')) + ".9.png";
        } else {
            ensure9Patch(image);
        }

        loadSupport();
        buildImageViewer();
        buildStatusPanel();

        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                // allow the image viewer to set the optimal zoom level and ensure that the
                // zoom slider's setting is in sync with the image viewer's zoom
                removeAncestorListener(this);
                synchronizeImageViewerZoomLevel();
            }
        });
    }

    private void synchronizeImageViewerZoomLevel() {
        zoomSlider.setValue(viewer.getZoom());
    }

    public ImageViewer getViewer() {
        return viewer;
    }

    private void loadSupport() {
        try {
            URL resource = getClass().getResource("/images/checker.png");
            BufferedImage checker = GraphicsUtilities.loadCompatibleImage(resource);
            texture = new TexturePaint(checker, new Rectangle2D.Double(0, 0,
                    checker.getWidth(), checker.getHeight()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void buildImageViewer() {
        viewer = new ImageViewer(this, texture, image, new ImageViewer.StatusBar() {
            @Override
            public void setPointerLocation(int x, int y) {
                //JtS
                xLabel.setText(String.format("%d px   -%d px", x, (image.getWidth() - x)));
                yLabel.setText(String.format("%d px   -%d px", y, (image.getHeight() - y)));
            }
        });

        JSplitPane splitter = new JSplitPane();
        splitter.setContinuousLayout(true);
        splitter.setResizeWeight(0.8);
        splitter.setBorder(null);

        JScrollPane scroller = new JScrollPane(viewer);
        scroller.setOpaque(false);
        scroller.setBorder(null);
        scroller.getViewport().setBorder(null);
        scroller.getViewport().setOpaque(false);

        //JtS
        if (mWheelToZoom) {
            scroller.setWheelScrollingEnabled(false);
            scroller.addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
                    int wheelRot = mouseWheelEvent.getWheelRotation();
                    zoomSlider.setValue(zoomSlider.getValue() + wheelRot);
                }
            });
        }

        splitter.setLeftComponent(scroller);
        splitter.setRightComponent(buildStretchesViewer());

        add(splitter);
    }

    private JComponent buildStretchesViewer() {
        stretchesViewer = new StretchesViewer(this, viewer, texture);
        JScrollPane scroller = new JScrollPane(stretchesViewer);
        scroller.setBorder(null);
        scroller.getViewport().setBorder(null);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return scroller;
    }

    private void buildStatusPanel() {
        JPanel status = new JPanel(new GridBagLayout());

        JLabel label = new JLabel();
        label.setText("Zoom: ");
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 6, 0, 0), 0, 0));

        label = new JLabel();
        label.setText("100%");
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(1, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        zoomSlider = new JSlider(ImageViewer.MIN_ZOOM, ImageViewer.MAX_ZOOM,
                ImageViewer.DEFAULT_ZOOM);
        zoomSlider.setMinimumSize(new Dimension(100, (int) zoomSlider.getMinimumSize().getHeight()));
        zoomSlider.setSnapToTicks(true);
        zoomSlider.putClientProperty("JComponent.sizeVariant", "small");
        zoomSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent evt) {
                viewer.setZoom(((JSlider) evt.getSource()).getValue());
            }
        });
        status.add(zoomSlider, new GridBagConstraints(2, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        JLabel maxZoomLabel = new JLabel();
        maxZoomLabel.putClientProperty("JComponent.sizeVariant", "small");
        maxZoomLabel.setText("800%");
        status.add(maxZoomLabel, new GridBagConstraints(3, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        label = new JLabel();
        label.setText("Patch scale: ");
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 6, 0, 0), 0, 0));

        label = new JLabel();
        label.setText("2x");
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(1, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        JSlider jSlider = new JSlider(200, 600, (int) (StretchesViewer.DEFAULT_SCALE * 100.0f));
        jSlider.setMinimumSize(new Dimension(100, (int) jSlider.getMinimumSize().getHeight()));
        jSlider.setSnapToTicks(true);
        jSlider.putClientProperty("JComponent.sizeVariant", "small");
        jSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent evt) {
                stretchesViewer.setScale(((JSlider) evt.getSource()).getValue() / 100.0f);
            }
        });
        status.add(jSlider, new GridBagConstraints(2, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        maxZoomLabel = new JLabel();
        maxZoomLabel.putClientProperty("JComponent.sizeVariant", "small");
        maxZoomLabel.setText("6x");
        status.add(maxZoomLabel, new GridBagConstraints(3, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        JCheckBox showLock = new JCheckBox("Show lock");
        showLock.setOpaque(false);
        showLock.setSelected(false);
        showLock.putClientProperty("JComponent.sizeVariant", "small");
        showLock.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                viewer.setLockVisible(((JCheckBox) event.getSource()).isSelected());
            }
        });
        status.add(showLock, new GridBagConstraints(4, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 12, 0, 0), 0, 0));

        JCheckBox showPatches = new JCheckBox("Show patches");
        showPatches.setOpaque(false);
        showPatches.putClientProperty("JComponent.sizeVariant", "small");
        showPatches.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                viewer.setPatchesVisible(((JCheckBox) event.getSource()).isSelected());
            }
        });
        status.add(showPatches, new GridBagConstraints(4, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 12, 0, 0), 0, 0));

        JCheckBox showPadding = new JCheckBox("Show content");
        showPadding.setOpaque(false);
        showPadding.putClientProperty("JComponent.sizeVariant", "small");
        showPadding.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                boolean isChecked = ((JCheckBox) event.getSource()).isSelected();
                stretchesViewer.setPaddingVisible(isChecked);
            }
        });

        status.add(showPadding, new GridBagConstraints(5, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));


        JCheckBox showText = new JCheckBox("Show text");
        showText.setOpaque(false);
        showText.putClientProperty("JComponent.sizeVariant", "small");

        final JTextField text = new JTextField("", 100);
        showText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                boolean isChecked = ((JCheckBox) event.getSource()).isSelected();
                stretchesViewer.setTextVisible(isChecked);
                text.setEnabled(isChecked);
            }
        });

        status.add(showText, new GridBagConstraints(0, 2, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));


        text.setEnabled(false);
        text.putClientProperty("JComponent.sizeVariant", "small");
        text.addKeyListener(new KeyListener() {
            @Override public void keyTyped(KeyEvent keyEvent) { }
            @Override public void keyPressed(KeyEvent keyEvent) { }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
                String newText = text.getText();
                stretchesViewer.setTextValue(newText);
            }
        });
        text.setMinimumSize(new Dimension(200, (int) text.getMinimumSize().getHeight()));
        status.add(text, new GridBagConstraints(1, 2, 3, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 2, 0, 0), 0, 0));


        Font[] sysFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        final Font[] fonts = new Font[sysFonts.length + 2];
        System.arraycopy(sysFonts, 0, fonts, 2, sysFonts.length);
        try {
            fonts[0] = Font.createFont(Font.TRUETYPE_FONT, getClass().getResource("/fonts/DroidSans.ttf").openStream());
            fonts[1] = Font.createFont(Font.TRUETYPE_FONT, getClass().getResource("/fonts/DroidSans-Bold.ttf").openStream());
            stretchesViewer.setFont(new Font(fonts[0].getName(), Font.PLAIN, stretchesViewer.getFontSize()));
        } catch (Exception e) {
            throw new IllegalStateException("unable to load droid fonts");
        }

        final JComboBox fontsComboBox = new JComboBox(new DefaultComboBoxModel(fonts));
        fontsComboBox.setSelectedIndex(0);
        fontsComboBox.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean b, boolean b2) {
                Component c = super.getListCellRendererComponent(jList, o, i, b, b2);
                setText(o != null ? ((Font)o).getName() : "");
                return c;
            }
        });
        fontsComboBox.setMinimumSize(new Dimension(150, (int) fontsComboBox.getMinimumSize().getHeight()));
        fontsComboBox.setOpaque(false);
        fontsComboBox.putClientProperty("JComponent.sizeVariant", "small");
        fontsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Font f = (Font) fontsComboBox.getSelectedItem();
                int type = f == fonts[1] ? Font.BOLD : Font.PLAIN;
                if (f != null) {
                    f = new Font(f.getName(), type, stretchesViewer.getFontSize());
                    stretchesViewer.setFont(f);
                }
            }
        });

        status.add(fontsComboBox, new GridBagConstraints(4, 2, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 2, 0, 0), 0, 0));


        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(24, 1, 256, 1));
        spinner.setOpaque(false);
        spinner.putClientProperty("JComponent.sizeVariant", "small");
        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                int value = (Integer) spinner.getModel().getValue();
                stretchesViewer.setFontSize(value);
            }
        });

        status.add(spinner, new GridBagConstraints(5, 2, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 2, 0, 0), 0, 0));

        final JComboBox vGravityCombo = new JComboBox(new DefaultComboBoxModel(new String[] {"Top", "Center", "Bottom"}));
        vGravityCombo.setSelectedIndex(0);
        vGravityCombo.setOpaque(false);
        vGravityCombo.putClientProperty("JComponent.sizeVariant", "small");
        vGravityCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                stretchesViewer.setTextVerticalGravity(vGravityCombo.getSelectedIndex());
            }
        });

        status.add(vGravityCombo, new GridBagConstraints(6, 2, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 2, 0, 0), 0, 0));

        final JComboBox hGravityCombo = new JComboBox(new DefaultComboBoxModel(new String[] {"Left", "Center", "Right"}));
        hGravityCombo.setSelectedIndex(0);
        hGravityCombo.setOpaque(false);
        hGravityCombo.putClientProperty("JComponent.sizeVariant", "small");
        hGravityCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                stretchesViewer.setTextHorizontalGravity(hGravityCombo.getSelectedIndex());
            }
        });

        status.add(hGravityCombo, new GridBagConstraints(7, 2, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 2, 0, 0), 0, 0));



        status.add(Box.createHorizontalGlue(), new GridBagConstraints(10, 0, 1, 1, 1.0f, 1.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

        label = new JLabel("X: ");
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(10, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        xLabel = new JLabel("0px");
        xLabel.putClientProperty("JComponent.sizeVariant", "small");
        status.add(xLabel, new GridBagConstraints(11, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 6), 0, 0));

        label = new JLabel("Y: ");
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(10, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        yLabel = new JLabel("0px");
        yLabel.putClientProperty("JComponent.sizeVariant", "small");
        status.add(yLabel, new GridBagConstraints(11, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 6), 0, 0));

        add(status, BorderLayout.SOUTH);
    }

    private static void ensure9Patch(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int i = 0; i < width; i++) {
            int pixel = image.getRGB(i, 0);
            if (pixel != 0 && pixel != PatchInfo.BLACK_TICK && pixel != PatchInfo.RED_TICK) {
                image.setRGB(i, 0, 0);
            }
            pixel = image.getRGB(i, height - 1);
            if (pixel != 0 && pixel != PatchInfo.BLACK_TICK && pixel != PatchInfo.RED_TICK) {
                image.setRGB(i, height - 1, 0);
            }
        }
        for (int i = 0; i < height; i++) {
            int pixel = image.getRGB(0, i);
            if (pixel != 0 && pixel != PatchInfo.BLACK_TICK && pixel != PatchInfo.RED_TICK) {
                image.setRGB(0, i, 0);
            }
            pixel = image.getRGB(width - 1, i);
            if (pixel != 0 && pixel != PatchInfo.BLACK_TICK && pixel != PatchInfo.RED_TICK) {
                image.setRGB(width - 1, i, 0);
            }
        }
    }

    private static BufferedImage convertTo9Patch(BufferedImage image) {
        BufferedImage buffer = GraphicsUtilities.createTranslucentCompatibleImage(
                image.getWidth() + 2, image.getHeight() + 2);

        Graphics2D g2 = buffer.createGraphics();
        g2.drawImage(image, 1, 1, null);
        g2.dispose();

        return buffer;
    }

    File chooseSaveFile() {
        if (is9Patch) {
            return new File(name);
        } else {
            JFileChooser chooser = new JFileChooser(
                    name.substring(0, name.lastIndexOf(File.separatorChar)));
            chooser.setFileFilter(new PngFileFilter());
            int choice = chooser.showSaveDialog(this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getAbsolutePath().endsWith(EXTENSION_9PATCH)) {
                    String path = file.getAbsolutePath();
                    if (path.endsWith(".png")) {
                        path = path.substring(0, path.lastIndexOf(".png")) + EXTENSION_9PATCH;
                    } else {
                        path = path + EXTENSION_9PATCH;
                    }
                    name = path;
                    is9Patch = true;
                    return new File(path);
                }
                is9Patch = true;
                return file;
            }
        }
        return null;
    }

    RenderedImage getImage() {
        return image;
    }

    public void dispose() {
        if (viewer != null) {
            viewer.dispose();
        }
    }
}
