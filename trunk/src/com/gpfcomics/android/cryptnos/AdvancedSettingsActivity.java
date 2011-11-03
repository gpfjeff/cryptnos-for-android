/* AdvancedSettingsActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          October 22, 2010
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This activity allows the user to tweak some of the more advanced settings within
 * Cryptnos, such as which text encoding to use.  Added in Cryptnos 1.2.0.
 * 
 * UPDATES FOR 1.2.1:  Minor UI enhancements
 * 
 * UPDATES FOR 1.2.4:  Added checkbox to manage the "copy to clipboard" setting
 * 
 * UPDATES FOR 1.3.0:  Added controls for QR scanner preference and "show master
 * passwords" setting
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

import java.nio.charset.Charset;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * This activity allows the user to tweak some of the more advanced settings within
 * Cryptnos, such as which text encoding to use.
 * @author Jeffrey T. Darlington
 * @version 1.3.0
 * @since 1.2
 */
public class AdvancedSettingsActivity extends Activity {

	/** A constant identifying the confirmation dialog displayed if the user
	 *  upgrades changes the text encoding selection */
	private static final int DIALOG_CONFIRM_ENCODING_CHANGE = 700;
	
	/** A constant identifying the confirmation dialog displayed if the user
	 *  turns on the "show master passwords" setting */
	private static final int DIALOG_SHOW_MASTER_PASSWD_WARNING =
		DIALOG_CONFIRM_ENCODING_CHANGE + 1;

	/** A constant indicating the Help option menu item. */
	private static final int OPTMENU_HELP = Menu.FIRST;

	/** The Spinner holding our text encoding options */
	private Spinner spinEncodings = null;
	
	/** A TextView to display the system default encoding, for reference */
	private TextView labelDefaultEncoding = null;
	
	/** A spinner to show the available file manager list */
	private Spinner spinFileManagers = null;
	
	/** A TextView prompt for the file manager spinner */
	private TextView labelFileManagerPreference = null;
	
	/** A Text View displayed if no file managers were found */
	private TextView labelNoFileManagersAvailable = null;
	
	/** A spinner to show the available QR code scanner list */
	private Spinner spinQRScanners = null;
	
	/** A TextView prompt for the QR code scanner spinner */
	private TextView labelQRScannerPreference = null;
	
	/** A Text View displayed if no QR code scanners were found */
	private TextView labelNoQRScannersAvailable = null;
	
	/** A CheckBox to manage the "copy password to clipboard" setting */
	private CheckBox chkCopyPasswordsToClipboard = null;
	
	/** A CheckBox to manage the "show master passwords" setting */
	private CheckBox chkShowMasterPasswords = null;
	
	/** A reference to the linear layout that contains our UI elements */
	private LinearLayout layout = null;

	/** A reference back to the main application */
	private CryptnosApplication theApp = null;
	
	/** The SharedPreferences for Cryptnos, obtained from the
	 *  CryptnosApplication.getPrefs() call.  We'll put this in its own
	 *  variable for convenience. */
	private SharedPreferences prefs = null;
	
	/** The current selection in the encoding spinner.  This is used to restore the
	 *  previous selection if the user cancels the change. */
	private int lastEncodingSelection = 0;
	
	/** The current selection in the file manager spinner.  This is used to restore
	 *  the previous selection if the user cancels the change. */
	private int lastFileManagerSelection = 0;
	
	/** The current selection in the QR scanner spinner.  This is used to restore
	 *  the previous selection if the user cancels the change. */
	private int lastQRScannerSelection = 0;
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        
        // Get references to the app and preferences:
        theApp = (CryptnosApplication)getApplication();
        prefs = theApp.getPrefs();
        
        // Get a reference to our layout:
        layout = (LinearLayout)findViewById(R.id.layoutSettings);
        
        // Get the current system default text encoding.  We'll use this to display
        // the default in a label later.
        String systemDefault = null;
        try { systemDefault = System.getProperty("file.encoding", "Unknown"); }
        catch (Exception ex) { systemDefault = "Unknown"; }
        
