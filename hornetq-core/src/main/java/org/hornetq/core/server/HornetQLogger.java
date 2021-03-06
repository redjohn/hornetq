/*
* JBoss, Home of Professional Open Source.
* Copyright 2010, Red Hat, Inc., and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.hornetq.core.server;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 *         3/8/12
 *
 * Logger Code 11
 *
 * each message id must be 6 digits long starting with 10, the 3rd digit donates the level so
 *
 * INF0  1
 * WARN  2
 * DEBUG 3
 * ERROR 4
 * TRACE 5
 * FATAL 6
 *
 * so an INFO message would be 101000 to 101999
 */

import org.hornetq.api.core.Interceptor;
import org.hornetq.api.core.Pair;
import org.hornetq.api.core.SimpleString;
import org.hornetq.core.client.impl.ServerLocatorInternal;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.journal.IOAsyncTask;
import org.hornetq.core.journal.SequentialFile;
import org.hornetq.core.journal.impl.JournalFile;
import org.hornetq.core.paging.cursor.PagePosition;
import org.hornetq.core.paging.cursor.PageSubscription;
import org.hornetq.core.persistence.OperationContext;
import org.hornetq.core.persistence.impl.journal.JournalStorageManager;
import org.hornetq.core.protocol.core.Packet;
import org.hornetq.core.protocol.stomp.StompConnection;
import org.hornetq.core.protocol.stomp.StompFrame;
import org.hornetq.core.server.cluster.Bridge;
import org.hornetq.core.server.cluster.impl.BridgeImpl;
import org.hornetq.core.server.cluster.impl.ClusterConnectionImpl;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.hornetq.core.server.impl.ServerSessionImpl;
import org.hornetq.spi.core.protocol.ProtocolType;
import org.hornetq.utils.FutureLatch;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.netty.channel.Channel;
import org.w3c.dom.Node;

