package fr.slama.yeahbot.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created on 23/09/2018.
 */
public class Config {

    private static final Logger logger = LoggerFactory.getLogger("Config");

    @JsonProperty
    public String token = "enter your token";
    @JsonProperty
    public String dbl_token = "";
    @JsonProperty
    public String id = "enter your bot id";
    @JsonProperty
    public String[] owners = new String[0];
    @JsonProperty
    public int apiPort = 8081;
    @JsonProperty
    public Database database = new Database();
    @JsonProperty
    public Redis redis = new Redis();

    public static Config parseFile(String path) {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try {
            final String yamlSource = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            return mapper.readValue(yamlSource, Config.class);
        } catch (IOException e) {
            logger.error("Missing config file!");
            try {
                mapper.writeValue(new File("config.yml"), new Config());
                logger.info("An empty config file has been created! Fill it and run the bot.");
                System.exit(100);
                return null;
            } catch (IOException e1) {
                logger.warn("Couldn't generate config file!");
                System.exit(1);
                return null;
            }
        }
    }

    public class Database {

        @JsonProperty
        public String host = "127.0.0.1";

        @JsonProperty
        public String username = "root";

        @JsonProperty
        public String password = "root";

        @JsonProperty
        public String name = "yeahbot";

        @JsonProperty
        public int port = 3306;

    }

    public class Redis {

        @JsonProperty
        public String host = "127.0.0.1";

        @JsonSetter
        public String password = "";

        @JsonSetter
        public int port = 6379;

    }

}
