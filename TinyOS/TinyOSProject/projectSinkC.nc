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
	
		//read interface (fake sensor)
		interface Read<uint32_t>;
  	}

} 
implementation
{


  	message_t packet;
  	bool locked = FALSE;
  	uint32_t treshold;
  	uint8_t nextHop;
  	uint16_t counter;

  	void sendData(uint32_t data, uint16_t source, uint32_t time);
  	void sendTreshold(uint32_t time);
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
				call TresholdTimer.startPeriodicAt(0, 2000);
			}
			else{
				//Inizializing node values
				treshold = 4294967295; // maximum value for a 32 bit integer
				nextHop = 0;
				call DataTimer.startPeriodicAt(0, 1000);
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
  	event message_t* Receive.receive(message_t* buf,void* payload, uint8_t len) 
  	{
      	Msg_t* mess = (Msg_t*)payload;
      	//type 1 -> SETUP message, discard packets from motes with higher IDs
      	if(mess->type ==1 && mess->sender < TOS_NODE_ID){
      		treshold = mess->value;
      		nextHop = mess->sender;
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
	 		dbg_clear("sink", "\n");
			dbg("sink","Sink just read new treshold %lu\n",dataRead);  	
			sendTreshold(sim_time());
			
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
  	
  	void sendTreshold(uint32_t elapsed_time){
  		if (!locked){
		  	Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
			if (mess == NULL) return;
			mess->sender = TOS_NODE_ID;
			mess->value = treshold;
			mess->type = 1;
			mess->source =1;
			mess-> time = elapsed_time;
			
			call Ack.requestAck(&packet);
			if(call AMSend.send(AM_BROADCAST_ADDR, &packet,sizeof(Msg_t)) == SUCCESS){
				dbg_clear("radio", "\n");
		   		dbg("radio","Packet sent from: %hu, data: %lu, type: %hu\n", TOS_NODE_ID, mess->value, mess->type);
		   		locked = TRUE;
			}
			else{
				dbg_clear("error", "\n");
				dbg("error","Error in sending packet\n ");
			}
		}
  	
  	}
  	
  	
  	void sendData(uint32_t data, uint16_t source, uint32_t elapsed_time){
  		if (!locked){
	  		Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
			if (mess == NULL) return;
			mess->sender = TOS_NODE_ID;
			mess->value = data;
			mess->type = 2;
			mess->source = source;
			mess-> time = elapsed_time;			
			if(nextHop == 0)return;
			else{
				call Ack.requestAck(&packet);
				if(call AMSend.send(nextHop, &packet,sizeof(Msg_t)) == SUCCESS){
					dbg_clear("radio", "\n");
		   			dbg("radio","Packet sent from: %hu, to: %hu , data: %lu, type: %hu\n ", TOS_NODE_ID, nextHop, mess->value, mess->type);
		   			locked = TRUE;
				}
		  	}
	  	}
  		
  	}
  	
  	

}



