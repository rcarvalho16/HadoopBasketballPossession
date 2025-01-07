package cdle.opencv;

import java.io.File;
import java.io.IOException;

import org.opencv.core.Core;

public class Utils {
	public static void loadNative() {
		try {
			System.out.printf( "[%s] Trying to load OpenCV library (%s) using native mode.\n", Utils.class.getName(), Core.NATIVE_LIBRARY_NAME );

			System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
			
			System.out.printf( "[%s] Library loaded using native mode.\n", Utils.class.getName() );
		}
		catch (Throwable t) {
			System.err.printf( "[%s] Error loading library.\n", Utils.class.getName() );
			System.err.printf( "Details:\n%s\n", t.getMessage()  );
		}
	}
	
	public static void loadLocal() {
		try {
			System.out.printf( "[%s] Trying to load OpenCV library from jar file.\n", Utils.class.getName() );
						
			nu.pattern.OpenCV.loadLocally();
			
			System.out.printf( "[%s] Library loaded from jara file.\n", Utils.class.getName() );
		}
		catch (Throwable t) {
			System.err.printf( "[%s] Error loading library.\n", Utils.class.getName() );
			System.err.printf( "Details:\n%s\n", t.getMessage()  );
		}
	}
	
	public static void showFiles() {
		showFiles( "." );
	}
	
	public static void showFiles(String directory) {
		try {
			File currentDirectory = new File( directory );
			File[] files = currentDirectory.listFiles();
			
			System.out.printf( "Existing files (%s):\n", currentDirectory.getCanonicalFile().toString() );
			for (File file : files) {
				System.out.printf( "\t%s (%d)\n", file.getName(), file.length() );
			}
		}
		catch (IOException ioEx) {
			System.err.printf( "[%s] Error listing file.\n", Utils.class.getName() );
			System.err.printf( "Details:\n%s\n", ioEx.getMessage()  );
		}
	}
}
