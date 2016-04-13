package andius;

import andius.objects.Icons;
import andius.objects.Actor;
import andius.objects.BaseMap;
import andius.objects.Monster;
import andius.objects.MutableMonster;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface Constants {

    public static int WORLD_TILE_DIM = 24;
    public static int TILE_DIM = 48;

    public static final String SAVE_FILENAME = "game.save";
    public static final String ROSTER_FILENAME = "roster.save";
    public static final int STATS_NONE = 0;
    public static final int STATS_PLAYER1 = 1;
    public static final int STATS_PLAYER2 = 2;
    public static final int STATS_PLAYER3 = 3;
    public static final int STATS_PLAYER4 = 4;
    public static int MOON_PHASES = 24;
    public static int MOON_SECONDS_PER_PHASE = 4;
    public static int MOON_CHAR = 20;
    public static final int MAX_CREATURES_ON_MAP = 10;
    public static final int MAX_WANDERING_CREATURES_IN_DUNGEON = 2;
    public static final int MAX_CREATURE_DISTANCE = 24;

    public enum Resistance {
        NONE,
        MAGIC,
        LVLDRAIN,
        STONING,
        POISON,
        COLD,
        FIRE;
    }

    public enum Ability {
        NONE,
        LVLDRAIN,
        RUN,
        SLEEP,
        AUTOKILL,
        PARALYZE,
        POISON,
        STONE;
    }

    public enum Map {

        WORLD("Andius", "world.tmx", WORLD_TILE_DIM),
        LLECHY("Llechy", "llechy.tmx", TILE_DIM),
        CAVE("Cave", "cave.tmx", TILE_DIM),
        ALIBABA("Shahriar", "ali-baba.tmx", TILE_DIM),
        CANT("Radiant Temple of Cant", "templeCant.tmx", TILE_DIM),
        BARAD_ENELETH("Barad Eneleth", "barad_eneleth.tmx", TILE_DIM),
        WIWOLD("Wiwold", "wiwold.tmx", TILE_DIM),
        WIWOLD_LVL_2("Wiwold Level 2", "wiwold_lvl_2.tmx", TILE_DIM),;

        private final String label;
        private final String tmxFile;
        private final int dim;
        private BaseMap baseMap;
        private TiledMap tiledMap;
        private BaseScreen screen;
        private int startX;
        private int startY;
        private int[][][] roomIds;

        private Map(String label, String tmx, int dim) {
            this.label = label;
            this.tmxFile = tmx;
            this.dim = dim;
        }

        public String getLabel() {
            return label;
        }

        public String getTmxFile() {
            return tmxFile;
        }

        public TiledMap getTiledMap() {
            if (this.tiledMap == null) {
                init();
            }
            return this.tiledMap;
        }

        public BaseMap getMap() {
            return baseMap;
        }

        public int getDim() {
            return dim;
        }

        public BaseScreen getScreen() {
            if (this.screen == null) {
                init();
            }
            return this.screen;
        }

        public int getStartX() {
            return startX;
        }

        public int getStartY() {
            return startY;
        }

        public int[][][] getRoomIds() {
            return roomIds;
        }

        public void init() {

            TmxMapLoader loader = new TmxMapLoader(CLASSPTH_RSLVR);
            this.tiledMap = loader.load("assets/data/" + this.tmxFile);
            this.baseMap = new BaseMap();

            MapProperties prop = this.tiledMap.getProperties();
            this.baseMap.setWidth(prop.get("width", Integer.class));
            this.baseMap.setHeight(prop.get("height", Integer.class));
            this.startX = Integer.parseInt(prop.get("startX", String.class));
            this.startY = Integer.parseInt(prop.get("startY", String.class));

            MapLayer portalsLayer = this.tiledMap.getLayers().get("portals");
            if (portalsLayer != null) {
                Iterator<MapObject> iter = portalsLayer.getObjects().iterator();
                while (iter.hasNext()) {
                    MapObject obj = iter.next();
                    Map pm;
                    try {
                        pm = Map.valueOf(obj.getName());
                    } catch (Exception e) {
                        pm = this;
                    }
                    float x = obj.getProperties().get("x", Float.class);
                    float y = obj.getProperties().get("y", Float.class);
                    int sx = (int) (x / this.dim);
                    int sy = this.baseMap.getHeight() - 1 - (int) (y / this.dim);
                    Object dx = obj.getProperties().get("dx");
                    Object dy = obj.getProperties().get("dy");
                    List<Vector3> randoms = new ArrayList<>();
                    for (int i = 0; i < 6; i++) {
                        String temp = (String) obj.getProperties().get("random" + i);
                        if (temp != null) {
                            String[] s = temp.split(",");
                            randoms.add(new Vector3(Integer.parseInt(s[1]), Integer.parseInt(s[2]), 0));
                        }
                    }
                    this.baseMap.addPortal(pm, sx, sy,
                            dx != null ? Integer.parseInt((String) dx) : -1, dy != null ? Integer.parseInt((String) dy) : -1,
                            randoms.size() > 0 ? randoms : null);
                }
            }

            MapLayer peopleLayer = this.tiledMap.getLayers().get("people");
            if (peopleLayer != null) {
                TiledMapTileLayer iconLayer = (TiledMapTileLayer) this.tiledMap.getLayers().get("creature");
                int firstgid = this.tiledMap.getTileSets().getTileSet("heroes").getProperties().get("firstgid", Integer.class);
                Iterator<MapObject> iter = peopleLayer.getObjects().iterator();
                while (iter.hasNext()) {
                    MapObject obj = iter.next();
                    String surname = obj.getName();
                    float x = obj.getProperties().get("x", Float.class);
                    float y = obj.getProperties().get("y", Float.class);
                    int sx = (int) (x / TILE_DIM);
                    int sy = (int) (y / TILE_DIM);
                    TiledMapTileLayer.Cell iconCell = iconLayer.getCell(sx, sy);
                    Icons icon = Icons.get(iconCell.getTile().getId() - firstgid);
                    Role role = Role.valueOf(obj.getProperties().get("type", String.class));
                    MovementBehavior movement = MovementBehavior.valueOf(obj.getProperties().get("movement", String.class));

                    //System.out.printf("Loading actor: %s %s %s on map %s.\n",surname,role,movement,m);
                    Actor actor = new Actor(icon);
                    if (role == Role.MONSTER) {
                        try {
                            String mid = obj.getProperties().get("creature", String.class);
                            Monster monster = Andius.MONSTER_MAP.get(mid);
                            if (monster != null) {
                                MutableMonster mm = new MutableMonster(monster);
                                mm.name = surname;
                                mm.setIcon(icon);
                                actor.set(mm, role, sx, this.baseMap.getHeight() - 1 - sy, x, y, movement);
                            } else {
                                System.err.printf("Cannot load actor: %s %s %s on map %s.\n", surname, role, movement, this);
                            }
                        } catch (Exception e) {
                            System.err.printf("Cannot find monster: %s on map %s.\n", surname, this);
                        }
                    } else {
                        actor.set(null, role, sx, this.baseMap.getHeight() - 1 - sy, x, y, movement);
                    }

                    this.baseMap.actors.add(actor);
                }
            }

            MapLayer roomsLayer = this.tiledMap.getLayers().get("rooms");
            if (roomsLayer != null) {
                this.roomIds = new int[this.baseMap.getWidth()][this.baseMap.getHeight()][3];
                Iterator<MapObject> iter = roomsLayer.getObjects().iterator();
                while (iter.hasNext()) {
                    MapObject obj = iter.next();
                    int id = obj.getProperties().get("id", Integer.class);
                    PolygonMapObject rmo = (PolygonMapObject) obj;
                    for (int y = 0; y < this.baseMap.getHeight(); y++) {
                        for (int x = 0; x < this.baseMap.getWidth(); x++) {
                            if (rmo.getPolygon().contains(x * TILE_DIM + TILE_DIM / 2, this.baseMap.getHeight() * TILE_DIM - y * TILE_DIM - TILE_DIM / 2)) {
                                if (this.roomIds[x][y][0] == 0) {
                                    this.roomIds[x][y][0] = id;
                                } else if (this.roomIds[x][y][1] == 0) {
                                    this.roomIds[x][y][1] = id;
                                } else if (this.roomIds[x][y][2] == 0) {
                                    this.roomIds[x][y][2] = id;
                                } else {
                                    throw new RuntimeException("Too many overlaps on roomids");
                                }
                            }
                        }
                    }
                }
            }

            this.screen = (this.dim == TILE_DIM ? new GameScreen(this) : new WorldScreen(this));

        }

    }

    public enum MovementBehavior {

        FIXED,
        WANDER,
        FOLLOW_AVATAR,
        ATTACK_AVATAR;
    }

    public static final int[][] LEVEL_PROGRESSION_TABLE = new int[][]{
        {1000, 1100, 1050, 900, 1200, 1200, 1300, 1450},
        {1724, 1896, 1810, 1551, 2105, 2105, 2280, 2543},
        {2972, 3268, 3120, 2674, 3677, 3677, 4000, 4461},
        {5124, 5634, 5379, 4610, 6477, 6477, 7017, 7826},
        {8834, 9713, 9274, 7948, 11363, 11363, 12310, 13729},
        {15231, 16746, 15989, 13703, 19935, 19935, 21596, 24085},
        {26260, 28872, 27567, 23625, 34973, 34973, 37887, 42254},
        {45275, 49779, 47529, 40732, 61356, 61356, 66468, 74129},
        {78060, 85825, 81946, 70227, 107642, 107642, 116610, 130050},
        {134586, 147974, 141286, 121081, 188845, 188845, 204578, 228157},
        {232044, 255127, 243596, 208760, 331307, 331307, 358908, 400275},
        {400075, 439874, 419993, 359931, 581240, 581240, 629663, 702236},
        {289709, 318529, 304132, 260639, 428479, 428479, 475008, 529756}
    };

    public enum CreatureStatus {

        FINE,
        DEAD,
        FLEEING,
        CRITICAL,
        HEAVILYWOUNDED,
        LIGHTLYWOUNDED,
        BARELYWOUNDED;
    }

    public enum Moongate {
        GATE_0(0, 1, 2),
        GATE_1(3, 4, 5),
        GATE_2(6, 7, 0),
        GATE_3(1, 2, 3),
        GATE_4(4, 5, 6),
        GATE_5(7, 0, 1),
        GATE_6(2, 3, 4),
        GATE_7(5, 6, 7),;

        private float x;
        private float y;
        private float mapX;
        private float mapY;
        private final int d1;
        private final int d2;
        private final int d3;

        private TextureAtlas.AtlasRegion currentTexture;

        private Moongate(int d1, int d2, int d3) {
            this.d1 = d1;
            this.d2 = d2;
            this.d3 = d3;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getMapX() {
            return mapX;
        }

        public float getMapY() {
            return mapY;
        }

        public int getD1() {
            return d1;
        }

        public int getD2() {
            return d2;
        }

        public int getD3() {
            return d3;
        }

        public TextureAtlas.AtlasRegion getCurrentTexture() {
            return currentTexture;
        }

        public void setCurrentTexture(TextureAtlas.AtlasRegion currentTexture) {
            this.currentTexture = currentTexture;
        }

        public static void init() {
            MapLayer moongatesLayer = Map.WORLD.getTiledMap().getLayers().get("moongates");
            Iterator<MapObject> iter = moongatesLayer.getObjects().iterator();
            while (iter.hasNext()) {
                MapObject obj = iter.next();
                Moongate mg = Moongate.valueOf(obj.getName());
                mg.x = obj.getProperties().get("x", Float.class);
                mg.y = obj.getProperties().get("y", Float.class);
                mg.mapX = Float.parseFloat(obj.getProperties().get("wx", String.class));
                mg.mapY = Float.parseFloat(obj.getProperties().get("wy", String.class));
            }
        }

    }

    public enum HealType {

        NONE,
        CURE,
        FULLHEAL,
        RESURRECT,
        HEAL;
    }

    public enum Status {

        OK(Color.WHITE),
        AFRAID(Color.YELLOW),
        ASLEEP(Color.MAGENTA),
        POISONED(Color.GREEN),
        PARALYZED(Color.CYAN),
        STONED(Color.LIGHT_GRAY),
        DEAD(Color.RED),
        ASHES(Color.LIGHT_GRAY);

        private final Color color;

        private Status(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

    }

    public enum Role {
        NONE,
        FRIENDLY,
        TEMPLE,
        MONSTER,
        INNKEEPER,
        MERCHANT;
    }

    public static final FileHandleResolver CLASSPTH_RSLVR = new FileHandleResolver() {
        @Override
        public FileHandle resolve(String fileName) {
            return Gdx.files.classpath(fileName);
        }
    };

    public enum SpellTarget {
        PERSON, PARTY, MONSTER, MONSTER_GROUP, ALL_MONSTERS, VARIABLE, NONE, CASTER
    };

    public enum SpellArea {
        COMBAT, ANY_TIME, LOOTING, CAMP, COMBAT_OR_CAMP
    };

    public enum AuraType {

        NONE,
        HORN,
        JINX,
        NEGATE,
        PROTECTION,
        QUICKNESS;
    }

    public enum AttackResult {

        HIT,
        MISS;
    }

    public enum CombatAction {

        ATTACK,
        ADVANCE,
        RANGED,
        FLEE,
        TELEPORT;
    }

    public class AttackVector {

        public final int x;
        public final int y;
        public int distance;
        public AttackResult result;
        public Actor victim;

        public AttackVector(int x, int y, int distance) {
            this.x = x;
            this.y = y;
            this.distance = distance;
        }
    }

    public class AddActorAction implements Runnable {

        private final com.badlogic.gdx.scenes.scene2d.Actor actor;
        private final Stage stage;

        public AddActorAction(Stage stage, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            this.actor = actor;
            this.stage = stage;
        }

        @Override
        public void run() {
            stage.addActor(actor);
        }
    }

    public class PlaySoundAction implements Runnable {

        private Sound s;

        public PlaySoundAction(Sound s) {
            this.s = s;
        }

        @Override
        public void run() {
            Sounds.play(s);
        }
    }

}
