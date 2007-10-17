 /*
  * Unitex
  *
  * Copyright (C) 2001-2007 Universit� Paris-Est Marne-la-Vall�e <unitex@univ-mlv.fr>
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
package fr.umlv.unitex.xalign;

import java.io.*;
import java.util.*;
import javax.swing.*;


/**
 * This is a loader for alignement concordance files.
 * 
 * @author S�bastien Paumier
 */
public class ConcordanceLoader {

	
	static class MatchedSentence {
		int sentenceNumber;
		Occurrence occurrence;
		
		MatchedSentence(int sentenceNumber,Occurrence occurrence) {
			this.sentenceNumber=sentenceNumber;
			this.occurrence=occurrence;
		}
	}
	
	
	/**
	 * This method loads a concordance file supposed to be a UTF8-encoded one,
	 * made of lines as follows:
	 * 
	 * A B C
	 * 
	 * where A is the number of the sentence containing the match,
	 * and B and C are the start and end positions of the match in characters
	 * from the beginning of the sentence.
	 */
	public static void load(final File file,final ConcordanceModel model) {
		new SwingWorker<Void,MatchedSentence>() {

			@Override
			protected Void doInBackground() throws Exception {
				Scanner scanner=null;
				try {
					scanner=new Scanner(file,"UTF8");
					int sentence,start,end;
					while (scanner.hasNextInt()) {
						sentence=scanner.nextInt();
						if (!scanner.hasNextInt()) {
							System.err.println("Invalid line in concordance file "+file.getName());
							return null;
						}
						start=scanner.nextInt();
						if (!scanner.hasNextInt()) {
							System.err.println("Invalid line in concordance file "+file.getName());
							return null;
						}
						end=scanner.nextInt();
						publish(new MatchedSentence(sentence,new Occurrence(start,end)));
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} finally {
					if (scanner!=null) scanner.close();
				}
				setProgress(100);
				return null;
			}
			
			@Override
			protected void process(java.util.List<MatchedSentence> chunks) {
				for (MatchedSentence s:chunks) {
					model.addMatch(s.sentenceNumber,s.occurrence);
				}
			}	
		}.execute();
	}
			
}