import javax.transaction.xa.Xid;
import java.io.File;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@MessageLogger(projectCode = "HQ")
public interface HornetQLogger extends BasicLogger
{
   /**
    * The default logger.
    */
   HornetQLogger LOGGER = Logger.getMessageLogger(HornetQLogger.class, HornetQLogger.class.getPackage().getName());

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111001, value = "{0} server is starting with configuration {1}", format = Message.Format.MESSAGE_FORMAT)
   void serverStarting(String type, Configuration configuration);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111002, value = "{0} is already started, ignoring the call to start..", format = Message.Format.MESSAGE_FORMAT)
   void serverAlreadyStarted(String type);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111003, value = "HornetQ Server version {0} [{1}] {2}", format = Message.Format.MESSAGE_FORMAT)
   void serverStarted(String fullVersion, SimpleString nodeId, String identity);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111004, value = "HornetQ Server version {0} [{1}] stopped", format = Message.Format.MESSAGE_FORMAT)
   void serverStopped(String version, SimpleString nodeId);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111005, value = "trying to deploy queue {0}", format = Message.Format.MESSAGE_FORMAT)
   void deployQueue(SimpleString queueName);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111006, value = "{0}", format = Message.Format.MESSAGE_FORMAT)
   void dumpServerInfo(String serverInfo);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111007, value = "Deleting pending large message as it wasn't completed: {0}", format = Message.Format.MESSAGE_FORMAT)
   void deletingPendingMessage(Pair<Long, Long> msgToDelete);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111008, value = "Waiting to obtain live lock", format = Message.Format.MESSAGE_FORMAT)
   void awaitingLiveLock();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111009, value = "Server is now live", format = Message.Format.MESSAGE_FORMAT)
   void serverIsLive();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111010, value = "live server wants to restart, restarting server in backup", format = Message.Format.MESSAGE_FORMAT)
   void awaitFailBack();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 1111, value = "HornetQ Backup Server version {0} [{1}] started, waiting live to fail before it gets active",
         format = Message.Format.MESSAGE_FORMAT)
   void backupServerStarted(String version, SimpleString nodeID);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111012, value = "Backup Server is now live", format = Message.Format.MESSAGE_FORMAT)
   void backupServerIsLive();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111013, value = "Server {0} is now live", format = Message.Format.MESSAGE_FORMAT)
   void serverIsLive(String identity);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111014, value = "**** Dumping session creation stacks ****", format = Message.Format.MESSAGE_FORMAT)
   void dumpingSessionStacks();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111015, value = "session created", format = Message.Format.MESSAGE_FORMAT)
   void dumpingSessionStack(@Cause Exception e);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111016, value = "Using AIO Journal", format = Message.Format.MESSAGE_FORMAT)
   void journalUseAIO();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111017, value = "Using NIO Journal", format = Message.Format.MESSAGE_FORMAT)
   void journalUseNIO();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111018, value = "{0}% loaded", format = Message.Format.MESSAGE_FORMAT)
   void percentLoaded(Long percent);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111019, value = "Can't find queue {0} while reloading ACKNOWLEDGE_CURSOR, deleting record now", format = Message.Format.MESSAGE_FORMAT)
   void journalCannotFindQueueReloading(Long queueID);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111020, value = "Can't find queue {0} while reloading PAGE_CURSOR_COUNTER_VALUE, deleting record now", format = Message.Format.MESSAGE_FORMAT)
   void journalCannotFindQueueReloadingPage(Long queueID);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111021, value = "Can't find queue {0} while reloading PAGE_CURSOR_COUNTER_INC, deleting record now", format = Message.Format.MESSAGE_FORMAT)
   void journalCannotFindQueueReloadingPageCursor(Long queueID);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111022, value = "Large message: {0} didn't have any associated reference, file will be deleted", format = Message.Format.MESSAGE_FORMAT)
   void largeMessageWithNoRef(Long messageID);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111023, value = "Deleting unreferenced message id={0} from the journal", format = Message.Format.MESSAGE_FORMAT)
   void journalUnreferencedMessage(Long messageID);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111024, value = "Started Netty Acceptor version {0} {1}:{2} for {3} protocol", format = Message.Format.MESSAGE_FORMAT)
   void startedNettyAcceptor(String id, String host, Integer port, ProtocolType protocol);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111025, value = "failed to remove connection", format = Message.Format.MESSAGE_FORMAT)
   void errorRemovingConnection();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111026, value = "unable to start connector service: {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorStartingConnectorService(@Cause Throwable e, String name);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111027, value = "unable to stop connector service: {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorStoppingConnectorService(@Cause Throwable e, String name);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111028, value = "Backup server {0} is synchronized with live-server.", format = Message.Format.MESSAGE_FORMAT)
   void backupServerSynched(HornetQServerImpl server);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111029, value = "replication Journal {0}. Reserving fileIDs for synchronization: {1}", format = Message.Format.MESSAGE_FORMAT)
   void reservingFileIDs(JournalStorageManager.JournalContent content, String actContent);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111030, value = "Replication: sending {0} (size={1}) to backup. {2}", format = Message.Format.MESSAGE_FORMAT)
   void journalSynch(JournalFile jf, Long size, SequentialFile file);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111031, value = "Bridge {0} connected to fowardingAddress={1}. {2} doesn't have any bindings what means messages will be ignored until a binding is created.", format = Message.Format.MESSAGE_FORMAT)
   void bridgeNoBindings(SimpleString name, SimpleString forwardingAddress, SimpleString address);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111032, value =  "Bridge {0} is connected", format = Message.Format.MESSAGE_FORMAT)
   void bridgeConnected(BridgeImpl name);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111033, value =  "Bridge is stopping, will not retry", format = Message.Format.MESSAGE_FORMAT)
   void bridgeStopping();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111034, value =  "stopped bridge {0}", format = Message.Format.MESSAGE_FORMAT)
   void bridgeStopped(SimpleString name);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111035, value =  "paused bridge {0}", format = Message.Format.MESSAGE_FORMAT)
   void bridgePaused(SimpleString name);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111036, value =  "backup announced", format = Message.Format.MESSAGE_FORMAT)
   void backupAnnounced();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111037, value =  "Waiting to become backup node", format = Message.Format.MESSAGE_FORMAT)
   void waitingToBecomeBackup();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111038, value =  "** got backup lock", format = Message.Format.MESSAGE_FORMAT)
   void gotBackupLock();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111039, value =  "Waiting to obtain live lock", format = Message.Format.MESSAGE_FORMAT)
   void waitingToObtainLiveLock();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111040, value =  "Live Server Obtained live lock", format = Message.Format.MESSAGE_FORMAT)
   void obtainedLiveLock();

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111041, value =  "Message with duplicate ID {0} was already set at {1}. Move from {2} being ignored and message removed from {3}",
         format = Message.Format.MESSAGE_FORMAT)
   void messageWithDuplicateID(Object duplicateProperty, SimpleString toAddress, SimpleString address, SimpleString simpleString);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111042, value =  "Message with duplicate ID {0} was already set at {1}. Move from {2} being ignored",
         format = Message.Format.MESSAGE_FORMAT)
   void messageWithDuplicateID(Object duplicateProperty, SimpleString toAddress, SimpleString address);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111043, value =  "ignoring prepare on xid as already called :{0}", format = Message.Format.MESSAGE_FORMAT)
   void ignoringPrepare(Xid xid);

   @LogMessage(level = Logger.Level.INFO)
   @Message(id = 111044, value =  "{0} to become 'live'", format = Message.Format.MESSAGE_FORMAT)
   void becomingLive(HornetQServer server);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112001, value = "HornetQServer is being finalized and has not been stopped. Please remember to stop the server before letting it go out of scope",
         format = Message.Format.MESSAGE_FORMAT)
   void serverFinalisedWIthoutBeingSTopped();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112002, value = "Error closing sessions while stopping server", format = Message.Format.MESSAGE_FORMAT)
   void errorClosingSessionsWhileStoppingServer(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112003, value = "Timed out waiting for pool to terminate {0}. Interrupting all its threads!", format = Message.Format.MESSAGE_FORMAT)
   void timedOutStoppingThreadpool(ExecutorService service);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112004, value = "Must specify a name for each divert. This one will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void divertWithNoName();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112005, value = "Must specify an address for each divert. This one will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void divertWithNoAddress();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112006, value = "Must specify a forwarding address for each divert. This one will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void divertWithNoForwardingAddress();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112007, value = "Binding already exists with name {0}, divert will not be deployed", format = Message.Format.MESSAGE_FORMAT)
   void divertBindingNotExists(SimpleString bindingName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112008, value = "Security risk! HornetQ is running with the default cluster admin user and default password. "
         + "Please see the HornetQ user guide, cluster chapter, for instructions on how to change this.", format = Message.Format.MESSAGE_FORMAT)
   void clusterSecurityRisk();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112009, value = "unable to restart server, please kill and restart manually", format = Message.Format.MESSAGE_FORMAT)
   void serverRestartWarning();

   @LogMessage(level = Logger.Level.WARN)
   void serverRestartWarning(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112010, value = "Unable to announce backup for replication. Trying to stop the server.", format = Message.Format.MESSAGE_FORMAT)
   void replicationStartProblem(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112011, value = "Critical IO Error, shutting down the server. code={0}, message={1}", format = Message.Format.MESSAGE_FORMAT)
   void ioErrorShutdownServer(int code, String message);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112012, value = "Error stopping server", format = Message.Format.MESSAGE_FORMAT)
   void errorStoppingServer(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112013, value = "Timed out waiting for backup activation to exit", format = Message.Format.MESSAGE_FORMAT)
   void backupActivationProblem();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112014, value = "Error when trying to start replication", format = Message.Format.MESSAGE_FORMAT)
   void errorStartingReplication(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112015, value = "Error when trying to stop replication", format = Message.Format.MESSAGE_FORMAT)
   void errorStoppingReplication(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112016, value = "{0}", format = Message.Format.MESSAGE_FORMAT)
   void warn(String message);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112017, value = "Error on clearing messages", format = Message.Format.MESSAGE_FORMAT)
   void errorClearingMessages(@Cause Throwable e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112018, value = "Timed out waiting for handler to complete processing", format = Message.Format.MESSAGE_FORMAT)
   void timeOutWaitingForProcessing();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112019, value = "Unable to close session", format = Message.Format.MESSAGE_FORMAT)
   void unableToCloseSession(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112020, value = "Failed to connect to server.", format = Message.Format.MESSAGE_FORMAT)
   void failedToConnectToServer();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112021, value = "Tried {0} times to connect. Now giving up on reconnecting it.", format = Message.Format.MESSAGE_FORMAT)
   void failedToConnectToServer(Integer reconnectAttempts);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112022, value = "Waiting {0} milliseconds before next retry. RetryInterval={1} and multiplier={2}", format = Message.Format.MESSAGE_FORMAT)
   void waitingForRetry(Long interval, Long retryInterval, Double multiplier);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112023, value =    "connector.create or connectorFactory.createConnector should never throw an exception, implementation is badly behaved, but we'll deal with it anyway."
               , format = Message.Format.MESSAGE_FORMAT)
   void createConnectorException(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112024, value = "I'm closing a core ClientSessionFactory you left open. Please make sure you close all ClientSessionFactories explicitly " + "before letting them go out of scope! {0}"
               , format = Message.Format.MESSAGE_FORMAT)
   void factoryLeftOpen(@Cause Exception e, int i);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112025, value = "resetting session after failure", format = Message.Format.MESSAGE_FORMAT)
   void resettingSessionAfterFailure();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112026, value = "Server is starting, retry to create the session {0}", format = Message.Format.MESSAGE_FORMAT)
   void retryCreateSessionSeverStarting(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112027, value = "committing transaction after failover occurred, any non persistent messages may be lost", format = Message.Format.MESSAGE_FORMAT)
   void commitAfterFailover();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112028, value = "failover occured during commit throwing XAException.XA_RETRY", format = Message.Format.MESSAGE_FORMAT)
   void failoverDuringCommit();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112029, value = "failover occurred during prepare re-trying", format = Message.Format.MESSAGE_FORMAT)
   void failoverDuringPrepare();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112030, value = "failover occurred during prepare rolling back", format = Message.Format.MESSAGE_FORMAT)
   void failoverDuringPrepareRollingBack();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112031, value = "failover occurred during prepare rolling back", format = Message.Format.MESSAGE_FORMAT)
   void errorDuringPrepare(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112032, value = "I'm closing a core ClientSession you left open. Please make sure you close all ClientSessions explicitly before letting them go out of scope! {0}", format = Message.Format.MESSAGE_FORMAT)
   void clientSessionNotClosed(@Cause Exception e, int identity);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112033, value = "error adding packet", format = Message.Format.MESSAGE_FORMAT)
   void errorAddingPacket(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112034, value = "error calling cancel", format = Message.Format.MESSAGE_FORMAT)
   void errorCallingCancel(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112035, value = "error reading index", format = Message.Format.MESSAGE_FORMAT)
   void errorReadingIndex(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112036, value = "error setting index", format = Message.Format.MESSAGE_FORMAT)
   void errorSettingIndex(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112037, value = "error resetting index", format = Message.Format.MESSAGE_FORMAT)
   void errorReSettingIndex(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112038, value = "error reading LargeMessage file cache", format = Message.Format.MESSAGE_FORMAT)
   void errorReadingCache(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112039, value = "error closing LargeMessage file cache", format = Message.Format.MESSAGE_FORMAT)
   void errorClosingCache(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112040, value = "Exception during finalization for LargeMessage file cache", format = Message.Format.MESSAGE_FORMAT)
   void errorFinalisingCache(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112041, value = "did not connect the cluster connection to other nodes", format = Message.Format.MESSAGE_FORMAT)
   void errorConnectingToNodes(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112042, value = "Timed out waiting for pool to terminate", format = Message.Format.MESSAGE_FORMAT)
   void timedOutWaitingForTermination();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112043, value = "Timed out waiting for scheduled pool to terminate", format = Message.Format.MESSAGE_FORMAT)
   void timedOutWaitingForScheduledPoolTermination();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112044, value = "error starting server locator", format = Message.Format.MESSAGE_FORMAT)
   void errorStartingLocator(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112045, value = "I'm closing a Server Locator you left open. Please make sure you close all Server Locators explicitly before letting them go out of scope! {0}", format = Message.Format.MESSAGE_FORMAT)
   void serverLocatorNotClosed(@Cause Exception e, int identity);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112046, value = "error sending topology", format = Message.Format.MESSAGE_FORMAT)
   void errorSendingTopology(@Cause Throwable e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112047, value = "error sending topology", format = Message.Format.MESSAGE_FORMAT)
   void errorSendingTopologyNodedown(@Cause Throwable e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112048, value = "Timed out waiting to stop discovery thread", format = Message.Format.MESSAGE_FORMAT)
   void timedOutStoppingDiscovery();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112049, value = "unable to send notification when discovery group is stopped", format = Message.Format.MESSAGE_FORMAT)
   void errorSendingNotifOnDiscoveryStop(@Cause Throwable e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112050, value = "There are more than one servers on the network broadcasting the same node id. "
                     + "You will see this message exactly once (per node) if a node is restarted, in which case it can be safely "
                     + "ignored. But if it is logged continuously it means you really do have more than one node on the same network "
                     + "active concurrently with the same node id. This could occur if you have a backup node active at the same time as "
                     + "its live node. nodeID={0}",
         format = Message.Format.MESSAGE_FORMAT)
   void multipleServersBroadcastingSameNode(String nodeId);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112051, value = "error receiving packet in discovery", format = Message.Format.MESSAGE_FORMAT)
   void errorReceivingPAcketInDiscovery(@Cause Throwable e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112052, value = "Cannot deploy a connector with no name specified.", format = Message.Format.MESSAGE_FORMAT)
   void connectorWithNoName();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112053, value = "There is already a connector with name {0} deployed. This one will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void connectorAlreadyDeployed(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112054, value = "AIO wasn't located on this platform, it will fall back to using pure Java NIO. If your platform is Linux, install LibAIO to enable the AIO journal", format = Message.Format.MESSAGE_FORMAT)
   void AIONotFound();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112055, value = "There is already a discovery group with name {0} deployed. This one will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void discoveryGroupAlreadyDeployed(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112056, value = "error scanning for URL's", format = Message.Format.MESSAGE_FORMAT)
   void errorScanningURLs(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112057, value = "problem undeploying {0}", format = Message.Format.MESSAGE_FORMAT)
   void problemUndeployingNode(@Cause Exception e, Node node);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112058, value = "Timed out waiting for paging cursor to stop {0} {1}", format = Message.Format.MESSAGE_FORMAT)
   void timedOutStoppingPagingCursor(FutureLatch future, Executor executor);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112059, value = "Timed out flushing executors for paging cursor to stop {0} {1}", format = Message.Format.MESSAGE_FORMAT)
   void timedOutFlushingExecutorsPagingCursor(FutureLatch future, Executor executor);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112060, value = "problem cleaning page address {0}", format = Message.Format.MESSAGE_FORMAT)
   void problemCleaningPageAddress(@Cause Exception e, SimpleString address);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112061, value = "Couldn't complete operations on IO context {0}", format = Message.Format.MESSAGE_FORMAT)
   void problemCompletingOperations(OperationContext e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112062, value = "Problem cleaning page subscription counter", format = Message.Format.MESSAGE_FORMAT)
   void problemCleaningPagesubscriptionCounter(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112063, value = "Error on cleaning up cursor pages", format = Message.Format.MESSAGE_FORMAT)
   void problemCleaningCursorPages(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112064, value = "Couldn't remove page {0} from consumed pages on cursor for address {1}", format = Message.Format.MESSAGE_FORMAT)
   void problemRemovingCursorPages(Long pageId, SimpleString address);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112065, value = "Timed out flushing executors for paging cursor to stop {0}", format = Message.Format.MESSAGE_FORMAT)
   void timedOutFlushingExecutorsPagingCursor(PageSubscription pageSubscription);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112066, value = "Couldn't find page cache for page {0} removing it from the journal", format = Message.Format.MESSAGE_FORMAT)
   void pageNotFound(PagePosition pos);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112067, value = "Couldn't locate page transaction {0}, ignoring message on position {1} on address={2} queue={3}",
         format = Message.Format.MESSAGE_FORMAT)
   void pageSubscriptionCouldntLoad(long transactionID, PagePosition position, SimpleString address, SimpleString name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112068, value = "File {0} being renamed to {1}.invalidPage as it was loaded partially. Please verify your data.", format = Message.Format.MESSAGE_FORMAT)
   void pageInvalid(String fileName, String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112069, value = "Error while deleting page file", format = Message.Format.MESSAGE_FORMAT)
   void pageDeleteError(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112070, value = "page finalise error", format = Message.Format.MESSAGE_FORMAT)
   void pageFinaliseError(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112071, value = "Page file had incomplete records at position {0} at record number {1}", format = Message.Format.MESSAGE_FORMAT)
   void pageSuspectFile(int position, int msgNumber);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112072, value = "Can't delete page transaction id={0}", format = Message.Format.MESSAGE_FORMAT)
   void pageTxDeleteError(@Cause Exception e, long recordID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112073, value = "Directory {0} didn't have an identification file {1}", format = Message.Format.MESSAGE_FORMAT)
   void pageStoreFactoryNoIdFile(String s, String addressFile);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112074, value = "Timed out on waiting PagingStore {0} to shutdown", format = Message.Format.MESSAGE_FORMAT)
   void pageStoreTimeout(SimpleString address);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112075, value = "IO Error, impossible to start paging", format = Message.Format.MESSAGE_FORMAT)
   void pageStoreStartIOError(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112076, value = "Starting paging on {0}, size = {1}, maxSize={2}", format = Message.Format.MESSAGE_FORMAT)
   void pageStoreStart(SimpleString storeName, long addressSize, long maxSize);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112077, value = "Messages are being dropped on address {0}", format = Message.Format.MESSAGE_FORMAT)
   void pageStoreDropMessages(SimpleString storeName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112078, value = "Server is stopped", format = Message.Format.MESSAGE_FORMAT)
   void serverIsStopped();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112079, value = "Cannot find queue {0} to update delivery count", format = Message.Format.MESSAGE_FORMAT)
   void journalCannotFindQueueDelCount(Long queueID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112080, value = "Cannot find message {0} to update delivery count", format = Message.Format.MESSAGE_FORMAT)
   void journalCannotFindMessageDelCount(Long msg);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112081, value = "Message for queue {0} which does not exist. This message will be ignored.", format = Message.Format.MESSAGE_FORMAT)
   void journalCannotFindQueueForMessage(Long queueID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112082, value = "It wasn't possible to delete message {0}", format = Message.Format.MESSAGE_FORMAT)
   void journalErrorDeletingMessage(@Cause Exception e, Long messageID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112083, value = "Message in prepared tx for queue {0} which does not exist. This message will be ignored.", format = Message.Format.MESSAGE_FORMAT)
   void journalMessageInPreparedTX(Long queueID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112084, value = "Failed to remove reference for {0}", format = Message.Format.MESSAGE_FORMAT)
   void journalErrorRemovingRef(Long messageID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112085, value = "Can't find queue {0} while reloading ACKNOWLEDGE_CURSOR", format = Message.Format.MESSAGE_FORMAT)
   void journalCannotFindQueueReloadingACK(Long queueID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112086, value = "PAGE_CURSOR_COUNTER_VALUE record used on a prepared statement, invalid state", format = Message.Format.MESSAGE_FORMAT)
   void journalPAGEOnPrepared();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112087, value = "InternalError: Record type {0} not recognized. Maybe you're using journal files created on a different version", format = Message.Format.MESSAGE_FORMAT)
   void journalInvalidRecordType(Byte recordType);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112088, value = "can't locate recordType={0} on loadPreparedTransaction//deleteRecords", format = Message.Format.MESSAGE_FORMAT)
   void journalInvalidRecordTypeOnPreparedTX(Byte recordType);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112089, value = "Journal Error", format = Message.Format.MESSAGE_FORMAT)
   void journalError(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112090, value = "error incrementing delay detection", format = Message.Format.MESSAGE_FORMAT)
   void errorIncrementDelayDeletionCount(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112091, value = "Error on copying large message {0} for DLA or Expiry", format = Message.Format.MESSAGE_FORMAT)
   void lareMessageErrorCopying(@Cause Exception e, LargeServerMessage largeServerMessage);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112092, value = "Error on executing IOAsyncTask", format = Message.Format.MESSAGE_FORMAT)
   void errorExecutingIOAsyncTask(@Cause Throwable t);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112093, value = "Error on deleting duplicate cache", format = Message.Format.MESSAGE_FORMAT)
   void errorDeletingDuplicateCache(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112094, value = "Reaper thread being restarted", format = Message.Format.MESSAGE_FORMAT)
   void reaperRestarted();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112095, value = "Did not route to any bindings for address {0} and sendToDLAOnNoRoute is true " +
                                 "but there is no DLA configured for the address, the message will be ignored.",
         format = Message.Format.MESSAGE_FORMAT)
   void noDLA(SimpleString address);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112096, value = "It wasn't possible to add references due to an IO error code {0} message = {1}", format = Message.Format.MESSAGE_FORMAT)
   void ioErrorAddingReferences(Integer errorCode, String errorMessage);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112097, value = "Duplicate message detected through the bridge - message will not be routed. Message information:\n{0}", format = Message.Format.MESSAGE_FORMAT)
   void duplicateMessageDetectedThruBridge(ServerMessage message);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112098, value = "Duplicate message detected - message will not be routed. Message information:\n{0}", format = Message.Format.MESSAGE_FORMAT)
   void duplicateMessageDetected(ServerMessage message);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112099, value = "Error while confirming large message completion on rollback for recordID={0}", format = Message.Format.MESSAGE_FORMAT)
   void journalErrorConfirmingLargeMessage(@Cause Throwable e, Long messageID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112100, value = "Client connection failed, clearing up resources for session {0}", format = Message.Format.MESSAGE_FORMAT)
   void clientConnectionFailed(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112101, value = "Cleared up resources for session {0}", format = Message.Format.MESSAGE_FORMAT)
   void clearingUpSession(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112102, value = "Error processing IOCallback code = {0} message = {1}", format = Message.Format.MESSAGE_FORMAT)
   void errorProcessingIOCallback(Integer errorCode, String errorMessage);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112103, value = "Can't find packet to clear: {0} last received command id first stored command id {1}",
         format = Message.Format.MESSAGE_FORMAT)
   void cannotFindPacketToClear(Integer lastReceivedCommandID, Integer firstStoredCommandID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112104, value = "Client with version {0} and address {1} is not compatible with server version {2}. " +
                     "Please ensure all clients and servers are upgraded to the same version for them to interoperate properly",
         format = Message.Format.MESSAGE_FORMAT)
   void incompatibleVersion(Integer version, String remoteAddress, String fullVersion);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112105, value = "Client is not being consistent on the request versioning. It just sent a version id={0}" +
            		   " while it informed {1} previously", format = Message.Format.MESSAGE_FORMAT)
   void incompatibleVersionAfterConnect(int version, int clientVersion);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112106, value = "Reattach request from {0} failed as there is no confirmationWindowSize configured, which may be ok for your system", format = Message.Format.MESSAGE_FORMAT)
   void reattachRequestFailed(String remoteAddress);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112107, value = "Connection failure has been detected: {0} [code={1}]", format = Message.Format.MESSAGE_FORMAT)
   void connectionFailureDetected(String message, Integer code);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112108, value = "Failure in calling interceptor: {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorCallingInterceptor(@Cause Throwable e, Interceptor interceptor);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112109, value = "connection closed {0}", format = Message.Format.MESSAGE_FORMAT)
   void connectionClosed(StompConnection connection);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112110, value = "error cleaning up stomp connection", format = Message.Format.MESSAGE_FORMAT)
   void errorCleaningStompConn(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112111, value = "Stomp Transactional acknowledgement is not supported", format = Message.Format.MESSAGE_FORMAT)
   void stompTXAckNorSupported();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112112, value = "Interrupted while waiting for stomp heart beate to die", format = Message.Format.MESSAGE_FORMAT)
   void errorOnStompHeartBeat(@Cause InterruptedException e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112113, value = "Timed out flushing channel on InVMConnection", format = Message.Format.MESSAGE_FORMAT)
   void timedOutFlushingInvmChannel();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112114, value = "Unexpected Netty Version was expecting {0} using {1} Version.ID", format = Message.Format.MESSAGE_FORMAT)
   void unexpectedNettyVersion(String nettyVersion, String id);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112115, value = "channel group did not completely close", format = Message.Format.MESSAGE_FORMAT)
   void nettyChannelGroupError();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112116, value = "{0} is still connected to {1}", format = Message.Format.MESSAGE_FORMAT)
   void nettyChannelStillOpen(Channel channel, SocketAddress remoteAddress);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112117, value = "channel group did not completely unbind", format = Message.Format.MESSAGE_FORMAT)
   void nettyChannelGroupBindError();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112118, value = "{0} is still bound to {1}", format = Message.Format.MESSAGE_FORMAT)
   void nettyChannelStillBound(Channel channel, SocketAddress remoteAddress);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112119, value = "Timed out waiting for netty ssl close future to complete", format = Message.Format.MESSAGE_FORMAT)
   void timeoutClosingSSL();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112120, value = "Timed out waiting for netty channel to close", format = Message.Format.MESSAGE_FORMAT)
   void timeoutClosingNettyChannel();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112121, value = "Timed out waiting for packet to be flushed", format = Message.Format.MESSAGE_FORMAT)
   void timeoutFlushingPacket();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112122, value = "Error instantiating remoting interceptor {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorCreatingRemotingInterceptor(@Cause Exception e, String interceptorClass);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112123, value = "The following keys are invalid for configuring the acceptor: {0} the acceptor will not be started.",
         format = Message.Format.MESSAGE_FORMAT)
   void invalidAcceptorKeys(String s);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112124, value = "Error instantiating remoting acceptor {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorCreatingAcceptor(@Cause Exception e, String factoryClassName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112125, value = "Timed out waiting for remoting thread pool to terminate", format = Message.Format.MESSAGE_FORMAT)
   void timeoutRemotingThreadPool();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112126, value = "error on connection failure check", format = Message.Format.MESSAGE_FORMAT)
   void errorOnFailureCheck(@Cause Throwable e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112127, value = "The following keys are invalid for configuring the connector service: {0} the connector will not be started.",
         format = Message.Format.MESSAGE_FORMAT)
   void connectorKeysInvalid(String s);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112128, value = "The following keys are required for configuring the connector service: {0} the connector will not be started.",
         format = Message.Format.MESSAGE_FORMAT)
   void connectorKeysMissing(String s);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112129, value = "Packet {0} can't be processed by the ReplicationEndpoint", format = Message.Format.MESSAGE_FORMAT)
   void invalidPacketForReplication(Packet packet);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112130, value = "error handling packet {0} for replication", format = Message.Format.MESSAGE_FORMAT)
   void errorHandlingReplciationPacket(@Cause Exception e, Packet packet);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112131, value = "Replication Error while closing the page on backup", format = Message.Format.MESSAGE_FORMAT)
   void errorClosingPageOnReplication(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112132, value = "Journal comparison mismatch:\n{0}", format = Message.Format.MESSAGE_FORMAT)
   void journalcomparisonMismatch(String s);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112133, value = "Replication Error deleting large message ID = {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorDeletingLargeMessage(@Cause Exception e, long messageId);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112134, value = "Replication Large MessageID {0}  is not available on backup server. Ignoring replication message", format = Message.Format.MESSAGE_FORMAT)
   void largeMessageNotAvailable(long messageId);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112135, value = "Error completing callback on replication manager", format = Message.Format.MESSAGE_FORMAT)
   void errorCompletingReplicationCallback(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112136, value = "The backup node has been shut-down, replication will now stop", format = Message.Format.MESSAGE_FORMAT)
   void replicationStopOnBackupShutdown();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112137, value = "Connection to the backup node failed, removing replication now", format = Message.Format.MESSAGE_FORMAT)
   void replicationStopOnBackupFail(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112138, value = "Timed out waiting to stop Bridge", format = Message.Format.MESSAGE_FORMAT)
   void timedOutWaitingToStopBridge();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112139, value = "Bridge unable to send notification when broadcast group is stopped", format = Message.Format.MESSAGE_FORMAT)
   void bridgeNotificationOnGroupStopped(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112140, value = "Bridge unable to send message {0}, will try again once bridge reconnects", format = Message.Format.MESSAGE_FORMAT)
   void bridgeUnableToSendMessage(@Cause Exception e, MessageReference ref);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112141, value = "Connection failed with failedOver={1}", format = Message.Format.MESSAGE_FORMAT)
   void bridgeConnectionFailed(@Cause Exception e, Boolean failedOver);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112142, value = "Error on querying binding on bridge {0}. Retrying in 100 milliseconds", format = Message.Format.MESSAGE_FORMAT)
   void errorQueryingBridge(@Cause Throwable t, SimpleString name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112143, value = "Address {0} doesn't have any bindings yet, retry #({1})", format = Message.Format.MESSAGE_FORMAT)
   void errorQueryingBridge(SimpleString address, Integer retryCount);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112144, value = "Server is starting, retry to create the session for bridge {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorStartingBridge(SimpleString name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112145, value = "Bridge {0} is unable to connect to destination. It will be disabled.", format = Message.Format.MESSAGE_FORMAT)
   void errorConnectingBridge(@Cause Exception e, Bridge bridge);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112146, value = "ServerLocator was shutdown, can't retry on opening connection for bridge", format = Message.Format.MESSAGE_FORMAT)
   void bridgeLocatorShutdown();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112147, value =  "Bridge {0} achieved {1} maxattempts={2} it will stop retrying to reconnect", format = Message.Format.MESSAGE_FORMAT)
   void bridgeAbortStart(SimpleString name, Integer retryCount, Integer reconnectAttempts);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112148, value = "Unexpected exception while trying to reconnect", format = Message.Format.MESSAGE_FORMAT)
   void errorReConnecting(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112149, value = "transaction with xid {0} timed out", format = Message.Format.MESSAGE_FORMAT)
   void unexpectedXid(Xid xid);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112150, value = "IO Error completing the transaction, code = {0}, message = {1}", format = Message.Format.MESSAGE_FORMAT)
   void ioErrorOnTX(Integer errorCode, String errorMessage);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112151, value = "Property {0} must be an Integer, it is {1}", format = Message.Format.MESSAGE_FORMAT)
   void propertyNotInteger(String propName, String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112152, value = "Property {0} must be an Long, it is {1}", format = Message.Format.MESSAGE_FORMAT)
   void propertyNotLong(String propName, String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112153, value = "Property {0} must be an Boolean, it is {1}", format = Message.Format.MESSAGE_FORMAT)
   void propertyNotBoolean(String propName, String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112154, value = "Cannot find hornetq-version.properties on classpath: {1}", format = Message.Format.MESSAGE_FORMAT)
   void noVersionOnClasspath(String classpath);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112155, value = "Warning: JVM allocated more data what would make results invalid {0}:{1}", format = Message.Format.MESSAGE_FORMAT)
   void jvmAllocatedMoreMemory(Long totalMemory1, Long totalMemory2);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112156, value = "Couldn't finish context execution in 10 seconds", format = Message.Format.MESSAGE_FORMAT)
   void errorCompletingContext(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112157, value = "Replacing incomplete LargeMessage with ID={0}", format = Message.Format.MESSAGE_FORMAT)
   void replacingIncompleteLargeMessage(Long messageID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112158, value = "Cleared up resources for session {0}", format = Message.Format.MESSAGE_FORMAT)
   void clientConnectionFailedClearingSession(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112159, value = "local-bind-address specified for broadcast group but no local-bind-port specified so socket will NOT be bound to a local address/port",
         format = Message.Format.MESSAGE_FORMAT)
   void broadcastGroupBindError();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112160, value = "unable to send notification when broadcast group is stopped",
         format = Message.Format.MESSAGE_FORMAT)
   void broadcastGroupClosed(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112161, value = "NodeID={0} is not available on the topology. Retrying the connection to that node now", format = Message.Format.MESSAGE_FORMAT)
   void nodeNotAvailable(String targetNodeID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112162, value = "no queue IDs defined!,  originalMessage  = {0}, copiedMessage = {1}, props={2}",
         format = Message.Format.MESSAGE_FORMAT)
   void noQueueIdDefined(ServerMessage message, ServerMessage messageCopy, SimpleString idsHeaderName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112163, value = "exception while invoking {0} on {1}",
         format = Message.Format.MESSAGE_FORMAT)
   void managementOperationError(@Cause Exception e, String op, String resourceName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112164, value = "exception while retrieving attribute {0} on {1}",
         format = Message.Format.MESSAGE_FORMAT)
   void managementAttributeError(@Cause Exception e, String att, String resourceName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112165, value = "On ManagementService stop, there are {0} unexpected registered MBeans: {1}",
         format = Message.Format.MESSAGE_FORMAT)
   void managementStopError(Integer size, List<String> unexpectedResourceNames);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112166, value = "Unable to delete group binding info {0}",
         format = Message.Format.MESSAGE_FORMAT)
   void unableToDeleteGroupBindings(@Cause Exception e, SimpleString groupId);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112167, value = "Error closing serverLocator={0}",
         format = Message.Format.MESSAGE_FORMAT)
   void errorClosingServerLocator(@Cause Exception e, ServerLocatorInternal clusterLocator);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112168, value = "unable to start broadcast group {0}", format = Message.Format.MESSAGE_FORMAT)
   void unableToStartBroadcastGroup(@Cause Exception e, String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112169, value = "unable to start cluster connection {0}", format = Message.Format.MESSAGE_FORMAT)
   void unableToStartClusterConnection(@Cause Exception e, SimpleString name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112170, value = "unable to start Bridge {0}", format = Message.Format.MESSAGE_FORMAT)
   void unableToStartBridge(@Cause Exception e, SimpleString name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112171, value = "No connector with name '{0}'. backup cannot be announced.", format = Message.Format.MESSAGE_FORMAT)
   void announceBackupNoConnector(String connectorName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112172, value = "no cluster connections defined, unable to announce backup", format = Message.Format.MESSAGE_FORMAT)
   void announceBackupNoClusterConnections();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112173, value =  "Must specify a unique name for each bridge. This one will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void bridgeNotUnique();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112174, value =  "Must specify a queue name for each bridge. This one will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void bridgeNoQueue();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112175, value =  "Bridge Forward address is not specified. Will use original message address instead", format = Message.Format.MESSAGE_FORMAT)
   void bridgeNoForwardAddress();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112176, value =  "There is already a bridge with name {0} deployed. This one will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void bridgeAlreadyDeployed(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112177, value =   "No queue found with name {0} bridge will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void bridgeNoQueue(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112178, value =   "No discovery group found with name {0} bridge will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void bridgeNoDiscoveryGroup(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112179, value =  "Must specify a unique name for each cluster connection. This one will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void clusterConnectionNotUnique();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112180, value =  "Must specify an address for each cluster connection. This one will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void clusterConnectionNoForwardAddress();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112181, value =  "No connector with name '{0}'. The cluster connection will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void clusterConnectionNoConnector(String connectorName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112182, value =   "Cluster Configuration  '{0}' already exists. The cluster connection will not be deployed." , format = Message.Format.MESSAGE_FORMAT)
   void clusterConnectionAlreadyExists(String connectorName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112183, value =   "No discovery group with name '{0}'. The cluster connection will not be deployed." , format = Message.Format.MESSAGE_FORMAT)
   void clusterConnectionNoDiscoveryGroup(String discoveryGroupName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112184, value =   "There is already a broadcast-group with name {0} deployed. This one will not be deployed." , format = Message.Format.MESSAGE_FORMAT)
   void broadcastGroupAlreadyExists(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112185, value =   "There is no connector deployed with name '{0}'. The broadcast group with name '{1}' will not be deployed."  , format = Message.Format.MESSAGE_FORMAT)
   void broadcastGroupNoConnector(String connectorName, String bgName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112186, value =   "No connector defined with name '{0}'. The bridge will not be deployed.", format = Message.Format.MESSAGE_FORMAT)
   void bridgeNoConnector(String name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112187, value =   "Stopping Redistributor, Timed out waiting for tasks to complete", format = Message.Format.MESSAGE_FORMAT)
   void errorStoppingRedistributor();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112188, value =   "IO Error during redistribution, errorCode = {0} message = {1}", format = Message.Format.MESSAGE_FORMAT)
   void ioErrorRedistributing(Integer errorCode, String errorMessage);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112189, value =   "Unable to announce backup, retrying", format = Message.Format.MESSAGE_FORMAT)
   void errorAnnouncingBackup(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112190, value =   "Local Member is not set at on ClusterConnection {0}", format = Message.Format.MESSAGE_FORMAT)
   void noLocalMemborOnClusterConnection(ClusterConnectionImpl clusterConnection);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112191, value =   "{0}::Remote queue binding {1} has already been bound in the post office. Most likely cause for this is you have a loop " +
                                   "in your cluster due to cluster max-hops being too large or you have multiple cluster connections to the same nodes using overlapping addresses",
         format = Message.Format.MESSAGE_FORMAT)
   void remoteQueueAlreadyBoundOnClusterConnection(Object messageFlowRecord, SimpleString clusterName);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112192, value =   "problem closing backup session factory for cluster connection", format = Message.Format.MESSAGE_FORMAT)
   void errorClosingBackupFactoryOnClusterConnection(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112193, value =   "Node Manager can't open file {0}", format = Message.Format.MESSAGE_FORMAT)
   void nodeManagerCantOpenFile(@Cause Exception e, File file);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112194, value =   "Error on resetting large message deliver - {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorResttingLargeMessage(@Cause Throwable e, Object deliverer);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112195, value =   "Timed out waiting for executor to complete", format = Message.Format.MESSAGE_FORMAT)
   void errorTransferringConsumer();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112196, value =   "Queue Couldn't finish waiting executors. Try increasing the thread pool size", format = Message.Format.MESSAGE_FORMAT)
   void errorFlushingExecutorsOnQueue();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112197, value =   "Error expiring reference {0} 0n queue", format = Message.Format.MESSAGE_FORMAT)
   void errorExpiringReferencesOnQueue(@Cause Exception e, MessageReference ref);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112198, value =   "Message has expired. No bindings for Expiry Address {0} so dropping it", format = Message.Format.MESSAGE_FORMAT)
   void errorExpiringReferencesNoBindings(SimpleString expiryAddress);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112199, value =   "Message has expired. No expiry queue configured for queue {0} so dropping it", format = Message.Format.MESSAGE_FORMAT)
   void errorExpiringReferencesNoQueue(SimpleString name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112200, value =    "Message {0} has exceeded max delivery attempts. No bindings for Dead Letter Address {1} so dropping it" ,
         format = Message.Format.MESSAGE_FORMAT)
   void messageExceededMaxDelivery(MessageReference ref, SimpleString name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112201, value =    "Message {0} has reached maximum delivery attempts, sending it to Dead Letter Address {1} from {2}",
         format = Message.Format.MESSAGE_FORMAT)
   void messageExceededMaxDeliverySendtoDLA(MessageReference ref, SimpleString name, SimpleString simpleString);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112202, value =     "Message has exceeded max delivery attempts. No Dead Letter Address configured for queue {0} so dropping it",
         format = Message.Format.MESSAGE_FORMAT)
   void messageExceededMaxDeliveryNoDLA(SimpleString name);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112203, value =     "removing consumer which did not handle a message, consumer={0}, message={1}",
         format = Message.Format.MESSAGE_FORMAT)
   void removingBadConsumer(@Cause Throwable e, Consumer consumer, MessageReference reference);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112204, value =     "Unable to decrement reference counting on queue" ,
         format = Message.Format.MESSAGE_FORMAT)
   void errorDecrementingRefCount(@Cause Throwable e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112205, value =     "Unable to remove message id = {0} please remove manually" ,
         format = Message.Format.MESSAGE_FORMAT)
   void errorRemovingMessage(@Cause Throwable e, Long messageID);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112206, value =     "Error checking DLQ" ,
         format = Message.Format.MESSAGE_FORMAT)
   void errorCheckingDLQ(@Cause Throwable e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112207, value =     "Failed to register as backup. Stopping the server."  ,
         format = Message.Format.MESSAGE_FORMAT)
   void errorRegisteringBackup();

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112208, value =     "Less than {0}%\n{1}\nYou are in danger of running out of RAM. Have you set paging parameters " +
                                          "on your addresses? (See user manual \"Paging\" chapter)"  ,
         format = Message.Format.MESSAGE_FORMAT)
   void memoryError(Integer memoryWarningThreshold, String info);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112209, value = "Error completing callback on replication manager"  ,
         format = Message.Format.MESSAGE_FORMAT)
   void errorCompletingCAllbackOnRepicationManager(@Cause Throwable e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112210, value = "{0} backup activation thread did not finish." ,
         format = Message.Format.MESSAGE_FORMAT)
   void backupActivationDidntFinish(HornetQServer server);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112211, value = "unable to send notification when broadcast group is stopped" ,
         format = Message.Format.MESSAGE_FORMAT)
   void broadcastBridgeStoppedError(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112212, value = "unable to send notification when broadcast group is stopped",
         format = Message.Format.MESSAGE_FORMAT)
   void notificationBridgeStoppedError(@Cause Exception e);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112213, value = "Could not bind to {0} ({1} address); " +
         "make sure your discovery group-address is of the same type as the IP stack (IPv4 or IPv6)." +
         "\nIgnoring discovery group-address, but this may lead to cross talking.",
         format = Message.Format.MESSAGE_FORMAT)
   void ioDiscoveryError(String hostAddress, String s);


   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112214, value = "Group Handler timed-out waiting for sendCondition",
         format = Message.Format.MESSAGE_FORMAT)
   void groupHandlerSendTimeout();

    @LogMessage(level = Logger.Level.WARN)
   @Message(id = 112215, value = "Compressed large message tried to read {0} bytes from stream {1}",
         format = Message.Format.MESSAGE_FORMAT)
   void compressedLargeMessageError(int length, int nReadBytes);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114001, value = "Failed to call onMessage", format = Message.Format.MESSAGE_FORMAT)
   void onMessageError(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114002, value = "Failure in initialisation", format = Message.Format.MESSAGE_FORMAT)
   void initializationError(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114003, value = "failed to cleanup session", format = Message.Format.MESSAGE_FORMAT)
   void failedToCleanupSession(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114004, value = "Failed to execute failure listener", format = Message.Format.MESSAGE_FORMAT)
   void failedToExecuteListener(@Cause Throwable t);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114005, value = "Failed to handle failover", format = Message.Format.MESSAGE_FORMAT)
   void failedToHandleFailover(@Cause Throwable t);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114006, value = "XA end operation failed ", format = Message.Format.MESSAGE_FORMAT)
   void errorCallingEnd(@Cause Throwable t);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114007, value = "XA start operation failed {0} code:{1}", format = Message.Format.MESSAGE_FORMAT)
   void errorCallingStart(String message, Integer code);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114008, value = "Session is not XA", format = Message.Format.MESSAGE_FORMAT)
   void sessionNotXA();

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114009, value = "Received exception asynchronously from server", format = Message.Format.MESSAGE_FORMAT)
   void receivedExceptionAsynchronously(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114010, value = "Failed to handle packet", format = Message.Format.MESSAGE_FORMAT)
   void failedToHandlePacket(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114011, value = "Failed to stop discovery group", format = Message.Format.MESSAGE_FORMAT)
   void failedToStopDiscovery(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114012, value = "Failed to start discovery group", format = Message.Format.MESSAGE_FORMAT)
   void failedToStartDiscovery(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114013, value = "Failed to receive datagram", format = Message.Format.MESSAGE_FORMAT)
   void failedToReceiveDatagramInDiscovery(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114014, value = "Failed to call discovery listener", format = Message.Format.MESSAGE_FORMAT)
   void failedToCallListenerInDiscovery(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114015, value = "Error deploying URI {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorDeployingURI(@Cause Throwable e, URI uri);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114016, value = "Error deploying URI", format = Message.Format.MESSAGE_FORMAT)
   void errorDeployingURI(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114017, value = "Error undeploying URI {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorUnDeployingURI(@Cause Throwable e, URI a);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114018, value = "key attribute missing for configuration {0}", format = Message.Format.MESSAGE_FORMAT)
   void keyAttributeMissing(Node node);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114019, value = "Unable to deply node {0}", format = Message.Format.MESSAGE_FORMAT)
   void unableToDeployNode(@Cause Exception e, Node node);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114020, value = "Invalid filter: {0}", format = Message.Format.MESSAGE_FORMAT)
   void invalidFilter(@Cause Throwable t, SimpleString filter);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114021, value = "page subscription = {0} error={1}", format = Message.Format.MESSAGE_FORMAT)
   void pageSubscriptionError(IOAsyncTask ioAsyncTask, String error);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114022, value = "Failed to store id", format = Message.Format.MESSAGE_FORMAT)
   void batchingIdError(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114023, value = "Cannot find message {0}", format = Message.Format.MESSAGE_FORMAT)
   void cannotFindMessage(Long id);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114024, value = "Cannot find queue messages for queueID={0} on ack for messageID={1}", format = Message.Format.MESSAGE_FORMAT)
   void journalCannotFindQueue(Long queue, Long id);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114025, value = "Cannot find queue messages {0} for message {1} while processing scheduled messages", format = Message.Format.MESSAGE_FORMAT)
   void journalCannotFindQueueScheduled(Long queue, Long id);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114026, value = "error releasing resources", format = Message.Format.MESSAGE_FORMAT)
   void largeMessageErrorReleasingResources(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114027, value = "failed to expire messages for queue", format = Message.Format.MESSAGE_FORMAT)
   void errorExpiringMessages(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114028, value = "Failed to close session", format = Message.Format.MESSAGE_FORMAT)
   void errorClosingSession(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114029, value = "Caught XA exception", format = Message.Format.MESSAGE_FORMAT)
   void caughtXaException(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114030, value = "Caught exception", format = Message.Format.MESSAGE_FORMAT)
   void caughtException(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114031, value = "Invalid packet {0}", format = Message.Format.MESSAGE_FORMAT)
   void invalidPacket(Packet packet);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114032, value = "Failed to create session", format = Message.Format.MESSAGE_FORMAT)
   void failedToCreateSession(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114033, value = "Failed to reattach session", format = Message.Format.MESSAGE_FORMAT)
   void failedToReattachSession(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114034, value = "Failed to handle create queue", format = Message.Format.MESSAGE_FORMAT)
   void failedToHandleCreateQueue(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114035, value = "Unexpected error handling packet {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorHandlingPacket(@Cause Throwable t, Packet packet);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114036, value = "Failed to decode packet", format = Message.Format.MESSAGE_FORMAT)
   void errorDecodingPacket(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114037, value = "Failed to execute failure listener", format = Message.Format.MESSAGE_FORMAT)
   void errorCallingFailureListener(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114038, value = "Unable to send frame {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorSendingFrame(@Cause Exception e, StompFrame frame);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114039, value = "Stomp Error, tx already exist! {0}", format = Message.Format.MESSAGE_FORMAT)
   void stompErrorTXExists(String txID);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114040, value = "Error encoding stomp packet", format = Message.Format.MESSAGE_FORMAT)
   void errorEncodingStompPacket(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114041, value = "Cannot create stomp ping frame due to encoding problem.", format = Message.Format.MESSAGE_FORMAT)
   void errorOnStompPingFrame(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114042, value = "Failed to write to handler on invm connector {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorWritingToInvmConnector(@Cause Exception e, Runnable runnable);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114043, value = "error flushing invm channel", format = Message.Format.MESSAGE_FORMAT)
   void errorflushingInvmChannel(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114044, value = "Failed to execute connection life cycle listener", format = Message.Format.MESSAGE_FORMAT)
   void errorCallingLifeCycleListener(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114045, value = "Failed to create netty connection", format = Message.Format.MESSAGE_FORMAT)
   void errorCreatingNettyConnection(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114046, value = "Failed to stop acceptor", format = Message.Format.MESSAGE_FORMAT)
   void errorStoppingAcceptor();

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114047, value = "large message sync: largeMessage instance is incompatible with it, ignoring data", format = Message.Format.MESSAGE_FORMAT)
   void largeMessageIncomatible();

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114048, value = "Couldn't cancel reference {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorCancellingRefOnBridge(@Cause Exception e, MessageReference ref2);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114049, value = "Failed to ack on Bridge", format = Message.Format.MESSAGE_FORMAT)
   void failedToAckOnBridge(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114050, value =  "-------------------------------Stomp begin tx: {0}", format = Message.Format.MESSAGE_FORMAT)
   void stompBeginTX(String txID);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114052, value =  "Failed to stop bridge", format = Message.Format.MESSAGE_FORMAT)
   void errorStoppingBridge(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114053, value =  "Failed to pause bridge", format = Message.Format.MESSAGE_FORMAT)
   void errorPausingBridge(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114054, value =  "Failed to broadcast connector configs", format = Message.Format.MESSAGE_FORMAT)
   void errorBroadcastingConnectorConfigs(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114055, value =  "Failed to close consumer", format = Message.Format.MESSAGE_FORMAT)
   void errorClosingConsumer(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114056, value =  "Failed to close cluster connection flow record", format = Message.Format.MESSAGE_FORMAT)
   void errorClosingFlowRecord(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114057, value =  "Failed to update cluster connection topology", format = Message.Format.MESSAGE_FORMAT)
   void errorUpdatingTopology(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114058, value =  "cluster connection Failed to handle message", format = Message.Format.MESSAGE_FORMAT)
   void errorHandlingMessage(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114059, value =  "Failed to ack old reference", format = Message.Format.MESSAGE_FORMAT)
   void errorAckingOldReference(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114060, value =  "Failed to expire message reference", format = Message.Format.MESSAGE_FORMAT)
   void errorExpiringRef(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114061, value =  "Failed to remove consumer", format = Message.Format.MESSAGE_FORMAT)
   void errorRemovingConsumer(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114062, value =  "Failed to deliver", format = Message.Format.MESSAGE_FORMAT)
   void errorDelivering(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114063, value =  "Error while restarting the backup server: {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorRestartingBackupServer(@Cause Exception e, HornetQServer backup);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114064, value =  "Failed to send forced delivery message", format = Message.Format.MESSAGE_FORMAT)
   void errorSendingForcedDelivery(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114065, value =  "error acknowledging message", format = Message.Format.MESSAGE_FORMAT)
   void errorAckingMessage(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114066, value =  "Failed to run large message deliverer", format = Message.Format.MESSAGE_FORMAT)
   void errorRunningLargeMessageDeliverer(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114067, value =  "Exception while browser handled from {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorBrowserHandlingMessage(@Cause Exception e, MessageReference current);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114068, value =  "Failed to delete large message file", format = Message.Format.MESSAGE_FORMAT)
   void errorDeletingLargeMessageFile(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114069, value =  "Failed to remove temporary queue {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorRemovingTempQueue(@Cause Exception e, SimpleString bindingName);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114070, value =  "Cannot find consumer with id {0}", format = Message.Format.MESSAGE_FORMAT)
   void cannotFindConsumer(long consumerID);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114071, value =  "Failed to close connection {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorClosingConnection(ServerSessionImpl serverSession);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114072, value =  "Failed to call notification listener", format = Message.Format.MESSAGE_FORMAT)
   void errorCallingNotifListener(@Cause Exception e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114073, value =  "Unable to call Hierarchical Repository Change Listener", format = Message.Format.MESSAGE_FORMAT)
   void errorCallingRepoListener(@Cause Throwable e);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114074, value =  "failed to timeout transaction, xid:{0}", format = Message.Format.MESSAGE_FORMAT)
   void errorTimingOutTX(@Cause Exception e, Xid xid);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114075, value =  "Caught unexpected Throwable", format = Message.Format.MESSAGE_FORMAT)
   void caughtunexpectedThrowable(@Cause Throwable t);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114076, value =  "Failed to invoke getTextContent() on node {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorOnXMLTransform(@Cause Throwable t, Node n);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114077, value =  "Invalid configuration", format = Message.Format.MESSAGE_FORMAT)
   void errorOnXMLTransformInvalidConf(@Cause Throwable t);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114078, value =  "exception while stopping the replication manager", format = Message.Format.MESSAGE_FORMAT)
   void errorStoppingReplicationManager(@Cause Throwable t);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = 114079, value = "Bridge Failed to ack", format = Message.Format.MESSAGE_FORMAT)
   void bridgeFailedToAck(@Cause Throwable t);
}
