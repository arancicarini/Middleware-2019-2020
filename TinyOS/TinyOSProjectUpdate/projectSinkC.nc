#include "project.h"
#include "Timer.h"

module projectSinkC

{
	uses 
	{
	/****** INTERFACES *****/

		interface Boot; 
	
		//communication interfaces
    	interface SplitControl as AMControl;
    	interface AMSend;
    	interface Packet;
    	interface AMPacket;
    	interface Receive;
    	
		//timer interfaces
		interface Timer<TMilli> as TresholdTimer;
		interface Timer<TMilli> as DataTimer;
	
		//other interfaces, if needed
    	interface PacketAcknowledgements as Ack;
    	interface LocalTime<TMilli>;
    		
		//read interface (fake sensor)
		interface Read<uint32_t>;
  	}

} 
implementation
{


  	message_t packet;
  	bool locked = FALSE;
  	uint32_t treshold;
  	uint16_t treshold_id;  	
  	uint8_t parent;
  	uint8_t children[2];
  	uint8_t index=0;
  	uint16_t counter;
  	bool mess_to_send = FALSE;
  	Msg_t* saved_mess;


  	 	

  	void sendData(uint32_t data, uint16_t source, uint32_t time);
  	void sendTreshold(uint32_t time);
  	void sendDiscovery(uint16_t id);  	
  	void sendDiscoveryAck(uint8_t destination, uint16_t id);
  	void sendSetup(uint8_t destination, uint16_t id);
  	void readData();



	//***************** Boot event ********************//
  	event void Boot.booted()
  	{
		call AMControl.start();	
  	}

	//***************** SplitControl event********************//
  	event void AMControl.startDone(error_t err)
  	{
  		if(err == SUCCESS){
  			dbg ("boot", "Booting ... I'm node : %d, Radio started at time: %s \n", TOS_NODE_ID, sim_time_string());
  			if(TOS_NODE_ID ==1){
  				//I'm the sink
  				treshold = 4294967295; // maximum value for a 32 bit integer
  				treshold_id = 0;
  				children[0]=0;
  				children[1]=0;
  				parent=1;
  				dbg ("boot", "node %d: booted, child1 : %d , child2 : %d\n", TOS_NODE_ID, children[0], children[1]);
				call TresholdTimer.startPeriodicAt(0, 150000);
			}
			else{
				//Inizializing node values
				treshold = 4294967295; // maximum value for a 32 bit integer
				treshold_id=0;
  				children[0]=0;
  				children[1]=0;
  				parent=0;
  				dbg ("boot", "node %d: booted, child1 : %d , child2 : %d , parent: %d \n", TOS_NODE_ID, children[0], children[1], parent);				
				//call DataTimer.startPeriodicAt(0, 1000);
			}
			
  		}
  		else{
  			dbg_clear("error", "\n");
  			dbgerror("error", "Error in booting the radio\n");
  			call AMControl.start();
  		}
  
  	}
  
  	event void AMControl.stopDone(error_t err){}


  
  //***************** MilliTimer events ( read a new treshold or a new data )********************//
  	event void TresholdTimer.fired() 
  	{
	 	dbg_clear("timer", "\n");
		dbg("timer","Treshold timer fired at %s.\n", sim_time_string());
		call Read.read();
	}
  
  	event void DataTimer.fired() 
  	{
	 	dbg_clear("timer", "\n");
		dbg("timer","Data timer fired at %s.\n", sim_time_string());
		call Read.read();
	}
  
  	


	//***************************** Receive interface *****************//
  	event message_t* Receive.receive(message_t* buf,void* payload, uint8_t len) //forse varrebbe la pena fare un terzo tipo
  	{
      	Msg_t* mess = (Msg_t*)payload;
      	if(mess->type ==3 && treshold_id < mess->treshold_id){ //discovery message	
      		dbg_clear("radio", "\n");
      		dbg("radio", "first discovery received from %d\n", mess->sender);
      		//reinizialing
      		parent=0;
      		children[0]=0;
      		children[1]=0;
      		index=0;
      		if (TOS_NODE_ID == 2) dbg("radio", "ho appena ricevuto un discovery message\n");
      		sendDiscoveryAck(mess->sender, mess->treshold_id); 
      	}
      	if(mess->type==4){
      		if(index<2){
      			children[index]=mess->sender;
      			dbg_clear("radio", "\n");
      			dbg("radio", "discovery ack riceived from %d, setting child %d to %d\n",mess->sender , index, children[index]);
      			index+=1;
      			sendSetup(mess->sender, mess->treshold_id);
      		}	
      	}
      	
      	if(mess->type==5){
      		if (TOS_NODE_ID == 2) dbg("radio", "ciao\n");
      		parent= mess->sender;
      		treshold= mess->value;
      		treshold_id = mess->treshold_id;
      		dbg_clear("radio", "\n");
      		dbg("radio", "setup message received from %d, setting parent to %d treshold id: %hu\n",mess->sender , parent, treshold_id);
      		sendDiscovery(mess->treshold_id);
      	}			
      	//case 2 : new treshold 
      	if(mess->type ==1){
      			treshold = mess->value;
      			dbg_clear("radio", "\n");
      			dbg("radio", "Mote %hu has received a treshold message with treshold %lu from mote %hu\n",TOS_NODE_ID, treshold, mess->sender);
				dbg_clear("analysis", "\n");      		
      			dbg("analysis", "Time for treshold %lu message from sink to %hu is : %lu ms\n", treshold, TOS_NODE_ID, (sim_time()-mess->time)); 
      			dbg("analysis", "current simulation time: %s\n", sim_time_string());
      			sendTreshold(mess->time);
      		}
     	//type 2 -> DATA message
		if (mess->type == 2){
			if( TOS_NODE_ID == 1){
				dbg_clear("sink", "\n");
				dbg("sink", "Sink node received data message from %hu with data %lu at time %s\n",mess->source, mess->value, sim_time_string()); 
				counter +=1;
				dbg_clear("analysis", "\n");
				dbg("analysis","Data Message counter: %hu\n",counter);
				dbg_clear("analysis", "\n");
      			dbg("analysis", "Time for data message %lu from %hu to sink is : %lu ms\n", mess->value, mess-> source, (sim_time()-mess->time)); 
      			dbg("analysis", "current simulation time: %s\n", sim_time_string());
			}
			else{
		      	dbg_clear("data", "\n");		
      			dbg("data", "Mote %hu has received a data message with data %lu\n",TOS_NODE_ID, mess->value);
				sendData(mess->value, mess->source, mess->time);
			}
		}
    	return buf; 
   	}

	
	
	  	
  
	//************************* Read interface **********************//
  	event void Read.readDone(error_t result, uint32_t dataRead) 
  	{
		if(TOS_NODE_ID ==1){
			treshold = dataRead;
			treshold_id = treshold_id +1;
	 		dbg_clear("sink", "\n");
			dbg("sink","Sink just read new treshold %lu, with id %d\n",dataRead, treshold_id);  	
			if(treshold_id==1 || treshold_id%10==0){
				sendDiscovery(treshold_id);
	 			dbg_clear("sink", "\n");
				dbg("sink","Sink is sending discovery");				
			}
			else{
				sendTreshold(sim_time());
			}
			
		}
		else{		
			if(dataRead > treshold){
				dbg_clear("data", "\n");
				dbg("data","Mote %hu just read new data %lu above the treshold %lu at time %s\n",TOS_NODE_ID,dataRead, treshold, sim_time_string());  	
				sendData(dataRead, TOS_NODE_ID,sim_time());
	  		}
		}
  	}
  	
  	//*********************Send methods ****************//
  	
  	event void AMSend.sendDone(message_t* buf,error_t err)
  	{
  		if( &packet == buf){   
  			locked = FALSE;
  			if ( err == SUCCESS){
				if(call Ack.wasAcked(buf)){
					dbg_clear("radio", "\n");
					dbg("radio", "Packet sent at time %s from %hu \n", sim_time_string(), TOS_NODE_ID);
					dbg_clear("radio", "\n");
		  			dbg("radio", "Message correctly delivered\n");
					dbg_clear("analysis", "\n");
					if(mess_to_send && children[1]!=0){ 
						
						Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));
						
						dbg("error","Must send another packet\n "); 
						mess=saved_mess;  
						call Ack.requestAck(&packet);
						if(call AMSend.send(children[1], &packet,sizeof(Msg_t)) == SUCCESS){
							dbg_clear("radio", "\n");
		   					dbg("radio","Packet sent from: %hu, data: %lu, type: %hu, treshold_id: %lu\n", TOS_NODE_ID, mess->value, mess->type, mess->treshold_id);
		   					dbg("radio", "Message to the second child %d", children[0]);
		   					locked = TRUE;
						}
						else{
							dbg_clear("error", "\n");
							dbg("error","Error in sending packet\n ");
						}						
					}
					mess_to_send=FALSE;											
			}

  			else{
  			    dbg_clear("error", "\n");
  				dbg("error", "Error in sending packet, send the request again NOT ACKED\n");		
			}
  		}
			else{
				dbg_clear("error", "\n");
				dbgerror("error", "Error in sending packet, send the request again NOT SEND\n");
			}  		
  		}
  	}
  	
  	void sendTreshold(uint32_t elapsed_time){
	  	Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t))); 
	  	 
		if (mess == NULL) return;
		mess->sender = TOS_NODE_ID;
		mess->value = treshold;
		mess->type = 1;
		mess->source =1;
		mess-> time = elapsed_time;
		mess->treshold_id = treshold_id;
			  	
  		if (!locked && children[0]!=0){
			call Ack.requestAck(&packet);
			if(call AMSend.send(children[0], &packet,sizeof(Msg_t)) == SUCCESS){
				dbg_clear("radio", "\n");
		   		dbg("radio","Packet sent from: %hu, data: %lu, type: %hu, treshold_id: %lu\n", TOS_NODE_ID, mess->value, mess->type, mess->treshold_id);
		   		dbg("radio", "Message to the first child %d", children[0]);
		   		locked = TRUE;
				mess_to_send=TRUE;
				saved_mess=mess;		   		
			}
			else{
				dbg_clear("error", "\n");
				dbg("error","Error in sending packet\n ");
			}				
		}
	}
  	
  	
  	
  	void sendData(uint32_t data, uint16_t source, uint32_t elapsed_time){
  		Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
		if (mess == NULL) return;
		mess->sender = TOS_NODE_ID;
		mess->value = data;
		mess->type = 2;
		mess->source = source;
		mess-> time = elapsed_time;  	
		
  		if (!locked){			
			if(parent!=0){
				call Ack.requestAck(&packet);
				if(call AMSend.send(parent, &packet,sizeof(Msg_t)) == SUCCESS){
					dbg_clear("radio", "\n");
	   				dbg("radio","Packet sent from: %hu, to: %hu , data: %lu, type: %hu\n ", TOS_NODE_ID, parent, mess->value, mess->type);
	   				locked = TRUE;
				}				
			}
		}
	}	  	
  	
  	
  	void sendDiscovery(uint16_t id){//manda in broadcast
  
  	  	Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
		if (mess == NULL) return;
		mess->sender = TOS_NODE_ID;
		mess->value = 0;
		mess->type = 3; //discovery
		mess->source =1;
		mess->treshold_id = id;
		dbg_clear("radio", "\n"); 	
  		dbg("radio","Packet sent from: %hu, data: %lu, type: %d, treshold_id %hu\n", TOS_NODE_ID, mess->value, mess->type, mess->treshold_id);  		
  		if (!locked){			
			//call Ack.requestAck(&packet);
			if(call AMSend.send(AM_BROADCAST_ADDR, &packet,sizeof(Msg_t)) == SUCCESS){
				dbg_clear("radio", "\n"); 	
		  		dbg("radio","Packet sent from: %hu, data: %lu, type: %hu ID %hu\n", TOS_NODE_ID, mess->value, mess->type);
		   		dbg("radio", "Message of discovery sent");
		   		locked = TRUE;
			}
			else{
				dbg_clear("error", "\n");
				dbg("error","Error in sending packet\n ");
			}
		}					  	
  	}
  	
  	void sendDiscoveryAck(uint8_t destination, uint16_t id){
  
  	  	Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
		if (mess == NULL) return;
		mess->sender = TOS_NODE_ID;
		mess->value = 0;
		mess->type = 4;
		mess->source = TOS_NODE_ID;
		mess->treshold_id=id;
  		
  		if (!locked){
			call Ack.requestAck(&packet);
			if(call AMSend.send(destination, &packet,sizeof(Msg_t)) == SUCCESS){
				dbg_clear("radio", "\n"); 	
		  		dbg("radio","Packet sent from: %hu, data: %lu, type: %hu\n", TOS_NODE_ID, mess->value, mess->type);
		   		dbg("radio", "Message of discovery ack sent");
		   		locked = TRUE;
			}
			else{
				dbg_clear("error", "\n");
				dbg("error","Error in sending packet\n ");
			}
		}		 	 	
  	}
  	
  	void sendSetup(uint8_t destination,uint16_t id){
  
  	  	Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
		if (mess == NULL) return;
		mess->sender = TOS_NODE_ID;
		mess->value = treshold;
		mess->type = 5;
		mess->source = TOS_NODE_ID;
		mess->treshold_id = id;
  		dbg("radio","Setup packet sent from: %hu, data: %lu, th id: %hu to %hu\n", TOS_NODE_ID, mess->value, mess->treshold_id, destination);
  		if (!locked){
			call Ack.requestAck(&packet);
			if(call AMSend.send(destination, &packet,sizeof(Msg_t)) == SUCCESS){
				dbg_clear("radio", "\n"); 	
		  		dbg("radio","Packet sent from: %hu, data: %lu, type: %hu\n", TOS_NODE_ID, mess->value, mess->type);
		   		dbg("radio", "Message of setup sent");
		   		locked = TRUE;
			}
			else{
				dbg_clear("error", "\n");
				dbg("error","Error in sending packet\n ");
			}
		}		 	 	
  	}  	
  	

}



