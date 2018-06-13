package com.mygdx.game.sprites;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.MarioGame;
import com.mygdx.game.screens.PlayScreen;

public class Mario extends Sprite {

    private static final int RADIUS = 6;

    public enum State {FALLING, JUMPING, STANDING, RUNNING, GROWING, DEAD};
    public State currentState;
    public State previousState;
    public World world;
    public Body body;
    private TextureRegion marioStand;
    private TextureRegion marioJump;
    private Animation<TextureRegion> marioRun;
    private TextureRegion marioDead;
    private TextureRegion bigMarioStand;
    private TextureRegion bigMarioJump;
    private Animation<TextureRegion> bigMarioRun;
    private Animation<TextureRegion> growMario
    ;
    private float stateTime;
    private boolean isFacingRight;
    private boolean isBig;
    private boolean isGrowing;
    private boolean isTimeToDefineBigMario;
    private boolean isTimeToRedefineMario;
    private boolean isDead;

    public Mario(PlayScreen screen) {
        this.world = screen.getWorld();
        currentState = State.STANDING;
        previousState = State.STANDING;
        stateTime = 0;
        isFacingRight = true;

        marioStand = new TextureRegion(screen.getAtlas().findRegion("little_mario"), 0, 0, 16, 16);
        marioJump = new TextureRegion(screen.getAtlas().findRegion("little_mario"), 80, 0, 16, 16);
        marioDead = new TextureRegion(screen.getAtlas().findRegion("little_mario"), 96, 0, 16, 16);
        bigMarioStand = new TextureRegion(screen.getAtlas().findRegion("big_mario"), 0, 0, 16, 32);
        bigMarioJump = new TextureRegion(screen.getAtlas().findRegion("big_mario"), 80, 0, 16, 32);

        Array<TextureRegion> frames = new Array<TextureRegion>();
        for (int i = 1; i < 4; i++) {
            frames.add(new TextureRegion(screen.getAtlas().findRegion("little_mario"), i*16, 0, 16, 16));
        }
        marioRun = new Animation(0.1f, frames);
        frames.clear();

        for (int i = 1; i < 4; i++) {
            frames.add(new TextureRegion(screen.getAtlas().findRegion("big_mario"), i*16, 0, 16, 32));
        }
        bigMarioRun = new Animation(0.1f, frames);
        frames.clear();

        frames.add(new TextureRegion(screen.getAtlas().findRegion("big_mario"), 240, 0, 16, 32));
        frames.add(new TextureRegion(screen.getAtlas().findRegion("big_mario"), 0, 0, 16, 32));
        frames.add(new TextureRegion(screen.getAtlas().findRegion("big_mario"), 240, 0, 16, 32));
        frames.add(new TextureRegion(screen.getAtlas().findRegion("big_mario"), 0, 0, 16, 32));
        growMario = new Animation<TextureRegion>(0.2f, frames);
        frames.clear();

        define();
        setBounds(0, 0, 16/MarioGame.PPM, 16/MarioGame.PPM);
        setRegion(marioStand);
    }

    public void update(float dt) {
        if (isBig) {
            setPosition(body.getPosition().x - getWidth()/2, body.getPosition().y - getHeight()/2 - 6/MarioGame.PPM);
        } else {
            setPosition(body.getPosition().x - getWidth()/2, body.getPosition().y - getHeight()/2);
        }
        setRegion(getFrame(dt));
        if (isTimeToDefineBigMario) {
            defineBigMario();
        } else if (isTimeToRedefineMario) {
            redefineMario();
        }
    }

    private TextureRegion getFrame(float dt) {
        currentState = getState();
        TextureRegion region;
        switch (currentState) {
            case DEAD:
                region = marioDead;
                break;
            case GROWING:
                region = growMario.getKeyFrame(stateTime);
                if (growMario.isAnimationFinished(stateTime)) {
                    isGrowing = false;
                }
                break;
            case JUMPING:
                region = isBig ? bigMarioJump : marioJump;
                break;
            case RUNNING:
                region = isBig ? bigMarioRun.getKeyFrame(stateTime, true) : marioRun.getKeyFrame(stateTime, true);
                break;
            case FALLING:
            case STANDING:
            default:
                region = isBig ? bigMarioStand : marioStand;
                break;
        }

        if ((body.getLinearVelocity().x < 0 || !isFacingRight) && !region.isFlipX()) {
            region.flip(true, false);
            isFacingRight = false;
        } else if ((body.getLinearVelocity().x > 0 || isFacingRight) && region.isFlipX()) {
            region.flip(true, false);
            isFacingRight = true;
        }

        stateTime = currentState == previousState ? stateTime + dt : 0;
        previousState = currentState;
        return region;
    }

    private State getState() {
        if (isDead) {
            return State.DEAD;
        } else if (isGrowing) {
            return State.GROWING;
        } else if (body.getLinearVelocity().y > 0 || (body.getLinearVelocity().y < 0 && previousState == State.JUMPING)) {
            return State.JUMPING;
        } else if (body.getLinearVelocity().y < 0) {
            return State.FALLING;
        } else if (body.getLinearVelocity().x != 0) {
            return State.RUNNING;
        } else {
            return State.STANDING;
        }
    }

    public void grow() {
        isGrowing = true;
        isBig = true;
        isTimeToDefineBigMario = true;
        setBounds(getX(), getY(), getWidth(), getHeight()*2);
        MarioGame.manager.get("audio/sounds/powerup.wav", Sound.class).play();
    }

    public boolean isBig() {
        return isBig;
    }

    public boolean isDead() {
        return isDead;
    }

