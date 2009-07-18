

import java.io.IOException;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.Sprite;

import net.java.dev.marge.communication.CommunicationListener;

public class PongCanvas extends GameCanvas implements
        CommunicationListener, Runnable {

    private static final int DISTANCE_FROM_SIDE = 6;
    private static final int TABLE_WALK_SIZE = 4;
    private static final int BALL_WALK_SIZE = 3;
    private boolean isServer;
    private Sprite server,  client,  ball,  background;
    private int ballAngle;
    private int xback,  yback;
    boolean moveup, movedown;
    private Sprite me;

    public PongCanvas() {
        super(false);
    }

    private void restartBall() {
        this.ball.setPosition(this.getWidth() >> 1, this.getHeight() >> 1);
    }

    private void startUI() {
        moveup = movedown = false;

        this.background = new Sprite(ImageUtil.loadImage("/back.png"));
        xback = (this.getWidth() >> 1) - (this.background.getWidth() >> 1);
        yback = (this.getHeight() >> 1) - (this.background.getWidth() >> 1);
        this.background.setPosition(xback, yback);

        Image table = ImageUtil.loadImage("/table.png");
        this.server = new Sprite(table);
        this.server.defineReferencePixel(this.server.getWidth() >> 1,
                this.server.getHeight() >> 1);
        this.server.setPosition((this.getWidth() - this.background.getWidth()) >> 1,
                (this.getHeight() - this.server.getHeight()) >> 1);

        this.client = new Sprite(table);
        this.client.defineReferencePixel(-this.client.getWidth() >> 1,
                -this.client.getHeight() >> 1);
        this.client.setPosition(((this.getWidth() + this.background.getWidth()) >> 1) - this.client.getWidth(),
                (this.getHeight() - this.server.getHeight()) >> 1);

        ballAngle = 1;//new Random().nextInt() % 4;

        this.ball = new Sprite(ImageUtil.loadImage("/ball.png"));
        this.ball.defineReferencePixel(-this.ball.getWidth() >> 1, -this.ball.getHeight() >> 1);
        this.restartBall();

        if (this.isServer) {
            this.me = server;
        } else {
            this.me = client;
        }
    }

    public void moveup() {
        if (this.me.collidesWith(this.background, false)) {
            this.me.setPosition(this.me.getX(), this.me.getY() - TABLE_WALK_SIZE);
            if (!this.me.collidesWith(this.background, false)) {
                this.me.setPosition(this.me.getX(), this.me.getY() + TABLE_WALK_SIZE);
            } else {
                this.sendPosition();
            }
        }
    }

    public void movedown() {
        if (this.me.collidesWith(this.background, false)) {
            this.me.setPosition(this.me.getX(), this.me.getY() + TABLE_WALK_SIZE);
            if (!this.me.collidesWith(this.background, false)) {
                this.me.setPosition(this.me.getX(), this.me.getY() - TABLE_WALK_SIZE);
            } else {
                this.sendPosition();
            }
        }
    }

    protected void keyPressed(int key) {
        if (key == KEY_NUM2) {
            this.moveup = true;
            this.movedown = false;
        } else if (key == KEY_NUM8) {
            this.movedown = true;
            this.moveup = false;
        }
        super.keyPressed(key);
    }

    protected void keyReleased(int key) {
        this.moveup = movedown = false;
    }

    public void setIsServer(boolean isServer) {
        this.isServer = isServer;
    }

    public void initialize() {
        this.startUI();
        PongMIDlet.instance.setCurrentDisplayable(this);
        Thread t = new Thread(this);
        t.start();
    }

    public void run() {
        while (true) {
            if (this.isServer) {
                this.checkCollision();
                this.moveBall();
                this.sendPosition();
            }
            if (moveup) {
                moveup();
            }
            if (movedown) {
                movedown();
            }
            this.repaint();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    public void checkCollision() {
        if (this.server.collidesWith(this.ball, false)) {
            if (ballAngle == 1) {
                ballAngle = 0;
            } else if (ballAngle == 2) {
                ballAngle = 3;
            }
        } else if (this.client.collidesWith(this.ball, false)) {
            if (ballAngle == 0) {
                ballAngle = 1;
            } else if (ballAngle == 3) {
                ballAngle = 2;
            }
        } else if (!this.background.collidesWith(ball, false)) {
            this.restartBall();
        } else if (this.ball.getY() <= this.yback) {
            if (ballAngle == 0) {
                ballAngle = 3;
            } else if (ballAngle == 1) {
                ballAngle = 2;
            }
        } else if (this.ball.getY() + ball.getHeight() > this.yback + this.background.getHeight()) {
            if (ballAngle == 2) {
                ballAngle = 1;
            } else if (ballAngle == 3) {
                ballAngle = 0;
            }
        }
    }

    public void moveBall() {
        int x = BALL_WALK_SIZE, y = -BALL_WALK_SIZE;
        if (ballAngle == 1) {
            x = -BALL_WALK_SIZE;
        } else if (ballAngle == 2) {
            y = BALL_WALK_SIZE;
            x = -BALL_WALK_SIZE;
        } else if (ballAngle == 3) {
            y = BALL_WALK_SIZE;
        }
        this.ball.setPosition(ball.getX() + x, ball.getY() + y);
    }

    public void sendPosition() {
        String msg;
        if (this.isServer) {
            msg = "p" + (this.server.getY() - (getHeight() >> 1)) 
                    + "b" + this.ball.getX() + "x" + this.ball.getY();
        } else {
            msg = "p" + (this.client.getY() - (getHeight() >> 1));
        }
        msg += ";";
        PongMIDlet.instance.getDevice().send(msg.getBytes());

    }

    public void paint(Graphics g) {
        g.setColor(0, 0, 0);
        g.fillRect(-5, -5, this.getWidth() + 5, this.getHeight() + 5);

        this.background.paint(g);
        this.server.paint(g);
        this.client.paint(g);
        this.ball.paint(g);
    }

    public void receiveMessage(byte[] b) {
        String msg = new String(b);
        int firstIndex = 0;
        int lastIndex = 0;
        while ((lastIndex = msg.indexOf(";", firstIndex)) != -1) {

            try {

                String nextToken = msg.substring(firstIndex, lastIndex);

                if (this.isServer) {
                    this.client.setPosition(this.client.getX(), (this.getHeight() >> 1) + Integer.parseInt(nextToken.substring(1)));
                } else {
                    int indexOfBallPosition = nextToken.indexOf("b");
                    this.server.setPosition(this.server.getX(), (this.getHeight() >> 1) + Integer.parseInt(nextToken.substring(1, indexOfBallPosition)));
                    int[] values = ImageUtil.separeValues(nextToken.substring(indexOfBallPosition + 1), "x");
                    this.ball.setPosition(values[0], values[1]);
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
}
