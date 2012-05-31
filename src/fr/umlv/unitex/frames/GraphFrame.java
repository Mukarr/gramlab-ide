/*
 * Unitex
 *
 * Copyright (C) 2001-2012 Université Paris-Est Marne-la-Vallée <unitex@univ-mlv.fr>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.
 *
 */
package fr.umlv.unitex.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import fr.umlv.unitex.DropTargetManager;
import fr.umlv.unitex.MyCursors;
import fr.umlv.unitex.config.Config;
import fr.umlv.unitex.config.ConfigManager;
import fr.umlv.unitex.config.Preferences;
import fr.umlv.unitex.diff.GraphDecorator;
import fr.umlv.unitex.files.FileUtil;
import fr.umlv.unitex.graphrendering.GenericGraphBox;
import fr.umlv.unitex.graphrendering.GraphBox;
import fr.umlv.unitex.graphrendering.GraphicalZone;
import fr.umlv.unitex.graphrendering.MultipleSelection;
import fr.umlv.unitex.graphrendering.TextField;
import fr.umlv.unitex.graphtools.Dependancies;
import fr.umlv.unitex.graphtools.GraphCall;
import fr.umlv.unitex.grf.GraphPresentationInfo;
import fr.umlv.unitex.io.GraphIO;
import fr.umlv.unitex.io.SVG;
import fr.umlv.unitex.listeners.GraphListener;
import fr.umlv.unitex.process.Launcher;
import fr.umlv.unitex.process.ToDo;
import fr.umlv.unitex.process.commands.Grf2Fst2Command;
import fr.umlv.unitex.process.commands.GrfDiffCommand;
import fr.umlv.unitex.svn.ConflictSolvedListener;
import fr.umlv.unitex.svn.SvnConflict;

/**
 * This class describes a frame used to display and edit a graph.
 * 
 * @author Sébastien Paumier
 */
public class GraphFrame extends KeyedInternalFrame<File> {
	private static int openFrameCount = 0;
	private static final int offset = 30;
	private TextField boxContentEditor;
	final GraphicalZone graphicalZone;
	DefaultListModel grfListModel = new DefaultListModel();
	JList grfList = new JList(grfListModel);
	JScrollPane grfListScroll = new JScrollPane(grfList);
	JPanel grfListPanel;
	JLabel grfListLabel = new JLabel();

	public GraphicalZone getGraphicalZone() {
		return graphicalZone;
	}

	/**
	 * The graph file
	 */
	File grf;
	long lastModification;
	/**
	 * Indicates if the graph must be saved
	 */
	boolean modified = false;
	private final UndoManager manager;
	private JButton redoButton;
	private JButton undoButton;
	public final JScrollPane scroll;
	private boolean nonEmptyGraph = false;

	public boolean isNonEmptyGraph() {
		return nonEmptyGraph;
	}

