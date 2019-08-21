package bftsmart.byzantine;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.communication.server.ServerConnection;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.reconfiguration.ViewManager;
import bftsmart.tom.util.KeyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

public class TTPManager {
    private KeyLoader keyLoader;
    private String configDir;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private ServerViewController controller;
    private int id;
    private TTPProxy proxy;

    private TTPServerCommunicationSystem cs;
    private ServerConnection conn;



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
        cs.start();
    }

    public void receiveVote(VoteMessage vote) {
        logger.debug("[TTPManager] vote received from {}", vote.getSender());
    }




    public TTPManager() {
        id = loadID("");
        //creating connections to replicas
        initializeConnections();

//        ViewManager viewManager = new ViewManager("", null);
//        viewManager.viewChange();
//        viewManager.executeVC();


    }







}
