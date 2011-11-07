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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * The main menu activity for the Cryptnos Android application. 
 * @author Jeffrey T. Darlington
 * @version 1.3.0
 * @since 1.0
 */
public class CryptnosMainMenu extends Activity implements SiteListListener {
	
	/* Public Constants *******************************************************/
	
	/** A constant indicating the Help option menu item. */
	public static final int OPTMENU_HELP = Menu.FIRST;
	/** A constant indicating the About option menu item. */
	public static final int OPTMENU_ABOUT = OPTMENU_HELP + 1;
	
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
	
	/** The New menu item identifier */
	private static final int MENUITEM_NEW = 0;
	/** The Regenerate menu item identifier */
	private static final int MENUITEM_REGEN = 1;
	/** The Edit menu item identifier */
	private static final int MENUITEM_EDIT = 2;
	/** The Delete menu item identifier */
	private static final int MENUITEM_DELETE = 3;
	/** The Export menu item identifier */
	private static final int MENUITEM_EXPORT = 4;
	/** The Import menu item identifier */
	private static final int MENUITEM_IMPORT = 5;
	/** The Settings menu item identifier */
	private static final int MENUITEM_SETTINGS = 6;
	/** The Help menu item identifier */
	private static final int MENUITEM_HELP = 7;

	/* Private Members **********************************************************/
	
	/** A reference to our top-level application */
	private CryptnosApplication theApp = null;
	/** Our database adapter */
	private ParamsDbAdapter mDBHelper;
	/** A site parameters object to store parameters scanned from a QR code */
	private SiteParameters siteParamsFromQRCode = null;
	/** A reference to the common QR code handler object */
	private QRCodeHandler qrCodeHandler = null;
	/** The current count of how many sites in within the database */
	private int siteCount = 0;
	/** This array, built in buildMenu(), determines the current capabilities based on
	 *  various factors.  Use the MENUITEM_* constants to determine whether or not a
	 *  given menu item is currently enabled. */
	private boolean[] menuEnabled = null;

