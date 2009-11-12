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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.table.AbstractTableModel;

public class StatisticsTableModelMode0 extends AbstractTableModel {

    private String[] columnNames;
    
    class Mode0Data {
        String left;
        String match;
        String right;
        int n;
    }
    
    private ArrayList<Mode0Data> data=new ArrayList<Mode0Data>();
    
    
    public StatisticsTableModelMode0(File file) {
        if (Config.isRightToLeftLanguage()) {
            columnNames=new String[] {"Right context","Match","Left context","Occurrences"};
        } else {
            columnNames=new String[] {"Left context","Match","Right context","Occurrences"};
        }
        try {
            FileInputStream stream=new FileInputStream(file);
            Scanner scanner=new Scanner(stream,"UTF-16");
            Pattern pattern=Pattern.compile("(.*)\t(.+)\t(.*)\t([0-9])+");
            while (scanner.hasNextLine()) {
                String line=scanner.nextLine();
                Matcher matcher=pattern.matcher(line);
                if (!matcher.matches()) {
                    throw new IOException("Invalid line in statistics file:\n"+line+"\n");
                }
                Mode0Data d=new Mode0Data();
                data.add(d);
                if (Config.isRightToLeftLanguage()) {
                    d.left=matcher.group(3);
                    d.right=matcher.group(1);
                } else {
                    d.left=matcher.group(1);
                    d.right=matcher.group(3);
                }
                d.match=matcher.group(2);
                d.n=Integer.parseInt(matcher.group(4));
            }
            scanner.close();
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    
    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return data.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Mode0Data d=data.get(rowIndex);
        switch (columnIndex) {
        case 0: return d.left;
        case 1: return d.match;
        case 2: return d.right;
        case 3: return d.n;
        default: throw new IllegalArgumentException("Invalid columnIndex: "+columnIndex);
        }
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 0: return String.class;
        case 1: return String.class;
        case 2: return String.class;
        case 3: return Integer.class;
        default: throw new IllegalArgumentException("Invalid columnIndex: "+columnIndex);
        }
    }

}
