

import java.io.IOException;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import net.java.dev.marge.entity.Device;

public class PongMIDlet extends MIDlet {

	public static PongMIDlet instance;
	
	private Display display;
	
	private Device device;
	
	public PongMIDlet() {
		PongMIDlet.instance = this;
		this.display = Display.getDisplay(this);
	}
        
        public void exit() {
            this.notifyDestroyed();
        }

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		// TODO Auto-generated method stub

	}

	protected void pauseApp() {
		// TODO Auto-generated method stub

	}

	protected void startApp() throws MIDletStateChangeException {
        splashCanvas splash = null;
        try {
            splash = new splashCanvas();
            Display.getDisplay(this).setCurrent(splash);
        } finally {
            splash = null;
        }
        long loopStartTime = System.currentTimeMillis();
        int loopDelay = 2000;
        while (true) {
            if (System.currentTimeMillis() - loopStartTime > loopDelay) {
                break;
            }
        }
		this.setCurrentDisplayable(new PongMenu());
	}
	
	public void setCurrentDisplayable(Displayable d) {
		this.display.setCurrent(d);
	}
	
	public void showAlert(AlertType type, String title, String message) {
		Alert a = new Alert(title, message, null, type);
		this.setCurrentDisplayable(a);
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}
        private class splashCanvas extends Canvas {

        protected void paint(Graphics g) {
            Image sImage = null;
            try {
                g.setColor(0, 0, 0);
                g.fillRect(0, 0, getWidth(), getHeight());

                try {
                    sImage = Image.createImage("/splash.png");
                } catch (IOException e) {
                }
                if (sImage != null) {
                    g.drawImage(sImage, getWidth() >> 1, getHeight() / 2, Graphics.VCENTER | Graphics.HCENTER);
                }

                g.setColor(255, 0, 0);
                g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE));
                g.drawString("Pong42", getWidth() - 1, 1, Graphics.RIGHT | Graphics.TOP);
                g.setColor(255, 255, 255);
                g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL));
                g.drawString("Released under the terms of GPL", 1, getHeight() - 1, Graphics.LEFT | Graphics.BOTTOM);
            } finally {
                sImage = null;
            }
        }
    }
}
