/*
 * Copyright 2010 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.core.transaction;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.transaction.xa.Xid;

import org.hornetq.core.server.MessageReference;
import org.hornetq.core.server.ServerMessage;
import org.hornetq.core.transaction.impl.XidImpl;
import org.hornetq.utils.json.JSONArray;
import org.hornetq.utils.json.JSONObject;

/**
 * A TransactionDetail
 *
 * @author <a href="tm.igarashi@gmail.com">Tomohisa Igarashi</a>
 */
public abstract class TransactionDetail
{
   public static final String KEY_CREATION_TIME = "creation_time";

   public static final String KEY_XID_AS_BASE64 = "xid_as_base64";

   public static final String KEY_XID_FORMAT_ID = "xid_format_id";

   public static final String KEY_XID_GLOBAL_TXID = "xid_global_txid";

   public static final String KEY_XID_BRANCH_QUAL = "xid_branch_qual";

   public static final String KEY_TX_RELATED_MESSAGES = "tx_related_messages";

   public static final String KEY_MSG_OP_TYPE = "message_operation_type";

   public static final String KEY_MSG_BODY_BUFFER = "message_body";

   public static final String KEY_MSG_TYPE = "message_type";

   public static final String KEY_MSG_PROPERTIES = "message_properties";

   public static final String KEY_MSG_PAYLOAD = "message_payload";
   
   private Xid xid;
   private Transaction transaction;
   private Long creationTime;
   
   public TransactionDetail(Xid xid, Transaction tx, Long creation)
   {
      this.xid = xid;
      this.transaction = tx;
      this.creationTime = creation;
   }
   
   public JSONObject toJSON() throws Exception
   {
      DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
      JSONObject detailJson = new JSONObject();

      detailJson.put(KEY_CREATION_TIME, dateFormat.format(new Date(this.creationTime)));
      detailJson.put(KEY_XID_AS_BASE64, XidImpl.toBase64String(this.xid));
      detailJson.put(KEY_XID_FORMAT_ID, this.xid.getFormatId());
      detailJson.put(KEY_XID_GLOBAL_TXID, new String(this.xid.getGlobalTransactionId()));
      detailJson.put(KEY_XID_BRANCH_QUAL, new String(this.xid.getBranchQualifier()));

      JSONArray msgsJson = new JSONArray();
      List<TransactionOperation> txops = this.transaction.getAllOperations();
      detailJson.put(KEY_TX_RELATED_MESSAGES, msgsJson);
      if (txops == null)
      {
         return detailJson;
      }

      for (TransactionOperation op : txops)
      {
         String opClassName = op.getClass().getName();
         String opType = null;
         if (opClassName.equals("org.hornetq.core.postoffice.impl.PostOfficeImpl$AddOperation"))
         {
            opType = "(+) send";
         }
         else if (opClassName.equals("org.hornetq.core.server.impl.QueueImpl$RefsOperation"))
         {
            opType = "(-) receive";
         }

         List<MessageReference> msgs = op.getRelatedMessageReferences();
         if (msgs == null)
         {
            continue;
         }

         for (MessageReference ref : msgs)
         {
            JSONObject msgJson = new JSONObject();
            msgsJson.put(msgJson);

            msgJson.put(KEY_MSG_OP_TYPE, opType);

            ServerMessage msg = ref.getMessage().copy();

            msgJson.put(KEY_MSG_TYPE, decodeMessageType(msg));
            msgJson.put(KEY_MSG_PAYLOAD, decodeMessagePayload(msg));
            msgJson.put(KEY_MSG_PROPERTIES, decodeMessageProperties(msg));
         }
      }
      return detailJson;
   }

   public abstract String decodeMessageType(ServerMessage msg);
   
   public abstract String decodeMessagePayload(ServerMessage msg);
   
   public abstract Map<String,Object> decodeMessageProperties(ServerMessage msg);
}
