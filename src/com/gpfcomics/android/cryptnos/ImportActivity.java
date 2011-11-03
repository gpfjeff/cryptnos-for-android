/* ImportActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          March 29, 2010
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * The Import Activity provides the user interface for the Cryptnos import
 * functionality.  We need two things from the user:  a file name to import
 * from and a password to decrypt the file.  Note that we will restrict the
 * user's file selection to only readable files in the root of their removable
 * storage (usually an SD card).
 * 
 * Note that the actual grunt work of importing the data is handled by the
 * separate ImportExportHander class.  We will pass the buck to it from here,
 * but this class will be responsible for gathering the inputs and creating
 * the progress dialog that the handler will update.
 * 
 * UPDATES FOR 1.1: If the OI File Manager application is installed, use that
 * to let the user select an import file.  This allows the user to put the
 * import file in any directory or select any file.  If OI File Manager is
 * not installed, the app defaults to the previous behavior by letting the
 * user select a file in the root of the SD card.  This should be sufficiently
 * abstracted that it should be relatively easy to add other file managers
 * if they publish their intents.
 * 
 * UPDATES FOR 1.2.0:  Added Help option menu.  Changed OI File Manager code
 * to use the more generic FileManager class so we can let the user decide what
 * file manager to use.
 * 
 * UPDATES FOR 1.2.1:  Minor UI enhancements
 * 
 * UPDATES FOR 1.2.6:  Fix for Issue #6, "ActivityNotFoundException in
 * Instrumentation.checkStartActivityResult()".  Added try/catch block around
 * calling third-party file manager intent.
 * 
 * UPDATES FOR 1.3.0:  Added "show master passwords" functionality.  Add the ability
 * for the user to selectively import site parameters from a file.
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Import Activity provides the user interface for the Cryptnos import
 * functionality.  We need two things from the user:  a file name to import
 * from and a password to decrypt the file.  Note that we will restrict the
 * user's file selection to only readable files in the root of their removable
 * storage (usually an SD card).
 * 
 * Note that the actual grunt work of importing the data is handled by the
 * separate ImportExportHander class.  We will pass the buck to it from here,
 * but this class will be responsible for gathering the inputs and creating
 * the progress dialog that the handler will update.
 * @author Jeffrey T. Darlington
 * @version 1.3.0
 * @since 1.0
 */
public class ImportActivity extends Activity implements ImportListener, SiteListListener {

	/** A constant indicating that we should show the progress dialog. */
	static final int DIALOG_PROGRESS = 1200;
	/** A constant indicating that we should show the imported sites dialog. */
	static final int DIALOG_IMPORTED_SITES = DIALOG_PROGRESS + 1;
	/** A constant indicating that we'll warn the user if they're about to overwrite
	 *  an existing site in the database. */
	static final int DIALOG_OVERWRITE_WARNING = DIALOG_IMPORTED_SITES + 1;
	
	/** A constant indicating the Help option menu item. */
	public static final int OPTMENU_HELP = Menu.FIRST;

	/** A TextView that contains our basic instructions for
	 *  this activity. */
	private TextView labelInstructions = null;
	/** The Select File button */
	private Button btnSelectFile = null;
	/** A Spinner containing the list of files the user can
	 *  select from. */
	private Spinner spinnerFiles = null;
	/** An EditText box containing the user's passphrase. */
	private EditText txtPassphrase = null;
	/** The label for the passphrase box. */
	private TextView labelPassphrase = null;
	/** The Import button */
	private Button btnImport = null;
	/** The warning label which informs the user that import
	 *  file values overwrite values in the database. */
	private TextView labelWarning = null;
	/** A reference to the linear layout that contains our UI elements */
	private LinearLayout layout = null;
	
	/** A File object representing the root of our import file path. */
	private File importRootPath = null;
	/** A String representing the selected import file name */
	private String importFile = null;
	/** A String containing the user's password for decryption */
	private String password = null;
	/** An Object array containing the SiteParameters imported from a file */
	private Object[] importedSites = null;
	/** A boolean array indicating which sites were selected from the import list */
	private boolean[] selectedSites = null;
	
