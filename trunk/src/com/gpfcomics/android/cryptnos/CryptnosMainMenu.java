/* CryptnosMainMenu.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          December 8, 2009
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This activity serves as the "official" Cryptnos "main menu", giving the
 * user the option of which path to take for dealing with their site
 * parameters.  The activities that actually perform the work of Cryptnos
 * are launched from here.  The list of options is somewhat dynamic.  There
 * are two sets of options; one set is always available (generate a new set
 * of parameters, about, and help) while others only appear when there are
 * items in the database (generate existing, edit, or delete).
 * 
 * UPDATES FOR 1.2.0:  Added Help option menu and Advanced Settings menu item.
 * 
 * UPDATES FOR 1.2.4:  Modified the Advanced Settings menu item to disable the
 * warning dialog.  I've left the dialog code in place for now but just removed the
 * call to it.  While I like the idea of warning the user about changing the
 * text encoding, there are more settings in that activity now that don't
 * warrant the warning, so it's likely confusing.  There's a warning on the
 * encoding drop-down itself which should be sufficient.
 * 
 * UPDATES FOR 1.3.0:  Code to handle import/export via QR code.  While
 * SiteListActivity handles much of the QR export process, the main menu does
 * most of the import process.  Also performed minor tweaks to existing code.
 * 
 * "QR code" is a registered trademark of Denso Wave Incorporated.
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

import java.util.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * The main menu activity for the Cryptnos Android application. 
 * @author Jeffrey T. Darlington
 * @version 1.3.0
 * @since 1.0
 */
public class CryptnosMainMenu extends ListActivity implements SiteListListener {
	
	/* Public Constants *******************************************************/
	
	/** A constant indicating the Help option menu item. */
	public static final int OPTMENU_HELP = Menu.FIRST;
	
	/* Private Constants ********************************************************/

	/** A constant representing the import method dialog */
	private static final int DIALOG_CHOOSE_IMPORT_METHOD = 600;
	/** A constant representing the export method dialog */
	private static final int DIALOG_CHOOSE_EXPORT_METHOD = 
		DIALOG_CHOOSE_IMPORT_METHOD + 1;
	/** A constant representing the warning dialog displayed if a site scanned from 
	 *  a QR code will overwrite an existing site */
	private static final int DIALOG_QRIMPORT_OVERWRITE_WARNING = 
		DIALOG_CHOOSE_EXPORT_METHOD + 1;
	/** A constant representing the file method option index in the import and
	 *  export method dialogs */
	private static final int IMPORT_EXPORT_METHOD_FILE = 0;
	/** A constant representing the QR code method option index in the import and
	 *  export method dialogs */
	private static final int IMPORT_EXPORT_METHOD_QRCODE =
		IMPORT_EXPORT_METHOD_FILE + 1;

	/* Private Members **********************************************************/
	
	/** A reference to our top-level application */
	private CryptnosApplication theApp = null;
	/** Our database adapter */
	private ParamsDbAdapter mDBHelper;
	/** A site parameters object to store parameters scanned from a QR code */
	private SiteParameters siteParamsFromQRCode = null;

