import ru.spbstu.pipeline.*;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Writer implements IWriter {

    public List<String> Grammar_tokens;
    enum Enum_tokens
    {
        BUFFER_SIZE ("BUFFER_SIZE");

        String token;
        Enum_tokens(String t)
        {token = t;}
    }
    public static final String GRAMMAR_SEPARATOR = "=";
    private final TYPE[] supportedFormats={TYPE.BYTE};

    private Configer config;
    private IProducer producer;
    private IMediator mediator;
    private OutputStream outputStream;
    private final Logger LOGGER;

    private int bufferSize;
    private int currentPos = 0;
    private byte[] buffer;

    public Writer(Logger logger)
    {
        Grammar_tokens = new ArrayList<String>();
        for(Enum<Enum_tokens> i: Enum_tokens.values() )
        {Grammar_tokens.add(i.toString());}
        LOGGER = logger;
    }

    @Override
    public RC setOutputStream(FileOutputStream fos)
    {
        outputStream = fos;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConfig(String cfg)
    {
        config = new Configer(cfg, Grammar_tokens,GRAMMAR_SEPARATOR, true, LOGGER);
        bufferSize = Integer.parseInt(config.find(Enum_tokens.BUFFER_SIZE.token));
        buffer = new byte[bufferSize];
        return config.errorState;
    }

    private TYPE typeIntersection(){
        TYPE[] target = (producer.getOutputTypes());
        for (TYPE el:supportedFormats) {
            for(TYPE target_el:target){
                if(target_el == el){
                    return el;
                }
            }
        }
        LOGGER.log(Level.SEVERE, "Executor can't get supported format of data");
        return null;
    }

    @Override
    public RC setConsumer(IConsumer c) {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer p)
    {
        if(p==null)
        {
            LOGGER.log(Level.SEVERE, "invalid producer object");
            return RC.CODE_INVALID_ARGUMENT;
        }
        producer = p;
        TYPE type = typeIntersection();
        if(type!=null) {
            mediator = producer.getMediator(type);
            return RC.CODE_SUCCESS;
        }
        return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
    }

    @Override
    public RC execute()
    {
        LOGGER.log(Level.INFO, "Execute writer");
        byte[] data = (byte[])mediator.getData();
        return binaryWriter(data);
    }


    public RC binaryWriter(byte[] data)
    {
        try
        {
            currentPos = 0;

            if(data == null)
            {
                outputStream.write(buffer, 0, currentPos );
            }
            else {
                while (currentPos < data.length)
                {
                    if (data.length == 0)
                    {
                        outputStream.write(data, 0, currentPos);
                    }
                    else if (data.length <= bufferSize + currentPos)
                    {
                        outputStream.write(data, currentPos, data.length - currentPos);
                        currentPos = data.length;
                    }
                    else
                    {
                        outputStream.write(data, currentPos, bufferSize);
                        currentPos += bufferSize;
                    }

                }
            }
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "Failed to write to the input stream");
            return RC.CODE_FAILED_TO_WRITE;
        }
        return RC.CODE_SUCCESS;
    }
}
