/*
 * Unitex
 *
 * Copyright (C) 2001-2011 Université Paris-Est Marne-la-Vallée <unitex@univ-mlv.fr>
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

package fr.umlv.unitex.text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractListModel;
import javax.swing.SwingWorker;

import fr.umlv.unitex.io.Encoding;


/**
 * This is a model for representing a text file as the list of its paragraphs.
 * Paragraphs are delimited by new lines. It uses a mapped file to avoid
 * to store large data in memory.
 *
 * @author Sébastien Paumier
 */
public class TextAsListModelImpl extends AbstractListModel {

    private MappedByteBuffer mappedBuffer;
    private SwingWorker<Void, Interval> worker;
    private Interval selection;
    private String content = null;
    private FileChannel channel;
    private FileInputStream stream;
    private File file;
    private boolean dataFromFile;
    ByteBuffer parseBuffer;
    Encoding encoding;

    private ArrayList<Interval> lines=new ArrayList<Interval>();

    public void load(File f) {
    	load(f,null);
    }

    public void load(File f,final Pattern filter) {
        content = null;
        dataFromFile = true;
        this.file = f;
        long fileLength = file.length();
        this.encoding=Encoding.getEncoding(f);
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        channel = stream.getChannel();
        try {
        	/* We skip the BOM for UTF16 encodings */
        	if (encoding==Encoding.UTF16LE || encoding==Encoding.UTF16BE) {
        		mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 2, fileLength - 2);
        	} else {
        		mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
        	}
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        /* parseBuffer must NOT be a final variable, because it would keep
         * a reference on the buffer that would never be null, so that the
         * buffer can never be garbage collected. As a consequence, the
         * underlying file mapping would never be released.
         */
        parseBuffer = mappedBuffer.duplicate();
        worker = new SwingWorker<Void,Interval>() {

            @Override
            protected Void doInBackground() throws Exception {
                int lastStartInChars = 0;
                int lastStartInBytes=0;
                StringBuilder builder1=new StringBuilder();
                int pos;
                for (pos = 0; parseBuffer.position()<parseBuffer.capacity(); pos = pos + 1) {
                	int posInBuffer=parseBuffer.position();
                	int c=encoding.readChar(parseBuffer);
                    if (c == '\n') {
                        // if we have an end-of-line
                    	boolean publish=false;
                    	if (filter==null) {
                    		publish=true;
                    	} else {
                    		Matcher m=filter.matcher(builder1.toString());
                    		if (m.matches()) publish=true;
                    	}
                        builder1.setLength(0);
                        setProgress((int) ((long) parseBuffer.position() * 100 / parseBuffer.capacity()));
                        if (publish) {
                        	publish(new Interval(lastStartInBytes,posInBuffer,lastStartInChars,pos));
                        }
                    	lastStartInChars = pos + 1;
                    	lastStartInBytes=parseBuffer.position();
                    } else {
                    	if (filter!=null && c!='\r') builder1.append(c);
                    }
                }
                if (lastStartInBytes < (parseBuffer.capacity() - 1)) {
                    publish(new Interval(lastStartInBytes,parseBuffer.position(),lastStartInChars,pos));
                    setProgress(100);
                }
                return null;
            }

            @SuppressWarnings("synthetic-access")
            @Override
            protected void process(java.util.List<Interval> chunks) {
            	int oldSize=lines.size();
            	for (Interval i:chunks) {
            		lines.add(i);
            	}
                fireIntervalAdded(this, oldSize,lines.size());
            }

        };
        worker.execute();
    }

    public TextAsListModelImpl() {
        super();
        dataFromFile = false;
        setText("");
    }

    public void setText(String string) {
        dataFromFile = false;
        int size=lines.size();
        lines.clear();
        fireIntervalRemoved(this, 0, size);
        content = string;
        fireIntervalAdded(this, 0, 0);
    }

    public int getSize() {
        if (content != null) return 1;
        return lines.size();
    }


    private final StringBuilder builder = new StringBuilder(40 * 100);

    /**
     * Returns the text corresponding to the paragraph #i.
     */
    public String getElementAt(int i) {
        if (!dataFromFile) return content;
        Interval interval = getInterval(i);
        builder.setLength(0);
        int start = interval.getStartInChars();
        int end = interval.getEndInChars();
        mappedBuffer.position(interval.getStartInBytes());
        for (int pos = start; pos <= end; pos++) {
        	int c=encoding.readChar(mappedBuffer);
            if (c != '\r' && c != '\n') {
                builder.append((char)c);
            }
        }
        return builder.toString();
    }


    Interval getInterval(int i) {
        if (!dataFromFile || i>=lines.size()) {
            return null;
        }
        return lines.get(i);
    }

    public Interval getSelection() {
        if (!dataFromFile) return null;
        return selection;
    }

    public void setSelection(Interval selection) {
        this.selection = selection;
        fireContentsChanged(this, 0, getSize() - 1);
    }

    /**
     * Just to ask the view to refresh.
     */
    public void refresh() {
        fireContentsChanged(this, 0, getSize());
    }

    /**
     * We want to get the number of the interval that contains the given position in chars.
     *
     * @param position
     * @return the number of the interval, or -1 if the position is not contained
     *         in an interval of the model
     */
    public int getElementContainingPositionInChars(int position) {
        if (!dataFromFile) return -1;
        if (position < 0) return -1;
        /* We cache the size and length in case there is a publish
         * operation that updates the array */
        for (int i=0;i<lines.size();i++) {
        	Interval tmp=lines.get(i);
        	if (position>=tmp.getStartInChars() && position<=tmp.getEndInChars()) return i;
        }
        return -1;
    }

    public String getContent() {
        return content;
    }

    public void reset() {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        if (mappedBuffer != null) mappedBuffer = null;
        if (parseBuffer != null) parseBuffer = null;
        System.gc();
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            channel = null;
        }
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stream = null;
        }
        setText("");
        int size=lines.size();
        lines.clear();
        fireIntervalRemoved(this,0,size);
    }

    
    public int getNextMatchedElement(int currentPosition,Pattern p) {
    	int n=getSize();
    	for (int i=currentPosition+1;i<n;i++) {
    		Matcher m=p.matcher(getElementAt(i));
    		if (m.matches()) return i;
    	}
    	return -1;
    }

    public int getPreviousMatchedElement(int currentPosition,Pattern p) {
    	for (int i=currentPosition-1;i>=0;i--) {
    		Matcher m=p.matcher(getElementAt(i));
    		if (m.matches()) return i;
    	}
    	return -1;
    }
}
