/*
 * A Simple Pong Game written by Leszek Lesner
 * released under the terms of GPL (later maybe BeerLicense) :P
 */

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.RemoteDevice;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import net.java.dev.marge.autocon.AutoConnect;
import net.java.dev.marge.communication.CommunicationListener;
import net.java.dev.marge.communication.ConnectionListener;
import net.java.dev.marge.entity.Device;
import net.java.dev.marge.entity.ServerDevice;
import net.java.dev.marge.entity.config.ServerConfiguration;
import net.java.dev.marge.factory.CommunicationFactory;
import net.java.dev.marge.factory.RFCOMMCommunicationFactory;
import net.java.dev.marge.inquiry.DeviceDiscoverer;
import net.java.dev.marge.inquiry.InquiryListener;
import util.ImageUtil;

public class myMidlet extends MIDlet implements CommandListener, CommunicationListener, ConnectionListener {

    private final Pong game = new Pong(this);
    private List menu,  menu2;
    private StringItem dialogText;
    private Form dialogForm;
    private Command dialogExit;
    private int lastMenuSelection = 0,  lastMenuSelection2 = 0;
    //private DevicesList deviceList;
    Pong gameCanvas;
    public static myMidlet instance;
    public Device device;
boolean connected = false;
    public myMidlet() {
        super();
        myMidlet.instance = this;
    }

