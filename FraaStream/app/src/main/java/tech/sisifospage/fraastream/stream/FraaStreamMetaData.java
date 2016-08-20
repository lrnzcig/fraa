package tech.sisifospage.fraastream.stream;

/**
 * Not used for the message to the server, but just as metadata of each request
 *
 * Created by elorrub on 20/08/2016.
 */
public class FraaStreamMetaData {

    public int minIndex;
    public int maxIndex;

    public FraaStreamMetaData(int minIndex, int maxIndex) {
        this.minIndex = minIndex;
        this.maxIndex = maxIndex;
    }
}
