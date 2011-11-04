/* GenerateExistingActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          December 15, 2009
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This activity provides a read-and-generate-only interface to the set of
 * site parameters selected in the calling activity.  For users of the .NET
 * version of Cryptnos, this is equivalent to the "Lock parameters" option;
 * all the site parameters are displayed to the user as a read-only TextView,
 * except for the passphrase, which must be entered by the user.  This
 * provides an easy way to call up a set of parameters and generate the
 * associated password without worrying about fat-fingering screwing up the
 * database.
 * 
 * UPDATES FOR 1.2.0:  Added Help option menu
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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Generate Existing Passphrase activity. Assuming that a set of site
 * parameters have already been saved to the database, this activity provides
 * an interface to generate the resulting password without altering the
 * parameters themselves.  In other words, this is a read-and-generate only
 * view, allowing the user to generate the password for a given site without
 * worrying about fat-fingering something and screwing up the site's settings. 
 * @author Jeffrey T. Darlington
 * @version 1.3.0
 * @since 1.0
 */
public class GenerateExistingActivity extends Activity {

	/** A constant indicating the Help option menu item. */
	public static final int OPTMENU_HELP = Menu.FIRST;

	/** The EditText view containing the user's passphrase. */
	private EditText txtPassphrase = null;
	/** The TextView that will display the site name */
	private TextView lblSiteName = null;
	/** The TextView that will display the other parameters in a read-only
	 *  fashion. */
	private TextView lblOtherParams = null;
	/** The EditText view that will eventually contain the final password. */
	private EditText txtOutput = null;
	/** The Button that actually generates the password and saves it to
	 *  the database. */
	private Button btnGenerate = null;

