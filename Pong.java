/*
 * A Simple Pong Game written by Leszek Lesner
 * released under the terms of GPL (later maybe BeerLicense) :P
 */

import java.io.IOException;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;
import java.util.Random;
import javax.microedition.lcdui.Image;

/**
 * @author Leszek Lesner
 *
 */
public class Pong extends GameCanvas implements Runnable, CommandListener {
	// plumbing
	private myMidlet parent; 
	private Thread thread = null;
	private Graphics g = getGraphics();
	private Random r = new Random();
	private Command back;
	private Command pause;
	private int screenWidth = getWidth()-1;
	private int screenHeight = getHeight()-1;
	private int ballTop;
	private int ballRight;
	private int ballBotom;
	private int ballLeft;
	// options
	public boolean pratice = false; // single player?
	// semaphores
	private int animation = -1;
	private int lastGoal;
	private int tacHeight = screenHeight/5;
	private int tacWidth = screenWidth/24;
	private int ballDiameter = Math.max(screenWidth, screenHeight)/20;
	private int ballTopSpeed = screenWidth/16; // ball can't cross the screen with less then 16 frames.
	private int ballMediuSpeed = screenWidth/32;
	private int ballMinSpeed = screenWidth/50;
	// ballTopSpeedY = ballspeedX/2 -- movement can't be too vertical.
	private int playerMove = tacHeight/4; //player movement is about 1/4 of the dash size. or about 5% of the screen height.
	// positions
	private int player1y;
	private int player2y;
	private int ballx;
	private int bally;
	private int ballmx = 0;
	private int ballmy;
	// scores
	private int score1;
	private int score2;
	


	public Pong(myMidlet p ){ this(false, p); }
	public Pong(boolean arg0, myMidlet p) {     // NOTE: Error if not there
		super(arg0);
		parent = p;
		back = new Command("Menü", Command.BACK, 1);addCommand(back);
		pause = new Command("Pause", Command.OK, 2);addCommand(pause);
		setCommandListener(this);
	}
	public void commandAction(Command cmd, Displayable displayable) {
		if( cmd == pause ){
			if( cmd.getLabel() == "Pause" )
				pause();
			else
				resume();
		}else if( cmd == back ){
			pause();
			parent.showMenu();
		}
	}
	
	/**
	 * Start a new game. reset all the values and kick off the thread 
	 */
	public void start(boolean praticeMode){
		// reset scores
		score1 = 0;
		score2=0;
		//center the ball
		ballx = screenWidth/2;
		bally = screenHeight/2;
		// center players
		player1y = screenWidth/2-tacHeight/2;
		player2y = screenWidth/2-tacHeight/2;
		animation = -1;
		lastGoal = 0;
		// randomize to choose which side the ball will move first
		int coin = r.nextInt() % 1;
		if( coin > 0 )
			ballmx = ballMediuSpeed;
		else
			ballmx = -ballMediuSpeed;
		// this randomize the Y movement of the ball
		resetY();
		// set the pratice mode status
		pratice = praticeMode;
		// start();
		resume(); // since pause() kills the thread, resume() starts it.
	}
	/**
	 * is there a current game actualy running?
	 * @return boolean
	 */
	public boolean isRuning(){
		return (thread != null);
	}
	/**
	 * is there a current game already started?
	 * @return boolean true=game is in progress, false=no game in progress
	 */
	public boolean isStarted(){
		if( (ballmx != 0 || animation > -1) && pratice == false ) return true;
		return false;
	}
	/**
	 * pause the game.
	 * kills the thread and change the "pause" button to "resume"
	 */
	public void pause(){
		removeCommand(pause);
		thread = null;
		pause = new Command("Weiter", Command.OK, 2);addCommand(pause);
	}
	/**
	 * resume a paused game
	 * or start the thread for the first time
	 */
	public void resume(){
		removeCommand(pause);
		thread = new Thread(this);
		thread.start();
		pause = new Command("Pause", Command.OK, 2);addCommand(pause);
	}
	/**
	 * stops execution
	 */
	public void end(){
		thread = null;
		ballmx = 0;
	}
	
