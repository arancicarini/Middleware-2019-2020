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
		//interface Timer<TMilli> as SendTimer;
	
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
  	uint16_t sink_treshold_id;
  	uint8_t parent;
  	uint8_t emergency_parent;
  	uint8_t childs[2];
  	uint8_t index=0;
  	uint8_t index1=0;
  	uint16_t counter;
  	Msg_t* q1[10];
  	uint8_t q1_index=0 ;
  	bool q1_full=FALSE;
  	 	

  	void sendData(uint32_t data, uint16_t source, uint32_t time);
  	void sendTreshold(uint32_t time, uint16_t t_id);
  	void sendDiscovery();  	
  	void sendDiscoveryAck(uint8_t destination, uint8_t type);
  	void readData();
  	void inizializing();


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
  				sink_treshold_id = 0;
  				childs[0]=0;
  				childs[1]=0;
  				parent=1;
  				emergency_parent=0;
  				inizializing();
  				dbg ("boot", "node %d: booted, child1 : %d , child2 : %d\n", TOS_NODE_ID, childs[0], childs[1]);
				call TresholdTimer.startPeriodicAt(0, 80000);
			}
			else{
				//Inizializing node values
				treshold = 4294967295; // maximum value for a 32 bit integer
				treshold_id = 0;
  				childs[0]=0;
  				childs[1]=0;
  				parent=0;
  				emergency_parent=0;
  				inizializing(); 				
  				dbg ("boot", "node %d: booted, child1 : %d , child2 : %d , parent: %d , emergency_parent: %d\n", TOS_NODE_ID, childs[0], childs[1], parent, 			emergency_parent);				
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
      	//case 1 : DISCOVERY message
      	if(mess->type ==3){ //discovery message	
      		sendDiscovery();
      		treshold = mess->value;
      		treshold_id = mess->treshold_id;
    		emergency_parent = mess->sender;
      		dbg_clear("radio", "\n");
      		dbg("radio", "first discovery riceived from %d, setting emergency parent to %d\n", mess->sender, emergency_parent);
      		sendDiscoveryAck(mess->sender, 4); 
      	}
      	if(mess->type==4){
      		if(index<=2){
      			childs[index]=mess->sender;
      			dbg_clear("radio", "\n");
      			dbg("radio", "discovery ack riceived from %d, setting child %d to %d\n",mess->sender , index, childs[index]);
      			index+=1;
      			sendDiscoveryAck(mess->sender, 5);
      		}	
      	}
      	if(mess->type==5){
      		parent= mess->sender;
      		dbg_clear("radio", "\n");
      		dbg("radio", "discovery ack reply (second ack) riceived from %d, setting parent to %d\n",mess->sender , parent);
      	}			
      	//case 2 : new treshold 
      	if(mess->type ==1 && treshold_id<mess->treshold_id){
      		treshold = mess->value;
      		treshold_id= mess->treshold_id;
      		dbg_clear("radio", "\n");
      		dbg("radio", "Mote %hu has received a treshold message with treshold %lu from mote %hu\n",TOS_NODE_ID, treshold, mess->sender);
			dbg_clear("analysis", "\n");      		
      		dbg("analysis", "Time for treshold %lu message from sink to %hu is : %lu ms\n", treshold, TOS_NODE_ID, (sim_time()-mess->time)); 
      		dbg("analysis", "current simulation time: %s\n", sim_time_string());
      		sendTreshold(mess->time, mess->treshold_id);
      	
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
			sink_treshold_id+=1;
			treshold_id+=1;			
	 		dbg_clear("sink", "\n");
			dbg("sink","Sink just read new treshold %lu, with id %d\n",dataRead, sink_treshold_id);  	
			if(sink_treshold_id==1){
				sendDiscovery();
	 			dbg_clear("sink", "\n");
				dbg("sink","Sink is sending first discovery");				
			}
			sendTreshold(sim_time(),sink_treshold_id);
			
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
	  			}
  				else{
  				    dbg_clear("error", "\n");
  					dbg("error", "Error in sending packet, send the request again\n");
  			
				}
			}
			
  		}
		else{
			dbg_clear("error", "\n");
			dbgerror("error", "Error in sending packet, send the request again\n");

		}
		
		  		
  	}
  	
  	void sendTreshold(uint32_t elapsed_time, uint16_t t_id){

	  	Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
		if (mess == NULL) return;
		mess->sender = TOS_NODE_ID;
		mess->value = treshold;
		mess->type = 1;
		mess->source =1;
		mess->treshold_id = t_id;
		mess-> time = elapsed_time;
			  	
  		if (!locked){
			//trovare un modo di mandare solo ai due figli o me ne batto e mando comunque in broadcast?
			//other messages only to the childs
			call Ack.requestAck(&packet);
			if(call AMSend.send(childs[index1], &packet,sizeof(Msg_t)) == SUCCESS){
				dbg_clear("radio", "\n");
		   		dbg("radio","Packet sent from: %hu, data: %lu, type: %hu, treshold_id: %lu\n", TOS_NODE_ID, mess->value, mess->type, mess->treshold_id);
		   		dbg("radio", "Message to the first child %d", childs[index1]);
		   		locked = TRUE;
			}
			else{
				dbg_clear("error", "\n");
				dbg("error","Error in sending packet\n ");
			}			
			
		}
		else{
			if(q1_index<10){
				q1[q1_index]=mess;	
				q1_index+=1;
				dbg_clear("error", "\n");
				dbg("error","element in q1[%d]: %s\n ", q1, q1[q1_index]);				
				q1_full=TRUE;
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
			if(parent == 0 && emergency_parent==0)return;
			else{
				if(parent!=0){
					call Ack.requestAck(&packet);
					if(call AMSend.send(parent, &packet,sizeof(Msg_t)) == SUCCESS){
						dbg_clear("radio", "\n");
		   				dbg("radio","Packet sent from: %hu, to: %hu , data: %lu, type: %hu\n ", TOS_NODE_ID, parent, mess->value, mess->type);
		   				locked = TRUE;
					}				
				}
				else{
					call Ack.requestAck(&packet);
					if(call AMSend.send(emergency_parent, &packet,sizeof(Msg_t)) == SUCCESS){
						dbg_clear("radio", "\n");
		   				dbg("radio","Packet sent from: %hu, to: %hu , data: %lu, type: %hu\n ", TOS_NODE_ID, emergency_parent, mess->value, mess->type);
		   				locked = TRUE;
					}
				}
		  	}
	  	}
		
  	}
  	
  	
  	void sendDiscovery(){//manda in broadcast
  
  	  	Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
		if (mess == NULL) return;
		mess->sender = TOS_NODE_ID;
		mess->value = treshold;
		mess->type = 3;
		mess->source =1;
		mess->treshold_id = 1;
  		
  		if (!locked){			
			call Ack.requestAck(&packet);
			if(call AMSend.send(AM_BROADCAST_ADDR, &packet,sizeof(Msg_t)) == SUCCESS){
				dbg_clear("radio", "\n"); 	
		  		dbg("radio","Packet sent from: %hu, data: %lu, type: %hu, treshold_id: %lu\n", TOS_NODE_ID, mess->value, mess->type, mess->treshold_id);
		   		dbg("radio", "Message of discovery sent");
		   		locked = TRUE;
			}
			else{
				dbg_clear("error", "\n");
				dbg("error","Error in sending packet\n ");
			}
		}			  	
  	}
  	
  	void sendDiscoveryAck(uint8_t destination, uint8_t type){
  
  	  	Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
		if (mess == NULL) return;
		mess->sender = TOS_NODE_ID;
		mess->value = treshold;
		mess->type = type;
		mess->source = TOS_NODE_ID;
		mess->treshold_id = treshold_id;
  		
  		if (!locked){
			call Ack.requestAck(&packet);
			if(call AMSend.send(destination, &packet,sizeof(Msg_t)) == SUCCESS){
				dbg_clear("radio", "\n"); 	
		  		dbg("radio","Packet sent from: %hu, data: %lu, type: %hu, treshold_id: %lu\n", TOS_NODE_ID, mess->value, mess->type, mess->treshold_id);
		   		dbg("radio", "Message of discovery ack sent");
		   		locked = TRUE;
			}
			else{
				dbg_clear("error", "\n");
				dbg("error","Error in sending packet\n ");
			}
		} 	 	
  	}
  	
  	
  	void inizializing(){
		for(q1_index=0;q1_index<10;q1_index++){
			q1[q1_index]=0;
		} 		  	
  	}
  	

}



