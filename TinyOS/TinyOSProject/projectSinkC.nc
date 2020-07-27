#include "project.h"
#include "Timer.h"

module projectSinkC

{
	uses 
	{
	/****** INTERFACES *****/

		interface Boot; 
	
		//interfaces for communication
    	interface SplitControl as AMControl;
    	interface AMSend;
    	interface Packet;
    	interface AMPacket;
    	interface Receive;
    	
		//timer interfaces
		interface Timer<TMilli> as TresholdTimer;
		interface Timer<TMilli> as DataTimer;
	
		//other interfaces, if needed
    	//interface PacketAcknowledgements as Ack;
	
		//interface used to perform sensor reading (to get the value from a sensor)
		interface Read<uint16_t>;
  	}

} 
implementation
{


  	message_t packet;
  	bool locked = FALSE;
  	uint16_t treshold;
  	uint16_t data;
  	uint8_t nextHop;

  	void sendData(uint16_t data);
  	void sendTreshold();
  	void readData();

  

	//***************** Boot event ********************//
  	event void Boot.booted()
  	{
		call AMControl.start();	
  	}

	//***************** SplitControl event********************//
  	event void AMControl.startDone(error_t err)
  	{
  		if(err == SUCCESS)
  		{
  			dbg ("boot", "Booting ... I'm node : %d, Radio started at time: %s \n", TOS_NODE_ID, sim_time_string());
  			if(TOS_NODE_ID ==1){
				call TresholdTimer.startPeriodicAt(0, 1000);
			}
			else{
				treshold = 0;
				nextHop = 0;
				call DataTimer.startPeriodicAt(0, 1000);
				//call ForwardingTimer.startPeriodicAt(0,100);
			}
			
  		}
  		else
  		{
  			dbgerror("error", "radio didn't start\n");
  			call AMControl.start();
  		}
  
  	}
  
  	event void AMControl.stopDone(error_t err){}


  
  //***************** MilliTimer events ( read a new treshold or a new Data ********************//
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
      	if(mess->type ==1 && mess->sender <= TOS_NODE_ID){
      		treshold = mess->value;
      		nextHop = mess->sender;
      		dbg_clear("radio", "\n");
      		dbg("radio", "Mote %hu has received a treshold message with treshold %hu\n",TOS_NODE_ID, treshold);
      		dbg("error", "Mote %hu has set treshold %hu and next hop %hu\n",TOS_NODE_ID, treshold, nextHop );
      		sendTreshold();
      	
      	}
		if (mess->type == 2){
			if( TOS_NODE_ID == 1){
				dbg("error", "sink node received data message from %hu with data %hu\n",TOS_NODE_ID, mess->value); 
			}
			else{
      			dbg("radio", "Mote %hu has received a data message with data %hu\n",TOS_NODE_ID, mess->value);
				sendData(mess->value);
			}
		}
    	return buf;
 
   	}

	
	
	  	
  
	//************************* Read interface **********************//
  	event void Read.readDone(error_t result, uint16_t dataRead) 
  	{
		if(TOS_NODE_ID ==1){
			treshold = dataRead;
	 		dbg_clear("treshold", "\n");
			dbg("treshold","Sink just read new treshold %hu\n",dataRead);  	
			sendTreshold();
			
		}
		else{		
			if(dataRead > treshold){
				dbg_clear("data", "\n");
				dbg("data","Mote %hu just read new data %hu above the treshold %hu\n",TOS_NODE_ID,dataRead, treshold);  	
				sendData(dataRead);
	  		}
		}
  	}
  	
  	//*********************Send methods ****************//
  	
  	event void AMSend.sendDone(message_t* buf,error_t err)
  	{
  		if( &packet == buf){   
  			locked = FALSE;
  			if ( err == SUCCESS){
				//dbg("radio", "packet sent at time %s from %hu \n", sim_time_string(), TOS_NODE_ID);
  			}
			else{
				dbgerror("error", "error in sending packet, send the request again\n");

			} 
		} 		
  	}
  	
  	void sendTreshold(){
  		if (locked) return;
  		else{
	  		Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
			if (mess == NULL) return;
			mess->sender = TOS_NODE_ID;
			mess->value = treshold;
			mess->type = 1;
			//dbgerror("error", "mote %hu has created packet with treshold %hu \n", TOS_NODE_ID, mess->value);
			if(call AMSend.send(AM_BROADCAST_ADDR, &packet,sizeof(Msg_t)) == SUCCESS){
		   		//dbg("radio","Packet sent from: %hu, data: %hu, type: %hu\n ", TOS_NODE_ID, mess->value, mess->type);
				locked = TRUE;
			}
			else{
				dbg("radio","error in sending packet to %hu\n ", AM_BROADCAST_ADDR);
			}
		}
  	
  	}
  	
  	
  	void sendData(uint16_t data){
  		if (locked) return;
  		else{
	  		Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
			if (mess == NULL) return;
			mess->sender = TOS_NODE_ID;
			mess->value = data;
			mess->type = 2;
			if(nextHop == 0)return;
			else{
				if(call AMSend.send(nextHop, &packet,sizeof(Msg_t)) == SUCCESS){
		   			//dbg("data","Packet sent from mote: %hu, data: %hu, type: %hu\n ", TOS_NODE_ID, mess->value, mess->type);
					locked = TRUE;
				}
	  		}
  		}
  	}
  	
  	

}



