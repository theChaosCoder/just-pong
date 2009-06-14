/*
 * A Simple Pong Game written by Leszek Lesner
 * released under the terms of GPL (later maybe BeerLicense) :P
 */

import java.io.IOException;

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

public class myMidlet extends MIDlet implements CommandListener {

	private final Pong game = new Pong(this);
	private List menu;
	private StringItem dialogText;
	private Form dialogForm;
	private Command dialogExit;
	private int lastMenuSelection = 0;

	public myMidlet() {
		super();
	}

	/**
	 *  show splash screen
	 */
	private class splashCanvas extends Canvas{
		protected void paint(Graphics g) {
			Image sImage = null;
			try{
				g.setColor(0,0,0);
				g.fillRect(0,0,getWidth(),getHeight());
                
				try{
					sImage = Image.createImage( "/splash.png" );
				}catch( IOException e ){}
				if( sImage != null )
					g.drawImage( sImage, getWidth()/2, getHeight()/2, Graphics.VCENTER | Graphics.HCENTER );

				g.setColor(255,0,0);
				g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,Font.SIZE_LARGE));
				g.drawString("Pong42", getWidth()-1, 1, Graphics.RIGHT| Graphics.TOP);
				g.setColor(255,255,255);
				g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN,Font.SIZE_SMALL));
				g.drawString("Released under the terms of GPL",1, getHeight()-1, Graphics.LEFT | Graphics.BOTTOM );
			}finally{
				sImage = null;
			}
		}
	}

	protected void startApp() throws MIDletStateChangeException {
		splashCanvas splash = null;
		try{
			splash = new splashCanvas();
			Display.getDisplay(this).setCurrent( splash );
		}finally{
			splash = null;
		}
		long loopStartTime = System.currentTimeMillis();
		int loopDelay = 2000;
		while( true ){
			if( System.currentTimeMillis()- loopStartTime > loopDelay ) break;
		}
		// End Of splash screen
		if( game.isRuning() ){
			Display.getDisplay(this).setCurrent(game);
		}else{
			showMenu();
		}
	}
	public void showMenu(){
		menu = new List("Pong42", Choice.IMPLICIT);
		if( game.isStarted() ){
			menu.append("Spiel fortsetzen", null);
		}else{
			menu.append("2 Spieler Spiel starten", null);
		}
		menu.append("Einzelspiel starten", null);
		menu.append("Hilfe", null);
		menu.append("Über", null);
		menu.setSelectedIndex(lastMenuSelection, true);
		menu.setCommandListener(this);
		Display.getDisplay(this).setCurrent(menu);
	}

	/**
	 * Starts a new game, or resumes a paused game.
	 */
	private void startGame(){
		if( game.isStarted() == false )
			game.start(false); // false = not pratice mode
		Display.getDisplay(this).setCurrent(game);
	}
	/**
	 * starts a new game in pratice mode.
	 * kills any running game that was in background
	 */
	private void startPratice(){
		game.start(true); // true = pratice mode
		Display.getDisplay(this).setCurrent(game);
	}
	/**
	 * shows a dialog with text. 
	 * @param textType what to show. values: [Hilfe|Über]. defaults to Über
	 */
	private void showDialog( String textType ){
		dialogForm = new Form(textType);
		dialogText = new StringItem("","");
		dialogExit = new Command("Back", Command.BACK, 1);
		dialogForm.append(dialogText);
		dialogForm.addCommand(dialogExit);
		dialogForm.setCommandListener(this);
		if( textType == "Hilfe")
			dialogText.setLabel("Left player keys: [1]up, [7]down.\n"+
					"Right player keys: [3]up, [9]down.\n\n"+
					"Win points by making goals on your oponent side.\nThe better player will rebate ball slower over time while the player with less points will rebate them faster.\n\n"+
					"On pratice mode, you control the Left piece, the phone controls the right one.\n\n"+
					"KNOWN ISSUES:\nOn sone phones (e.g Nokia6820) the keys may change (e.g. [9]and[#] instead of [3]and[9]).\n\n"+
					"Some models have a problem that they cannot process more then one key at a time. One players should be polite and release the key when not defending.\n"+
					"Some models (e.g. 6820) have work-arounds, for example, it doesn't accepet two keys being pressed while closed, but if the flip is open, up to four keys can be pressed at the same time. Try out your model.");
		else
			dialogText.setLabel("Pong42 programmiert von Anatolij and Leszek\n\n"+
					"Werbung muss sein ;) http://www.zevenos.com :P/\n\n");
		Display.getDisplay(this).setCurrent(dialogForm);
	}

	protected void pauseApp(){
		game.pause();
	}

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		// TODO Auto-generated method stub
	}

	/* Deal with any button being pressed on the game menu.
	 */
	public void commandAction(Command cmd, Displayable displayable) {
		if( cmd == dialogExit ){
			showMenu();
		}else{
			lastMenuSelection = menu.getSelectedIndex();
			switch(lastMenuSelection) {
			case 0: startGame();break;
			case 1: startPratice();break;
			case 2: showDialog("Hilfe");break;
			case 3: showDialog("Über");break;
			}
		}
	}
}
