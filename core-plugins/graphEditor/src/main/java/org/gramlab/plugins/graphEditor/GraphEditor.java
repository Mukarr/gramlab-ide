/*
 * Unitex
 *
 * Copyright (C) 2001-2016 Université Paris-Est Marne-la-Vallée <unitex@univ-mlv.fr>
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
package org.gramlab.plugins.graphEditor;

import java.awt.Dimension;
import java.awt.Event;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;

import org.gramlab.api.InternalGraphEditor;
import org.gramlab.api.GramlabMenu;
import org.gramlab.api.GraphMenu;
import org.gramlab.core.GramlabConfigManager;
import org.gramlab.core.gramlab.project.GramlabProject;
import org.gramlab.core.gramlab.project.GramlabProjectManager;
import org.gramlab.core.gramlab.util.GraphSearchDialog;
import org.gramlab.core.umlv.unitex.common.project.manager.GlobalProjectManager;
import org.gramlab.core.umlv.unitex.config.ConfigManager;
import org.gramlab.core.umlv.unitex.frames.FileEditionTextFrame;
import org.gramlab.core.umlv.unitex.frames.GraphFrame;
import org.gramlab.core.umlv.unitex.frames.InternalFrameManager;
import org.gramlab.core.umlv.unitex.frames.MenuAdapter;
import org.gramlab.core.umlv.unitex.frames.TextAutomatonFrame;
import org.gramlab.core.umlv.unitex.frames.UnitexFrame;
import org.gramlab.core.umlv.unitex.graphrendering.GraphMenuBuilder;
import org.gramlab.core.umlv.unitex.grf.GraphPresentationInfo;
import org.gramlab.core.umlv.unitex.print.PrintManager;

import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.Extension;

/**
 * Unitex/GramLab Internal Graph Editor implementation
 * @author Mukarram Tailor
 */ 
@Extension
public class GraphEditor implements GramlabMenu, InternalGraphEditor{
  @Override
  public JMenu Addmenu(){
	  
		JMenu m = new JMenu("Graphs");
		final Action n = new AbstractAction("New") {
			public void actionPerformed(ActionEvent e) {
				GlobalProjectManager.search(null)
						.getFrameManagerAs(InternalFrameManager.class)
						.newGraphFrame(null);
			}
		};
		m.add(new JMenuItem(n));
		final Action open = new AbstractAction("Open") {
			public void actionPerformed(ActionEvent e) {
				openGraph();
			}
		};
		open.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
		m.add(new JMenuItem(open));
		final Action save = new AbstractAction("Save") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					f.saveGraph();
					return;
				}
				/*
				 * Evil hack to allow save with ctrl+S in the internal text
				 * editor
				 */
				JInternalFrame frame = manager.getSelectedFrame();
				if (frame instanceof FileEditionTextFrame) {
					((FileEditionTextFrame) frame).saveFile();
					return;
				}
			}
		};
		save.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
		m.add(new JMenuItem(save));
		final Action saveAs = new AbstractAction("Save as...") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f == null)
					return;
				f.saveAsGraph();
			}
		};
		m.add(new JMenuItem(saveAs));
		final Action saveAll = new AbstractAction("Save all") {
			public void actionPerformed(ActionEvent e) {
				GlobalProjectManager.search(null)
						.getFrameManagerAs(InternalFrameManager.class)
						.saveAllGraphFrames();
			}
		};
		m.add(new JMenuItem(saveAll));

