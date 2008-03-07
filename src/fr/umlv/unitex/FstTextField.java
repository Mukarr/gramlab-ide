 /*
  * Unitex
  *
  * Copyright (C) 2001-2008 Universit� Paris-Est Marne-la-Vall�e <unitex@univ-mlv.fr>
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
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.text.*;

/**
 * This class describes the text field used to get the box text in a sentence graph.
 * @author S�bastien Paumier
 *
 */
public class FstTextField extends JTextField {

   protected TextAutomatonFrame parent;
   protected boolean modified= false;
   protected SpecialPaste specialPaste;
   protected SpecialCopy specialCopy;
   protected SelectAll selectAll;
   protected Cut cut;
   protected Clipboard clip;

   /**
    * Constructs a new empty <code>FstTextField</code>. 
    * @param n number of columns
    * @param p frame that contains this component
    */
   public FstTextField(int n, TextAutomatonFrame p) {
      super(n);
      setEditable(false);
      modified= false;
      parent= p;
      clip= Toolkit.getDefaultToolkit().getSystemClipboard();
      setDisabledTextColor(Color.white);
      setBackground(Color.white);
      Keymap k= getKeymap();
      k= addKeymap("fsttextfield-keymap", k);
      k.removeKeyStrokeBinding(KeyStroke.getKeyStroke('v', Event.CTRL_MASK));
      k.removeKeyStrokeBinding(KeyStroke.getKeyStroke('V', Event.CTRL_MASK));
      k.removeKeyStrokeBinding(KeyStroke.getKeyStroke('c', Event.CTRL_MASK));
      k.removeKeyStrokeBinding(KeyStroke.getKeyStroke('C', Event.CTRL_MASK));
      k.removeKeyStrokeBinding(KeyStroke.getKeyStroke('x', Event.CTRL_MASK));
      k.removeKeyStrokeBinding(KeyStroke.getKeyStroke('X', Event.CTRL_MASK));
      k.removeKeyStrokeBinding(KeyStroke.getKeyStroke('a', Event.CTRL_MASK));
      k.removeKeyStrokeBinding(KeyStroke.getKeyStroke('A', Event.CTRL_MASK));

      selectAll= new SelectAll("select all");
      cut= new Cut("cut");
      specialPaste= new SpecialPaste("special-paste");
      specialCopy= new SpecialCopy("special-copy");

      k.addActionForKeyStroke(
         KeyStroke.getKeyStroke('v', Event.CTRL_MASK),
         specialPaste);
      k.addActionForKeyStroke(
         KeyStroke.getKeyStroke('V', Event.CTRL_MASK),
         specialPaste);
      k.addActionForKeyStroke(
         KeyStroke.getKeyStroke('c', Event.CTRL_MASK),
         specialCopy);
      k.addActionForKeyStroke(
         KeyStroke.getKeyStroke('C', Event.CTRL_MASK),
         specialCopy);
      k.removeKeyStrokeBinding(KeyStroke.getKeyStroke('m', Event.CTRL_MASK));
      k.removeKeyStrokeBinding(KeyStroke.getKeyStroke('M', Event.CTRL_MASK));
      k.addActionForKeyStroke(
         KeyStroke.getKeyStroke('a', Event.CTRL_MASK),
         selectAll);
      k.addActionForKeyStroke(
         KeyStroke.getKeyStroke('A', Event.CTRL_MASK),
         selectAll);
      k.addActionForKeyStroke(
         KeyStroke.getKeyStroke('x', Event.CTRL_MASK),
         cut);
      k.addActionForKeyStroke(
         KeyStroke.getKeyStroke('X', Event.CTRL_MASK),
         cut);
      this.setKeymap(k);

      addKeyListener(new MyKeyListener());
   }

   protected TextAutomatonFrame parent() {
      return TextAutomatonFrame.frame;
   }

   class SpecialCopy extends TextAction implements ClipboardOwner {
      public SpecialCopy(String s) {
         super(s);
      }

      public void actionPerformed(ActionEvent e) {
         if (parent().graphicalZone.selectedBoxes.isEmpty()
            || parent().graphicalZone.selectedBoxes.size() < 2) {
            // is there is no or one box selected, we copy normally
            copy();
            UnitexFrame.clip.setContents(null, this);
            return;
         }
         UnitexFrame.clip.setContents(
            new MultipleBoxesSelection(
               new MultipleSelection(
                  parent().graphicalZone.selectedBoxes,
                  true)),
            this);
      }

      public void lostOwnership(Clipboard c, Transferable d) {
    	  // nothing to do
      }
   }

   class SelectAll extends TextAction implements ClipboardOwner {
      public SelectAll(String s) {
         super(s);
      }

      public void actionPerformed(ActionEvent e) {
         if (!parent().graphicalZone.selectedBoxes.isEmpty()
            && parent().graphicalZone.selectedBoxes.size() == 1)
            selectAll();
         else
            parent().graphicalZone.selectAllBoxes();
      }

      public void lostOwnership(Clipboard c, Transferable d) {
    	  // nothing to do
      }
   }

