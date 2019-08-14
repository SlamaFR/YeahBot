package fr.slama.yeahbot;

import fr.slama.yeahbot.blub.TaskScheduler;
import fr.slama.yeahbot.commands.core.CommandMap;
import fr.slama.yeahbot.config.Config;
import fr.slama.yeahbot.listeners.*;
import fr.slama.yeahbot.managers.DatabaseManager;
import fr.slama.yeahbot.managers.MusicManager;
import fr.slama.yeahbot.managers.PrivateChannelsManager;
import fr.slama.yeahbot.managers.SetupManager;
import fr.slama.yeahbot.redis.RedisAccess;
import fr.slama.yeahbot.rest.Application;
import fr.slama.yeahbot.sql.DatabaseUpdater;
import fr.slama.yeahbot.tasks.PrivateChannelsTask;
import fr.slama.yeahbot.tasks.SpamTask;
import fr.slama.yeahbot.tasks.SwearingTask;
import fr.slama.yeahbot.tasks.UnmuteTask;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.discordbots.api.client.DiscordBotListAPI;
import org.jooby.Jooby;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.downgoon.snowflake.Snowflake;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created on 09/09/2018.
 */
public class YeahBot extends ListenerAdapter implements Runnable {

    private static final Config CONFIG = Config.parseFile("./config.yml");
    private static final Logger LOGGER = LoggerFactory.getLogger(YeahBot.class);
    private static YeahBot INSTANCE;
    private static boolean dev;
    private final Scanner scanner = new Scanner(System.in);
    private final MusicManager musicManager;
    private final DatabaseManager databaseManager;
    private final CommandMap commandMap;
    private final Snowflake snowflake;
    private final ShardManager shardManager;
    private final DiscordBotListAPI api;
    private final SetupManager setupManager;
    private final MusicListener musicListener;

    private boolean running;

    private YeahBot() throws LoginException {

        INSTANCE = this;

        LOGGER.info("Initializing managers");
        musicManager = new MusicManager();
        databaseManager = new DatabaseManager();
        commandMap = new CommandMap();
        snowflake = new Snowflake(CONFIG.datacenterId, CONFIG.workerId);
        setupManager = new SetupManager();
        musicListener = new MusicListener();

        LOGGER.info("Logging in Discord API...");
        shardManager = new DefaultShardManagerBuilder(CONFIG.token)
                .addEventListeners(this)
                .addEventListeners(new CommandListener(commandMap))
                .addEventListeners(musicListener, new RoleListener())
                .addEventListeners(new SpamListener(), new SwearingListener(), new AdvertisingListener())
                .addEventListeners(new PrivateChannelsManager(), new JoinLeaveListener())
                .build();
        LOGGER.info("Logging in DBL API...");

        RestAction.setPassContext(false);
        RestAction.setDefaultFailure(null);

        api = new DiscordBotListAPI.Builder()
                .token(CONFIG.dbl_token)
                .botId(CONFIG.id)
                .build();
        RedisAccess.init();
    }

    public static void main(String[] args) throws LoginException {

        if (CONFIG == null) {
            LOGGER.error("Config couldn't be loaded!");
            System.exit(1);
        }

        LOGGER.info("Loaded config.");
        if (CONFIG.token == null) {
            LOGGER.error("No token provided!");
            System.exit(1);
        }
        LOGGER.debug("Found token.");

        dev = Arrays.asList(args).contains("--dev");
        if (dev) LOGGER.warn("Starting in DEVELOPMENT MODE!");

        new Thread(new YeahBot()).start();
    }

    public static YeahBot getInstance() {
        return INSTANCE;
    }

    public static boolean isDev() {
        return dev;
    }

    @Override
    public void onReady(ReadyEvent event) {
        super.onReady(event);
        if (event.getGuildAvailableCount() < event.getGuildTotalCount()) return;
        TaskScheduler.scheduleRepeating(new DatabaseUpdater(), 5 * 1000, 300 * 1000);
        TaskScheduler.scheduleRepeating(new SpamTask(), 5 * 1000, 5 * 1000);
        TaskScheduler.scheduleRepeating(new SwearingTask(), 5 * 1000, 60 * 1000);

        TaskScheduler.scheduleDelayed(new UnmuteTask(), 5 * 1000);
        TaskScheduler.scheduleDelayed(new PrivateChannelsTask(), 5 * 1000);

        TaskScheduler.async(() -> Jooby.run(Application::new, "application.port=" + CONFIG.apiPort));

        shardManager.removeEventListener(this);
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            if (scanner.hasNextLine()) commandMap.commandConsole(scanner.nextLine());
        }
        scanner.close();
        shardManager.shutdown();

        LOGGER.info("Saving data to database...");
        new DatabaseUpdater().run();

        databaseManager.close();
        RedisAccess.close();

        LOGGER.info("Stopping bot...");
        System.exit(0);
    }

    public static Config getConfig() {
        return CONFIG;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public CommandMap getCommandMap() {
        return commandMap;
    }

    public Snowflake getSnowflake() {
        return snowflake;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public DiscordBotListAPI getDiscordBotAPI() {
        return api;
    }

    public SetupManager getSetupManager() {
        return setupManager;
    }

    public MusicListener getMusicListener() {
        return musicListener;
    }

    public ThreadLocalRandom getRandomGenerator() {
        return ThreadLocalRandom.current();
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
