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
 * UPDATES FOR 1.1.1:  Added Help option menu
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

import java.util.*;

import android.app.Dialog;
import android.app.ListActivity;
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
 * @version 1.0
 * @since 1.0
 */
public class CryptnosMainMenu extends ListActivity {
	
	/** A reference to our top-level application */
	private CryptnosApplication theApp = null;
	/** Our database adapter */
	private ParamsDbAdapter mDBHelper;

	/** A constant indicating the Help option menu item. */
	public static final int OPTMENU_HELP = Menu.FIRST;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try
        {
            // Get a reference to the top-level application, as well as the
            // DB helper:
            theApp = (CryptnosApplication)getApplication();
            mDBHelper = theApp.getDBHelper();
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
    	// Run the UpgradeManager if it hasn't already been run this session:
    	if (!theApp.hasUpgradeManagerRun()) theApp.runUpgradeManager(this);
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
        Resources res = this.getResources();
        // Launch the about activity:
        if (menuItem.compareTo(res.getString(R.string.mainmenu_about1)) == 0)
        {
        	Intent i = new Intent(this, AboutActivity.class);
        	startActivity(i);
        }
        // Launch the help/tutorial activity
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_help1)) == 0)
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
        	i.putExtra("mode", SiteListActivity.EDIT_MODE);
        	startActivity(i);
        }
        // Launch the edit activity menu (i.e. display the site listing so
        // the user can pick some parameters to edit):
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_existing1)) == 0)
        {
        	Intent i = new Intent(this, SiteListActivity.class);
        	i.putExtra("mode", SiteListActivity.EXISTING_MODE);
        	startActivity(i);
        }
        // Launch the delete activity menu (i.e. display the site listing so
        // the user can pick some parameters to delete):
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_delete1)) == 0)
        {
        	Intent i = new Intent(this, SiteListActivity.class);
        	i.putExtra("mode", SiteListActivity.DELETE_MODE);
        	startActivity(i);
        }
        // Launch the export activity:
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_export1)) == 0)
        {
        	Intent i = new Intent(this, ExportActivity.class);
        	startActivity(i);
        }
        // Launch the import activity:
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_import1)) == 0)
        {
        	Intent i = new Intent(this, ImportActivity.class);
        	startActivity(i);
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
    	switch (id)
    	{
			case CryptnosApplication.DIALOG_UPGRADE_TO_UTF8:
				dialog = theApp.onCreateDialog(id);
				break;
    	}
    	return dialog;
    }
    
    /**
     * Build the main application menu.  This menu is somewhat dynamic, in
     * that certain items will only appear if there are existing parameters
     * saved in the database.
     */
    private void buildMenu()
    {
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
        	// to export and we can write to the SD card:
        	if (theApp.canWriteToExternalStorage()) {
	        	item = new HashMap<String,String>();
	        	item.put("line1", res.getString(R.string.mainmenu_export1));
	        	item.put("line2", res.getString(R.string.mainmenu_export2));
	        	menuItems.add(item);
        	}
        }
        // Importing a set of parameters only makes sense if we can read
        // from the SD card:
        if (theApp.canReadFromExternalStorage()) {
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
        // Finally, show a simple about box:
    	item = new HashMap<String,String>();
    	item.put("line1", res.getString(R.string.mainmenu_about1));
    	item.put("line2", res.getString(R.string.mainmenu_about2));
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
    }

}