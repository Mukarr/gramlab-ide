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
package org.gramlab.plugins.dictionaryEditor;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.gramlab.api.InternalDictionaryEditor;
import org.gramlab.api.InternalGraphEditor;
import org.gramlab.api.GramlabMenu;
import org.gramlab.core.GramlabConfigManager;
import org.gramlab.core.gramlab.project.GramlabProject;
import org.gramlab.core.gramlab.project.GramlabProjectManager;
import org.gramlab.core.umlv.unitex.common.project.manager.GlobalProjectManager;
import org.gramlab.core.umlv.unitex.files.PersonalFileFilter;
import org.gramlab.core.umlv.unitex.frames.DelaFrame;
import org.gramlab.core.umlv.unitex.frames.InternalFrameManager;
import org.gramlab.core.umlv.unitex.process.Launcher;
import org.gramlab.core.umlv.unitex.process.ToDo;
import org.gramlab.core.umlv.unitex.process.commands.CompressCommand;
import org.gramlab.core.umlv.unitex.process.commands.SortTxtCommand;

import ro.fortsoft.pf4j.Extension;

/**
 * Unitex/GramLab Internal Dictionary Editor implementation
 * @author Mukarram Tailor
 */ 
@Extension
public class DictionaryEditor implements GramlabMenu, InternalDictionaryEditor{
  @Override
  public JMenu Addmenu(){
	  final JMenu delaMenu = new JMenu("Dictionaries");
		final JMenuItem open2 = new JMenuItem("Open...");
		open2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openDELA();
			}
		});
		delaMenu.add(open2);
		delaMenu.addSeparator();
		Action checkDelaFormat = new AbstractAction("Check Format...") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager=GlobalProjectManager.search(null)
					.getFrameManagerAs(InternalFrameManager.class);
				if (manager==null) {
					JOptionPane.showMessageDialog(null,
							"This operation is not possible if no project is open!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				JInternalFrame frame = manager.getSelectedFrame();
				if (frame == null || !(frame instanceof DelaFrame)) {
					JOptionPane.showMessageDialog(null,
							"No dictionary is selected!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				DelaFrame df = (DelaFrame) frame;
				File f = df.getKey();
				if (f == null) {
					JOptionPane.showMessageDialog(null,
							"No dictionary is selected!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				manager.newCheckDicFrame(f);
			}
		};
		checkDelaFormat.putValue(Action.ACCELERATOR_KEY, KeyStroke
				.getKeyStroke(KeyEvent.VK_K, Event.CTRL_MASK));
		delaMenu.add(new JMenuItem(checkDelaFormat));
		Action sortDictionary = new AbstractAction("Sort Dictionary") {
			public void actionPerformed(ActionEvent e) {
				sortDELA();
			}
		};
		delaMenu.add(new JMenuItem(sortDictionary));
		Action inflect = new AbstractAction("Inflect...") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager=GlobalProjectManager.search(null)
					.getFrameManagerAs(InternalFrameManager.class);
				if (manager==null) {
					JOptionPane.showMessageDialog(null,
							"This operation is not possible if no project is open!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				JInternalFrame frame = manager.getSelectedFrame();
				if (frame == null || !(frame instanceof DelaFrame)) {
					JOptionPane.showMessageDialog(null,
							"No dictionary is selected!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				DelaFrame df = (DelaFrame) frame;
				File f = df.getKey();
				if (f == null) {
					JOptionPane.showMessageDialog(null,
							"No dictionary is selected!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				manager.newInflectFrame(f);
			}
		};
		delaMenu.add(new JMenuItem(inflect));
		Action compressIntoFST = new AbstractAction("Compress into FST") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager=GlobalProjectManager.search(null)
					.getFrameManagerAs(InternalFrameManager.class);
				if (manager==null) {
					JOptionPane.showMessageDialog(null,
							"This operation is not possible if no project is open!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				JInternalFrame frame = manager.getSelectedFrame();
				if (frame == null || !(frame instanceof DelaFrame)) {
					JOptionPane.showMessageDialog(null,
							"No dictionary is selected!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				DelaFrame df = (DelaFrame) frame;
				File f = df.getKey();
				if (f == null) {
					JOptionPane.showMessageDialog(null,
							"No dictionary is selected!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				CompressCommand cmd = new CompressCommand().dic(f);
				if (GlobalProjectManager.getAs(GramlabProjectManager.class)
						.getProject(f).isSemitic()) {
					cmd = cmd.semitic();
				}
				Launcher.exec(cmd, false);
			}
		};
		delaMenu.add(new JMenuItem(compressIntoFST));
		delaMenu.addSeparator();
		Action closeDela = new AbstractAction("Close") {
			public void actionPerformed(ActionEvent e) {
				InternalFrameManager manager=GlobalProjectManager.search(null)
					.getFrameManagerAs(InternalFrameManager.class);
				if (manager==null) {
					JOptionPane.showMessageDialog(null,
							"This operation is not possible if no project is open!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				JInternalFrame frame = manager.getSelectedFrame();
				if (frame == null || !(frame instanceof DelaFrame))
					return;
				frame.doDefaultCloseAction();
			}
		};
		delaMenu.add(new JMenuItem(closeDela));
		return delaMenu;
  }
  
  void openDELA() {
		File dir;
		GramlabProject p = GlobalProjectManager.getAs(GramlabProjectManager.class)
				.getCurrentProject();
		if (p == null)
			dir = GramlabConfigManager.getWorkspaceDirectory();
		else
			dir = p.getProjectDirectory();
		final JFileChooser fc = new JFileChooser(dir);
		fc.addChoosableFileFilter(new PersonalFileFilter("dic",
				"Unicode DELA Dictionaries"));
		fc.setMultiSelectionEnabled(true);
		fc.setDialogType(JFileChooser.OPEN_DIALOG);
		final int returnVal = fc.showOpenDialog(fc);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			// we return if the user has clicked on CANCEL
			return;
		}
		GramlabProject proj=null;
		for (File f : fc.getSelectedFiles()) {
			InternalFrameManager m=GlobalProjectManager.search(f)
				.getFrameManagerAs(InternalFrameManager.class);
			if (m==null) {
				JOptionPane.showMessageDialog(null, "Dictionary "
						+ f.getAbsolutePath() + " does not belong\n"
						+"to any project of your workspace. Cannot open it if no project is open.",
						"Warning", JOptionPane.WARNING_MESSAGE);
			} else {
				m.newDelaFrame(f);
				if (proj==null) {
					proj=GlobalProjectManager.getAs(GramlabProjectManager.class).getProject(f);
				}
			}
		}
		if (proj!=null) {
			GlobalProjectManager.getAs(GramlabProjectManager.class).setCurrentProject(proj);
		}
	}

	/**
	 * Sorts the current dictionary. The external program "SortTxt" is called
	 * through the creation of a <code>ProcessInfoFrame</code> object.
	 */
	private void sortDELA() {
		InternalFrameManager manager=GlobalProjectManager.search(null)
			.getFrameManagerAs(InternalFrameManager.class);
		if (manager==null) {
			JOptionPane.showMessageDialog(null,
					"This operation is not possible if no project is open!", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		JInternalFrame frame = manager.getSelectedFrame();
		if (frame == null || !(frame instanceof DelaFrame))
			if (frame == null || !(frame instanceof DelaFrame)) {
				JOptionPane.showMessageDialog(null,
						"No dictionary is selected!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		DelaFrame df = (DelaFrame) frame;
		File f = df.getKey();
		if (f == null)
			return;
		SortTxtCommand command = new SortTxtCommand().file(f);
		GramlabProject p = GlobalProjectManager.getAs(GramlabProjectManager.class)
				.getCurrentProject();
		if (p == null)
			return;
		if (p.getLanguage().equals("th")) {
			command = command.thai(true);
		} else {
			command = command.sortAlphabet(p.getSortAlphabet());
		}
		df.doDefaultCloseAction();
		Launcher.exec(command, true, new DelaDo(f));
	}

	class DelaDo implements ToDo {
		final File dela;

		public DelaDo(File s) {
			dela = s;
		}

		public void toDo(boolean success) {
			GlobalProjectManager.search(dela)
				.getFrameManagerAs(InternalFrameManager.class).newDelaFrame(dela);
		}
	}

}