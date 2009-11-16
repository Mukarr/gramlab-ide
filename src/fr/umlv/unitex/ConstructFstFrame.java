/*
 * Unitex
 *
 * Copyright (C) 2001-2009 Universit� Paris-Est Marne-la-Vall�e <unitex@univ-mlv.fr>
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

package fr.umlv.unitex;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.border.*;

import fr.umlv.unitex.process.*;

/**
 * This class describes the "Construct Text FST" frame that offers to the user
 * to build the text automaton.
 * 
 * @author S�bastien Paumier
 *  
 */
public class ConstructFstFrame extends JDialog {

  JCheckBox reconstrucao = new JCheckBox("Build clitic normalization grammar (available only for Portuguese (Portugal))");
  JCheckBox normFst = new JCheckBox(
      "Apply the Normalization grammar");

  JCheckBox cleanFst = new JCheckBox("Clean Text FST");
  JCheckBox morphFst = new JCheckBox(
      "Use morpheme structures: available for Korean");

  JCheckBox elagFst = new JCheckBox("Normalize according to Elag tagset.def");
  JTextField normGrf = new JTextField(Config.getCurrentNormGraph().getAbsolutePath());
  
  /**
   * Creates and shows a new <code>ConstructFstFrame</code>.
   *  
   */
  public ConstructFstFrame() {
    super(UnitexFrame.mainFrame, "Construct the Text FST", true);
    setContentPane(constructPanel());
    pack();
    setResizable(false);
    setLocationRelativeTo(UnitexFrame.mainFrame);
    this.setVisible(true);
  }

