package com.mygdx.game.sprites;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.mygdx.game.screens.PlayScreen;

public abstract class Enemy extends Sprite {

    protected static final int RADIUS = 6;

    protected PlayScreen screen;
    protected World world;
    public Body body;
    public Vector2 velocity;

    public Enemy(PlayScreen screen, float x, float y) {
        this.screen = screen;
        this.world = screen.getWorld();
        setPosition(x, y);
        define();
        velocity = new Vector2(1, 0);
        body.setActive(false);
    }

    protected abstract void define();
    public abstract void update(float dt);
    public abstract void hitOnHead(Mario mario);
    public abstract void onEnemyHit(Enemy enemy);

    public void reverseVelocity(boolean x, boolean y) {
        if (x) {
            velocity.x = -velocity.x;
        }
        if (y) {
            velocity.y = -velocity.y;
        }
    }
}
