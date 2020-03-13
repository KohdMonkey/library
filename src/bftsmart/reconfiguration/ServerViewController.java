/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bftsmart.reconfiguration;

import java.net.InetSocketAddress;
import java.util.*;

import bftsmart.reconfiguration.views.View;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.KeyLoader;
import bftsmart.tom.util.TOMUtil;

import java.security.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author eduardo
 */
public class ServerViewController extends ViewController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int ADD_SERVER = 0;
    public static final int REMOVE_SERVER = 1;
    public static final int CHANGE_F = 2;

    private int quorumBFT; // ((n + f) / 2) replicas
    private int quorumCFT; // (n / 2) replicas
    private int[] otherProcesses;
    private int[] lastJoinStet;
    private List<TOMMessage> updates = new LinkedList<TOMMessage>();
    private TOMLayer tomLayer;
    // protected View initialView;

    public ServerViewController(int procId, KeyLoader loader) {
        this(procId, "", loader);
        /*super(procId);
        initialView = new View(0, getStaticConf().getInitialView(), 
                getStaticConf().getF(), getInitAdddresses());
        getViewStore().storeView(initialView);
        reconfigureTo(initialView);*/
    }

    public ServerViewController(int procId, String configHome, KeyLoader loader) {
        super(procId, configHome, loader);
        View cv = getViewStore().readView();
        if (cv == null) {

            logger.info("Creating current view from configuration file");
            reconfigureTo(new View(0, getStaticConf().getInitialView(),
                    getStaticConf().getF(), getInitAdddresses()));
        } else {
            logger.info("Using view stored on disk");
            reconfigureTo(cv);
        }
    }

    private InetSocketAddress[] getInitAdddresses() {

        int nextV[] = getStaticConf().getInitialView();
        InetSocketAddress[] addresses = new InetSocketAddress[nextV.length];
        for (int i = 0; i < nextV.length; i++) {
            addresses[i] = getStaticConf().getRemoteAddress(nextV[i]);
        }

        return addresses;
    }

    public void setTomLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }


    public boolean isInCurrentView() {
        return this.currentView.isMember(getStaticConf().getProcessId());
    }

    public int[] getCurrentViewOtherAcceptors() {
        return this.otherProcesses;
    }

    public int[] getCurrentViewAcceptors() {
        return this.currentView.getProcesses();
    }

    public boolean hasUpdates() {
        return !this.updates.isEmpty();
    }

    public void enqueueUpdate(TOMMessage up) {
        ReconfigureRequest request = (ReconfigureRequest) TOMUtil.getObject(up.getContent());
        if (request != null && request.getSender() == getStaticConf().getTTPId()
                && TOMUtil.verifySignature(getStaticConf().getPublicKey(request.getSender()),
                request.toString().getBytes(), request.getSignature())) {
            //if (request.getSender() == getStaticConf().getTTPId()) {
            this.updates.add(up);
        } else {
            logger.warn("Invalid reconfiguration from {}, discarding", up.getSender());
        }
    }

