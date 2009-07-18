

import java.io.IOException;

import java.util.Vector;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import net.java.dev.marge.entity.ClientDevice;
import net.java.dev.marge.entity.config.ClientConfiguration;
import net.java.dev.marge.factory.RFCOMMCommunicationFactory;
import net.java.dev.marge.inquiry.DeviceDiscoverer;
import net.java.dev.marge.inquiry.ServiceDiscoverer;
import net.java.dev.marge.inquiry.ServiceSearchListener;

public class DevicesList extends List implements CommandListener, ServiceSearchListener {

    private final Command select = new Command("Select",
            Command.SCREEN, 1);
    private final Command back = new Command("Back", Command.BACK, 1);
    private Vector remoteDevices;
    private Displayable previous;
    private PongCanvas gameCanvas;

    public DevicesList(Displayable previous, PongCanvas gameCanvas) {
        super("Found Devices", List.IMPLICIT);
        this.previous = previous;
        this.gameCanvas = gameCanvas;
        remoteDevices = new Vector();
        this.addCommand(select);
        this.addCommand(back);
        this.setCommandListener(this);
    }

    public void addRemoteDevice(RemoteDevice remoteDevice) {
        remoteDevices.addElement(remoteDevice);
        try {
            super.append(remoteDevice.getFriendlyName(false), null);
        } catch (IOException e) {
            super.append(remoteDevice.getBluetoothAddress(), null);
        }
    }

    public void commandAction(Command command, Displayable arg1) {
        if (command == back) {
            PongMIDlet.instance.setCurrentDisplayable(previous);
        } else {
            try {
                DeviceDiscoverer.getInstance().cancelInquiry();
            } catch (BluetoothStateException ex) {
                ex.printStackTrace();
            }
            
            try {
                int selectedIndex = this.getSelectedIndex();
                ServiceDiscoverer.getInstance().startSearch((RemoteDevice) remoteDevices.elementAt(selectedIndex), this);
            } catch (BluetoothStateException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void serviceSearchCompleted(RemoteDevice remoteDevice, ServiceRecord[] arg1) {
        try {
            ClientConfiguration clientConfiguration = new ClientConfiguration(arg1[0], this.gameCanvas);
            RFCOMMCommunicationFactory factory = new RFCOMMCommunicationFactory();
            ClientDevice clientDevice = factory.connectToServer(clientConfiguration);
            PongMIDlet.instance.setDevice(clientDevice);
            this.gameCanvas.initialize();
            clientDevice.startListening();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void deviceNotReachable() {
        System.out.println("deviceNotReachable");
    }

    public void serviceSearchError() {
        System.out.println("serviceSearchError");
    }
}
