 /*
  * Unitex
  *
  * Copyright (C) 2001-2006 Universit� de Marne-la-Vall�e <unitex@univ-mlv.fr>
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

package fr.umlv.unitex.undo;

import java.util.*;

import javax.swing.undo.*;

import fr.umlv.unitex.*;

/**
 * class uses to save the state of the graph before translate boxes
 * @author Decreton Julien
 */
public class TranslationGroupEdit extends AbstractUndoableEdit {

	/** boxes selected in the graph */
	private ArrayList selectedBoxes;	
	/** length of X, Y shift in pixels */
	private int x, y;

	/**
	 * @param selectedBoxes boes selected in the graph
	 * @param x length of X shift in pixels
	 * @param y length of Y shift in pixels
	 */
	public TranslationGroupEdit(ArrayList selectedBoxes, int x, int y) {
		this.selectedBoxes = (ArrayList) selectedBoxes.clone();
		this.x = x;
		this.y = y;

	}

	public void undo() {
		super.undo();
		GenericGraphBox g;
		int L = selectedBoxes.size();
		for (int i = 0; i < L; i++) {
			g = (GenericGraphBox) selectedBoxes.get(i);
			g.translate(-x, -y);
		}

	}

	public void redo() {
		super.redo();
		GenericGraphBox g;
		int L = selectedBoxes.size();
		for (int i = 0; i < L; i++) {
			g = (GenericGraphBox) selectedBoxes.get(i);
			g.translate(x, y);
		}

	}

}