	/* the game loop
	 * 
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while( true ){
		
		
		if( animation < 0 ){ 
			// INPUT
			int key = getKeyStates();
            if( (key & GameCanvas.UP_PRESSED) != 0 ){ // left up
				player1y -= playerMove;
				if( player1y < 0 ) player1y = 0;
			}else if( (key & GameCanvas.DOWN_PRESSED) != 0 ){ // left down
				player1y += playerMove;
				if( player1y > (screenHeight-tacHeight) ) player1y = screenHeight-tacHeight;
			}
			if( pratice == true ){ // single player
				if( player2y > ballTop ){
					player2y -= playerMove;
					if( player2y < 0 ) player2y = 0;
				}else if( player2y + tacHeight < ballBotom ){				
					player2y += playerMove;
					if( player2y > (screenHeight-tacHeight) ) player2y = screenHeight-tacHeight;
				}
			}else{
				if( (key & GameCanvas.GAME_B_PRESSED) != 0 ){ // right up
					player2y -= playerMove;
					if( player2y < 0 ) player2y = 0;
				}else if( (key & GameCanvas.GAME_D_PRESSED) != 0 ){ //  right down
					player2y += playerMove;
					if( player2y > (screenHeight-tacHeight) ) player2y = screenHeight-tacHeight;
				}
			}
			// LOGIC
			//1. move ball
			ballx += ballmx;
			bally += ballmy;
			ballTop = bally;
			ballRight = ballx + ballDiameter;
			ballBotom = bally + ballDiameter;
			ballLeft = ballx;
			// 2. colision?
			// 2.a with player
			// 2.b with margin (goal)
			if( ballLeft < tacWidth ){ // left of left player
				if( ballBotom > player1y && ballTop < player1y+tacHeight){
					// player1 rebates
					handleServingSpeed(1);
					randomizeY();
				}else{
					score2++;
					lastGoal = 2;
					animation = 20; //20 frames to breath until the ball rolls again
				}
			}else if( ballRight > screenWidth-tacWidth){
				if( ballBotom > player2y && ballTop < player2y+tacHeight){
					// player2 rebates
					handleServingSpeed(2);
					randomizeY();
				}else{
					score1++;
					lastGoal = 1;
					animation = 20;
				}
			}
			// 3. with top/bottom
			if( // hit top and moving up
				( ballTop < 1 && ballmy < 0 )
			||  // hit bottom and moving down
				( ballBotom > screenHeight && ballmy > 0 )
			){ ballmy *= -1; }
		}
		
		// GRAPHICS
		// 1. black background (white maybe eye friendlier)
		//g.setColor(0xffeeeeee);
        g.setColor(0x000000);
		g.fillRect(0,0, screenWidth+1, screenHeight+1); 
		
		
		// 2. draw players
		g.setColor(0xffff0000);
		g.fillRect(0,player1y, tacWidth, tacHeight);
		g.setColor(0xff0000ff);
		g.fillRect(screenWidth-8,player2y, tacWidth, tacHeight);
		// 3. draw ball if it's not flashing
		if( animation < 0 ){
			//g.setColor(0xff000000);
			g.setColor(0xffeeeeee);
            g.fillArc(ballx, bally, ballDiameter, ballDiameter, 0,360);
		}else if( animation % 2 == 0 ){ // pinta bola vermelho em frames pares da animacao de gol
			g.setColor(0xffffcccc);
			g.fillArc(ballx, bally, ballDiameter, ballDiameter, 0,360);			
		}
		// paint score if its in goal animation
		// TODO: use a custom pixel font method to draw the scores rotated.
		if( animation > -1 ){
			if( 0 == animation ){
				// stop the animation, put the ball back in move
				if( 1 == lastGoal ){
					handleServingSpeed(1);
					bally = player1y + tacHeight/2;
					ballx = tacWidth + ballDiameter + 1;
					resetY();
				}else{
					handleServingSpeed(2);
					bally = player2y + tacHeight/2;
					ballx = ((screenWidth - ballDiameter) - tacWidth) - 1;
					resetY();
				}
			}
			animation--;
			g.setColor(0xffff0000);
			g.drawString( String.valueOf(score1), tacWidth + 2, player1y + tacHeight/2, Graphics.TOP | Graphics.LEFT );
			g.setColor(0xff0000ff);
			g.drawString( String.valueOf(score2), screenWidth - (tacWidth + 2), player2y+ tacHeight/2, Graphics.TOP | Graphics.RIGHT );
		}
		repaint();
		try{
			Thread.sleep(30);
			//thread.sleep(20);
		}catch(Exception e) {}
		if( isRuning() == false ) return; 
		}
	}

    protected void keyPressed(int key) {
        if (key == UP) {
            player1y -= playerMove;
			if( player1y < 0 ) player1y = 0;
        } else if (key == UP_PRESSED) {
            player1y += playerMove;
			if( player1y > (screenHeight-tacHeight) ) player1y = screenHeight-tacHeight;
        }
        super.keyPressed(key);
    }

	private void randomizeY(){
		int coin = r.nextInt() % 2;
		if( coin < 0 ){ // decrease angle
			if( ballmy > 0 ) ballmy--;
			else ballmy++;
		}else if( coin > 0){ // increase angle
			if( ballmy > 0 ) ballmy++;
			else ballmy--;
		}
		// TODO: clean up this garbage.
		// TODO: see what else i submited in this 3am commit...
		if( ballmx > 0 && ballmy > 0){
			if( ballmy > ballmx/2 ){ ballmy = ballmx/2;}
		}else if( ballmx > 0 && ballmy < 0){ 
			if( -ballmy > ballmx/2 ){ ballmy = -ballmx/2;}
		}else if( ballmx < 0 && ballmy > 0){ 
			if( ballmy > -ballmx/2 ){ ballmy = -ballmx/2;}
		}else{
			if( ballmy < ballmx/2 ){ ballmy = ballmx/2;}
		}
				
	}
	private void handleServingSpeed(int player){
		if( 1 == player ){
			// simple mode OFF; ballmx *= -1;

			// complex mode OFF;  no point if we gona use only INTs;
			//ballmx *= (-1 * (score2/(score1+1)));
			//if( ballmx < ballMinSpeed ) ballmx = ballMinSpeed;
			//else if( ballmx > ballTopSpeed ) ballmx = ballTopSpeed;

			// medium mode ON :) constants whore
			if( score2 - score1 > 3){ // if 4 points bellow, serve furiously
				ballmx = ballTopSpeed;
			}else if( score2 - score1 < -4 ){ // if 5 points above, serve slow
				ballmx = ballMinSpeed;
			}else{
				ballmx = ballMediuSpeed;
			}
		}else{		
			if( score1 - score2 > 3){
				ballmx = -ballTopSpeed;
			}else if( score1 - score2 < -5 ){
				ballmx = -ballMinSpeed;
			}else{
				ballmx = -ballMediuSpeed;
			}
		}		
	}
	private void resetY(){
		int coin = r.nextInt() % 4;
		if( coin > 3 )
			ballmy = ballmx/2;
		else if( coin > 2)
			ballmy = ballmx/4;
		else if( coin > 1)
			ballmy = -ballmx/2;
		else
			ballmy = -ballmx/4;
	}
}
