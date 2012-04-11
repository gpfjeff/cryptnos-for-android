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
 * UPDATES FOR 1.2.0:  Added Help option menu.  Added prompt for character
 * type drop-down.
 * 
 * UPDATES FOR 1.2.1:  Minor UI enhancements to make things prettier and (hopefully)
 * easier to use.
 * 
 * UPDATES FOR 1.2.2:  Added code to temporarily save and restore state if the
 * user rotates the screen or slides out a physical keyboard.  Android handles
 * these events by destroying the activity and rebuilding it, which results in
 * the master and generated passwords being wiped out.  It should now hold onto
 * these values temporarily and restore them once the rebuild is complete.
 * 
 * UPDATES FOR 1.2.4:  Enabled "copy to clipboard" setting added.  The user can
 * no enable and disable copying the generated password to the clipboard.
 * 
 * UPDATES FOR 1.3.0:  Enabled "show master passwords" setting.
 *
 * UPDATES FOR 1.3.1:  Enabled "clear passwords on focus loss" setting.
 *
 * This program is Copyright 2012, Jeffrey T. Darlington.
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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
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
 * @version 1.3.1
 * @since 1.0
 */
public class EditParametersActivity extends Activity {

	/** A constant specifying that this activity is being called in new
	 *  (generate) mode. */
	public static final int MODE_NEW = 0;
	/** A constant specifying that this activity is being called in edit
	 *  mode. */
	public static final int MODE_EDIT = 1;
	
	/** A constant indicating the Help option menu item. */
	public static final int OPTMENU_HELP = Menu.FIRST;

	/** The current mode of this activity. */
	private int mode = MODE_NEW;
	
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
	/** The Spinner containing length restriction value. */
	private Spinner spinCharLimit = null;
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
	private long rowID = ParamsDbAdapter.DB_ERROR;
	
	/** The site token used the last time the Generate button was pressed
	 *  in this session.  This is used to detect if the parameters have been
	 *  changed between button presses, to determine if we should save a
	 *  given set of parameters as a new item or to update an existing one. */
	private String lastSite = null;
	/** This flag determines whether or not we should clear the master and
	 *  generated password boxes when the activity is displayed.  There are
	 *  several reasons why this may or may not happen, depending on the user's
	 *  preferences and whether we're handling a configuration change like
	 *  rotating the screen.  This is the "final" flag that determines whether
	 *  or not the clearing takes place and this will be set to true or false
	 *  based on all these factors.  By default, this will be false. */
	private boolean clearPasswords = false;
	
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
        spinCharLimit = (Spinner)findViewById(R.id.spinCharLimit);
        txtOutput = (EditText)findViewById(R.id.txtOutput);
        btnGenerate = (Button)findViewById(R.id.btnGenerate);
        
        // Determine whether or not the user has specified to show or hide
        // master passwords and toggle the behavior of the master passphrase
        // box accordingly:
        if (theApp.showMasterPasswords())
        	txtPassphrase.setTransformationMethod(null);
        
        // Get the user's preference of whether the password boxes should be
        // cleared when Cryptnos goes into the background:
        clearPasswords = theApp.clearPasswordsOnFocusLoss();

        // Set the prompt for the top of the drop-downs when they display.
        // There's probably a way to set this in the layout XML, but I didn't
        // bother to look it up.
        hashSpinner.setPromptId(R.string.edit_hash_prompt);
        charTypesSpinner.setPromptId(R.string.edit_chartypes_prompt);
        spinCharLimit.setPromptId(R.string.edit_charlimit_prompt);
        
