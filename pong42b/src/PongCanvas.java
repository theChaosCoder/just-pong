
import java.io.IOException;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
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
    boolean moveup, movedown;
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
    private int tacHeight1 = screenHeight / 8;
    private int tacHeight2 = screenHeight / 8;
    private int tacWidth = screenWidth / 44;
    private int ballDiameter = Math.max(screenWidth, screenHeight) / 40;
    private int ballTopSpeed = screenWidth / 22; // ball can't cross the screen with less then 16 frames.
    private int ballMediumSpeed = screenWidth / 38;
    private int ballMinSpeed = screenWidth / 50;
    // ballTopSpeedY = ballspeedX/2 -- movement can't be too vertical.
    private int playerMove = tacHeight1 / 4; //player movement is about 1/4 of the dash size. or about 5% of the screen height.
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
    int itemXcenter;
    int itemYcenter;
    Image itemImage = null;
    Image bgImage = null;
    int itemAnimation = 0;
    int itemAniDelay = 0;
    int itemStatus = 0; // 0 keiner hat Item, 1 Player1 hat Item, 2 = Player2 hat Item eingesammelt.
    int itemTyp;
    boolean ballOwner = false; // wer hat zuletzt den Ball berührt. true = Player1, false = Player2.
    int itemDauer = 200;

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
        player1y = (screenWidth + tacHeight1) / 2;
        player2y = (screenWidth + tacHeight1) / 2;
        animation = -1;
        lastGoal = 0;
        //Item Timer wird alle x sek aufgerufen;
        itemTimer.scheduleAtFixedRate(itemTask, 0, 10000);


        // randomize to choose which side the ball will move first
        int coin = r.nextInt() % 1;
        if (coin > 0) {
            ballmx = ballMediumSpeed;
        } else {
            ballmx = -ballMediumSpeed;
        }
        // this randomize the Y movement of the ball
        resetY();

        try {
            bgImage = Image.createImage("/starBG.png");
        } catch (IOException e) {
        }
        try {
            itemImage = Image.createImage("/item.png");
        } catch (IOException e) {
        }



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
                    checkItem();
                    ItemMagic();
                    this.sendPosition();
                }
            }
            if (animation < 0) {
                if (isCPU) {
                    checkItem();
                    ItemMagic();
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
                if (player1y > (screenHeight - tacHeight1)) {
                    player1y = screenHeight - tacHeight1;
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
                if (player2y > (screenHeight - tacHeight2)) {
                    player2y = screenHeight - tacHeight2;
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
            } else if (player2y + tacHeight2 < ballBotom) {
                player2y += playerMove;
                if (player2y > (screenHeight - tacHeight2)) {
                    player2y = screenHeight - tacHeight2;
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
        if (ballLeft < tacWidth + 4) { // left of left player
            if (ballBotom > player1y && ballTop < player1y + tacHeight1) {
                // player1 rebates
                handleServingSpeed(1);
                //randomizeY();
                angle(1, player1y);
                ballOwner = true;
                if (SOUND) {
                    PlaySoundEffect(true, 0);
                }
            } else if (ballLeft < tacWidth - 2) {
                if (SOUND) {
                    PlaySoundEffect(true, 2);
                }
                score2++;
                lastGoal = 2;
                animation = 20; //20 frames to breath until the ball rolls again
            }
        } else if (ballRight > screenWidth - tacWidth - 4) {
            if (ballBotom > player2y && ballTop < player2y + tacHeight2) {
                // player2 rebates
                handleServingSpeed(2);
                //randomizeY();
                angle(2, player2y);
                ballOwner = false;
                if (SOUND) {
                    PlaySoundEffect(true, 0);
                }
            } else if (ballRight > screenWidth - tacWidth + 2) {
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

    private void checkItem() {
        if (itemStatus == 0) {
            if ((Math.abs(ballx - itemXcenter) < 18) && (Math.abs(bally - itemYcenter)) < 18) {
                //System.out.println("balxy: "+ballx + " " + bally);
                //System.out.println("itemxy: "+itemXcenter + " " + itemYcenter);

                if (ballOwner) {
                    itemStatus = 1;
                    switch (itemTyp) {
                        case 0:
                            tacHeight1 = (int) (tacHeight1 * 1.4);
                            break;

                    }
                } else {
                    itemStatus = 2;
                    switch (itemTyp) {
                        case 0:
                            tacHeight2 = (int) (tacHeight2 * 1.4);
                            break;
                    }
                }
            }
        }
    }

    private void ItemMagic() {

        if (itemStatus != 0) {

            if (itemDauer > 0) {
                itemDauer--;
            } else {
                itemDauer = 80;
                itemStatus = 0;
                if (ballOwner) {
                    itemStatus = 1;
                    switch (itemTyp) {
                        case 0:
                            tacHeight1 = screenHeight / 8;
                            break;

                    }
                } else {
                    itemStatus = 2;
                    switch (itemTyp) {
                        case 0:
                            tacHeight2 = screenHeight / 8;
                            break;
                    }
                }
            }
        }
    }

    /*   public void drawGraphics() { // NOT USED due a bug in K850i, see paint()
    // 1. black background (white maybe eye friendlier)
    //g.setColor(0xffeeeeee);
    g.setColor(0x000000);
    g.fillRect(0, 0, screenWidth + 1, screenHeight + 1);

    // Draw the middle line
    g.setColor(0xffeeeeee);
    g.drawLine((screenWidth / 2), 0, (screenWidth / 2), screenHeight);
    // 2. draw players
    g.setColor(0xffff0000);
    // Paddle1 wird größer?
    if (itemStatus == 1 && itemTyp == 0) {
    g.fillRect(4, player1y, (int) (tacWidth * 1.4), (int) (tacHeight * 1.4));
    if (itemDauer > 0) {
    itemDauer--;
    } else {
    itemDauer = 200;
    itemStatus = 0;
    }
    } else {
    g.fillRect(4, player1y, tacWidth, tacHeight);
    }

    g.setColor(0xff0000ff);
    // Paddle2 wird größer?
    if (itemStatus == 2 && itemTyp == 0) {
    g.fillRect(screenWidth - 8, player2y, (int) (tacWidth * 1.4), (int) (tacHeight * 1.4));
    if (itemDauer > 0) {
    itemDauer--;
    } else {
    itemDauer = 200;
    itemStatus = 0;
    }
    } else {
    g.fillRect(screenWidth - 8, player2y, tacWidth, tacHeight);
    }

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
    }*/
    public void paint(Graphics g) {
        // 1. black background (white maybe eye friendlier)
        // g.setColor(0xffeeeeee);
        // g.setColor(0x000000);
        // g.fillRect(0, 0, screenWidth + 1, screenHeight + 1);
        g.drawImage(bgImage, (screenHeight / 2) - ballx / 4, ((screenWidth / 2 + screenWidth / 4)) - bally / 4, Graphics.VCENTER | Graphics.HCENTER);

        // Draw the middle line
        g.setColor(0xffeeeeee);
        g.drawLine((screenWidth / 2), 0, (screenWidth / 2), screenHeight);
        // 2. draw players
        g.setColor(0xffff0000);
        g.fillRect(4, player1y, tacWidth, tacHeight1);
        g.setColor(0xff0000ff);
        // Paddle2 wird größer?
        if (itemStatus == 2 && itemTyp == 0) {
            g.fillRect(screenWidth - 8, player2y, tacWidth, (int) (tacHeight2));
            if (itemDauer > 0) {
                itemDauer--;
            } else {
                itemDauer = 200;
                itemStatus = 0;
            }
        } else {
            g.fillRect(screenWidth - 8, player2y, tacWidth, tacHeight2);
        }

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
                    bally = player1y + tacHeight1 / 2;
                    ballx = tacWidth + ballDiameter + 1;
                    resetY();
                } else {
                    handleServingSpeed(2);
                    bally = player2y + tacHeight2 / 2;
                    ballx = ((screenWidth - ballDiameter) - tacWidth) - 1;
                    resetY();
                }
            }
            animation--;
            //g.setColor(0xffff0000);
            g.setColor(0xffffffff); // weiss sieht man besser :P
            g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE));
            g.drawString(String.valueOf(score1), tacWidth + 8, player1y + tacHeight1 / 2 - 8, Graphics.TOP | Graphics.LEFT);
            //g.setColor(0xff0000ff);
            g.drawString(String.valueOf(score2), screenWidth - (tacWidth + 8), player2y + tacHeight2 / 2 - 8, Graphics.TOP | Graphics.RIGHT);
        }
        if (itemStatus == 0) {
            //Item paint
            if (itemAniDelay < 10) {
                itemAniDelay++;
            } else {
                if (itemAnimation == 48) {
                    itemAnimation = 0;
                } else {
                    itemAnimation += 16;
                }
                itemAniDelay = 0;
            }

            g.setClip(itemX, itemY, itemImage.getWidth() / 4, itemImage.getHeight());
            g.drawImage(itemImage, itemX - itemAnimation, itemY, Graphics.TOP | Graphics.LEFT);
        }
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
            if (isServer || isCPU) {
                if (itemStatus == 0) {
                    int abstandX = (int) (screenWidth * 0.2); //20% von der ScreenBreite
                    int abstandY = (int) (screenHeight * 0.1); //10% von der ScreenHöhe
                    itemX = 0;
                    itemY = 0;
                    boolean itemPosOK = false;
                    itemTyp = 0;//itemRand.nextInt() % 3; // 3 different Items
                    while (!itemPosOK) {
                        itemX = itemRand.nextInt() % screenWidth;
                        itemY = itemRand.nextInt() % screenHeight;
                        if ((itemX > 0 + abstandX) && (itemX < screenWidth - abstandX) && (itemY > 0 + abstandY) && (itemY < screenHeight - abstandY)) {
                            itemPosOK = true;
                            if (isServer) {
                                sendPosition();
                        }
                        }
                        itemXcenter = itemX + 8;
                        itemYcenter = itemY + 8;
                        
                    }
                }
            }
        }
    };

    public void sendPosition() {
        String msg;
        if (this.isServer) {
            msg = "p" + (player1y) + "b" + ballx + "x" + bally + "i" + itemX + "j" + itemX + "k" + itemStatus;
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
               // System.out.println(nextToken);

                if (this.isServer) {
                    player2y = Integer.parseInt(nextToken.substring(1));
                } else {
                    int indexOfBallxPosition = nextToken.indexOf("b");
                    int indexOfBallyPosition = nextToken.indexOf("x");
                    int indexOfItemXPosition = nextToken.indexOf("i");
                    int indexOfItemYPosition = nextToken.indexOf("j");
                    int indexOfItemStatusPosition = nextToken.indexOf("k");
                    
                    player1y = Integer.parseInt(nextToken.substring(1, indexOfBallxPosition));
                    ballx = Integer.parseInt(nextToken.substring(indexOfBallxPosition+1, indexOfBallyPosition));
                    bally = Integer.parseInt(nextToken.substring(indexOfBallyPosition+1, indexOfItemXPosition));
                    itemX = Integer.parseInt(nextToken.substring(indexOfItemXPosition+1, indexOfItemYPosition));
                    itemY = Integer.parseInt(nextToken.substring(indexOfItemYPosition+1, indexOfItemStatusPosition));
                    itemStatus = Integer.parseInt(nextToken.substring(indexOfItemStatusPosition+1, indexOfItemStatusPosition+2));
                    System.out.println(itemStatus);
                    /*int[] values = separeValues(nextToken.substring(indexOfBallPosition + 1), "x");
                    ballx = values[0];
                    bally = values[1];
                    int indexOfItemPosition = nextToken.indexOf("i");
                    values = separeValues(nextToken.substring(indexOfItemPosition + 1), "j");
                    itemXcenter = values[0];
                    itemYcenter = values[1];
                    //values = separeValues(nextToken.substring(indexOfItemStatusPosition + 1), "l");
                    //itemStatus = values[0];*/
                    this.repaint();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            firstIndex = lastIndex + 1;
        }

    }
        //public int indexOf(String str)
        //Liefert die erste Position, an der str vollständig im String enthalten ist. Ist str nicht im String enthalten, wird -1 geliefert.

    public static int[] separeValues(String str, String sep) {
        int values[] = new int[2];
        int separatorIndex = str.indexOf(sep);
        values[0] = Integer.parseInt(str.substring(0, separatorIndex));
        values[1] = Integer.parseInt(str.substring(separatorIndex + 1));
        return values;
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
        //System.out.println("sound: "+ (getKeyStates() & GameCanvas.KEY_STAR));
        if ((this.getKeyStates()) == 4096) { // 4096 = 9 Taste
            //if ((getKeyStates() & GameCanvas.KEY_STAR) != 0) { // 4096 = 9 Taste

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
