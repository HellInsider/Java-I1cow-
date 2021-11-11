import ru.spbstu.pipeline.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RLECompression implements IExecutor
{
    public static final String GRAMMAR_SEPARATOR = "=";

    private Configer config;
    private static final int  MaxByteCount = 15;
    private static final byte leftwards = 0x10;
    public static int MaxMultiplierDecompress = 8;
    private IConsumer consumer;
    private IProducer producer;
    private IMediator mediator;
    private byte[] processed;
    private final Logger LOGGER;

    private byte[] result = null;
    private final TYPE[] supportedFormats = {TYPE.BYTE};
    public List<String> Grammar_tokens;
    enum Enum_tokens
    {
        MODE ("MODE");

        String token;
        Enum_tokens(String t)
        {token = t;}
    }

    enum Modes
    {
        COMPRESS,
        DECOMPRESS
    }

    public RLECompression(Logger logger)
    {
        Grammar_tokens = new ArrayList<String>();
        for(Enum<Enum_tokens> i: Enum_tokens.values() )
        {Grammar_tokens.add(i.toString());}
        LOGGER=logger;
    }

    private TYPE typeIntersection()
    {
        TYPE[] target = (producer.getOutputTypes());
        for (TYPE el:supportedFormats)
        {
            for(TYPE target_el:target)
            {
                if(target_el == el)
                {
                    return el;
                }
            }
        }
        LOGGER.log(Level.SEVERE, "Executor can't get supported format of data");
        return null;
    }

    @Override
    public RC setConsumer(IConsumer iConsumer)
    {
        if(iConsumer==null)
        {
            LOGGER.log(Level.SEVERE, "Invalid consumer object");
            return RC.CODE_INVALID_ARGUMENT;
        }
        consumer = iConsumer;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer iProducer)
    {
        if(iProducer==null)
        {
            LOGGER.log(Level.SEVERE, "Invalid producer object");
            return RC.CODE_INVALID_ARGUMENT;
        }

        producer = iProducer;
        TYPE type = typeIntersection();
        if(type!=null) {
            mediator = producer.getMediator(type);
            return RC.CODE_SUCCESS;
        }
        return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
    }

    public byte[] RemoveEmpty(byte[] bytes, int size)
    {
        byte[] new_bytes = new byte[size];
        if (size >= 0)
        {
            for(int i=0;i<size;i++)
            {
                new_bytes[i] = bytes[i];
            }
        }
        return new_bytes;
    }

    public RC Compress()
    {

        if(processed == null)
        {
            return RC.CODE_INVALID_ARGUMENT;
        }

        int len = 0;
        byte count = 1;

        for (int i = 0; i < processed.length-1; i++)
        {
            if(processed[i] == processed[i+1] && count<MaxByteCount)
            {
                count++;
            }
            else
            {
                result[len] = (byte)( leftwards + count);
                result[len + 1] = processed[i];
                len += 2;
                count = 1;
            }
        }

        result[len] = (byte)( 0x10 + count);
        result[len + 1] = processed[processed.length-1];
        len += 2;

        result = RemoveEmpty(result, len);
        processed = result;
        return RC.CODE_SUCCESS;
    }

    public RC Decompress()
    {
        byte temp;
        int len = 0;

        for(int i = 0; i< processed.length; i+=2 )
        {
            temp = processed[i];
            temp = (byte) (temp & 0x0F);

            for(int j=0; j<temp; j++)
            {
                result[len] = processed[i+1];
                len++;
            }
        }

        result = RemoveEmpty(result, len);
        processed = result;
        return RC.CODE_SUCCESS;
    }

    public RC Compressor(String mode)
    {
        result = new byte[MaxMultiplierDecompress*processed.length + 1];

        LOGGER.log(Level.INFO, "RLE Mode: " + mode);

        if(mode.equals(Modes.COMPRESS.toString()))
        {
            return Compress();
        }
        else
        {
            return Decompress();
        }

    }

@Override
    public RC execute()
    {
        LOGGER.log(Level.INFO, "Execute RLE");

        RC errorState = RC.CODE_SUCCESS;
        processed = (byte[]) mediator.getData();

        if(processed!=null)
        {
            errorState = Compressor(config.find(Enum_tokens.MODE.token));
        }

        if(errorState == RC.CODE_SUCCESS)
        {
            consumer.execute();
        }


        return errorState;
    }



    @Override
    public IMediator getMediator(TYPE type)
    {
        IMediator _mediat;
        switch (type)
        {
            case BYTE :
                _mediat = new ByteMediator(); break;
            case CHAR :
                _mediat = new CharMediator(); break;
            case SHORT :
                _mediat = new ShortMediator(); break;
            default :
                throw new IllegalStateException("Unexpected value: " + type);
        }
        return _mediat;
    }

    @Override
    public RC setConfig(String cfg)
    {
        config = new Configer(cfg, Grammar_tokens, GRAMMAR_SEPARATOR, true, LOGGER);
        return config.errorState;
    }
    public TYPE[] getOutputTypes() {
        return new TYPE[]{TYPE.SHORT,TYPE.CHAR, TYPE.BYTE};
    }

    private class ByteMediator implements IMediator
    {
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
            if(processed == null)
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



