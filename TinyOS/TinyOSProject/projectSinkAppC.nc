#include "project.h"

configuration projectSinkAppC {}

implementation {


	/****** COMPONENTS *****/
	components MainC, projectSinkC as App;
	components new TimerMilliC() as Timer1;
	components new TimerMilliC() as Timer2;
	components new TimerMilliC() as Timer3;
	components new FakeSensorC();
	components ActiveMessageC;
	components new AMSenderC( AM_MY_MSG);
	components new AMReceiverC (AM_MY_MSG);
 
  

	/****** INTERFACES *****/
	
	//Boot interface
	App.Boot -> MainC.Boot;

	/****** Wire the other interfaces down here *****/

	//Send and Receive interfaces
	App.AMSend-> AMSenderC;
	App.Receive-> AMReceiverC;

	//Radio Control
	App.AMControl -> ActiveMessageC;

	//Interfaces to access package fields
	App.AMPacket -> AMSenderC;
	App.Packet -> AMSenderC;

	//Timer interface
	App.TresholdTimer -> Timer1;  
	App.DataTimer -> Timer2;  
	App.ForwardingTimer -> Timer3;
	
	//Fake Sensor read
	App.Read -> FakeSensorC;
	

  
  	

}

