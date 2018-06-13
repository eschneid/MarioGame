package com.mygdx.game.sprites;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.mygdx.game.MarioGame;
import com.mygdx.game.screens.PlayScreen;

public abstract class Item extends Sprite {

    protected static final int RADIUS = 6;

    protected PlayScreen screen;
    protected World world;
    protected Vector2 velocity;
    protected boolean isToBeDestroyed;
    protected boolean isDestroyed;
    protected Body body;

    public Item(PlayScreen screen, float x, float y) {
        this.screen = screen;
        this.world = screen.getWorld();
        setPosition(x, y);
        setBounds(getX(), getY(), 16/MarioGame.PPM, 16/MarioGame.PPM);
        define();
        isToBeDestroyed = false;
        isDestroyed = false;
    }

    public abstract void define();
    public abstract void use(Mario mario);

    public void update(float dt) {
        if (isToBeDestroyed && !isDestroyed) {
            world.destroyBody(body);
            isDestroyed = true;
        }
    }

    public void draw(Batch batch) {
        if (!isDestroyed) {
            super.draw(batch);
        }
    }

    public void destroy() {
        isToBeDestroyed = true;
    }

    public void reverseVelocity(boolean x, boolean y) {
        if (x) {
            velocity.x = -velocity.x;
        }
        if (y) {
            velocity.y = -velocity.y;
        }
    }
}
