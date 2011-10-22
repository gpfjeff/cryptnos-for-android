/* ParamsDbAdapter.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          December 8, 2009
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This class provides an interface for reading and writing SiteParameters
 * data to and from the database.
 * 
 * I will readily admit that I'm not 100% comfortable with this code, and
 * that I didn't necessarily write it all myself.  It was originally copied
 * verbatim from Google's "Notepad" sample app, but was subsequently heavily
 * modified by me to fit Cryptnos' special needs.  Therefore, I'm not sure I
 * understand it as well as I should, and it's not commented nearly as well
 * as it should be.  My apologies in advance.
 * 
 * UPDATES FOR 1.1:  Added the deleteAllRecords() method to facilitate
 * deleting all records in the database at once.
 * 
 * UPDATES FOR 1.3.0:  Minor code clean-up and tweaks.  Added DB_ERROR
 * public constant.
 * 
 * This program is Copyright 2011, Jeffrey T. Darlington.
 * E-mail:  android_support@cryptnos.com
 * Web:     http://www.cryptnos.com/
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See theGNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
*/
package com.gpfcomics.android.cryptnos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * A database adaptor for reading and writing Cryptnos site parameter data to
 * and from the database.
 * @author Jeffrey T. Darlington
 * @version 1.3.0
 * @since 1.0
 */
public class ParamsDbAdapter {

	/* Public Constants *******************************************************/
	
    /** A constant representing the row ID database field. */
    public static final String DBFIELD_ROWID = "_id";
    /** A constant representing the site "key" or unique token. */
    public static final String DBFIELD_SITE = "site";
    /** A constant representing the site parameters database field. */
    public static final String DBFIELD_PARAMS = "params";
    /** A constant representing a failure.  Use this in comparisons when you are
     *  looking at a row ID to see if the action failed or not. */
    public static final long DB_ERROR = -1L;

	/* Private Constants ********************************************************/

    /** I *think* this is used for the SQLiteOpenHelper.onUpgrade() log and
     *  nowhere else.  That said, I'm not sure what other purpose this
     *  constant may serve. */
    private static final String TAG = "ParamsDbAdapter";
    /** Database creation SQL statement */
    private static final String DATABASE_CREATE_SQL =
            "create table parameters (_id integer primary key autoincrement, "
                    + "site text not null, params text not null);";
    /** A constant representing the name of the database. */
    private static final String DATABASE_NAME = "cryptnos";
    /** A constant representing the primary data table in the database. */
    private static final String DATABASE_TABLE = "parameters";
    /** The version of this database. */
    private static final int DATABASE_VERSION = 1;

	/* Private Members **********************************************************/
	
    /** An instance of our internal DatabaseHelper class*/
    private DatabaseHelper mDbHelper;
    /** A reference to the underlying SQLiteDatabase */
    private SQLiteDatabase mDb;
    /** Our calling Context. */
    private final Context mCtx;

    /**
     * This helper wraps a little bit of extra functionality around the
     * default SQLiteOpenHelper, giving it a bit more code specific to
     * how Cryptnos works.
     * @author Jeffrey T. Darlington
	 * @version 1.0
	 * @since 1.0
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

	/* Public methods: ***********************************************************/
	
    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public ParamsDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the Cryptnos database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public ParamsDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    /**
     * Close the Cryptnos database.
     */
    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new site parameter record using the SiteParameters object
     * provided.  If the record is successfully created, return the new row
     * ID for that note; otherwise return a -1 to indicate failure.
     * @param siteParams a SiteParameters object containing the parameter
     * information to store in the record.
     * @return The row ID of the created record or -1 if failed
     * @throws Exception Thrown when the parameters could not be saved.
     */
    public long createRecord(SiteParameters siteParams) throws Exception {
        try
        {
        	// Technically, when we create a record, we want to add something
        	// new.  This works great in Android when the rowID is the default
        	// primary field.  In our case, however, we really want the site
        	// key to be our driver, and it must be unique.  Thus, if we
        	// attempt to create a record that is actually a duplicate site,
        	// we don't want to create it but update it instead.  In order to
        	// facilitate this, we'll first check to see if the site parameters
        	// provided already exist in the database:
        	Cursor c = fetchRecord(siteParams.getKey());
        	// If we got a record, go into update mode instead:
        	if (c!= null && c.getCount() > 0) {
        		// Get the existing row's ID:
        		long rowID = c.getLong(0);
        		// Close the cursor, since we don't really need it:
        		c.close();
        		// And update the existing row with the new data.  Hurray for
        		// code reuse!
        		if (updateRecord(rowID, siteParams)) return rowID;
        		else return DB_ERROR;
        	// If the site doesn't exist, create a new one:
        	} else {
        		// Close out the cursor:
        		if (c!= null) c.close();
        		// Perform the insert:
		    	ContentValues initialValues = new ContentValues();
		        initialValues.put(DBFIELD_SITE, siteParams.getKey());
		        initialValues.put(DBFIELD_PARAMS, siteParams.exportEncryptedString());
		        return mDb.insert(DATABASE_TABLE, null, initialValues);
        	}
        }
        catch (Exception e)
        {
        	throw new Exception(mCtx.getResources().getString(R.string.error_bad_save));
        }
    }