    public float getStateTime() {
        return stateTime;
    }

    public void hit(Enemy enemy) {
        if (enemy instanceof Turtle && ((Turtle) enemy).getCurrentState() == Turtle.State.STANDING_SHELL) {
            ((Turtle) enemy).kick(this.getX() <= enemy.getX()? Turtle.KICK_RIGHT_SPEED : Turtle.KICK_LEFT_SPEED);
        } else {
            if (isBig) {
                isBig = false;
                isTimeToRedefineMario = true;
                setBounds(getX(), getY(), getWidth(), getHeight() / 2);
                MarioGame.manager.get("audio/sounds/powerdown.wav", Sound.class).play();
            } else {
                MarioGame.manager.get("audio/music/mario_music.ogg", Music.class).stop();
                MarioGame.manager.get("audio/sounds/mariodie.wav", Sound.class).play();
                isDead = true;
                // set all attached filters to collide with nothing
                Filter filter = new Filter();
                filter.maskBits = MarioGame.NOTHING_BIT;
                for (Fixture fixture : body.getFixtureList()) {
                    fixture.setFilterData(filter);
                }
                body.applyLinearImpulse(new Vector2(0, 4f), body.getWorldCenter(), true);
            }
        }
    }

    private void define() {
        BodyDef bdef = new BodyDef();
        bdef.position.set(32/MarioGame.PPM, 32/MarioGame.PPM);
        bdef.type = BodyDef.BodyType.DynamicBody;
        body = world.createBody(bdef);

        FixtureDef fdef = new FixtureDef();
        CircleShape shape = new CircleShape();
        shape.setRadius(RADIUS/MarioGame.PPM);
        fdef.filter.categoryBits = MarioGame.MARIO_BIT;
        fdef.filter.maskBits = MarioGame.GROUND_BIT |
            MarioGame.COIN_BIT |
            MarioGame.BRICK_BIT |
            MarioGame.ENEMY_BIT |
            MarioGame.OBJECT_BIT |
            MarioGame.ENEMY_HEAD_BIT |
            MarioGame.ITEM_BIT;
        fdef.shape = shape;
        body.createFixture(fdef).setUserData(this);

        EdgeShape head = new EdgeShape();
        head.set(new Vector2(-2/MarioGame.PPM, RADIUS/MarioGame.PPM),
            new Vector2(2/MarioGame.PPM, RADIUS/MarioGame.PPM));
        fdef.filter.categoryBits = MarioGame.MARIO_HEAD_BIT;
        fdef.shape = head;
        fdef.isSensor = true;

        body.createFixture(fdef).setUserData(this);
    }

    private void redefineMario() {
        Vector2 position = body.getPosition();
        world.destroyBody(body);

        BodyDef bdef = new BodyDef();
        bdef.position.set(position);
        bdef.type = BodyDef.BodyType.DynamicBody;
        body = world.createBody(bdef);

        FixtureDef fdef = new FixtureDef();
        CircleShape shape = new CircleShape();
        shape.setRadius(RADIUS/MarioGame.PPM);
        fdef.filter.categoryBits = MarioGame.MARIO_BIT;
        fdef.filter.maskBits = MarioGame.GROUND_BIT |
            MarioGame.COIN_BIT |
            MarioGame.BRICK_BIT |
            MarioGame.ENEMY_BIT |
            MarioGame.OBJECT_BIT |
            MarioGame.ENEMY_HEAD_BIT |
            MarioGame.ITEM_BIT;
        fdef.shape = shape;
        body.createFixture(fdef).setUserData(this);

        EdgeShape head = new EdgeShape();
        head.set(new Vector2(-2/MarioGame.PPM, RADIUS/MarioGame.PPM),
            new Vector2(2/MarioGame.PPM, RADIUS/MarioGame.PPM));
        fdef.filter.categoryBits = MarioGame.MARIO_HEAD_BIT;
        fdef.shape = head;
        fdef.isSensor = true;

        body.createFixture(fdef).setUserData(this);
        isTimeToRedefineMario = false;
    }

    private void defineBigMario() {
        Vector2 currentPosition = body.getPosition();
        world.destroyBody(body);

        BodyDef bdef = new BodyDef();
        bdef.position.set(currentPosition.add(0, 10/MarioGame.PPM));
        bdef.type = BodyDef.BodyType.DynamicBody;
        body = world.createBody(bdef);

        FixtureDef fdef = new FixtureDef();
        CircleShape shape = new CircleShape();
        shape.setRadius(RADIUS/MarioGame.PPM);
        fdef.filter.categoryBits = MarioGame.MARIO_BIT;
        fdef.filter.maskBits = MarioGame.GROUND_BIT |
            MarioGame.COIN_BIT |
            MarioGame.BRICK_BIT |
            MarioGame.ENEMY_BIT |
            MarioGame.OBJECT_BIT |
            MarioGame.ENEMY_HEAD_BIT |
            MarioGame.ITEM_BIT;
        fdef.shape = shape;
        body.createFixture(fdef).setUserData(this);
        shape.setPosition(new Vector2(0, -14/MarioGame.PPM));
        body.createFixture(fdef).setUserData(this);

        EdgeShape head = new EdgeShape();
        head.set(new Vector2(-2/MarioGame.PPM, RADIUS/MarioGame.PPM),
            new Vector2(2/MarioGame.PPM, RADIUS/MarioGame.PPM));
        fdef.filter.categoryBits = MarioGame.MARIO_HEAD_BIT;
        fdef.shape = head;
        fdef.isSensor = true;

        body.createFixture(fdef).setUserData(this);
        isTimeToDefineBigMario = false;
    }
}
