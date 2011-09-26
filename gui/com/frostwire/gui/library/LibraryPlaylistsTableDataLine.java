package com.frostwire.gui.library;

import java.io.File;

import com.frostwire.alexandria.PlaylistItem;
import com.frostwire.gui.player.AudioPlayer;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.gui.tables.SizeHolder;

public final class LibraryPlaylistsTableDataLine extends AbstractLibraryTableDataLine<PlaylistItem> {
    
    /**
     * Starred column
     */
    static final int STARRED_IDX = 0;
    private static final LimeTableColumn STARRED_COLUMN = new LimeTableColumn(STARRED_IDX, "PLAYLIST_TABLE_STARRED", I18n.tr("Starred"), 20, true, false,
            false, PlaylistItemStar.class);
    
    /**
     * Title column
     */
    static final int TITLE_IDX = 1;
    private static final LimeTableColumn TITLE_COLUMN =
        new LimeTableColumn(TITLE_IDX, "PLAYLIST_TABLE_TITLE", I18n.tr("Title"),
                 80, true, PlaylistItemProperty.class);

    /**
     * Artist column
     */
    static final int ARTIST_IDX = 2;
    private static final LimeTableColumn ARTIST_COLUMN =
        new LimeTableColumn(ARTIST_IDX, "PLAYLIST_TABLE_ARTIST", I18n.tr("Artist"),
                 80, true, PlaylistItemProperty.class);
    
    /**
     * Length column (in hour:minutes:seconds format)
     */
    static final int LENGTH_IDX = 3;
    private static final LimeTableColumn LENGTH_COLUMN =
        new LimeTableColumn(LENGTH_IDX, "PLAYLIST_TABLE_LENGTH", I18n.tr("Length"),
                150, true, PlaylistItemProperty.class);
    
    /**
     * Album column
     */
    static final int ALBUM_IDX = 4;
    private static final LimeTableColumn ALBUM_COLUMN =
        new LimeTableColumn(ALBUM_IDX, "PLAYLIST_TABLE_ALBUM", I18n.tr("Album"),
                120, true, PlaylistItemProperty.class);
       
    /**
     * Track column
     */
    static final int TRACK_IDX = 5;
    private static final LimeTableColumn TRACK_COLUMN =
        new LimeTableColumn(TRACK_IDX, "PLAYLIST_TABLE_TRACK", I18n.tr("Track"),
                20, false, PlaylistItemProperty.class);
    
    /**
     * Genre column
     */
    static final int GENRE_IDX = 6;
    private static final LimeTableColumn GENRE_COLUMN =
        new LimeTableColumn(GENRE_IDX, "PLAYLIST_TABLE_GENRE", I18n.tr("Genre"),
                 80, true, PlaylistItemProperty.class);

    
    
    /**
     * Bitrate column info
     */
    static final int BITRATE_IDX = 7;
    private static final LimeTableColumn BITRATE_COLUMN =
        new LimeTableColumn(BITRATE_IDX, "PLAYLIST_TABLE_BITRATE",I18n.tr("Bitrate"),
                60, true, PlaylistItemProperty.class);
    
    /**
     * Comment column info
     */
    static final int COMMENT_IDX = 8;
    private static final LimeTableColumn COMMENT_COLUMN =
        new LimeTableColumn(COMMENT_IDX, "PLAYLIST_TABLE_COMMENT", I18n.tr("Comment"),
                20, false, PlaylistItemProperty.class);
           
    /**
     * Size column (in bytes)
     */
    static final int SIZE_IDX = 9;
    private static final LimeTableColumn SIZE_COLUMN =
        new LimeTableColumn(SIZE_IDX, "PLAYLIST_TABLE_SIZE", I18n.tr("Size"),
                80, false, PlaylistItemProperty.class);
    
    
    /**
     * TYPE column
     */
    static final int TYPE_IDX = 10;
    private static final LimeTableColumn TYPE_COLUMN = 
        new LimeTableColumn(TYPE_IDX, "PLAYLIST_TABLE_TYPE", I18n.tr("Type"),
                 40, true, PlaylistItemProperty.class);
    