	/* Public methods: ***********************************************************/
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try
        {
	        // The usual GUI setup stuff:
	    	super.onCreate(savedInstanceState);
	        setContentView(R.layout.mainmenu_grid);
            // Get a reference to the top-level application, as well as the
            // DB helper and QR code handler:
            theApp = (CryptnosApplication)getApplication();
            mDBHelper = theApp.getDBHelper();
	        qrCodeHandler = theApp.getQRCodeHandler();
	        // We want to put the version number (android:versionName from the
	        // manifest) into the About header.  This is not quite as simple as
	        // it sounds.  In order to do this, we first need to get a hold of
	        // the header TextView so we can modify it:
	        TextView header = (TextView)findViewById(R.id.txtVersionLabel);
	        // This seems like a round-about way of doing things, but get the
	        // PackageInfo for the Cryptnos application package.  From that we
	        // can get the version name string.  Append that to the current value
	        // of the header string, which is populated from the strings.xml
	        // file like everything else.  If that fails for some reason, print
	        // something else to indicate the error.  The advantage here is that
	        // we can update the version string in one place (the manifest) and
	        // that will propagate here with no code changes.
	        try {
		        PackageInfo info =
		        	this.getPackageManager().getPackageInfo(this.getPackageName(),
	        			PackageManager.GET_META_DATA);
		        header.setText(header.getText().toString().concat(" " + info.versionName)); 
	        } catch (Exception e) {
	        	header.setText(header.getText().toString().concat(" ???"));
	        }
        	// Run the UpgradeManager if it hasn't already been run this session:
        	if (!theApp.hasUpgradeManagerRun()) theApp.runUpgradeManager(this);
        }
        // If anything blew up, show an error message in a Toast.  This
        // may not be the best way to do it, but we'll experiment with
        // that later.
        catch (Exception e)
        {
        	Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        	finish();
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
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	// Add the "Help" and "About" menu items:
    	menu.add(0, OPTMENU_HELP, Menu.NONE,
        	R.string.optmenu_help).setIcon(android.R.drawable.ic_menu_help);
    	menu.add(0, OPTMENU_ABOUT, Menu.NONE,
            	R.string.optmenu_about).setIcon(android.R.drawable.ic_menu_info_details);
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
	        	Intent i1 = new Intent(this, HelpActivity.class);
	        	i1.putExtra("helptext", R.string.help_text_whatis);
	        	startActivity(i1);
	    		return true;
	    	// If the About item is selected, show the "About box":
	    	case OPTMENU_ABOUT:
	        	Intent i2 = new Intent(this, AboutActivity.class);
	        	startActivity(i2);
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
    		// Find out how many records are currently in the database:
	        siteCount = mDBHelper.recordCount();
	        // To determine whether or not a menu item is enabled or disabled, we
	        // have to determine our capabilities.  Start by creating a new boolean
	        // array, which will default all to false:
	        menuEnabled = new boolean[8];
	        // These items are always enabled:
	        menuEnabled[MENUITEM_NEW] = true;
	        menuEnabled[MENUITEM_SETTINGS] = true;
	        menuEnabled[MENUITEM_HELP] = true;
	        // The following settings only make sense if there are items
	        // currently in the database:
	        if (siteCount > 0) {
	        	menuEnabled[MENUITEM_REGEN] = true;
	        	menuEnabled[MENUITEM_EDIT] = true;
	        	menuEnabled[MENUITEM_DELETE] = true;
	        	// Exporting requires both sites to be in the database and
	        	// the ability to either write to external storage or the
	        	// ability to generate QR codes:
	        	if (theApp.canWriteToExternalStorage() ||
	        			qrCodeHandler.canGenerateQRCodes()) {
		        	menuEnabled[MENUITEM_EXPORT] = true;
	        	}
	        }
	        // Importing relies on the ability to read from storage or import
	        // QR codes:
	        if (theApp.canReadFromExternalStorage() || qrCodeHandler.canScanQRCodes()) {
	        	menuEnabled[MENUITEM_IMPORT] = true;
	        }
	        // Now get access to the grid view, set its adapter, and figure out what
	        // to do if the menu item is enabled:
	        GridView grid = (GridView)findViewById(R.id.gridMainMenu);
            grid.setAdapter(new MainMenuShortcutAdapter(this));
            grid.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int menuItem,
						long id) {
			        // For imports and exports, it will help if we know what the app is
			        // capable of.  Find out whether or not we can read and/or write external
			        // storage and scan/generate QR codes and take note of our capabilities.
			    	boolean canScanQRs = qrCodeHandler.canScanQRCodes();
			    	boolean canGenQRs = qrCodeHandler.canGenerateQRCodes();
			    	boolean canWriteToSD = theApp.canWriteToExternalStorage();
			    	boolean canReadFromSD = theApp.canReadFromExternalStorage();
			    	// Perform the appropriate task depending on the menu item
			    	// selected:
					switch (menuItem) {
						// For a new set of parameters, launch the Edit utility in its
						// default mode, so we'll start creating a new site:
						case MENUITEM_NEW:
				        	Intent i1 = new Intent(getBaseContext(),
				        			EditParametersActivity.class);
				        	startActivity(i1);
							break;
						// The Regenerate item recreates an existing password but does
						// so in a read-only way so we can't change any of the parameters.
						// Note that this menu item is only enabled if there are one or
						// more parameters in the database.  Also note that this actually
						// launches the site list activity so the user can pick the site
						// to regenerate, but we specify which subsequent action will be
						// the default for a tap.
						case MENUITEM_REGEN:
							if (menuEnabled[MENUITEM_REGEN]) {
					        	Intent i2 = new Intent(getBaseContext(),
					        			SiteListActivity.class);
					        	i2.putExtra("mode", SiteListActivity.MODE_EXISTING);
					        	startActivity(i2);
							}
							break;
						// The Edit item allows us to edit the parameters of a given set
						// of parameters, except for the site token or name.  Like
						// Regenerate, this requires at least one item to be in the
						// database, and we actually call the site list activity but set
						// the default tap action to edit mode.
						case MENUITEM_EDIT:
							if (menuEnabled[MENUITEM_EDIT]) {
					        	Intent i3 = new Intent(getBaseContext(),
					        			SiteListActivity.class);
					        	i3.putExtra("mode", SiteListActivity.MODE_EDIT);
					        	startActivity(i3);
							}
							break;
						// Delete removes one or more sets of parameters from the
						// database.  Like Regenerate and Edit, we need to have something
						// in the database and we pass the buck to the site list activity,
						// setting the default action to delete.
						case MENUITEM_DELETE:
							if (menuEnabled[MENUITEM_DELETE]) {
					        	Intent i4 = new Intent(getBaseContext(),
					        			SiteListActivity.class);
					        	i4.putExtra("mode", SiteListActivity.MODE_DELETE);
					        	startActivity(i4);
							}
							break;
						// Export allows us to transfer one or more sets of parameters to
						// another copy of Cryptnos, either as a means of sharing amongst
						// devices or for backing up our data.  There are currently two
						// export methods, via encrypted file and via QR code, and which
						// options are available depends on the capabilities of the device.
						// Export is only available if we have something we can export.
						case MENUITEM_EXPORT:
							if (menuEnabled[MENUITEM_EXPORT]) {
					        	// If we can both generate QR codes and write to external
								// storage, ask the user which export method they'd like to
								// use:
					        	if (canGenQRs && canWriteToSD) {
					        		showDialog(DIALOG_CHOOSE_EXPORT_METHOD);
					        	// If they can generate QR codes but can't write to external
					        	// storage, go ahead and launch site list activity in QR
					        	// export mode:
					        	} else if (canGenQRs && !canWriteToSD) {
						        	Intent i5a = new Intent(getBaseContext(),
						        			SiteListActivity.class);
						        	i5a.putExtra("mode", SiteListActivity.MODE_EXPORT_QR);
						        	startActivity(i5a);
					        	// If they can't generate QR codes but can write to external
						        // storage, go ahead and launch the classic export activity:
					        	} else if (!canGenQRs && canWriteToSD) {
						        	Intent i5b = new Intent(getBaseContext(),
						        			ExportActivity.class);
						        	startActivity(i5b);
					        	}
							}
							break;
						// Import is the inverse of Export:  We bring in data previously
						// exported by another copy of Cryptnos.  We can either import
						// from an encrypted file or from a QR code.  Note that import
						// may not be available if the device's current state does not
						// support it.
						case MENUITEM_IMPORT:
							if (menuEnabled[MENUITEM_IMPORT]) {
					        	// If we can both scan QR codes and read from external
								// storage, let the user pick which import method they'd
								// like to use:
					        	if (canScanQRs && canReadFromSD) {
					        		showDialog(DIALOG_CHOOSE_IMPORT_METHOD);
					        	// If they can only scan QR codes, launch the QR code scanner:
					        	} else if (canScanQRs && !canReadFromSD) {
									Intent iqri = qrCodeHandler.generateScanIntent();
									startActivityForResult(iqri, QRCodeHandler.INTENT_SCAN_QRCODE);
					        	// If they can only read from external storage, send them to
								// the classic import activity:
					        	} else if (!canScanQRs && canReadFromSD) {
						        	Intent i6 = new Intent(getBaseContext(),
						        			ImportActivity.class);
						        	startActivity(i6);
					        	}
							}
							break;
						// Settings allows us to determine how Cryptnos works behind the
						// scenes.  This option is always available.
						case MENUITEM_SETTINGS:
				        	Intent i7 = new Intent(getBaseContext(),
				        			AdvancedSettingsActivity.class);
				        	startActivity(i7);
							break;
						// The Help system guides the user in how to use Cryptnos.  This
						// option is also always available.
						case MENUITEM_HELP:
				        	Intent i8 = new Intent(getBaseContext(), HelpMenuActivity.class);
				        	startActivity(i8);
							break;
						// If we got anything else, default to saying the option is not
						// implemented.  Who knows... maybe we added a feature and forgot
						// to implement it?
						default:
							Toast.makeText(getBaseContext(), R.string.error_not_implemented,
					        		Toast.LENGTH_LONG).show();
						break;
					}
				}
            });
    	} catch (Exception e) {
    		Toast.makeText(this, getResources().getString(R.string.error_unknown),
    				Toast.LENGTH_LONG).show();
    		finish();
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

	/**
	 * This private sub-class of BaseAdapter is used in building the Cryptnos icon-based
	 * main menu.
	 * @author Jeffrey T. Darlington
	 * @version 1.3.0
	 * @since 1.3.0
	 */
    private class MainMenuShortcutAdapter extends BaseAdapter {
    	
    	/** The calling context */
    	private Context context = null;
    	/** The resources of the calling context */
    	private Resources res = null;
    	
    	/**
    	 * Constructor
    	 * @param context The calling context
    	 */
    	MainMenuShortcutAdapter(Context context) {
    		this.context = context;
    		res = context.getResources();
    	}

		public int getCount() {
			return 8;
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int menuItem, View convertView, ViewGroup parent) {
			// For convenience, all our "icons" will actually be TextViews, as we can
			// combine text and icons together.  Start by declaring the temporary view
			// we will use along the way.
			TextView tv = null;
			// Does the view already exist?  If not, create the object and give it a
			// few defining values to start with.  Otherwise, cast the existing object
			// to a TextView and move on.
            if (convertView == null) {
            	int padding = res.getInteger(R.integer.main_menu_grid_padding);
                tv = new TextView(context);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(padding, padding, padding, padding);
            } else {
                tv = (TextView) convertView;
            }
            // Declare an icon drawable and a string title, then switch across the
            // menu item position to determine which item we're currently building:
            Drawable icon = null;
            String title = null;
            switch (menuItem) {
	            case MENUITEM_NEW:
	            	icon = res.getDrawable(R.drawable.key_add);
	            	title = res.getString(R.string.mainmenugrid_new);
	            	break;
	            case MENUITEM_REGEN:
	            	icon = res.getDrawable(R.drawable.key_regenerate);
	            	title = res.getString(R.string.mainmenugrid_regenerate);
	            	break;
	            case MENUITEM_EDIT:
	            	icon = res.getDrawable(R.drawable.key_edit);
	            	title = res.getString(R.string.mainmenugrid_edit);
	            	break;
	            case MENUITEM_DELETE:
	            	icon = res.getDrawable(R.drawable.key_delete);
	            	title = res.getString(R.string.mainmenugrid_delete);
	            	break;
	            case MENUITEM_EXPORT:
	            	icon = res.getDrawable(R.drawable.key_export);
	            	title = res.getString(R.string.mainmenugrid_export);
	            	break;
	            case MENUITEM_IMPORT:
	            	icon = res.getDrawable(R.drawable.key_import);
	            	title = res.getString(R.string.mainmenugrid_import);
	            	break;
	            case MENUITEM_SETTINGS:
	            	icon = res.getDrawable(R.drawable.settings);
	            	title = res.getString(R.string.mainmenugrid_settings);
	            	break;
	            case MENUITEM_HELP:
	            	icon = res.getDrawable(R.drawable.help);
	            	title = res.getString(R.string.mainmenugrid_help);
	            	break;
            }
            // Look at the enabled array for the menu items (built by buildMenu() in our
            // parent) and see if the item is supposed to be enabled or not.  If it should
            // be disabled, set the alpha value of the icon to about 1/4 the full strength.
            // This will make it look dim against the dark background and thus disabled.
            // Otherwise, set the alpha at full strength.  (If we don't go that, the icon
            // might still look disabled even if we change state and it *should* be
            // enabled.
            if (!menuEnabled[menuItem]) icon.setAlpha(64);
            else icon.setAlpha(255);
            // Now assign the icon above the title text and return the text view object:
            tv.setCompoundDrawablesWithIntrinsicBounds(
                    null, icon, null, null);
            tv.setText(title);
            tv.setTag(title);
            return tv;
		}
    	
    }

}