import javafx.util.Pair;
import ru.spbstu.pipeline.RC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Configer {

    private static String splitter;
    private final List<String> grammar;
    public List<Pair<String,String>> config;
    private boolean withGrammar;
    private final Logger LOGGER;
    RC errorState;

    String find(String name)
    {
        for(int i =0;i<config.size(); i++)
        {
            if(config.get(i).getKey().equals(name))
            {
                return config.get(i).getValue();
            }
        }
        LOGGER.log(Level.SEVERE, "Can't find config's parameter name");
        return  null;
    }

    Configer(String configFile, List<String> grammar_tokens, String separator, boolean _withGrammar, Logger logger)
    {
        LOGGER = logger;
        withGrammar = _withGrammar;
        config = new ArrayList<Pair<String, String >>();
        grammar = grammar_tokens;
        splitter = separator;
        errorState = readConfig(configFile);
    }

    private RC readConfig(String file)
    {
        if(file == null)
        {
            return RC.CODE_INVALID_ARGUMENT;
        }
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            LOGGER.log(Level.INFO, "Parsing config file " + file);

            while((line = br.readLine())!=null)
            {
                if(withGrammar) parseConfig(line);
                else parseConfigWithoutGrammar(line);
            }
            br.close();
        }
        catch(IOException ex)
        {
            LOGGER.log(Level.SEVERE, "Failed opening the config file: " + file);
            return RC.CODE_INVALID_INPUT_STREAM;
        }
        return checkConfig();
    }

    public void setWithoutGrammarMode()
    {
        withGrammar=false;
    }

    void putPair(String name, String cfg)
    {
        Pair<String, String> pair = new Pair<>(name, cfg);
        config.add(pair);
    }

    private void parseConfig(String line)
    {
        String[] words =  line.split(splitter);
        LOGGER.log(Level.INFO, "Filling config container with line " + line);

        for(String value: grammar)
        {
            if(value.equals(words[0]))
            {
                putPair(words[0], words[1]);
                break;
            }
        }
    }

    private void parseConfigWithoutGrammar(String line)
    {
        LOGGER.log(Level.INFO, line);
        String[] words =  line.split(splitter);
        LOGGER.log(Level.INFO, "Filling config container with line " + line);
        putPair(words[0], words[1]);
    }

    private RC checkConfig()
    {
        if(withGrammar)
        {
            if (config.size() < grammar.size())
            {
                return RC.CODE_CONFIG_GRAMMAR_ERROR;
            }
            return RC.CODE_SUCCESS;
        }
        return RC.CODE_SUCCESS;
    }

}
