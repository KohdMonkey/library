package bftsmart.byzantine;


import bftsmart.communication.ServerCommunicationSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.reconfiguration.ServerViewController;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;


public class Observer{
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int ALPHA = 1; //number of marks before replica is blacklisted
    public static final int Delta = 1; //f+Delta votes for successful removal
    public static final int Phi = 3;   //consecutive view changes

    private static final int MAX_EXPECTED_LATENCY = 40;

    private int F;
    private int N;

    private int voteNum;


    private int[] marks;        //array to mark the replicas
    private boolean[] blacklist;    //array to blacklist replicas

    private ServerViewController controller;

    private HashMap<Integer, Long> requestStart;

    private ReentrantLock marksLock = new ReentrantLock();

    public Observer(ServerViewController controller) {
        this.controller = controller;
        this.N = controller.getCurrentViewN();
        this.F = controller.getCurrentViewF();
        this.voteNum = 0;

        marks = new int[this.N];
        blacklist = new boolean[this.N];

        this.requestStart = new HashMap<>();
    }

    private int countMarkedReplicas() {
        int toRemove = 0;
        for(int i = 0; i < N; i++) {
            if(marks[i] >= ALPHA)
                toRemove++;
        }

        return toRemove;
    }

    public int constructVote() {
        int vote = 0;

        for(int i = 0; i < N; i++) {
            if(marks[i] >= ALPHA) {
                vote |= (1 << i);
            }
        }
        return vote;
    }

    public void removeMarks(int removedId) {
        marksLock.lock();
        logger.debug("Removing marks for: {}", removedId);
        marks[removedId] = 0;
        blacklist[removedId] = true;
        marksLock.unlock();
    }


    //marks the replica
    public void mark(int repID) {
        marksLock.lock();
        marks[repID]++;
        logger.debug("repID {} marks {}", repID, marks[repID]);
        if(marks[repID] >= ALPHA) {
            int leader = controller.getTomLayer().execManager.getCurrentLeader();
            int toRemove = countMarkedReplicas();
            //if the leader got ALPHA marks or F or more nodes got ALPHA marks, need to start voting
            if((repID == leader || toRemove > F) && !blacklist[leader]) {
                logger.debug("Need to start voting repID {} toRemove{}", repID, toRemove);
                int vote = constructVote();
                logger.debug("vote: {}", vote);
                int id = controller.getStaticConf().getProcessId();
                VoteMessage v = new VoteMessage(id, voteNum, vote);
                controller.getTomLayer().getCommunication().sendVote(v);
                blacklist[repID] = true;
            }
        }
        marksLock.unlock();
    }


    //blacklist the replica
    public void blacklist(int repID) {

    }


    public boolean isBlacklisted() {
        return true; // stuff here
    }

    public void recordReceivedTime(int requestHash, long receiveTime) {
        logger.debug("Received request {} at time {}", requestHash, receiveTime);
        requestStart.put(requestHash, receiveTime);
    }

    public void recordProposedTime(int requestHash, long proposedTime) {
        logger.debug("[Observer] request {} proposed time: {}", requestHash, proposedTime);
        if(requestStart.get(requestHash) != null) {
            long delay = proposedTime - requestStart.get(requestHash);
            logger.debug("[Observer] received time: {} delay: {}", requestStart.get(requestHash), delay);
            if(delay > MAX_EXPECTED_LATENCY) {
                int leader = controller.getTomLayer().execManager.getCurrentLeader();
                logger.debug("Marking leader{}. {} marks", leader, marks[leader]);
                mark(leader);
            }
        }
    }

    public void setVoteNum(int voteNum) {
        logger.debug("[Observer] reconfig request, setting vote num to {}", voteNum);
        this.voteNum = voteNum;
    }

    public int getVoteNum() { return voteNum; }

    public void run() {
        System.out.println("Observer running");
        try {
            Thread.sleep(2000);
        }catch(Exception e) {
            System.out.println("observer thread cannot sleep");
        }
    }


}