    /**
     * YEAR column
     */
    static final int YEAR_IDX = 11;
    private static final LimeTableColumn YEAR_COLUMN =
        new LimeTableColumn(YEAR_IDX, "PLAYLIST_TABLE_YEAR", I18n.tr("Year"),
                 30, false, PlaylistItemProperty.class);
    
    /**
     * Total number of columns
     */
    static final int NUMBER_OF_COLUMNS = 12;

    /**
     * Number of columns
     */
    public int getColumnCount() { return NUMBER_OF_COLUMNS; }

    /**
     *  Coverts the size of the PlayListItem into readable form postfixed with
     *  Kb or Mb
     */
    private SizeHolder sizeHolder;
    
    private boolean exists;
    

    /**
     * Sets up the dataline for use with the playlist.
     */
    public void initialize(PlaylistItem item) {
        super.initialize(item);
        sizeHolder = new SizeHolder(item.getFileSize());
        exists = new File(item.getFilePath()).exists();
    }

    /**
     * Returns the value for the specified index.
     */
    public Object getValueAt(int idx) {
        boolean playing = isPlaying();
        switch(idx) {
            case STARRED_IDX:
                return new PlaylistItemStar(this, playing, exists);
            case ALBUM_IDX:
                return new PlaylistItemProperty(initializer.getTrackAlbum(), playing, exists);
            case ARTIST_IDX:
                return new PlaylistItemProperty(initializer.getTrackArtist(), playing, exists);
            case BITRATE_IDX:
                return new PlaylistItemProperty(initializer.getTrackBitrate(), playing, exists);
            case COMMENT_IDX:
                return new PlaylistItemProperty(initializer.getTrackComment(), playing, exists);
            case GENRE_IDX:
                return new PlaylistItemProperty(initializer.getTrackGenre(), playing, exists);
            case LENGTH_IDX:
                return new PlaylistItemProperty(LibraryUtils.getSecondsInDDHHMMSS((int) initializer.getTrackDurationInSecs()), playing, exists);
            case SIZE_IDX:
                return new PlaylistItemProperty(sizeHolder.toString(), playing, exists);
            case TITLE_IDX:
                return new PlaylistItemProperty(initializer.getTrackTitle(), playing, exists);
            case TRACK_IDX:
                return new PlaylistItemProperty(initializer.getTrackNumber(), playing, exists);
            case TYPE_IDX:
                return new PlaylistItemProperty(initializer.getFileExtension(), playing, exists);
            case YEAR_IDX:
                return new PlaylistItemProperty(initializer.getTrackYear(), playing, exists);
        }
        return null;
    }

    private boolean isPlaying() {
		if (initializer != null) {
			return AudioPlayer.instance().isThisBeingPlayed(initializer);
		}
		
		return false;
	}

	/**
     * Return the table column for this index.
     */
    public LimeTableColumn getColumn(int idx) {
        switch(idx) {
            case STARRED_IDX:       return STARRED_COLUMN; 
            case ALBUM_IDX:         return ALBUM_COLUMN;
            case ARTIST_IDX:        return ARTIST_COLUMN;
            case BITRATE_IDX:       return BITRATE_COLUMN;
            case COMMENT_IDX:       return COMMENT_COLUMN;
            case GENRE_IDX:         return GENRE_COLUMN;
            case LENGTH_IDX:        return LENGTH_COLUMN;
            case SIZE_IDX:          return SIZE_COLUMN;
            case TITLE_IDX:         return TITLE_COLUMN;
            case TRACK_IDX:         return TRACK_COLUMN;
            case TYPE_IDX:          return TYPE_COLUMN;
            case YEAR_IDX:          return YEAR_COLUMN;
        }
        return null;
    }
    
    public boolean isClippable(int idx) {
        return false;
    }
    
    public int getTypeAheadColumn() {
        return STARRED_IDX;
    }

    public boolean isDynamic(int idx) {
        return false;
    }
    
    /**
     * @return the PlayListItem for this table row
     */
    public PlaylistItem getPlayListItem() {
        return initializer;
    }
    
    /**
     * Creates a tool tip for each row of the playlist. Tries to grab any information
     * that was extracted from the Meta-Tag or passed in to the PlaylistItem as 
     * a property map
     */
    public String[] getToolTipArray(int col) {
        return new String[] {""} ;//initializer.getToolTips();
    }

    public File getFile() {
        return new File(initializer.getFilePath());
    }
}