	/** A reference to our top-level application */
	private CryptnosApplication theApp = null;
	/** Our database adapter for manipulating the database. */
	private ParamsDbAdapter dbHelper = null;
	/** Since this activity only deals with one set of parameters at a time,
	 *  this member will hold the parameters to make working with them
	 *  easier. */
	private SiteParameters params = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.gen_exist_layout);
        // Open the database:
        // Get a reference to the top-level application, as well as the
        // DB helper:
        theApp = (CryptnosApplication)getApplication();
        dbHelper = theApp.getDBHelper();
        // Get handier references to our various input/output controls:
        txtPassphrase = (EditText)findViewById(R.id.txtPassphrase);
        lblSiteName = (TextView)findViewById(R.id.labelSiteName);
        lblOtherParams = (TextView)findViewById(R.id.labelOtherParams);
        txtOutput = (EditText)findViewById(R.id.txtOutput);
        btnGenerate = (Button)findViewById(R.id.btnGenerate);

        // Determine whether or not the user has specified to show or hide
        // master passwords and toggle the behavior of the master passphrase
        // box accordingly:
        if (theApp.showMasterPasswords())
        	txtPassphrase.setTransformationMethod(null);

        // Asbestos underpants:
        try {
        	// Check to see if we currently have a saved state for this activity,
        	// such as before a screen rotate or before the user slides out a
        	// physical keyboard.  If such a state exists, we want to restore it:
	        final ParameterViewState state =
	        	(ParameterViewState)getLastNonConfigurationInstance();
	        // We found a previously saved state, so re-populate the form:
	        if (state != null) {
	        	params = state.getSiteParameters();
	        	populateParametersLabel();
	        	txtPassphrase.setText(state.getMasterPassword());
	        	txtOutput.setText(state.getGeneratedPassword());
	        // There was no saved state, so proceed to the next step:
	        } else {
	            // This activity doesn't make any sense if we don't get anything
	            // from the caller, so make sure we get valid input:
	            Bundle extras = getIntent().getExtras();
	            if (extras != null)
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
            				// Get the parameters.  Note that we need both the
            				// site key and the full encrypted string to get
            				// everything back out.
            				params = new SiteParameters(theApp,
            						c.getString(1),
            						c.getString(2));
            				// Populate the other parameters label with the
            				// parameters other than the passphrase.  All of this
            				// is essentially read-only info in this case, so
            				// it's purely informative.
            				populateParametersLabel();
        				}
        				// If we didn't get exactly one row, throw an error:
        				else
        				{
        					Toast.makeText(this, R.string.error_bad_restore,
        						Toast.LENGTH_LONG).show();
        					finish();
        				}
        				c.close();
        			}
        			// If the site name was not in the intent bundle, throw
        			// an error:
        			else
        			{
    					Toast.makeText(this, R.string.error_bad_restore,
       						Toast.LENGTH_LONG).show();
    					finish();
        			}
	            }
	            // The bundle was empty, which shouldn't happen:
	            else
	            {
	            	Toast.makeText(this, R.string.error_bad_restore,
	    				Toast.LENGTH_LONG).show();
					finish();
	            }
	        }
	        
	        // Now define what to do if the Generate button is clicked:
	        btnGenerate.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// Make sure the passphrase box isn't empty:
					if (txtPassphrase.getText() == null ||
						txtPassphrase.getText().length() == 0)
							Toast.makeText(v.getContext(),
								R.string.error_edit_bad_password,
								Toast.LENGTH_LONG).show();
					// Otherwise, try to generate the password:
					else
					{
						try
						{
							// In theory, this part should be easy:
							String password =
								params.generatePassword(txtPassphrase.getText().toString());
							// Display the generated password in the output text
							// box: 
							txtOutput.setText(password);
							// If the user chose to copy the password to the clipboard,
							// go ahead and copy it now:
							if (theApp.copyPasswordsToClipboard()) {
								ClipboardManager clippy = ClipboardManager.newInstance(theApp);
								clippy.setText(password);
								// We'll assume both of those tasks were successful:
								Toast.makeText(v.getContext(), R.string.edit_gen_success,
										Toast.LENGTH_LONG).show();
							// Otherwise, just confirm to the user that the password
							// was generated:
							} else Toast.makeText(v.getContext(),
									R.string.edit_gen_success_no_copy,
									Toast.LENGTH_LONG).show();
						}
						catch (Exception e)
						{
							Toast.makeText(v.getContext(), R.string.error_bad_generate,
									Toast.LENGTH_LONG).show();
						}
					}
				}
	        });

	    // Something blew up alone the way:
        } catch (Exception e) {
			Toast.makeText(this, R.string.error_bad_restore,
					Toast.LENGTH_LONG).show();
			finish();
        }
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
	    	// If the Help item is selected, open up the "Working with
    		// existing parameters" help:
	    	case OPTMENU_HELP:
	        	Intent i = new Intent(this, HelpActivity.class);
	        	i.putExtra("helptext", R.string.help_text_existing);
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
		final ParameterViewState state = new ParameterViewState(params,
				txtPassphrase.getText().toString(),
				txtOutput.getText().toString());
		return state;
	}

	/**
	 * Populate the parameter labels using the data in the SiteParameters object.
	 * This has been pulled out into its own function since it technically gets
	 * called multiple times.
	 */
	private void populateParametersLabel() {
		// As a safety measure, make sure the parameters object exists before
		// we start reading from it:
		if (params != null) {
			lblSiteName.setText(params.getSite());
			String nl = System.getProperty("line.separator");
			lblOtherParams.setText(
				getResources().getString(R.string.gen_exist_hash_prompt) + " " +
				params.getHash() + nl +
				getResources().getString(R.string.gen_exist_iterations_prompt) + " " +
				Integer.toString(params.getIterations()) + nl +
				getResources().getString(R.string.gen_exist_chartypes_prompt) + " " +
				getResources().getStringArray(R.array.charTypeList)[params.getCharTypes()] + nl +
				getResources().getString(R.string.gen_exist_charlimit_prompt) + " " +
				(params.getCharLimit() == 0 ?
					getResources().getString(R.string.gen_exist_charlimit_none) : 
					Integer.toString(params.getCharLimit())));
		}
	}
}