    	// Check to see if we currently have a saved state for this activity,
    	// such as before a screen rotate or before the user slides out a
    	// physical keyboard.  If such a state exists, we want to restore it:
        final ParameterViewState state =
        	(ParameterViewState)getLastNonConfigurationInstance();
        // We found a previously saved state, so re-populate the form:
        if (state != null) {
        	mode = state.getMode();
        	lastSite = state.getLastSite();
        	rowID = state.getRowID();
        	txtSite.setText(state.getSite());
	        hashSpinner.setSelection(getHashPosition(state.getHash()));
	        txtIterations.setText(state.getIterations());
	        charTypesSpinner.setSelection(state.getCharTypes());
	        rebuildCharLimitSpinner(state.getHash());
	        spinCharLimit.setSelection(state.getCharLimit(), true);
	        txtPassphrase.setText(state.getMasterPassword());
	        txtOutput.setText(state.getGeneratedPassword());
        	// Since we're restoring our state from a configuration change, we
        	// don't want the password boxes to be cleared, regardless of the
        	// user's preference.  Override whatever got set above and force
        	// this clearing not to take place.  See onResume() below for
        	// more details.
        	clearPasswords = false;
        // There was no saved state, so proceed to the next step:
        } else {
            // Determine what mode we're being called in.  This activity can
            // be used to create a new set of parameters or to edit an existing
            // set.  Note that the default is new mode, and if an invalid mode
            // has been specified or the data cannot be restored for some reason,
            // that's what we go into as well.
            Bundle extras = getIntent().getExtras();
            if (extras != null)
            {
            	mode = extras.getInt("mode");
            	if (mode < MODE_NEW || mode > MODE_EDIT) mode = MODE_NEW;
            	// If we're supposed to be in edit mode, we need data to edit:
            	if (mode == MODE_EDIT)
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
    	        				// Note that the spinners need special treatment, and
    	        				// that the site box is disabled from editing.
    	        				txtSite.setText(params.getSite());
    	        				txtSite.setEnabled(false);
    	        		        hashSpinner.setSelection(getHashPosition(params.getHash()));
    	        		        txtIterations.setText(Integer.toString(params.getIterations()));
    	        		        charTypesSpinner.setSelection(params.getCharTypes());
    	        		        rebuildCharLimitSpinner(params.getHash());
    	        		        if (params.getCharLimit() < 0 ||
    	        		        		params.getCharLimit() > theApp.getEncodedHashLength(params.getHash()))
    	        		        	spinCharLimit.setSelection(0, true);
    	        		        else
    	        		        	spinCharLimit.setSelection(params.getCharLimit(), true);
    	        		        lastSite = params.getSite();
            				}
            				// If we didn't get exactly one row, throw an error:
            				else
            				{
            					Toast.makeText(this, R.string.error_bad_restore,
            						Toast.LENGTH_LONG).show();
            					mode = MODE_NEW;
            				}
            				c.close();
            			}
            			// If the site name was not in the intent bundle, throw
            			// an error:
            			else
            			{
        					Toast.makeText(this, R.string.error_bad_restore,
           						Toast.LENGTH_LONG).show();
            				mode = MODE_NEW;
            			}
            		}
            		// If anything else blew up, throw an error:
            		catch (Exception e1)
            		{
    					Toast.makeText(this, R.string.error_bad_restore,
       						Toast.LENGTH_LONG).show();
        				mode = MODE_NEW;
            		}
            	}
            }
            // If new mode was specified anywhere above, set some sane
            // defaults.  For hashes, make the default SHA-1.  For the
        	// number of iterations, one is usually sufficient.  For
        	// the character limit, set it to unrestricted.  Note that
            // the location of this check means that these tasks do not
            // get performed if we're restoring a previous state; in that
            // case, we want the values the user previously had, not the
            // defaults.
            if (mode == MODE_NEW) {
                hashSpinner.setSelection(1);
                txtIterations.setText("1");
                rebuildCharLimitSpinner("SHA-1");
                spinCharLimit.setSelection(0, true);
            }
        }
        
        // Depending on the mode, make a few last minute tweaks:
        switch (mode)
        {
	        case MODE_EDIT:
	        	setTitle(R.string.edit_title);
	        	txtSite.setEnabled(false);
	        	break;
	        default:
	        	setTitle(R.string.new_title);
        }
        
        btnGenerate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Get our input values and cram them into some convenient
				// variables:
				String site = txtSite.getText().toString();
				String passphrase = txtPassphrase.getText().toString();
				String hash = (String)hashSpinner.getSelectedItem();
				int charType = charTypesSpinner.getSelectedItemPosition();
				int charLimit = spinCharLimit.getSelectedItemPosition();
				int iterations = 1;
				// If we were called in "new" mode and the site token has
				// changed since the last time we hit Generate, assume that
				// we're about to add a new site to the database, rather than
				// edit the previously saved one.  If the site token matches,
				// rowID should be populated with the row ID of the last save
				// and we'll default to editing the same parameters.
				if (mode == MODE_NEW && (lastSite == null ||
					site.compareTo(lastSite) != 0))
						rowID = ParamsDbAdapter.DB_ERROR;
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
						// Try to parse the iteration box value:
						iterations =
							Integer.parseInt(txtIterations.getText().toString());
						// Proceed only if the two values are legal:
						if (charLimit >= 0 && iterations > 0 &&
								iterations <= CryptnosApplication.HASH_ITERATION_WARNING_LIMIT)
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
							// If the user chose to copy the password to the clipboard,
							// go ahead and copy it now:
							if (theApp.copyPasswordsToClipboard()) {
								ClipboardManager clippy = ClipboardManager.newInstance(theApp);
								clippy.setText(password);
								// We'll assume both of those tasks were successful, so
								// start our status Toast stating such.  We'll append the
								// database status below.
								messages = getResources().getString(R.string.edit_gen_success);
							// If the user doesn't want to copy the password to the
							// clipboard, just let them know we were successful.
							} else messages = getResources().getString(R.string.edit_gen_success_no_copy);
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
								if (rowID != ParamsDbAdapter.DB_ERROR)
									dbHelper.updateRecord(rowID, params);
								else {
									// If we're adding a record, set the site list
									// on the main app to dirty so it will get
									// rebuilt the next time it's needed:
									rowID = dbHelper.createRecord(params);
									theApp.setSiteListDirty();
								}
								if (success && rowID != ParamsDbAdapter.DB_ERROR)
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
							else if (iterations > CryptnosApplication.HASH_ITERATION_WARNING_LIMIT)
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
								txtIterations.setText(String.valueOf(1));
							// In testing, I found iterations of 500 or more
							// started showing visible pauses on both the
							// emulator and my personal Motorola Droid, which
							// has a pretty beefy processor.  We don't want to
							// keep the user from going this high, but we
							// should at least warn them if it's going to take
							// a long time to do.
							} else if (iterations >= CryptnosApplication.HASH_ITERATION_WARNING_LIMIT) {
								Toast.makeText(v.getContext(),
										R.string.error_excessive_hashing,
										Toast.LENGTH_LONG).show();
								txtIterations.setText(String.valueOf(CryptnosApplication.HASH_ITERATION_WARNING_LIMIT));
							}
						// If the string was empty, that's invalid:
						} else {
							Toast.makeText(v.getContext(),
								R.string.error_bad_iterations,
								Toast.LENGTH_LONG).show();
							txtIterations.setText(String.valueOf(1));
						}
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
						txtIterations.setText(String.valueOf(1));
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
					// For now, ignore any exceptions:
					catch (Exception e) { }
				}
			}
        });
        
        hashSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View v,
					int position, long id) {
				// When the hash spinner changes, get the string value of the
				// new selection and feed that to the character limit spinner
				// builder to rebuild the spinner's acceptable values:
				String hash = (String)hashSpinner.getSelectedItem();
				rebuildCharLimitSpinner(hash);
			}
			public void onNothingSelected(AdapterView<?> arg0) {
				// What should we do here?  For now, nothing.
			}
        });
 
        // Next, we'll add a key listener to the master password text box
        // and listen for the Enter key.  If the Enter key is pressed, we
        // don't want that character to be added to the master password.
        // Rather, we want it to trigger the generate password event, just
        // as if the user tapped the Generate button.
        txtPassphrase.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					btnGenerate.performClick();
					return true;
				} else return false;
			}
        });

    }
    
	@Override
	public void onResume()
	{
		// The only thing we're concerned about when resuming is whether or not
		// we should be clearing the master and generated password boxes.  In
		// general, this is the user's preference.  The user may elect to have
		// Cryptnos clear these boxes whenever it goes into the background (i.e.
		// loses focus), such as when they multitask and switch to another
		// application.  By default, this preference is turned off, which
		// replicates the behavior of the app before the option was added.
		//
		// This becomes complicated when we have to take into account configuration
		// changes, such as rotating the screen or sliding out a physical keyboard.
		// Android handles these actions by destroying and recreating the activity.
		// In this case, we *DON'T* want to clear the passwords, as that might be
		// annoying.  When this happens, we explicitly override the user's preference
		// and disable this functionality.
		//
		// By the time we reach this step, the clearPasswords flag should have
		// already performed this logic and we should know whether or not we
		// should clear those boxes.  If we need to clear them, we'll do that now.
        if (clearPasswords) {
        	txtPassphrase.setText("");
        	txtOutput.setText("");
        }
        // Now restore the user's original preference, just in case we did
        // override it to handle a configuration change:
        clearPasswords = theApp.clearPasswordsOnFocusLoss();
        // Call the super's onResume() to complete the process:
        super.onResume();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	// Add the "Help" menu item:
    	menu.add(0, OPTMENU_HELP, Menu.NONE,
        	R.string.optmenu_help).setIcon(android.R.drawable.ic_menu_help);
    	return true;
    }
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
	    	// If the Help item is selected, open up either the "getting
    		// started" or "working with existing parameters" help, depending
    		// on which mode we were called in:
	    	case OPTMENU_HELP:
	        	Intent i = new Intent(this, HelpActivity.class);
	        	if (mode == MODE_NEW)
	        		i.putExtra("helptext", R.string.help_text_start);
	        	else i.putExtra("helptext", R.string.help_text_existing);
	        	startActivity(i);
	    		return true;
    	}
    	return false;
    }
    
	public Object onRetainNonConfigurationInstance() {
		// This gets called primarily if the user rotates the screen or slides out
		// the physical keyboard.  In these cases, we don't want to wipe out the
		// user's data from the form, so we'll need to preserve it.  Stuff the
		// site parameters, the user's master password, and the generated password
		// into a temporary object and hold onto that until we return from the
		// rebuilding process.
		final ParameterViewState state = new ParameterViewState(
				txtSite.getText().toString(),
				txtPassphrase.getText().toString(),
				(String)hashSpinner.getSelectedItem(),
				txtIterations.getText().toString(),
				charTypesSpinner.getSelectedItemPosition(),
				spinCharLimit.getSelectedItemPosition(),
				txtOutput.getText().toString(),
				mode, lastSite, rowID);
		return state;
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
    
    /**
     * Rebuild the items in the character limit spinner based on the hash algorithm
     * name specified
     * @param hash The name of the new hash algorithm
     */
    private void rebuildCharLimitSpinner(String hash) {
    	// Get the current position of the spinner, if any:
    	int currentPosition = spinCharLimit.getSelectedItemPosition();
    	// Get the length of the Base64-encoded result of the hash:
    	int hashLength = theApp.getEncodedHashLength(hash);
    	// As long as the above did not generate an error:
    	if (hashLength != -1) {
    		// Create a String array just big enough to hold all the hash
    		// length values, plus one more:
    		String[] charLimits = new String[hashLength + 1];
    		// Set the first item in the list to the "None" value:
    		charLimits[0] = getResources().getString(R.string.edit_charlimit_none);
    		// Now populate the rest of the list with the numeric values between
    		// one and the total hash length:
    		for (int i = 1; i <= hashLength; i++)
    			charLimits[i] = String.valueOf(i);
    		// Create a adapter to work with this new array and assign it to the
    		// spinner:
    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
    				android.R.layout.simple_spinner_item, charLimits);
    		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		spinCharLimit.setAdapter(adapter);
    		// If the old position of the spinner is outside the bounds of the new
    		// one, set the position to zero, or "None".  Anything less than zero
    		// isn't valid anyway, and anything above our current hash length is
    		// the same thing as saying no limit at all.
    		if (currentPosition < 0 || currentPosition > hashLength)
    			spinCharLimit.setSelection(0, true);
    		// Otherwise, just restore the old position:
    		else spinCharLimit.setSelection(currentPosition, true);
    	// If anything blew up, complain:
    	} else Toast.makeText(getBaseContext(),
				"ERROR: Invalid hash algorithm name",
				Toast.LENGTH_LONG).show();
    }

}
