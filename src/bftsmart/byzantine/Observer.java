package bftsmart.byzantine;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.reconfiguration.ServerViewController;

import java.util.HashMap;


public class Observer{
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int Alpha = 5; //number of marks before replica is blacklisted
    public static final int Delta = 5; //f+Delta votes for successful removal
    public static final int Phi = 3;   //consecutive view changes

    private static final int MAX_EXPECTED_LATENCY = 20;

    private int[] marks;        //array to mark the replicas
    private int[] blacklist;    //array to blacklist replicas

    private ServerViewController controller;

    private HashMap<Integer, Long> requestStart;


    public Observer(ServerViewController controller) {
        this.controller = controller;
        int n = controller.getCurrentViewN();

        marks = new int[n];
        blacklist = new int[n];

        this.requestStart = new HashMap<>();
    }


    //marks the replica
    public void mark(int repID) {
        marks[repID]++;
    }


    //blacklist the replica
    public void blacklist(int repID) {


    }


    public boolean isBlacklisted() {
        return true; // stuff here
    }

    public void recordReceivedTime(int requestHash, long receiveTime) {
        requestStart.put(requestHash, receiveTime);
    }

    public void recordProposedTime(int requestHash, long proposedTime) {
        logger.debug("[Observer] request proposed time: {}", proposedTime);
        long delay = proposedTime - requestStart.get(requestHash);
        logger.debug("[Observer] received time: {} delay: {}", requestStart.get(requestHash), delay);
        if(delay > MAX_EXPECTED_LATENCY) {
            int leader = controller.getTomLayer().execManager.getCurrentLeader();
            mark(leader);
            logger.debug("Marking leader. {} marks", marks[leader]);
        }
    }


    public void run() {
        System.out.println("Observer running");
        try {
            Thread.sleep(2000);
        }catch(Exception e) {
            System.out.println("observer thread cannot sleep");
        }
    }


}
