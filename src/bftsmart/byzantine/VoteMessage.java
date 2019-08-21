package bftsmart.byzantine;

import bftsmart.communication.SystemMessage;
import bftsmart.reconfiguration.ReconfigureReply;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class VoteMessage extends SystemMessage {

    private int vote;

    public VoteMessage() {}

    public VoteMessage(int vote) {
        super();
        this.vote = vote;
    }

    // Implemented method of the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(vote);
    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.vote = (int) in.readObject();
    }

    public int getVote() { return vote; }
}
