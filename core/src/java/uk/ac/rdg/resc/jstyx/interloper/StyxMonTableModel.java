/*
 * Copyright (c) 2005 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.jstyx.interloper;

import javax.swing.table.AbstractTableModel;
import java.util.Vector;

/**
 * Table model for the StyxMon
 * @todo Add a "hint" column containing info on what the message exchange is doing
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/02/24 09:07:12  jonblower
 * Added code to support filtering by pop-up menu
 *
 * Revision 1.1  2005/02/24 07:42:07  jonblower
 * Initial import
 *
 */
class StyxMonTableModel extends AbstractTableModel
{
    
    private final static String[] columnNames = {"Type", "Tag", "Filename",
        "TMessage", "RMessage"}; //, "Hint"};
        
    private Vector rows; // Vector of String arrays containing the actual
                         // underlying data (one String array per row)
    private Vector filter; // Contains the indices of all the rows in the "rows"
                           // Vector that are visible. If this is null, all the
                           // rows will be visible
    private String filterFilename;
    
    /**
     * Creates a new StyxMonTableModel
     */
    public StyxMonTableModel()
    {
        this.rows = new Vector();
        this.filter = null;
    }
    
    /**
     * Required by AbstractTableModel. Gets the number of rows of data.
     */
    public int getRowCount()
    {
        if (this.filter == null)
        {
            return this.rows.size();
        }
        else
        {
            return this.filter.size();
        }
    }
    
    /**
     * Required by AbstractTableModel. Gets the number of columns of data.
     */
    public int getColumnCount()
    {
        return columnNames.length;
    }
    
    /**
     * Required by AbstractTableModel. Gets the object at the given row and
     * column.
     * @return the data as a String
     * @throws ArrayIndexOutOfBoundsException if the given row or column are 
     * out of range
     */
    public synchronized Object getValueAt(int row, int column)
    {
        if (this.filter == null)
        {
            return this.getRowData(row, column)[column];
        }
        else
        {
            // Get the row in the real data
            Integer intRow = (Integer)this.filter.get(row);
            if (intRow == null)
            {
                throw new ArrayIndexOutOfBoundsException("The row index was out of range");
            }
            return this.getRowData(intRow.intValue(), column)[column];
        }
    }
    
    /**
     * Sets the cell at the given row and column to the given value, which must
     * be a string or a ClassCastException will be thrown
     * @throws ArrayIndexOutOfBoundsException if the given row or column
     * are out of range.
     * @throws ClassCastException if the given value was not a String
     */
    public synchronized void setValueAt(Object value, int row, int column)
    {
        String[] s = this.getRowData(row, column);
        s[column] = (String)value;
        this.fireTableCellUpdated(row, column);
    }
    
    /**
     * Gets the String array at the given row in the data model and also checks
     * that the given column index is valid.
     * @throws ArrayIndexOutOfBoundsException if the given row or column
     * are out of range
     */
    private synchronized String[] getRowData(int row, int column)
    {
        if (column > this.columnNames.length - 1)
        {
            throw new ArrayIndexOutOfBoundsException("The column index was out of range");
        }
        String[] rowData = (String[])this.rows.get(row);
        if (rowData == null)
        {
            throw new ArrayIndexOutOfBoundsException("The row index was out of range");
        }
        return rowData;
    }
    
    /**
     * @return the heading for the column with the given index, or the empty
     * string if the given column is out of range
     */
    public String getColumnName(int column)
    {
        if (column > this.columnNames.length - 1)
        {
            return "";
        }
        return this.columnNames[column];
    }
    
    /**
     * @return the class of data in the given column (always String)
     */
    public Class getColumnClass(int columnIndex)
    {
        return String.class;
    }
    
    /**
     * Adds a row to the end of the data model
     */
    public synchronized void addRow()
    {
        this.rows.add(new String[this.columnNames.length]);
        // Notify all listeners that a row has been inserted. If we do not do 
        // this, the StyxMon GUI will not be updated
        int newRowIndex = this.rows.size() - 1;
        this.fireTableRowsInserted(newRowIndex, newRowIndex);
    }
    
    /**
     * Adds data when a Tmessage arrives. Checks to see if the data should be
     * included in the current filtered view. The row will have already been
     * created by this point.
     */
    public synchronized void addTMessageData(int row, String messageName,
        int tag, String filename, String message)
    {
        this.setValueAt(messageName, row, 0);
        this.setValueAt("" + tag, row, 1);
        this.setValueAt(filename, row, 2);
        // Now check to see if this should be included in the filter
        if (this.filter != null && filename.equals(this.filterFilename))
        {
            this.filter.add(new Integer(row));
        }
        this.setValueAt(message, row, 3);
    }
    
    /**
     * @return true if the given row contains a message pair (i.e. both a
     * Tmessage and an Rmessage). Used by the StyxMonTableCellRenderer.
     */
    public boolean containsMessagePair(int row)
    {
        String[] rowData = this.getRowData(row, 0);
        // If the "Rmessage" column is empty, return false
        if (rowData[4] == null || rowData[4].equalsIgnoreCase(""))
        {
            return false;
        }
        return true;
    }
    
    /**
     * @return true if the row contains an Rerror message. Used by the
     * StyxMonTableCellRenderer. Simply looks to see if the contents of the
     * Rmessage column start with "ERROR" (not the best way perhaps as it relies
     * on the output of RerrorMessage.getFriendlyString()).
     */
    public boolean containsError(int row)
    {
        String[] rowData = this.getRowData(row, 0);
        // If the "Rmessage" column is empty, return false
        if (rowData[4] == null)
        {
            return false;
        }
        return rowData[4].startsWith("ERROR");
    }
    
    /**
     * Presents a view of the data that only includes rows that contain
     * the given filename. Does not affect the actual data and can be undone
     * with this.showAllData().
     */
    public synchronized void filterByFilename(String filename)
    {
        this.filterFilename = filename.trim();
        this.filter = new Vector();
        // Search through all the rows to find the rows that contain this filename
        for (int i = 0; i < this.rows.size(); i++)
        {
            String[] rowData = this.getRowData(i, 0);
            if (rowData[2].equals(this.filterFilename.trim()))
            {
                this.filter.add(new Integer(i));
            }
        }
        this.fireTableDataChanged();
    }
    
    /**
     * Removes any filters, allowing all the data in the model to be displayed
     */
    public synchronized void showAllData()
    {
        this.filter = null;
        this.fireTableDataChanged();
    }
    
    /**
     * @return true if the data model is currently filtered
     */
    public boolean isFiltered()
    {
        return (this.filter != null);
    }
    
}
