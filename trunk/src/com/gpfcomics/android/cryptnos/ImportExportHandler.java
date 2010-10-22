/* ImportExportHandler.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          March 26, 2010
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This class provides a self-contained interface for importing and exporting
 * Cryptnos parameter data to and from an encrypted file on the device SD
 * card.  It does not directly interface with the user; rather, it is called
 * by the UI activities, which then pass it the necessary parameters to do its
 * work.  Note that it updates UI elements and requires references back to the
 * calling activity and a ProgressDialog it controls.
 * 
 * UPDATES FOR 1.1:  Moved the old Importer internal class to OldFormatImporter,
 * then added the XMLFormat1Importer and XMLHandler classes.  The old format
 * importer will allow us to import files exported by Cryptnos 1.0, while the
 * XML importer will import files created in the new XML-based cross-platform
 * format.  In addition, the Exporter class has been modified to export to the
 * new XML-format.  Note that exporting to the old format is no longer an
 * option; we'll only export to the new format.  The new XML format should be
 * readable by any version of Cryptnos on any platform.
 * 
 * UPDATES FOR 1.1.1:  Attempting to fix some text encoding issues by forcing
 * all text-to-binary and binary-to-text operations to use UTF-8.  I know this
 * is what the Windows client should be using, but Android Strings seem to
 * uncertain.  Note that the one place we *CAN'T* do this is the old format
 * stuff, which requires us to use whatever default the system uses.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * This class provides a self-contained interface for importing and exporting
 * Cryptnos parameter data to and from an encrypted file on the device SD
 * card.  It does not directly interface with the user; rather, it is called
 * by the UI activities, which then pass it the necessary parameters to do its
 * work.  Note that it updates UI elements and requires references back to the
 * calling activity and a ProgressDialog it controls.
 * @author Jeffrey T. Darlington
 * @version 1.1
 * @since 1.0
 */
public class ImportExportHandler {

	// Private Constants **************************************************
	
	/** The number of iterations used for salt generation.  For the encryption
	 *  used in this class, we'll derive our salt from the user's password;
	 *  not ideal, of course, but definitely portable.  This constant will set
	 *  the number of times we'll hash the user's password with the selected
	 *  hash algorithm to generate our salt. */
	private static final int SALT_ITERATION_COUNT = 10;

	/** The number of iterations used for key generation. */
	private static final int KEY_ITERATION_COUNT = 100;

	/** The size of the AES encryption key in bits */
	private static final int KEY_SIZE = 256;

	/** The size of the AES encryption intialization vector (IV) in bits */
	private static final int IV_SIZE = 128;
	
	// Private Variables **************************************************

	/** A reference to our top-level application */
	private CryptnosApplication theApp = null;
	/** The calling activity, so we can refer back to it. */
	private Activity caller = null;
	/** The parameter DB adapter from the caller. */
	private ParamsDbAdapter DBHelper = null;
	/** The caller's ProgressDialog, which we'll help control.*/
	private ProgressDialog progressDialog = null;
	/** The caller's ProgressDialog ID number, so we can close the dialog
	 *  when we're done. */
	private int progressDialogID = 0;
	/** The private Exporter class that does the grunt work of exporting data. */
	private Exporter exporter = null;
	/** The private OldFormatImporter class that does the grunt work of
	 *  importing data from the old Android format. */
	private OldFormatImporter oldFormatImporter = null;
	/** The private XMLFormat1Importer class that does the grunt work of
	 *  importing data from the new XML-based, cross-platform format. */
	private XMLFormat1Importer xmlFormatImporter = null;
	private String importFilename = null;
	private String importPassword = null;
	
	/**
	 * The ImportExportHandler in intended to be a self-contained class for
	 * writing Cryptnos import/export files.  Use the exportToFile() and
	 * importFromFile() methods to perform these tasks.
	 * @param caller The calling activity, used as a back-reference to
	 * communicate back to the user
	 * @param progressDialog A ProgressDialog, owned by the caller Activity,
	 * that will be updated as the import/export process is performed
	 * @param progressDialogID The ID of the ProgressDialog, internal to the
	 * caller Activity. This is pulled out as another parameter because I
	 * can't find a better way to get at it.
	 */
	public ImportExportHandler(Activity caller,
			ProgressDialog progressDialog,
			int progressDialogID)
	{
		this.caller = caller;
		this.progressDialog = progressDialog;
		this.progressDialogID = progressDialogID;
		theApp = (CryptnosApplication)caller.getApplication();
		DBHelper = theApp.getDBHelper();
	}
	