//		final JMenu exportMenu = GraphMenuBuilder.createExportMenu();
//		m.add(exportMenu);
		// added by mukarram
		DefaultPluginManager pluginManager = GramlabConfigManager.getPluginManager();
		List<GraphMenu> grfmenus = pluginManager.getExtensions(GraphMenu.class);
		for (GraphMenu grfmenu : grfmenus) {
			m.add(grfmenu.Addmenu());
		}

		m.addSeparator();
		final Action search = new AbstractAction("Search") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f == null) {
					JOptionPane.showMessageDialog(null,
							"You must select a graph to search in", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				new GraphSearchDialog(f);
			}
		};
		search.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK));
		m.add(new JMenuItem(search));
		final Action setup = new AbstractAction("Page Setup") {
			public void actionPerformed(ActionEvent e) {
				PrintManager.pageSetup();
			}
		};
		m.add(new JMenuItem(setup));
		final Action print = new AbstractAction("Print...") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				PrintManager.print(manager.getSelectedFrame());
			}
		};
		print.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke('P', Event.CTRL_MASK));
		m.add(new JMenuItem(print));
		final Action printAll = new AbstractAction("Print All...") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				PrintManager.printAllGraphs(manager.getGraphFrames());
			}
		};
		m.add(new JMenuItem(printAll));
		m.addSeparator();
		final Action undo = new AbstractAction("Undo") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					f.undo();
					return;
				}
			}
		};
		undo.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK));
		m.add(new JMenuItem(undo));
		final Action redo = new AbstractAction("Redo") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					f.redo();
					return;
				}
			}
		};
		redo.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK));
		m.add(new JMenuItem(redo));
		m.addSeparator();
		final Action seq2grf = new AbstractAction("Build sequence automaton") {
			public void actionPerformed(ActionEvent e) {
				GlobalProjectManager.search(null)
						.getFrameManagerAs(InternalFrameManager.class)
						.getSeq2GrfFrame();
			}
		};
		m.add(new JMenuItem(seq2grf));
		m.addSeparator();
		final JMenu tools = new JMenu("Tools");
		final JMenuItem sortNodeLabel = new JMenuItem("Sort Node Label");
		sortNodeLabel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					f.sortNodeLabel();
				}
			}
		});
		final JMenuItem explorePaths = new JMenuItem("Explore graph paths");
		explorePaths.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					GlobalProjectManager.search(null)
							.getFrameManagerAs(InternalFrameManager.class)
							.newGraphPathDialog();
				}
			}
		});
		final JMenuItem compileFST = new JMenuItem("Compile FST2");
		compileFST.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame currentFrame = manager
						.getCurrentFocusedGraphFrame();
				if (currentFrame == null)
					return;
				currentFrame.compileGraph();
			}
		});
		final JMenuItem flatten = new JMenuItem("Compile & Flatten FST2");
		flatten.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UnitexFrame.compileAndFlattenGraph();
			}
		});
		final JMenuItem graphCollection = new JMenuItem(
				"Build Graph Collection");
		graphCollection.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GlobalProjectManager.search(null)
						.getFrameManagerAs(InternalFrameManager.class)
						.newGraphCollectionFrame();
			}
		});
		final JMenuItem svn = new JMenuItem("Look for SVN conflicts");
		svn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GramlabProject p = GlobalProjectManager.getAs(
						GramlabProjectManager.class).getCurrentProject();
				if (p == null)
					return;
				p.getSvnMonitor().monitor(false);
			}
		});
		tools.add(sortNodeLabel);
		tools.add(explorePaths);
		tools.addSeparator();
		tools.add(compileFST);
		tools.add(flatten);
		tools.addSeparator();
		tools.add(graphCollection);
		tools.addSeparator();
		tools.add(svn);
		m.add(tools);
		final JMenu format = new JMenu("Format");
		final JMenuItem alignment = new JMenuItem("Alignment...");
		alignment.setAccelerator(KeyStroke.getKeyStroke('M', Event.CTRL_MASK));
		alignment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					manager.newGraphAlignmentDialog(f);
				}
			}
		});
		final JMenuItem antialiasing = new JMenuItem("Antialiasing...");
		antialiasing.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final JInternalFrame f = manager.getSelectedFrame();
				if (f == null)
					return;
				if (f instanceof GraphFrame) {
					final GraphFrame f2 = (GraphFrame) f;
					f2.changeAntialiasingValue();
					return;
				}
				if (f instanceof TextAutomatonFrame) {
					final TextAutomatonFrame f2 = (TextAutomatonFrame) f;
					f2.changeAntialiasingValue();
				}
			}
		});
		final JMenuItem presentation = new JMenuItem("Presentation...");
		presentation.setAccelerator(KeyStroke
				.getKeyStroke('R', Event.CTRL_MASK));
		presentation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					final GraphPresentationInfo info = manager
							.newGraphPresentationDialog(
									f.getGraphPresentationInfo(), true);
					if (info != null) {
						f.setGraphPresentationInfo(info);
					}
				}
			}
		});
		final JMenuItem graphSize = new JMenuItem("Graph Size...");
		graphSize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					manager.newGraphSizeDialog(f);
				}
			}
		});
		format.add(antialiasing);
		format.addSeparator();
		format.add(alignment);
		format.add(presentation);
		format.add(graphSize);
		final JMenu zoom = new JMenu("Zoom");
		final ButtonGroup groupe = new ButtonGroup();
		final JRadioButtonMenuItem fitInScreen = new JRadioButtonMenuItem(
				"Fit in screen");
		final JRadioButtonMenuItem fitInWindow = new JRadioButtonMenuItem(
				"Fit in window");
		final JRadioButtonMenuItem fit60 = new JRadioButtonMenuItem("60%");
		final JRadioButtonMenuItem fit80 = new JRadioButtonMenuItem("80%");
		final JRadioButtonMenuItem fit100 = new JRadioButtonMenuItem("100%");
		final JRadioButtonMenuItem fit120 = new JRadioButtonMenuItem("120%");
		final JRadioButtonMenuItem fit140 = new JRadioButtonMenuItem("140%");
		groupe.add(fitInScreen);
		groupe.add(fitInWindow);
		groupe.add(fit60);
		groupe.add(fit80);
		groupe.add(fit100);
		fit100.setSelected(true);
		groupe.add(fit120);
		groupe.add(fit140);
		fitInScreen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					f.removeComponentListener(f.compListener);
					Dimension screenSize = Toolkit.getDefaultToolkit()
							.getScreenSize();
					final double scale_x = screenSize.width
							/ (double) f.getGraphicalZone().getWidth();
					final double scale_y = screenSize.height
							/ (double) f.getGraphicalZone().getHeight();
					if (scale_x < scale_y)
						f.setScaleFactor(scale_x);
					else
						f.setScaleFactor(scale_y);
				}
			}
		});
		fitInWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					final Dimension d = f.getScroll().getSize();
					final double scale_x = (d.width - 3)
							/ (double) f.getGraphicalZone().getWidth();
					final double scale_y = (d.height - 3)
							/ (double) f.getGraphicalZone().getHeight();
					if (scale_x < scale_y)
						f.setScaleFactor(scale_x);
					else
						f.setScaleFactor(scale_y);
					f.compListener = new ComponentAdapter() {
						@Override
						public void componentResized(ComponentEvent e2) {
							final Dimension d2 = f.getScroll().getSize();
							final double scale_x2 = (d2.width - 3)
									/ (double) f.getGraphicalZone().getWidth();
							final double scale_y2 = (d2.height - 3)
									/ (double) f.getGraphicalZone().getHeight();
							if (scale_x2 < scale_y2)
								f.setScaleFactor(scale_x2);
							else
								f.setScaleFactor(scale_y2);
						}
					};
					f.addComponentListener(f.compListener);
				}
			}
		});
		fit60.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					f.removeComponentListener(f.compListener);
					f.setScaleFactor(0.6);
				}
			}
		});
		fit80.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					f.removeComponentListener(f.compListener);
					f.setScaleFactor(0.8);
				}
			}
		});
		fit100.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					f.removeComponentListener(f.compListener);
					f.setScaleFactor(1.0);
				}
			}
		});
		fit120.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					f.removeComponentListener(f.compListener);
					f.setScaleFactor(1.2);
				}
			}
		});
		fit140.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager = GlobalProjectManager
						.search(null).getFrameManagerAs(
								InternalFrameManager.class);
				final GraphFrame f = manager.getCurrentFocusedGraphFrame();
				if (f != null) {
					f.removeComponentListener(f.compListener);
					f.setScaleFactor(1.4);
				}
			}
		});
		zoom.add(fitInScreen);
		zoom.add(fitInWindow);
		zoom.add(fit60);
		zoom.add(fit80);
		zoom.add(fit100);
		zoom.add(fit120);
		zoom.add(fit140);
		m.add(tools);
		m.add(format);
		m.add(zoom);
		m.addSeparator();
		final Action closeAll = new AbstractAction("Close all") {
			public void actionPerformed(ActionEvent e) {
				GlobalProjectManager.search(null)
						.getFrameManagerAs(InternalFrameManager.class)
						.closeAllGraphFrames();
			}
		};
		m.add(new JMenuItem(closeAll));

		m.addMenuListener(new MenuAdapter() {
			@Override
			public void menuSelected(MenuEvent e) {
				boolean existsFocusedGrFrame = false;
				boolean existsAnyGrFrame = false;
				boolean existsManager = false;

				if (GlobalProjectManager.getAs(GramlabProjectManager.class)
						.getCurrentProject() != null) {
					InternalFrameManager manager = GlobalProjectManager.search(
							null).getFrameManagerAs(InternalFrameManager.class);

					if (manager != null) {
						existsManager = true;
						existsFocusedGrFrame = manager
								.getCurrentFocusedGraphFrame() != null;
						existsAnyGrFrame = manager.getGraphFrames().size() != 0;
					}
				}

				n.setEnabled(existsManager);
				open.setEnabled(existsManager);
				save.setEnabled(existsFocusedGrFrame);
				saveAs.setEnabled(existsFocusedGrFrame);
				saveAll.setEnabled(existsAnyGrFrame);
//				exportMenu.setEnabled(existsFocusedGrFrame);
				//added by mukarram
//				for (GraphMenu grfmenu : grfmenus) {
//					final JMenu menu = grfmenu.Addmenu();
//					menu.setEnabled(existsFocusedGrFrame);
//				}
				search.setEnabled(existsFocusedGrFrame);
				setup.setEnabled(existsAnyGrFrame);
				print.setEnabled(existsFocusedGrFrame);
				printAll.setEnabled(existsAnyGrFrame);
				undo.setEnabled(existsFocusedGrFrame);
				redo.setEnabled(existsFocusedGrFrame);
				seq2grf.setEnabled(existsManager);
				tools.setEnabled(existsFocusedGrFrame);
				format.setEnabled(existsFocusedGrFrame);
				zoom.setEnabled(existsFocusedGrFrame);
				closeAll.setEnabled(existsManager);
			}
		});

		return m;
  }
  public void openGraph() {
		File dir;
		GramlabProject p = GlobalProjectManager.getAs(GramlabProjectManager.class)
				.getCurrentProject();
		if (p == null)
			dir = GramlabConfigManager.getWorkspaceDirectory();
		else
			dir = p.getProjectDirectory();
		if (ConfigManager.getManager().getCurrentGraphDirectory() != null) {
			dir = ConfigManager.getManager().getCurrentGraphDirectory();
		}
		final JFileChooser fc = new JFileChooser(dir);
		fc.setMultiSelectionEnabled(true);
		fc.setDialogType(JFileChooser.OPEN_DIALOG);
		final int returnVal = fc.showOpenDialog(fc);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			// we return if the user has clicked on CANCEL
			return;
		}
		final File[] graphs = fc.getSelectedFiles();
		GramlabProject proj=null;
		for (int i = 0; i < graphs.length; i++) {
			String s = graphs[i].getAbsolutePath();
			if (!graphs[i].exists() && !s.endsWith(".grf")) {
				s = s + ".grf";
				graphs[i] = new File(s);
				if (!graphs[i].exists()) {
					JOptionPane.showMessageDialog(null, "File "
							+ graphs[i].getAbsolutePath() + " does not exist",
							"Error", JOptionPane.ERROR_MESSAGE);
					continue;
				}
			}
			InternalFrameManager manager = GlobalProjectManager
					.search(graphs[i]).getFrameManagerAs(InternalFrameManager.class);
			if (manager == null) {
				JOptionPane.showMessageDialog(null,
						"You can not open a graph if no project is opened.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			manager.newGraphFrame(graphs[i]);
			if (proj==null) {
				proj=GlobalProjectManager.getAs(GramlabProjectManager.class)
						.getProject(graphs[i]);
			}
		}
		if (proj!=null) {
			GlobalProjectManager.getAs(GramlabProjectManager.class)
				.setCurrentProject(proj);
		}
	}

}