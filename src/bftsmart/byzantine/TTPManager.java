package bftsmart.byzantine;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.communication.server.ServerConnection;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.reconfiguration.ViewManager;
import bftsmart.tom.leaderchange.LCManager;
import bftsmart.tom.util.KeyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

public class TTPManager {
    private KeyLoader keyLoader;
    private String configDir;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private ServerViewController controller;
    private int id;
    private int currentVoteNum;
    private int currentLeader;
    private TTPProxy proxy;

    private TTPServerCommunicationSystem cs;
    private ServerConnection conn;

    private HashMap<Integer, int[]> votes;

    private ReentrantLock voteLock = new ReentrantLock();
    private LCManager lcManager;
    private ViewManager viewManager;

    private ArrayList<Server> servers;

    private class Server {
        int id;
        String ip;
        int port;
        int portRR;

        public Server (int id, String ip, int port, int portRR) {
            this.id = id;
            this.ip = ip;
            this.port = port;
            this.portRR = portRR;
        }

        public int getId() { return id; }

        public String getIp() { return ip; }

        public int getPort() { return port; }

        public int getPortRR() { return portRR; }
    }

    public void addNewServer(int id, String ip, int port, int portRR) {
        this.servers.add(new Server(id, ip, port, portRR));
    }

    private int loadID(String configHome) {
        try {
            String path = "";
            String sep = System.getProperty("file.separator");
            if (configHome == null || configHome.equals("")) {
                path = "config" + sep + "system.config";
            } else {
                path = configHome + sep + "system.config";
            }
            FileReader fr = new FileReader(path);
            BufferedReader rd = new BufferedReader(fr);
            String line = null;
            while ((line = rd.readLine()) != null) {
                if (!line.startsWith("#")) {
                    StringTokenizer str = new StringTokenizer(line, "=");
                    if (str.countTokens() > 1
                            && str.nextToken().trim().equals("system.ttp.id")) {
                        fr.close();
                        rd.close();
                        return Integer.parseInt(str.nextToken().trim());
                    }
                }
            }
            fr.close();
            rd.close();
            return -1;
        } catch (Exception e) {
            logger.error("Could not load ID", e);
            return -1;
        }
    }

    private void initializeConnections() {
        this.controller = new ServerViewController(id, "", null);

        try {
            cs = new TTPServerCommunicationSystem(this.controller, null, this);
        } catch (Exception ex) {
            logger.error("Failed to initialize client-to-replicas communication systems", ex);
            throw new RuntimeException("Unable to build a communication system.");
        }

    }

    public int[] getVote(int voteNum) {
        int[] vote = votes.get(voteNum);
        if(vote == null) {
            logger.debug("[TTPManager] new voting round");
            vote = new int[controller.getStaticConf().getN()];
            votes.put(voteNum, vote);
        }


        return vote;
    }


    /*determines whether TTP should initiate call to replace replicas
    * replicas are removed if they have received at least f+1 votes against it
    * */
//    public boolean canReplace(int[] currentVoteRound) {
//
//    }


    public void processVote(int[] currentVoteRound, int vote) {
        int N = controller.getStaticConf().getN();
        for(int i = 0; i < N; i++) {
            if((vote & (1 << i)) == 1) {
                currentVoteRound[i]++;
                logger.debug("vote against {} total: {}", i, currentVoteRound[i]);
                if(currentVoteRound[i] >= controller.getStaticConf().getF()) {
                    //if to remove is leader, then vc first
                    if(currentLeader == i) {
                        currentLeader++; //NEED TO GET NEW LEADER FROM REPLIES
                        viewManager.viewChange(currentVoteNum, currentLeader);
                        viewManager.executeVC();
                        logger.debug("[TTPManager] done view change");
                    }
                    Server newServer = servers.get(0);
                    if(newServer == null) {
                        logger.debug("[TTPManager] no replacement server");
                    }else{
                        viewManager.replaceServer(newServer.getId(), newServer.getIp(), newServer.getPort(),
                                                  newServer.getPortRR(), i);
                        viewManager.executeUpdates();
                    }
                    //temporary hack to reset
                    currentVoteRound[i] = -100;
                    currentVoteNum++;
                }
            }
        }
    }


    public void receiveVote(VoteMessage voteMsg) {
        logger.debug("[TTPManager] vote received from {}", voteMsg.getSender());
        logger.debug("voteNum {} vote {}", voteMsg.getVoteNum(), voteMsg.getVote());

        int voteNum = voteMsg.getVoteNum();


        voteLock.lock();

        if(voteNum == currentVoteNum) {
            logger.debug("message vote num {}", voteNum);
            logger.debug("current vote num {}", currentVoteNum);
            int[] currentVoteRound = getVote(voteNum);
            if(currentVoteRound == null){
                logger.error("TTPManager currentVoteRound NULL");
            }

            int vote = voteMsg.getVote();
            processVote(currentVoteRound, vote);
        }
        voteLock.unlock();
    }




    public TTPManager() {
        currentLeader = 0;
        currentVoteNum = 0;
        votes = new HashMap<>();
        servers = new ArrayList<>();

        //load some test servers
        addNewServer(4, "127.0.0.1", 11040, 11041);
        addNewServer(5, "127.0.0.1", 11050, 11051);

        id = loadID("");

        //creating connections to replicas
        initializeConnections();

        viewManager = new ViewManager(id,"", null, this);
        lcManager = new LCManager(this.controller);
//        viewManager.viewChange();
//        viewManager.executeVC();


    }




    public int getCurrentVoteNum() { return currentVoteNum; }


}
