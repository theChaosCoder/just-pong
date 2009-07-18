
import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;


import javax.microedition.lcdui.StringItem;
import javax.microedition.media.MediaException;
import net.java.dev.marge.communication.ConnectionListener;
import net.java.dev.marge.entity.ServerDevice;
import net.java.dev.marge.entity.config.ServerConfiguration;
import net.java.dev.marge.factory.RFCOMMCommunicationFactory;
import net.java.dev.marge.inquiry.DeviceDiscoverer;
import net.java.dev.marge.inquiry.InquiryListener;


import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.midlet.MIDlet;

public class PongMenu extends List implements CommandListener,
        InquiryListener, ConnectionListener {

    private Command exit;
    private DevicesList deviceList;
    PongCanvas gameCanvas;
    private Form dialogForm;
    private StringItem dialogText;
    private Command dialogExit;
    private boolean p;
    Player midiPlayer = null;

    public PongMenu() {
        super("Pong42, die Antwort auf alle Pong Games", List.IMPLICIT);

        this.addCommand(this.exit = new Command("Exit", Command.EXIT, 1));
        this.addCommand(new Command("OK", Command.OK, 1));

        this.append("2 Spieler Client", null);
        this.append("2 Spieler Server", null);
        this.append("1 Spieler gegen CPU", null);
        this.append("Hilfe", null);
        this.append("About", null);
        this.append("Beenden", null);

        this.setCommandListener(this);
        this.gameCanvas = new PongCanvas();
        this.deviceList = new DevicesList(this, gameCanvas);
        this.PlayMIDI(true, 0);
    }

    public void PlayMIDI(boolean p, int song) {
        // Musik
        if (p) {
            if (song == 0) {
                try {
                    midiPlayer = Manager.createPlayer(getClass().getResourceAsStream("/gradius.mid"), "audio/midi");
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
            if (song == 1) {
                try {
                    midiPlayer = Manager.createPlayer(getClass().getResourceAsStream("/bubbleb.mid"), "audio/midi");
                } catch (Exception e) {
                    System.err.println(e);
                }
            }

            try {
                if (midiPlayer != null) {
                    midiPlayer.start();
                }
            } catch (Exception e) {
                System.err.println(e);
            }
        } else {
            try {
                midiPlayer.stop();
            } catch (MediaException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void commandAction(Command c, Displayable arg1) {
        if (c == this.exit) {
            PongMIDlet.instance.notifyDestroyed();
        } else {
            switch (getSelectedIndex()) {
                case 0: //Client
                    try {
                        this.PlayMIDI(false, 0);
                        this.PlayMIDI(true, 1);
                        gameCanvas.setIsServer(false);
                        DeviceDiscoverer.getInstance().startInquiryGIAC(this);
                        PongMIDlet.instance.setCurrentDisplayable(this.deviceList);
                    } catch (BluetoothStateException e) {
                        e.printStackTrace();
                    }
                    break;
                case 1: //Server
                    this.PlayMIDI(false, 0);
                    this.PlayMIDI(true, 1);
                    gameCanvas.setIsServer(true);
                    RFCOMMCommunicationFactory factory = new RFCOMMCommunicationFactory();
                    ServerConfiguration serverConfiguration = new ServerConfiguration(gameCanvas);
                    factory.waitClients(serverConfiguration, this);
                    try {
                        PongMIDlet.instance.setCurrentDisplayable(new DefaultLoadingScreen("pong42", "Starting... " + LocalDevice.getLocalDevice().getBluetoothAddress(), null));
                    } catch (BluetoothStateException ex) {
                        PongMIDlet.instance.setCurrentDisplayable(new DefaultLoadingScreen("pong42", "Starting...", null));
                    }
                    break;
                case 2: //Gegen CPU spielen
                    this.PlayMIDI(false, 0);
                    this.PlayMIDI(true, 1);
                    gameCanvas.setIsCPU(false);
                    gameCanvas.setIsCPU(true);
                    this.gameCanvas.initialize(); // zum test erstmal, man muss server sein damit man das paddle bewegen kann^^
                    break;
                case 3: //Hilfe
                    Alert h = new Alert("Hilfe pong42",
                            "Blabla\n\n" +
                            "Hilfetext\n\n", null, AlertType.CONFIRMATION);
                    h.setTimeout(Alert.FOREVER);
                    PongMIDlet.instance.setCurrentDisplayable(h);
                    break;
                case 4: //About
                    //PongMIDlet.instance.setCurrentDisplayable(new About(this));
                    Alert a = new Alert("About pong42",
                            "Pong42 programmiert von Anatolij and Leszek\n\n" +
                            "Werbung muss sein ;) http://www.zevenos.com :P\n\n", null, AlertType.INFO);
                    a.setTimeout(Alert.FOREVER);
                    PongMIDlet.instance.setCurrentDisplayable(a);
                    break;
                case 5: //Exit
                    PongMIDlet.instance.notifyDestroyed();
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass arg1) {
        this.deviceList.addRemoteDevice(remoteDevice);

    }

    public void inquiryError() {
        System.out.println("inquiryError");
    }

    public void inquiryCompleted(RemoteDevice[] arg0) {
    }

    public void connectionEstablished(ServerDevice remoteDevice, RemoteDevice arg1) {
        PongMIDlet.instance.setDevice(remoteDevice);
        this.gameCanvas.initialize();
        remoteDevice.startListening();
    }

    public void errorOnConnection(IOException arg0) {
        arg0.printStackTrace();
    }
}
