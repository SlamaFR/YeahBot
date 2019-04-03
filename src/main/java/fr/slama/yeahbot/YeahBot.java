package fr.slama.yeahbot;

import fr.slama.yeahbot.commands.core.CommandMap;
import fr.slama.yeahbot.config.Config;
import fr.slama.yeahbot.listeners.*;
import fr.slama.yeahbot.music.MusicManager;
import fr.slama.yeahbot.redis.RedisAccess;
import fr.slama.yeahbot.rest.Application;
import fr.slama.yeahbot.sql.DatabaseManager;
import fr.slama.yeahbot.sql.DatabaseUpdater;
import fr.slama.yeahbot.tasks.PrivateChannelsTask;
import fr.slama.yeahbot.tasks.SpamTask;
import fr.slama.yeahbot.tasks.SwearingTask;
import fr.slama.yeahbot.tasks.UnmuteTask;
import fr.slama.yeahbot.utilities.TaskScheduler;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.discordbots.api.client.DiscordBotListAPI;
import org.jooby.Jooby;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created on 09/09/2018.
 */
public class YeahBot extends ListenerAdapter implements Runnable {

    public static final Config CONFIG = Config.parseFile("./config.yml");
    private static final Logger logger = LoggerFactory.getLogger(YeahBot.class);
    private static YeahBot INSTANCE;
    private static boolean dev;
    private final Scanner scanner = new Scanner(System.in);
    private final MusicManager musicManager;
    private final DatabaseManager databaseManager;
    private final CommandMap commandMap;
    private final ShardManager shardManager;
    private final DiscordBotListAPI api;

    private boolean running;

    private YeahBot() throws LoginException {

        INSTANCE = this;

        logger.info("Initializing managers");
        musicManager = new MusicManager();
        databaseManager = new DatabaseManager();
        commandMap = new CommandMap();

        logger.info("Logging in Discord API...");
        shardManager = new DefaultShardManagerBuilder(CONFIG.token)
                .addEventListeners(this)
                .addEventListeners(new CommandListener(commandMap))
                .addEventListeners(new MusicListener(), new RoleListener())
                .addEventListeners(new SpamListener(), new SwearingListener(), new AdvertisingListener())
                .addEventListeners(new PrivateChannelsListener(), new JoinLeaveListener())
                .build();
        logger.info("Logging in DBL API...");
        api = new DiscordBotListAPI.Builder()
                .token(CONFIG.dbl_token)
                .botId(CONFIG.id)
                .build();
        RedisAccess.init();
    }

    public static void main(String[] args) throws LoginException {

        if (CONFIG == null) {
            logger.error("Config couldn't be loaded!");
            System.exit(1);
        }

        logger.info("Loaded config.");
        if (CONFIG.token == null) {
            logger.error("No token provided!");
            System.exit(1);
        }
        logger.debug("Found token.");

        dev = Arrays.asList(args).contains("--dev");

        if (dev) logger.warn("Starting in DEV MODE!");

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
        TaskScheduler.scheduleRepeating(new DatabaseUpdater(), 10L * 1000, 300L * 1000);
        TaskScheduler.scheduleRepeating(new SpamTask(), 10L * 1000, 5L * 1000);
        TaskScheduler.scheduleRepeating(new SwearingTask(), 10L * 1000, 60L * 1000);

        TaskScheduler.async(() -> Jooby.run(Application::new, "application.port=" + CONFIG.apiPort));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new UnmuteTask().run();
        new PrivateChannelsTask().run();

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

        logger.info("Saving data to database...");
        new DatabaseUpdater().run();

        databaseManager.close();
        RedisAccess.close();

        logger.info("Stopping bot...");
        System.exit(0);
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

    public ShardManager getShardManager() {
        return shardManager;
    }

    public DiscordBotListAPI getDiscordBotAPI() {
        return api;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
