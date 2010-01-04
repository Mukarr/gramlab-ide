 /*
  * Unitex
  *
  * Copyright (C) 2001-2010 Universit� Paris-Est Marne-la-Vall�e <unitex@univ-mlv.fr>
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

package fr.umlv.unitex.process;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import fr.umlv.unitex.UnitexFrame;

public class HelpOnCommandFrame extends JInternalFrame {

    @SuppressWarnings("unchecked")
    Class[] commands={CheckDicCommand.class
            ,CompressCommand.class
            ,CompressKrCommand.class
            ,ConcordCommand.class
            ,ConcorDiffCommand.class
            ,ConsultDicCommand.class
            ,ConvertCommand.class
            ,DicoCommand.class
            ,ElagCommand.class
            ,ElagCompCommand.class
            ,EvambCommand.class
            ,ExtractCommand.class
            ,FlattenCommand.class
            ,Fst2ListCommand.class
            ,Fst2TxtCommand.class
            ,Grf2Fst2Command.class
            ,ImplodeTfstCommand.class
            ,InflectKrCommand.class
            ,Jamo2SylCommand.class
            ,LocateCommand.class
            ,LocateTfstCommand.class
            ,MergeBinCommand.class
            /* This is normal that MkdirCommand is not in this list,
             * since it's not a Unitex command */
            ,MultiFlexCommand.class
            ,NormalizeCommand.class
            ,PolyLexCommand.class
            ,RebuildTfstCommand.class
            ,ReconstrucaoCommand.class
            ,Reg2GrfCommand.class
            ,SortMorphCommand.class
            ,SortTxtCommand.class
            ,StatsCommand.class
            ,SufForm2RacCommand.class
            ,Syl2JamoCommand.class
            ,Table2GrfCommand.class
            ,TagsetNormTfstCommand.class
            ,TEI2TxtCommand.class
            ,Tfst2GrfCommand.class
            ,Tfst2UnambigCommand.class
            ,TokenizeCommand.class
            ,Txt2Fst2KrCommand.class
            ,Txt2TfstCommand.class
            ,UncompressCommand.class
            ,XAlignCommand.class
            ,XMLizerCommand.class
            };

    
	static HelpOnCommandFrame frame;

	public HelpOnCommandFrame() {
		super("Help on commands", true, true, true, true);
		JPanel top = new JPanel(new BorderLayout());
		final JList list=new JList(commands);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new DefaultListCellRenderer() {
		    @SuppressWarnings("unchecked")
            @Override
		    public Component getListCellRendererComponent(JList l, Object value, int index, boolean zz, boolean cellHasFocus) {
		        Class c=(Class)value;
		        String name=c.getSimpleName().substring(0,c.getSimpleName().lastIndexOf("Command"));
		        return super.getListCellRendererComponent(l, name, index, zz, cellHasFocus);
		    }
		});
        final ProcessOutputListModel stdout=new ProcessOutputListModel();
        final JList stdoutList=new JList(stdout);
        stdoutList.setCellRenderer(ProcessInfoFrame.myRenderer);
        top.add(new JScrollPane(stdoutList));
        
		list.addListSelectionListener(new ListSelectionListener() {
		    public void valueChanged(ListSelectionEvent e) {
		        @SuppressWarnings("unchecked")
                Class c=(Class)list.getSelectedValue();
		        if (c==null) return;
		        try {
                    CommandBuilder command=(CommandBuilder) c.newInstance();
                    stdout.removeAllElements();
                    final String[] comm = command.getCommandArguments();
                    Process p = Runtime.getRuntime().exec(comm);
                    new ProcessInfoThread(stdoutList, p
                            .getInputStream(), false,null,false,null).start();
                    try {
                        p.waitFor();
                    } catch (java.lang.InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } catch (InstantiationException e1) {
                    e1.printStackTrace();
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
		    }
		});
		top.add(new JScrollPane(list),BorderLayout.WEST);
		
		setContentPane(top);
		setSize(600,400);
		setVisible(true);
		frame = this;
		UnitexFrame.addInternalFrame(this,true);
		addInternalFrameListener(new InternalFrameAdapter() {
			public void internalFrameClosing(InternalFrameEvent e) {
				setVisible(false);
			}
		});
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	}

	public static void showFrame() {
	    if (frame==null) {
	        frame=new HelpOnCommandFrame();
	    }
	    frame.setVisible(true);
	}
	
}