    private void startblueclient() {
        try {
            //TODO: Implement Client
            System.out.println("Client");
            this.device = AutoConnect.connectToServer("bla", myMidlet.this);
            this.gameCanvas.setIsServer(false);
            //Display.getDisplay(this).setCurrent(this.deviceList);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void startblueserver() {
        //TODO: Implement Server
        System.out.println("Server");
    // Erstelle Werkseinstellungen, die das RFCOMM-Protokoll nutzten. Wenn Sie das L2CAP-Protokoll nutzen möchten,
    //benutzten Sie bitte L2CAPCommunicationFactory
    // CommunicationFactory factory = new RFCOMMCommunicationFactory();

    // Erstellen Sie eine Serverkonfiguration. Sie können eine spezifische Konfigurationen
    // mit dieser Klasse nutzen.
    //ServerConfiguration sconf = new ServerConfiguration(new CommunicationListenerImpl());

    // Erstellen Sie eine Werkseinstellung für Server. Die Server-Geräte-Instanz wird
    // umgesetzt von der ConnectionListenerImpl,
    // welche eine Implemention von ConnectionListener ist.
    //factory.waitClients(sconf, new ConnectionListenerImpl());
        AutoConnect.createServer("bla", myMidlet.this, myMidlet.this);
        this.gameCanvas.setIsServer(true);
        while(!connected) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        String message = "" + game.player1y ;     // dirty way to convert int to string ;)
        this.device.send(message.getBytes());     // What message do we have to put there ?
    }

    public void receiveMessage(byte[] arg0) {
        //String bla = arg0.toString();
        //System.out.println(bla);
        //game.player1y = Integer.parseInt(bla);

        String msg = new String(arg0);
        int firstIndex = 0;
        int lastIndex = 0;
        while ((lastIndex = msg.indexOf(";", firstIndex)) != -1) {

            try {

                String nextToken = msg.substring(firstIndex, lastIndex);
                if (gameCanvas.isServer) {
                    gameCanvas.player2y = Integer.parseInt(nextToken.substring(1));
                } else {
                    int indexOfBallPosition = nextToken.indexOf("b");   // hier hakt noch was
                    gameCanvas.player1y = Integer.parseInt(nextToken.substring(1, indexOfBallPosition));
                    int[] values = ImageUtil.separeValues(nextToken.substring(indexOfBallPosition + 1), "x");
                    gameCanvas.ballx = values[0];
                    gameCanvas.bally = values[1];
                    gameCanvas.repaint();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            firstIndex = lastIndex + 1;
        }

    }

    public void errorOnReceiving(IOException arg0) {
        arg0.printStackTrace();
    }

    public void errorOnSending(IOException arg0) {
        arg0.printStackTrace();
    }

    public void connectionEstablished(ServerDevice arg0, RemoteDevice arg1) {
        this.device = arg0;
        this.connected = true;
        //startGame();
        System.out.println("Connection Established!");
        Display.getDisplay(this).setCurrent(game);
    }

    public void errorOnConnection(IOException arg0) {
        arg0.printStackTrace();
    }

    public void initialisationSucessful() {
        startGame();
    }

    /**
     *  show splash screen
     */
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
                    g.drawImage(sImage, getWidth() / 2, getHeight() / 2, Graphics.VCENTER | Graphics.HCENTER);
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
        // End Of splash screen
        if (game.isRuning()) {
            Display.getDisplay(this).setCurrent(game);
        } else {
            showMenu();
        }
    }

    public void showMenu() {
        menu = new List("Pong42", Choice.IMPLICIT);
        menu.append("2 Spieler Spiel über Bluetooth Client", null);
        menu.append("2 Spieler Spiel über Bluetooth Server", null);
        if (game.isStarted()) {
            menu.append("Spiel fortsetzen", null);
        } else {
            menu.append("2 Spieler Spiel starten", null);
        }
        menu.append("Einzelspiel starten", null);
        menu.append("Hilfe", null);
        menu.append("Über", null);
        menu.append("Beenden", null);
        menu.setSelectedIndex(lastMenuSelection, true);
        menu.setCommandListener(this);
        Display.getDisplay(this).setCurrent(menu);

        this.gameCanvas = new Pong();
    //this.deviceList = new DevicesList(this, gameCanvas);
    }

    /**
     * Starts a new game, or resumes a paused game.
     */
    private void startGame() {
        if (game.isStarted() == false) {
            game.start(false); // false = not practice mode
        }		//Display.getDisplay(this).setCurrent(game);
    }

    /**
     * starts a new game in practice mode.
     * kills any running game that was in background
     */
    private void startpractice() {
        game.start(true); // true = practice mode
        Display.getDisplay(this).setCurrent(game);
    }

    /**
     * shows a dialog with text.
     * @param textType what to show. values: [Hilfe|Über]. defaults to Über
     */
    private void showDialog(String textType) {
        dialogForm = new Form(textType);
        dialogText = new StringItem("", "");
        dialogExit = new Command("Back", Command.BACK, 1);
        dialogForm.append(dialogText);
        dialogForm.addCommand(dialogExit);
        dialogForm.setCommandListener(this);
        if (textType == "Hilfe") {
            dialogText.setLabel("Left player keys: [1]up, [7]down.\n" +
                    "Right player keys: [3]up, [9]down.\n\n" +
                    "Win points by making goals on your oponent side.\nThe better player will rebate ball slower over time while the player with less points will rebate them faster.\n\n" +
                    "On practice mode, you control the Left piece, the phone controls the right one.\n\n" +
                    "KNOWN ISSUES:\nOn sone phones (e.g Nokia6820) the keys may change (e.g. [9]and[#] instead of [3]and[9]).\n\n" +
                    "Some models have a problem that they cannot process more then one key at a time. One players should be polite and release the key when not defending.\n" +
                    "Some models (e.g. 6820) have work-arounds, for example, it doesn't accepet two keys being pressed while closed, but if the flip is open, up to four keys can be pressed at the same time. Try out your model.");
        } else {
            dialogText.setLabel("Pong42 programmiert von Anatolij and Leszek\n\n" +
                    "Werbung muss sein ;) http://www.zevenos.com :P/\n\n");
        }
        Display.getDisplay(this).setCurrent(dialogForm);
    }

    protected void pauseApp() {
        game.pause();
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
    }

    /* Deal with any button being pressed on the game menu.
     */
    public void commandAction(final Command cmd, Displayable displayable) {
        new Thread() {

            public void run() {

                if (cmd == dialogExit) {
                    showMenu();
                } else {
                    lastMenuSelection = menu.getSelectedIndex();
                    switch (lastMenuSelection) {
                        case 0:
                            startblueclient();
                            break;
                        case 1:
                            startblueserver();
                            
                            break;
                        case 2:
                            startGame();
                            break;
                        case 3:
                            startpractice();
                            break;
                        case 4:
                            showDialog("Hilfe");
                            break;
                        case 5:
                            showDialog("Über");
                            break;
                        case 6:
                            try {
                                destroyApp(false);
                            } catch (MIDletStateChangeException ex) {
                                ex.printStackTrace();
                            }
                            notifyDestroyed();
                            break;
                    }
                }
            }
        }.start();
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}
