#include "project.h"

configuration projectSinkAppC {}

implementation {


	/****** COMPONENTS *****/
	components MainC, projectSinkC as App;
	components new TimerMilliC() as Timer1;
	components new TimerMilliC() as Timer2;
	components new FakeSensorC();
	components ActiveMessageC;
	//components new AMSenderC( AM_MY_MSG);
	//components new AMReceiverC (AM_MY_MSG);
	components DisseminationC;
  	components new DisseminatorC(uint16_t, 0x1234) as Diss16C;	
  	components CollectionC as Collector;
  	components new CollectionSenderC(0xee);
 
  

	/****** INTERFACES *****/
	
	//Boot interface
	App.Boot -> MainC.Boot;

	/****** Wire the other interfaces down here *****/

	//Send and Receive interfaces
	//App.AMSend-> AMSenderC;
	//App.Receive-> AMReceiverC;

	//Radio Control
	App.SplitControl -> ActiveMessageC;

	//Interfaces to access package fields
	//App.AMPacket -> AMSenderC;
	//App.Packet -> AMSenderC;

	//Timer interface
	App.TresholdTimer -> Timer1;  
	App.DataTimer -> Timer2;  
	//Fake Sensor read
	App.Read -> FakeSensorC;
	
	//Dissemination 
  	App.DisseminationControl-> DisseminationC;
  	App.Value -> Diss16C;
  	App.Update -> Diss16C;
  	
  	//Routing
  	App.RoutingControl -> Collector;
	App.Send -> CollectionSenderC;
	App.Receive -> Collector.Receive[0xee];
	App.RootControl -> Collector;
  	

}