    /**
     * Delete the note with the given row ID
     * 
     * @param rowID The row ID of parameters to delete
     * @return True if deleted, false otherwise
     */
    public boolean deleteRecord(long rowId) {
        return mDb.delete(DATABASE_TABLE, DBFIELD_ROWID + "=" + rowId, null)
        	> 0;
    }

    /**
     * Delete the note with the given site token
     * 
     * @param site The site token of the parameters to delete
     * @return True if deleted, false otherwise
     */
    public boolean deleteRecord(String site) {
        return mDb.delete(DATABASE_TABLE, DBFIELD_SITE + "='" + site + "'",
        	null) > 0;
    }
    
    /**
     * Delete all records from the database
     * @return A count of the number of sites deleted
     */
    public int deleteAllRecords() {
    	return mDb.delete(DATABASE_TABLE, "1", null);
    }

    /**
     * Return a Cursor over the list of all parameter items in the database
     * 
     * @return Cursor over all items
     */
    public Cursor fetchAllSites() {
        return mDb.query(DATABASE_TABLE, new String[] {DBFIELD_ROWID,
        	DBFIELD_SITE, DBFIELD_PARAMS}, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the site parameter item that matches the
     * given row ID
     * 
     * @param rowID The row ID of parameters to retrieve
     * @return Cursor positioned to matching item, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchRecord(long rowId) throws SQLException {
        Cursor mCursor =
	        mDb.query(true, DATABASE_TABLE, new String[] {DBFIELD_ROWID,
	        	DBFIELD_SITE, DBFIELD_PARAMS}, DBFIELD_ROWID + "=" + rowId,
	        		null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Return a Cursor positioned at the site parameter item that matches the
     * given site token
     * 
     * @param site The site token of the parameters to retrieve
     * @return Cursor positioned to matching item, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchRecord(String site) throws SQLException {
        Cursor mCursor =
	        mDb.query(true, DATABASE_TABLE, new String[] {DBFIELD_ROWID,
	        	DBFIELD_SITE, DBFIELD_PARAMS}, DBFIELD_SITE + "='" + site + "'",
	        		null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Update the site parameter record using the details provided. The item
     * to be updated is specified using the row ID, and it is altered to use
     * the title and body values passed in.
     * 
     * @param The row ID of the item to update
     * @param siteParams A SiteParameters object containing the parameter data
     * @return True if the item was successfully updated, false otherwise
     * @throws Exception Thrown when the site parameters cannot be saved.
     */
    public boolean updateRecord(long rowId, SiteParameters siteParams)
    	throws Exception
    {
    	try
    	{
	        ContentValues args = new ContentValues();
	        args.put(DBFIELD_SITE, siteParams.getKey());
	        args.put(DBFIELD_PARAMS, siteParams.exportEncryptedString());
	        return mDb.update(DATABASE_TABLE, args, DBFIELD_ROWID + "=" +
	        	rowId, null) > 0;
    	}
    	catch (Exception e)
    	{
    		throw new Exception(mCtx.getResources().getString(R.string.error_bad_update));
    	}
    }
    
    /**
     * Get the count of all records currently in the database.
     * @return An integer representing the number of records in the database.
     */
    public int recordCount()
    {
    	// Asbestos underpants:
    	try
    	{
    		// Apparently there isn't a way to query an Android database
    		// without using a cursor, so that's what we'll do.  Note that
    		// we name the result "column" because that's the only way I
    		// can tell to get the value back out.
	    	Cursor c = mDb.rawQuery("select count(*) as count from " +
	    			DATABASE_TABLE + ";", null);
	    	// Assuming that didn't blow up and we got something useful,
	    	// grab the count value from the result set, close the cursor,
	    	// and return the count:
	        if (c != null)
	        {
	        	c.moveToFirst();
	        	int count = c.getInt(c.getColumnIndex("count"));
	        	c.close();
	        	return count;
	        }
	        // If the cursor failed, obviously there aren't any records:
	        else return 0;
    	}
    	// If anything blew up, we'll assume there are no records:
    	catch (Exception e) { return 0; }
    }

}
