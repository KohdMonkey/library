package bftsmart.byzantine;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.reconfiguration.ServerViewController;


public class Observer {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int Alpha = 5; //number of marks before replica is blacklisted
    public static final int Delta = 5; //f+Delta votes for successful removal
    public static final int Phi = 3;   //consecutive view changes

    private int[] marks;        //array to mark the replicas
    private int[] blacklist;    //array to blacklist replicas

    private ServerViewController controller;

    public Observer(ServerViewController controller) {
        this.controller = controller;
        int n = controller.getCurrentViewN();

        marks = new int[n];
        blacklist = new int[n];
    }


    //marks the replica
    public void mark(int repID) {



    }


    //blacklist the replica
    public void blacklist(int repID) {


    }


    public boolean isBlacklisted() {
        return true; // stuff here
    }


}
