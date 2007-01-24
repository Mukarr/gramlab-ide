 /*
  * Unitex
  *
  * Copyright (C) 2001-2007 Universit� de Marne-la-Vall�e <unitex@univ-mlv.fr>
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

import java.io.File;

/**
 * @author S�bastien Paumier
 *  
 */
public class Fst2GrfCommand extends CommandBuilder {

	public Fst2GrfCommand() {
		super("Fst2Grf");
	}

	public Fst2GrfCommand automaton(File s) {
		protectElement(s.getAbsolutePath());
		return this;
	}

	public Fst2GrfCommand sentence(int n) {
		element("" + n);
		return this;
	}

	public Fst2GrfCommand sentence(String n) {
		Integer.parseInt(n);
		element("" + n);
		return this;
	}

	public Fst2GrfCommand output(String s) {
		protectElement(s);
		return this;
	}

	public Fst2GrfCommand font(String s) {
	    
		element("-f=" + s);
		return this;
	}

}