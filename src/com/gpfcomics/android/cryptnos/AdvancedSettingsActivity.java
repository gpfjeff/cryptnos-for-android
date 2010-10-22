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
 * Cryptnos, such as which text encoding to use.
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * This activity allows the user to tweak some of the more advanced settings within
 * Cryptnos, such as which text encoding to use.
 * @author Jeffrey T. Darlington
 * @version 1.2
 * @since 1.2
 */
public class AdvancedSettingsActivity extends Activity {

	/** A constant identifying the confirmation dialog displayed if the user
	 *  upgrades changes the text encoding selection */
	private static final int DIALOG_CONFIRM_ENCODING_CHANGE = 700;

	/** A constant indicating the Help option menu item. */
	private static final int OPTMENU_HELP = Menu.FIRST;

	/** The Spinner holding our text encoding options */
	private Spinner spinEncodings = null;
	
	private TextView labelDefaultEncoding = null;
	
	/** A reference back to the main application */
	private CryptnosApplication theApp = null;
	
	/** The SharedPreferences for Cryptnos, obtained from the
	 *  CryptnosApplication.getPrefs() call.  We'll put this in its own
	 *  variable for convenience. */
	private SharedPreferences prefs = null;
	
	/** The current selection in the encoding spinner.  This is used to restore the
	 *  previous selection if the user cancels the change. */
	private int lastEncodingSelection = 0;
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        
        // Get references to the app and preferences:
        theApp = (CryptnosApplication)getApplication();
        prefs = theApp.getPrefs();
        
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
        spinEncodings.setAdapter(adapter);
        spinEncodings.setSelection(lastEncodingSelection);

        // Set up the encoding spinner's selection listener.  If the user selects
        // anything different from the current selection, show the confirmation
        // dialog to make sure they want to make the change.
        spinEncodings.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				if (spinEncodings.getSelectedItemPosition() != lastEncodingSelection)
					showDialog(DIALOG_CONFIRM_ENCODING_CHANGE);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) { }
        });
        
        // Set up the default encoding label:
        labelDefaultEncoding = (TextView)findViewById(R.id.labelDefaultEncoding);
        String labelText =
        	getResources().getString(R.string.settings_default_encoding_label);
        labelDefaultEncoding.setText(labelText.replace(getResources().getString(R.string.meta_replace_token),
        		systemDefault));
    }
    
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog = null;
    	final Activity theActivity = this;
    	switch (id)
    	{
    		// Set up the confirmation dialog for changing the encoding:
			case DIALOG_CONFIRM_ENCODING_CHANGE:
				AlertDialog.Builder adb = new AlertDialog.Builder(this);
				dialog = (Dialog)adb.create();
				adb.setMessage(getResources().getString(R.string.settings_confirm_encoding_change));
				adb.setCancelable(true);
				adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					// If they said yes, change the preferences to use the new
					// character encoding, refresh the parameter salt, and 
					// take note of the new encoding position.
					@Override
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
    				@Override
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   dialog.cancel();
 		           }
 		       	});
    			// If they cancel the dialog, dismiss it and reset the encoding
    			// selection back to where it was before:
    			adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						theActivity.dismissDialog(DIALOG_CONFIRM_ENCODING_CHANGE);
						spinEncodings.setSelection(lastEncodingSelection);
					}
				});
				dialog = (Dialog)adb.create();
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