package bftsmart.byzantine;

import bftsmart.communication.SystemMessage;
import bftsmart.reconfiguration.ReconfigureReply;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class VoteMessage extends SystemMessage {

    private int vote;
    private int voteNum;

    public VoteMessage() {}

    public VoteMessage(int vote) {
        super();
        this.vote = vote;
    }

    public VoteMessage(int from, int voteNum, int vote) {
        super(from);
        this.voteNum = voteNum;
        this.vote = vote;
    }

    // Implemented method of the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(voteNum);
        out.writeObject(vote);
    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.voteNum = (int) in.readObject();
        this.vote = (int) in.readObject();
    }

    public int getVote() { return vote; }

    public int getVoteNum() { return voteNum; }
}