	/** A reference to our top-level application */
	private CryptnosApplication theApp = null;
	/** A reference to our database adapter. */
	private ParamsDbAdapter dbHelper = null;
	/** A handy reference to the ProgressDialog used when loading encrypted
	 *  data from the database. */
	private ProgressDialog progressDialog = null;
	/** The export handler */
	private ImportExportHandler importer = null;

    public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.import_layout);

        // Get a reference to the top-level application, as well as the
        // DB helper:
        theApp = (CryptnosApplication)getApplication();
        dbHelper = theApp.getDBHelper();
        
        // Check to make sure the external storage is mounted for read
        // access.  If it isn't, there's not much point in doing an import,
        // now is there?  Warn the user and quit this activity.
        if (!theApp.canReadFromExternalStorage()) {
			Toast.makeText(this, R.string.error_import_card_not_mounted,
				Toast.LENGTH_LONG).show();
			finish();
        }
        
        // Get handy references for our GUI elements:
        labelInstructions = (TextView)findViewById(R.id.labelImportFile);
        btnSelectFile = (Button)findViewById(R.id.btnOIImportFileSelect);
        spinnerFiles = (Spinner)findViewById(R.id.spinImportFiles);
        txtPassphrase = (EditText)findViewById(R.id.txtPassphraseImport);
        labelPassphrase = (TextView)findViewById(R.id.labelPassphraseImport);
        labelWarning = (TextView)findViewById(R.id.labelImportWarning);
        btnImport = (Button)findViewById(R.id.btnImport);
        layout = (LinearLayout)findViewById(R.id.layoutImport);
        
        // Determine whether or not the user has specified to show or hide
        // master passwords and toggle the behavior of the master passphrase
        // box accordingly:
        if (!theApp.showMasterPasswords())
        	txtPassphrase.setTransformationMethod(PasswordTransformationMethod.getInstance());

        // Get the import root path:
        importRootPath = theApp.getImportExportRootPath();
        
        // Check the import path and make sure it's a directory,
        // then make sure it is not empty:
        if (importRootPath.isDirectory() && importRootPath.list() != null
        		&& importRootPath.list().length > 0)
        {
        	// If the user's selected file manager is available, we'll provide a
        	// relatively easy way for the user to select their file:
        	if (theApp.getFileManager().isFileManagerSelected()) {
        		// First, hide the old file select spinner and reset the
        		// instruction label to the new instruction set:
        		layout.removeView(spinnerFiles);
        		labelInstructions.setText(R.string.import_file_label_oiimport);
        		btnSelectFile.setText(getResources().getString(R.string.import_file_button_prompt) +
        				" " + getResources().getString(R.string.import_file_button_prompt_none));
        		
        	// If an external file selection routine isn't available, fall
        	// back to our old original method.  This restricts the user's
        	// selection to the root of the SD card.
        	} else {
            	// First, hide the Select File button, because we no longer
        		// need it:
        		layout.removeView(btnSelectFile);
            	// Get a list of valid files from the root of the
            	// SD card.  We don't want directories, nor do we
            	// want files we can't read.
            	ArrayList<String> fileListTemp = new ArrayList<String>();
            	File[] fileListFiles = importRootPath.listFiles();
            	for (int i = 0; i < fileListFiles.length; i++) {
            		if (fileListFiles[i].isFile() && fileListFiles[i].canRead())
            			fileListTemp.add(fileListFiles[i].getName());
            	}
            	// If we got any files after filtering them, create
            	// a adapter so we can populate the spinner, which
            	// will give us a way to force the user to select
            	// one and only one file to import.
            	if (fileListTemp.size() > 0) {
            		String[] fileList = new String[fileListTemp.size()];
            		fileListTemp.toArray(fileList);
            		java.util.Arrays.sort(fileList, String.CASE_INSENSITIVE_ORDER);
            		ArrayAdapter<String> fileAdapter = new ArrayAdapter<String>(this,
            				android.R.layout.simple_spinner_item, fileList);
            		fileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            		spinnerFiles.setAdapter(fileAdapter);
            		spinnerFiles.setPromptId(R.string.import_file_prompt);
            		labelInstructions.setText(R.string.import_file_label_filefound);
            	// If no files could be found on the SD card, disable
            	// the form elements:
            	} else disableForm();
        	}
        // Similarly, if we can't get access to the SD card at
        // all, there's no point going forward:
        } else disableForm();
        
        /** What to do when the Import button is clicked */
        btnImport.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// First of all, make sure the passphrase box isn't empty:
				if (txtPassphrase.getText().toString().length() == 0) {
					Toast.makeText(v.getContext(),
							R.string.error_import_nopassword,
       						Toast.LENGTH_LONG).show();
				// Next, make sure we can actually read from the external
				// storage device:
				} else if (!theApp.canReadFromExternalStorage()) {
					Toast.makeText(v.getContext(),
							R.string.error_import_card_not_mounted,
							Toast.LENGTH_LONG).show();
				// If we have OI File Manager available and no file has been
				// selected, complain:
				} else if (btnSelectFile.isShown() && importFile == null) {
					Toast.makeText(v.getContext(),
							R.string.error_import_no_file_selected,
							Toast.LENGTH_LONG).show();
				// If we pass those tests, we should be good.  The file spinner
				// is restricted to actual files, so that should be OK.  Get
				// the full file path and password and launch the progress
				// dialog, which starts the import.
				} else {
					File theFile = null;
					if (btnSelectFile.isShown()) theFile = new File(importFile);
					else theFile = new File(importRootPath, (String)spinnerFiles.getSelectedItem());
					importFile = theFile.getAbsolutePath();
					password = txtPassphrase.getText().toString();
					showDialog(DIALOG_PROGRESS);
				}
			}
        });
        
        /** What to do when the Select File button is clicked */
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Get a handier reference to the app's FileManager object:
				FileManager fm = theApp.getFileManager();
				try {
					// Check to see if the user has an active preference for file
					// manager.  This should only occur if one is actively available.
					// If they do, generate the intent for the selected file manager
					// and start that activity up:
					if (fm.isFileManagerSelected()) {
						Intent intent = fm.generateSelectFileIntent(importRootPath.toString(),
								getResources().getString(R.string.import_file_dialog_title),
								getResources().getString(R.string.import_file_dialog_button));
						startActivityForResult(intent,
								FileManager.INTENT_REQUEST_SELECT_FILE);
					// If no file manager is available, this button should be available.
					// If for some bizarre reason it gets pressed, complain:
					} else Toast.makeText(v.getContext(),
							R.string.error_no_external_file_manager,
							Toast.LENGTH_LONG).show();
				// There is a chance that we'll try to fire off the intent to pick a
				// file and end up generating an exception.  (See Issue $6.)  The most
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

        // Finally, since we need to know what sites we may potentially overwrite,
        // go ahead and get the current site list now:
        theApp.requestSiteList(this, this);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	// Look at the request code:
    	String filename = null;
    	switch (requestCode) {
    		// If we're returning from a file selection, get the file name from
    		// the file manager:
	    	case FileManager.INTENT_REQUEST_SELECT_FILE:
	    		if (resultCode == RESULT_OK && data != null)
	    			filename = theApp.getFileManager().getSelectedFile(data);
	    		break;
    	}
    	// Now see if we got anything useful:
        if (filename != null) {
            // Get rid of URI prefix if present:
            if (filename.startsWith("file://")) {
                    filename = filename.substring(7);
            }
            // Take note of the selected file's name:
            importFile = filename;
            // Update the button text:
            btnSelectFile.setText(getResources().getString(R.string.import_file_button_prompt) +
            		" " + filename);
            // Update the import root path with the directory
            // containing the selected file.  This way, if they
            // tap the button again, they'll start in the same
            // place they left off.
            File tempFile = new File(filename);
            importRootPath = tempFile.getParentFile();
        }
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
	    	// The progress dialog does the work of the actual export.  We'll
	    	// create this dialog, create an exporter object and pass a
	    	// reference to the dialog along with it. 
	    	case DIALOG_PROGRESS:
	    		// Create the progress dialog:
	    		progressDialog = new ProgressDialog(this);
	    		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    		progressDialog.setMax(100);
	            progressDialog.setMessage(getResources().getString(R.string.import_progress_message));
	            // Create the importer and put it to work:
	    		importer = new ImportExportHandler(theActivity,
	    			progressDialog, DIALOG_PROGRESS);
	    		importer.importFromFile(importFile, password, this);
	            dialog = progressDialog;
	    		break;
	    	// This dialog allows the user selectively import sites from a file by
	    	// displaying the list of sites in the file as a scrolling list of
	    	// checkboxes.  The user may select to import one or more sites
	    	// individually, select all sites at once, or cancel out of the list.
	    	case DIALOG_IMPORTED_SITES:
	    		AlertDialog.Builder adb = new AlertDialog.Builder(this);
    			adb.setTitle(R.string.export_select_sites_title);
    			adb.setCancelable(false);
    			// While we have the SiteParameter objects in memory, we don't
    			// have a convenient list of names.  Build a string array of just
    			// the names to use in the selection list below.
    			String[] importedSiteNames = new String[importedSites.length];
    			for (int i = 0; i < importedSites.length; i++) {
    				importedSiteNames[i] = ((SiteParameters)importedSites[i]).getSite();
    			}
    			// This is where it gets a bit funky.  This lets us create the
    			// actual checkbox list.  Pass in the array of site names built
    			// above, as well as the boolean array of which sites have been
    			// checked.  Then we create a listener that flips the bits in the
    			// selection array each time a checkbox is toggled.
    			adb.setMultiChoiceItems(importedSiteNames, selectedSites,
    				new OnMultiChoiceClickListener() {
    				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
    					selectedSites[which] = isChecked;
    				}
    			});
    			// What to do when the OK button is clicked:
    			adb.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
  		           public void onClick(DialogInterface dialog, int id) {
  		        	   // Just clear the dialog and move on to the next step:
 		        	   theActivity.removeDialog(DIALOG_IMPORTED_SITES);
 		        	  importedSitesSelected();
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
 		        	   // Now remove the dialog and move on to the next step:
 		        	   theActivity.removeDialog(DIALOG_IMPORTED_SITES);
 		        	  importedSitesSelected();
 		           }
 		       });
    			// What to do when the Cancel button is clicked
    			adb.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   // If the user clicks Cancel, clear the dialog and close
 		        	   // the activity so we can return to the main menu:
						Toast.makeText(theActivity, R.string.error_import_aborted,
										Toast.LENGTH_LONG).show();
						theActivity.removeDialog(DIALOG_IMPORTED_SITES);
						theActivity.finish();
 		           }
 		       });
    			dialog = (Dialog)adb.create();
				break;
			// This dialog is displayed if we've discovered that at least one site from
			// the import file will overwrite something that already exists:
	    	case DIALOG_OVERWRITE_WARNING:
	    		AlertDialog.Builder adb2 = new AlertDialog.Builder(this);
    			adb2.setTitle(R.string.import_overwrite_warning_dialog_title);
    			adb2.setMessage(R.string.import_overwrite_warning_dialog_text);
    			adb2.setCancelable(true);
    			// What to do when the OK button is clicked:
    			adb2.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
  		           public void onClick(DialogInterface dialog, int id) {
  		        	   // Remove this dialog and proceed to the final import:
  		        	 theActivity.removeDialog(DIALOG_OVERWRITE_WARNING);
  		        	   doFinalImport();
  		           }
    			});
    			adb2.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
  		           public void onClick(DialogInterface dialog, int id) {
  		        	   // Saying no is the same as canceling:
  		        	   dialog.cancel();
  		           }
    			});
    			adb2.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						// Clear the dialog, close the activity, and let the user know
						// we're giving up:
	  		        	   theActivity.removeDialog(DIALOG_OVERWRITE_WARNING);
	  		        	 Toast.makeText(theActivity, R.string.error_import_aborted,
	  		    				Toast.LENGTH_LONG).show();
	  		        	   theActivity.finish();
					}
				});
    			dialog = (Dialog)adb2.create();
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
     * Disable the form elements not needed if there are no
     * files suitable for import.
     */
    private void disableForm()
    {
    	btnSelectFile.setVisibility(Button.INVISIBLE);
    	spinnerFiles.setVisibility(Spinner.INVISIBLE);
    	txtPassphrase.setVisibility(EditText.INVISIBLE);
    	labelPassphrase.setVisibility(TextView.INVISIBLE);
    	labelWarning.setVisibility(TextView.INVISIBLE);
    	btnImport.setVisibility(Button.INVISIBLE);
    	labelInstructions.setText(R.string.import_file_label_nofile);
    }

    /**
     * Once the user has made their selection of sites to import, look at the
     * selection and see if anything actually needs to be done.  If so, check
     * to see if any of the imported sites will overwrite an existing site and
     * display a warning if necessary.  Otherwise, proceed with the import.
     */
    private void importedSitesSelected() {
    	// Get the count of sites selected in the dialog:
    	int selectedCount = 0;
    	for (int i = 0; i < selectedSites.length; i++)
    		if (selectedSites[i]) selectedCount++;
    	// Were any sites selected?
    	if (selectedCount > 0) {
    		// Check to see if any of the imported sites will overwrite an
    		// existing site.  We'll do this by looping through the imported
    		// site list, check to see if that site is currently selected.  If
    		// it is, we'll check the master list to see if the site token is
    		// already in there.  Note that if we encounter something that will
    		// be overwritten, we'll bail out early since there's no point in
    		// continuing the loop.
    		boolean willOverwrite = false;
    		for (int j = 0; j < importedSites.length; j++) {
    			if (selectedSites[j]) {
    				SiteParameters site = (SiteParameters)importedSites[j];
    				if (theApp.siteListContainsSite(site.getSite())) {
    					willOverwrite = true;
    					break;
    				}
    			}
    		}
    		// If we'll overwrite something, show the warning dialog.  Otherwise,
    		// move on to the import.
    		if (willOverwrite) showDialog(DIALOG_OVERWRITE_WARNING);
    		else doFinalImport();
    	// If no sites were selected, complain and exit:
    	} else {
    		Toast.makeText(this, R.string.error_import_no_sites_selected,
    				Toast.LENGTH_LONG).show();
    		finish();
    	}
    }
    
    /**
     * Perform the final step of actually importing the selected sites from the file
     * into the database.  Note that this method assumes the site list is populated
     * and the user has selected one or more sites to import.
     */
    private void doFinalImport() {
    	// Asbestos underpants:
    	try {
    		// Whatever happens here, we should probably force the site list to be
    		// rebuilt the next time it is needed:
    		theApp.setSiteListDirty();
    		// Keep track of how many sites we'll import:
    		int count = 0;
    		// Loop through the list of imported sites.  If the site was selected,
    		// try to add it to the database and count it:
    		for (int i = 0; i < importedSites.length; i++) {
    			if (selectedSites[i]) {
    				dbHelper.createRecord((SiteParameters)importedSites[i]);
    				count++;
    			}
    		}
    		// Build our success message:
    		String message = getResources().getString(R.string.import_complete_message);
    		message = message.replace(getResources().getString(R.string.meta_replace_token),
    				String.valueOf(count));
    		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    	// Something blew up:
    	} catch (Exception e) {
    		Toast.makeText(this, R.string.error_import_unknown_error,
    				Toast.LENGTH_LONG).show();
    	}
    	// Regardless of what happens above, exist out of this activity and return
    	// to the main menu:
    	finish();
    }
    
	public void onSitesImported(Object[] importedSites) {
		// Check the list of sites returned by the importer.  If the list is non-
		// empty, take note of the list.
		if (importedSites != null && importedSites.length > 0) {
			this.importedSites = importedSites;
			// We'll need to keep track of which sites get selected in the dialog
			// for the next step.  Create a boolean array of the same size and
			// default all items in it to false.
			selectedSites = new boolean[importedSites.length];
			for (int i = 0; i < importedSites.length; i++)
				selectedSites[i] = false;
			// Show the dialog to let the user select which sites to import:
			showDialog(DIALOG_IMPORTED_SITES);
		// If the list that was returned was empty, complain:
		} else {
			Toast.makeText(this, R.string.error_bad_import_file_or_password,
					Toast.LENGTH_LONG).show();
		}
	}

	public void onSiteListReady(String[] siteList) {
		// This has to be the simplest site list request handling in the entire
		// app.  We don't really care about the result of the site list as we
		// aren't going to do anything with it.  However, we want to make sure
		// it gets built so we can search the list (through the main app class)
		// and see if we're going to overwrite something.
	}
    
}
