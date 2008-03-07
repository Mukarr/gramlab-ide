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

package fr.umlv.unitex.undo;

import java.util.*;

import javax.swing.undo.*;

import fr.umlv.unitex.*;

/**
 * class uses to save the state of the graph delete a group of boxes 
 * @author Decreton Julien
 */
public class DeleteBoxGroupEdit extends AbstractUndoableEdit {
	/** graph's boxes */
	ArrayList<GenericGraphBox>  boxes,
	/** boxes selected in the graph */
	selectedBoxes,
	/** boxes selected in the graph before adding a transition */
	oldSelectedBoxes;
	/** zone where the graph is drawn */
	GenericGraphicalZone zone;
	/** hashmap which store the boxes selected before the delete action and the transitions to its */
	HashMap<GenericGraphBox,ArrayList<GenericGraphBox>> selectedBoxesAndTransitionsTo;
	/** hashmap which store the boxes selected before the delete action and the transitions from its */
	HashMap<GenericGraphBox,ArrayList<GenericGraphBox>> selectedBoxesAndTransitionsFrom;

/**
 * 
 * @param selectedBoxes the boxes selected before the delete action
 * @param graphBoxes the boxes of the graph 
 * @param zone the zone where the graph is drawn
 */
	@SuppressWarnings("unchecked")
	public DeleteBoxGroupEdit(
            ArrayList selectedBoxes,
            ArrayList graphBoxes,
		GenericGraphicalZone zone) {
			
		selectedBoxesAndTransitionsTo = new HashMap<GenericGraphBox,ArrayList<GenericGraphBox>>();
		selectedBoxesAndTransitionsFrom = new HashMap<GenericGraphBox,ArrayList<GenericGraphBox>>();	
		this.selectedBoxes = selectedBoxes;		
		this.oldSelectedBoxes = (ArrayList)selectedBoxes.clone();
		this.boxes = graphBoxes;
		this.zone = zone;
		
		// get, for each deleted boxes the box which have transition to it		
		GenericGraphBox g;		
		for (Iterator it = selectedBoxes.iterator() ; it.hasNext(); ) {
			g = (GenericGraphBox)it.next();
			ArrayList<GenericGraphBox> boxeTransitionsTo = zone.getTransitionTo(g);
			ArrayList<GenericGraphBox> boxeTransitionsFrom = (ArrayList)g.getTransitions().clone();
			selectedBoxesAndTransitionsTo.put(g,boxeTransitionsTo);			
			selectedBoxesAndTransitionsFrom.put(g,boxeTransitionsFrom);
		}
		 
	}

	public void undo() {
		super.undo();
		GenericGraphBox g,g2;
		ArrayList transitionsToBoxe;
		ArrayList transitionsFromBoxe;
		Set keys = selectedBoxesAndTransitionsTo.keySet();
		
		// for each selected boxes before delete
		for( Iterator it = keys.iterator(); it.hasNext();){
			g = (GenericGraphBox)it.next();
			g.setTransitions(new ArrayList<GenericGraphBox>());
			transitionsToBoxe = selectedBoxesAndTransitionsTo.get(g);
			transitionsFromBoxe = selectedBoxesAndTransitionsFrom.get(g);
			boxes.add(g);
			
			// select this boxe
			g.setSelected(true);
			selectedBoxes.add(g);
			zone.initText(g.getContent());
			
			// add transitions which pointed on this box
			if( g.hasTransitionToItself() )
			 g.addTransitionTo(g);
			 
			for (Iterator it2 = transitionsToBoxe.iterator(); it2.hasNext();) {
			  g2 = (GenericGraphBox) it2.next();
			  if( !selectedBoxes.contains(g2) )
			  g2.onlyAddTransitionTo(g);
			}
			
			// add transitions from each boxe
			for (Iterator it2 = transitionsFromBoxe.iterator(); it2.hasNext();) {
			  g2 = (GenericGraphBox) it2.next();
			  g.onlyAddTransitionTo(g2);
			}
			
		}
		zone.repaint();
	}
	
	

	public void redo() {
		super.redo();
		
		int i, L;
			  GenericGraphBox g;
			  if (selectedBoxes.isEmpty())
				 return;
			  L= oldSelectedBoxes.size();      
      
			// delete each selected boxes before the delete action
			  for (i= 0; i < L; i++) {
				 g= oldSelectedBoxes.get(i);         
				 if (g.getType() == GenericGraphBox.NORMAL){			      
					boxes.remove(g);
				 }
				zone.removeTransitionsToSelected();
			  }
			  zone.unSelectAllBoxes();
			  zone.repaint();
	}
	
}
