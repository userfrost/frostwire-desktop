/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.limegroup.gnutella.gui.search;

import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.gui.tables.AbstractTableMediator;
import com.limegroup.gnutella.gui.tables.BasicDataLineModel;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.settings.SearchSettings;

/** 
 * Model for search results.
 *
 * Ensures that if new lines are added and they are similiar to old lines,
 * that the new lines are added as extra information to the existing lines,
 * instead of as brand new lines.
 */
class ResultPanelModel extends BasicDataLineModel<SearchResultDataLine, SearchResult> {
    
    /**
     * 
     */
    private static final long serialVersionUID = -2382156313320196261L;
    
    /**
     * The columns.
     */
    protected final SearchTableColumns COLUMNS = new SearchTableColumns();
    
    /**
     * HashMap for quick access to indexes based on SHA1 info.
     */
    private final Map<String, Integer> _indexes = new HashMap<String, Integer>();
    
    /**
     * The number of sources for this search.
     */
    private int _numSources;
    
    private int _numResults;
    
    /**
     * Constructs a new ResultPanelModel with the given MetadataModel.
     */
    ResultPanelModel() {
        super(SearchResultDataLine.class);
    }
    
    /**
     * Gets the columns used by this model.
     */
    SearchTableColumns getColumns() {
        return COLUMNS;
    }
    
    /**
     * Creates a new TableLine.
     */
    public SearchResultDataLine createDataLine() {
        return new SearchResultDataLine(COLUMNS);
    }
    
    /**
     * Gets the column at the specified index.
     */
    public LimeTableColumn getTableColumn(int idx) {
        return COLUMNS.getColumn(idx);
    }
    
    /**
     * Overrides default compare to move spam results to the bottom,
     * or to change the 'count' compare to use different values for
     * multicast or secure results.
     */
    public int compare(SearchResultDataLine ta, SearchResultDataLine tb) {
        int spamRet = compareSpam(ta, tb);
        if (spamRet != 0)
            return spamRet;

        //super.compare() will only sort Comparables and Strings.
        //since the Type column returns an Icon, we compare by hand using the file extension.
        if (_activeColumn == SearchTableColumns.TYPE_IDX) {
        	return AbstractTableMediator.compare(ta.getExtension(), tb.getExtension()) * _ascending;
        }  else if (!isSorted() || _activeColumn != SearchTableColumns.COUNT_IDX) {
            return super.compare(ta, tb);
        }
        else {
            return compareCount(ta, tb, false);
        }
    }
    
    /** 
     * Overrides the default remove to remove the index from the hashmap.
     *
     * @param row  the index of the row to remove.
     */
    public void remove(int row) {
        SearchResultDataLine tl = get(row);
        String sha1 = getHash(row);
        if(sha1 != null)
            _indexes.remove(sha1);
        super.remove(row);
        _numSources -= tl.getSeeds();
        _numResults -= 1;
        remapIndexes(row);
    }
    
    /**
     * Override default so new ones get added to the end
     */
    public int add(SearchResult o) {
        return add(o, getRowCount());
    }
    
    /**
     * Override to fix compile error on OSX.
     */
    public int add(SearchResultDataLine dl) {
        return super.add(dl);
	}
	
	/**
	 * Override to not iterate through each result.
	 */
	public Object refresh() {
        fireTableRowsUpdated(0, getRowCount());
        return null;
    }
    
    /**
     * Does a slow refresh, forcing the underlying results to have
     * 'update' called on them.
     */
    public void slowRefresh() {
        super.refresh();
    }

    /**
     * Maintains the indexes HashMap & MetadataModel.
     */    
    public int add(SearchResultDataLine tl, int row) {
        _numSources += tl.getSeeds();
        _numResults += 1;
        String sha1 = tl.getHash();
        if(sha1 != null)
            _indexes.put(sha1, new Integer(row));
        int addedAt = super.add(tl, row);
        remapIndexes(addedAt + 1);
        return addedAt;
    }
    
