package andius;

import static andius.Andius.CTX;
import static andius.Andius.REWARDS;
import static andius.Andius.mainGame;
import static andius.Constants.TILE_DIM;
import andius.objects.Actor;
import andius.objects.Conversations.Conversation;
import andius.objects.Portal;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.Iterator;
import utils.PartyDeathException;
import utils.TmxMapRenderer;
import utils.TmxMapRenderer.CreatureLayer;

public class GameScreen extends BaseScreen {

    private final Map map;
    private final TmxMapRenderer renderer;
    private final Batch batch;
    private final Viewport mapViewPort;

    public GameScreen(Map map) {

        this.map = map;

        batch = new SpriteBatch();

        stage = new Stage(viewport);

        camera = new OrthographicCamera(Andius.MAP_VIEWPORT_DIM, Andius.MAP_VIEWPORT_DIM);

        mapViewPort = new ScreenViewport(camera);

        addButtons(this.map);

        renderer = new TmxMapRenderer(this.map, this.map.getTiledMap(), 1f);

        renderer.registerCreatureLayer(new CreatureLayer() {
            @Override
            public void render(float time) {
                renderer.getBatch().draw(Andius.game_scr_avatar.getKeyFrame(time, true), newMapPixelCoords.x, newMapPixelCoords.y - TILE_DIM + 8);
                for (Actor cr : GameScreen.this.map.getMap().actors) {
                    if (renderer.shouldRenderCell(currentRoomId, cr.getWx(), cr.getWy())) {
                        renderer.getBatch().draw(cr.getAnimation().getKeyFrame(time, true), cr.getX(), cr.getY() + 8);
                    }
                }
            }
        });

        mapPixelHeight = this.map.getMap().getHeight() * TILE_DIM;

        setMapPixelCoords(newMapPixelCoords, this.map.getStartX(), this.map.getStartY());

        if (this.map.getRoomIds() != null) {
            currentRoomId = this.map.getRoomIds()[this.map.getStartX()][this.map.getStartY()][0];
        }

    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputMultiplexer(this, stage));
    }

    @Override
    public void hide() {

    }

    @Override
    public void log(String s) {
        Andius.HUD.add(s);
    }

    @Override
    public void render(float delta) {

        time += delta;

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (renderer == null) {
            return;
        }

        camera.position.set(newMapPixelCoords.x + 3 * TILE_DIM + 24 + 8, newMapPixelCoords.y - 1 * TILE_DIM, 0);

        camera.update();

        renderer.setView(camera.combined,
                camera.position.x - TILE_DIM * 10,
                camera.position.y - TILE_DIM * 6,
                Andius.MAP_VIEWPORT_DIM,
                Andius.MAP_VIEWPORT_DIM);

        renderer.render();

        batch.begin();

        batch.draw(Andius.backGround, 0, 0);
        Andius.HUD.render(batch, Andius.CTX);

//        Vector3 v = getCurrentMapCoords();
//        Andius.smallFont.draw(batch, String.format("%s, %s\n", v.x, v.y), 200, Andius.SCREEN_HEIGHT - 32);
        batch.end();

        stage.act();
        stage.draw();

    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        mapViewPort.update(width, height, false);
    }

    @Override
    public void setMapPixelCoords(Vector3 v, int x, int y) {
        v.set(x * TILE_DIM, mapPixelHeight - y * TILE_DIM, 0);
    }

    @Override
    public void setCurrentMapCoords(Vector3 v) {
        Vector3 tmp = camera.unproject(new Vector3(TILE_DIM * 7, TILE_DIM * 8, 0), 48, 96, Andius.MAP_VIEWPORT_DIM, Andius.MAP_VIEWPORT_DIM);
        v.set(Math.round(tmp.x / TILE_DIM) - 3, ((mapPixelHeight - Math.round(tmp.y) - TILE_DIM) / TILE_DIM) - 0, 0);
    }

    @Override
    public boolean keyUp(int keycode) {
        Vector3 v = new Vector3();
        setCurrentMapCoords(v);

        if (keycode == Keys.UP) {
            if (!preMove(v, Direction.NORTH)) {
                return false;
            }
            newMapPixelCoords.y = newMapPixelCoords.y + TILE_DIM;
            v.y -= 1;
        } else if (keycode == Keys.DOWN) {
            if (!preMove(v, Direction.SOUTH)) {
                return false;
            }
            newMapPixelCoords.y = newMapPixelCoords.y - TILE_DIM;
            v.y += 1;
        } else if (keycode == Keys.RIGHT) {
            if (!preMove(v, Direction.EAST)) {
                return false;
            }
            newMapPixelCoords.x = newMapPixelCoords.x + TILE_DIM;
            v.x += 1;
        } else if (keycode == Keys.LEFT) {
            if (!preMove(v, Direction.WEST)) {
                return false;
            }
            newMapPixelCoords.x = newMapPixelCoords.x - TILE_DIM;
            v.x -= 1;
        } else if (keycode == Keys.E || keycode == Keys.K) {
            Portal p = this.map.getMap().getPortal((int) v.x, (int) v.y);
            if (p != null && p.getMap() != this.map) {
                Vector3 dv = p.getDest();
                int dx = (int) dv.x;
                int dy = (int) dv.y;
                if (dx >= 0 && dy >= 0) {
                    if (p.getMap().getRoomIds() != null) {
                        p.getMap().getScreen().currentRoomId = p.getMap().getRoomIds()[dx][dy][0];
                    }
                    p.getMap().getScreen().setMapPixelCoords(p.getMap().getScreen().newMapPixelCoords, dx, dy);
                }
                Andius.mainGame.setScreen(p.getMap().getScreen());
            }
            return false;
        } else if (keycode == Keys.G) {
            TiledMapTileLayer layer = (TiledMapTileLayer) this.map.getTiledMap().getLayers().get("props");
            TiledMapTileLayer.Cell cell = layer.getCell((int) v.x, this.map.getMap().getHeight() - 1 - (int) v.y);
            if (cell != null && cell.getTile().getId() == 1350) { //chest item id
                RewardScreen rs = new RewardScreen(CTX, this.map, 1, 0, REWARDS.get(rand.nextInt(10)), REWARDS.get(rand.nextInt(10)));
                mainGame.setScreen(rs);
                cell.setTile(null);
                return false;
            }
        } else if (keycode == Keys.T) {
            Actor a = this.map.getMap().getCreatureAt((int) v.x, (int) v.y);
            if (a != null) {
                if (a.getRole() == Role.TEMPLE) {
                    TempleScreen rs = new TempleScreen(CTX, this.map);
                    mainGame.setScreen(rs);
                    return false;
                } else if (a.getRole() == Role.MERCHANT || a.getRole() == Role.MERCHANT1 || a.getRole() == Role.MERCHANT2) {
                    VendorScreen rs = new VendorScreen(CTX, a.getRole(), this.map);
                    mainGame.setScreen(rs);
                    return false;
                } else if (a.getRole() == Role.INNKEEPER) {
                    InnScreen rs = new InnScreen(CTX, this.map);
                    mainGame.setScreen(rs);
                    return false;
                } else {
                    Conversation c = Andius.CONVERSATIONS.get(this.map.getLabel(), a.getName());
                    if (c != null) {
                        new ConversationDialog(CTX, this, c).show(this.stage);
                    }
                }
            }
        }

        finishTurn((int) v.x, (int) v.y);

        return false;
    }

    private boolean preMove(Vector3 current, Direction dir) {

        int nx = (int) current.x;
        int ny = (int) current.y;

        if (dir == Direction.NORTH) {
            ny = (int) current.y - 1;
        }
        if (dir == Direction.SOUTH) {
            ny = (int) current.y + 1;
        }
        if (dir == Direction.WEST) {
            nx = (int) current.x - 1;
        }
        if (dir == Direction.EAST) {
            nx = (int) current.x + 1;
        }

        if (nx > this.map.getMap().getWidth() - 1 || nx < 0 || ny > this.map.getMap().getHeight() - 1 || ny < 0) {
            Andius.mainGame.setScreen(Map.WORLD.getScreen());
            return false;
        }

        TiledMapTileLayer layer = (TiledMapTileLayer) this.map.getTiledMap().getLayers().get("floor");
        TiledMapTileLayer.Cell cell = layer.getCell(nx, this.map.getMap().getHeight() - 1 - ny);
        if (cell == null) {
            //Sounds.play(Sound.BLOCKED);
            return false;
        }
        
        MapLayer messagesLayer = this.map.getTiledMap().getLayers().get("messages");
        if (messagesLayer != null) {
            Iterator<MapObject> iter = messagesLayer.getObjects().iterator();
            while (iter.hasNext()) {
                MapObject obj = iter.next();
                float mx = obj.getProperties().get("x", Float.class) / TILE_DIM;
                float my = obj.getProperties().get("y", Float.class) / TILE_DIM;
                if (nx == mx && this.map.getMap().getHeight() - 1 - ny == my) {
                    String msg = obj.getProperties().get("type", String.class);
                    animateText(msg, Color.WHITE, 100, 300, 100, 500, 2);
                }
            }
        }
    

        Portal p = this.map.getMap().getPortal((int) nx, (int) ny);
        if (p != null && p.getMap() == this.map) { //warp over portal on the same map ie ali-baba map has this
            Vector3 dv = p.getDest();
            if (this.map.getRoomIds() != null) {
                currentRoomId = this.map.getRoomIds()[(int) dv.x][(int) dv.y][0];
            }
            setMapPixelCoords(newMapPixelCoords, (int) dv.x, (int) dv.y);
            return false;
        }

        return true;
    }

    @Override
    public void endCombat(boolean isWon, andius.objects.Actor opponent) {
        if (isWon) {
            this.map.getMap().removeCreature(opponent);
        }
    }

    @Override
    public void finishTurn(int x, int y) {

        if (this.map.getRoomIds() != null && this.map.getRoomIds()[x][y][1] == 0) {
            this.currentRoomId = this.map.getRoomIds()[x][y][0];
        }

        try {
            this.map.getMap().moveObjects(this.map, this, x, y);
        } catch (PartyDeathException t) {
            partyDeath();
        }
    }

    @Override
    public void partyDeath() {
    }

}
