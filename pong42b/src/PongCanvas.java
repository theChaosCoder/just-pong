
import java.io.IOException;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

import net.java.dev.marge.communication.CommunicationListener;

public class PongCanvas extends GameCanvas implements
        CommunicationListener, Runnable, CommandListener {

    private boolean isServer;
    private boolean isCPU = false;
    //private Sprite server,  client,  ball,  background;
    //private int xback,  yback;
    boolean moveup, movedown;
    private Sprite me;
    private Graphics g = getGraphics();
    private Random r = new Random();
    private Command back;
    private Command pause;
    private Command exit;
    private Player midiPlayer = null;
    private boolean SOUND = false;
    private int screenWidth = getWidth();
    private int screenHeight = getHeight() - 1;
    private int ballTop;
    private int ballRight;
    private int ballBotom;
    private int ballLeft;
    // options
    public boolean practice = true; // single player?
    // semaphores
    private int animation = -1;
    private int lastGoal;
    private int tacHeight = screenHeight / 8;
    private int tacWidth = screenWidth / 48;
    private int ballDiameter = Math.max(screenWidth, screenHeight) / 40;
    private int ballTopSpeed = screenWidth / 22; // ball can't cross the screen with less then 16 frames.
    private int ballMediumSpeed = screenWidth / 36;
    private int ballMinSpeed = screenWidth / 50;
    // ballTopSpeedY = ballspeedX/2 -- movement can't be too vertical.
    private int playerMove = tacHeight / 4; //player movement is about 1/4 of the dash size. or about 5% of the screen height.
    // positions
    public int player1y;
    public int player2y;
    public int ballx;
    public int bally;
    private int ballmx = 0;
    private int ballmy;
    // scores
    private int score1;
    private int score2;

    //Items
    Timer itemTimer = new Timer();
    Random itemRand = new Random();
    public int item;
    int itemX;
    int itemY;

    public PongCanvas() {
        super(false);
    }
    
    private void start() {
        // reset scores
        score1 = 0;
        score2 = 0;
        //center the ball
        ballx = screenWidth / 2;
        bally = screenHeight / 2;
        // center players
        player1y = (screenWidth + tacHeight) / 2;
        player2y = (screenWidth + tacHeight) / 2;
        animation = -1;
        lastGoal = 0;
        //Item Timer wird alle x sek aufgerufen;
        itemTimer.scheduleAtFixedRate(itemTask, 0, 3000);

        // randomize to choose which side the ball will move first
        int coin = r.nextInt() % 1;
        if (coin > 0) {
            ballmx = ballMediumSpeed;
        } else {
            ballmx = -ballMediumSpeed;
        }
        // this randomize the Y movement of the ball
        resetY();
    }

    protected void keyPressed(int key) {
        super.keyPressed(key);
    }

    public void setIsServer(boolean isServer) {
        this.isServer = isServer;
    }

    public void setIsCPU(boolean cpu) {
        this.isCPU = cpu;
    }

    public void initialize() {
        this.start();
        PongMIDlet.instance.setCurrentDisplayable(this);
        Thread t = new Thread(this);
        t.start();
        //back = new Command("Menü", Command.BACK, 1);
        //addCommand(back);
        exit = new Command("exit", Command.BACK, 1);
        addCommand(exit);
        //pause = new Command("Pause", Command.OK, 2);
        //addCommand(pause);
        setCommandListener(this);
    }

    public void run() {
        while (true) {
            checkSound();
            if (isServer) {
                if (animation < 0) {
                    moveBall();
                    this.sendPosition();
                }
            }
            if (animation < 0) {
                if (isCPU) {
                    moveBall();
                }
                movePlayer();
            }
            //drawGraphics();
            repaint();
            //serviceRepaints() sorgt dafür das repaint() auch wirklich direkt
            //ausgeführt wird, Thread.yield()gibt der Anwedung "Luft zu atmen", das
            //ist bei älteren Geräten hilfreich um Tastaturschläge besser zu verarbeiten.
            //Es entstehen keine Performance einbußen.
            serviceRepaints();
            Thread.yield();
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
            }
        }
    }

    public void movePlayer() {
        // INPUT
        int key = getKeyStates();
        //System.out.println("bool: " + isServer);
        if (isCPU || isServer) { // ServerPlayer or 1.Player move
            if ((key & GameCanvas.UP_PRESSED) != 0) { // left up
                player1y -= playerMove;

                if (player1y < 0) {
                    player1y = 0;
                }
            } else if ((key & GameCanvas.DOWN_PRESSED) != 0) { // left down
                player1y += playerMove;
                if (player1y > (screenHeight - tacHeight)) {
                    player1y = screenHeight - tacHeight;
                }
            }
        } else if (!isServer && !isCPU) { // ClientPlayer move
            if ((key & GameCanvas.UP_PRESSED) != 0) { // left up
                player2y -= playerMove;

                if (player2y < 0) {
                    player2y = 0;
                }
            } else if ((key & GameCanvas.DOWN_PRESSED) != 0) { // left down
                player2y += playerMove;
                if (player2y > (screenHeight - tacHeight)) {
                    player2y = screenHeight - tacHeight;
                }
            }
        }
        if (!isCPU) {
            this.sendPosition();
        }
        // MOVE CPU (only in vs. CPU Mode)
        if (isCPU) {
            if (player2y > ballTop) {
                player2y -= playerMove;
                if (player2y < 0) {
                    player2y = 0;
                }
            } else if (player2y + tacHeight < ballBotom) {
                player2y += playerMove;
                if (player2y > (screenHeight - tacHeight)) {
                    player2y = screenHeight - tacHeight;
                }
            }
        }
    }

    public void moveBall() {
        //1. move ball
        ballx += ballmx;
        bally += ballmy;
        ballTop = bally;
        ballRight = ballx + ballDiameter;
        ballBotom = bally + ballDiameter;
        ballLeft = ballx;
        // 2. (Kollision)
        // 2.a (mit dem Paddle)
        // 2.b (mit dem Spielrand)
        if (ballLeft < tacWidth) { // left of left player
            if (ballBotom > player1y && ballTop < player1y + tacHeight) {
                // player1 rebates
                handleServingSpeed(1);
                //randomizeY();
                angle(1, player1y);
                if (SOUND) {
                    PlaySoundEffect(true, 0);
                }
            } else {
                if (SOUND) {
                    PlaySoundEffect(true, 2);
                }
                score2++;
                lastGoal = 2;
                animation = 20; //20 frames to breath until the ball rolls again
            }
        } else if (ballRight > screenWidth - tacWidth) {
            if (ballBotom > player2y && ballTop < player2y + tacHeight) {
                // player2 rebates
                handleServingSpeed(2);
                //randomizeY();
                angle(2, player2y);
                if (SOUND) {
                    PlaySoundEffect(true, 0);
                }
            } else {
                if (SOUND) {
                    PlaySoundEffect(true, 2);
                }
                score1++;
                lastGoal = 1;
                animation = 20;
            }
        }
        // 3. with top/bottom
        if ( // hit top and moving up
                (ballTop < 1 && ballmy < 0) || // hit bottom and moving down
                (ballBotom > screenHeight && ballmy > 0)) {
            ballmy *= -1;
            if (SOUND) {
                PlaySoundEffect(true, 1);
            }
        }




    }

    public void drawGraphics() { // NOT USED due a bug in K850i, see paint()
        // 1. black background (white maybe eye friendlier)
        //g.setColor(0xffeeeeee);
        g.setColor(0x000000);
        g.fillRect(0, 0, screenWidth + 1, screenHeight + 1);

        // Draw the middle line
        g.setColor(0xffeeeeee);
        g.drawLine((screenWidth / 2), 0, (screenWidth / 2), screenHeight);
        // 2. draw players
        g.setColor(0xffff0000);
        g.fillRect(0, player1y, tacWidth, tacHeight);
        g.setColor(0xff0000ff);
        g.fillRect(screenWidth - 8, player2y, tacWidth, tacHeight);
        // 3. draw ball if it's not flashing
        if (animation < 0) {
            //g.setColor(0xff000000);
            g.setColor(0xffeeeeee);
            g.fillArc(ballx, bally, ballDiameter, ballDiameter, 0, 360);
        } else if (animation % 2 == 0) { // pinta bola vermelho em frames pares da animacao de gol
            g.setColor(0xffffcccc);
            g.fillArc(ballx, bally, ballDiameter, ballDiameter, 0, 360);
        }
        // paint score if its in goal animation
        if (animation > -1) {
            if (0 == animation) {
                // stop the animation, put the ball back in move
                if (1 == lastGoal) {
                    handleServingSpeed(1);
                    bally = player1y + tacHeight / 2;
                    ballx = tacWidth + ballDiameter + 1;
                    resetY();
                } else {
                    handleServingSpeed(2);
                    bally = player2y + tacHeight / 2;
                    ballx = ((screenWidth - ballDiameter) - tacWidth) - 1;
                    resetY();
                }
            }
            animation--;
            g.setColor(0xffff0000);
            g.drawString(String.valueOf(score1), tacWidth + 2, player1y + tacHeight / 2, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xff0000ff);
            g.drawString(String.valueOf(score2), screenWidth - (tacWidth + 2), player2y + tacHeight / 2, Graphics.TOP | Graphics.RIGHT);
        }
        //Item paint test
        g.setColor(100, 100, 100);
        g.fillArc(itemX, itemY, (int) (ballDiameter * 1.3), (int) (ballDiameter * 1.3), 0, 360);
    }

    public void paint(Graphics g) {
        // 1. black background (white maybe eye friendlier)
        //g.setColor(0xffeeeeee);
        g.setColor(0x000000);
        g.fillRect(0, 0, screenWidth + 1, screenHeight + 1);

        // Draw the middle line
        g.setColor(0xffeeeeee);
        g.drawLine((screenWidth / 2), 0, (screenWidth / 2), screenHeight);
        // 2. draw players
        g.setColor(0xffff0000);
        g.fillRect(0, player1y, tacWidth, tacHeight);
        g.setColor(0xff0000ff);
        g.fillRect(screenWidth - 8, player2y, tacWidth, tacHeight);
        // 3. draw ball if it's not flashing
        if (animation < 0) {
            //g.setColor(0xff000000);
            g.setColor(0xffeeeeee);
            g.fillArc(ballx, bally, ballDiameter, ballDiameter, 0, 360);
        } else if (animation % 2 == 0) { // pinta bola vermelho em frames pares da animacao de gol
            g.setColor(0xffffcccc);
            g.fillArc(ballx, bally, ballDiameter, ballDiameter, 0, 360);
        }
        // paint score if its in goal animation
        if (animation > -1) {
            if (0 == animation) {
                // stop the animation, put the ball back in move
                if (1 == lastGoal) {
                    handleServingSpeed(1);
                    bally = player1y + tacHeight / 2;
                    ballx = tacWidth + ballDiameter + 1;
                    resetY();
                } else {
                    handleServingSpeed(2);
                    bally = player2y + tacHeight / 2;
                    ballx = ((screenWidth - ballDiameter) - tacWidth) - 1;
                    resetY();
                }
            }
            animation--;
            g.setColor(0xffff0000);
            g.drawString(String.valueOf(score1), tacWidth + 2, player1y + tacHeight / 2, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xff0000ff);
            g.drawString(String.valueOf(score2), screenWidth - (tacWidth + 2), player2y + tacHeight / 2, Graphics.TOP | Graphics.RIGHT);
        }
        //Item paint test
        g.setColor(100, 100, 100);
        g.fillArc(itemX, itemY,(int) (ballDiameter * 1.4),(int) (ballDiameter * 1.4), 0, 360);
        //g.drawString(String.valueOf(screenHeight), screenWidth / 2, screenHeight / 2, Graphics.TOP | Graphics.RIGHT);
    }

    /**
     * Winkelberechnung für ballmy, je höher, bzw. niedriger der Wert von ballmy ist,
     * desto steiler ist der Abprallwinkel.
     */
    private void angle(int player, int playerPos) {
        playerPos = playerPos + 12; // da player1y, bzw. player2y nicht die Mitte ist, warum auch immer :/
        int ballAngle = playerPos - bally;
        double ballMaxAngle = 0.45;
        //System.out.println("ballX: " + ballAngle);
        //System.out.println("ballY: " + ballmy);
        if (ballAngle > 0) {
            ballmy = (int) (ballAngle * ballMaxAngle) * -1;
        } else {
            ballmy = (int) (ballAngle * ballMaxAngle) * -1;
        }

    //System.out.println("schuss: " + ballmy);
    //Der untere code erzeugt immer den selben Winkel, könnte für nen "easy Mode"
    //oder eingesetzt werden, bzw wenn der Gegner zuviele Punke hat. ;-)
    /* if (ballmx > 0 && ballmy > 0) {
    if (ballmy > ballmx / 2) {
    ballmy = ballmx / 2;
    }
    } else if (ballmx > 0 && ballmy < 0) {
    if (-ballmy > ballmx / 2) {
    ballmy = -ballmx / 2;
    }
    } else if (ballmx < 0 && ballmy > 0) {
    if (ballmy > -ballmx / 2) {
    ballmy = -ballmx / 2;
    }
    } else {
    if (ballmy < ballmx / 2) {
    ballmy = ballmx / 2;
    }
    }*/
    //System.out.println("schuss: " + ballmy);
    }

    private void handleServingSpeed(int player) {
        if (1 == player) {
            // simple mode OFF; ballmx *= -1;

            // complex mode OFF;  no point if we gona use only INTs;
            //ballmx *= (-1 * (score2/(score1+1)));
            //if( ballmx < ballMinSpeed ) ballmx = ballMinSpeed;
            //else if( ballmx > ballTopSpeed ) ballmx = ballTopSpeed;

            // medium mode ON :) constants whore
            if (score2 - score1 > 3) { // if 4 points bellow, serve furiously
                ballmx = ballTopSpeed;
            } else if (score2 - score1 < -4) { // if 5 points above, serve slow
                ballmx = ballMinSpeed;
            } else {
                ballmx = ballMediumSpeed;
            }

        } else {
            if (score1 - score2 > 3) {
                ballmx = -ballTopSpeed;
            } else if (score1 - score2 < -5) {
                ballmx = -ballMinSpeed;
            } else {
                ballmx = -ballMediumSpeed;
            }

        }
    }

    private void resetY() {
        int coin = r.nextInt() % 4;
        if (coin > 3) {
            ballmy = ballmx / 2;
        } else if (coin > 2) {
            ballmy = ballmx / 4;
        } else if (coin > 1) {
            ballmy = -ballmx / 2;
        } else {
            ballmy = -ballmx / 4;
        }

    }
    TimerTask itemTask = new TimerTask() {

        public void run() {
            int abstandX = (int) (screenWidth * 0.2); //20% von der ScreenBreite
            int abstandY = (int) (screenHeight * 0.1); //10% von der ScreenHöhe
            itemX =
                    0;
            itemY =
                    0;
            boolean itemPosOK = false;
            while (!itemPosOK) {
                itemX = itemRand.nextInt() % screenWidth;
                itemY =
                        itemRand.nextInt() % screenHeight;
                if ((itemX > 0 + abstandX) && (itemX < screenWidth - abstandX) && (itemY > 0 + abstandY) && (itemY < screenHeight - abstandY)) {
                    itemPosOK = true;
                }

            }
        //    Random random = new Random();
        //   actX = 0;
        //  while (actX < 42) {
        //     actX = random.nextInt() % getWidth();
        //}
        //System.out.println("Timer: ");
        }
    };

    public void sendPosition() {
        String msg;
        if (this.isServer) {
            msg = "p" + (player1y) + "b" + ballx + "x" + bally;
        } else {
            msg = "p" + player2y;
        }
        msg += ";";
        PongMIDlet.instance.getDevice().send(msg.getBytes());


    }

    public void receiveMessage(byte[] b) {
        String msg = new String(b);
        int firstIndex = 0;
        int lastIndex = 0;
        while ((lastIndex = msg.indexOf(";", firstIndex)) != -1) {

            try {

                String nextToken = msg.substring(firstIndex, lastIndex);

                if (this.isServer) {
                    player2y = Integer.parseInt(nextToken.substring(1));
                } else {
                    int indexOfBallPosition = nextToken.indexOf("b");
                    player1y = Integer.parseInt(nextToken.substring(1, indexOfBallPosition));
                    int[] values = ImageUtil.separeValues(nextToken.substring(indexOfBallPosition + 1), "x");
                    ballx = values[0];
                    bally = values[1];
                    this.repaint();
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

    public void commandAction(Command cmd, Displayable d) {
        if (cmd == exit) {
            PongMIDlet.instance.notifyDestroyed();
        }
    /*if (cmd == pause) {
    if (cmd.getLabel() == "Pause") { // stringvergleich nicht optimal, funzt aber :P
    pause();
    } else {
    resume();
    }
    } else if (cmd == back) {
    pause();
    //System.out.println("bla");
    PongMIDlet.instance.setCurrentDisplayable(new PongMenu());
    }*/
    }

    /**
     * pause the game. NOT USED
     */
    public void pause() {
        removeCommand(pause);
        //thread = null;
        pause = new Command("Resume", Command.OK, 2);
        addCommand(pause);
    }

    /**
     * resume a paused game. NOT USED
     */
    public void resume() {
        removeCommand(pause);
        //thread = new Thread(this);
        //thread.start();
        pause = new Command("Pause", Command.OK, 2);
        addCommand(pause);
    }

    private void checkSound() {
        //System.out.println("sound: "+GameCanvas.KEY_STAR);
        //keyPressed(KEY_STAR);
        System.out.println("sound: "+ KEY_STAR);
        if (( GameCanvas) != 0) {
            
            if (SOUND) {
                SOUND = false;
            } else {
                SOUND = true;
            }
        }
    }

    public void PlaySoundEffect(boolean p, int song) {
        // Sound Effects
        if (p) {
            if (song == 0) {
                try {
                    midiPlayer = Manager.createPlayer(getClass().getResourceAsStream("/beep-7.wav"), "audio/x-wav");
                    midiPlayer.realize(); //Vorbereiten
                    midiPlayer.prefetch(); //Laden
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
            if (song == 1) {
                try {
                    midiPlayer = Manager.createPlayer(getClass().getResourceAsStream("/beep-8.wav"), "audio/x-wav");
                    midiPlayer.realize(); //Vorbereiten
                    midiPlayer.prefetch(); //Laden
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
            if (song == 2) {
                try {
                    midiPlayer = Manager.createPlayer(getClass().getResourceAsStream("/beep-10.wav"), "audio/x-wav");
                    midiPlayer.realize(); //Vorbereiten
                    midiPlayer.prefetch(); //Laden
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
}
