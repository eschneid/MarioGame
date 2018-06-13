package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.MarioGame;
import com.mygdx.game.scenes.Hud;
import com.mygdx.game.sprites.*;
import com.mygdx.game.tools.B2WorldCreator;
import com.mygdx.game.tools.WorldContactListener;

import java.util.concurrent.LinkedBlockingQueue;

public class PlayScreen implements Screen {

    private MarioGame game;
    private TextureAtlas atlas;

    private OrthographicCamera cam;
    private Viewport viewport;
    private Hud hud;

    // Tiled map
    private TmxMapLoader mapLoader;
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    // Box2d
    private World world;
    private Box2DDebugRenderer b2dr;
    private B2WorldCreator creator;

    private Music music;

    private Mario mario;
    private Array<Item> items;
    private LinkedBlockingQueue<ItemDef> itemsToSpawn;

    public PlayScreen(MarioGame game) {
        this.game = game;
        atlas = new TextureAtlas("Mario_and_Enemies.pack");

        cam = new OrthographicCamera();
//        viewport = new StretchViewport(800, 480, cam); // Stretch graphics
//        viewport = new ScreenViewport(cam); // Show extra game world based on screen size
        viewport = new FitViewport(MarioGame.V_WIDTH/MarioGame.PPM, MarioGame.V_HEIGHT/MarioGame.PPM, cam); // scale to fit screen, adding bars
        hud = new Hud(game.batch);

        mapLoader = new TmxMapLoader();
        map = mapLoader.load("level1.tmx");
        renderer = new OrthogonalTiledMapRenderer(map, 1/MarioGame.PPM);
        cam.position.set(viewport.getWorldWidth()/2, viewport.getWorldHeight()/2, 0);

        world = new World(new Vector2(0, -10), true);
        b2dr = new Box2DDebugRenderer();

        creator = new B2WorldCreator(this);
        mario = new Mario(this);
        items = new Array<Item>();
        itemsToSpawn = new LinkedBlockingQueue<ItemDef>();

        world.setContactListener(new WorldContactListener());

        music = MarioGame.manager.get("audio/music/mario_music.ogg", Music.class);
        music.setLooping(true);
        music.setVolume(.3f);
        music.play();
    }

    public void spawnItem(ItemDef idef) {
        itemsToSpawn.add(idef);
    }

    public void handleSpawningItems() {
        if (!itemsToSpawn.isEmpty()) {
            ItemDef idef = itemsToSpawn.poll();
            if (idef.type == Mushroom.class) {
                items.add(new Mushroom(this, idef.position.x, idef.position.y));
            }
        }
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }

    @Override
    public void show() {

    }

    private void handleInput(float dt) {
        if (mario.currentState != Mario.State.DEAD) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                mario.body.applyLinearImpulse(new Vector2(0, 4f), mario.body.getWorldCenter(), true);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) && mario.body.getLinearVelocity().x <= 2) {
                mario.body.applyLinearImpulse(new Vector2(0.1f, 0), mario.body.getWorldCenter(), true);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) && mario.body.getLinearVelocity().x >= -2) {
                mario.body.applyLinearImpulse(new Vector2(-0.1f, 0), mario.body.getWorldCenter(), true);
            }
        }
    }

    public void update(float dt) {
        handleInput(dt);
        handleSpawningItems();
        world.step(1/60f, 6, 2);
        mario.update(dt);
        for (Enemy enemy: creator.getEnemies()) {
            enemy.update(dt);
        }
        for (Item item: items) {
            item.update(dt);
        }
        hud.update(dt);
        if (mario.currentState != Mario.State.DEAD) {
            cam.position.x = mario.body.getPosition().x;
        }
        cam.update();
        renderer.setView(cam);
    }

    @Override
    public void render(float dt) {
        update(dt);

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.render();

        b2dr.render(world, cam.combined);

        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        mario.draw(game.batch);
        for (Enemy enemy: creator.getEnemies()) {
            enemy.draw(game.batch);
            if (enemy.getX() < mario.getX() + 224/MarioGame.PPM) {
                enemy.body.setActive(true);
            }
        }
        for (Item item: items) {
            item.draw(game.batch);
        }
        game.batch.end();

		game.batch.setProjectionMatrix(hud.stage.getCamera().combined);
		hud.stage.draw();

		if (isGameOver()) {
		    game.setScreen(new GameOverScreen(game));
		    dispose();
        }
    }

    public boolean isGameOver() {
        if (mario.currentState == Mario.State.DEAD && mario.getStateTime() > 3) {
            return true;
        }
        return false;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    public TiledMap getMap() {
        return map;
    }

    public World getWorld() {
        return world;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        map.dispose();
        renderer.dispose();
        world.dispose();
        b2dr.dispose();
        hud.dispose();
    }
}