	/**
	 * Export the parameters of the specified site tokens to an encrypted
	 * file.  Note that starting with Cryptnos 1.1, this only export files
	 * in the new XML-based, cross-platform format, not the original 1.0
	 * platform-specific format.
	 * @param filename The full path of the export file.
	 * @param password The password used to encrypt the file.
	 * @param sites An array of Strings containing the site tokens to export.
	 */
	public void exportToFile(String filename, String password, String[] sites)
	{
		// Simple enough:  Make sure all the inputs appear to be valid, then
		// create the Exporter thread to do the grunt work.
		if (filename != null && password != null && sites != null &&
			sites.length > 0)
		{
			exporter = new Exporter(caller, handler, sites, password,
					filename, theApp);
			exporter.start();
		}
		// If any of the inputs were invalid, inform the user:
		else {
        	Toast.makeText(caller, R.string.error_bad_export_params,
					Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * Import site parameters from the specified file.  Note that if any
	 * site tokens in the file already exist in the database, the values in
	 * the database will be overwritten with the values from the file.  This
	 * method supports both the original platform-specific export format and
	 * the new XML-based, cross-platform format, and should transparently
	 * handle which export format the file was saved in.
	 * @param filename The full path to the import file.
	 * @param password The password used to decrypt the file:
	 */
	public void importFromFile(String filename, String password)
	{
		// As long as we've got inputs, we'll assume for now they've been
		// vetted by the caller and start the importer thread:
		if (filename != null && password != null)
		{
			// This is probably horribly inefficient, but we'll try and open
			// the file as an XML-based, cross-platform file first.  If that
			// doesn't work, then we'll fall back to the old format.
			// Unfortunately, this won't be pretty, as we'll have to let the
			// Handler below launch the old format attempt.
			importFilename = filename;
			importPassword = password;
			xmlFormatImporter =
				new XMLFormat1Importer(handler, password, filename);
			xmlFormatImporter.start();
		}
		// If any of the inputs were invalid, inform the user:
		else {
        	Toast.makeText(caller, R.string.error_bad_import_params,
            		Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * Create the cipher used to encrypt or decrypt site parameter data for
	 * the old platform-specific export format.
	 * @param password A "secret" String value, usually the derived site
	 * "key".  This is specified as an input parameter rather than using the
	 * member variable because this method will be needed for one of the
	 * constructors.
	 * @param mode The Cipher encrypt/decryption mode.  This should be either
	 * Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE.
	 * @return A Cipher suitable for the encryption/decryption task.
	 * @throws Exception Thrown if the mode is invalid or if any error occurs
	 * while creating the cipher.
	 */
	private static Cipher createOldFormatCipher(String password, int mode)
		throws Exception
	{
		// Asbestos underpants:
		try
		{
			// Generate our encryption salt.  This has to be something
			// semi-random but not tied to the device, so we can't use the
			// unique device ID like CryptnosApplication.PARAMETER_SALT.
			// We'll derive one from the password by hashing it multiple times.
			// Note that since this is the *OLD* format cipher, we can't use
			// the CryptnosApplication.getTextEncoding() here; this
			// *MUST* be the default character encoding for the platform for
			// it to be backward-compatible.
			byte[] salt = password.getBytes();
			MessageDigest hasher = MessageDigest.getInstance("SHA-512");
			for (int i = 0; i < CryptnosApplication.SALT_ITERATION_COUNT; i++)
				salt = hasher.digest(salt);
			
			// I had a devil of a time getting this to work, but I eventually
			// peeked at the Google "Secrets" application source code to get
			// to this setup.  The Password Based Key (PBE) spec lets us
			// specify a password to generate keys from.  We'll use the key
			// passed in (most likely a "site key" from the site parameters)
			// as that password, salting it with the device's unique ID to
			// give it some uniqueness from device to device.
			PBEKeySpec pbeKeySpec =	new PBEKeySpec(password.toCharArray(),
				salt, CryptnosApplication.KEY_ITERATION_COUNT,
				CryptnosApplication.KEY_LENGTH);
			// Next we'll need a key factory to actually build the key:
			SecretKeyFactory keyFac =
				SecretKeyFactory.getInstance(CryptnosApplication.KEY_FACTORY);
			// The key is generated from the key factory:
			SecretKey key = keyFac.generateSecret(pbeKeySpec);
			// The cipher needs some parameter specs to know how to use
			// the key:
			AlgorithmParameterSpec aps = new PBEParameterSpec(salt,
					CryptnosApplication.KEY_ITERATION_COUNT);
			// Now that we have all of this information, actually start
			// creating the cipher:
			Cipher cipher = Cipher.getInstance(CryptnosApplication.KEY_FACTORY);
			// For our purposes, we're combining the creation of encryption
			// and decryption ciphers into one method.  So take the mode
			// passed in and initialize the cipher based on that mode.  Note
			// that the key and parameter specs are being pulled in at this
			// point, making the cipher complete.  If we get an invalid mode
			// type, throw an error.
			switch (mode)
			{
				case Cipher.ENCRYPT_MODE:
					cipher.init(Cipher.ENCRYPT_MODE, key, aps);
					break;
				case Cipher.DECRYPT_MODE:
					cipher.init(Cipher.DECRYPT_MODE, key, aps);
					break;
				default:
					throw new Exception("Invalid cipher mode");
			}
			// By now our cipher *should* be ready.  Go ahead an return it:
			return cipher;
		}
		// If anything blew up, throw it back out:
		catch (Exception e)
		{
			throw e;
		}
	}
	
	/** Given the user's password, generate a salt which will be mixed with
	 *  the password when setting up the encryption parameters
	 * @param password A string containing the user's password
	 * @return An array of bytes containing the raw salt value
	 * @throws Exception Thrown if the salt-generating hash is unavailable
	 */
	private static byte[] generateSaltFromPassword(String password,
			CryptnosApplication theApp)
		throws Exception
	{
		// Get the password as a series of bytes:
		byte[] salt = password.getBytes(theApp.getTextEncoding());
		// Try to hash password multiple times using a really strong hash.
		// This should give us some really random-ish data for the salt.
		MessageDigest hasher = MessageDigest.getInstance("SHA-512");
		for (int i = 0; i < SALT_ITERATION_COUNT; i++)
		{
			// Java notes:  This is a lot easier than in .NET.  We
			// don't have to initialize the hash engine each time it's
			// used.  Just pass in the old salt to get the new.
			salt = hasher.digest(salt);
		}
		return salt;
	}

	/**
	 * Create the cipher to handle encryption and decryption for the XML-based
	 * cross-platform file format.
	 * @param password A String containing the password, which will be used
     * to derive all our encryption parameters
	 * @param encrypt A boolean value specifying whether we should go into
     * encryption mode (true) or decryption mode (false)
	 * @return A BufferedBlockCipher in the specified mode
	 * @throws Exception Thrown whenever anything bad happens
	 */
	private static BufferedBlockCipher createXMLFormatCipher(String password,
			boolean encrypt, CryptnosApplication theApp) throws Exception {
		// I tried a dozen different things, none of which seemed to work
		// all that well.  I finally resorted to doing everyting the Bouncy
		// Castle way, simply because it brought things a lot closer to being
		// consistent.  Trying to do things entirely within .NET or Java just
		// wasn't cutting it.  There are, however, differences between the
		// implementations, which are denoted below.
		try
		{
			// Get the password's raw bytes:
			byte[] pwd = password.getBytes(theApp.getTextEncoding());
			byte[] salt = generateSaltFromPassword(password, theApp);
			// From the BC JavaDoc: "Generator for PBE derived keys and IVs as
			// defined by PKCS 5 V2.0 Scheme 2. This generator uses a SHA-1
			// HMac as the calculation function."  This is apparently a standard,
			// which makes my old .NET SecureFile class seem a bit embarrassing.
			PKCS5S2ParametersGenerator generator =
				new PKCS5S2ParametersGenerator();
			// Initialize the generator with our password and salt.  Note the
			// iteration count value.  Examples I found around the net set this
			// as a hex value, but I'm not sure why advantage there is to that.
			// I changed it to decimal for clarity.  1000 iterations may seem
			// a bit excessive, and I saw some real sluggishness on the Android
			// emulator that could be caused by this.  In the final program,
			// this should probably be set in a global app constant.
			generator.init(pwd, salt, KEY_ITERATION_COUNT);
			// Generate our parameters.  We want to do AES-256, so we'll set
			// that as our key size.  That also implies a 128-bit IV.  Note
			// that the 2-int method used here is considered deprecated in the
			// .NET library, which could be a problem in the long term.  This
			// is where .NET and Java diverge in BC; this is the only method
			// available in Java, and the comparable method is deprecated in
			// .NET.  I'm not sure how this will work going forward.  We need
			// to watch this, as this could be a failure point down the road.
			ParametersWithIV iv =
				((ParametersWithIV)generator.generateDerivedParameters(KEY_SIZE, IV_SIZE));
			// Create our AES (i.e. Rijndael) engine and create the actual
			// cipher object from it.  We'll use CBC padding.
			RijndaelEngine engine = new RijndaelEngine();
			BufferedBlockCipher cipher =
				new PaddedBufferedBlockCipher(new CBCBlockCipher(engine));
			// Pick our mode, encryption or decryption:
			cipher.init(encrypt, iv);
			// Return the cipher:
			return cipher;
		}
		catch (Exception e) { throw e; }
	}
	
	/**
	 * This handler receives messages from the various worker threads and
	 * updates the calling Activity's ProgessDialog with their status.  If
	 * the status is 100%, this closes the progress dialog and shuts down
	 * the thread.  Negative "percentage" statuses usually indicate some
	 * sort of error.
	 * @author Jeffrey T. Darlington
	 * @version 1.1
	 * @since 1.0
	 */
	private final Handler handler = new Handler()
	{
       public void handleMessage(Message msg) {
        	// Get our percent done and update the progress dialog:
            int total = msg.getData().getInt("percent_done");
            if (total >= 0) progressDialog.setProgress(total);
        	int count = msg.getData().getInt("site_count");
            // If we reach 100%, it's time to close up shop:
            if (total >= 100) {
                // Originally, we used dismissDialog() here to close the
            	// dialog.  The difference is that dismiss keeps the dialog
            	// around in memory, which is more efficient.  The caveat is
            	// that the next time we need it, our ListBuilderThread won't
            	// work properly.  So instead we'll remove the dialog once
            	// we're done, forcing Android to rebuild it and refresh the
            	// list every time.  Less efficient, but at least it works.
                caller.removeDialog(progressDialogID);
                // Create our success message:
                String message = null;
                // Check to see if one of the importers was being used.  If
                // so, we'll want to show the import complete message:
                if (oldFormatImporter != null || xmlFormatImporter != null) {
                    message = caller.getResources().getString(R.string.import_complete_message);
                    // As an additional step, when we import files, we'll want
                    // the application to set the site list as "dirty" so it
                    // will be rebuilt:
                    theApp.setSiteListDirty();
                // If we didn't use one of the importers, we must have used
                // the exporter.  Show the export complete message:
                } else {
                    message = caller.getResources().getString(R.string.export_complete_message);
                }
                message = message.replace(caller.getResources().getString(R.string.meta_replace_token), String.valueOf(count));
                Toast.makeText(caller, message, Toast.LENGTH_LONG).show();
                // Close the calling activity:
                caller.finish();
	        // If we got a "percentage" of -1, that indicates a bad import file
	        // or bad import password.  Warn the user as such and kill the
	        // dialog:
	        } else if (total == -1) {
	            caller.removeDialog(progressDialogID);
	        	Toast.makeText(caller, R.string.error_bad_import_file_or_password,
	            		Toast.LENGTH_LONG).show();
            // A "percentage" of -2 indicates that the import file could not
	        // be found, couldn't be read, or wasn't really a file (maybe it
	        // was a directory).  Warn the user and close the dialog:
			} else if (total == -2) {
			     caller.removeDialog(progressDialogID);
			     Toast.makeText(caller, R.string.error_bad_import_file,
			       		Toast.LENGTH_LONG).show();
            // A "percentage" of -3 indicates a general error during the export
			// process.  Warn the user and close the dialog:
			} else if (total == -3) {
			     caller.removeDialog(progressDialogID);
			     Toast.makeText(caller, R.string.error_bad_export,
			       		Toast.LENGTH_LONG).show();
		    // A "percentage" of -4 indicates the user's export parameters
			// weren't up to snuff.  Warn the user and close the dialog:
			} else if (total == -4) {
			     caller.removeDialog(progressDialogID);
			     Toast.makeText(caller, R.string.error_bad_export_params,
			       		Toast.LENGTH_LONG).show();
		    // A "percentage" of -1000 indicates something blew up while
			// trying to import the file in the cross-platform format.  Now
			// it's time to fall back and punt with the old format.  Create
			// the old format importer and let it have a go:
			} else if (total == -1000) {
			     oldFormatImporter =
			    	 new OldFormatImporter(handler, importPassword, importFilename);
			     oldFormatImporter.start();
			}
        }
	};
	
	/**
	 * This Thread performs the grunt work of the Cryptnos export process.
	 * Note that this class has changed starting with 1.1 to export only to
	 * the new XML-based cross-platform format.
	 * @author Jeffrey T. Darlington
	 * @version 1.1
	 * @since 1.0
	 */
	private class Exporter extends Thread
	{
		/** The Handler to update our status to */
		private Handler mHandler;
    	/** Our calling Activity */
		private Activity mCaller;
    	/** The password used to encrypt the file */
		private String mPassword;
    	/** The full path to the export file */
		private String mFilename;
        /** An array of Strings containing the site tokens of the parameters
         * to export */
		private String[] mSites = null;
		private CryptnosApplication theApp = null;

        /**
         * The Exporter constructor
         * @param caller The calling Activity
         * @param handler The Handler to update our status to
         * @param sites An array of Strings[] containing the site tokesn of
         * the parameters to export
         * @param password The password used to encrypt the file
         * @param filename The full path to the export file
         */
        Exporter(Activity caller, Handler handler, String[] sites,
        		String password, String filename, CryptnosApplication app) {
            mCaller = caller;
        	mHandler = handler;
        	mSites = sites;
        	mPassword = password;
        	mFilename = filename;
        	theApp = app;
        }
        
        @Override
        public void run() {
        	// Get us started:
            Message msg = null;
            Bundle b = null;
            // Assuming there are sites to export:
            if (mSites.length > 0) {
	            try
	            {
	            	// We want to write out our data to memory so we can
	            	// subsequently encrypt it.  This is, as far as I can tell,
	            	// the best equivalent to .NET's MemoryStream I can find:
	            	ByteArrayOutputStream ms = new ByteArrayOutputStream();
	            	// Neither Android nor Java have any conveniences for
	            	// *writing* XML, so we'll have to do it by hand.  Create
	            	// a PrintStream to conveniently write our text out, and
	            	// then pipe that through a GZIPOutputStream to compress
	            	// it.  All of this chains into the ByteArrayOutputStream
	            	// above, so we'll ultimately end up with a byte array
	            	// that contains the compressed XML.
	            	PrintStream out = new PrintStream(new GZIPOutputStream(ms),
	            			true, theApp.getTextEncoding());
	            	// Print our our XML header info.  Note that we're writing
	            	// a version 1 export file; later changes to the format
	            	// may require us to update that.  Also note that we'll
	            	// try to get the package info and extract the friendly
	            	// version number code to put in the <generator> tag.
	            	// If that blows up for some reason, we won't write out
	            	// the tag; it's optional but strongly recommended.  (At
	            	// least it will help with debugging.)
	            	out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
	            	out.println("<cryptnos xmlns=\"http://www.cryptnos.com/\">");
	            	out.println("\t<version>1</version>");
	            	try {
	        	        PackageInfo info =
	        	        	theApp.getPackageManager().getPackageInfo(theApp.getPackageName(),
	                			PackageManager.GET_META_DATA);
		            	out.println("\t<generator>Cryptnos for Android v" +
		            			info.versionName + "</generator>");
	                } catch (Exception e) { }
	                // Print out the site count tag.  We'll assume for now
	                // that every site we've been passed in will get exported,
	                // which should always be the case unless something is
	                // seriously wrong.
	                out.println("\t<siteCount>" +
	                		String.valueOf(mSites.length) + "</siteCount>");
	                // Now we'll begin our <sites> block.  From here on out,
	                // we'll need to iterate over our sites to export:
	                out.println("\t<sites>");
	            	// Declare our cursor:
	            	Cursor cursor = null;
	            	// Step through the sites in our list.  It might be more
	            	// efficient to just get all the sites in the DB and
	            	// compare the two lists, but for now we'll pull each
	            	// site from the DB individually.
	            	for (int i = 0; i < mSites.length; i++) {
	            		// Get the site from the DB:
	            		cursor = DBHelper.fetchRecord(SiteParameters.generateKeyFromSite(mSites[i], theApp));
	        	        mCaller.startManagingCursor(cursor);
	        	        cursor.moveToFirst();
	        	        if (cursor.getCount() == 1) {
		        	        // Convert it to a SiteParamemters object: 
		        	        SiteParameters params =
	    		        		new SiteParameters(theApp,
	    		        			cursor.getString(1),
	    		        			cursor.getString(2));
		        	        // Generate the XML tags from the SiteParameters
		        	        // object.  There's not much to comment on here,
		        	        // aside from the fact that we'll HTML-encode
		        	        // the text fields to make sure they go through
		        	        // without a problem.
		        	        out.println("\t\t<site>");
		        	        out.println("\t\t\t<siteToken>" +
		        	        		TextUtils.htmlEncode(params.getSite()) +
		        	        		"</siteToken>");
        	        		out.println("\t\t\t<hash>" + 
        	        				TextUtils.htmlEncode(params.getHash()) +
        	        				"</hash>");
        	        		out.println("\t\t\t<iterations>" +
        	        				String.valueOf(params.getIterations()) +
        	        				"</iterations>");
        	        		out.println("\t\t\t<charTypes>" +
        	        				String.valueOf(params.getCharTypes()) +
        	        				"</charTypes>");
        	        		out.println("\t\t\t<charLimit>" +
        	        				String.valueOf(params.getCharLimit()) +
        	        				"</charLimit>");
		        	        out.println("\t\t</site>");
	        	        }
	        	        // Update the progress dialog by sending a message to
	        	        // the handler.  Note that we're only going up to 90%
	        	        // here, as we'll estimate the rest of the work will
	        	        // encompass the remaining 10%.
	        	        msg = mHandler.obtainMessage();
		                b = new Bundle();
		                b.putInt("percent_done",
		                	(int)(Math.floor(((double)i / (double)mSites.length * 90.0d))));
		                b.putInt("site_count", mSites.length);
		                msg.setData(b);
		                mHandler.sendMessage(msg);
		                // Close the cursor for the next run:
		                cursor.close();
	            	}
	                // Close out the <sites> block and the rest of the file:
	                out.println("\t</sites>");
	                out.println("</cryptnos>");
	            	out.flush();
	            	out.close();
	            	// Now our memory stream should contain the raw binary
	            	// data of our compressed XML.  Grab that and put it into
	            	// a byte array:
	            	byte[] plaintext = ms.toByteArray();
	            	ms.close();
	            	// Create our cipher.  Note that we're using the
	            	// encryption mode, and that we're passing in the
	            	// password:
	            	BufferedBlockCipher cipher =
	            		createXMLFormatCipher(mPassword, true, theApp);
	            	// Create our ciphertext container.  Note that we call the
	                // cipher's getOutputSize() method, which tells us how big
	                // the resulting ciphertext should be.  In practice, this
	                // has always been the same size as the plaintext, but we
	                // can't take that for granted.
	            	byte[] ciphertext =
	            		new byte[cipher.getOutputSize(plaintext.length)];
	            	// Do the encryption.  Note that the Java version is a bit
					// different from the .NET version.  Here, we need to process
					// most of the data with processBytes() first, then do a final
					// call to doFinal() to finish things off.  Note that we also
					// have to keep track of the number of bytes processed in
					// the first step so we can pass it to the final step.  That
					// was a major gotcha that caused me a lot of headaches.
	            	int bytesSoFar = cipher.processBytes(plaintext, 0,
	            			plaintext.length, ciphertext, 0);
					cipher.doFinal(ciphertext, bytesSoFar);
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done", 95);
	                b.putInt("site_count", mSites.length);
	                msg.setData(b);
	                mHandler.sendMessage(msg);
					// Write the ciphertext to the export file:
					FileOutputStream fos = new FileOutputStream(mFilename);
	    			fos.write(ciphertext);
	    			fos.flush();
	    			fos.close();
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done", 100);
	                b.putInt("site_count", mSites.length);
	                msg.setData(b);
	                mHandler.sendMessage(msg);
	            }
	            // We should probably provide more detailed information here,
	            // but for now just tell the user that the export failed.
	            catch (Exception e)
	            {
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done", -3);
	                b.putInt("site_count", 0);
	                msg.setData(b);
	                mHandler.sendMessage(msg);
	            }
	        // This should have already been covered by the caller, but if
	        // if we got bad inputs, complain:
            } else {
	        	msg = mHandler.obtainMessage();
                b = new Bundle();
                b.putInt("percent_done", -4);
                b.putInt("site_count", 0);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
        }
	}
	
	/**
 	 * This Thread performs the grunt work of the Cryptnos import process if
 	 * the file is in the old platform-specific format.
	 * @author Jeffrey T. Darlington
	 * @version 1.1
	 * @since 1.0
	 */
	private class OldFormatImporter extends Thread
	{
		/** The Handler to update our status to */
		private Handler mHandler;
    	/** The password used to decrypt the file */
		private String mPassword;
    	/** The full path to the import file */
		private String mFilename;

        /**
         * The Importer constructor
         * @param handler The Handler to update our status to
         * @param password The password used to decrypt the file
         * @param filename The full path to the import file
         */
        OldFormatImporter(Handler handler, String password,
        		String filename) {
        	mHandler = handler;
        	mPassword = password;
        	mFilename = filename;
        }
        
        @Override
        public void run() {
            Message msg = null;
            Bundle b = null;
            
            try
            {
            	// Try to get the specified file and make sure it exists, it
            	// actually is a file, it's readable, and it's size does not
            	// exceed the maximum size of an integer (if it's too big,
            	// we can't read it):
	            File file = new File(mFilename);
	            if (file.exists() && file.isFile() && file.canRead() &&
	            		file.length() <= (long)Integer.MAX_VALUE)
	            {
	            	// Open the file and read its raw binary contents.  The
	            	// file shouldn't be very large, so we should have no
	            	// trouble putting it into memory.
	            	FileInputStream fis = new FileInputStream(file);
	            	byte[] encryptedData = new byte[(int)file.length()];
	            	fis.read(encryptedData, 0, encryptedData.length);
	            	fis.close();
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done", 5);
	                b.putInt("site_count", 0);
	                msg.setData(b);
	                mHandler.sendMessage(msg);
	                // Decrypt the data and put it into a string:
	    			Cipher cipher = createOldFormatCipher(mPassword, Cipher.DECRYPT_MODE);
	    			String unencryptedData = new String(cipher.doFinal(encryptedData));
	                // Split the decrypted data based on newlines, then step
	                // through the list:
	    			String[] sites = unencryptedData.split("\n");
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done", 10);
	                b.putInt("site_count", sites.length);
	                msg.setData(b);
	                mHandler.sendMessage(msg);
	    			for (int i = 0; i < sites.length; i++)
	    			{
	    				// Try to recreate the site parameters object from the
	    				// data.  Note that this will blow up if the data is
	    				// invalid.
	    				SiteParameters params =
	    					new SiteParameters(theApp, sites[i]);
	    				// Write the data to the database:
	    				DBHelper.createRecord(params);
	    				// Update the progress dialog.  Note that we're at the tail
	    				// end of the process here, so we're saying that reading the
	    				// file and decrypting the data amounts to 10% of the work.
	    				// We're doing the remaining 90%, so scale what we've done
	    				// to 1-90 and add the remaining 10% on top.
	        	        msg = mHandler.obtainMessage();
		                b = new Bundle();
		                b.putInt("percent_done",
		                	(int)(Math.floor(((double)i / (double)sites.length * 90.0d))) + 10);
		                b.putInt("site_count", sites.length);
		                msg.setData(b);
		                mHandler.sendMessage(msg);
	    			}
	    			// Just to make sure, force the progress dialog to say we're at
	    			// 100%:
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done", 100);
	                b.putInt("site_count", sites.length);
	                msg.setData(b);
	                mHandler.sendMessage(msg);
	            // The file could not be read, didn't exist, or wasn't a
	            // file at all:
	            } else {
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done", -2);
	                b.putInt("site_count", 0);
	                msg.setData(b);
	                mHandler.sendMessage(msg);
	            }
            }
            // Any number of things may have occurred to make the above
            // explode.  We should probably test for every option, but for
            // now we'll just assume the file wasn't valid and let it go.
            // Send a message back to the handler effectively saying as such:
            catch (Exception e)
            {
	        	msg = mHandler.obtainMessage();
                b = new Bundle();
                b.putInt("percent_done", -1);
                b.putInt("site_count", 0);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
        }
	}
	
	/**
	 * This XML SAX handler  will process parsing the new XML-based export
	 * format, ultimately building a list of site parameters to return to
	 * the importer.
	 * @author Jeffrey T. Darlington
	 * @version 1.1
	 * @since 1.1
	 */
	private class XMLHandler extends DefaultHandler
	{
		/** An ArrayList of SiteParameters holding the current list sites
		 *  we have successfully parsed from the file */
		private ArrayList<SiteParameters> siteList = null;
		/** The current set of working parameters we are actively building */
		private SiteParameters currentSite = null;
		/** A StringBuilder to let us gather the tag values piecemeal if
		 *  necessary */
		private StringBuilder builder = null;
		/** The count of sites in the file, as reported by the
		 *  &lt;siteCount&gt; tag */
		private int siteCount = 0;
		/** Whether or not we are currently inside the &lt;cryptnos&gt; tag */
		private boolean inCryptnosTag = false;
		/** Whether or not we are currently inside the &lt;version&gt; tag */
		private boolean inVersionTag = false;
		/** Whether or not we are currently inside either  the
		 *  &lt;generator&gt; or &lt;comment&gt; tags, which are currently
		 *  ignored */
		private boolean inIgnoredTag = false;
		/** Whether or not we are currently inside the &lt;siteCount&gt; tag */
		private boolean inSiteCountTag = false;
		/** Whether or not we are currently inside the &lt;sites&gt; tag */
		private boolean inSitesTag = false;
		/** Whether or not we are currently inside a &lt;site&gt; tag */
		private boolean inSiteTag = false;
		/** Whether or not we are currently in one of the parameter tags */
		private boolean inParamTag = false;
		/** A reference back to the Handler that updates the GUI of our
		 *  progress, so we can update the progress dialog */
		private Handler topHandler = null;
		/** A Message to pass back to the progress dialog */
        private Message msg = null;
        /** A Bundle for communicating with the progress dialog */
        private Bundle b = null;

		/**
		 * The XMLHandler constructor
		 * @param topHandler A reference back to the caller's handler, so
		 * we can update the progress dialog
		 */
        XMLHandler(Handler topHandler) {
			super();
			this.topHandler = topHandler;
		}
		
		@Override
	    public void startDocument() throws SAXException {
			// Start off by letting the super do its work, then initialize
			// our site list and StringBuilder to get things started:
	        super.startDocument();
	        siteList = new ArrayList<SiteParameters>();
	        builder = new StringBuilder();
	    }
		
		@Override
	    public void startElement(String uri, String localName, String name,
	            Attributes attributes) throws SAXException {
			// Let the super do its work first:
	        super.startElement(uri, localName, name, attributes);
	        // Take a look at the string builder's current contents.  If all
	        // we can find is whitespace, chop that out and ignore it.  This
	        // is to get around some problems I encountered where whitespace
	        // was making its way into values where it shouldn't.  We don't
	        // care about whitespace *between* tags, just whitespace that may
	        // be *part* of tags, such as whitespace inside the <siteToken>
	        // tag value.
	        if (builder.toString().matches("^\\s+$")) builder.setLength(0);
	        // Are we entering the <cryptnos> tag?  Note this must be at the
	        // root of everything, so this tag cannot be inside any other
	        // tag, including a nested <cryptnos> tag.
	        if (localName.equalsIgnoreCase("cryptnos") && !inVersionTag &&
	        		!inIgnoredTag && !inSitesTag && !inSiteTag &&
	        		!inParamTag && !inSiteCountTag && !inCryptnosTag){
	        	inCryptnosTag = true;
	        // Are we entering the <version> tag?  Note this should only be
	        // valid if we're inside the <cryptnos> tag as well.
	        } else if (localName.equalsIgnoreCase("version") && inCryptnosTag
	        		&& !inIgnoredTag && !inSitesTag && !inSiteTag &&
	        		!inParamTag && !inVersionTag && !inSiteCountTag){
	        	inVersionTag = true;
	        // Are we entering the <siteCount> tag?  Note this should only be
	        // valid if we're inside the <cryptnos> tag as well.
	        } else if (localName.equalsIgnoreCase("siteCount") && inCryptnosTag
	        		&& !inIgnoredTag && !inSitesTag && !inSiteTag &&
	        		!inParamTag && !inVersionTag && !inSiteCountTag){
	        	inSiteCountTag = true;
	        // Are we entering the <sites> tag?  Note this should only be
	        // valid if we're inside the <cryptnos> tag as well.
	        } else if (localName.equalsIgnoreCase("sites") && inCryptnosTag
	        		&& !inVersionTag && !inIgnoredTag && !inSiteTag &&
	        		!inParamTag && !inSitesTag && !inSiteCountTag){
	        	inSitesTag = true;
	        	msg = topHandler.obtainMessage();
                b = new Bundle();
                b.putInt("percent_done", 15);
                b.putInt("site_count", 0);
                msg.setData(b);
                topHandler.sendMessage(msg);
	        // Are we entering the <generator> or <comment> tags?  These
	        // should only be valid if we're inside the <cryptnos> tag.  Also
	        // note we don't really care what's in them; they are just going
	        // to be ignored for now.
	        } else if ((localName.equalsIgnoreCase("generator") ||
	        		localName.equalsIgnoreCase("comment")) && inCryptnosTag
	        		&& !inVersionTag && !inSitesTag && !inSiteTag &&
	        		!inParamTag && !inIgnoredTag && !inSiteCountTag){
	        	inIgnoredTag = true;
	        // Are we entering a <site> tag?  We must be inside the <cryptnos>
	        // and <sites> tag for this to be valid.  This is the main place
	        // we actually do work on a start tag, because we need to
	        // initialize the current site object before continuing.
	        } else if (localName.equalsIgnoreCase("site") && inCryptnosTag &&
	        		inSitesTag && !inVersionTag && !inIgnoredTag &&
	        		!inSiteTag && !inParamTag && !inSiteCountTag){
	            this.currentSite = new SiteParameters(theApp);
	            inSiteTag = true;
	        // Are we entering one of the parameter tags?  We have to be
	        // deeply nested inside a <site> tag and all its hierarchy for
	        // this to be valid.  We won't bother being persnickety about
	        // keeping track of which tag we're in.  We probably *should* but
	        // for now this is more complex than I really wanted anyway.
	        } else if (inCryptnosTag && inSitesTag && inSiteTag && 
	        		!inVersionTag && !inIgnoredTag && !inParamTag &&
	        		!inSiteCountTag && (
	        		localName.equalsIgnoreCase("siteToken") || 
	        		localName.equalsIgnoreCase("hash") || 
	        		localName.equalsIgnoreCase("iterations") || 
	        		localName.equalsIgnoreCase("charTypes") || 
	        		localName.equalsIgnoreCase("charLimit"))) {
	        	inParamTag = true;
	        // If *ANYTHING* blew up above, then our XML is not well formed
	        // and doesn't follow the schema.  Blow up:
	        } else throw new SAXException("Unexpected tag or invalid tag order");
	    }

		@Override
	    public void characters(char[] ch, int start, int length)
	            throws SAXException {
			// Let the super do its work, then pull out the characters we
			// read and stuff them in the StringBuilder.  Hopefully, this
			// we be a value we can use later.
	        super.characters(ch, start, length);
	        builder.append(ch, start, length);
	    }

	    @Override
	    public void endElement(String uri, String localName, String name)
	            throws SAXException {
	    	// Let the super do its work:
	        super.endElement(uri, localName, name);
	        try {
	        	// Note that in all of these cases, we don't get as anal as
	        	// we did about tag nesting as when we were processing the
	        	// start tags.  The way I figure it, that did most of our
	        	// schema checking for us, so there's no need to do it again.
	        	// Here, we're mostly checking to close out our status and do
	        	// anything that needs to be done by closing the tag, such as
	        	// processing the value.  In each case, though, we need to
	        	// make sure that the tag we're closing has already been
	        	// opened.
	        	
	        	// We encountered a closing <cryptnos> tag:
	        	if (localName.equalsIgnoreCase("cryptnos") && inCryptnosTag){
	        		inCryptnosTag = false;
	        	// We encountered a closing <version> tag.  If that's the case,
	        	// pull the value from the StringBuilder and attempt to parse
	        	// it to an integer.  If the value is 1 (the only valid file
	        	// version we currently support), then the file is valid;
	        	// otherwise, blow up:
	        	} else if (localName.equalsIgnoreCase("version") &&
	        			inVersionTag){
	        		inVersionTag = false;
	        		int version = Integer.parseInt(builder.toString().trim());
	        		if (version != 1) throw new Exception();
	        	// We encountered a closing <generator> or <comment> tag:
	        	} else if ((localName.equalsIgnoreCase("generator") ||
		        		localName.equalsIgnoreCase("comment")) && inIgnoredTag){
	        		inIgnoredTag = false;
	        	// We encountered a closing <siteCount> tag.  Parse the value
	        	// and make sure it is an integer greater than zero; we cannot
	        	// have an export file that does not have at least one site.
	        	} else if (localName.equalsIgnoreCase("siteCount") &&
	        			inSiteCountTag){
	        		inSiteCountTag = false;
	        		siteCount = Integer.parseInt(builder.toString().trim());
	        		if (siteCount <= 0) throw new Exception();
	        	// We encountered a closing <sites> tag:
	        	} else if (localName.equalsIgnoreCase("sites") && inSitesTag){
	        		inSitesTag = false;
	        	// We encountered a closing <siteToken> tag.  Note that this
	        	// and the rest of the tags below are only valid if we're (a)
	        	// inside a <site> tag and (b) the current site object is
	        	// valid.
	        	} else if (localName.equalsIgnoreCase("siteToken") &&
	        			inSiteTag && inParamTag && currentSite != null){
	            	currentSite.setSite(builder.toString().trim());
	            	inParamTag = false;
	        	// We encountered a closing <hash> tag:
	            } else if (localName.equalsIgnoreCase("hash") && inSiteTag &&
	            		inParamTag && currentSite != null){
	            	currentSite.setHash(builder.toString().trim());
	            	inParamTag = false;
	        	// We encountered a closing <iterations> tag.  Note that we
	            // need to parse the value into an integer for this one and
	            // the next two, so if we didn't get an integer, this will
	            // blow up.
	            } else if (localName.equalsIgnoreCase("iterations") &&
	            		inSiteTag && inParamTag && currentSite != null){
	            	currentSite.setIterations(Integer.parseInt(builder.toString().trim()));
	            	inParamTag = false;
	        	// We encountered a closing <charTypes> tag:
	            } else if (localName.equalsIgnoreCase("charTypes") &&
	            		inSiteTag && inParamTag && currentSite != null){
	            	currentSite.setCharTypes(Integer.parseInt(builder.toString().trim()));
	            	inParamTag = false;
	        	// We encountered a closing <charLimit> tag:
	            } else if (localName.equalsIgnoreCase("charLimit") &&
	            		inSiteTag && inParamTag && currentSite != null){
	            	currentSite.setCharLimit(Integer.parseInt(builder.toString().trim()));
	            	inParamTag = false;
	        	// We encountered a closing <site> tag.  Again, we have to be
	            // in a <site> tag and the current site must be valid for this
	            // to work.  Take the site parameters object and stuff it into
	            // the site list.
	            } else if (localName.equalsIgnoreCase("site") && inSiteTag &&
	            		currentSite != null){
	                siteList.add(currentSite);
	                inSiteTag = false;
	                // For our percent done, we're scaling this part of the
	                // process to be between 15-50%, or a range of 35.  The
	                // first 15% is the reading and decrypting of the data,
	                // while the remaining 50% will be updating the database.
		        	msg = topHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done",
		                	(int)(Math.floor(((double)siteList.size() / (double)siteCount * 35.0d))) + 15);	                b.putInt("site_count", 0);
	                b.putInt("site_count", siteCount);
	                msg.setData(b);
	                topHandler.sendMessage(msg);
	            // If we didn't encounter any of the above conditions, the
	            // schema must not be valid:
	            } else throw new Exception();
	        	// In pretty much every case, we'll need to clear out the
	        	// StringBuilder anyway, so we'll just do it once here:
	        	builder.setLength(0);
	        }
	        // If we caught any exceptions, throw a SAXException here to
	        // indicate that our parsing was invalid:
	        catch (Exception ex) {
	        	throw new SAXException("Invalid data type");
	        }
	    }

	    /**
	     * Get the list of SiteParameters parsed from the XML
	     * @return An array of SiteParameters objects
	     */
	    Object[] getSites() {
	    	// This only makes sense if the site list is populated.  If it
	    	// isn't, return null:
			if (siteList == null || siteList.isEmpty()) return null;
			else return siteList.toArray();
		}
	}
	
	/**
	 * This Thread performs the grunt work of the Cryptnos import process if
 	 * the file is in the new XML-based cross-platform format.
	 * @author Jeffrey T. Darlington
	 * @version 1.1
	 * @since 1.1
	 */
	private class XMLFormat1Importer extends Thread
	{
		/** The Handler to update our status to */
    	private Handler mHandler;
    	/** The password used to decrypt the file */
    	private String mPassword;
    	/** The full path to the import file */
    	private String mFilename;

        /**
         * The XMLFormat1Importer constructor
         * @param handler The Handler to update our status to
         * @param password The password used to decrypt the file
         * @param filename The full path to the import file
         */
    	XMLFormat1Importer(Handler handler, String password,
        		String filename) {
        	mHandler = handler;
        	mPassword = password;
        	mFilename = filename;
        }
        
        @Override
        public void run() {
            Message msg = null;
            Bundle b = null;
            
            try {
            	// Try to get the specified file and make sure it exists, it
            	// actually is a file, it's readable, and it's size does not
            	// exceed the maximum size of an integer (if it's too big,
            	// we can't read it):
	            File file = new File(mFilename);
	            if (file.exists() && file.isFile() && file.canRead() &&
	            		file.length() <= (long)Integer.MAX_VALUE)
	            {
	            	// Open the file and read its raw binary contents.  The
	            	// file shouldn't be very large, so we should have no
	            	// trouble putting it into memory.
	            	FileInputStream fis = new FileInputStream(file);
	            	byte[] encryptedData = new byte[(int)file.length()];
	            	fis.read(encryptedData, 0, encryptedData.length);
	            	fis.close();
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done", 5);
	                b.putInt("site_count", 0);
	                msg.setData(b);
	                mHandler.sendMessage(msg);
	                // Create our cipher in decrypt mode:
					BufferedBlockCipher cipher =
						createXMLFormatCipher(mPassword, false, theApp);
					// Create our plaintext container:
					byte[] plaintext =
						new byte[cipher.getOutputSize(encryptedData.length)];
					// Decrypt the data.  See the export method for notes on
					// decryption here vs decryption in .NET.
					int bytesSoFar = cipher.processBytes(encryptedData, 0,
							encryptedData.length, plaintext, 0);
					cipher.doFinal(plaintext, bytesSoFar);
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done", 10);
	                b.putInt("site_count", 0);
	                msg.setData(b);
	                mHandler.sendMessage(msg);
	                // Now we need to parse the decrypted data.  To do that,
	                // we'll need to unzip it and parse the XML.  We'll chain
	                // some streams together here to get things started.
	                BufferedInputStream in =
	                	new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(plaintext)));
	                // Create a handler to do the grunt work of dealing with
	                // the XML.  While the parser tokenizes things for us,
	                // it doesn't do any logic with the data.
	                XMLHandler xmlHandler = new XMLHandler(mHandler);
	                // Set up a SAX parser and feed it both the unzipped data
	                // and the handler.  It will internally build the list of
	                // site parameters if successful.
	                SAXParser parser =
	                	SAXParserFactory.newInstance().newSAXParser();
	                parser.parse(in, xmlHandler);
	                // Now try to get the site parameters from the handler
	                // and close the streams:
	                Object[] sites = xmlHandler.getSites();
	                in.close();
	                // If we got any useful data, we'll proceed from here:
	                if (sites != null && sites.length > 0) {
		    			for (int i = 0; i < sites.length; i++)
		    			{
		    				// Write the data to the database:
		    				DBHelper.createRecord((SiteParameters)sites[i]);
		    				// Update the progress dialog.  Note that we're at the tail
		    				// end of the process here, so we're saying that reading the
		    				// file and decrypting the data amounts to 10% of the work.
		    				// We're doing the remaining 50%, so scale what we've done
		    				// to 1-50 and add the remaining 50% on top.
		        	        msg = mHandler.obtainMessage();
			                b = new Bundle();
			                b.putInt("percent_done",
			                	(int)(Math.floor(((double)i / (double)sites.length * 50.0d))) + 50);
			                b.putInt("site_count", sites.length);
			                msg.setData(b);
			                mHandler.sendMessage(msg);
		    			}
		    			// If we get to here, everything must have gone A-OK.
		    			// Explicitly send a 100% complete here to close out
		    			// the progress dialog.  (I originally left this out,
		    			// which resulted in the program hanging on the dialog
		    			// and no way to close it.  Oops.)
	        	        msg = mHandler.obtainMessage();
		                b = new Bundle();
		                b.putInt("percent_done", 100);
		                b.putInt("site_count", sites.length);
		                msg.setData(b);
		                mHandler.sendMessage(msg);
		    		// If we couldn't get any useful sites from the file,
		    		// complain:
	                } else {
			        	msg = mHandler.obtainMessage();
		                b = new Bundle();
		                b.putInt("percent_done", -1000);
		                b.putInt("site_count", 0);
		                msg.setData(b);
		                mHandler.sendMessage(msg);
		            }
	            // The file didn't exist, wasn't a file, couldn't be read, or
	            // was too long to read:
	            } else {
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done", -1000);
	                b.putInt("site_count", 0);
	                msg.setData(b);
	                mHandler.sendMessage(msg);
	            }
            }
            // Any number of things may have occurred to make the above
            // explode.  We should probably test for every option, but for
            // now we'll just assume the file wasn't valid and let it go.
            // Send a message back to the handler effectively saying as such:
            catch (Exception e)
            {
	        	msg = mHandler.obtainMessage();
                b = new Bundle();
                b.putInt("percent_done", -1000);
                b.putInt("site_count", 0);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
        }
	}
	
}
