/* HelpMenuActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          December 10, 2009
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This activity displays the Cryptnos help menu.  This is very similar to
 * the main menu (in fact, it uses the same layout), only every menu item
 * opens the HelpActivity class, passing in a reference to the help text to
 * display.
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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * This activity displays the Cryptnos help menu.  This is very similar to
 * the main menu (in fact, it uses the same layout), only every menu item
 * opens the HelpActivity class, passing in a reference to the help text to
 * display.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class HelpMenuActivity extends ListActivity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    	buildMenu();
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
        // Launch the "What is Cryptnos?" help activity:
        if (menuItem.compareTo(res.getString(R.string.help_menu_whatis1)) == 0)
        {
        	Intent i = new Intent(this, HelpActivity.class);
        	i.putExtra("helptext", R.string.help_text_whatis);
        	startActivity(i);
        }
        // Launch the "Getting Started" help:
        else if (menuItem.compareTo(res.getString(R.string.help_menu_start1)) == 0)
        {
        	Intent i = new Intent(this, HelpActivity.class);
        	i.putExtra("helptext", R.string.help_text_start);
        	startActivity(i);
        }
        // Launch the "Working with Existing Parameters" help:
        else if (menuItem.compareTo(res.getString(R.string.help_menu_existing1)) == 0)
        {
        	Intent i = new Intent(this, HelpActivity.class);
        	i.putExtra("helptext", R.string.help_text_existing);
        	startActivity(i);
        }
        // Launch the "Import/Export" help:
        else if (menuItem.compareTo(res.getString(R.string.help_menu_importexport1)) == 0)
        {
        	Intent i = new Intent(this, HelpActivity.class);
        	i.putExtra("helptext", R.string.help_text_importexport);
        	startActivity(i);
        }
        // Launch the "Advanced Settings" help:
        else if (menuItem.compareTo(res.getString(R.string.help_menu_settings1)) == 0)
        {
        	Intent i = new Intent(this, HelpActivity.class);
        	i.putExtra("helptext", R.string.help_text_settings);
        	startActivity(i);
        }
        // Launch the "Disclaimers" help:
        else if (menuItem.compareTo(res.getString(R.string.help_menu_disclaimers1)) == 0)
        {
        	Intent i = new Intent(this, HelpActivity.class);
        	i.putExtra("helptext", R.string.help_text_disclaimers);
        	startActivity(i);
        }
        // Launch the "Change Log" help:
        else if (menuItem.compareTo(res.getString(R.string.help_menu_changelog1)) == 0)
        {
        	Intent i = new Intent(this, HelpActivity.class);
        	i.putExtra("helptext", R.string.help_text_changelog);
        	startActivity(i);
        }
        // Launch the "License" help:
        else if (menuItem.compareTo(res.getString(R.string.help_menu_license1)) == 0)
        {
        	Intent i = new Intent(this, HelpActivity.class);
        	i.putExtra("helptext", R.string.help_text_license);
        	startActivity(i);
        }
        // Launch the about activity:
        else if (menuItem.compareTo(res.getString(R.string.mainmenu_about1)) == 0)
        {
        	Intent i = new Intent(this, AboutActivity.class);
        	startActivity(i);
        }
        // For the moment, nothing is working.  Show a quick Toast to let
        // the user know that's our fault and not theirs.
        else Toast.makeText(this, R.string.error_not_implemented,
        		Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Build the main application menu.  This menu is somewhat dynamic, in
     * that certain items will only appear if there are existing parameters
     * saved in the database.
     */
    private void buildMenu()
    {
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
        Resources res = this.getResources();
        // Add the "About" item:
    	item = new HashMap<String,String>();
    	item.put("line1", res.getString(R.string.mainmenu_about1));
    	item.put("line2", res.getString(R.string.mainmenu_about2));
    	menuItems.add(item);
        // Add the "What is Cryptnos?" item:
    	item = new HashMap<String,String>();
    	item.put("line1", res.getString(R.string.help_menu_whatis1));
    	item.put("line2", res.getString(R.string.help_menu_whatis2));
    	menuItems.add(item);
        // Add the "Getting Started" item:
    	item = new HashMap<String,String>();
    	item.put("line1", res.getString(R.string.help_menu_start1));
    	item.put("line2", res.getString(R.string.help_menu_start2));
    	menuItems.add(item);
        // Add the "Working with Existing Parameters" item:
    	item = new HashMap<String,String>();
    	item.put("line1", res.getString(R.string.help_menu_existing1));
    	item.put("line2", res.getString(R.string.help_menu_existing2));
    	menuItems.add(item);
        // Add the "Import/Export" item:
    	item = new HashMap<String,String>();
    	item.put("line1", res.getString(R.string.help_menu_importexport1));
    	item.put("line2", res.getString(R.string.help_menu_importexport2));
    	menuItems.add(item);
        // Add the "Advanced Settings" item:
    	item = new HashMap<String,String>();
    	item.put("line1", res.getString(R.string.help_menu_settings1));
    	item.put("line2", res.getString(R.string.help_menu_settings2));
    	menuItems.add(item);
        // Add the "Disclaimers" item:
    	item = new HashMap<String,String>();
    	item.put("line1", res.getString(R.string.help_menu_disclaimers1));
    	item.put("line2", res.getString(R.string.help_menu_disclaimers2));
    	menuItems.add(item);
        // Add the "Change Log" item:
    	item = new HashMap<String,String>();
    	item.put("line1", res.getString(R.string.help_menu_changelog1));
    	item.put("line2", res.getString(R.string.help_menu_changelog2));
    	menuItems.add(item);
        // Add the "License" item:
    	item = new HashMap<String,String>();
    	item.put("line1", res.getString(R.string.help_menu_license1));
    	item.put("line2", res.getString(R.string.help_menu_license2));
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