	/* Public methods: ***********************************************************/
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try
        {
	        // The usual GUI setup stuff:
	    	super.onCreate(savedInstanceState);
	        setContentView(R.layout.main);
            // Get a reference to the top-level application, as well as the
            // DB helper:
            theApp = (CryptnosApplication)getApplication();
            mDBHelper = theApp.getDBHelper();
        	// Run the UpgradeManager if it hasn't already been run this session:
        	if (!theApp.hasUpgradeManagerRun()) theApp.runUpgradeManager(this);
        }
        // If anything blew up, show an error message in a Toast.  This
        // may not be the best way to do it, but we'll experiment with
        // that later.
        catch (Exception e)
        {
        	Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /** Called just before input is handed to the user. */
    @Override public void onResume() {
    	try {
	    	// Originally, I had the method to build the menu in the onCreate()
	    	// event, which made sense to start with.  The first time we create
	    	// the activity, we need to build the form.  The problem was, whenever
	    	// we came back to the main menu from another internal activity, the
	    	// menu was never rebuilt.  This was readily apparent if you started
	    	// with no site parameters in the database, then added or imported
	    	// some.  When you returned to the main menu, it would still be the
	    	// same as the initial creation and wouldn't show things like generate
	    	// existing, edit, or export, all options that require data to be
	    	// in the database.  Moving this step to onResume(), however makes the
	    	// menu be rebuilt whenever we return to it, meaning it will be updated
	    	// to reflect changes that occur based on adding or removing sites.
	    	buildMenu();
	    	super.onResume();
    	} catch (Exception e)
        {
        	Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    @SuppressWarnings("unchecked")
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Let the super do whatever it needs to do:
    	super.onListItemClick(l, v, position, id);
    	// This seems a bit weird, but here's how this seems to work.  Use
    	// the internal list view to get the item at the selected position,
    	// which is the HashMap we used to create the menu item with.  Then
    	// use the HashMap.get() method to get the "line1" value, which is
    	// the main text of the menu, and hold onto it.  We'll use that to
    	// check out what activity needs to be performed in each case.
        HashMap<String,String> menuItemHash = 
        	(HashMap<String,String>)getListView().getItemAtPosition(position);
        String menuItem = (String)menuItemHash.get("line1");
        // Go ahead and get a copy of our resources, as we'll need that when we
        // compare menu text strings below:
        Resources res = this.getResources();
        // We may not always need this, but grab the QR code handler as well:
        QRCodeHandler qrCodeHandler = theApp.getQRCodeHandler();
        // For imports and exports, it will help if we know what the app is
        // capable of.  Find out whether or not we can read and/or write external
        // storage and scan/generate QR codes and take note of our capabilities.
    	boolean canScanQRs = qrCodeHandler.canScanQRCodes();
    	boolean canGenQRs = qrCodeHandler.canGenerateQRCodes();
    	boolean canWriteToSD = theApp.canWriteToExternalStorage();
    	boolean canReadFromSD = theApp.canReadFromExternalStorage();
        // Launch the help/tutorial activity
        if (menuItem.compareTo(res.getString(R.string.mainmenu_help1)) == 0)
        {
        	Intent i = new Intent(this, HelpMenuActivity.class);
        	startActivity(i);
        }
        // Launch the generate new password activity:
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_generate1)) == 0)
        {
        	Intent i = new Intent(this, EditParametersActivity.class);
        	startActivity(i);
        }
        // Launch the edit activity menu (i.e. display the site listing so
        // the user can pick some parameters to edit):
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_edit1)) == 0)
        {
        	Intent i = new Intent(this, SiteListActivity.class);
        	i.putExtra("mode", SiteListActivity.MODE_EDIT);
        	startActivity(i);
        }
        // Launch the edit activity menu (i.e. display the site listing so
        // the user can pick some parameters to edit):
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_existing1)) == 0)
        {
        	Intent i = new Intent(this, SiteListActivity.class);
        	i.putExtra("mode", SiteListActivity.MODE_EXISTING);
        	startActivity(i);
        }
        // Launch the delete activity menu (i.e. display the site listing so
        // the user can pick some parameters to delete):
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_delete1)) == 0)
        {
        	Intent i = new Intent(this, SiteListActivity.class);
        	i.putExtra("mode", SiteListActivity.MODE_DELETE);
        	startActivity(i);
        }
        // Choose an export method:
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_export1)) == 0)
        {
        	// If we can both generate QR codes and write to external storage, ask the
        	// user which export method they'd like to use:
        	if (canGenQRs && canWriteToSD) {
        		showDialog(DIALOG_CHOOSE_EXPORT_METHOD);
        	// If they can generate QR codes but can't write to external storage, go
        	// ahead and launch site list activity in QR export mode:
        	} else if (canGenQRs && !canWriteToSD) {
	        	Intent eqi = new Intent(this, SiteListActivity.class);
	        	eqi.putExtra("mode", SiteListActivity.MODE_EXPORT_QR);
	        	startActivity(eqi);
        	// If they can't generate QR codes but can write to external storage, go
        	// ahead and launch the classic export activity:
        	} else if (!canGenQRs && canWriteToSD) {
	        	Intent i = new Intent(this, ExportActivity.class);
	        	startActivity(i);
        	}
        }
        // Choose an import method:
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_import1)) == 0)
        {
        	// If we can both scan QR codes and read from external storage, let the
        	// user pick which import method they'd like to use:
        	if (canScanQRs && canReadFromSD) {
        		showDialog(DIALOG_CHOOSE_IMPORT_METHOD);
        	// If they can only scan QR codes, launch the QR code scanner:
        	} else if (canScanQRs && !canReadFromSD) {
				Intent iqri = qrCodeHandler.generateScanIntent();
				startActivityForResult(iqri, QRCodeHandler.INTENT_SCAN_QRCODE);
        	// If they can only read from external storage, send them to the classic
        	// import activity:
        	} else if (!canScanQRs && canReadFromSD) {
	        	Intent i = new Intent(this, ImportActivity.class);
	        	startActivity(i);
        	}
        }
        // Launch the advanced settings activity:
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_advanced1)) == 0)
        {
        	Intent i = new Intent(this, AdvancedSettingsActivity.class);
        	startActivity(i);
        }
        // For the moment, nothing is working.  Show a quick Toast to let
        // the user know that's our fault and not theirs.
        else Toast.makeText(this, R.string.error_not_implemented,
        		Toast.LENGTH_SHORT).show();
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
	    	// If the Help item is selected, open up the "What is Cryptnos?"
    		// help.  (Should this open the help index, rather than this
    		// specific item?  I'm not sure.  We can always change that, but
    		// such a change will require using the HelpMenuActivity rather
    		// than the HelpActivity.)
	    	case OPTMENU_HELP:
	        	Intent i = new Intent(this, HelpActivity.class);
	        	i.putExtra("helptext", R.string.help_text_whatis);
	        	startActivity(i);
	    		return true;
    	}
    	return false;
    }
	
    @Override
    protected Dialog onCreateDialog(int id)
    {
    	Dialog dialog = null;
    	final Activity theActivity = this;
    	switch (id)
    	{
    		// Let the main app handle the upgrade to UTF-8 warning:
			case CryptnosApplication.DIALOG_UPGRADE_TO_UTF8:
				dialog = theApp.onCreateDialog(id);
				break;
	  		// The site list building progress dialog is actually handled by
    		// the application, but we need to attach it here to actually get
    		// it to display.  If we get this value, pass the buck:
    		case CryptnosApplication.DIALOG_PROGRESS:
    			dialog = theApp.onCreateDialog(id);
    			break;
	  		// If there are multiple import methods available, let the user pick which
			// one to use:
			case DIALOG_CHOOSE_IMPORT_METHOD:
				AlertDialog.Builder adb2 = new AlertDialog.Builder(this);
				adb2.setTitle(getResources().getString(R.string.mainmenu_dialog_import_method));
	    		adb2.setCancelable(true);
	    		// The "items" will be a string array from the strings XML file.  The first
	    		// option should always be to a file, while the second will be a QR code.
	    		adb2.setItems(getResources().getStringArray(R.array.importExportMethods),
	    				new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
									// Import from a QR code:
									case IMPORT_EXPORT_METHOD_QRCODE:
										// Launch the preferred QR code scanner:
										QRCodeHandler qrCodeHandler = 
											theApp.getQRCodeHandler();
										Intent iqri = qrCodeHandler.generateScanIntent();
										if (iqri != null)
											theActivity.startActivityForResult(iqri,
												QRCodeHandler.INTENT_SCAN_QRCODE);
										else Toast.makeText(getBaseContext(), "ERROR: Got null intent!", Toast.LENGTH_LONG).show();
										break;
									// The first item, and which should be the default, is
									// to import from a file.  Launch the old import
									// activity:
									case IMPORT_EXPORT_METHOD_FILE:
									default:
							        	Intent ifi = new Intent(theActivity,
							        			ImportActivity.class);
							        	startActivity(ifi);
										break;
									}
								theActivity.removeDialog(DIALOG_CHOOSE_IMPORT_METHOD);
							}
	    		});
    			// What to do if the dialog is canceled:
    			adb2.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						theActivity.removeDialog(DIALOG_CHOOSE_IMPORT_METHOD);
					}
				});
				dialog = (Dialog)adb2.create();
				break;
			// Like the import method, let the user select the method for export:
			case DIALOG_CHOOSE_EXPORT_METHOD:
				AlertDialog.Builder adb3 = new AlertDialog.Builder(this);
				adb3.setTitle(getResources().getString(R.string.mainmenu_dialog_export_method));
	    		adb3.setCancelable(true);
	    		// The "items" will be a string array from the strings XML file.  The first
	    		// option should always be to a file, while the second will be a QR code.
	    		adb3.setItems(getResources().getStringArray(R.array.importExportMethods),
	    				new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								switch (which) {
									// Exporting to a QR code will be handled by the
									// site list activity, which is well suited to adding
									// this functionality:
									case IMPORT_EXPORT_METHOD_QRCODE:
							        	Intent eqi = new Intent(theActivity,
							        			SiteListActivity.class);
							        	eqi.putExtra("mode", SiteListActivity.MODE_EXPORT_QR);
							        	startActivity(eqi);
										break;
									// The first item, and which should be the default, is
									// to export to a file.  Launch the old export
									// activity:
									case IMPORT_EXPORT_METHOD_FILE:
									default:
							        	Intent efi = new Intent(theActivity,
							        			ExportActivity.class);
							        	startActivity(efi);
										break;
									}
								theActivity.removeDialog(DIALOG_CHOOSE_IMPORT_METHOD);
							}
	    		});
    			// What to do if the dialog is canceled:
    			adb3.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						theActivity.removeDialog(DIALOG_CHOOSE_IMPORT_METHOD);
					}
				});
				dialog = (Dialog)adb3.create();
				break;
			// If the user tried to import a site from a QR code and it will overwrite
			// an existing site, warn the user before actually overwriting it:
			case DIALOG_QRIMPORT_OVERWRITE_WARNING:
				// Since this relies on the existence of the site parameter object we
				// got from the scan, we need to test and make sure it's populated.
				// When this process is complete, we null it out to conserve memory
				// and to signal that we're done.  Unfortunately, I ran into issues
				// with this in practices, particularly if this dialog was created and
				// subsequently cleared, then the user rotated the device.  During
				// orientation changes, the activity is disposed of then rebuilt.  In
				// this case, Andorid tries to launch this dialog again.  If the site
				// parameters happen to be null, this code blows up at this point.
				// So we'll test to make sure we have site parameters before trying
				// to launch the dialog.  If we don't, we'll silently fall through
				// and the dialog won't launch.
				if (siteParamsFromQRCode != null) {
					AlertDialog.Builder adb4 = new AlertDialog.Builder(this);
					adb4.setTitle(getResources().getString(R.string.mainmenu_dialog_import_overwrite_title));
					String message = getResources().getString(R.string.mainmenu_dialog_import_overwrite_warn);
					message = message.replace(getResources().getString(R.string.meta_replace_token), 
							siteParamsFromQRCode.getSite());
					adb4.setMessage(message);
		    		adb4.setCancelable(true);
		    		// What to do when the Yes button is clicked:
		    		adb4.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						// If they said yes:
						public void onClick(DialogInterface dialog, int which) {
							try {
								if (siteParamsFromQRCode != null) {
									// Try to add the record to the database:
									mDBHelper.createRecord(siteParamsFromQRCode);
									// Set the site list to "dirty" so it will be rebuilt:
									theApp.setSiteListDirty();
									// Notify the user of our success:
									String message = getResources().getString(R.string.mainmenu_dialog_import_success);
									message = message.replace(getResources().getString(R.string.meta_replace_token),
											siteParamsFromQRCode.getSite());
									Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
									// Clear out the imported site so we can use it again:
									siteParamsFromQRCode = null;
								}
							} catch (Exception e) {
								// Warn the user if we failed, then clear out the imported
								// site data:
								Toast.makeText(getBaseContext(),
										getResources().getString(R.string.mainmenu_dialog_import_error2),
										Toast.LENGTH_LONG).show();
								siteParamsFromQRCode = null;
							}
						}
		    		});
		    		// What to do when the No button is clicked:
	    			adb4.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
	 		           public void onClick(DialogInterface dialog, int id) {
	 		        	   // For now, just cancel the dialog.  We'll follow
	 		        	   // up on that below.
	 		        	   dialog.cancel();
	 		           }
	 		       	});
	    			// What to do if the dialog is canceled:
	    			adb4.setOnCancelListener(new DialogInterface.OnCancelListener() {
						public void onCancel(DialogInterface dialog) {
							// Note that we clear out the imported site here as well,
							// just in case:
							siteParamsFromQRCode = null;
							theActivity.removeDialog(DIALOG_QRIMPORT_OVERWRITE_WARNING);
						}
					});
					dialog = (Dialog)adb4.create();
				}
				break;
    	}
    	return dialog;
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
		QRCodeHandler qrCodeHandler = theApp.getQRCodeHandler();
    	// What result code did we get:
    	switch (requestCode) {
    		// If we're coming back from scanning a QR code:
    		case QRCodeHandler.INTENT_SCAN_QRCODE:
    			// Was the scan successful?
    			if (qrCodeHandler.wasScanSuccessful(resultCode, data)) {
    				// Try to extract the parameters from the scanned text:
    				SiteParameters params = qrCodeHandler.getSiteParamsFromScan(data);
    				// If that was successful, take note of the parameters and request
    				// the current site list from the main application.  This will
    				// probably cause the progress dialog to load.  When we get the
    				// list, our SiteListListener method will handle the next step.
    				if (params != null) {
    					siteParamsFromQRCode = params;
    					theApp.requestSiteList(this, this);
    				// If we got a null from the import, something blew up:
    				} else Toast.makeText(getBaseContext(), 
    						getResources().getString(R.string.mainmenu_dialog_import_error1),
    						Toast.LENGTH_LONG).show();
    			}
    			break;
    	}
    }
    
    /**
     * Build the main application menu.  This menu is somewhat dynamic, in
     * that certain items will only appear if there are existing parameters
     * saved in the database.
     */
    private void buildMenu()
    {
    	try {
	    	// This is a bit wonky, but so far this is the only way I've been
	    	// able to find to get this to work.  First, we need to know how
	    	// many records we have saved in the database.  It won't make much
	    	// sense to show certain menu items if there are no saved parameters.
	        int siteCount = mDBHelper.recordCount();
	        // Here's where we get funky.  To make a list activity, we need a
	        // list to work from.  Most examples do this by mapping to a database
	        // but we haven't gotten to that point yet.  Instead, we're building
	        // a list on the fly.  So we'll start with an ArrayList.  But it can't
	        // be any old ArrayList; it has to have a Map of some sort.  So we'll
	        // use a HashMap to map strings to strings.  The first string will
	        // be the main menu text, while the second will be additional help
	        // text below it.
	        //
	        // Start by making the list itself, then declaring the hash map but
	        // not instantiating it.
	        ArrayList<HashMap<String,String>> menuItems =
	        	new ArrayList<HashMap<String,String>>();
	        HashMap<String,String> item = null;
	        // Get a handier reference to our resources so we can get access to
	        // the strings more quickly:
	        Resources res = getResources();
	        QRCodeHandler qrCodeHandler = theApp.getQRCodeHandler();
	        // The most commonly used item will be generating existing passphrases,
	        // so that should be at the top.  But this only makes sense if there
	        // are already parameters saved.
	        if (siteCount > 0)
	        {
	        	item = new HashMap<String,String>();
	        	item.put("line1", res.getString(R.string.mainmenu_existing1));
	        	item.put("line2", res.getString(R.string.mainmenu_existing2));
	        	menuItems.add(item);
	        }
	        // Add an item for generating new passphrases by entering new sets of
	        // parameters:
	    	item = new HashMap<String,String>();
	    	item.put("line1", res.getString(R.string.mainmenu_generate1));
	    	item.put("line2", res.getString(R.string.mainmenu_generate2));
	    	menuItems.add(item);
	    	// These three require items in the database:
	        if (siteCount > 0)
	        {
	        	// Edit an existing set of parameters:
	        	item = new HashMap<String,String>();
	        	item.put("line1", res.getString(R.string.mainmenu_edit1));
	        	item.put("line2", res.getString(R.string.mainmenu_edit2));
	        	menuItems.add(item);
	        	// Delete a set of parameters:
	        	item = new HashMap<String,String>();
	        	item.put("line1", res.getString(R.string.mainmenu_delete1));
	        	item.put("line2", res.getString(R.string.mainmenu_delete2));
	        	menuItems.add(item);
	        	// Exporting a set of parameters only makes sense if we have sites
	        	// to export and we can either write to the SD card or generate
	        	// QR codes:
	        	if (theApp.canWriteToExternalStorage() || qrCodeHandler.canGenerateQRCodes()) {
		        	item = new HashMap<String,String>();
		        	item.put("line1", res.getString(R.string.mainmenu_export1));
		        	item.put("line2", res.getString(R.string.mainmenu_export2));
		        	menuItems.add(item);
	        	}
	        }
	        // Importing a set of parameters only makes sense if we can either read
	        // from the SD card or scan QR codes:
	        if (theApp.canReadFromExternalStorage() || qrCodeHandler.canScanQRCodes()) {
		    	item = new HashMap<String,String>();
		    	item.put("line1", res.getString(R.string.mainmenu_import1));
		    	item.put("line2", res.getString(R.string.mainmenu_import2));
		    	menuItems.add(item);
	        }
	        // Add the advanced settings item:
	    	item = new HashMap<String,String>();
	    	item.put("line1", res.getString(R.string.mainmenu_advanced1));
	    	item.put("line2", res.getString(R.string.mainmenu_advanced2));
	    	menuItems.add(item);
	        // Add the help/tutorials item:
	    	item = new HashMap<String,String>();
	    	item.put("line1", res.getString(R.string.mainmenu_help1));
	    	item.put("line2", res.getString(R.string.mainmenu_help2));
	    	menuItems.add(item);
	    	// Now that we've got our list, create a SimpleAdapter to map each
	    	// hash map item to certain text fields in a row view.  This is a bit
	    	// weird, but that's the way Android seems to do it.
	    	SimpleAdapter menuAdapter = new SimpleAdapter(
	    			this,
	    			menuItems,
	    			R.layout.mainmenu_row,
	    			new String[] { "line1", "line2" },
	    			new int[] { R.id.text1, R.id.text2 }
	    		);
	    	// Last but not least, set the adapter for the list, making it all
	    	// active:
	    	setListAdapter(menuAdapter);
    	} catch (Exception e) {
        	Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

	public void onSiteListReady(String[] siteList) {
		// Asbestos underpants:
		try {
			// Make sure the site list we got back wasn't null:
			if (siteList != null) {
				// One possibility of return from retrieving the site list is that we
				// imported a set of parameters from a scan and now we want to import it.
				// If that's the case, the parameters should be stored in this member:
				if (siteParamsFromQRCode != null) {
					// If the site token is in the list, show a dialog warning the user
					// that we're about to overwrite an existing site:
					if (theApp.siteListContainsSite(siteParamsFromQRCode.getSite())) {
						showDialog(DIALOG_QRIMPORT_OVERWRITE_WARNING);
					}
					// Otherwise, we'll go ahead and import that site directly into the
					// database:
					else {
						// Try to add the site to the database:
						mDBHelper.createRecord(siteParamsFromQRCode);
						// Mark the site list as "dirty" so it will get rebuilt the next
						// time it's needed:
						theApp.setSiteListDirty();
						// Notify the user of our success:
						String message = getResources().getString(R.string.mainmenu_dialog_import_success);
						message = message.replace(getResources().getString(R.string.meta_replace_token),
								siteParamsFromQRCode.getSite());
						Toast.makeText(this, message, Toast.LENGTH_LONG).show();
						// Empty out the saved site so it's ready if we make another scan:
						siteParamsFromQRCode = null;
					}
				}
			// The site list we got back from the main app code was null:
			} else {
				Toast.makeText(this, R.string.error_bad_listfetch,
	            		Toast.LENGTH_LONG).show();
				siteParamsFromQRCode = null;
			}
		// Something blew up.  Warn the user and empty out the saved site:
		} catch (Exception e) {
			Toast.makeText(this,
					getResources().getString(R.string.mainmenu_dialog_import_error2),
					Toast.LENGTH_LONG).show();
			siteParamsFromQRCode = null;
		}
	}

}