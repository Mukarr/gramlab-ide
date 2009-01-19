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

package fr.umlv.unitex.process;

import java.io.File;

/**
 * @author S�bastien Paumier
 *
 */
public class CompressKrCommand extends CommandBuilder {
    
    public CompressKrCommand() {
    	super("CompressKr");
    }
    
    public CompressKrCommand name(File s) {
      protectElement(s.getAbsolutePath());
      return this;
    }

    public CompressKrCommand avecList() {
        element("-l");
        return this;
    }
    public CompressKrCommand modeSuf() {
        element("-s");
        return this;
    }
    public CompressKrCommand dest(File s) {
        element("-o");
        protectElement(s.getAbsolutePath());
        return this;
    }
    

}
