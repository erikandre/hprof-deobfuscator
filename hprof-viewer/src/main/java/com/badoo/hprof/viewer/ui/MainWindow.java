package com.badoo.hprof.viewer.ui;

import com.badoo.hprof.viewer.model.View;
import com.badoo.hprof.viewer.model.ViewGroup;
import com.badoo.hprof.viewer.rendering.ViewRenderer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

/**
 * Created by Erik Andre on 22/11/15.
 */
public class MainWindow extends JFrame implements TreeSelectionListener, ItemListener {

    private final JSplitPane splitPane;
    private final JTree viewTree;
    private final List<ViewGroup> roots;
    private final ViewRenderer renderer = new ViewRenderer();
    private final JComboBox rootPicker;
    private ViewGroup selectedRoot;
    private ImagePanel imagePanel;
    private View selectedView;

    public MainWindow(List<ViewGroup> roots) {
        super("Hprof Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.roots = roots;

        imagePanel = new ImagePanel();
        viewTree = new JTree(new DefaultMutableTreeNode("Loading..."));
        viewTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        viewTree.addTreeSelectionListener(this);
        JScrollPane treeScroller = new JScrollPane(viewTree);

        rootPicker = new JComboBox(new Vector<Object>(roots));
        rootPicker.addItemListener(this);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(rootPicker, BorderLayout.NORTH);
        leftPanel.add(treeScroller, BorderLayout.CENTER);

        // Split pane for the tree and image views
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(imagePanel);
        add(splitPane);
        setVisible(true);
        selectedRoot = roots.get(0);
        update();
    }

    private void update() {
        showViewTree();
        updateImage(true);
    }

    public void showViewTree() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(selectedRoot);
        addChildViews(rootNode, selectedRoot);
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        viewTree.setModel(model);
    }

    private void addChildViews(DefaultMutableTreeNode parent, ViewGroup group) {
        for (View view : group.getChildren()) {
            if (view instanceof  ViewGroup) {
                DefaultMutableTreeNode newParent = new DefaultMutableTreeNode(view);
                parent.add(newParent);
                addChildViews(newParent, (ViewGroup) view);
            }
            else {
                parent.add(new DefaultMutableTreeNode(view));
            }
        }
    }

    public void updateImage(boolean resize) {
        BufferedImage image = renderer.renderViews(selectedRoot);
        imagePanel.setImage(image);
        if (resize) {
            Dimension size = new Dimension((int) (imagePanel.getPreferredSize().getWidth() + rootPicker.getPreferredSize().getWidth() + 15), (int) imagePanel.getPreferredSize().getHeight() + 25);
            setPreferredSize(size);
            pack();
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent event) {
        if (selectedView != null) {
            selectedView.setSelected(false);
        }
        if (event.getNewLeadSelectionPath() == null) {
            selectedView = null;
            return;
        }
        DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) event.getNewLeadSelectionPath().getLastPathComponent();
        View newView = (View) newNode.getUserObject();
        newView.setSelected(true);
        selectedView = newView;
        updateImage(false);
    }

    @Override
    public void itemStateChanged(ItemEvent itemEvent) {
        selectedRoot = (ViewGroup) itemEvent.getItem();
        update();
    }
}