//    public void test(ReconfigureRequest request) {
//
//        List<Integer> jSet = new LinkedList<>();
//        List<Integer> rSet = new LinkedList<>();
//        int f = -1;
//
//        List<String> jSetInfo = new LinkedList<>();
//
//        int key = ADD_SERVER;
//        String value = request.getProperties().get(key);
//
//        logger.info(Arrays.toString(currentView.getProcesses()));
//        if (key == ADD_SERVER) {
//            logger.info("here");
//            StringTokenizer str = new StringTokenizer(value, ":");
//            if (str.countTokens() > 2) {
//                int id = Integer.parseInt(str.nextToken());
//                if (!isCurrentViewMember(id) && !contains(id, jSet)) {
//                    jSetInfo.add(value);
//                    jSet.add(id);
//                    String host = str.nextToken();
//                    int port = Integer.valueOf(str.nextToken());
//                    int portRR = Integer.valueOf(str.nextToken());
//                    this.getStaticConf().addHostInfo(id, host, port, portRR);
//                }
//            }
//        } else if (key == REMOVE_SERVER) {
//            if (isCurrentViewMember(Integer.parseInt(value))) {
//                rSet.add(Integer.parseInt(value));
//            }
//        } else if (key == CHANGE_F) {
//            f = Integer.parseInt(value);
//        }
//
//        logger.info("jSet: " + jSet.toString());
//        logger.info("jsetinfo: " + jSetInfo.toString());
//        reconfigure(jSetInfo, jSet, rSet, f, 1);
//    }


    public byte[] executeUpdates(int cid) {


        List<Integer> jSet = new LinkedList<>();
        List<Integer> rSet = new LinkedList<>();
        int f = -1;

        List<String> jSetInfo = new LinkedList<>();


        for (int i = 0; i < updates.size(); i++) {
            ReconfigureRequest request = (ReconfigureRequest) TOMUtil.getObject(updates.get(i).getContent());
            Iterator<Integer> it = request.getProperties().keySet().iterator();

            while (it.hasNext()) {
                int key = it.next();
                String value = request.getProperties().get(key);

                if (key == ADD_SERVER) {
                    StringTokenizer str = new StringTokenizer(value, ":");
                    if (str.countTokens() > 2) {
                        int id = Integer.parseInt(str.nextToken());
                        if (!isCurrentViewMember(id) && !contains(id, jSet)) {
                            jSetInfo.add(value);
                            jSet.add(id);
                            String host = str.nextToken();
                            int port = Integer.valueOf(str.nextToken());
                            int portRR = Integer.valueOf(str.nextToken());
                            this.getStaticConf().addHostInfo(id, host, port, portRR);
                        }
                    }
                } else if (key == REMOVE_SERVER) {
                    if (isCurrentViewMember(Integer.parseInt(value))) {
                        rSet.add(Integer.parseInt(value));
                    }
                } else if (key == CHANGE_F) {
                    f = Integer.parseInt(value);
                }
            }

        }
        return reconfigure(jSetInfo, jSet, rSet, f, cid);
    }

    private boolean contains(int id, List<Integer> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).intValue() == id) {
                return true;
            }
        }
        return false;
    }


    private byte[] reconfigure(List<String> jSetInfo, List<Integer> jSet, List<Integer> rSet, int f, int cid) {
        lastJoinStet = new int[jSet.size()];
        int[] nextV = new int[currentView.getN() + jSet.size() - rSet.size()];
        int p = 0;

        boolean forceLC = false;
        for (int i = 0; i < jSet.size(); i++) {
            lastJoinStet[i] = jSet.get(i);
            nextV[p++] = jSet.get(i);
        }

        for (int i = 0; i < currentView.getProcesses().length; i++) {
            if (!contains(currentView.getProcesses()[i], rSet)) {
                nextV[p++] = currentView.getProcesses()[i];
            } else if (tomLayer.execManager.getCurrentLeader() == currentView.getProcesses()[i]) {

                forceLC = true;

            }
        }

        if (f < 0) {
            f = currentView.getF();
        }

        InetSocketAddress[] addresses = new InetSocketAddress[nextV.length];

        for (int i = 0; i < nextV.length; i++)
            addresses[i] = getStaticConf().getRemoteAddress(nextV[i]);

        View newV = new View(currentView.getId() + 1, nextV, f, addresses);

        logger.info("New view: " + newV);
        logger.info("Installed on CID: " + cid);
        logger.info("lastJoinSet: " + jSet);

        //TODO:Remove all information stored about each process in rSet
        //processes execute the leave!!!
        reconfigureTo(newV);

        if (forceLC) {

            //TODO: Reactive it and make it work
            logger.info("Shortening LC timeout");
            tomLayer.requestsTimer.stopTimer();
            tomLayer.requestsTimer.setShortTimeout(3000);
            tomLayer.requestsTimer.startTimer();
            //tomLayer.triggerTimeout(new LinkedList<TOMMessage>());

        }
        return TOMUtil.getBytes(new ReconfigureReply(newV, jSetInfo.toArray(new String[0]),
                cid, tomLayer.execManager.getCurrentLeader()));
    }


    public byte[] replaceReplicas(List<Integer> rSet, List<String> toAdd) {
        List<Integer> jSet = new ArrayList<>();
        for (String val : toAdd) {
            StringTokenizer str = new StringTokenizer(val, ":");
            int id = Integer.parseInt(str.nextToken());
            jSet.add(id);
            String host = str.nextToken();
            int port = Integer.valueOf(str.nextToken());
            int portRR = Integer.valueOf(str.nextToken());
            logger.info("adding " + id + " host: " + host + " port: " + port);
            this.getStaticConf().addHostInfo(id, host, port, portRR);
        }
        int[] nextV = new int[currentView.getN() + jSet.size() - rSet.size()];

        //setting the new nodes at the front of the new view
        int p = 0;
        for (int i = 0; i < jSet.size(); i++) {
            nextV[p++] = jSet.get(i);
        }

        logger.info("p: " + p);
        logger.info("next view");
        for (int i = 0; i < nextV.length; i++) {
            logger.info("" + nextV[i]);
        }

        logger.info("current view: " + Arrays.toString(currentView.getProcesses()));
        logger.info("rset: " + rSet.toString());

        //adding the nodes in the current view
        for (int i = 0; i < currentView.getProcesses().length; i++) {
            //adding to next view
            if (!contains(currentView.getProcesses()[i], rSet)) {
                nextV[p++] = currentView.getProcesses()[i];
                logger.info(i + " is not in removed set, p: " + p);
            }
        }

        //retrieve f
        int f = currentView.getF();

        InetSocketAddress[] addresses = new InetSocketAddress[nextV.length];

        logger.info("nodes in the new view");
        for (int i = 0; i < nextV.length; i++) {
            addresses[i] = getStaticConf().getRemoteAddress(nextV[i]);
            logger.info("id: " + nextV[i]);
            logger.info("host: " + getStaticConf().getHost(nextV[i]));
            logger.info("port: " + getStaticConf().getPort(nextV[i]));
            logger.info("RRport: " + getStaticConf().getServerToServerPort(nextV[i]));
        }

        View newV = new View(currentView.getId() + 1, nextV, f, addresses);
        return TOMUtil.getBytes(new ReconfigureReply(newV, toAdd.toArray(new String[0]), 0));
    }


    public TOMMessage[] clearUpdates() {
        TOMMessage[] ret = new TOMMessage[updates.size()];
        for (int i = 0; i < updates.size(); i++) {
            ret[i] = updates.get(i);
        }
        updates.clear();
        return ret;
    }

    public boolean isInLastJoinSet(int id) {
        if (lastJoinStet != null) {
            for (int i = 0; i < lastJoinStet.length; i++) {
                if (lastJoinStet[i] == id) {
                    return true;
                }
            }

        }
        return false;
    }

    public void processJoinResult(ReconfigureReply r) {
        logger.info("Received reconfigurable reply from TTP");
        logger.info("new view: " + r.getView());
//        tomLayer.execManager.stop();


        this.reconfigureTo(r.getView());

        String[] s = r.getJoinSet();

        this.lastJoinStet = new int[s.length];

        for (int i = 0; i < s.length; i++) {
            StringTokenizer str = new StringTokenizer(s[i], ":");
            int id = Integer.parseInt(str.nextToken());
            this.lastJoinStet[i] = id;
            String host = str.nextToken();
            int port = Integer.valueOf(str.nextToken());
            int portRR = Integer.valueOf(str.nextToken());
            this.getStaticConf().addHostInfo(id, host, port, portRR);
        }

    }


    @Override
    public final void reconfigureTo(View newView) {
        this.currentView = newView;
        getViewStore().storeView(this.currentView);
        if (newView.isMember(getStaticConf().getProcessId())) {
            //membro da view atual
            otherProcesses = new int[currentView.getProcesses().length - 1];
            int c = 0;
            for (int i = 0; i < currentView.getProcesses().length; i++) {
                if (currentView.getProcesses()[i] != getStaticConf().getProcessId()) {
                    otherProcesses[c++] = currentView.getProcesses()[i];
                }
            }

            this.quorumBFT = (int) Math.ceil((this.currentView.getN() + this.currentView.getF()) / 2);
            this.quorumCFT = (int) Math.ceil(this.currentView.getN() / 2);
        } else if (this.currentView != null && this.currentView.isMember(getStaticConf().getProcessId())) {
            //TODO: Left the system in newView -> LEAVE
            //CODE for LEAVE   
        } else {
            //TODO: Didn't enter the system yet

        }
    }

    /*public int getQuorum2F() {
        return quorum2F;
    }*/


    public int getQuorum() {
        return getStaticConf().isBFT() ? quorumBFT : quorumCFT;
    }
}
