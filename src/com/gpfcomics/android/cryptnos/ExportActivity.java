/* ExportActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          March 29, 2010
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * The Export Activity provides the user interface for the Cryptnos export
 * functionality.  We need three things from the user:  a file name to export
 * to, a password to encrypt the file with, and a list of sites for us to
 * export.  For added measure, we'll prompt the user for the password twice to
 * make sure typos are less likely.  Throw in some input checking to make sure
 * everything is valid and we should be ready to export those sites.  Note
 * that we will restrict the user's file selection to only writable files in
 * the root of their removable storage (usually an SD card).
 * 
 * Note that the actual grunt work of exporting the data is handled by the
 * separate ImportExportHander class.  We will pass the buck to it from here,
 * but this class will be responsible for gathering the inputs and creating
 * the progress dialog that the handler will update.
 * 
 * UPDATES FOR 1.1: When OI File Manager is available, let the user use it to
 * specify the path in which they can save their export file.  If it is not
 * available, fall back to the old 1.0 behavior and force the user to export
 * to the root of the SD card.  This should be abstracted enough that other
 * file managers can be slipped in if their intents are known.
 * 
 * UPDATES FOR 1.2.0:  Added Help option menu.  Changed OI File Manager code
 * to use the more generic FileManager class so we can let the user decide what
 * file manager to use.
 * 
 * UPDATES FOR 1.2.6:  Fix for Issue #6, "ActivityNotFoundException in
 * Instrumentation.checkStartActivityResult()".  Added try/catch block around
 * calling third-party file manager intent.
 * 
 * UPDATES FOR 1.3.0: Added "show master passwords" functionality.  Added view
 * state functionality to preserve user inputs on orientation change.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Export Activity provides the user interface for the Cryptnos export
 * functionality.  We need three things from the user:  a file name to export
 * to, a password to encrypt the file with, and a list of sites for us to
 * export.  For added measure, we'll prompt the user for the password twice to
 * make sure typos are less likely.  Throw in some input checking to make sure
 * everything is valid and we should be ready to export those sites.  Note
 * that we will restrict the user's file selection to only writable files in
 * the root of their removable storage (usually an SD card).
 * 
 * Note that the actual grunt work of exporting the data is handled by the
 * separate ImportExportHander class.  We will pass the buck to it from here,
 * but this class will be responsible for gathering the inputs and creating
 * the progress dialog that the handler will update.
 * @author Jeffrey T. Darlington
 * @version 1.3.0
 * @since 1.0
 */
