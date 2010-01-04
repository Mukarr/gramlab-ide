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

import java.io.*;

import fr.umlv.unitex.*;

/**
 * @author S�bastien Paumier
 *  
 */
public class SortTxtCommand extends CommandBuilder {

	public SortTxtCommand() {
		super("SortTxt");
	}

	public SortTxtCommand file(File s) {
		protectElement(s.getAbsolutePath());
		return this;
	}

	public SortTxtCommand removeDuplicates(boolean remove) {
		element(remove ? "-n" : "-d");
		return this;
	}

	public SortTxtCommand reverse() {
		element("-r");
		return this;
	}

	public SortTxtCommand sortAlphabet() {
    protectElement("-o"+new File(Config
                .getUserCurrentLanguageDir(),"Alphabet_sort.txt").getAbsolutePath());
		return this;
	}

    public SortTxtCommand saveNumberOfLines(File file) {
        protectElement("-l"+file.getAbsolutePath());
        return this;
    }

    public SortTxtCommand thai() {
        element("--thai");
        return this;
    }

}