import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Reader implements IReader
{
    public List<String> Grammar_tokens;
    enum Enum_tokens
    {
        BUFFER_SIZE ("BUFFER_SIZE");

        String token;
        Enum_tokens(String t)
        {token = t;}
    }
    public static final String GRAMMAR_SEPARATOR = "=";

    private Configer config;
    private IConsumer consumer;
    private InputStream inputStream;
    private byte[] processed;

    public int bufferSize;
    private final Logger LOGGER;

    public Reader(Logger logger)
    {
        LOGGER = logger;
        Grammar_tokens = new ArrayList<String>();
        for(Enum<Enum_tokens> i: Enum_tokens.values() )
        {Grammar_tokens.add(i.toString());}
    }

    @Override
    public RC setInputStream(FileInputStream fis)
    {
        inputStream = fis;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConfig(String cfg)
    {
        config = new Configer(cfg, Grammar_tokens, GRAMMAR_SEPARATOR, true, LOGGER);
        bufferSize = Integer.parseInt(config.find(Enum_tokens.BUFFER_SIZE.token));
        return config.errorState;
    }

    @Override
    public RC setConsumer(IConsumer c)
    {
        if(c==null)
        {
            LOGGER.log(Level.SEVERE, "Invalid consumer object");
            return RC.CODE_INVALID_ARGUMENT;
        }
        consumer = c;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer p)
    {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute()
    {
        byte[] buffer;
        do
            {
            try
            {
                buffer  = binaryReader();
            }
            catch (IOException e)
            {
                LOGGER.log(Level.SEVERE, "Failed to read from the input stream");
                return RC.CODE_FAILED_TO_READ;
            }

            processed = buffer;
            consumer.execute();

        } while(buffer!=null);
        return RC.CODE_SUCCESS;
    }

    public byte[] binaryReader() throws IOException
    {
        int count;

        byte[] buffer = new byte[bufferSize];
        while((count = inputStream.read(buffer) )!= -1)
        {
            if(count!= bufferSize)
            {
                return  RemoveEmpty(buffer, count);
            }
            return buffer;
        }
        return  null;
    }

    private byte[] RemoveEmpty(byte[] bytes, int size)
    {
        byte[] new_bytes = new byte[size];
        if (size >= 0) System.arraycopy(bytes, 0, new_bytes, 0, size);
        return new_bytes;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return new TYPE[]{TYPE.SHORT,TYPE.CHAR, TYPE.BYTE};
    }


    @Override
    public IMediator getMediator(TYPE type)
    {
        IMediator _mediat;
        switch (type)
        {
            case BYTE :
                _mediat = new Reader.ByteMediator(); break;
            case CHAR :
                _mediat = new Reader.CharMediator(); break;
            case SHORT :
                _mediat = new Reader.ShortMediator(); break;
            default :
                throw new IllegalStateException("Unexpected value: " + type);
        }
        return _mediat;
    }

    private class ByteMediator implements IMediator {
        @Override
        public Object getData()
        {
            if(processed==null)
                return null;

            byte[] copy = new byte[processed.length];
            System.arraycopy(processed, 0, copy, 0, processed.length);
            return copy;
        }
    }

    private class ShortMediator implements IMediator
    {
        @Override
        public Object getData()
        {
            if(processed==null)
                return null;

            int size = processed.length;
            ByteBuffer buffer = ByteBuffer.wrap(processed);

            short[] shortArray = new short[size / 2];

            for(int index = 0; index < size / 2; ++index)
            {
                shortArray[index] = buffer.getShort(2 * index);
            }

            if(shortArray.length>0)
            {
                return shortArray;
            }
            return null;
        }
    }

    private class CharMediator implements IMediator
    {
        @Override
        public Object getData()
        {
            if(processed==null)
                return null;

            return new String(processed, StandardCharsets.UTF_8).toCharArray();
        }
    }
}