   class Cut extends TextAction implements ClipboardOwner {
      public Cut(String s) {
         super(s);
      }

      public void actionPerformed(ActionEvent e) {
         if (!parent().graphicalZone.selectedBoxes.isEmpty()
            && parent().graphicalZone.selectedBoxes.size() == 1) {
            cut();
         } else {
            UnitexFrame.clip.setContents(
               new MultipleBoxesSelection(
                  new MultipleSelection(
                     parent().graphicalZone.selectedBoxes,
                     true)),
               this);
            parent().graphicalZone.removeSelected();
            setText("");
         }
      }

      public void lostOwnership(Clipboard c, Transferable d) {
    	  // nothing to do
      }
   }

   class SpecialPaste extends TextAction {
      public SpecialPaste(String s) {
         super(s);
      }

      public void actionPerformed(ActionEvent e) {
         Transferable data;
         MultipleSelection res= null;
         data= UnitexFrame.clip.getContents(this);
         try {
            if (data != null)
               res=
                  (MultipleSelection)data.getTransferData(
                     new DataFlavor("unitex/boxes", "Unitex dataflavor"));
         } catch (UnsupportedFlavorException e2) {
            e2.printStackTrace();
         } catch (IOException e2) {
            e2.printStackTrace();
         }
         if (res == null || FstTextField.this.modified == true) {
            // if there is no boxes to copy we do a simple paste
            paste();
            return;
         }
         res.n++;
         parent().graphicalZone.pasteSelection(res);
      }
   }

   /**
    * Sets the content of the text field 
    * @param s the new content
    */
   public void initText(String s) {
      modified= false;
      setEditable(true);
      setText(s);
      requestFocus();
      getCaret().setVisible(true);
      selectAll();
   }

   /**
    * Returns a new <code>ListDocument</code> object.
    * @return the <code>ListDocument</code>
    */
   public Document createDefaultModel() {
      return new ListDocument();
   }

   /**
    * Validates the content of the text field as the content of selected boxes. 
    * @return <code>true</code> if boxes have actually been modified, <code>false</code> otherwise 
    */
   public boolean validateTextField() {
      if (!hasChangedTextField())
         return false;
      if (isGoodText(getText())) {
         parent().graphicalZone.setTextForSelected(getText());
         parent().graphicalZone.unSelectAllBoxes();
         setText("");
         parent().graphicalZone.repaint();
         setEditable(false);
         parent().setModified(true);
         return true;
      }
      return false;
   }

   class MyKeyListener extends KeyAdapter {
      
      public void keyPressed(KeyEvent e) {
         if (e.isControlDown() || e.isAltDown()) {
            // if the control key or alt key is pressed, we do nothing: the event we be caught by the ActionListeners
            return;
         }
         if (e.getKeyCode() == 10)
            validateTextField();
         modified= true;
      }

   }

   /**
    * Tests if the content of the text field has changed. 
    * @return <code>true</code> if the content has changed, <code>false</code> otherwise 
    */
   public boolean hasChangedTextField() {
      return modified;
   }

   /**
    * Tests if a content is a valid content for a sentence graph box.
    * @param s the content to test 
    * @return <code>true</code> if the content is valid, <code>false</code> otherwise 
    */
   public boolean isGoodText(String s) {
      if (s.equals(""))
         return true;
      char ligne[]= new char[10000];
      int i, L;

      ligne= s.toCharArray();
      L= s.length();
      if (ligne[0] != '{')
         return true;

      i= 1;
      while (i < L && ligne[i] != ',') {
         if (ligne[i] == '\\') {
            if (i < L) {
               i++;
            } else {
               JOptionPane.showMessageDialog(
                  null,
                  "Unexpected '\\' at end of line",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
               return false;
            }

         }
         i++;
      }
      if (i == L) {
         JOptionPane.showMessageDialog(
            null,
            "No ',' delimiting inflected part from canonical part",
            "Error",
            JOptionPane.ERROR_MESSAGE);
         return false;
      }
      i++;
      while (i < L && ligne[i] != '.') {
         if (ligne[i] == '\\') {
            if (i < L) {
               i++;
            } else {
               JOptionPane.showMessageDialog(
                  null,
                  "Unexpected '\\' at end of line",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
               return false;
            }

         }
         i++;
      }
      if (i == L) {
         JOptionPane.showMessageDialog(
            null,
            "No '.' delimiting canonical part from grammatical informations",
            "Error",
            JOptionPane.ERROR_MESSAGE);
         return false;
      }
      i++;
      while (i < L && ligne[i] != '}') {
         if (ligne[i] == '\\') {
            if (i < L) {
               i++;
            } else {
               JOptionPane.showMessageDialog(
                  null,
                  "Unexpected '\\' at end of line",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
               return false;
            }

         }
         i++;
      }
      if (i == L) {
         JOptionPane.showMessageDialog(
            null,
            "No closing '}'",
            "Error",
            JOptionPane.ERROR_MESSAGE);
         return false;
      }
      return true;
   }

}