public class ExportActivity extends Activity implements
	SiteListListener {

	/** The top-level instructions label */
	private TextView labelInstructions = null;
	/** The Selected Path button */
	private Button btnPickPath = null;
	/** The text box containing the name of the export file */
	private EditText txtExportFile = null;
	/** The text box containing the initial password value */
	private EditText txtPassphrase1 = null;
	/** The text box containing the confirmation password value */
	private EditText txtPassphrase2 = null;
	/** The Pick Sites button */
	private Button btnPickSites = null;
	/** The Export button */
	private Button btnExport = null;
	/** A reference to the linear layout that contains our UI elements */
	private LinearLayout layout = null;
	
	/** A constant indicating that we should show the site list dialog. */
	static final int DIALOG_SITE_LIST = 1100;
	/** A constant indicating that we should show the progress dialog during
	 *  the export process. */
	static final int DIALOG_PROGRESS_EXPORT = 1101;

	/** A constant indicating the Help option menu item. */
	public static final int OPTMENU_HELP = Menu.FIRST;

	/** A reference to our top-level application */
	private CryptnosApplication theApp = null;
	/** A handy reference to the ProgressDialog used when loading encrypted
	 *  data from the database. */
	private ProgressDialog progressDialog = null;
	/** The export handler */
	private ImportExportHandler exporter = null;
	
	/** The name of the export file */
	private String exportFile = null;
	/** The final password value, after validation */
	private String password = null;
	/** An array of Strings containing the list of all site
	 *  tokens read from the database.  This will serve as the
	 *  data for the site selection dialog and, combined with
	 *  the selectedSites array, will be used to generate the
	 *  final list of sites to export. */
	String[] allSites = null;
	/** A boolean array identical in length to allSites that
	 *  indicates whether the site token with the same index
	 *  has been selected for export.  This is fed to the
	 *  site selection dialog and will be combined with allSites
	 *  to produce the final export list. */
	boolean[] selectedSites = null;
	/** A File object representing the root of our export file path. */
	private File exportRootPath = null;
	/** This boolean flag determines whether or not we will rebuild the
	 *  selected sites array.  By default, we want to do this.  But if
	 *  we're coming back from a configuration change, we don't; we want
	 *  to use the copy coming from the view state.  We will test this
	 *  flag when the time comes to rebuild the array. */
	private boolean rebuildSelectedSites = true;
	
    public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.export_layout);

        // Get a reference to the top-level application, as well as the
        // DB helper:
        theApp = (CryptnosApplication)getApplication();
        
        // Check to make sure the external storage is mounted for read/write
        // access.  If it isn't, there's not much point in doing an export,
        // now is there?  Warn the user and quit this activity.
        if (!theApp.canWriteToExternalStorage()) {
			Toast.makeText(this, R.string.error_export_card_not_mounted,
				Toast.LENGTH_LONG).show();
			finish();
        }
        
        // Get handy references to our UI elements:
        labelInstructions = (TextView)findViewById(R.id.labelExportFile);
        btnPickPath = (Button)findViewById(R.id.btnExportPickPath);
        txtExportFile = (EditText)findViewById(R.id.txtExportFile);
        txtPassphrase1 = (EditText)findViewById(R.id.txtPassphrase1);
        txtPassphrase2 = (EditText)findViewById(R.id.txtPassphrase2);
        btnPickSites = (Button)findViewById(R.id.btnPickSites);
        btnExport = (Button)findViewById(R.id.btnExport);
        layout = (LinearLayout)findViewById(R.id.layoutExport);
        
        // Determine whether or not the user has specified to show or hide
        // master passwords and toggle the behavior of the master passphrase
        // boxes accordingly:
        if (theApp.showMasterPasswords()) {
        	txtPassphrase1.setTransformationMethod(null);
        	txtPassphrase2.setTransformationMethod(null);
        }
        
        // Asbestos underpants:
        try {
        	// Check to see if we've preserved the view state from a previous version.
        	// This often happens on a configuration change, such as rotating the device
        	// or sliding out a physical keyboard.  If such a state exists, populate the
        	// internal variables and UI elements to recreate the previous state.
        	final ExportViewState state =
        		(ExportViewState)getLastNonConfigurationInstance();
        	if (state != null) {
        		exportRootPath = new File(state.getExportPath());
        		txtExportFile.setText(state.getExportFile());
        		txtPassphrase1.setText(state.getPassword1());
        		txtPassphrase2.setText(state.getPassword2());
        		selectedSites = state.getSelectedSites();
        		// Set this flag to make sure we don't override the selected sites
        		// array when we get the site list from the main app class:
        		rebuildSelectedSites = false;
        	// If a previous state did not exist, set our default values:
        	} else setDefaults();
        // Similarly, if anything blew up above, punt with the defaults:
        } catch (Exception e) {
        	setDefaults();
        }

        // If we found a file manager's pick file intent, update the UI to
        // let the user select a path using the Selected Path button.  The
        // instructions label contains the old text by default, so update it
        // to reflect the instructions including this button.
        if (theApp.getFileManager().isFileManagerSelected()) {
        	btnPickPath.setText(getResources().getString(R.string.export_file_pick_path_button_label) +
        			" " + exportRootPath.getAbsolutePath());
        	labelInstructions.setText(R.string.export_file_label_pick_path);
        // If a pick file intent could not be found, default to the old way
        // of doing things and remove the Selected Path button from the view:
        } else layout.removeView(btnPickPath);
        
        /**
         * What to do when the Pick Sites button is clicked
         */
        btnPickSites.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Since the site list dialog does all the work,
				// pass the buck here:
				showDialog(DIALOG_SITE_LIST);
			}
        });

        /** What to do when the Export button is clicked */
        btnExport.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Get the values of the three text boxes:
				exportFile = txtExportFile.getText().toString();
				String password1 = txtPassphrase1.getText().toString();
				String password2 = txtPassphrase2.getText().toString();
				boolean somethingChecked = false;
				for (int i = 0; i < selectedSites.length; i++) {
					if (selectedSites[i]) {
						somethingChecked = true;
						break;
					}
				}
				// If the export file box is empty, there's no point going
				// any further:
				if (exportFile == null || exportFile.length() == 0) {
					Toast.makeText(v.getContext(),
							R.string.error_export_bad_filename,
       						Toast.LENGTH_LONG).show();
				// Similarly, the password boxes need to be populated:
				} else if (password1 == null || password1.length() == 0 ||
						password2 == null || password2.length() == 0) {
					Toast.makeText(v.getContext(),
							R.string.error_export_missing_password,
       						Toast.LENGTH_LONG).show();
				// And the passwords better match:
				} else if (password1.compareTo(password2) != 0) {
					Toast.makeText(v.getContext(),
							R.string.error_export_password_nomatch,
       						Toast.LENGTH_LONG).show();
				// And at least one site must be selected:
				} else if (!somethingChecked) {
					Toast.makeText(v.getContext(),
							R.string.error_export_no_sites_checked,
       						Toast.LENGTH_LONG).show();
		        // Check to make sure the external storage is mounted for read/write
		        // access.  If it isn't, there's not much point in doing an export,
		        // now is there?  Warn the user:
				} else if (!theApp.canWriteToExternalStorage()) {
					Toast.makeText(v.getContext(),
							R.string.error_export_card_not_mounted,
							Toast.LENGTH_LONG).show();
				// If all our inputs are correct, proceed with the export:
				} else {
					File exportFileFile = new File(exportRootPath, exportFile);
					exportFile = exportFileFile.getAbsolutePath();
					password = password1;
					showDialog(DIALOG_PROGRESS_EXPORT);
				}
			}
        });
        
        /** What to do if the text in the export file box changes */
        txtExportFile.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				// Did we just lose focus?  If so, check the current file path
				// and selected file and issue a warning if the user is about
				// to overwrite an existing file.
				if (!hasFocus) checkForExistingFile();
			}
        });
        
        /** What to do when the Selected Path button is clicked */
        btnPickPath.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// This should only get called if we have a valid file manager
				// selected, but were should double-check that this is valid
				// anyway:
				FileManager fm = theApp.getFileManager();
				try {
					if (fm.isFileManagerSelected()) {
						// Use the FileManager class to generate our select folder
						// intent, then fire it off:
						Intent intent = fm.generateSelectFolderIntent(exportRootPath.toString(),
								getResources().getString(R.string.export_file_dialog_title),
								getResources().getString(R.string.import_file_dialog_button));
						startActivityForResult(intent,
								FileManager.INTENT_REQUEST_SELECT_FOLDER);
					// If for some reason no file manager was selected, complain:
					} else Toast.makeText(v.getContext(),
							R.string.error_no_external_file_manager,
							Toast.LENGTH_LONG).show();
					// There is a chance that we'll try to fire off the intent to pick a
					// directory and end up generating an exception.  (See Issue $6.)  The most
					// likely exception is ActivityNotFoundException, but we'll be generic
					// here to play it safe.  If we throw an exception above, warn the user
					// to check their file manager preference in the settings.
				} catch (Exception e) {
					String message = getResources().getString(R.string.error_file_manager_not_found);
					message = message.replace(getResources().getString(R.string.meta_replace_token),
							fm.getPreferredFileManagerName());
					Toast.makeText(v.getContext(), message, Toast.LENGTH_LONG).show();
				}
			}
		});
        
        // Request the site list from the main app.  We'll actually get the
        // list in the onSiteListReady() method below.
        theApp.requestSiteList(this, this);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	String filename = null;
    	// Look at the request code:
    	switch (requestCode) {
    		// If we're returning from a folder selection, try to get the folder
    		// name:
	    	case FileManager.INTENT_REQUEST_SELECT_FOLDER:
	    		if (resultCode == RESULT_OK && data != null)
                    filename = theApp.getFileManager().getSelectedFolder(data);
	    		break;
    	}
    	// if we got anything useful:
        if (filename != null) {
            // Get rid of URI prefix if present:
            if (filename.startsWith("file://")) {
                    filename = filename.substring(7);
            }
            // Take note of the selected folder's name:
            exportRootPath = new File(filename);
            // On the off chance the user selected an actual file, break
            // apart the name from the path and put the name into the text
            // box for the file name.  Then reset the root path to the parent
            // of the file.
            if (exportRootPath.isFile()) {
            	txtExportFile.setText(exportRootPath.getName());
            	exportRootPath = exportRootPath.getParentFile();
            	filename = exportRootPath.getAbsolutePath();
            }
            // Now update the button text with the path:
            btnPickPath.setText(getResources().getString(R.string.export_file_pick_path_button_label) +
            		" " + filename);
            // Now check to see if the new path plus the existing file name
            // point to an existing file.  If they do, warn the user.
            checkForExistingFile();
            // Check the path to make sure we can write to it.  If not,
            // issue a warning Toast:
            if (!exportRootPath.canWrite()) {
            	String message = getResources().getString(R.string.error_export_path_not_writable).replace(getResources().getString(R.string.meta_replace_token), filename);
            	Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    protected void onStop()
    {
    	// This may seem a bit weird, but whenever we stop this activity
    	// (i.e., switch to another one, either by moving to the next one in
    	// the task flow or canceling out and moving to a different task), we
    	// don't want to come back to this step.  Rather, we want to take the
    	// user back to the main menu.  So whenever this task gets stopped,
    	// go ahead and finish it.
    	super.onStop();
    	//finish();
    }
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
    	// We need a dialog reference to pass out, as well as references
    	// to ourself to pass in for contexts on Toasts and dialog control.
    	// (These references also have to be final to be seen by all the inner
    	// classes.)
    	Dialog dialog = null;
    	final Activity theActivity = this;
    	// Determine which dialog to show:
    	switch (id)
    	{
    		// The site list building progress dialog is actually handled by
    		// the application, but we need to attach it here to actually get
    		// it to display.  If we get this value, pass the buck:
    		case CryptnosApplication.DIALOG_PROGRESS:
    			dialog = theApp.onCreateDialog(id);
    			break;
    		// This dialog creates a pop-up list of checkboxes for all the
    		// sites in the DB.  The user can check the checkboxes to select
    		// which sites to export.
	    	case DIALOG_SITE_LIST:
	    		AlertDialog.Builder adb = new AlertDialog.Builder(this);
    			adb.setTitle(R.string.export_select_sites_title);
    			adb.setCancelable(false);
    			// This is where it gets a bit funky.  This lets us create the
    			// actual checkbox list.  Pass in the array of site names built
    			// during activity start up, as well as the boolean array of
    			// which sites have been checked.  Then we create a listener
    			// that flips the bits in the selection array each time a
    			// checkbox is toggled.
    			adb.setMultiChoiceItems(allSites, selectedSites,
    				new OnMultiChoiceClickListener() {
    				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
    					selectedSites[which] = isChecked;
    				}
    			});
    			// What to do when the OK button is clicked:
    			adb.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
  		           public void onClick(DialogInterface dialog, int id) {
  		        	   // We want to show the user how many sites are currently
  		        	   // selected, so we'll have to count how many true bits
  		        	   // are in the selection array:
  		        	   int count = 0;
  		        	   for (int i = 0; i < selectedSites.length; i++)
  		        		   if (selectedSites[i]) count++;
  		        	   // Now get the message string, replace the placeholder
  		        	   // with the count value, and make a Toast:
  		        	   String message = getResources().getString(R.string.export_selected_count_message);
  		        	   message = message.replace(getResources().getString(R.string.meta_replace_token), String.valueOf(count));
  		        	   Toast.makeText(theActivity, message,
  	       						Toast.LENGTH_LONG).show();
  		        	   // The button always dismisses the dialog, but we don't
  		        	   // want to remove it because we want Android to remember
  		        	   // it's state.
  		           }
    			});
    			// What to do when the Check All button is clicked
    			adb.setNeutralButton(R.string.dialog_select_all, new DialogInterface.OnClickListener() {
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   // We're using the neutral button as our "select all" button
 		        	   // this time.  So flip all the selection bits in the array
 		        	   // to true:
 		        	   for (int i = 0; i < selectedSites.length; i++)
 		        		   selectedSites[i] = true;
 		        	   // Make a Toast informing the user how many sites have
 		        	   // been selected (in this case, all of them):
  		        	   String message = getResources().getString(R.string.export_selected_count_message);
  		        	   message = message.replace(getResources().getString(R.string.meta_replace_token), String.valueOf(selectedSites.length));
  		        	   Toast.makeText(theActivity, message,
  	       						Toast.LENGTH_LONG).show();
  		        	   // As above, we want Android to remember the state this time,
  		        	   // so dismiss but don't remove the dialog.
 		           }
 		       });
    			// What to do when the Clear All button is clicked
    			adb.setNegativeButton(R.string.dialog_clear_all, new DialogInterface.OnClickListener() {
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   // We're using the negative button as the "clear all" button.
 		        	   // Flip all the selection bits to false:
 		        	   for (int i = 0; i < selectedSites.length; i++)
 		        		   selectedSites[i] = false;
 		        	   // Make a Toast to inform the user no sites are selected;
  		        	   String message = getResources().getString(R.string.export_selected_count_message);
  		        	   message = message.replace(getResources().getString(R.string.meta_replace_token), "0");
  		        	   Toast.makeText(theActivity, message,
  	       						Toast.LENGTH_LONG).show();
  		        	   // Now THIS time, we need to force Android to remove the
  		        	   // dialog and rebuild it the next time around.  Otherwise,
  		        	   // it will remember any checkboxes, which aren't affected
  		        	   // when we flip the selection bits.  When Android rebuilds
  		        	   // the dialog, it will be forced to revisit the selection
  		        	   // array and make all the checkboxes cleared.
 		        	   theActivity.removeDialog(DIALOG_SITE_LIST);
 		           }
 		       });
    			dialog = (Dialog)adb.create();
	    		break;
	    	// The progress dialog does the work of the actual export.  We'll
	    	// create this dialog, create an exporter object and pass a
	    	// reference to the dialog along with it. 
	    	case DIALOG_PROGRESS_EXPORT:
	    		// We have an array with all our sites, and an array with flags
	    		// indicating which sites are selected.  We need to convert
	    		// both of these to a simple array of site token strings  to
	    		// feed to the exporter.  Walk through both arrays and stuff
	    		// the selected ones into an ArrayList.
	    		ArrayList<String> exportSitesList = new ArrayList<String>();
	    		for (int i = 0; i < allSites.length; i++)
	    			if (selectedSites[i]) exportSitesList.add(allSites[i]);
	    		// Convert the ArrayList to an array: 
	    		String[] exportSites = new String[exportSitesList.size()];
	    		exportSitesList.toArray(exportSites);
	    		// Now create the progress dialog:
	    		progressDialog = new ProgressDialog(this);
	    		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    		progressDialog.setMax(100);
	            progressDialog.setMessage(getResources().getString(R.string.export_progress_message));
	            // Create the exporter and put it to work:
	    		exporter = new ImportExportHandler(theActivity,
	    			progressDialog, DIALOG_PROGRESS_EXPORT);
	            exporter.exportToFile(exportFile, password, exportSites);
	            dialog = progressDialog;
	    		break;
    	}
    	return dialog;
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
	    	// If the Help item is selected, open up the "Export and
    		// Import" help:
	    	case OPTMENU_HELP:
	        	Intent i = new Intent(this, HelpActivity.class);
	        	i.putExtra("helptext", R.string.help_text_importexport);
	        	startActivity(i);
	    		return true;
    	}
    	return false;
    }
	
	/**
	 * Set some default values for when we first come into the activity.  This
	 * is pulled into its own method because several paths need to perform the
	 * same steps, so this encapsulates it into one place.
	 */
	private void setDefaults() {
        // Get the export root path:
        exportRootPath = theApp.getImportExportRootPath();
        // Set the default export file name based on today's date.  I'm not
        // sure how well this will work for any calendar other than Gregorian,
        // but this should produce "cryptnos_export_YYYYMMDD_HHMMSS.dat".
        Calendar calendar = Calendar.getInstance();
        txtExportFile.setText("cryptnos_export_" +
        		String.valueOf(calendar.get(Calendar.YEAR)) +
        		zeroPadNumber(calendar.get(Calendar.MONTH) + 1) +
        		zeroPadNumber(calendar.get(Calendar.DAY_OF_MONTH)) + "_" +
        		zeroPadNumber(calendar.get(Calendar.HOUR_OF_DAY)) +
        		zeroPadNumber(calendar.get(Calendar.MINUTE)) +
        		zeroPadNumber(calendar.get(Calendar.SECOND)) + ".dat");
	}
    
    /**
     * Given an integer (assumed to be < 100), return a zero-padded string
     * value (i.e. 5 yields "05", 12 yields "12").
     * @param number The integer to pad
     * @return A zero-padded string containing the number
     */
    private String zeroPadNumber(int number)
    {
    	return number < 10 ? "0" + String.valueOf(number) :
    		String.valueOf(number);
    }

    /**
     * Test the current combination of the path specified in the Select Path
     * button (i.e. the exportRootPath) and the file name in the Export File
     * text box corresponds with an existing file.  If so, issue a warning
     * Toast to the user that the file will be overwritten.  This has been
     * pulled out into its own method to allow it to be reused.
     */
    private void checkForExistingFile() {
		try
		{
			// If we did, try to find out if a file by that name
			// already exists:
			String testFileName = txtExportFile.getText().toString();
			File testFile = new File(exportRootPath, testFileName);
			if (testFile.exists()) {
				// If the file exists, warn the user that we're
				// about to overwrite it:
				String message = getResources().getString(R.string.error_export_file_exists);
				message = message.replace(getResources().getString(R.string.meta_replace_token),
						exportRootPath.getAbsolutePath() + File.separator +
						testFileName);
				Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			}
		}
		// In this case, we don't care if the test above blows up.
		// Just quietly ignore any errors.
		catch (Exception e) {}
    }
    
	public void onSiteListReady(String[] siteList) {
		try {
			// Make sure we got something useful:
			if (siteList != null) {
				// Copy the generated list back to the caller:
                allSites = siteList;
                // If we need to rebuild the selection list, do so and
                // default everything to being deselected.  Note that if
                // we're coming from a configuration change, we don't want
                // to do this as we'll lose the user's current selection.
                if (rebuildSelectedSites) {
                	selectedSites = new boolean[allSites.length];
	                for (int i = 0; i < selectedSites.length; i++)
	                	selectedSites[i] = false;
                }
            // The site list was empty:
			} else Toast.makeText(this, R.string.error_bad_listfetch,
            		Toast.LENGTH_LONG).show();
		// Something blew up:
		} catch (Exception e) {
        	Toast.makeText(this, R.string.error_bad_listfetch,
            		Toast.LENGTH_LONG).show();
		}
	}
	
	public Object onRetainNonConfigurationInstance() {
		// Preserve our view state:
		final ExportViewState state = new ExportViewState(
				exportRootPath.getAbsolutePath(),
				txtExportFile.getText().toString(),
				txtPassphrase1.getText().toString(),
				txtPassphrase2.getText().toString(),
				selectedSites);
		return state;
	}
	
	/**
	 * This class preserves the view state of the export activity during a
	 * configuration change, such as rotating the device or sliding out a
	 * physical keyboard.
	 * @author Jeffrey T. Darlington
	 * @version 1.3.0
	 * @since 1.3.0
	 */
	private class ExportViewState {

		/** The current export root path */
		private String exportPath = null;
		/** The current export file name */
		private String exportFile = null;
		/** The current value of the first password box */
		private String password1 = null;
		/** The current value of the second password box */
		private String password2 = null;
		/** The current value of the selected sites array */
		boolean[] selectedSites = null;

		/**
		 * The constructor
		 * @param exportPath The current export root path
		 * @param exportFile The current export file name
		 * @param password1 The current value of the first password box
		 * @param password2 The current value of the second password box
		 * @param selectedSites The current value of the selected sites array
		 */
		protected ExportViewState(String exportPath, String exportFile, String password1,
				String password2, boolean[] selectedSites) {
			this.exportPath = exportPath;
			this.exportFile = exportFile;
			this.password1 = password1;
			this.password2 = password2;
			this.selectedSites = selectedSites;
		}
		
		/** The current export root path */
		protected String getExportPath() { return exportPath; }
		
		/** The current export file name */
		protected String getExportFile() { return exportFile; }
		
		/** The current value of the first password box */
		protected String getPassword1() { return password1; }
		
		/** The current value of the second password box */
		protected String getPassword2() { return password2; }
		
		/** The current value of the selected sites array */
		protected boolean[] getSelectedSites() { return selectedSites; }
		
	}

}
