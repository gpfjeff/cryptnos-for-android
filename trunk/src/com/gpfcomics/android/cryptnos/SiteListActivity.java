/* SiteListActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          December 15, 2009
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This activity acts as a launching point for the Generate Existing, Edit,
 * and Delete activities.  It loads the list of site parameters from the
 * database and displays it as a ListActivity, letting the user select which
 * set of parameters they wish to operate on.  Which main menu option was
 * selected determines the default action for tapping an item in the list;
 * if Generate Existing was selected on the main menu, selecting a site here
 * will take you to the Generate Existing activity.  However, all three
 * activities can be selected from each item's context menu.
 * 
 * Because decrypting the values in the database takes a while, a progress
 * dialog is displayed whenever the list is being built.
 * 
 * UPDATES FOR 1.1.0:  Added the Delete All option menu and dialog to allow the
 * user to delete all the parameters in the database in one action.
 * 
 * UPDATES FOR 1.2.0:  Added Help option menu
 *
 * UPDATES FOR 1.2.1:  Minor code changes to support new UI enhancements
 * 
 * UPDATES FOR 1.2.2:  Commented out the onStop() method to prevent the user
 * from being forced back to the main menu for small configuration changes,
 * like rotating the screen or sliding out a physical keyboard.
 * 
 * UPDATES FOR 1.3.0:  Added code to handle new QR code export mode.  If the
 * user selects to export a site via QR code from the main menu, they are sent
 * here in the new mode.  Default selection sends the parameters to the user's
 * selection of QR code scanner app, which encodes and displays the code.  The
 * context menu has be updated as well to make exporting to QR code an option
 * in all list modes.
 * 
 * UPDATES FOR 1.3.1:  Added setTextFilterEnabled() to ListView to enable filtering
 * of the site list based on the user typing.
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * The SiteListActivity provides the basic interface for selecting one saved
 * site token in the Cryptnos database.  It is used as the gateway to the
 * EditParametersActivity and GenerateExistingActivity, as well as handles
 * the deletion of site parameters.  Which of these actions are the default
 * when a site is tapped is handled by the *_MODE constants, but a tap & hold
 * will also bring up a context menu that will allow any of the subsequent
 * actions to be selected.
 * @author Jeffrey T. Darlington
 * @version 1.3.1
 * @since 1.0
 */public class SiteListActivity extends ListActivity implements
 		SiteListListener {
	
	/** A constant indicating this activity has been called in edit mode. */
	public static final int MODE_EDIT = 0;
	/** A constant indicating this activity has been called in delete mode. */
	public static final int MODE_DELETE = MODE_EDIT + 1;
	/** A constant indicating this activity has been called in generate
	 *  existing mode. */
	public static final int MODE_EXISTING = MODE_DELETE + 1;
	/** A constant indicating this activity has been called in "export to QR code:
	 *  mode. */
	public static final int MODE_EXPORT_QR = MODE_EXISTING + 1;
	/** A constant indicating the Generate menu item. */
	public static final int MENU_GENERATE = Menu.FIRST;
	/** A constant indicating the Edit menu item. */
	public static final int MENU_EDIT = MENU_GENERATE + 1;
	/** A constant indicating the Delete menu item. */
	public static final int MENU_DELETE = MENU_EDIT + 1;
	/** A constant indicating the Export to QR Code menu item .*/
	public static final int MENU_EXPORT_QR = MENU_DELETE + 1;
	/** A constant indicating the Delete All option menu item. */
	public static final int OPTMENU_DELETE_ALL = Menu.FIRST + 100;
	/** A constant indicating the Help option menu item. */
	public static final int OPTMENU_HELP = MENU_DELETE + 1;
	/** A constant indicating that we should show the confirm delete dialog. */
	static final int DIALOG_CONFIRM_DELETE = 1000;
	/** A constant indicating that we should show the confirm delete all
	 *  dialog. */
	static final int DIALOG_CONFIRM_DELETE_ALL = DIALOG_CONFIRM_DELETE + 1;
	/** A constant indicating the request code to look for when returning from
	 *  a third-party QR code generating app that needs us to display its code. */
	static final int REQUEST_GEN_QRCODE = 101010;

	/** A reference to our top-level application */
	private CryptnosApplication theApp = null;
	/** Our database adapter for interfacing with the database. */
	private ParamsDbAdapter DBHelper = null;
	/** The mode this activity was called in. */
	private int mode = 0;
	/** The site token of the last selected site.  This had to be pulled out
	 *  into a member variable so it could be seen by dialogs and such. */
	private String selectedSite = null;
	
	/** A handy reference to the ProgressDialog used when loading encrypted
	 *  data from the database. */
	ProgressDialog progressDialog = null;
	/** A SimpleAdapter used to build the site list.  This has been pulled
	 *  out into a member variable so it can be referenced from multiple
	 *  locations. */
	private SimpleAdapter menuAdapter = null;
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // Get a reference to the top-level application, as well as the
        // DB helper.  (While the app does the work of building the site list
        // now, we'll need the DB helper to delete items.)
        theApp = (CryptnosApplication)getApplication();
        DBHelper = theApp.getDBHelper();
        // Register the list for context menu events:
        ListView lv = getListView();
        registerForContextMenu(lv);
        // Enable filtering of the list by letting the user type text while the
        // list is displayed.  The request for focus helps make sure this gets
        // enabled as soon as the view comes into play.
        lv.setTextFilterEnabled(true);
        lv.requestFocus();
        // Try to get the mode from the intent.  Note that if anything
    	// goes wrong, we go into edit mode rather than delete mode, so
    	// default actions won't destroy data.
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
        	mode = extras.getInt("mode");
        	if (mode < MODE_EDIT || mode > MODE_EXPORT_QR)
        		mode = MODE_EDIT;
        }
        // Request the site list from the main app.  We'll actually get the
        // list in the onSiteListReady() method below.
        theApp.requestSiteList(this, this);
    }

    // Commented out the onStop() declaration.  Odds are we don't really need
    // it, and there's the side effect that small configuration changes like
    // rotating the screen or sliding out a physical keyboard call this too,
    // forcing the user back to the main menu whenever these changes occur.
    // We can always add this back in if we find it to be more secure, but
    // for now the caveats seem to outweigh the benefits.
    //
    //@Override
    //protected void onStop()
    //{
    	// This may seem a bit weird, but whenever we stop this activity
    	// (i.e., switch to another one, either by moving to the next one in
    	// the task flow or canceling out and moving to a different task), we
    	// don't want to come back to this step.  Rather, we want to take the
    	// user back to the main menu.  So whenever this task gets stopped,
    	// go ahead and finish it.
    	//super.onStop();
    	//finish();
    //}
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
    	// We need a dialog reference to pass out, as well as references
    	// to ourself to pass in for contexts on Toasts and dialog control.
    	// (These references also have to be final to be seen by all the inner
    	// classes.)
    	Dialog dialog = null;
    	final Context context = this;
    	final ListActivity theActivity = this;
    	// Determine which dialog to show:
    	switch (id)
    	{
    		// The site list building progress dialog is actually handled by
    		// the application, but we need to attach it here to actually get
    		// it to display.  If we get this value, pass the buck:
    		case CryptnosApplication.DIALOG_PROGRESS:
    			dialog = theApp.onCreateDialog(id);
    			break;
    		// The confirm delete dialog asks the user to confirm the deletion
    		// of the selected site parameters.  If the user selects No or
    		// otherwise cancels the dialog (hitting Back for example), nothing
    		// happens.  Otherwise, the record should be deleted.
    		case DIALOG_CONFIRM_DELETE:
    			AlertDialog.Builder adb = new AlertDialog.Builder(this);
    			adb.setTitle(R.string.sitelist_dialog_confirmdetele_title);
    			// For the message, we want to show the name of the site token
    			// we're going to delete, just in case they fat-fingered their
    			// selection.  This isn't as easy as it sounds.  Grab the
    			// message text from the resources, but replace a hard-coded
    			// token (currently getResources().getString(R.string.meta_replace_token) because it's alphanumeric and unlikely
    			// to be a real world) with the token to delete.  Then set that
    			// as the message.  This has a caveat of making us explicitly
    			// destroy this dialog later; see the comments below.
    			String message = getResources().getString(R.string.sitelist_dialog_confirmdetele_msg);
    			message = message.replaceAll(getResources().getString(R.string.meta_replace_token), selectedSite);
    			adb.setMessage(message);
    			adb.setCancelable(true);
    			// What to do when they confirm the delete
    			adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   // Delete the record if valid, otherwise show an error
 		        	   // Toast:
		                if (selectedSite != null)
		                {
		                	if (DBHelper.deleteRecord(SiteParameters.generateKeyFromSite(selectedSite, theApp)))
		                		Toast.makeText(context,
		                			context.getResources().getString(R.string.sitelist_dialog_confirmdetele_success).replaceAll(getResources().getString(R.string.meta_replace_token), selectedSite), Toast.LENGTH_LONG).show();
		                	else
		                		Toast.makeText(context,
		                			context.getResources().getString(R.string.sitelist_dialog_confirmdetele_fail).replaceAll(getResources().getString(R.string.meta_replace_token), selectedSite), Toast.LENGTH_LONG).show();
		                }
		                // Originally, we tried using dismissDialog() here, but
		                // that didn't work as expected.  For efficiency,
		                // Android holds onto dialogs once they've been created,
		                // reusing them whenever possible.  That's all well and
		                // good, but since w're customizing our message to fit
		                // the site being deleted, we can't reuse our dialog
		                // without confusing the user.  After all, if you
		                // delete site "foo" and then decided to delete site
		                // "bar" as well, the dialog shouldn't say "foo" anymore.
		                // So instead we'll explicitly remove the dialog, forcing
		                // Android to rebuild it the next time it's needed, making
		                // sure the text matches what the user tapped.
						theActivity.removeDialog(DIALOG_CONFIRM_DELETE);
						// Now we need to refresh the list.  I wish there was a
						// better way to do this, but we'll have to rebuild the
						// list completely and re-associate it with the UI.  To
						// do this, ask the app to mark the list as "dirty" and
						// in need of refreshing, then request the list again.
						theApp.setSiteListDirty();
						theApp.requestSiteList(theActivity,
								(SiteListListener)theActivity);
		           }
    			});
    			// What to do when the No button is clicked:
    			adb.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
    		           public void onClick(DialogInterface dialog, int id) {
    		        	   // For now, just cancel the dialog.  We'll follow
    		        	   // up on that below.
    		        	   dialog.cancel();
    		           }
    		       });
    			// What to do if the dialog is canceled:
    			adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						// See the comment above.  Simply canceling the dialog
						// makes it be reused, causing the message text not to
						// get refreshed.  We have to actually tell the activity
						// to remove the dialog and force it to be rebuilt the
						// next time it is needed.
						theActivity.removeDialog(DIALOG_CONFIRM_DELETE);
					}
				});
    			dialog = (Dialog)adb.create();
    			break;
    		// The Confirm Delete All dialog is created when we've been asked
    		// to delete all site parameters from the database.  This is a lot
    		// like the other confirm delete dialog, only this time we're
    		// operating all every record in the database, not the one
    		// selected by the user
    		case DIALOG_CONFIRM_DELETE_ALL:
    			AlertDialog.Builder adb2 = new AlertDialog.Builder(this);
    			adb2.setTitle(R.string.sitelist_dialog_confirmdetele_title);
    			adb2.setMessage(R.string.sitelist_dialog_confirmdeteleall_msg);
    			adb2.setCancelable(true);
    			adb2.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
  		           public void onClick(DialogInterface dialog, int id) {
  		        	   // Delete all the records, otherwise show an error
  		        	   // Toast:
  		        	   int countDeleted = DBHelper.deleteAllRecords();
  		        	   if (countDeleted > 0)
  		        		   Toast.makeText(context,
  		        			   context.getResources().getString(R.string.sitelist_dialog_confirmdeteleall_success).replaceAll(getResources().getString(R.string.meta_replace_token), String.valueOf(countDeleted)), Toast.LENGTH_LONG).show();
  		        	   else
  		        		   Toast.makeText(context,
	        				   context.getResources().getString(R.string.sitelist_dialog_confirmdeteleall_fail), Toast.LENGTH_LONG).show();
  		        	   // Unlike the single delete, this time we can actually
  		        	   // use dismissDialog(), since this dialog doesn't have
  		        	   // to be rebuilt every time.
  		        	   theActivity.dismissDialog(DIALOG_CONFIRM_DELETE_ALL);
  		        	   // Set the app's site list reference to dirty, so it
  		        	   // will be forced to refresh:
  		        	   theApp.setSiteListDirty();
  		        	   // At this point, there's no reason keeping this
  		        	   // activity open.  There will be nothing in here to
  		        	   // work with.  So close the activity, which should
  		        	   // return us to the main menu.
  		        	   theActivity.finish();
 		           }
     			});
     			// What to do when the No button is clicked:
     			adb2.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
     		           public void onClick(DialogInterface dialog, int id) {
     		        	   // For now, just cancel the dialog.  We'll follow
     		        	   // up on that below.
     		        	   dialog.cancel();
     		           }
     		       });
     			// What to do if the dialog is canceled:
     			adb2.setOnCancelListener(new DialogInterface.OnCancelListener() {
 					public void onCancel(DialogInterface dialog) {
 						theActivity.dismissDialog(DIALOG_CONFIRM_DELETE_ALL);
 					}
 				});
     			dialog = (Dialog)adb2.create();
    			break;
    	}
    	return dialog;
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
        selectedSite = (String)menuItemHash.get("site");
        // Now perform the default "click" task depending on which mode we've
        // be called in:
        switch (mode)
        {
        	// In edit mode, bring up the edit parameters activity, populating
        	// it with information from the database:
	        case MODE_EDIT:
	            Intent i1 = new Intent(this, EditParametersActivity.class);
	            i1.putExtra("mode", EditParametersActivity.MODE_EDIT);
	            i1.putExtra(ParamsDbAdapter.DBFIELD_SITE, 
	            	SiteParameters.generateKeyFromSite(selectedSite, theApp));
	            startActivity(i1);
	        	break;
	        // In generate existing mode: all we're doing is giving the user
	        // a chance to pick a site token to generate a password for:
	        case MODE_EXISTING:
	            Intent i2 = new Intent(this, GenerateExistingActivity.class);
	            i2.putExtra(ParamsDbAdapter.DBFIELD_SITE, 
	            	SiteParameters.generateKeyFromSite(selectedSite, theApp));
	            startActivity(i2);
	        	break;
	        // In delete mode, display the confirmation dialog, which will
	        // take care of actually deleting the record if they confirm the
	        // action:
	        case MODE_DELETE:
	        	showDialog(DIALOG_CONFIRM_DELETE);
	        	break;
	        // If our default is to export a QR code, get the site parameter data
	        // and do whatever is necessary to display the code:
	        case MODE_EXPORT_QR:
	        	if (theApp.getQRCodeHandler().canGenerateQRCodes())
	        		exportSiteToQRCode();
	        	else Toast.makeText(this, R.string.error_qrexport_no_app_found,
		        		Toast.LENGTH_LONG).show();
	        	break;
	        // Nothing else should be implemented yet:
	        default:
	        	Toast.makeText(this, R.string.error_not_implemented,
		        		Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// The context menu allows us to get around whatever we selected on
		// the main menu.  Here, we can do all four main tasks of generating
		// an existing password, editing a set of parameters, deleting a
		// parameter set, or exporting it to a QR code. I originally planned
		// to change the default order depending on the calling mode, making
		// the option selected from the main menu be at the top, but then I
		// thought it might be confusing for the context menu to change all
		// the time.  Thus, the order is hard- coded to what you see here.
		menu.setHeaderTitle(R.string.sitelist_contextmenu_header);
        menu.add(0, MENU_GENERATE, 0, R.string.menu_generate);
        menu.add(0, MENU_EDIT, 1, R.string.menu_edit);
        menu.add(0, MENU_DELETE, 2, R.string.menu_delete);
        if (theApp.getQRCodeHandler().canGenerateQRCodes())
        	menu.add(0, MENU_EXPORT_QR, 3, R.string.menu_export_qrcode);
	}
    
    @Override
	public boolean onContextItemSelected(MenuItem item) {
    	// Get the selected site token from the menu item selected.  Get the
    	// menu info from the menu item, then get the target View from there.
    	// From the text view, extract the text value.
		AdapterContextMenuInfo info =
			(AdapterContextMenuInfo)item.getMenuInfo();
		TextView tv = (TextView)info.targetView;
		selectedSite = tv.getText().toString();
		// Now determine what to do depending on which item was selected:
		switch(item.getItemId()) {
			// Generate the password for the selected set of parameters:
	    	case MENU_GENERATE:
	    		Intent i1 = new Intent(this, GenerateExistingActivity.class);
	            i1.putExtra(ParamsDbAdapter.DBFIELD_SITE, 
		            	SiteParameters.generateKeyFromSite(selectedSite, theApp));
	    		startActivity(i1);
	        	return true;
	        // Edit the selected parameters:
    		case MENU_EDIT:
	            Intent i2 = new Intent(this, EditParametersActivity.class);
	            i2.putExtra("mode", EditParametersActivity.MODE_EDIT);
	            i2.putExtra(ParamsDbAdapter.DBFIELD_SITE, 
	            	SiteParameters.generateKeyFromSite(selectedSite, theApp));
	            startActivity(i2);
    			return true;
    		// Delete the selected parameters:
    		case MENU_DELETE:
    			showDialog(DIALOG_CONFIRM_DELETE);
    			return true;
    		// Export to a QR code:
    		case MENU_EXPORT_QR:
	        	if (theApp.getQRCodeHandler().canGenerateQRCodes())
	        		exportSiteToQRCode();
	        	else Toast.makeText(this, R.string.error_qrexport_no_app_found,
		        		Toast.LENGTH_LONG).show();
	        	return true;
		}
		// If the above didn't return anything useful, pass the buck:
		return super.onContextItemSelected(item);
	}

    public boolean onCreateOptionsMenu(Menu menu) {
    	// Add the "Delete All" and "Help" menu items.  For now, that's all
    	// we really have or need, as everything else requires us to select
    	// a specific item in the list.
    	menu.add(0, OPTMENU_DELETE_ALL, Menu.NONE,
    		R.string.sitelist_optmenu_delete_all).setIcon(android.R.drawable.ic_menu_delete);
    	menu.add(0, OPTMENU_HELP, Menu.NONE,
        	R.string.optmenu_help).setIcon(android.R.drawable.ic_menu_help);
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
	    	// If the Delete All item is selected, show the
	    	// confirmation dialog, which does all the actual work.
	    	case OPTMENU_DELETE_ALL:
	    		showDialog(DIALOG_CONFIRM_DELETE_ALL);
	    		return true;
	    	// If the Help item is selected, check which mode we're in.  If we're in
	    	// QR code export mode, send the user to the import/export help item.
	    	// Otherwise, send the user to the "working with existing parameters"
	    	// help item.
	    	case OPTMENU_HELP:
	        	Intent i = new Intent(this, HelpActivity.class);
	    		if (mode == MODE_EXPORT_QR)
		        	i.putExtra("helptext", R.string.help_text_importexport);
	    		else
	    			i.putExtra("helptext", R.string.help_text_existing);
	        	startActivity(i);
	    		return true;
    	}
    	return false;
    }

	public void onSiteListReady(String[] siteList) {
		try {
			// If we got a useful site list:
			if (siteList != null) {
		        // Start actually building the menu list.  It's a bit weird,
				// but it works.  Create an ArrayList of HashMap objects,
		        // which we can map a label to a value with.
		        ArrayList<HashMap<String,String>> menuItems =
		        	new ArrayList<HashMap<String,String>>();
		        HashMap<String,String> item = null;
		        // Step through our sorted site array and add each item to the
		        // new list:
		        for (int i = 0; i < siteList.length; i++)
		        {
		        	item = new HashMap<String,String>();
		        	item.put("site", siteList[i]);
		        	menuItems.add(item);
		        }
		        // Now use a SimpleAdapter to map the new list so it will get
		        // displayed to the user:
		        menuAdapter = new SimpleAdapter(
		    			this,
		    			menuItems,
		    			R.layout.sitelist_row,
		    			new String[] { "site" },
		    			new int[] { R.id.text1, }
		    		);
		        // And apply that adapter to the list:
		        if (menuAdapter != null) setListAdapter(menuAdapter);
		        // For good measure, request focus for the ListView again, just
		        // to make sure filtering of the list continues to work:
		        getListView().requestFocus();
		    // If the site list was null, something bad must have happened:
			} else Toast.makeText(this, R.string.error_bad_listfetch,
            		Toast.LENGTH_LONG).show();
		}
		// If anything blew up, complain:
		catch (Exception e) {
        	Toast.makeText(this, R.string.error_bad_listfetch,
            		Toast.LENGTH_LONG).show();
		}
	}
	
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// Let the super do its part:
    	super.onActivityResult(requestCode, resultCode, data);
    	// Which request are we responding to?
		switch (requestCode) {
			// If we requested that a QR code be generated:
			case REQUEST_GEN_QRCODE:
		    	// We'll need the QR code handler for this:
				QRCodeHandler qrCodeHandler = theApp.getQRCodeHandler();
		    	// Check to see if the code was generated successfully:
				if (qrCodeHandler.wasGenerateSuccessful(resultCode, data)) {
					// Try to get the path to the generated file:
					String pathToFile = qrCodeHandler.getPathToGeneratedQRCodeFile(data);
					// If we got a path, look to see if it actually points to a file:
					if (pathToFile != null) {
						File theFile = new File(pathToFile);
						if (theFile.exists() && theFile.canRead()) {
							// Generate the intent for the QRViewActivity to display
							// the generated code:
							Intent i = new Intent(this, QRViewActivity.class);
				        	i.putExtra("qrcode_file", pathToFile);
				        	startActivity(i);
				        } else Toast.makeText(this, "ERROR: File could not be read!",
			            		Toast.LENGTH_LONG).show();
					} else Toast.makeText(this, "ERROR: Path to file was null!",
		            		Toast.LENGTH_LONG).show();
				} else Toast.makeText(this, "ERROR: QR code failed to generate!",
	            		Toast.LENGTH_LONG).show();
		}
    }

	/**
	 * Assuming that a site has already been selected, either by being in QR export
	 * mode and the site has been tapped, or the user has long-pressed an entry in
	 * any mode and selected "Export to QR code", get the site parameters for the
	 * selected site, generate its export QR code, and display it.
	 */
	private void exportSiteToQRCode() {
		// Asbestos underpants:
		try {
			// Make sure a site has been selected first:
			if (selectedSite != null) {
				// Get the record from the database:
				Cursor c =
					DBHelper.fetchRecord(SiteParameters.generateKeyFromSite(selectedSite,
							theApp));
				// If we got something useful:
				if (c != null & c.getCount() == 1) {
					// Generate a new site parameters object:
					SiteParameters params =
    					new SiteParameters(theApp, c.getString(1), c.getString(2));
					// Close the cursor for good measure:
					c.close();
					// Get the app's QR code handler and make sure we can work with
					// QR codes:
					QRCodeHandler qrCodeHandler = theApp.getQRCodeHandler();
					if (qrCodeHandler.canGenerateQRCodes()) {
						// Generate our intent to encode data:
						Intent i = qrCodeHandler.generateEncodeIntent(params);
						// Some QR encoders will display the generated code for us
						// while some will not.  If they won't, we'll need to get
						// the raw image data from the encoder and display it ourselves.
						if (qrCodeHandler.needQRViewActivity())
							startActivityForResult(i, REQUEST_GEN_QRCODE);
				        // If the encoder will display the code for us, just send
				        // the intent to generate the code:
						else startActivity(i);
					// If we can't export QR codes, complain:
					} else Toast.makeText(this, R.string.error_qrexport_no_app_found,
							Toast.LENGTH_LONG).show();
				// If we didn't get anything useful from the database:
				} else {
					if (c != null) c.close();
					Toast.makeText(this, R.string.error_bad_restore,
		            		Toast.LENGTH_LONG).show();
				}
			}
			// If no site was selected:
			else Toast.makeText(this, R.string.error_export_no_sites_checked,
            		Toast.LENGTH_LONG).show();
		// If something blew up:
		} catch (Exception e) {
        	Toast.makeText(this, R.string.error_qrexport_fail,
            		Toast.LENGTH_LONG).show();
		}
	}
}