        // Get the current text encoding value from the preferences if possible,
        // falling back to the system encoding or just plain UTF-8 if necessary:
        String currentEncoding = null;
        try {
        	currentEncoding = prefs.getString(CryptnosApplication.PREFS_TEXT_ENCODING,
        		System.getProperty("file.encoding",
        				CryptnosApplication.TEXT_ENCODING_UTF8));
        }
        catch (Exception e) {
        	currentEncoding = prefs.getString(CryptnosApplication.PREFS_TEXT_ENCODING,
            		CryptnosApplication.TEXT_ENCODING_UTF8);
        }
        
        // Set up the encodings spinner.  First we'll get a reference, then get a
        // list of all encodings the system supports.  We'll find out where the
        // current encoding is in the list, then create an adapter for the list,
        // assign that to the spinner, and make the current encoding the selected
        // value.
        spinEncodings = (Spinner)findViewById(R.id.spinEncodings);
        String[] charsets = Charset.availableCharsets().keySet().toArray(new String[1]);
        for (int i = 0; i < charsets.length; i++) {
        	if (charsets[i].compareTo(currentEncoding) == 0) {
        		lastEncodingSelection = i;
        		break;
        	}
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        		android.R.layout.simple_spinner_item, charsets);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinEncodings.setAdapter(adapter);
        spinEncodings.setSelection(lastEncodingSelection, true);
        spinEncodings.setPrompt(getResources().getString(R.string.settings_encoding_prompt));

