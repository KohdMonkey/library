package bftsmart.byzantine;

import bftsmart.tom.core.TOMSender;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.Extractor;
import bftsmart.tom.util.KeyLoader;

public class TTPProxy extends TOMSender {

    private TOMMessage replies[] = null; // Replies from replicas are stored here
    private Extractor extractor;

    public TTPProxy(int processId, String configHome, KeyLoader loader) {
        init(processId, configHome, loader);

        replies = new TOMMessage[getViewManager().getCurrentViewN()];

        extractor = new Extractor() {
            @Override
            public TOMMessage extractResponse(TOMMessage[] replies, int sameContent, int lastReceived) {
                return replies[lastReceived];
            }
        };


    }





    @Override
    public void replyReceived(TOMMessage reply) {

    }




}




