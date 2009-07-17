package util;

import java.io.IOException;

import javax.microedition.lcdui.Image;

public class ImageUtil {

	public static Image loadImage(String filename) {
		Image i = null;
		try {
			i = Image.createImage(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return i;
	}
	
	public static int[] separeValues(String str, String sep) {
		int values[] = new int[2];
		int separatorIndex = str.indexOf(sep);
		values[0] = Integer.parseInt(str.substring(0, separatorIndex));
		values[1] = Integer.parseInt(str.substring(separatorIndex + 1));
		return values;
	}
}