        // Set up the encoding spinner's selection listener.  If the user selects
        // anything different from the current selection, show the confirmation
        // dialog to make sure they want to make the change.
        spinEncodings.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				if (spinEncodings.getSelectedItemPosition() != lastEncodingSelection)
					showDialog(DIALOG_CONFIRM_ENCODING_CHANGE);
			}
			public void onNothingSelected(AdapterView<?> arg0) { }
        });
        
        // Set up the default encoding label:
        labelDefaultEncoding = (TextView)findViewById(R.id.labelDefaultEncoding);
        String labelText =
        	getResources().getString(R.string.settings_default_encoding_label);
        labelDefaultEncoding.setText(labelText.replace(getResources().getString(R.string.meta_replace_token),
        		systemDefault));
        
        // Get handy references to the file manager UI elements:
    	spinFileManagers = (Spinner)findViewById(R.id.spinFileManagers);
    	labelFileManagerPreference = (TextView)findViewById(R.id.labelFileManagerPreference);
    	labelNoFileManagersAvailable = (TextView)findViewById(R.id.labelNoFileManagersAvailable);

    	// Get the app FileManager object and a list of available file managers.
    	// If any file managers were found, set up the spinner to let the user
    	// chose:
        FileManager fm = theApp.getFileManager();
        int[] availableFMs = fm.getAvailableFileManagers();
        if (availableFMs != null && availableFMs.length > 0) {
        	// Hide the "no file managers found" label:
        	//labelNoFileManagersAvailable.setVisibility(View.INVISIBLE);
        	layout.removeView(labelNoFileManagersAvailable);
        	// Get the list of file manager names, as well as the currently
        	// selected one by name:
        	String[] fmList = fm.getAvailableFileManagerNames();
        	String selected = fm.getPreferredFileManagerName();
        	// Default to the first position in the list, which should be the
        	// "no preference" option:
        	lastFileManagerSelection = 0;
        	// There should be a better way to do this, but step through the list
        	// and compare the selected name with the names in the list.  If we
        	// find it, take note of its position.  Fortunately, this list should
        	// be small, so this should taken long.
        	for (int i = 0; i < fmList.length; i++) {
        		if (fmList[i].compareTo(selected) == 0) {
        			lastFileManagerSelection = i;
        			break;
        		}
        	}
        	// Set up the adapter with the spinner:
            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this,
            		android.R.layout.simple_spinner_item, fmList);
    		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinFileManagers.setAdapter(adapter2);
            // Set the current selection and prompt:
            spinFileManagers.setSelection(lastFileManagerSelection, true);
            spinFileManagers.setPrompt(getResources().getString(R.string.settings_file_manager_prompt));
        } else {
        	layout.removeView(spinFileManagers);
        	layout.removeView(labelFileManagerPreference);
        	labelNoFileManagersAvailable.setText(labelNoFileManagersAvailable.getText().toString() +
        			fm.getRecognizedFileManagerNames());
        }
        
        // Set the action when the file manager spinner changes.  Basically, we'll
        // just set the new preference in the FileManager, which will handle the
        // real work behind the scenes.
        spinFileManagers.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				if (spinFileManagers.getSelectedItemPosition() != lastFileManagerSelection) {
					theApp.getFileManager().setPreferredFileManager((String)spinFileManagers.getSelectedItem());
					lastFileManagerSelection = spinFileManagers.getSelectedItemPosition();
				}
			}
			public void onNothingSelected(AdapterView<?> arg0) { }
        });
        
        // Get handy references to the QR scanner UI elements:
        spinQRScanners = (Spinner)findViewById(R.id.spinQRScanners);
        labelQRScannerPreference = (TextView)findViewById(R.id.labelQRScannerPreference);
        labelNoQRScannersAvailable = (TextView)findViewById(R.id.labelNoQRScannersAvailable);

        // This is pretty much exactly like the file manager selection UI,
        // only this time we'll set up the QR code scanner selection.  Make
        // we *can* scan QR codes, then get the current selection and available
        // scanners and set up the spinner with the current selection ready.  If
        // no scanners are available, hide the spinner and let the user know
        // which scanners are available.
        QRCodeHandler qrch = theApp.getQRCodeHandler();
        if (qrch.canHandleQRCodes()) {
        	layout.removeView(labelNoQRScannersAvailable);
        	String[] qrsList = qrch.getAvailableQRCodeAppNames();
        	String selectedQR = qrch.getPreferredQRCodeAppName();
        	lastQRScannerSelection = 0;
        	for (int i = 0; i < qrsList.length; i++) {
        		if (qrsList[i].compareTo(selectedQR) == 0) {
        			lastQRScannerSelection = i;
        			break;
        		}
        	}
            ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this,
            		android.R.layout.simple_spinner_item, qrsList);
            adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinQRScanners.setAdapter(adapter3);
            spinQRScanners.setSelection(lastQRScannerSelection, true);
            spinQRScanners.setPrompt(getResources().getString(R.string.settings_qrscanner_prompt));
        } else {
        	layout.removeView(spinQRScanners);
        	layout.removeView(labelQRScannerPreference);
        	labelNoQRScannersAvailable.setText(labelNoQRScannersAvailable.getText().toString() +
        			qrch.getRecognizedQRScannerNames());
        }
        
        // Set up the QR scanner spinner.  Again, like the file manager spinner,
        // update the preference when something is selected other than the option
        // already selected.
        spinQRScanners.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				if (spinQRScanners.getSelectedItemPosition() != lastQRScannerSelection) {
					theApp.getQRCodeHandler().setPreferredQRCodeApp((String)spinQRScanners.getSelectedItem());
					lastQRScannerSelection = spinQRScanners.getSelectedItemPosition();
				}
			}
			public void onNothingSelected(AdapterView<?> arg0) { }
        });
        
        // Get the checkbox to handle the "copy passwords to clipboard" setting
        // and set it to the current state of the setting.  Then give the checkbox
        // some functionality and let it toggle the setting in the preferences.
        chkCopyPasswordsToClipboard =
        	(CheckBox)findViewById(R.id.chkCopyPasswordsToClipboard);
        chkCopyPasswordsToClipboard.setChecked(theApp.copyPasswordsToClipboard());
        chkCopyPasswordsToClipboard.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				theApp.toggleCopyPasswordsToClipboard();
			}
        });
        
        // Get the checkbox for the "show master passwords" setting and set it to
        // the current state of the setting.  Then give the checkbox some functionality
        // and let it toggle the setting in the preferences.  Note that unlike the
        // clipboard setting, this time we'll throw a warning when the user turns
        // this setting on.
        chkShowMasterPasswords = 
        	(CheckBox)findViewById(R.id.chkShowMasterPasswords);
        chkShowMasterPasswords.setChecked(theApp.showMasterPasswords());
        chkShowMasterPasswords.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (chkShowMasterPasswords.isChecked())
					showDialog(DIALOG_SHOW_MASTER_PASSWD_WARNING);
				else theApp.toggleShowMasterPasswords();
			}
        });
        
    }
    
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog = null;
    	final Activity theActivity = this;
    	switch (id)
    	{
    		// Set up the confirmation dialog for changing the encoding:
			case DIALOG_CONFIRM_ENCODING_CHANGE:
				AlertDialog.Builder adb = new AlertDialog.Builder(this);
				adb.setTitle(getResources().getString(R.string.settings_confirm_encoding_change_title));
				adb.setMessage(getResources().getString(R.string.settings_confirm_encoding_change));
				adb.setCancelable(true);
				adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					// If they said yes, change the preferences to use the new
					// character encoding, refresh the parameter salt, and 
					// take note of the new encoding position.
					public void onClick(DialogInterface dialog, int which) {
						try {
							String newEncoding = (String)spinEncodings.getSelectedItem();
							theApp.setTextEncoding(newEncoding);
							theApp.refreshParameterSalt();
							lastEncodingSelection = spinEncodings.getSelectedItemPosition();
						}
						catch (Exception ex) {
							Toast.makeText(getBaseContext(), ex.getMessage(),
									Toast.LENGTH_LONG).show();
						}
					}
	    		});
				// If they said no, simply cancel the dialog.  Canceling does the
				// same as saying no, so we'll handle both cases below.
    			adb.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   dialog.cancel();
 		           }
 		       	});
    			// If they cancel the dialog, dismiss it and reset the encoding
    			// selection back to where it was before:
    			adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						theActivity.dismissDialog(DIALOG_CONFIRM_ENCODING_CHANGE);
						spinEncodings.setSelection(lastEncodingSelection, true);
					}
				});
				dialog = (Dialog)adb.create();
				break;
    		// Set up the confirmation dialog for displaying master passwords:
			case DIALOG_SHOW_MASTER_PASSWD_WARNING:
				AlertDialog.Builder adb2 = new AlertDialog.Builder(this);
				adb2.setTitle(getResources().getString(R.string.settings_show_master_passwd_dialog_title));
				adb2.setMessage(getResources().getString(R.string.settings_show_master_passwd_dialog_text));
				adb2.setCancelable(true);
				adb2.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					// If they said yes, change the preference to show the master
					// passwords and store the updated value to the preferences
					// file.  Then dismiss this dialog.
					public void onClick(DialogInterface dialog, int which) {
						theApp.toggleShowMasterPasswords();
						theActivity.dismissDialog(DIALOG_SHOW_MASTER_PASSWD_WARNING);
					}
	    		});
				// If they said no, simply cancel the dialog.  Canceling does the
				// same as saying no, so we'll handle both cases below.
    			adb2.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   dialog.cancel();
 		           }
 		       	});
    			// If they cancel the dialog, dismiss it and reset the checkbox back
    			// to the the unchecked state:
    			adb2.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						chkShowMasterPasswords.setChecked(false);
						theActivity.dismissDialog(DIALOG_SHOW_MASTER_PASSWD_WARNING);
					}
				});
				dialog = (Dialog)adb2.create();
				break;
    	}
    	return dialog;
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	// Add the "Help" menu items:
    	menu.add(0, OPTMENU_HELP, Menu.NONE,
        	R.string.optmenu_help).setIcon(android.R.drawable.ic_menu_help);
    	return true;
    }
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
	    	// If the Help item is selected, open up the "Advanced Settings"
    		// help.  (Should this open the help index, rather than this
    		// specific item?  I'm not sure.  We can always change that, but
    		// such a change will require using the HelpMenuActivity rather
    		// than the HelpActivity.)
	    	case OPTMENU_HELP:
	        	Intent i = new Intent(this, HelpActivity.class);
	        	i.putExtra("helptext", R.string.help_text_settings);
	        	startActivity(i);
	    		return true;
    	}
    	return false;
    }
	

}