	private final Timer autoRefresh = new Timer(2000, new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (grf == null) {
				return;
			}
			if (!grf.exists()) {
				/* Case of a graph that has been removed */
				final Timer t = (Timer) e.getSource();
				t.stop();
				final String[] options = { "Yes", "No" };
				final int n = JOptionPane.showOptionDialog(GraphFrame.this,
						"The file "+grf.getAbsolutePath()+" does\n"
						+"not exist anymore on disk. Do you want to close the frame?", "", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				if (n == 1) {
					return;
				}
				setModified(false);
				doDefaultCloseAction();
				return;
			}
			final SvnConflict conflict = SvnConflict.getConflict(grf);
			if (conflict != null) {
				final Timer t = (Timer) e.getSource();
				t.stop();
				handleSvnConflict(conflict, t);
				return;
			}
			if (grf.lastModified() > lastModification) {
				final int ret = JOptionPane
						.showConfirmDialog(
								GraphFrame.this,
								"Graph has changed on disk. Do you want to reload it ?",
								"", JOptionPane.YES_NO_OPTION);
				if (ret == JOptionPane.YES_OPTION) {
					final GraphIO g = GraphIO.loadGraph(getGraph(), false,
							false);
					graphicalZone.refresh(g);
					setModified(false);
					lastModification = grf.lastModified();
				} else {
					/*
					 * We don't want to be asked again until another
					 * modification
					 */
					lastModification = grf.lastModified();
				}
			}
		}
	});
	private final JPanel mainPanel;
	private final JPanel actualMainPanel;
	private JPanel svnPanel;
	/**
	 * Component used to listen frame changes. It is used to adapt the zoom
	 * factor to the frame's dimensions when the zoom mode is "Fit in Windows"
	 */
	public ComponentListener compListener = null;
	/**
	 * The frame's tool bar that contains icons
	 */
	private JToolBar myToolBar;

	/**
	 * Constructs a new <code>GraphFrame</code>
	 * 
	 * @param nonEmpty
	 *            indicates if the graph is non empty
	 */
	GraphFrame(GraphIO g) {
		super("", true, true, true, true);
		DropTargetManager.getDropTarget().newDropTarget(this);
		openFrameCount++;
		setTitle("Graph");
		actualMainPanel = new JPanel(new BorderLayout());
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		final JPanel top = new JPanel(new BorderLayout());
		top.add(buildTextPanel(), BorderLayout.NORTH);
		graphicalZone = new GraphicalZone(g, getBoxContentEditor(), this, null);
		graphicalZone.addGraphListener(new GraphListener() {
			public void graphChanged(boolean m) {
				repaint();
				if (m)
					setModified(true);
			}
		});
		manager = new UndoManager();
		getManager().setLimit(-1);
		graphicalZone.addUndoableEditListener(getManager());
		final GraphPresentationInfo info = getGraphPresentationInfo();
		scroll = new JScrollPane(graphicalZone);
		scroll.getHorizontalScrollBar().setUnitIncrement(20);
		scroll.getVerticalScrollBar().setUnitIncrement(20);
		scroll.setPreferredSize(new Dimension(1188, 840));
		top.add(scroll, BorderLayout.CENTER);
		getBoxContentEditor().setFont(info.getInput().getFont());
		createToolBar(info.getIconBarPosition());
		if (!(info.getIconBarPosition().equals(Preferences.NO_ICON_BAR))) {
			mainPanel.add(myToolBar, info.getIconBarPosition());
		}
		mainPanel.add(top, BorderLayout.CENTER);
		getActualMainPanel().add(mainPanel, BorderLayout.CENTER);
		setContentPane(getActualMainPanel());
		pack();
		addInternalFrameListener(new MyInternalFrameListener());
		setBounds(offset * (openFrameCount % 6), offset * (openFrameCount % 6),
				1000, 550);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		getBoxContentEditor().setFont(info.getInput().getFont());
		if (g != null) {
			setGraph(g.getGrf());
		}
		/*
		 * Some loading operations may have set the modified flag, so we reset
		 * it
		 */
		setModified(false);
		grfList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean selected,
					boolean cellHasFocus) {
				final GraphCall c = (GraphCall) value;
				String s = FileUtil.getRelativePath(getGraph(), c.getGrf());
				if (s.endsWith(".grf")) {
					s = s.substring(0, s.lastIndexOf('.'));
				}
				if (!c.isDirect()) {
					s = "-> " + s;
				}
				super.getListCellRendererComponent(list, s, index, selected,
						cellHasFocus);
				if (!c.getGrf().exists()) {
					setForeground(Color.RED);
				} else {
					if (!c.isUseful()) {
						setForeground(Color.ORANGE);
					}
				}
				return this;
			}
		});
		grfList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		grfList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				final int n = grfList.getSelectedIndex();
				if (n == -1)
					return;
				final GraphCall c = (GraphCall) grfListModel.get(n);
				final File f = c.getGrf();
				InternalFrameManager.getManager(f).newGraphFrame(f);
			}
		});
		grfListPanel = new JPanel(new BorderLayout());
		final JPanel up = new JPanel(new BorderLayout());
		up.add(grfListLabel, BorderLayout.CENTER);
		final JButton closeButton = new JButton(MyCursors.closeIcon);
		closeButton.setPreferredSize(new Dimension(MyCursors.closeIcon
				.getIconWidth() + 8, MyCursors.closeIcon.getIconHeight() + 8));
		closeButton.setBorderPainted(false);
		closeButton.setOpaque(false);
		closeButton.setContentAreaFilled(false);
		closeButton.setFocusPainted(false);
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getActualMainPanel().remove(grfListPanel);
				getActualMainPanel().revalidate();
				getActualMainPanel().repaint();
			}
		});
		up.add(closeButton, BorderLayout.EAST);
		up.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		grfListPanel.add(up, BorderLayout.NORTH);
		grfListPanel.add(grfListScroll, BorderLayout.CENTER);
		grfListPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		getAutoRefresh().setInitialDelay(5);
		getAutoRefresh().start();
	}

	protected void handleSvnConflict(final SvnConflict conflict, final Timer t) {
		setGraph(conflict.mine);
		final GraphIO g = GraphIO.loadGraph(conflict.mine, false, false);
		graphicalZone.refresh(g);
		setModified(false);
		setSvnPanel(createSvnPanel(conflict));
		getActualMainPanel().add(getSvnPanel(), BorderLayout.NORTH);
		conflict.addConflictSolvedListener(new ConflictSolvedListener() {
			public void conflictSolved() {
				getActualMainPanel().remove(getSvnPanel());
				setSvnPanel(null);
				setGraph(conflict.grf);
				final GraphIO g2 = GraphIO.loadGraph(conflict.grf, false, false);
				graphicalZone.refresh(g2);
				setModified(false);
				lastModification = grf.lastModified();
				getActualMainPanel().revalidate();
				getActualMainPanel().repaint();
				t.start();
			}
		});
		getActualMainPanel().revalidate();
		getActualMainPanel().repaint();
	}

	private JPanel createSvnPanel(final SvnConflict conflict) {
		final JPanel main = new JPanel(new GridLayout(2, 1));
		final TitledBorder border = BorderFactory
				.createTitledBorder("Svn conflict detected on graph "
						+ conflict.grf.getAbsolutePath());
		border.setTitleColor(Color.RED);
		main.setBorder(border);
		final JPanel p = new JPanel(null);
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		final JButton showBase = new JButton("Show base (r"
				+ conflict.baseNumber + ")");
		showBase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager.getManager(conflict.base).newGraphFrame(
						conflict.base);
			}
		});
		p.add(showBase);
		final JButton showOther = new JButton("Show conflicting grf (r"
				+ conflict.otherNumber + ")");
		showOther.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager.getManager(conflict.other).newGraphFrame(
						conflict.other);
			}
		});
		p.add(showOther);
		final JButton diff = new JButton("Show diff with r"
				+ conflict.otherNumber);
		diff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final File diffResult = new File(ConfigManager.getManager().getCurrentLanguageDir(), "diff");
				final GrfDiffCommand cmd = new GrfDiffCommand().files(
						conflict.mine, conflict.other).output(diffResult);
				Launcher.exec(cmd, true, new ShowDiffDo(conflict.mine,
						conflict.other, diffResult));
			}
		});
		p.add(diff);
		main.add(p);
		final JPanel p2 = new JPanel(null);
		p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
		p2.add(new JLabel("Conflict resolution:  "));
		final JButton useMine = new JButton("Use mine");
		useMine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				conflict.useMine();
			}
		});
		p2.add(useMine);
		final JButton useOther = new JButton("Use other");
		useOther.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				conflict.useOther();
			}
		});
		p2.add(useOther);
		final JButton merge = new JButton("Try to merge");
		merge.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (modified) {
					JOptionPane.showMessageDialog(null,
							"Save graph before trying to merge", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (!conflict.merge()) {
					JOptionPane.showMessageDialog(null,
							"Conflicts remain, cannot merge files",
							"Merge failed", JOptionPane.ERROR_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(null, "Merge successed", "",
							JOptionPane.PLAIN_MESSAGE);
				}
			}
		});
		p2.add(merge);
		main.add(p2);
		return main;
	}

	private void createToolBar(String iconBarPosition) {
		myToolBar = new JToolBar("Tools");
		if (iconBarPosition.equals(Preferences.ICON_BAR_WEST)
				|| iconBarPosition.equals(Preferences.ICON_BAR_EAST)) {
			myToolBar.setOrientation(SwingConstants.VERTICAL);
		} else {
			myToolBar.setOrientation(SwingConstants.HORIZONTAL);
		}
		myToolBar.setMargin(new Insets(0, 0, 0, 0));
		final JButton save = new JButton(MyCursors.saveIcon);
		save.setMaximumSize(new Dimension(36, 36));
		save.setMinimumSize(new Dimension(36, 36));
		save.setPreferredSize(new Dimension(36, 36));
		final JButton compile = new JButton(MyCursors.compilationIcon);
		compile.setMaximumSize(new Dimension(36, 36));
		compile.setMinimumSize(new Dimension(36, 36));
		compile.setPreferredSize(new Dimension(36, 36));
		final JButton copy = new JButton(MyCursors.copyIcon);
		final JButton cut = new JButton(MyCursors.cutIcon);
		final JButton paste = new JButton(MyCursors.pasteIcon);
		copy.setMaximumSize(new Dimension(36, 36));
		cut.setMaximumSize(new Dimension(36, 36));
		paste.setMaximumSize(new Dimension(36, 36));
		copy.setMinimumSize(new Dimension(36, 36));
		cut.setMinimumSize(new Dimension(36, 36));
		paste.setMinimumSize(new Dimension(36, 36));
		copy.setPreferredSize(new Dimension(36, 36));
		cut.setPreferredSize(new Dimension(36, 36));
		paste.setPreferredSize(new Dimension(36, 36));
		redoButton = new JButton(MyCursors.redoIcon);
		undoButton = new JButton(MyCursors.undoIcon);
		redoButton.setToolTipText("Redo");
		undoButton.setToolTipText("Undo");
		redoButton.addActionListener(new RedoIt());
		redoButton.setMaximumSize(new Dimension(36, 36));
		redoButton.setMinimumSize(new Dimension(36, 36));
		redoButton.setPreferredSize(new Dimension(36, 36));
		undoButton.addActionListener(new UndoIt());
		undoButton.setMaximumSize(new Dimension(36, 36));
		undoButton.setMinimumSize(new Dimension(36, 36));
		undoButton.setPreferredSize(new Dimension(36, 36));
		final JToggleButton normal = new JToggleButton(MyCursors.arrowIcon);
		normal.setMaximumSize(new Dimension(36, 36));
		normal.setMinimumSize(new Dimension(36, 36));
		normal.setPreferredSize(new Dimension(36, 36));
		final JToggleButton create = new JToggleButton(
				MyCursors.createBoxesIcon);
		create.setMaximumSize(new Dimension(36, 36));
		create.setMinimumSize(new Dimension(36, 36));
		create.setPreferredSize(new Dimension(36, 36));
		final JToggleButton kill = new JToggleButton(MyCursors.killBoxesIcon);
		kill.setMaximumSize(new Dimension(36, 36));
		kill.setMinimumSize(new Dimension(36, 36));
		kill.setPreferredSize(new Dimension(36, 36));
		final JToggleButton link = new JToggleButton(MyCursors.linkBoxesIcon);
		link.setMaximumSize(new Dimension(36, 36));
		link.setMinimumSize(new Dimension(36, 36));
		link.setPreferredSize(new Dimension(36, 36));
		final JToggleButton reverseLink = new JToggleButton(
				MyCursors.reverseLinkBoxesIcon);
		reverseLink.setMaximumSize(new Dimension(36, 36));
		reverseLink.setMinimumSize(new Dimension(36, 36));
		reverseLink.setPreferredSize(new Dimension(36, 36));
		final JToggleButton openSubgraph = new JToggleButton(
				MyCursors.openSubgraphIcon);
		openSubgraph.setMaximumSize(new Dimension(36, 36));
		openSubgraph.setMinimumSize(new Dimension(36, 36));
		openSubgraph.setPreferredSize(new Dimension(36, 36));
		final JButton configuration = new JButton(MyCursors.configurationIcon);
		configuration.setMaximumSize(new Dimension(36, 36));
		configuration.setMinimumSize(new Dimension(36, 36));
		configuration.setPreferredSize(new Dimension(36, 36));
		save.setToolTipText("Save graph");
		compile.setToolTipText("Compile graph");
		copy.setToolTipText("Copy");
		cut.setToolTipText("Cut");
		paste.setToolTipText("Paste");
		normal.setToolTipText("Normal editing mode");
		create.setToolTipText("Create a new box");
		kill.setToolTipText("Remove a box");
		link.setToolTipText("Link boxes");
		reverseLink.setToolTipText("Reversed link between boxes");
		openSubgraph.setToolTipText("Open a sub-graph");
		configuration.setToolTipText("Graph configuration");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveGraph();
			}
		});
		compile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						compileGraph();
					}
				});
			}
		});
		copy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final ActionEvent E = e;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						((TextField) graphicalZone.text).getSpecialCopy()
								.actionPerformed(E);
						repaint();
					}
				});
			}
		});
		cut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final ActionEvent E = e;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						((TextField) graphicalZone.text).getCut()
								.actionPerformed(E);
						repaint();
					}
				});
			}
		});
		paste.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final ActionEvent E = e;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						((TextField) graphicalZone.text).getSpecialPaste()
								.actionPerformed(E);
						repaint();
					}
				});
			}
		});
		normal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graphicalZone.setCursor(MyCursors.normalCursor);
				graphicalZone.EDITING_MODE = MyCursors.NORMAL;
				repaint();
			}
		});
		create.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graphicalZone.setCursor(MyCursors.createBoxesCursor);
				graphicalZone.EDITING_MODE = MyCursors.CREATE_BOXES;
				repaint();
			}
		});
		kill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graphicalZone.setCursor(MyCursors.killBoxesCursor);
				graphicalZone.EDITING_MODE = MyCursors.KILL_BOXES;
				graphicalZone.unSelectAllBoxes();
				graphicalZone.validateContent();
			}
		});
		link.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graphicalZone.setCursor(MyCursors.linkBoxesCursor);
				graphicalZone.EDITING_MODE = MyCursors.LINK_BOXES;
				repaint();
			}
		});
		reverseLink.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graphicalZone.setCursor(MyCursors.reverseLinkBoxesCursor);
				graphicalZone.EDITING_MODE = MyCursors.REVERSE_LINK_BOXES;
				repaint();
			}
		});
		openSubgraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graphicalZone.setCursor(MyCursors.openSubgraphCursor);
				graphicalZone.EDITING_MODE = MyCursors.OPEN_SUBGRAPH;
				graphicalZone.validateContent();
			}
		});
		configuration.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						final GraphPresentationInfo info = InternalFrameManager
								.getManager(grf).newGraphPresentationDialog(
										getGraphPresentationInfo(), true);
						if (info != null) {
							setGraphPresentationInfo(info);
						}
					}
				});
			}
		});
		final ButtonGroup bg = new ButtonGroup();
		bg.add(normal);
		bg.add(create);
		bg.add(kill);
		bg.add(link);
		bg.add(reverseLink);
		bg.add(openSubgraph);
		normal.setSelected(true);
		myToolBar.add(save);
		myToolBar.add(compile);
		myToolBar.addSeparator();
		myToolBar.addSeparator();
		myToolBar.add(copy);
		myToolBar.add(cut);
		myToolBar.add(paste);
		myToolBar.add(redoButton);
		myToolBar.add(undoButton);
		myToolBar.addSeparator();
		myToolBar.addSeparator();
		myToolBar.add(normal);
		myToolBar.add(create);
		myToolBar.add(kill);
		myToolBar.add(link);
		myToolBar.add(reverseLink);
		myToolBar.add(openSubgraph);
		myToolBar.addSeparator();
		myToolBar.addSeparator();
		myToolBar.add(configuration);
		final JButton calledGrf = new JButton(MyCursors.calledGrfIcon);
		calledGrf.setMaximumSize(new Dimension(36, 36));
		calledGrf.setMinimumSize(new Dimension(36, 36));
		calledGrf.setPreferredSize(new Dimension(36, 36));
		calledGrf.setToolTipText("Subgraphs called from this one");
		calledGrf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showGraphDependencies(true);
			}
		});
		myToolBar.add(calledGrf);
		final JButton callersGrf = new JButton(MyCursors.callersGrfIcon);
		callersGrf.setMaximumSize(new Dimension(36, 36));
		callersGrf.setMinimumSize(new Dimension(36, 36));
		callersGrf.setPreferredSize(new Dimension(36, 36));
		callersGrf.setToolTipText("Graphs that call this one");
		callersGrf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showGraphDependencies(false);
			}
		});
		myToolBar.add(callersGrf);
		final JButton refresh = new JButton(MyCursors.refreshIcon);
		refresh.setMaximumSize(new Dimension(36, 36));
		refresh.setMinimumSize(new Dimension(36, 36));
		refresh.setPreferredSize(new Dimension(36, 36));
		refresh.setToolTipText("Reload graph from disk");
		refresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final GraphIO g = GraphIO.loadGraph(getGraph(), false, false);
				graphicalZone.refresh(g);
				setModified(false);
			}
		});
		myToolBar.add(refresh);
		final JButton diff = new JButton(MyCursors.diffIcon);
		diff.setMaximumSize(new Dimension(36, 36));
		diff.setMinimumSize(new Dimension(36, 36));
		diff.setPreferredSize(new Dimension(36, 36));
		diff.setToolTipText("Compare with another graph");
		diff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (grf == null) {
					JOptionPane
							.showMessageDialog(
									null,
									"Cannot compare a graph with no name, save it first",
									"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				final JFileChooser fc = Config.getGraphDiffDialogBox(grf);
				final int returnVal = fc.showOpenDialog(GraphFrame.this);
				if (returnVal != JFileChooser.APPROVE_OPTION) {
					return;
				}
				final File file = fc.getSelectedFile();
				if (file == null || !file.exists())
					return;
				final File diffResult = new File(grf.getParent(), "diff");
				final GrfDiffCommand cmd = new GrfDiffCommand()
						.files(grf, file).output(diffResult);
				Launcher.exec(cmd, true, new ShowDiffDo(grf, file, diffResult));
			}
		});
		myToolBar.add(diff);
		myToolBar.addSeparator();
		final JButton inputVar = new JButton(graphicalZone
				.getSurroundWithInputVarAction());
		inputVar.setHideActionText(true);
		inputVar.setText("( )");
		inputVar.setMaximumSize(new Dimension(36, 36));
		inputVar.setMinimumSize(new Dimension(36, 36));
		inputVar.setPreferredSize(new Dimension(36, 36));
		inputVar.setForeground(Color.RED);
		myToolBar.add(inputVar);
		final JButton outputVar = new JButton(graphicalZone
				.getSurroundWithOutputVarAction());
		outputVar.setHideActionText(true);
		outputVar.setText("( )");
		outputVar.setMaximumSize(new Dimension(36, 36));
		outputVar.setMinimumSize(new Dimension(36, 36));
		outputVar.setPreferredSize(new Dimension(36, 36));
		outputVar.setForeground(Color.BLUE);
		myToolBar.add(outputVar);
		final JButton morpho = new JButton(graphicalZone
				.getSurroundWithMorphologicalModeAction());
		morpho.setHideActionText(true);
		morpho.setText("< >");
		morpho.setMaximumSize(new Dimension(36, 36));
		morpho.setMinimumSize(new Dimension(36, 36));
		morpho.setPreferredSize(new Dimension(36, 36));
		morpho.setForeground(new Color(0xC4, 0x4F, 0xD0));
		myToolBar.add(morpho);
		final JButton left = new JButton(graphicalZone
				.getSurroundWithLeftContextAction());
		left.setHideActionText(true);
		left.setText("$*");
		left.setMaximumSize(new Dimension(36, 36));
		left.setMinimumSize(new Dimension(36, 36));
		left.setPreferredSize(new Dimension(36, 36));
		left.setForeground(Color.GREEN);
		myToolBar.add(left);
		final JButton right = new JButton(graphicalZone
				.getSurroundWithRightContextAction());
		right.setHideActionText(true);
		right.setText("$[");
		right.setMaximumSize(new Dimension(36, 36));
		right.setMinimumSize(new Dimension(36, 36));
		right.setPreferredSize(new Dimension(36, 36));
		right.setForeground(Color.GREEN);
		myToolBar.add(right);
		final JButton negative = new JButton(graphicalZone
				.getSurroundWithNegativeRightContextAction());
		negative.setHideActionText(true);
		negative.setText("$![");
		negative.setMaximumSize(new Dimension(36, 36));
		negative.setMinimumSize(new Dimension(36, 36));
		negative.setPreferredSize(new Dimension(36, 36));
		negative.setForeground(Color.GREEN);
		myToolBar.add(negative);
	}

	public static class ShowDiffDo implements ToDo {
		File base, dest, diffResult;

		public ShowDiffDo(File base, File dest, File diffResult) {
			this.base = base;
			this.dest = dest;
			this.diffResult = diffResult;
		}

		public void toDo(boolean success) {
			final GraphDecorator info = GraphDecorator.loadDiffFile(diffResult);
			if (info == null) {
				JOptionPane.showMessageDialog(null,
						"Cannot load diff result file", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (info.noDifference()) {
				JOptionPane.showMessageDialog(null, "Graphs are identical",
						"Diff result", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			final GraphIO baseGrf = GraphIO.loadGraph(base, false, false);
			if (baseGrf == null) {
				JOptionPane.showMessageDialog(null,
						"Cannot load base graph file", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			final GraphIO destGrf = GraphIO.loadGraph(dest, false, false);
			if (destGrf == null) {
				JOptionPane.showMessageDialog(null,
						"Cannot load dest graph file", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			InternalFrameManager.getManager(base).newGraphDiffFrame(baseGrf, destGrf, info);
		}
	}

	protected void showGraphDependencies(boolean showCalledGrf) {
		if (getGraph() == null) {
			JOptionPane.showMessageDialog(null,
					"Cannot compile a graph with no name", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		ArrayList<GraphCall> files;
		if (showCalledGrf) {
			files = Dependancies.getAllSubgraphs(getGraph());
			grfListLabel.setText("Called graphs:");
		} else {
			files = Dependancies.whoCalls(getGraph(), ConfigManager.getManager().getCurrentLanguageDir());
			grfListLabel.setText("Caller graphs:");
		}
		grfListModel.clear();
		for (final GraphCall c : files) {
			grfListModel.addElement(c);
		}
		if (!getActualMainPanel().isAncestorOf(grfListPanel)) {
			getActualMainPanel().add(grfListPanel, BorderLayout.EAST);
		}
		getActualMainPanel().revalidate();
		getActualMainPanel().repaint();
	}

	private JPanel buildTextPanel() {
		final JPanel p = new JPanel(new BorderLayout());
		setBoxContentEditor(new TextField(25, this));
		getBoxContentEditor()
				.setComponentOrientation(ConfigManager.getManager()
						.isRightToLeftForGraphs(null) ? ComponentOrientation.RIGHT_TO_LEFT
						: ComponentOrientation.LEFT_TO_RIGHT);
		p.add(getBoxContentEditor());
		return p;
	}

	class MyInternalFrameListener extends InternalFrameAdapter {
		@Override
		public void internalFrameActivated(InternalFrameEvent e) {
			getBoxContentEditor().requestFocus();
			getBoxContentEditor().getCaret().setVisible(true);
		}

		@Override
		public void internalFrameClosing(InternalFrameEvent e) {
			if (modified) {
				final Object[] options_on_exit = { "Save", "Don't save" };
				final Object[] normal_options = { "Save", "Don't save",
						"Cancel" };
				int n;
				if (UnitexFrame.closing) {
					n = JOptionPane
							.showOptionDialog(
									GraphFrame.this,
									"Graph has been modified. Do you want to save it ?",
									"", JOptionPane.YES_NO_CANCEL_OPTION,
									JOptionPane.QUESTION_MESSAGE, null,
									options_on_exit, options_on_exit[0]);
				} else {
					n = JOptionPane
							.showOptionDialog(
									GraphFrame.this,
									"Graph has been modified. Do you want to save it ?",
									"", JOptionPane.YES_NO_CANCEL_OPTION,
									JOptionPane.QUESTION_MESSAGE, null,
									normal_options, normal_options[0]);
				}
				if (n == JOptionPane.CLOSED_OPTION)
					return;
				if (n == 0) {
					if (!saveGraph())
						return;
				}
				if (n != 2) {
					getAutoRefresh().stop();
					dispose();
					return;
				}
				return;
			}
			getAutoRefresh().stop();
			dispose();
		}
	}

	/**
	 * Resizes the drawing area
	 * 
	 * @param x
	 * @param y
	 */
	public void reSizeGraphicalZone(int x, int y) {
		graphicalZone.setSize(new Dimension(x, y));
		graphicalZone.setPreferredSize(new Dimension(x, y));
		graphicalZone.revalidate();
		graphicalZone.repaint();
		setModified(true);
	}

	/**
	 * Sets the <code>modified</code> field
	 * 
	 * @param b
	 *            <code>true</code> if the graph must be marked as modified,
	 *            <code>false</code> otherwise
	 */
	void setModified(boolean b) {
		graphicalZone.setHighlight(false);
		modified = b;
		if (grf != null) {
			if (modified)
				setTitle(grf.getName() + " (" + grf.getParent() + ")"
						+ " (Unsaved)");
			else
				setTitle(grf.getName() + " (" + grf.getParent() + ")");
		} else {
			if (modified)
				setTitle(" (Unsaved)");
			else
				setTitle("Graph");
		}
	}

	/**
	 * Sets the zoom scale factor
	 * 
	 * @param d
	 *            scale factor
	 */
	public void setScaleFactor(double d) {
		graphicalZone.scaleFactor = d;
		graphicalZone.setPreferredSize(new Dimension((int) (graphicalZone
				.getWidth() * graphicalZone.scaleFactor), (int) (graphicalZone
				.getHeight() * graphicalZone.scaleFactor)));
		graphicalZone.revalidate();
		graphicalZone.repaint();
	}

	/**
	 * Sorts lines of all selected boxes
	 */
	public void sortNodeLabel() {
		if (graphicalZone.selectedBoxes.isEmpty())
			return;
		for (int i = 0; i < graphicalZone.selectedBoxes.size(); i++) {
			final GraphBox g = (GraphBox) graphicalZone.selectedBoxes.get(i);
			g.sortNodeLabel();
		}
		graphicalZone.unSelectAllBoxes();
		getBoxContentEditor().setContent("");
		graphicalZone.repaint();
	}

	/**
	 * Inverts the antialiasing flag
	 */
	public void changeAntialiasingValue() {
		final GraphPresentationInfo info = getGraphPresentationInfo();
		info.setAntialiasing(!info.isAntialiasing());
		graphicalZone.repaint();
	}

	/**
     *
     */
	private void updateDoUndoButtons() {
		if (undoButton != null && redoButton != null) {
			undoButton.setEnabled(getManager().canUndo());
			redoButton.setEnabled(getManager().canRedo());
		}
	}

	class UndoIt implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			try {
				getManager().undo();
			} catch (final CannotUndoException ex) {
				ex.printStackTrace();
			} finally {
				repaint();
			}
		}
	}

	class RedoIt implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			try {
				getManager().redo();
			} catch (final CannotRedoException ex) {
				ex.printStackTrace();
			} finally {
				repaint();
			}
		}
	}

	@Override
	public void repaint() {
		super.repaint();
		updateDoUndoButtons();
	}

	public void setRedoEnabled(boolean b) {
		redoButton.setEnabled(b);
	}

	public void setUndoEnabled(boolean b) {
		undoButton.setEnabled(b);
	}

	public File getGraph() {
		return grf;
	}

	public void setGraph(File grf) {
		this.lastModification = grf.lastModified();
		this.grf = grf;
		this.nonEmptyGraph = true;
		this.setTitle(grf.getName() + " (" + grf.getParent() + ")");
	}

	public void saveGraphAsAnImage(File output) {
		final BufferedImage image = new BufferedImage(graphicalZone.getWidth(),
				graphicalZone.getHeight(), BufferedImage.TYPE_INT_RGB);
		final Graphics g = image.getGraphics();
		try {
			graphicalZone.paintAll(g);
			try {
				final ImageOutputStream stream = ImageIO
						.createImageOutputStream(output);
				ImageIO.write(image, "png", stream);
				stream.close();
			} catch (final IOException e1) {
				e1.printStackTrace();
			}
		} finally {
			g.dispose();
		}
	}

	/**
	 * This function saves the current graph frame as a SVG file.
	 * 
	 * @param file
	 */
	public void saveGraphAsAnSVG(File file) {
		try {
			if (!file.exists())
				file.createNewFile();
		} catch (final IOException e) {
			JOptionPane.showMessageDialog(null, "Cannot write "
					+ file.getAbsolutePath(), "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (!file.canWrite()) {
			JOptionPane.showMessageDialog(null, "Cannot write "
					+ file.getAbsolutePath(), "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			final OutputStreamWriter writer = ConfigManager.getManager()
					.getEncoding(null).getOutputStreamWriter(file);
			final SVG svg = new SVG(writer, this);
			svg.save();
			writer.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public JScrollPane getScroll() {
		return scroll;
	}

	public GraphPresentationInfo getGraphPresentationInfo() {
		return graphicalZone.getGraphPresentationInfo();
	}

	public void setGraphPresentationInfo(GraphPresentationInfo info) {
		updateToolBar(info.getIconBarPosition());
		graphicalZone.setGraphPresentationInfo(info);
		setModified(true);
	}

	private void updateToolBar(String iconBarPosition) {
		mainPanel.remove(myToolBar);
		if (!(iconBarPosition.equals(Preferences.NO_ICON_BAR))) {
			if (iconBarPosition.equals(Preferences.ICON_BAR_WEST)
					|| iconBarPosition.equals(Preferences.ICON_BAR_EAST)) {
				myToolBar.setOrientation(SwingConstants.VERTICAL);
			} else {
				myToolBar.setOrientation(SwingConstants.HORIZONTAL);
			}
			mainPanel.add(myToolBar, iconBarPosition);
		}
		mainPanel.revalidate();
		mainPanel.repaint();
	}

	public void HTopAlign() {
		graphicalZone.HTopAlign();
		setModified(true);
	}

	public void HCenterAlign() {
		graphicalZone.HCenterAlign();
		setModified(true);
	}

	public void HBottomAlign() {
		graphicalZone.HBottomAlign();
		setModified(true);
	}

	public void VLeftAlign() {
		graphicalZone.VLeftAlign();
		setModified(true);
	}

	public void VCenterAlign() {
		graphicalZone.VCenterAlign();
		setModified(true);
	}

	public void VRightAlign() {
		graphicalZone.VRightAlign();
		setModified(true);
	}

	public void setGrid(boolean b, int n) {
		graphicalZone.setGrid(b, n);
		setModified(true);
	}

	public ArrayList<GenericGraphBox> getSelectedBoxes() {
		return graphicalZone.selectedBoxes;
	}

	public void selectAllBoxes() {
		graphicalZone.selectAllBoxes();
	}

	public void removeSelected() {
		graphicalZone.removeSelected();
	}

	public void pasteSelection(MultipleSelection m) {
		graphicalZone.pasteSelection(m);
	}

	public void setTextForSelected(String text) {
		graphicalZone.setTextForSelected(text);
		setModified(true);
	}

	public void unSelectAllBoxes() {
		graphicalZone.unSelectAllBoxes();
	}

	/**
	 * Compiles the current <code>GraphFrame</code>. If the graph is unsaved, an
	 * error message is shown and nothing is done; otherwise the compilation
	 * process is launched through the creation of a
	 * <code>ProcessInfoFrame</code> object.
	 */
	public void compileGraph() {
		if (modified) {
			JOptionPane.showMessageDialog(null,
					"Save graph before compiling it", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (getGraph() == null) {
			JOptionPane.showMessageDialog(null,
					"Cannot compile a graph with no name", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		final Grf2Fst2Command command = new Grf2Fst2Command().grf(getGraph())
				.enableLoopAndRecursionDetection(true).tokenizationMode(
						graphicalZone.getMetadata().getLanguage(), getGraph())
				.repositories().emitEmptyGraphWarning().displayGraphNames();
		Launcher.exec(command, false);
	}

	public boolean saveGraph() {
		final File file = getGraph();
		if (file == null) {
			return saveAsGraph();
		}
		final GraphIO g = new GraphIO(graphicalZone);
		modified = false;
		g.saveGraph(file);
		setGraph(file);
		return true;
	}

	public boolean saveAsGraph() {
		final GraphIO g = new GraphIO(graphicalZone);
		final JFileChooser fc = Config.getGraphDialogBox(true);
		fc.setMultiSelectionEnabled(false);
		fc.setDialogType(JFileChooser.SAVE_DIALOG);
		File file = null;
		for (;;) {
			final int returnVal = fc.showSaveDialog(this);
			fc.setMultiSelectionEnabled(true);
			if (returnVal != JFileChooser.APPROVE_OPTION) {
				// we return if the user has clicked on CANCEL
				return false;
			}
			file = fc.getSelectedFile();
			if (file == null || !file.exists())
				break;
			final String message = file
					+ "\nalready exists. Do you want to replace it ?";
			final String[] options = { "Yes", "No" };
			final int n = JOptionPane.showOptionDialog(null, message, "Error",
					JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null,
					options, options[0]);
			if (n == 0) {
				break;
			}
		}
		if (file == null) {
			return false;
		}
		final String name = file.getAbsolutePath();
		// if the user wants to save the graph as an image
		if (name.endsWith(".png") || name.endsWith(".PNG")) {
			// we do not change the "modified" status and the title of the
			// frame
			saveGraphAsAnImage(file);
			return true;
		}
		// if the user wants to save the graph as a vectorial file
		if (name.endsWith(".svg") || name.endsWith(".SVG")) {
			// we do not change the "modified" status and the title of the
			// frame
			saveGraphAsAnSVG(file);
			return true;
		}
		if (!name.endsWith(".grf")) {
			file = new File(name + ".grf");
		}
		modified = false;
		g.saveGraph(file);
		setGraph(file);
		return true;
	}

	@Override
	public File getKey() {
		return grf;
	}

	@Override
	public String getTabName() {
		return (grf==null)?"(unsaved grf)":((modified?"*":"")+grf.getName());
	}

	public int find(String text) {
		return graphicalZone.find(text);
	}

	public void setBoxContentEditor(TextField boxContentEditor) {
		this.boxContentEditor = boxContentEditor;
	}

	public TextField getBoxContentEditor() {
		return boxContentEditor;
	}

	public Timer getAutoRefresh() {
		return autoRefresh;
	}

	public UndoManager getManager() {
		return manager;
	}

	public JPanel getActualMainPanel() {
		return actualMainPanel;
	}

	public void setSvnPanel(JPanel svnPanel) {
		this.svnPanel = svnPanel;
	}

	public JPanel getSvnPanel() {
		return svnPanel;
	}

}