    /**
     * Gets the row this DataLine is at.
     */
    public int getRow(SearchResultDataLine tl) {
        String sha1 = tl.getHash();
        if(sha1 != null)
            return fastMatch(sha1);
        else
            return super.getRow(tl);
    }
    
    /**
     * Returns the number of sources found for this search.
     */
    int getTotalSources() {
        return _numSources;
    }
    
    /** 
     * Overrides the default sort to maintain the indexes HashMap,
     * according to the current sort column and order.
     */
    protected void doResort() {
        super.doResort();
        _indexes.clear(); // it's easier & quicker to just clear & re-input
        remapIndexes(0);
    }
    
    /**
     * Overrides the default clear to erase the indexes HashMap,
     * Metadata and Grouper.
     */
    public void clear() {
        simpleClear();
    }
    
    /**
     * Does nothing -- lines need no cleanup.
     */
    protected void cleanup() {}
    
    /**
     * Simple clear -- clears the number of sources & cached SHA1 indexes.
     * Calls super.clear to erase the stored lines.
     */
    protected void simpleClear() {
        _numSources = 0;
        _numResults = 0;
        _indexes.clear();
        super.clear();
    }
    
    /**
     * Remaps the indexes, starting at 'start' and going to the end of
     * the list.  This is needed for when rows are added to the middle of
     * the list to maintain the correct rows per objects.
     */
    private void remapIndexes(int start) {
        remapIndexes(start, getRowCount());
    }        
    
    /**
     * Remaps the indexes, starting at 'start' and going to 'end'.
     * This is useful for when we move a row from end to start or vice versa.
     */
    private void remapIndexes(int start, int end) {
        for (int i = start; i < end; i++) {
            String sha1 = getHash(i);
            if(sha1 != null)
                _indexes.put(sha1, new Integer(i));
        }
    }
    
    /**
     * Gets the SHA1 URN for a row.
     */
    private String getHash(int idx) {
        if(idx >= getRowCount())
            return null;
        return get(idx).getHash();
    }
    
    /** Compares the spam difference between the two rows. */
    private int compareSpam(SearchResultDataLine a, SearchResultDataLine b) {
        if (SearchSettings.moveJunkToBottom()) {
            if (SpamFilter.isAboveSpamThreshold(a)) {
                if (!SpamFilter.isAboveSpamThreshold(b)) {
                    return 1;
                }
            } else if (SpamFilter.isAboveSpamThreshold(b)) {
                return -1;
            }
        }
        
        return 0;
    }
    
    /**
     * Compares the count between two rows.
     */
    private int compareCount(SearchResultDataLine a, SearchResultDataLine b, boolean spamCompare) {
        if(spamCompare) {
            int spamRet = compareSpam(a, b);
            if(spamRet != 0)
                return spamRet;
        }
        
        int c1 = a.getSeeds() + ((a.getDisplayName().indexOf(YouTubePackageItemSearchResult.AAC_HIGH_QUALITY) == 0) ? 1000 : 0);
        int c2 = b.getSeeds() + ((b.getDisplayName().indexOf(YouTubePackageItemSearchResult.AAC_HIGH_QUALITY) == 0) ? 1000 : 0);
        
        int aSeId = a.getSearchEngine().getId();
        int bSeId = b.getSearchEngine().getId();
        
        if (aSeId == SearchEngine.YOUTUBE_ID) {
            c1 += 1000;
        }
        if (bSeId == SearchEngine.YOUTUBE_ID) {
            c2 += 1000;
        }
        if (aSeId == SearchEngine.SOUNDCLOUD_ID) {
            c1 += 200;
        }
        if (bSeId == SearchEngine.SOUNDCLOUD_ID) {
            c2 += 200;
        }
        
        return (c1 - c2) * _ascending;
    }
    
    /**
     * Fast match -- lookup in the table.
     */
    private int fastMatch(String sha1) {
        Integer idx = _indexes.get(sha1);
        if(idx == null)
            return -1;
        else
            return idx.intValue();
    }

    public int getTotalResults() {
        return _numResults;
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == SearchTableColumns.NAME_IDX || columnIndex == SearchTableColumns.SOURCE_IDX;
    }
}