  private JPanel constructPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(constructNormalizationPanel(), BorderLayout.NORTH);
    panel.add(constructDicPanel(), BorderLayout.CENTER);
    panel.add(constructButtonsPanel(), BorderLayout.SOUTH);
    return panel;
  }

  private JPanel constructNormalizationPanel() {
    JPanel normalizationPanel = new JPanel(new GridLayout(6,1));
    normalizationPanel.setBorder(new TitledBorder("Normalization"));
    boolean portuguese = Config.getCurrentLanguage().equals("Portuguese (Portugal)");
    reconstrucao.setEnabled(portuguese);
    reconstrucao.setSelected(portuguese);
    cleanFst.setSelected(true);
    boolean morphemeCase = Config.isKorean();
    morphFst.setEnabled(morphemeCase);
    morphFst.setSelected(morphemeCase);
    elagFst.setSelected(false);
    if(!morphemeCase){
      normFst.setSelected(true);
    } else {
    	normFst.setSelected(false);
    }

    normalizationPanel.add(reconstrucao);
    
    normalizationPanel.add(normFst);
    JPanel norm=new JPanel(new BorderLayout());
    JCheckBox foo=new JCheckBox("");
    norm.setBorder(BorderFactory.createEmptyBorder(0,foo.getPreferredSize().width,0,0));
    norm.add(normGrf,BorderLayout.CENTER);
    Action setAction = new AbstractAction("Set...") {
        public void actionPerformed(ActionEvent arg0) {
            JFileChooser chooser = Config.getNormDialogBox();
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                // we return if the user has clicked on CANCEL
                return;
            }
            normGrf.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    };
    JButton setNorm = new JButton(setAction);
    norm.add(setNorm,BorderLayout.EAST);
    normalizationPanel.add(norm);
    
    normalizationPanel.add(cleanFst);
    normalizationPanel.add(morphFst);
    normalizationPanel.add(elagFst);

    return normalizationPanel;
  }

  private JPanel constructDicPanel() {
    JPanel dicPanel = new JPanel(new GridLayout(2,1));
    dicPanel.setBorder(new TitledBorder(
          "Use Following Dictionaries previously constructed:"));
    dicPanel.add(new JLabel("The program will construct the text FST according to the DLF, DLC and tags.ind files"));
    dicPanel.add(new JLabel("previously built by the Dico program for the current text."));
    return dicPanel;
  }

  private JPanel constructButtonsPanel() {
    JPanel buttons = new JPanel(new GridLayout(1, 2));
    buttons.setBorder(new EmptyBorder(8, 8, 2, 2));
    buttons.setLayout(new GridLayout(1, 2));

    Action okAction = new AbstractAction("Construct FST") {

      public void actionPerformed(ActionEvent arg0) {
        setVisible(false);
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            File dir = Config.getCurrentSntDir();
            if (!dir.exists()) {
              // if the directory toto_snt does not exist, we
              // create
              // it
              dir.mkdir();
            }
            //File graph = new File(Config
            //		.getUserCurrentLanguageDir(), "Graphs");
            //graph = new File(graph, "Normalization");
            // we clean the text automaton files
            Config.deleteFileByName(new File(Config
                .getCurrentSntDir(), "sentence*.grf"));
            Config.deleteFileByName(new File(Config
                .getCurrentSntDir(), "cursentence.grf"));
            Config.deleteFileByName(new File(Config
                .getCurrentSntDir(), "cursentence.txt"));
            // we clean the ELAG files
            Config
          .deleteFileByName(new File(Config
              .getCurrentSntDir(),
              "currentelagsentence.grf"));
            Config
              .deleteFileByName(new File(Config
                    .getCurrentSntDir(),
                    "currentelagsentence.txt"));
            Config.deleteFileByName(new File(Config
                  .getCurrentSntDir(), "text-elag.tfst"));
            Config.deleteFileByName(new File(Config
                  .getCurrentSntDir(), "text-elag.tfst.bak"));
            Config.deleteFileByName(new File(Config
                    .getCurrentSntDir(), "text-elag.tind"));
            Config.deleteFileByName(new File(Config
                    .getCurrentSntDir(), "text-elag.tind.bak"));

            File graphDir = new File(Config
                .getUserCurrentLanguageDir(), "Graphs");
            File normalizationDir = new File(graphDir,
                "Normalization");
            File delaDir = new File(Config
                .getUnitexCurrentLanguageDir(), "Dela");
            File vProSuf = new File(normalizationDir,
                "V-Pro-Suf.fst2");
            File normalizePronouns = new File(normalizationDir,
                "NormalizePronouns.fst2");
            File raizBin = new File(delaDir, "Raiz.bin");
            File raizInf = new File(delaDir, "Raiz.inf");
            File futuroCondicionalBin = new File(delaDir,
                "FuturoCondicional.bin");
            File futuroCondicionalInf = new File(delaDir,
                "FuturoCondicional.inf");

            MultiCommands commands = new MultiCommands();
            if (normFst.isSelected() && reconstrucao.isSelected()
                && vProSuf.exists()
                && normalizePronouns.exists()
                && raizBin.exists()
                && futuroCondicionalBin.exists()
                && raizInf.exists()
                && futuroCondicionalInf.exists()) {
              // if the user has choosen both to build the clitic
              // normalization grammar
              // and to apply this grammar, and if the necessary
              // files for the
              // Reconstrucao program exist, we launch the
              // construction of this grammar
              LocateCommand locateCmd = new LocateCommand().snt(
                  Config.getCurrentSnt()).fst2(vProSuf)
                .alphabet().longestMatches().mergeOutputs()
                .noLimit();
              if (Config.isKorean() || Config.isKoreanJeeSun()) {
            	  locateCmd=locateCmd.korean();
      		}
              commands.addCommand(locateCmd);
              ReconstrucaoCommand reconstrucaoCmd = new ReconstrucaoCommand()
                .alphabet()
                .ind(
                    new File(Config.getCurrentSntDir(),
                      "concord.ind"))
                .rootDic(raizBin)
                .dic(futuroCondicionalBin)
                .fst2(normalizePronouns)
                .nasalFst2(
                    new File(graphDir,
                      "NasalSuffixPronouns.fst2"))
                .output(
                    new File(normalizationDir,
                      "Norm.grf"));
              commands.addCommand(reconstrucaoCmd);
              Grf2Fst2Command grfCommand = new Grf2Fst2Command()
                .grf(new File(normalizationDir, "Norm.grf"))
                .tokenizationMode().library();
              commands.addCommand(grfCommand);
                }

            Txt2TfstCommand txtCmd = new Txt2TfstCommand().text(
                Config.getCurrentSnt()).alphabet().clean(
                cleanFst.isSelected());
            File normFile=null;
            File normGrfFile=null;
            if (normFst.isSelected()) {
                String grfName = normGrf.getText();
                if (grfName.substring(grfName.length() - 3, grfName.length()).equalsIgnoreCase("grf")) {
                    // we must compile the grf
                    normGrfFile=new File(grfName);
                    Grf2Fst2Command grfCmd = new Grf2Fst2Command().grf(normGrfFile).enableLoopAndRecursionDetection(true).tokenizationMode();
                    commands.addCommand(grfCmd);
                    String fst2Name = grfName.substring(0, grfName.length() - 3);
                    fst2Name = fst2Name + "fst2";
                    normFile = new File(fst2Name);
                } else {
                    if (!(grfName.substring(grfName.length() - 4, grfName.length()).equalsIgnoreCase("fst2"))) {
                        // if the extension is nor GRF neither
                        // FST2
                        JOptionPane.showMessageDialog(null, "Invalid graph name extension !", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    normFile=normGrfFile=new File(grfName);
                }
                txtCmd=txtCmd.fst2(normFile);
                Config.setCurrentNormGraph(normGrfFile);
            }
            if (elagFst.isSelected()) {
                txtCmd=txtCmd.tagset(new File(Config.getCurrentElagDir(),"tagset.def"));
            }
            
            if (Config.isKorean() || Config.isKoreanJeeSun()) {
                File DecodingDir=new File(Config.getUserCurrentLanguageDir(),"Decoding");
                txtCmd=txtCmd.jamoTable(new File(Config.getUserCurrentLanguageDir(),"jamoTable.txt"))
                .jamoFst2(new File(DecodingDir,"uneSyl.fst2"));
            }
            commands.addCommand(txtCmd);
            TextAutomatonFrame.hideFrame();
            new ProcessInfoFrame(commands, true,
                new ConstructFstDo(),false);
          }
        });
        dispose();
      }
    };

    Action cancelAction = new AbstractAction("Cancel") {
      public void actionPerformed(ActionEvent arg0) {
        setVisible(false);
        dispose();
      }
    };

    JButton OK = new JButton(okAction);
    JButton CANCEL = new JButton(cancelAction);
    buttons.add(CANCEL);
    buttons.add(OK);
    return buttons;
  }

  class ConstructFstDo extends ToDoAbstract {

    public void toDo() {
      TextAutomatonFrame.showFrame();
      try {
        TextAutomatonFrame.getFrame().setIcon(false);
        TextAutomatonFrame.getFrame().setSelected(true);
        TextAutomatonFrame.frame.loadCurrSentence();
      } catch (java.beans.PropertyVetoException e) {
    	  e.printStackTrace();
      }
    }
  }

}
