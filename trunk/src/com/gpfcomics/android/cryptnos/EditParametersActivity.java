/* EditParametersActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          December 9, 2009
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This activity provides the primary interface for creating and editing
 * site parameters.  Which path you take depends on which "mode" this activity
 * is called in.  Creation mode starts with a blank slate and as along as the
 * site token changes from generation to generation, each set of parameters
 * is treated as new.  Editing mode starts with a set of parameters loaded
 * from the database and continues to work with them, updating their record
 * each time the Generate button is pressed.
 * 
 * This program is Copyright 2010, Jeffrey T. Darlington.
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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * The Edit Existing / Generate New Password activity.  This activity presents
 * the core user interface for generating passwords from a series of 
 * parameters.  When the incoming Intent includes a valid SiteParameters 
 * object, this view goes into Edit mode, allowing the user to modify those
 * settings and save them to the database.  If there are no SiteParameters
 * attached, we enter New mode, where a set of sane defaults allow us create
 * an all new set of parameters.  When the Generate button is tapped, the
 * parameters are saved to the database and the generated password is
 * displayed to the user.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class EditParametersActivity extends Activity {

	/** A constant specifying that this activity is being called in new
	 *  (generate) mode. */
	public static final int NEW_MODE = 0;
	/** A constant specifying that this activity is being called in edit
	 *  mode. */
	public static final int EDIT_MODE = 1;
	
	/** The current mode of this activity. */
	private int mode = NEW_MODE;
	
	/** The EditText view containing the site token. */
	private EditText txtSite = null;
	/** The EditText view containing the user's passphrase. */
	private EditText txtPassphrase = null;
	/** The Spinner view that allows the user to pick a cryptographic hash. */
	private Spinner hashSpinner = null;
	/** The EditText view containing the number of iterations of the has to
	 *  perform. */
	private EditText txtIterations = null;
	/** The Spinner view that lets the user choose what types of characters
	 *  to include or exclude from the final password. */
	private Spinner charTypesSpinner = null;
	/** The EditText view containing length restriction value. */
	private EditText txtCharLimit = null;
	/** The EditText view that will eventually contain the final password. */
	private EditText txtOutput = null;
	/** The Button that actually generates the password and saves it to
	 *  the database. */
	private Button btnGenerate = null;
	
	/** A reference to our top-level application */
	private CryptnosApplication theApp = null;
	/** Our database adapter for manipulating the database. */
	private ParamsDbAdapter dbHelper = null;
	/** The database row ID of the current note if we are in edit mode. */
	private long rowID = -1L;
	
	/** The site token used the last time the Generate button was pressed
	 *  in this session.  This is used to detect if the parameters have been
	 *  changed between button presses, to determine if we should save a
	 *  given set of parameters as a new item or to update an existing one. */
	private String lastSite = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_layout);

        // Get a reference to the top-level application, as well as the
        // DB helper:
        theApp = (CryptnosApplication)getApplication();
        dbHelper = theApp.getDBHelper();
        
        // Get handier references to our various input controls:
        txtSite = (EditText)findViewById(R.id.txtSite);
        txtPassphrase = (EditText)findViewById(R.id.txtPassphrase);
        hashSpinner = (Spinner)findViewById(R.id.spinHashes);
        txtIterations = (EditText)findViewById(R.id.txtIterations);
        charTypesSpinner = (Spinner)findViewById(R.id.spinCharTypes);
        txtCharLimit = (EditText)findViewById(R.id.txtCharLimit);
        txtOutput = (EditText)findViewById(R.id.txtOutput);
        btnGenerate = (Button)findViewById(R.id.btnGenerate);
        
        // Set the prompt for the top of the hash drop-down when it displays.
        // There's probably a way to set this in the layout XML, but I didn't
        // bother to look it up.
        hashSpinner.setPromptId(R.string.edit_hash_prompt);
        
        // Determine what mode we're being called in.  This activity can
        // be used to create a new set of parameters or to edit an existing
        // set.  Note that the default is new mode, and if an invalid mode
        // has been specified or the data cannot be restored for some reason,
        // that's what we go into as well.
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
        	mode = extras.getInt("mode");
        	if (mode < NEW_MODE || mode > EDIT_MODE) mode = NEW_MODE;
        	// If we're supposed to be in edit mode, we need data to edit:
        	if (mode == EDIT_MODE)
        	{
        		try
        		{
        			// Try to get the site token from the intent bundle.  If
        			// get got one, try to load the parameters from the
        			// database:
        			String site =
        				extras.getString(ParamsDbAdapter.DBFIELD_SITE);
        			if (site != null)
        			{
        				Cursor c = dbHelper.fetchRecord(site);
        				startManagingCursor(c);
        				// Got our record:
        				if (c.getCount() == 1)
        				{
        					// The row ID uniquely identifies us in the
        					// database, but isn't directly associated with
        					// the parameters themselves.  Even if we change
        					// the site, this will keep the record the same.
	        				rowID = c.getLong(0);
	        				// Get the parameters.  Note that we need both the
	        				// site key and the full encrypted string to get
	        				// everything back out.
	        				SiteParameters params =
	        					new SiteParameters(theApp,
	        						c.getString(1),
	        						c.getString(2));
	        				// Populate the GUI elements with the old data.
	        				// Note that the hash spinner and character limit
	        				// need special treatment, and that the site box
	        				// is disabled from editing.
	        				txtSite.setText(params.getSite());
	        				txtSite.setEnabled(false);
	        		        hashSpinner.setSelection(getHashPosition(params.getHash()));
	        		        txtIterations.setText(Integer.toString(params.getIterations()));
	        		        charTypesSpinner.setSelection(params.getCharTypes());
	        		        if (params.getCharLimit() != 0)
	        		        	txtCharLimit.setText(Integer.toString(params.getCharLimit()));
	        		        lastSite = params.getSite();
        				}
        				// If we didn't get exactly one row, throw an error:
        				else
        				{
        					Toast.makeText(this, R.string.error_bad_restore,
        						Toast.LENGTH_LONG).show();
        					mode = NEW_MODE;
        				}
        				c.close();
        			}
        			// If the site name was not in the intent bundle, throw
        			// an error:
        			else
        			{
    					Toast.makeText(this, R.string.error_bad_restore,
       						Toast.LENGTH_LONG).show();
        				mode = NEW_MODE;
        			}
        		}
        		// If anything else blew up, throw an error:
        		catch (Exception e1)
        		{
					Toast.makeText(this, R.string.error_bad_restore,
   						Toast.LENGTH_LONG).show();
    				mode = NEW_MODE;
        		}
        	}
        }
        
        // Depending on the mode, make a few last minute tweaks:
        switch (mode)
        {
	        case EDIT_MODE:
	        	this.setTitle(R.string.edit_title);
	        	break;
	        default:
	        	this.setTitle(R.string.new_title);
	            // If we're generating a new passphrase, set some sane
	        	// defaults.  For hashes, make the default SHA-1.  For the
	        	// number of iterations, one is usually sufficient.
	            hashSpinner.setSelection(1);
	            txtIterations.setText("1");
        }
        
        btnGenerate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Get our input values and cram them into some convenient
				// variables:
				String site = txtSite.getText().toString();
				String passphrase = txtPassphrase.getText().toString();
				String hash = (String)hashSpinner.getSelectedItem();
				int charType = charTypesSpinner.getSelectedItemPosition();
				int iterations = 1;
				int charLimit = 0;
				// If we were called in "new" mode and the site token has
				// changed since the last time we hit Generate, assume that
				// we're about to add a new site to the database, rather than
				// edit the previously saved one.  If the site token matches,
				// rowID should be populated with the row ID of the last save
				// and we'll default to editing the same parameters.
				if (mode == NEW_MODE && (lastSite == null ||
					site.compareTo(lastSite) != 0))
						rowID = -1;
				// A bit of error checking.  Make sure both the site and
				// passphrase boxes are populated.  We'll handle character
				// type and length restrictions further below.
				if (site == null || site.length() == 0)
					Toast.makeText(v.getContext(),
						R.string.error_edit_bad_site,
						Toast.LENGTH_LONG).show();
				else if (site.contains("|"))
					Toast.makeText(v.getContext(),
							R.string.error_site_has_pipe,
							Toast.LENGTH_LONG).show();
				else if (passphrase == null || passphrase.length() == 0)
					Toast.makeText(v.getContext(),
						R.string.error_edit_bad_password,
						Toast.LENGTH_LONG).show();
				else
				{
					// Create a string to store status messages in.  Strings are
					// immutable in Java, so what we start out with here isn't
					// important; we just need a reference that we'll "append"
					// to by concatenating and overwriting later.
					String messages = new String();
					try
					{
						// The character limit and iterations boxes take a bit
						// more work.  *SUPPOSEDLY* the restrictions we place in
						// the layout on the respective views should prohibit
						// non-numeric input, but we won't take that for granted.
						// Here we'll do some sanity checking to get things
						// moving.
						//
						// Start off by checking the character limit.  It's OK to
						// leave this blank; in that case, we'll default to zero,
						// which means no limit will be applied.  But if the limit
						// is there, try to parse it:
						if (txtCharLimit.getText().toString().length() > 0)
							charLimit =
								Integer.parseInt(txtCharLimit.getText().toString());
						// Now try to parse the iteration box value:
						iterations =
							Integer.parseInt(txtIterations.getText().toString());
						// Proceed only if the two values are legal:
						if (charLimit >= 0 && iterations > 0 &&
								iterations <= theApp.HASH_ITERATION_WARNING_LIMIT)
						{
							// For code reuse and modularization, the
							// SiteParameters class will actually do the heavy
							// lifting for us.  Create a new object, feed it all
							// of the inputs from the page, generate the password,
							// and display it to the user.
							SiteParameters params =
								new SiteParameters(theApp, site, 
									charType, charLimit, hash, iterations);
							String password = params.generatePassword(passphrase);
							txtOutput.setText(password);
							// Because they most likely wanted to copy the password to
							// the clipboard to paste it into a password form somewhere
							// else, go ahead and copy it into the clipboard now:
							ClipboardManager clippy =
								(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
							clippy.setText(password);
							// We'll assume both of those tasks were successful, so
							// start our status Toast stating such.  We'll append the
							// database status below.
							messages = getResources().getString(R.string.edit_gen_success);
							// Save parameters to the database.  If this is an
							// existing record, make sure we update the existing
							// record.  Otherwise, create a new one and take note
							// of its row ID so additional changes go into the
							// same record.
							try
							{
								// updateRecord() returns a Boolean while
								// createRecord() returns a long row ID (-1 on
								// failure).  To determine that everything worked,
								// assume true here, and then make sure this flag
								// is true *and* the row ID is positive.
								boolean success = true;
								if (rowID > 0) dbHelper.updateRecord(rowID, params);
								else {
									// If we're adding a record, set the site list
									// on the main app to dirty so it will get
									// rebuilt the next time it's needed:
									rowID = dbHelper.createRecord(params);
									theApp.setSiteListDirty();
								}
								if (success && rowID > 0)
								{
									messages = messages.concat(" ").concat(getResources().getString(R.string.edit_save_success));
									lastSite = site;
								}
								else
									messages = messages.concat(" ").concat(getResources().getString(R.string.error_edit_params_not_saved));
							}
							// If anything blew up up there, put its error
							// message into the message buffer:
							catch (Exception e1) { messages =
								messages.concat(" " + e1.getMessage()); }
							// Finally, display our accumulated status Toast to the
							// user:
							Toast.makeText(v.getContext(), messages,
								Toast.LENGTH_LONG).show();
						}
						// If the iterations or character limit parsing didn't
						// come up roses, show error messages.  Note that there's
						// also a generic "unknown" one here, just in case, but
						// it's probably irrelevant.
						else
						{
							if (iterations <= 0)
								Toast.makeText(v.getContext(),
									R.string.error_bad_iterations,
									Toast.LENGTH_LONG).show();
							else if (iterations > theApp.HASH_ITERATION_WARNING_LIMIT)
								Toast.makeText(v.getContext(),
									R.string.error_excessive_hashing,
									Toast.LENGTH_LONG).show();
							else if (charLimit < 0)
								Toast.makeText(v.getContext(),
										R.string.error_bad_charlimit,
										Toast.LENGTH_LONG).show();
							else Toast.makeText(v.getContext(),
										R.string.error_unknown,
										Toast.LENGTH_LONG).show();
						}
					}
					// Just in case something *really* blew up, put a generic
					// Toast up with the message of the error.  This should
					// probably be made more robust, but this will do for now.
					catch (Exception e2)
					{
						Toast.makeText(v.getContext(),
								e2.getMessage(),
								Toast.LENGTH_LONG).show();
					}
				}
			}
        });

        /**
         * What to do if the iterations box looses focus
         */
        txtIterations.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				// Did we just lose focus?
				if (!hasFocus) {
					try
					{
						// Try to get the number of iterations, which should
						// be populated and an integer greater than zero:
						String itersString = txtIterations.getText().toString();
						if (itersString != null && itersString.length() > 0)
						{
							// If the string was OK, parse it into an integer:
							int iterations = Integer.parseInt(itersString);
							// If we get a value zero or less, that's invalid,
							// so complain:
							if (iterations <= 0) {
								Toast.makeText(v.getContext(),
										R.string.error_bad_iterations,
										Toast.LENGTH_LONG).show();
							// In testing, I found iterations of 500 or more
							// started showing visible pauses on both the
							// emulator and my personal Motorola Droid, which
							// has a pretty beefy processor.  We don't want to
							// keep the user from going this high, but we
							// should at least warn them if it's going to take
							// a long time to do.
							} else if (iterations >= theApp.HASH_ITERATION_WARNING_LIMIT) {
								Toast.makeText(v.getContext(),
										R.string.error_excessive_hashing,
										Toast.LENGTH_LONG).show();
							}
						// If the string was empty, that's invalid:
						} else Toast.makeText(v.getContext(),
								R.string.error_bad_iterations,
								Toast.LENGTH_LONG).show();
					}
					// The most likely situation where this will blow up is
					// if the integer parsing fails.  In theory, this should
					// never happen; the iterations TextView has an integer
					// restriction, so non-integers should never get entered.
					// Still, belt and suspenders here...
					catch (Exception e) {
						Toast.makeText(v.getContext(),
								R.string.error_bad_iterations,
								Toast.LENGTH_LONG).show();
					}
				}
			}
        });
    
        txtSite.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				// Did we just lose focus?
				if (!hasFocus) {
					try
					{
						// Grab the site value:
						String site = txtSite.getText().toString();
						// Don't let the site be null or empty:
						if (site == null || site.length() == 0) {
							Toast.makeText(v.getContext(),
									R.string.error_site_empty,
									Toast.LENGTH_LONG).show();
						// We use the pipe character as a delimiter for storing
						// parameters, both in the DB and in import/export
						// files.  Don't let the user use this character in
						// the site name:
						} else if (site.contains("|")) {
							Toast.makeText(v.getContext(),
									R.string.error_site_has_pipe,
									Toast.LENGTH_LONG).show();
						}
					}
					// For now, ignore any excpetions:
					catch (Exception e) { }
				}
			}
        });
 
    }
    
    /**
     * Return the position of the specified hash name string in the hash
     * list array.
     * @param hash The hash name to look for.
     * @return The position of the hash in the array, or the default if
     * the hash could not be found.
     */
    private int getHashPosition(String hash)
    {
    	String[] hashes = getResources().getStringArray(R.array.hashList);
    	for (int i = 0; i < hashes.length; i++)
    		if (hash.compareTo(hashes[i]) == 0)
    			return i;
    	return 1;
    }

}
